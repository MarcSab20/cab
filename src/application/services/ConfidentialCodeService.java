package application.services;

import application.models.User;
import application.utils.SessionManager;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.sql.*;
import java.util.Base64;

/**
 * Service de gestion des codes confidentiels
 * Gère la validation et le logging des accès aux ressources confidentielles
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
    
    /**
     * Valide un code confidentiel
     * @param code Code saisi par l'utilisateur
     * @return true si le code est valide
     */
    public boolean validateCode(String code) {
        if (code == null || code.trim().isEmpty()) {
            return false;
        }
        
        // Validation du format (8 caractères alphanumériques)
        if (!code.matches("^[a-zA-Z0-9]{8}$")) {
            System.err.println("❌ Format de code invalide (doit être 8 caractères alphanumériques)");
            return false;
        }
        
        try {
            // Calculer le hash du code saisi
            String codeHash = hashCode(code);
            
            // Vérifier dans la base de données
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
    
    /**
     * Vérifie l'accès et demande le code si nécessaire
     * @param actionType Type d'action à effectuer
     * @param resourceType Type de ressource (dossier, document, courrier)
     * @param resourceId ID de la ressource
     * @param code Code confidentiel saisi
     * @return true si l'accès est autorisé
     */
    public boolean checkAccessWithCode(ActionType actionType, String resourceType, 
                                        Integer resourceId, String code) {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        
        if (currentUser == null) {
            System.err.println("❌ Aucun utilisateur connecté");
            logAccess(null, actionType, resourceType, resourceId, false);
            return false;
        }
        
        // Valider le code
        boolean isValid = validateCode(code);
        
        // Logger la tentative
        logAccess(currentUser.getId(), actionType, resourceType, resourceId, isValid);
        
        return isValid;
    }
    
    /**
     * Enregistre une tentative d'accès confidentiel
     */
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
    
    /**
     * Change le code confidentiel (administrateurs uniquement)
     * @param ancienCode Ancien code
     * @param nouveauCode Nouveau code (8 caractères)
     * @param description Description du nouveau code
     * @return true si le changement a réussi
     */
    public boolean changeCode(String ancienCode, String nouveauCode, String description) {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        
        // Vérifier les permissions (admin uniquement)
        if (currentUser == null || !currentUser.getRole().isAdmin()) {
            System.err.println("❌ Seuls les administrateurs peuvent changer le code confidentiel");
            return false;
        }
        
        // Valider l'ancien code
        if (!validateCode(ancienCode)) {
            System.err.println("❌ Ancien code incorrect");
            return false;
        }
        
        // Valider le format du nouveau code
        if (!nouveauCode.matches("^[a-zA-Z0-9]{8}$")) {
            System.err.println("❌ Le nouveau code doit contenir 8 caractères alphanumériques");
            return false;
        }
        
        try {
            String nouveauHash = hashCode(nouveauCode);
            
            // Désactiver tous les anciens codes
            String disableQuery = "UPDATE codes_confidentiels SET actif = FALSE";
            
            // Insérer le nouveau code
            String insertQuery = "INSERT INTO codes_confidentiels (code_hash, description, cree_par, actif) " +
                                "VALUES (?, ?, ?, TRUE)";
            
            try (Connection conn = databaseService.getConnection()) {
                conn.setAutoCommit(false);
                
                try {
                    // Désactiver les anciens codes
                    try (PreparedStatement stmt = conn.prepareStatement(disableQuery)) {
                        stmt.executeUpdate();
                    }
                    
                    // Insérer le nouveau code
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
    
    /**
     * Calcule le hash SHA-256 d'un code
     */
    private String hashCode(String code) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(code.getBytes(StandardCharsets.UTF_8));
            
            // Convertir en hexadécimal
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
    
    /**
     * Récupère le nombre de tentatives échouées récentes pour un utilisateur
     * @param userId ID de l'utilisateur
     * @param minutes Nombre de minutes à considérer
     * @return Nombre de tentatives échouées
     */
    public int getRecentFailedAttempts(int userId, int minutes) {
        String query = "SELECT COUNT(*) FROM acces_confidentiels_log " +
                      "WHERE user_id = ? AND succes = FALSE " +
                      "AND date_tentative > NOW() - INTERVAL '" + minutes + " minutes'";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, userId);
            
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
    
    /**
     * Vérifie si un utilisateur est temporairement bloqué (trop de tentatives)
     * @param userId ID de l'utilisateur
     * @return true si l'utilisateur est bloqué
     */
    public boolean isUserBlocked(int userId) {
        int failedAttempts = getRecentFailedAttempts(userId, 15); // 15 minutes
        
        // Bloquer après 5 tentatives échouées
        if (failedAttempts >= 5) {
            System.err.println("⚠️ Utilisateur temporairement bloqué (trop de tentatives échouées)");
            return true;
        }
        
        return false;
    }
}