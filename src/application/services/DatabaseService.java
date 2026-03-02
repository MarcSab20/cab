package application.services;

import application.models.User;
import application.models.Role;
import application.models.Permission;
import application.utils.PasswordUtils;

// ✅ AJOUT : imports HikariCP
import com.zaxxer.hikari.HikariConfig;
import com.zaxxer.hikari.HikariDataSource;

import java.io.FileInputStream;
import java.io.IOException;
import java.io.InputStream;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service de base de données utilisant MySQL + HikariCP (pool de connexions)
 * VERSION OPTIMISÉE : Pool HikariCP + credentials externalisés
 */
public class DatabaseService {

    private static DatabaseService instance;

    // ✅ MODIFIÉ : credentials lus depuis un fichier .properties externe
    // Créer le fichier : config/database.properties (à la racine du projet, hors resources)
    private static final String CONFIG_FILE = "config/database.properties";
    private static final Properties DB_PROPS = loadProperties();

    private static final String DB_HOST     = DB_PROPS.getProperty("db.host",     "localhost");
    private static final String DB_PORT     = DB_PROPS.getProperty("db.port",     "3306");
    private static final String DB_NAME     = DB_PROPS.getProperty("db.name",     "document");
    private static final String DB_USER     = DB_PROPS.getProperty("db.user",     "marco");
    private static final String DB_PASSWORD = DB_PROPS.getProperty("db.password", "");

    private static final String JDBC_URL =
            "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME +
            "?useSSL=false" +
            "&allowPublicKeyRetrieval=true" +
            "&serverTimezone=UTC" +
            "&cachePrepStmts=true" +
            "&useServerPrepStmts=true" +
            "&rewriteBatchedStatements=true";

    // ✅ AJOUT : DataSource HikariCP (remplace la connexion unique)
    private HikariDataSource dataSource;

    // Connexion principale conservée UNIQUEMENT pour initialize() (createTables, insertDefaultData)
    private Connection mainConnection;
    private boolean initialized = false;

    // =========================================================================
    // SINGLETON
    // =========================================================================

    private DatabaseService() {}

