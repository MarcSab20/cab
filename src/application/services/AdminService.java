package application.services;

import application.models.User;
import application.models.Role;
import application.models.Permission;
import application.models.LogActivity;
import application.utils.PasswordUtils;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Service pour les opérations d'administration
 */
public class AdminService {
    
    private static AdminService instance;
    private final DatabaseService databaseService;
    
    private AdminService() {
        this.databaseService = DatabaseService.getInstance();
    }
    
    public static synchronized AdminService getInstance() {
        if (instance == null) {
            instance = new AdminService();
        }
        return instance;
    }
    
    // ========================================
    // GESTION DES UTILISATEURS
    // ========================================
    
    /**
     * Récupère tous les utilisateurs
     */
    public List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        
        String query = """
            SELECT u.*, r.nom as role_nom, r.description as role_desc, r.permissions, r.actif as role_actif
            FROM users u
            LEFT JOIN roles r ON u.role_id = r.id
            ORDER BY u.nom, u.prenom
        """;
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                User user = mapResultSetToUser(rs);
                users.add(user);
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération utilisateurs: " + e.getMessage());
            e.printStackTrace();
        }
        
        return users;
    }
    
    /**
     * Crée un nouvel utilisateur
     */
    public boolean createUser(User user) {
        String query = """
            INSERT INTO users (code, password, nom, prenom, email, role_id, actif)
            VALUES (?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, user.getCode());
            stmt.setString(2, user.getPassword()); // Déjà hashé
            stmt.setString(3, user.getNom());
            stmt.setString(4, user.getPrenom());
            stmt.setString(5, user.getEmail());
            stmt.setInt(6, user.getRole() != null ? user.getRole().getId() : 0);
            stmt.setBoolean(7, user.isActif());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    user.setId(generatedKeys.getInt(1));
                }
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur création utilisateur: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Met à jour un utilisateur
     */
    public boolean updateUser(User user) {
        String query = """
            UPDATE users SET
                code = ?, nom = ?, prenom = ?, email = ?,
                role_id = ?, actif = ?
            WHERE id = ?
        """;
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, user.getCode());
            stmt.setString(2, user.getNom());
            stmt.setString(3, user.getPrenom());
            stmt.setString(4, user.getEmail());
            stmt.setInt(5, user.getRole() != null ? user.getRole().getId() : 0);
            stmt.setBoolean(6, user.isActif());
            stmt.setInt(7, user.getId());
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Erreur modification utilisateur: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Supprime un utilisateur
     */
    public boolean deleteUser(int userId) {
        String query = "DELETE FROM users WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, userId);
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Erreur suppression utilisateur: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Réinitialise le mot de passe d'un utilisateur
     */
    public boolean resetPassword(User user, String newPassword) {
        String query = "UPDATE users SET password = ? WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            String hashedPassword = PasswordUtils.hashPassword(newPassword);
            stmt.setString(1, hashedPassword);
            stmt.setInt(2, user.getId());
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Erreur reset password: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    // ========================================
    // GESTION DES RÔLES
    // ========================================
    
    /**
     * Récupère tous les rôles
     */
    public List<Role> getAllRoles() {
        List<Role> rolesList = new ArrayList<>();
        
        String query = "SELECT * FROM roles ORDER BY nom";
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                Role role = mapResultSetToRole(rs);
                rolesList.add(role);
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération rôles: " + e.getMessage());
            e.printStackTrace();
        }
        
        return rolesList;
    }
    
    /**
     * Crée un nouveau rôle
     */
    public boolean createRole(Role role) {
        String query = """
            INSERT INTO roles (nom, description, permissions, actif)
            VALUES (?, ?, ?, ?)
        """;
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, role.getNom());
            stmt.setString(2, role.getDescription());
            stmt.setString(3, permissionsToJson(role.getPermissions()));
            stmt.setBoolean(4, role.isActif());
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                ResultSet generatedKeys = stmt.getGeneratedKeys();
                if (generatedKeys.next()) {
                    role.setId(generatedKeys.getInt(1));
                }
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur création rôle: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Met à jour un rôle
     */
    public boolean updateRole(Role role) {
        String query = """
            UPDATE roles SET
                nom = ?, description = ?, permissions = ?, actif = ?
            WHERE id = ?
        """;
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, role.getNom());
            stmt.setString(2, role.getDescription());
            stmt.setString(3, permissionsToJson(role.getPermissions()));
            stmt.setBoolean(4, role.isActif());
            stmt.setInt(5, role.getId());
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Erreur modification rôle: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Supprime un rôle
     */
    public boolean deleteRole(int roleId) {
        String query = "DELETE FROM roles WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, roleId);
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Erreur suppression rôle: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    // ========================================
    // GESTION DES LOGS
    // ========================================
    
    /**
     * Récupère les logs récents
     */
    public List<LogActivity> getRecentLogs(int limit) {
        return getLogs(null, null, null, null, limit);
    }
    
    /**
     * Récupère les logs avec filtres
     */
    public List<LogActivity> getLogs(LocalDateTime debut, LocalDateTime fin, 
                                     String typeAction, String utilisateur) {
        return getLogs(debut, fin, typeAction, utilisateur, -1);
    }
    
    /**
     * Récupère les logs avec tous les paramètres
     */
    public List<LogActivity> getLogs(LocalDateTime debut, LocalDateTime fin, 
                                     String typeAction, String utilisateur, int limit) {
        List<LogActivity> logsList = new ArrayList<>();
        
        StringBuilder query = new StringBuilder("""
            SELECT l.*, u.code as user_code
            FROM logs_activite l
            LEFT JOIN users u ON l.user_id = u.id
            WHERE 1=1
        """);
        
        List<Object> params = new ArrayList<>();
        
        if (debut != null) {
            query.append(" AND l.timestamp >= ?");
            params.add(Timestamp.valueOf(debut));
        }
        
        if (fin != null) {
            query.append(" AND l.timestamp <= ?");
            params.add(Timestamp.valueOf(fin));
        }
        
        if (typeAction != null && !typeAction.equals("Toutes")) {
            query.append(" AND l.action LIKE ?");
            params.add("%" + typeAction + "%");
        }
        
        if (utilisateur != null && !utilisateur.equals("Tous")) {
            query.append(" AND u.code = ?");
            params.add(utilisateur);
        }
        
        query.append(" ORDER BY l.timestamp DESC");
        
        if (limit > 0) {
            query.append(" LIMIT ?");
            params.add(limit);
        }
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query.toString())) {
            
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            
            ResultSet rs = stmt.executeQuery();
            
            while (rs.next()) {
                LogActivity log = mapResultSetToLog(rs);
                logsList.add(log);
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération logs: " + e.getMessage());
            e.printStackTrace();
        }
        
        return logsList;
    }
    
    /**
     * Enregistre une action dans les logs
     */
    public void logAction(int userId, String action, String details) {
        logAction(userId, action, details, null);
    }
    
    /**
     * Enregistre une action dans les logs avec adresse IP
     */
    public void logAction(int userId, String action, String details, String ipAddress) {
        String query = """
            INSERT INTO logs_activite (user_id, action, details, ip_address)
            VALUES (?, ?, ?, ?)
        """;
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, userId);
            stmt.setString(2, action);
            stmt.setString(3, details);
            stmt.setString(4, ipAddress);
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Erreur log action: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ========================================
    // EXPORT CSV
    // ========================================
    
    /**
     * Exporte la liste des utilisateurs en CSV
     */
    public boolean exportUsersToCSV(File file, List<User> users) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            
            // En-têtes
            writer.println("Code,Nom,Prénom,Email,Rôle,Statut,Date création,Dernier accès");
            
            // Données
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            
            for (User user : users) {
                StringBuilder line = new StringBuilder();
                
                // Code
                line.append(escapeCsv(user.getCode())).append(",");
                
                // Nom
                line.append(escapeCsv(user.getNom())).append(",");
                
                // Prénom
                line.append(escapeCsv(user.getPrenom())).append(",");
                
                // Email
                line.append(escapeCsv(user.getEmail())).append(",");
                
                // Rôle
                String roleName = user.getRole() != null ? user.getRole().getNom() : "";
                line.append(escapeCsv(roleName)).append(",");
                
                // Statut
                line.append(user.isActif() ? "Actif" : "Inactif").append(",");
                
                // Date création
                String dateCreation = user.getDateCreation() != null 
                    ? user.getDateCreation().format(formatter) 
                    : "";
                line.append(escapeCsv(dateCreation)).append(",");
                
                // Dernier accès
                String dernierAcces = user.getDernierAcces() != null 
                    ? user.getDernierAcces().format(formatter) 
                    : "Jamais";
                line.append(escapeCsv(dernierAcces));
                
                writer.println(line.toString());
            }
            
            return true;
            
        } catch (IOException e) {
            System.err.println("Erreur lors de l'export CSV des utilisateurs: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Exporte les logs d'activité en CSV
     */
    public boolean exportLogsToCSV(File file, List<LogActivity> logs) {
        try (PrintWriter writer = new PrintWriter(new FileWriter(file))) {
            
            // En-têtes
            writer.println("Date/Heure,Utilisateur,Action,Détails,Adresse IP,Statut");
            
            // Données
            for (LogActivity log : logs) {
                StringBuilder line = new StringBuilder();
                
                // Date/Heure
                line.append(escapeCsv(log.getTimestampFormatted())).append(",");
                
                // Utilisateur
                String userCode = log.getUserCode() != null ? log.getUserCode() : "Système";
                line.append(escapeCsv(userCode)).append(",");
                
                // Action
                line.append(escapeCsv(log.getAction())).append(",");
                
                // Détails
                String details = log.getDetails() != null ? log.getDetails() : "";
                line.append(escapeCsv(details)).append(",");
                
                // Adresse IP
                String ipAddress = log.getIpAddress() != null ? log.getIpAddress() : "";
                line.append(escapeCsv(ipAddress)).append(",");
                
                // Statut
                String statut = log.getStatut() != null ? log.getStatut() : "Succès";
                line.append(escapeCsv(statut));
                
                writer.println(line.toString());
            }
            
            return true;
            
        } catch (IOException e) {
            System.err.println("Erreur lors de l'export CSV des logs: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Échappe les valeurs pour le format CSV
     */
    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }
        
        // Si la valeur contient une virgule, un guillemet ou un retour à la ligne,
        // on l'entoure de guillemets et on double les guillemets internes
        if (value.contains(",") || value.contains("\"") || value.contains("\n")) {
            return "\"" + value.replace("\"", "\"\"") + "\"";
        }
        
        return value;
    }
    
    // ========================================
    // MÉTHODES UTILITAIRES
    // ========================================
    
    /**
     * Mappe un ResultSet vers un User
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
            role.setActif(rs.getBoolean("role_actif"));
            
            String permissionsJson = rs.getString("permissions");
            if (permissionsJson != null) {
                role.setPermissions(jsonToPermissions(permissionsJson));
            }
            
            user.setRole(role);
        }
        
        return user;
    }
    
    /**
     * Mappe un ResultSet vers un Role
     */
    private Role mapResultSetToRole(ResultSet rs) throws SQLException {
        Role role = new Role();
        role.setId(rs.getInt("id"));
        role.setNom(rs.getString("nom"));
        role.setDescription(rs.getString("description"));
        role.setActif(rs.getBoolean("actif"));
        
        String permissionsJson = rs.getString("permissions");
        if (permissionsJson != null) {
            role.setPermissions(jsonToPermissions(permissionsJson));
        }
        
        return role;
    }
    
    /**
     * Mappe un ResultSet vers un LogActivity
     */
    private LogActivity mapResultSetToLog(ResultSet rs) throws SQLException {
        LogActivity log = new LogActivity();
        log.setId(rs.getInt("id"));
        log.setUserId(rs.getInt("user_id"));
        log.setUserCode(rs.getString("user_code"));
        log.setAction(rs.getString("action"));
        log.setDetails(rs.getString("details"));
        log.setIpAddress(rs.getString("ip_address"));
        
        Timestamp timestamp = rs.getTimestamp("timestamp");
        if (timestamp != null) {
            log.setTimestamp(timestamp.toLocalDateTime());
        }
        
        return log;
    }
    
    /**
     * Convertit un Set de permissions en JSON
     */
    private String permissionsToJson(Set<Permission> permissions) {
        if (permissions == null || permissions.isEmpty()) {
            return "[]";
        }
        
        List<String> permNames = permissions.stream()
            .map(Permission::name)
            .sorted()
            .toList();
        
        return "[\"" + String.join("\",\"", permNames) + "\"]";
    }
    
    /**
     * Convertit une chaîne JSON en Set de permissions
     */
    private Set<Permission> jsonToPermissions(String json) {
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
                System.err.println("Permission inconnue: " + permName);
            }
        }
        
        return permissions;
    }
}