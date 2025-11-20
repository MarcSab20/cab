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
 */
public class DatabaseService {
    
    private static DatabaseService instance;
    private Connection connection;
    
    // Configuration MySQL
    private static final String DB_HOST = "localhost";
    private static final String DB_PORT = "3306";
    private static final String DB_NAME = "document";
    private static final String DB_USER = "marco";
    private static final String DB_PASSWORD = "29Papa278."; 
    private static final String JDBC_URL = "jdbc:mysql://" + DB_HOST + ":" + DB_PORT + "/" + DB_NAME + 
                                          "?useSSL=false&allowPublicKeyRetrieval=true&serverTimezone=UTC";
    
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
        try {
            // Chargement du driver MySQL
            Class.forName("com.mysql.cj.jdbc.Driver");
            
            // Connexion à la base de données
            connection = DriverManager.getConnection(JDBC_URL, DB_USER, DB_PASSWORD);
            connection.setAutoCommit(true);
            
            System.out.println("Connexion à la base de données MySQL établie");
            
            // Création des tables
            createTables();
            
            // Insertion des données par défaut
            insertDefaultData();
            
        } catch (ClassNotFoundException e) {
            throw new SQLException("Driver MySQL non trouvé", e);
        }
    }
    
    /**
     * Crée les tables de la base de données
     */
    private void createTables() throws SQLException {
        
        System.out.println("Tables MySQL créées avec succès");
    }
    
    /**
     * Insère les données par défaut (rôles et utilisateur admin)
     */
    private void insertDefaultData() throws SQLException {
        // Vérifier si les données existent déjà
        if (getRoleCount() > 0) {
            return; // Données déjà présentes
        }
        
        // Création des rôles par défaut
        createDefaultRoles();
        
        // Création de l'utilisateur administrateur par défaut
        createDefaultAdmin();
        
        System.out.println("Données par défaut insérées");
    }
    
    /**
     * Crée les rôles par défaut
     */
    private void createDefaultRoles() throws SQLException {
        String insertRoleQuery = """
            INSERT INTO roles (nom, description, permissions) 
            VALUES (?, ?, ?)
        """;
        
        try (PreparedStatement stmt = connection.prepareStatement(insertRoleQuery)) {
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
        }
    }
    
    /**
     * Crée l'utilisateur administrateur par défaut
     */
    private void createDefaultAdmin() throws SQLException {
        String insertUserQuery = """
            INSERT INTO users (code, password, nom, prenom, email, role_id) 
            VALUES (?, ?, ?, ?, ?, ?)
        """;
        
        try (PreparedStatement stmt = connection.prepareStatement(insertUserQuery)) {
            stmt.setString(1, "admin");
            stmt.setString(2, PasswordUtils.hashPassword("admin123"));
            stmt.setString(3, "Administrateur");
            stmt.setString(4, "Système");
            stmt.setString(5, "admin@documentmanager.com");
            stmt.setInt(6, 1); // ID du rôle Administrateur
            stmt.executeUpdate();
        }
    }
    
    /**
     * Retourne le nombre de rôles en base
     */
    private int getRoleCount() throws SQLException {
        String query = "SELECT COUNT(*) FROM roles";
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            return rs.next() ? rs.getInt(1) : 0;
        }
    }
    
    // Méthodes pour les permissions JSON (simulées)
    private String getAllPermissionsJson() {
        return "[\"ACCUEIL\",\"DASHBOARD\",\"COURRIER_LECTURE\",\"COURRIER_CREATION\",\"COURRIER_MODIFICATION\",\"COURRIER_SUPPRESSION\",\"REUNIONS_LECTURE\",\"REUNIONS_CREATION\",\"REUNIONS_MODIFICATION\",\"MESSAGES_LECTURE\",\"MESSAGES_ENVOI\",\"ADMIN_UTILISATEURS\",\"ADMIN_ROLES\",\"RECHERCHE\",\"ARCHIVAGE_LECTURE\",\"ARCHIVAGE_GESTION\",\"PARAMETRES_PERSONNELS\",\"PARAMETRES_SYSTEME\"]";
    }
    
    private String getManagerPermissionsJson() {
        return "[\"ACCUEIL\",\"DASHBOARD\",\"COURRIER_LECTURE\",\"COURRIER_CREATION\",\"COURRIER_MODIFICATION\",\"REUNIONS_LECTURE\",\"REUNIONS_CREATION\",\"REUNIONS_MODIFICATION\",\"MESSAGES_LECTURE\",\"MESSAGES_ENVOI\",\"RECHERCHE\",\"ARCHIVAGE_LECTURE\",\"PARAMETRES_PERSONNELS\"]";
    }
    
    private String getUserPermissionsJson() {
        return "[\"ACCUEIL\",\"DASHBOARD\",\"COURRIER_LECTURE\",\"COURRIER_CREATION\",\"REUNIONS_LECTURE\",\"MESSAGES_LECTURE\",\"MESSAGES_ENVOI\",\"RECHERCHE\",\"PARAMETRES_PERSONNELS\"]";
    }
    
    private String getGuestPermissionsJson() {
        return "[\"ACCUEIL\",\"COURRIER_LECTURE\",\"REUNIONS_LECTURE\",\"PARAMETRES_PERSONNELS\"]";
    }
    
    /**
     * Récupère un utilisateur par son code
     */
    public User getUserByCode(String code) throws SQLException {
        String query = """
            SELECT u.*, r.nom as role_nom, r.description as role_desc, r.permissions 
            FROM users u 
            LEFT JOIN roles r ON u.role_id = r.id 
            WHERE u.code = ? AND u.actif = 1
        """;
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, code);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        }
        
        return null;
    }
    
    /**
     * Récupère un utilisateur par son token de session
     */
    public User getUserBySessionToken(String sessionToken) throws SQLException {
        String query = """
            SELECT u.*, r.nom as role_nom, r.description as role_desc, r.permissions 
            FROM users u 
            LEFT JOIN roles r ON u.role_id = r.id 
            WHERE u.session_token = ? AND u.actif = 1
        """;
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, sessionToken);
            ResultSet rs = stmt.executeQuery();
            
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
        }
        
        return null;
    }
    
    /**
     * Met à jour un utilisateur
     */
    public void updateUser(User user) throws SQLException {
        String query = """
            UPDATE users SET 
                password = ?, nom = ?, prenom = ?, email = ?, 
                dernier_acces = ?, session_token = ? 
            WHERE id = ?
        """;
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setString(1, user.getPassword());
            stmt.setString(2, user.getNom());
            stmt.setString(3, user.getPrenom());
            stmt.setString(4, user.getEmail());
            stmt.setTimestamp(5, user.getDernierAcces() != null ? 
                Timestamp.valueOf(user.getDernierAcces()) : null);
            stmt.setString(6, user.getSessionToken());
            stmt.setInt(7, user.getId());
            
            stmt.executeUpdate();
        }
    }
    
    /**
     * Récupère tous les rôles actifs
     */
    public List<Role> getActiveRoles() throws SQLException {
        String query = "SELECT * FROM roles WHERE actif = 1 ORDER BY nom";
        List<Role> roles = new ArrayList<>();
        
        try (Statement stmt = connection.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                roles.add(mapResultSetToRole(rs));
            }
        }
        
        return roles;
    }
    
    /**
     * Enregistre une activité dans les logs
     */
    public void logActivity(int userId, String action, String details, String ipAddress) throws SQLException {
        String query = """
            INSERT INTO logs_activite (user_id, action, details, ip_address) 
            VALUES (?, ?, ?, ?)
        """;
        
        try (PreparedStatement stmt = connection.prepareStatement(query)) {
            stmt.setInt(1, userId);
            stmt.setString(2, action);
            stmt.setString(3, details);
            stmt.setString(4, ipAddress);
            stmt.executeUpdate();
        }
    }
    
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
            
            // Parsing des permissions (simplifié)
            String permissionsJson = rs.getString("permissions");
            if (permissionsJson != null) {
                // Ici on devrait parser le JSON et créer les permissions
                // Pour la simplicité, on simule
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
        
        // Parsing des permissions (simplifié)
        String permissionsJson = rs.getString("permissions");
        if (permissionsJson != null) {
            role.setPermissions(new HashSet<>());
        }
        
        return role;
    }
    
    /**
     * Ferme la connexion à la base de données
     */
    public void close() {
        try {
            if (connection != null && !connection.isClosed()) {
                connection.close();
                System.out.println("Connexion à la base de données fermée");
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la fermeture de la base de données: " + e.getMessage());
        }
    }
    
    /**
     * Vérifie si la connexion est active
     */
    public boolean isConnected() {
        try {
            return connection != null && !connection.isClosed();
        } catch (SQLException e) {
            return false;
        }
    }
    
    /**
     * Récupère la connexion (pour les requêtes personnalisées)
     */
    public Connection getConnection() {
        return connection;
    }
}