    public static synchronized DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }

    // =========================================================================
    // ✅ AJOUT : Chargement des properties externe
    // =========================================================================

    /**
     * Charge le fichier config/database.properties.
     * Si introuvable, retourne des properties vides (les valeurs par défaut seront utilisées).
     *
     * Contenu attendu du fichier :
     *   db.host=localhost
     *   db.port=3306
     *   db.name=document
     *   db.user=marco
     *   db.password=VotreMotDePasse
     */
    private static Properties loadProperties() {
        Properties props = new Properties();
        try (InputStream in = new FileInputStream(CONFIG_FILE)) {
            props.load(in);
            System.out.println("✓ Configuration chargée depuis " + CONFIG_FILE);
        } catch (IOException e) {
            System.err.println("⚠ Fichier " + CONFIG_FILE + " introuvable - valeurs par défaut utilisées");
            System.err.println("  → Créez le fichier config/database.properties à la racine du projet");
        }
        return props;
    }

    // =========================================================================
    // ✅ AJOUT : Initialisation du pool HikariCP
    // =========================================================================

    /**
     * Initialise le pool de connexions HikariCP.
     * Appelé UNE SEULE FOIS dans initialize().
     */
    private void initPool() {
        HikariConfig config = new HikariConfig();

        config.setJdbcUrl(JDBC_URL);
        config.setUsername(DB_USER);
        config.setPassword(DB_PASSWORD);
        config.setDriverClassName("com.mysql.cj.jdbc.Driver");

        // Taille du pool
        config.setMaximumPoolSize(30);   // Max 10 connexions simultanées
        config.setMinimumIdle(5);        // 2 connexions maintenues à chaud au repos

        // Timeouts
        config.setConnectionTimeout(30_000);   // 30s max pour obtenir une connexion
        config.setIdleTimeout(600_000);         // Libère une connexion idle après 10min
        config.setMaxLifetime(1_800_000);       // Renouvelle chaque connexion après 30min

        // Performance MySQL
        config.addDataSourceProperty("cachePrepStmts",          "true");
        config.addDataSourceProperty("prepStmtCacheSize",        "250");
        config.addDataSourceProperty("prepStmtCacheSqlLimit",    "2048");
        config.addDataSourceProperty("useServerPrepStmts",       "true");
        config.addDataSourceProperty("rewriteBatchedStatements", "true");

        // Nom du pool (visible dans les logs et JMX)
        config.setPoolName("DocumentApp-Pool");

        dataSource = new HikariDataSource(config);

        System.out.println("✓ Pool HikariCP initialisé (" + config.getMaximumPoolSize() + " connexions max)");
    }

    // =========================================================================
    // INITIALISATION (inchangée dans sa logique, modifiée dans l'ordre d'appel)
    // =========================================================================

    /**
     * Initialise la base de données et crée les tables.
     * ✅ MODIFIÉ : appelle initPool() avant tout le reste.
     */
    public void initialize() throws SQLException {
        if (initialized) {
            System.out.println("DatabaseService déjà initialisé");
            return;
        }

        try {
            // Chargement du driver MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");

            // ✅ 1. Initialiser le pool HikariCP EN PREMIER
            initPool();

            // ✅ 2. Obtenir une connexion depuis le pool pour l'initialisation des tables
            mainConnection = dataSource.getConnection();
            mainConnection.setAutoCommit(true);

            System.out.println("✓ Connexion à MySQL établie via le pool");
            System.out.println("✓ Base de données : " + DB_NAME);
            System.out.println("✓ Utilisateur     : " + DB_USER);

            // Création des tables
            createTables();

            // Insertion des données par défaut
            insertDefaultData();

            // Libérer la connexion d'initialisation dans le pool
            mainConnection.close();
            mainConnection = null;

            initialized = true;
            System.out.println("✓ DatabaseService initialisé avec succès");

        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver MySQL non trouvé", e);
        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de l'initialisation : " + e.getMessage());
            throw e;
        }
    }

    // =========================================================================
    // ✅ MODIFIÉ : getConnection() utilise maintenant le pool
    // =========================================================================

    /**
     * Retourne une connexion depuis le pool HikariCP.
     * ⚠ TOUJOURS utiliser dans un try-with-resources pour la rendre au pool automatiquement.
     *
     * Exemple :
     *   try (Connection conn = databaseService.getConnection();
     *        PreparedStatement stmt = conn.prepareStatement(query)) { ... }
     */
    public Connection getConnection() throws SQLException {
        if (dataSource == null || dataSource.isClosed()) {
            throw new SQLException("Pool de connexions non initialisé ou fermé");
        }
        return dataSource.getConnection(); // Depuis le pool : ~0-1ms au lieu de ~50ms
    }

    // =========================================================================
    // MÉTHODES INTERNES (inchangées)
    // =========================================================================

    /**
     * Crée les tables de la base de données.
     * ✅ MODIFIÉ : utilise getConnection() au lieu de mainConnection directement.
     */
    private void createTables() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement()) {

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS roles (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    nom VARCHAR(50) UNIQUE NOT NULL,
                    description TEXT,
                    permissions TEXT,
                    actif BOOLEAN DEFAULT TRUE,
                    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS users (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    code VARCHAR(50) UNIQUE NOT NULL,
                    password VARCHAR(255) NOT NULL,
                    nom VARCHAR(100) NOT NULL,
                    prenom VARCHAR(100) NOT NULL,
                    email VARCHAR(100) UNIQUE NOT NULL,
                    role_id INT,
                    service_code VARCHAR(50),
                    niveau_autorite INT DEFAULT 0,
                    actif BOOLEAN DEFAULT TRUE,
                    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    dernier_acces TIMESTAMP NULL,
                    session_token VARCHAR(255),
                    FOREIGN KEY (role_id) REFERENCES roles(id) ON DELETE SET NULL,
                    INDEX idx_code (code),
                    INDEX idx_session_token (session_token),
                    INDEX idx_service_code (service_code)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

            stmt.executeUpdate("""
                CREATE TABLE IF NOT EXISTS logs_activite (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT,
                    action VARCHAR(100) NOT NULL,
                    details TEXT,
                    ip_address VARCHAR(45),
                    statut VARCHAR(20) DEFAULT 'info',
                    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
                    INDEX idx_timestamp (timestamp),
                    INDEX idx_user_id (user_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """);

            System.out.println("✓ Tables MySQL créées/vérifiées avec succès");
        }
    }

    /**
     * Insère les données par défaut.
     */
    private void insertDefaultData() throws SQLException {
        if (getRoleCount() > 0) {
            System.out.println("✓ Données par défaut déjà présentes");
            return;
        }
        createDefaultRoles();
        createDefaultAdmin();
        System.out.println("✓ Données par défaut insérées");
    }

    private void createDefaultRoles() throws SQLException {
        String query = "INSERT INTO roles (nom, description, permissions) VALUES (?, ?, ?)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, "Administrateur");
            stmt.setString(2, "Accès complet à toutes les fonctionnalités");
            stmt.setString(3, getAllPermissionsJson());
            stmt.executeUpdate();

            stmt.setString(1, "Gestionnaire");
            stmt.setString(2, "Gestion des documents et courriers");
            stmt.setString(3, getManagerPermissionsJson());
            stmt.executeUpdate();

            stmt.setString(1, "Utilisateur");
            stmt.setString(2, "Consultation et création de documents");
            stmt.setString(3, getUserPermissionsJson());
            stmt.executeUpdate();

            stmt.setString(1, "Invité");
            stmt.setString(2, "Accès en lecture seule");
            stmt.setString(3, getGuestPermissionsJson());
            stmt.executeUpdate();

            System.out.println("✓ Rôles par défaut créés");
        }
    }

    private void createDefaultAdmin() throws SQLException {
        String query = "INSERT INTO users (code, password, nom, prenom, email, role_id, niveau_autorite) " +
                       "VALUES (?, ?, ?, ?, ?, ?, 0)";

        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {

            stmt.setString(1, "admin");
            stmt.setString(2, PasswordUtils.hashPassword("admin123"));
            stmt.setString(3, "Administrateur");
            stmt.setString(4, "Système");
            stmt.setString(5, "admin@documentmanager.com");
            stmt.setInt(6, 1);
            stmt.executeUpdate();

            System.out.println("✓ Utilisateur administrateur créé (admin / admin123)");
        }
    }

    private int getRoleCount() throws SQLException {
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery("SELECT COUNT(*) FROM roles")) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }

    // =========================================================================
    // MÉTHODES PUBLIQUES (inchangées fonctionnellement)
    // =========================================================================

    public User getUserByCode(String code) throws SQLException {
        String query = """
            SELECT u.*, r.nom as role_nom, r.description as role_desc, r.permissions, r.actif as role_actif
            FROM users u
            LEFT JOIN roles r ON u.role_id = r.id
            WHERE u.code = ? AND u.actif = 1
        """;
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, code);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapResultSetToUser(rs) : null;
            }
        }
    }

    public User getUserBySessionToken(String sessionToken) throws SQLException {
        String query = """
            SELECT u.*, r.nom as role_nom, r.description as role_desc, r.permissions, r.actif as role_actif
            FROM users u
            LEFT JOIN roles r ON u.role_id = r.id
            WHERE u.session_token = ? AND u.actif = 1
        """;
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, sessionToken);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapResultSetToUser(rs) : null;
            }
        }
    }

    public List<User> getAllUsers() throws SQLException {
        String query = """
            SELECT u.*, r.nom as role_nom, r.description as role_desc, r.permissions, r.actif as role_actif
            FROM users u
            LEFT JOIN roles r ON u.role_id = r.id
            ORDER BY u.nom, u.prenom
        """;
        List<User> users = new ArrayList<>();
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            while (rs.next()) users.add(mapResultSetToUser(rs));
        }
        return users;
    }

    public User getUserById(int userId) throws SQLException {
        String query = """
            SELECT u.*, r.nom as role_nom, r.description as role_desc, r.permissions, r.actif as role_actif
            FROM users u
            LEFT JOIN roles r ON u.role_id = r.id
            WHERE u.id = ?
        """;
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userId);
            try (ResultSet rs = stmt.executeQuery()) {
                return rs.next() ? mapResultSetToUser(rs) : null;
            }
        }
    }

    public void updateUser(User user) throws SQLException {
        String query = """
            UPDATE users SET
                password = ?, nom = ?, prenom = ?, email = ?,
                dernier_acces = ?, session_token = ?,
                service_code = ?, niveau_autorite = ?
            WHERE id = ?
        """;
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setString(1, user.getPassword());
            stmt.setString(2, user.getNom());
            stmt.setString(3, user.getPrenom());
            stmt.setString(4, user.getEmail());
            stmt.setTimestamp(5, user.getDernierAcces() != null ? Timestamp.valueOf(user.getDernierAcces()) : null);
            stmt.setString(6, user.getSessionToken());
            stmt.setString(7, user.getServiceCode());
            stmt.setInt(8, user.getNiveauAutorite());
            stmt.setInt(9, user.getId());
            stmt.executeUpdate();
        }
    }

    public List<Role> getActiveRoles() throws SQLException {
        String query = "SELECT * FROM roles WHERE actif = 1 ORDER BY nom";
        List<Role> roles = new ArrayList<>();
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            while (rs.next()) roles.add(mapResultSetToRole(rs));
        }
        return roles;
    }

    public void logActivity(int userId, String action, String details, String ipAddress) throws SQLException {
        String query = "INSERT INTO logs_activite (user_id, action, details, ip_address) VALUES (?, ?, ?, ?)";
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setString(2, action);
            stmt.setString(3, details);
            stmt.setString(4, ipAddress);
            stmt.executeUpdate();
        }
    }

    // =========================================================================
    // MAPPING (inchangé)
    // =========================================================================

    User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setCode(rs.getString("code"));
        user.setPassword(rs.getString("password"));
        user.setNom(rs.getString("nom"));
        user.setPrenom(rs.getString("prenom"));
        user.setEmail(rs.getString("email"));
        user.setActif(rs.getBoolean("actif"));
        user.setServiceCode(rs.getString("service_code"));
        user.setNiveauAutorite(rs.getInt("niveau_autorite"));

        Timestamp dateCreation = rs.getTimestamp("date_creation");
        if (dateCreation != null) user.setDateCreation(dateCreation.toLocalDateTime());

        Timestamp dernierAcces = rs.getTimestamp("dernier_acces");
        if (dernierAcces != null) user.setDernierAcces(dernierAcces.toLocalDateTime());

        user.setSessionToken(rs.getString("session_token"));

        String roleNom = rs.getString("role_nom");
        if (roleNom != null) {
            Role role = new Role();
            role.setId(rs.getInt("role_id"));
            role.setNom(roleNom);
            role.setDescription(rs.getString("role_desc"));
            role.setActif(rs.getBoolean("role_actif"));

            String permissionsJson = rs.getString("permissions");
            role.setPermissions(permissionsJson != null ? parsePermissions(permissionsJson) : new HashSet<>());

            user.setRole(role);
        }
        return user;
    }

    private Role mapResultSetToRole(ResultSet rs) throws SQLException {
        Role role = new Role();
        role.setId(rs.getInt("id"));
        role.setNom(rs.getString("nom"));
        role.setDescription(rs.getString("description"));
        role.setActif(rs.getBoolean("actif"));

        String permissionsJson = rs.getString("permissions");
        role.setPermissions(permissionsJson != null ? parsePermissions(permissionsJson) : new HashSet<>());

        return role;
    }

    private Set<Permission> parsePermissions(String json) {
        Set<Permission> permissions = new HashSet<>();
        if (json == null || json.isBlank() || json.equals("[]")) return permissions;

        String cleaned = json.replace("[", "").replace("]", "").replace("\"", "");
        for (String permName : cleaned.split(",")) {
            try {
                permissions.add(Permission.valueOf(permName.trim()));
            } catch (IllegalArgumentException e) {
                System.err.println("Permission inconnue ignorée : " + permName);
            }
        }
        return permissions;
    }

    // =========================================================================
    // PERMISSIONS JSON (inchangées)
    // =========================================================================

    private String getAllPermissionsJson() {
        return "[\"ACCUEIL\",\"DASHBOARD\",\"COURRIER_LECTURE\",\"COURRIER_CREATION\",\"COURRIER_MODIFICATION\",\"COURRIER_SUPPRESSION\",\"COURRIER_ARCHIVAGE\",\"COURRIER_VALIDATION\",\"DOCUMENT_LECTURE\",\"DOCUMENT_CREATION\",\"DOCUMENT_MODIFICATION\",\"DOCUMENT_SUPPRESSION\",\"DOCUMENT_PARTAGE\",\"ADMIN_UTILISATEURS\",\"ADMIN_ROLES\",\"ADMIN_SYSTEME\",\"ADMIN_LOGS\",\"ADMIN_SAUVEGARDE\",\"RECHERCHE\",\"RECHERCHE_AVANCEE\",\"ARCHIVAGE_LECTURE\",\"ARCHIVAGE_GESTION\",\"RAPPORTS_LECTURE\",\"RAPPORTS_CREATION\",\"RAPPORTS_EXPORT\",\"STATISTIQUES\"]";
    }

    private String getManagerPermissionsJson() {
        return "[\"ACCUEIL\",\"DASHBOARD\",\"COURRIER_LECTURE\",\"COURRIER_CREATION\",\"COURRIER_MODIFICATION\",\"COURRIER_ARCHIVAGE\",\"DOCUMENT_LECTURE\",\"DOCUMENT_CREATION\",\"DOCUMENT_MODIFICATION\",\"DOCUMENT_PARTAGE\",\"RECHERCHE\",\"RECHERCHE_AVANCEE\",\"ARCHIVAGE_LECTURE\",\"RAPPORTS_LECTURE\",\"RAPPORTS_CREATION\",\"STATISTIQUES\"]";
    }

    private String getUserPermissionsJson() {
        return "[\"ACCUEIL\",\"DASHBOARD\",\"COURRIER_LECTURE\",\"COURRIER_CREATION\",\"DOCUMENT_LECTURE\",\"DOCUMENT_CREATION\",\"RECHERCHE\"]";
    }

    private String getGuestPermissionsJson() {
        return "[\"ACCUEIL\",\"COURRIER_LECTURE\",\"DOCUMENT_LECTURE\"]";
    }

    // =========================================================================
    // ✅ MODIFIÉ : Fermeture propre du pool
    // =========================================================================

    /**
     * Ferme le pool HikariCP proprement (à appeler dans Application.stop()).
     */
    public void close() {
        if (dataSource != null && !dataSource.isClosed()) {
            dataSource.close();
            System.out.println("✓ Pool HikariCP fermé proprement");
        }
    }

    public boolean isInitialized() { return initialized; }

    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("Test de connexion échoué : " + e.getMessage());
            return false;
        }
    }
}