package application.services;

import application.models.User;
import application.models.Role;
import application.models.Permission;
import application.utils.PasswordUtils;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;

/**
 * Service de base de données utilisant MySQL pour la persistance
 * VERSION AMÉLIORÉE : Gestion correcte du pool de connexions + Support Workflow
 */
public class DatabaseService {
    
    private static DatabaseService instance;
    
    // Configuration MySQL
    private static final String DB_HOST = "localhost";
    private static final String DB_PORT = "3306";
    private static final String DB_NAME = "document";
    private static final String DB_USER = "marco";
    private static final String DB_PASSWORD = "29Papa278."; 
    
    // URL JDBC améliorée avec gestion de reconnexion
    private static final String JDBC_URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME + 
                                          "?useSSL=false" +
                                          "&allowPublicKeyRetrieval=true" +
                                          "&serverTimezone=UTC" +
                                          "&autoReconnect=true" +           // Reconnexion automatique
                                          "&maxReconnects=3" +              // Nombre de tentatives
                                          "&initialTimeout=2" +             // Timeout initial
                                          "&cachePrepStmts=true" +          // Cache des PreparedStatements
                                          "&useServerPrepStmts=true" +      // Utiliser PreparedStatements serveur
                                          "&rewriteBatchedStatements=true"; // Optimisation des batchs
    
    // Connexion principale pour l'initialisation
    private Connection mainConnection;
    private boolean initialized = false;
    
    private DatabaseService() {}
    
    public static synchronized DatabaseService getInstance() {
        if (instance == null) {
            instance = new DatabaseService();
        }
        return instance;
    }
    
