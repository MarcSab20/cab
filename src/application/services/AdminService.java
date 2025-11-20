package application.services;

import application.models.User;
import application.models.Role;
import application.models.Permission;
import application.controllers.AdminController;
import application.utils.PasswordUtils;

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
    
    public List<User> getTousUtilisateurs() {
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
    
    public boolean creerUtilisateur(User user) {
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
    
    public boolean modifierUtilisateur(User user) {
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
    
    public boolean supprimerUtilisateur(User user) {
        String query = "DELETE FROM users WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, user.getId());
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Erreur suppression utilisateur: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
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
    
    public List<Role> getTousRoles() {
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
    
    public boolean creerRole(Role role) {
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
    
    public boolean modifierRole(Role role) {
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
    
    public boolean supprimerRole(Role role) {
        String query = "DELETE FROM roles WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, role.getId());
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
    
    public List<LogActivite> getLogsRecents(int limit) {
        return getLogs(null, null, null, null, limit);
    }
    
    public List<LogActivite> getLogs(LocalDateTime debut, LocalDateTime fin, 
                                     String typeAction, String utilisateur) {
        return getLogs(debut, fin, typeAction, utilisateur, -1);
    }
    
    public List<LogActivite> getLogs(LocalDateTime debut, LocalDateTime fin, 
                                     String typeAction, String utilisateur, int limit) {
        List<LogActivite> logsList = new ArrayList<>();
        
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
            query.append(" AND l.action = ?");
            params.add(typeAction);
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
                LogActivite log = new LogActivite(
                    formatTimestamp(rs.getTimestamp("timestamp")),
                    rs.getString("user_code"),
                    rs.getString("action"),
                    rs.getString("details"),
                    rs.getString("ip_address"),
                    "Succès"
                );
                logsList.add(log);
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération logs: " + e.getMessage());
            e.printStackTrace();
        }
        
        return logsList;
    }
    
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
    // MÉTHODES UTILITAIRES
    // ========================================
    
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
    
    private String formatTimestamp(Timestamp timestamp) {
        if (timestamp == null) return "";
        
        LocalDateTime dateTime = timestamp.toLocalDateTime();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
        return dateTime.format(formatter);
    }
}