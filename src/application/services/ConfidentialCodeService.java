package application.services;

import application.models.User;
import application.utils.SessionManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service ÉTENDU de gestion des codes confidentiels
 * VERSION ADMINISTRATION avec gestion complète
 */
public class ConfidentialCodeService {
    
    private static ConfidentialCodeService instance;
    private final DatabaseService databaseService;
    
    // Types d'actions
    public enum ActionType {
        ACCESS_DOSSIER("Accès au dossier confidentiel"),
        SAVE_DOCUMENT("Enregistrement document confidentiel"),
        READ_DOCUMENT("Lecture document confidentiel"),
        SAVE_COURRIER("Enregistrement courrier confidentiel");
        
        private final String description;
        
        ActionType(String description) {
            this.description = description;
        }
        
        public String getDescription() {
            return description;
        }
    }
    
    private ConfidentialCodeService() {
        this.databaseService = DatabaseService.getInstance();
    }
    
    public static synchronized ConfidentialCodeService getInstance() {
        if (instance == null) {
            instance = new ConfidentialCodeService();
        }
        return instance;
    }
    
    // ==================== MÉTHODES EXISTANTES ====================
    
    public boolean validateCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        
        if (!code.matches("^[a-zA-Z0-9]{8}$")) {
            System.err.println("❌ Format de code invalide (doit être 8 caractères alphanumériques)");
            return false;
        }
        
        try {
            String codeHash = hashCode(code);
            
            String query = "SELECT COUNT(*) FROM codes_confidentiels WHERE code_hash = ? AND actif = TRUE";
            
            try (Connection conn = databaseService.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                
                stmt.setString(1, codeHash);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        boolean isValid = rs.getInt(1) > 0;
                        
                        if (isValid) {
                            System.out.println("✅ Code confidentiel validé");
                        } else {
                            System.err.println("❌ Code confidentiel incorrect");
                        }
                        
                        return isValid;
                    }
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur validation code: " + e.getMessage());
        }
        