    /**
     * Initialise la base de données et crée les tables
     */
    public void initialize() throws SQLException {
        if (initialized) {
            System.out.println("DatabaseService déjà initialisé");
            return;
        }
        
        try {
            // Chargement du driver MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Connexion à la base de données pour initialisation
            mainConnection = createNewConnection();
            mainConnection.setAutoCommit(true);
            
            System.out.println("✓ Connexion à la base de données MySQL établie");
            System.out.println("✓ Base de données: " + DB_NAME);
            System.out.println("✓ Utilisateur: " + DB_USER);
            
            // Création des tables
            createTables();
            
            // Insertion des données par défaut
            insertDefaultData();
            
            initialized = true;
            System.out.println("✓ DatabaseService initialisé avec succès");
            
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver MySQL non trouvé", e);
        } catch (SQLException e) {
            System.err.println("✗ Erreur lors de l'initialisation: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Crée une NOUVELLE connexion à chaque appel
     * CORRECTION: Ne réutilise pas la même connexion
     */
    private Connection createNewConnection() throws SQLException {
        return DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);
    }
    
    /**
     * Récupère une connexion pour une opération
     * CORRECTION: Retourne une NOUVELLE connexion à chaque fois
     */
    public Connection getConnection() throws SQLException {
        try {
            Connection conn = createNewConnection();
            
            // Vérifier que la connexion est valide
            if (conn == null || conn.isClosed()) {
                throw new SQLException("Impossible d'établir une connexion à la base de données");
            }
            
            conn.setAutoCommit(true);
            return conn;
            
        } catch (SQLException e) {
            System.err.println("Erreur lors de la création de connexion: " + e.getMessage());
            throw e;
        }
    }
    
    /**
     * Crée les tables de la base de données
     */
    private void createTables() throws SQLException {
        try (Statement stmt = mainConnection.createStatement()) {
            
            // Table des rôles
            String createRolesTable = """
                CREATE TABLE IF NOT EXISTS roles (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    nom VARCHAR(50) UNIQUE NOT NULL,
                    description TEXT,
                    permissions TEXT,
                    actif BOOLEAN DEFAULT TRUE,
                    date_creation TIMESTAMP DEFAULT CURRENT_TIMESTAMP
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;
            stmt.executeUpdate(createRolesTable);
            
            // Table des utilisateurs
            String createUsersTable = """
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
            """;
            stmt.executeUpdate(createUsersTable);
            
            // Table des logs d'activité
            String createLogsTable = """
                CREATE TABLE IF NOT EXISTS logs_activite (
                    id INT AUTO_INCREMENT PRIMARY KEY,
                    user_id INT,
                    action VARCHAR(100) NOT NULL,
                    details TEXT,
                    ip_address VARCHAR(45),
                    timestamp TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
                    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL,
                    INDEX idx_timestamp (timestamp),
                    INDEX idx_user_id (user_id)
                ) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4
            """;
            stmt.executeUpdate(createLogsTable);
            
            System.out.println("✓ Tables MySQL créées/vérifiées avec succès");
        }
    }
    
    /**
     * Insère les données par défaut (rôles et utilisateur admin)
     */
    private void insertDefaultData() throws SQLException {
        // Vérifier si les données existent déjà
        if (getRoleCount() > 0) {
            System.out.println("✓ Données par défaut déjà présentes");
            return; // Données déjà présentes
        }
        
        // Création des rôles par défaut
        createDefaultRoles();
        
        // Création de l'utilisateur administrateur par défaut
        createDefaultAdmin();
        
        System.out.println("✓ Données par défaut insérées");
    }
    
    /**
     * Crée les rôles par défaut
     */
    private void createDefaultRoles() throws SQLException {
        String insertRoleQuery = """
            INSERT INTO roles (nom, description, permissions) 
            VALUES (?, ?, ?)
        """;
        
        try (PreparedStatement stmt = mainConnection.prepareStatement(insertRoleQuery)) {
            // Rôle Administrateur
            stmt.setString(1, "Administrateur");
            stmt.setString(2, "Accès complet à toutes les fonctionnalités");
            stmt.setString(3, getAllPermissionsJson());
            stmt.executeUpdate();
            
            // Rôle Gestionnaire
            stmt.setString(1, "Gestionnaire");
            stmt.setString(2, "Gestion des documents et courriers");
            stmt.setString(3, getManagerPermissionsJson());
            stmt.executeUpdate();
            
            // Rôle Utilisateur
            stmt.setString(1, "Utilisateur");
            stmt.setString(2, "Consultation et création de documents");
            stmt.setString(3, getUserPermissionsJson());
            stmt.executeUpdate();
            
            // Rôle Invité
            stmt.setString(1, "Invité");
            stmt.setString(2, "Accès en lecture seule");
            stmt.setString(3, getGuestPermissionsJson());
            stmt.executeUpdate();
            
            System.out.println("✓ Rôles par défaut créés");
        }
    }
    
    /**
     * Crée l'utilisateur administrateur par défaut
     */
    private void createDefaultAdmin() throws SQLException {
        String insertUserQuery = """
            INSERT INTO users (code, password, nom, prenom, email, role_id, service_code, niveau_autorite) 
            VALUES (?, ?, ?, ?, ?, ?, NULL, 0)
        """;
        
        try (PreparedStatement stmt = mainConnection.prepareStatement(insertUserQuery)) {
            stmt.setString(1, "admin");
            stmt.setString(2, PasswordUtils.hashPassword("admin123"));
            stmt.setString(3, "Administrateur");
            stmt.setString(4, "Système");
            stmt.setString(5, "admin@documentmanager.com");
            stmt.setInt(6, 1); // ID du rôle Administrateur
            stmt.executeUpdate();
            
            System.out.println("✓ Utilisateur administrateur créé (admin/admin123)");
        }
    }
    
    /**
     * Retourne le nombre de rôles en base
     */
    private int getRoleCount() throws SQLException {
        String query = "SELECT COUNT(*) FROM roles";
        try (Statement stmt = mainConnection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
    
    // Méthodes pour les permissions JSON
    private String getAllPermissionsJson() {
        return "[\"ACCUEIL\",\"DASHBOARD\",\"COURRIER_LECTURE\",\"COURRIER_CREATION\",\"COURRIER_MODIFICATION\",\"COURRIER_SUPPRESSION\",\"COURRIER_ARCHIVAGE\",\"COURRIER_VALIDATION\",\"DOCUMENT_LECTURE\",\"DOCUMENT_CREATION\",\"DOCUMENT_MODIFICATION\",\"DOCUMENT_SUPPRESSION\",\"DOCUMENT_PARTAGE\",\"REUNIONS_LECTURE\",\"REUNIONS_CREATION\",\"REUNIONS_MODIFICATION\",\"REUNIONS_SUPPRESSION\",\"REUNIONS_ANIMATION\",\"MESSAGES_LECTURE\",\"MESSAGES_ENVOI\",\"MESSAGES_SUPPRESSION\",\"MESSAGES_DIFFUSION\",\"ADMIN_UTILISATEURS\",\"ADMIN_ROLES\",\"ADMIN_SYSTEME\",\"ADMIN_LOGS\",\"ADMIN_SAUVEGARDE\",\"RECHERCHE\",\"RECHERCHE_AVANCEE\",\"ARCHIVAGE_LECTURE\",\"ARCHIVAGE_GESTION\",\"RAPPORTS_LECTURE\",\"RAPPORTS_CREATION\",\"RAPPORTS_EXPORT\",\"STATISTIQUES\"]";
    }
    
    private String getManagerPermissionsJson() {
        return "[\"ACCUEIL\",\"DASHBOARD\",\"COURRIER_LECTURE\",\"COURRIER_CREATION\",\"COURRIER_MODIFICATION\",\"COURRIER_ARCHIVAGE\",\"DOCUMENT_LECTURE\",\"DOCUMENT_CREATION\",\"DOCUMENT_MODIFICATION\",\"DOCUMENT_PARTAGE\",\"REUNIONS_LECTURE\",\"REUNIONS_CREATION\",\"REUNIONS_MODIFICATION\",\"REUNIONS_ANIMATION\",\"MESSAGES_LECTURE\",\"MESSAGES_ENVOI\",\"RECHERCHE\",\"RECHERCHE_AVANCEE\",\"ARCHIVAGE_LECTURE\",\"RAPPORTS_LECTURE\",\"RAPPORTS_CREATION\",\"STATISTIQUES\"]";
    }
    
    private String getUserPermissionsJson() {
        return "[\"ACCUEIL\",\"DASHBOARD\",\"COURRIER_LECTURE\",\"COURRIER_CREATION\",\"DOCUMENT_LECTURE\",\"DOCUMENT_CREATION\",\"REUNIONS_LECTURE\",\"REUNIONS_CREATION\",\"MESSAGES_LECTURE\",\"MESSAGES_ENVOI\",\"RECHERCHE\"]";
    }
    
    private String getGuestPermissionsJson() {
        return "[\"ACCUEIL\",\"COURRIER_LECTURE\",\"DOCUMENT_LECTURE\",\"REUNIONS_LECTURE\"]";
    }
    
    // ==================== MÉTHODES UTILISATEURS ====================
    
    /**
     * Récupère un utilisateur par son code
     * CORRECTION: Utilise une NOUVELLE connexion
     */
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
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Récupère un utilisateur par son token de session
     * CORRECTION: Utilise une NOUVELLE connexion
     */
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
                if (rs.next()) {
                    return mapResultSetToUser(rs);
                }
            }
        }
        
        return null;
    }
    
    /**
     * Met à jour un utilisateur
     * CORRECTION: Utilise une NOUVELLE connexion
     */
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
            stmt.setTimestamp(5, user.getDernierAcces() != null ? 
                Timestamp.valueOf(user.getDernierAcces()) : null);
            stmt.setString(6, user.getSessionToken());
            stmt.setString(7, user.getServiceCode());
            stmt.setInt(8, user.getNiveauAutorite());
            stmt.setInt(9, user.getId());
            
            stmt.executeUpdate();
        }
    }
    
    /**
     * Récupère tous les rôles actifs
     * CORRECTION: Utilise une NOUVELLE connexion
     */
    public List<Role> getActiveRoles() throws SQLException {
        String query = "SELECT * FROM roles WHERE actif = 1 ORDER BY nom";
        List<Role> roles = new ArrayList<>();
        
        try (Connection conn = getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                roles.add(mapResultSetToRole(rs));
            }
        }
        
        return roles;
    }
    
    /**
     * Enregistre une activité dans les logs
     * CORRECTION: Utilise une NOUVELLE connexion
     */
    public void logActivity(int userId, String action, String details, String ipAddress) throws SQLException {
        String query = """
            INSERT INTO logs_activite (user_id, action, details, ip_address) 
            VALUES (?, ?, ?, ?)
        """;
        
        try (Connection conn = getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, userId);
            stmt.setString(2, action);
            stmt.setString(3, details);
            stmt.setString(4, ipAddress);
            stmt.executeUpdate();
        }
    }
    
    // ==================== MAPPING UTILISATEURS/ROLES ====================
    
    /**
     * Mappe un ResultSet vers un objet User
     */
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setCode(rs.getString("code"));
        user.setPassword(rs.getString("password"));
        user.setNom(rs.getString("nom"));
        user.setPrenom(rs.getString("prenom"));
        user.setEmail(rs.getString("email"));
        user.setActif(rs.getBoolean("actif"));
        
        // Nouveaux champs pour le workflow
        user.setServiceCode(rs.getString("service_code"));
        user.setNiveauAutorite(rs.getInt("niveau_autorite"));
        
        Timestamp dateCreation = rs.getTimestamp("date_creation");
        if (dateCreation != null) {
            user.setDateCreation(dateCreation.toLocalDateTime());
        }
        
        Timestamp dernierAcces = rs.getTimestamp("dernier_acces");
        if (dernierAcces != null) {
            user.setDernierAcces(dernierAcces.toLocalDateTime());
        }
        
        user.setSessionToken(rs.getString("session_token"));
        
        // Mapping du rôle
        String roleNom = rs.getString("role_nom");
        if (roleNom != null) {
            Role role = new Role();
            role.setId(rs.getInt("role_id"));
            role.setNom(roleNom);
            role.setDescription(rs.getString("role_desc"));
            role.setActif(rs.getBoolean("role_actif"));
            
            // Parsing des permissions
            String permissionsJson = rs.getString("permissions");
            if (permissionsJson != null && !permissionsJson.trim().isEmpty()) {
                role.setPermissions(parsePermissions(permissionsJson));
            } else {
                role.setPermissions(new HashSet<>());
            }
            
            user.setRole(role);
        }
        
        return user;
    }
    
    /**
     * Mappe un ResultSet vers un objet Role
     */
    private Role mapResultSetToRole(ResultSet rs) throws SQLException {
        Role role = new Role();
        role.setId(rs.getInt("id"));
        role.setNom(rs.getString("nom"));
        role.setDescription(rs.getString("description"));
        role.setActif(rs.getBoolean("actif"));
        
        // Parsing des permissions
        String permissionsJson = rs.getString("permissions");
        if (permissionsJson != null && !permissionsJson.trim().isEmpty()) {
            role.setPermissions(parsePermissions(permissionsJson));
        } else {
            role.setPermissions(new HashSet<>());
        }
        
        return role;
    }
    
    /**
     * Parse les permissions depuis le JSON
     */
    private Set<Permission> parsePermissions(String json) {
        Set<Permission> permissions = new HashSet<>();
        
        if (json == null || json.trim().isEmpty() || json.equals("[]")) {
            return permissions;
        }
        
        // Parse simple du JSON
        String cleaned = json.replace("[", "").replace("]", "").replace("\"", "");
        String[] permNames = cleaned.split(",");
        
        for (String permName : permNames) {
            try {
                Permission perm = Permission.valueOf(permName.trim());
                permissions.add(perm);
            } catch (IllegalArgumentException e) {
                System.err.println("Permission inconnue ignorée: " + permName);
            }
        }
        
        return permissions;
    }
    
    // ==================== MÉTHODES DE FERMETURE ====================
    
    /**
     * Ferme la connexion principale d'initialisation
     * CORRECTION: Ne ferme que la connexion principale
     */
    public void close() {
        try {
            if (mainConnection != null && !mainConnection.isClosed()) {
                mainConnection.close();
                System.out.println("✓ Connexion principale fermée");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la fermeture de la connexion: " + e.getMessage());
        }
    }
    
    /**
     * Vérifie si le service est initialisé
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Vérifie si la connexion principale est active
     */
    public boolean isConnected() {
        try {
            return mainConnection != null && !mainConnection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Teste la connexion à la base de données
     */
    public boolean testConnection() {
        try (Connection conn = getConnection()) {
            return conn != null && !conn.isClosed();
        } catch (SQLException e) {
            System.err.println("Test de connexion échoué: " + e.getMessage());
            return false;
        }
    }
}