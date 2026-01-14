package application.services;

import application.models.User;
import application.utils.SessionManager;

import java.net.InetAddress;
import java.sql.*;
import java.time.LocalDateTime;

/**
 * Service de gestion des logs d'activité
 * Enregistre toutes les actions importantes des utilisateurs
 */
public class LogService {
    
    private static LogService instance;
    private final DatabaseService databaseService;
    
    private LogService() {
        this.databaseService = DatabaseService.getInstance();
    }
    
    public static synchronized LogService getInstance() {
        if (instance == null) {
            instance = new LogService();
        }
        return instance;
    }
    
    /**
     * Enregistre une action utilisateur
     */
    public void logAction(String action, String details) {
        logAction(action, details, "info");
    }
    
    /**
     * Enregistre une action utilisateur avec un statut spécifique
     */
    public void logAction(String action, String details, String statut) {
        try {
            User currentUser = SessionManager.getInstance().getCurrentUser();
            Integer userId = currentUser != null ? currentUser.getId() : null;
            
            String ipAddress = getLocalIPAddress();
            
            insertLog(userId, action, details, ipAddress, statut);
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'enregistrement du log: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Enregistre une connexion réussie
     */
    public void logConnexion(User user) {
        try {
            String ipAddress = getLocalIPAddress();
            String details = "Connexion réussie - Rôle: " + user.getRole().getNom();
            
            insertLog(user.getId(), "connexion", details, ipAddress, "succes");
            
            System.out.println("✓ Connexion enregistrée pour: " + user.getCode());
            
        } catch (Exception e) {
            System.err.println("Erreur log connexion: " + e.getMessage());
        }
    }
    
    /**
     * Enregistre une tentative de connexion échouée
     */
    public void logTentativeConnexionEchouee(String code, String raison) {
        try {
            String ipAddress = getLocalIPAddress();
            String details = "Tentative échouée pour l'utilisateur: " + code + " - Raison: " + raison;
            
            insertLog(null, "tentative_connexion", details, ipAddress, "echec");
            
            System.out.println("✗ Tentative de connexion échouée enregistrée: " + code);
            
        } catch (Exception e) {
            System.err.println("Erreur log tentative: " + e.getMessage());
        }
    }
    
    /**
     * Enregistre une déconnexion
     */
    public void logDeconnexion(User user) {
        try {
            String ipAddress = getLocalIPAddress();
            String details = "Déconnexion - Session terminée";
            
            insertLog(user.getId(), "deconnexion", details, ipAddress, "info");
            
            System.out.println("✓ Déconnexion enregistrée pour: " + user.getCode());
            
        } catch (Exception e) {
            System.err.println("Erreur log déconnexion: " + e.getMessage());
        }
    }
    
    /**
     * Enregistre la création d'un document
     */
    public void logCreationDocument(int documentId, String codeDocument, String titre) {
        String details = String.format("Document créé - Code: %s - Titre: %s", codeDocument, titre);
        logAction("creation_document", details, "succes");
    }
    
    /**
     * Enregistre la modification d'un document
     */
    public void logModificationDocument(int documentId, String codeDocument, String modifications) {
        String details = String.format("Document modifié - Code: %s - %s", codeDocument, modifications);
        logAction("modification_document", details, "info");
    }
    
    /**
     * Enregistre la suppression d'un document
     */
    public void logSuppressionDocument(int documentId, String codeDocument) {
        String details = String.format("Document supprimé - Code: %s", codeDocument);
        logAction("suppression_document", details, "avertissement");
    }
    
    /**
     * Enregistre le déplacement d'un document
     */
    public void logDeplacementDocument(String codeDocument, String ancienDossier, String nouveauDossier) {
        String details = String.format("Document déplacé - Code: %s - De: %s vers: %s", 
            codeDocument, ancienDossier, nouveauDossier);
        logAction("deplacement_document", details, "info");
    }
    
    /**
     * Enregistre le partage d'un document
     */
    public void logPartageDocument(String codeDocument, int nombreUtilisateurs) {
        String details = String.format("Document partagé - Code: %s - Avec: %d utilisateurs", 
            codeDocument, nombreUtilisateurs);
        logAction("partage_document", details, "info");
    }
    
    /**
     * Enregistre une erreur système
     */
    public void logErreur(String action, String messageErreur) {
        logAction(action, "Erreur: " + messageErreur, "erreur");
    }
    
    /**
     * Enregistre un accès refusé
     */
    public void logAccesRefuse(String ressource, String raison) {
        String details = String.format("Accès refusé - Ressource: %s - Raison: %s", ressource, raison);
        logAction("acces_refuse", details, "avertissement");
    }
    
    /**
     * Insère un log dans la base de données
     */
    private void insertLog(Integer userId, String action, String details, String ipAddress, String statut) {
        String query = "INSERT INTO logs_activite (user_id, action, details, ip_address, statut, timestamp) " +
                      "VALUES (?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            if (userId != null) {
                stmt.setInt(1, userId);
            } else {
                stmt.setNull(1, Types.INTEGER);
            }
            
            stmt.setString(2, action);
            stmt.setString(3, details);
            stmt.setString(4, ipAddress);
            stmt.setString(5, statut);
            stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Erreur insertion log: " + e.getMessage());
            // Ne pas propager l'erreur pour ne pas bloquer l'application
        }
    }
    
    /**
     * Récupère l'adresse IP locale
     */
    private String getLocalIPAddress() {
        try {
            InetAddress inetAddress = InetAddress.getLocalHost();
            return inetAddress.getHostAddress();
        } catch (Exception e) {
            return "0.0.0.0";
        }
    }
    
    /**
     * Récupère les logs d'un utilisateur
     */
    public void getLogs_ForUser(int userId, int limit) {
        // Méthode pour récupérer les logs d'un utilisateur spécifique
        // À implémenter selon les besoins
    }
    
    /**
     * Récupère les logs par type d'action
     */
    public void getLogsByAction(String action, int limit) {
        // Méthode pour récupérer les logs par type d'action
        // À implémenter selon les besoins
    }
    
    /**
     * Nettoie les anciens logs
     */
    public int nettoyerLogsAnciens(int nombreJours) {
        String query = "DELETE FROM logs_activite WHERE timestamp < DATE_SUB(NOW(), INTERVAL ? DAY)";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, nombreJours);
            int deleted = stmt.executeUpdate();
            
            System.out.println("✓ " + deleted + " logs supprimés (> " + nombreJours + " jours)");
            return deleted;
            
        } catch (SQLException e) {
            System.err.println("Erreur nettoyage logs: " + e.getMessage());
            return 0;
        }
    }
}