        return false;
    }
    
    public boolean checkAccessWithCode(ActionType actionType, String resourceType, 
                                        Integer resourceId, String code) {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        
        if (currentUser == null) {
            System.err.println("❌ Aucun utilisateur connecté");
            logAccess(null, actionType, resourceType, resourceId, false);
            return false;
        }
        
        boolean isValid = validateCode(code);
        logAccess(currentUser.getId(), actionType, resourceType, resourceId, isValid);
        
        return isValid;
    }
    
    private void logAccess(Integer userId, ActionType actionType, String resourceType, 
                          Integer resourceId, boolean success) {
        String query = "INSERT INTO acces_confidentiels_log " +
                      "(user_id, action, resource_type, resource_id, succes) " +
                      "VALUES (?, ?, ?, ?, ?)";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            if (userId != null) {
                stmt.setInt(1, userId);
            } else {
                stmt.setNull(1, Types.INTEGER);
            }
            
            stmt.setString(2, actionType.name());
            stmt.setString(3, resourceType);
            
            if (resourceId != null) {
                stmt.setInt(4, resourceId);
            } else {
                stmt.setNull(4, Types.INTEGER);
            }
            
            stmt.setBoolean(5, success);
            
            stmt.executeUpdate();
            
            if (success) {
                System.out.println("📝 Accès confidentiel autorisé: " + actionType.getDescription());
            } else {
                System.err.println("🚫 Accès confidentiel refusé: " + actionType.getDescription());
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur logging accès confidentiel: " + e.getMessage());
        }
    }
    
    public boolean changeCode(String ancienCode, String nouveauCode, String description) {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        
        if (currentUser == null || !currentUser.getRole().isAdmin()) {
            System.err.println("❌ Seuls les administrateurs peuvent changer le code confidentiel");
            return false;
        }
        
        if (!validateCode(ancienCode)) {
            System.err.println("❌ Ancien code incorrect");
            return false;
        }
        
        if (!nouveauCode.matches("^[a-zA-Z0-9]{8}$")) {
            System.err.println("❌ Le nouveau code doit contenir 8 caractères alphanumériques");
            return false;
        }
        
        try {
            String nouveauHash = hashCode(nouveauCode);
            
            String disableQuery = "UPDATE codes_confidentiels SET actif = FALSE";
            String insertQuery = "INSERT INTO codes_confidentiels (code_hash, description, cree_par, actif) " +
                                "VALUES (?, ?, ?, TRUE)";
            
            try (Connection conn = databaseService.getConnection()) {
                conn.setAutoCommit(false);
                
                try {
                    try (PreparedStatement stmt = conn.prepareStatement(disableQuery)) {
                        stmt.executeUpdate();
                    }
                    
                    try (PreparedStatement stmt = conn.prepareStatement(insertQuery)) {
                        stmt.setString(1, nouveauHash);
                        stmt.setString(2, description);
                        stmt.setInt(3, currentUser.getId());
                        stmt.executeUpdate();
                    }
                    
                    conn.commit();
                    
                    System.out.println("✅ Code confidentiel changé avec succès");
                    return true;
                    
                } catch (SQLException e) {
                    conn.rollback();
                    throw e;
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur changement code: " + e.getMessage());
            return false;
        }
    }
    
    private String hashCode(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(code.getBytes(StandardCharsets.UTF_8));
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            System.err.println("Erreur algorithme hash: " + e.getMessage());
            return null;
        }
    }
    
    public int getRecentFailedAttempts(int userId, int minutes) {
        String query = "SELECT COUNT(*) FROM acces_confidentiels_log " +
                      "WHERE user_id = ? AND succes = FALSE " +
                      "AND date_tentative > DATE_SUB(NOW(), INTERVAL ? MINUTE)";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, minutes);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération tentatives: " + e.getMessage());
        }
        
        return 0;
    }
    
    public boolean isUserBlocked(int userId) {
        int failedAttempts = getRecentFailedAttempts(userId, 15);
        
        if (failedAttempts >= 5) {
            System.err.println("⚠️ Utilisateur temporairement bloqué (trop de tentatives échouées)");
            return true;
        }
        
        return false;
    }
    
    // ==================== NOUVELLES MÉTHODES D'ADMINISTRATION ====================
    
    /**
     * Récupère le code confidentiel actif avec ses métadonnées
     * ADMIN UNIQUEMENT
     */
    public CodeInfo getCodeActuel() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        
        if (currentUser == null || !currentUser.getRole().isAdmin()) {
            System.err.println("❌ Accès refusé - Admin uniquement");
            return null;
        }
        
        String query = "SELECT c.*, u.code as user_code, u.nom, u.prenom, " +
                      "DATEDIFF(NOW(), c.date_creation) as age_jours " +
                      "FROM codes_confidentiels c " +
                      "LEFT JOIN users u ON c.cree_par = u.id " +
                      "WHERE c.actif = TRUE " +
                      "LIMIT 1";
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            if (rs.next()) {
                CodeInfo info = new CodeInfo();
                info.setId(rs.getInt("id"));
                info.setDescription(rs.getString("description"));
                info.setActif(rs.getBoolean("actif"));
                
                Timestamp dateCreation = rs.getTimestamp("date_creation");
                if (dateCreation != null) {
                    info.setDateCreation(dateCreation.toLocalDateTime());
                }
                
                String userCode = rs.getString("user_code");
                String nom = rs.getString("nom");
                String prenom = rs.getString("prenom");
                if (userCode != null) {
                    info.setCreateurNom(userCode + " - " + prenom + " " + nom);
                } else {
                    info.setCreateurNom("Système");
                }
                
                info.setAgeJours(rs.getInt("age_jours"));
                
                return info;
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération code actuel: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Récupère l'historique des codes confidentiels
     * ADMIN UNIQUEMENT
     */
    public List<CodeHistoryEntry> getHistoriqueCode() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        
        if (currentUser == null || !currentUser.getRole().isAdmin()) {
            System.err.println("❌ Accès refusé - Admin uniquement");
            return new ArrayList<>();
        }
        
        List<CodeHistoryEntry> historique = new ArrayList<>();
        
        String query = "SELECT c.*, u.code as user_code, u.nom, u.prenom, " +
                      "DATEDIFF(NOW(), c.date_creation) as age_jours " +
                      "FROM codes_confidentiels c " +
                      "LEFT JOIN users u ON c.cree_par = u.id " +
                      "ORDER BY c.date_creation DESC " +
                      "LIMIT 50";
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                CodeHistoryEntry entry = new CodeHistoryEntry();
                entry.setId(rs.getInt("id"));
                entry.setDescription(rs.getString("description"));
                entry.setActif(rs.getBoolean("actif"));
                
                Timestamp dateCreation = rs.getTimestamp("date_creation");
                if (dateCreation != null) {
                    entry.setDateCreation(dateCreation.toLocalDateTime());
                }
                
                String userCode = rs.getString("user_code");
                String nom = rs.getString("nom");
                String prenom = rs.getString("prenom");
                if (userCode != null) {
                    entry.setCreateurNom(userCode + " - " + prenom + " " + nom);
                } else {
                    entry.setCreateurNom("Système");
                }
                
                entry.setAgeJours(rs.getInt("age_jours"));
                
                historique.add(entry);
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération historique: " + e.getMessage());
        }
        
        return historique;
    }
    
    /**
     * Récupère les statistiques d'accès aux ressources confidentielles
     * ADMIN UNIQUEMENT
     */
    public AccessStatistics getStatistiquesAcces() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        
        if (currentUser == null || !currentUser.getRole().isAdmin()) {
            System.err.println("❌ Accès refusé - Admin uniquement");
            return null;
        }
        
        AccessStatistics stats = new AccessStatistics();
        
        String query = "SELECT " +
                      "COUNT(*) as total_tentatives, " +
                      "SUM(CASE WHEN succes = TRUE THEN 1 ELSE 0 END) as acces_reussis, " +
                      "SUM(CASE WHEN succes = FALSE THEN 1 ELSE 0 END) as acces_refuses, " +
                      "COUNT(DISTINCT user_id) as utilisateurs_distincts " +
                      "FROM acces_confidentiels_log";
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            if (rs.next()) {
                stats.setTotalTentatives(rs.getInt("total_tentatives"));
                stats.setAccesReussis(rs.getInt("acces_reussis"));
                stats.setAccesRefuses(rs.getInt("acces_refuses"));
                stats.setUtilisateursDistincts(rs.getInt("utilisateurs_distincts"));
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération statistiques: " + e.getMessage());
        }
        
        // Compter les codes actifs et total
        String queryCode = "SELECT " +
                          "COUNT(*) as total_codes, " +
                          "SUM(CASE WHEN actif = TRUE THEN 1 ELSE 0 END) as codes_actifs " +
                          "FROM codes_confidentiels";
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(queryCode)) {
            
            if (rs.next()) {
                stats.setTotalCodes(rs.getInt("total_codes"));
                stats.setCodesActifs(rs.getInt("codes_actifs"));
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération stats codes: " + e.getMessage());
        }
        
        return stats;
    }
    
    /**
     * Définit le responsable des courriers confidentiels
     * ADMIN UNIQUEMENT
     */
    public boolean setResponsableCourrierConfidentiel(int userId) {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        
        if (currentUser == null || !currentUser.getRole().isAdmin()) {
            System.err.println("❌ Accès refusé - Admin uniquement");
            return false;
        }
        
        String query = "INSERT INTO config_responsable_courrier (service_code, user_id, actif) " +
                      "VALUES ('CONFIDENTIEL', ?, TRUE) " +
                      "ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), actif = TRUE, date_modification = NOW()";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, userId);
            stmt.executeUpdate();
            
            System.out.println("✅ Responsable courriers confidentiels défini: user_id=" + userId);
            return true;
            
        } catch (SQLException e) {
            System.err.println("Erreur définition responsable: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Récupère le responsable des courriers confidentiels
     * ADMIN UNIQUEMENT
     */
    public User getResponsableCourrierConfidentiel() throws SQLException {
        String query = """
            SELECT u.*, 
			       r.nom as role_nom, 
			       r.description as role_desc, 
			       r.permissions, 
			       r.actif as role_actif
			FROM config_responsable_courrier crc
			JOIN users u ON crc.user_id = u.id
			LEFT JOIN roles r ON u.role_id = r.id
			WHERE crc.service_code = 'CONFIDENTIEL' 
			AND crc.actif = 1
			LIMIT 1;
        """;
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query);
             ResultSet rs = stmt.executeQuery()) {
            
            if (rs.next()) {
                return databaseService.mapResultSetToUser(rs);
            }
        }
        
        return null;
    }

    
    /**
     * Supprime le responsable des courriers confidentiels
     * ADMIN UNIQUEMENT
     */
    public boolean supprimerResponsableCourrierConfidentiel() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        
        if (currentUser == null || !currentUser.getRole().isAdmin()) {
            System.err.println("❌ Accès refusé - Admin uniquement");
            return false;
        }
        
        String query = "UPDATE config_responsable_courrier SET actif = FALSE " +
                      "WHERE service_code = 'CONFIDENTIEL'";
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement()) {
            
            stmt.executeUpdate(query);
            
            System.out.println("✅ Responsable courriers confidentiels supprimé");
            return true;
            
        } catch (SQLException e) {
            System.err.println("Erreur suppression responsable: " + e.getMessage());
            return false;
        }
    }
    
    // ==================== CLASSES INTERNES ====================
    
    /**
     * Informations sur le code actif
     */
    public static class CodeInfo {
        private int id;
        private String description;
        private boolean actif;
        private LocalDateTime dateCreation;
        private String createurNom;
        private int ageJours;
        
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public boolean isActif() { return actif; }
        public void setActif(boolean actif) { this.actif = actif; }
        
        public LocalDateTime getDateCreation() { return dateCreation; }
        public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
        
        public String getCreateurNom() { return createurNom; }
        public void setCreateurNom(String createurNom) { this.createurNom = createurNom; }
        
        public int getAgeJours() { return ageJours; }
        public void setAgeJours(int ageJours) { this.ageJours = ageJours; }
    }
    
    /**
     * Entrée d'historique de code
     */
    public static class CodeHistoryEntry {
        private int id;
        private String description;
        private boolean actif;
        private LocalDateTime dateCreation;
        private String createurNom;
        private int ageJours;
        
        public int getId() { return id; }
        public void setId(int id) { this.id = id; }
        
        public String getDescription() { return description; }
        public void setDescription(String description) { this.description = description; }
        
        public boolean isActif() { return actif; }
        public void setActif(boolean actif) { this.actif = actif; }
        
        public LocalDateTime getDateCreation() { return dateCreation; }
        public void setDateCreation(LocalDateTime dateCreation) { this.dateCreation = dateCreation; }
        
        public String getCreateurNom() { return createurNom; }
        public void setCreateurNom(String createurNom) { this.createurNom = createurNom; }
        
        public int getAgeJours() { return ageJours; }
        public void setAgeJours(int ageJours) { this.ageJours = ageJours; }
    }
    
    /**
     * Statistiques d'accès
     */
    public static class AccessStatistics {
        private int totalTentatives;
        private int accesReussis;
        private int accesRefuses;
        private int utilisateursDistincts;
        private int totalCodes;
        private int codesActifs;
        
        public int getTotalTentatives() { return totalTentatives; }
        public void setTotalTentatives(int totalTentatives) { this.totalTentatives = totalTentatives; }
        
        public int getAccesReussis() { return accesReussis; }
        public void setAccesReussis(int accesReussis) { this.accesReussis = accesReussis; }
        
        public int getAccesRefuses() { return accesRefuses; }
        public void setAccesRefuses(int accesRefuses) { this.accesRefuses = accesRefuses; }
        
        public int getUtilisateursDistincts() { return utilisateursDistincts; }
        public void setUtilisateursDistincts(int utilisateursDistincts) { this.utilisateursDistincts = utilisateursDistincts; }
        
        public int getTotalCodes() { return totalCodes; }
        public void setTotalCodes(int totalCodes) { this.totalCodes = totalCodes; }
        
        public int getCodesActifs() { return codesActifs; }
        public void setCodesActifs(int codesActifs) { this.codesActifs = codesActifs; }
    }
}