package application.services;

import application.models.User;
import application.utils.SessionManager;

import java.net.InetAddress;
import java.sql.*;
import java.time.LocalDateTime;

/**
 * Service de gestion des logs d'activité - VERSION AMÉLIORÉE
 * Enregistre toutes les actions importantes des utilisateurs
 * AJOUT: Méthodes pour dossiers et documents avec signatures simplifiées
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
    
    // ==================== MÉTHODES DE CONNEXION ====================
    
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
    
    // ==================== MÉTHODES POUR DOCUMENTS (ORIGINALES) ====================
    
    /**
     * Enregistre la création d'un document (version avec ID)
     */
    public void logCreationDocument(int documentId, String codeDocument, String titre) {
        String details = String.format("Document créé - Code: %s - Titre: %s", codeDocument, titre);
        logAction("creation_document", details, "succes");
    }
    
    /**
     * Enregistre la modification d'un document (version avec ID)
     */
    public void logModificationDocument(int documentId, String codeDocument, String modifications) {
        String details = String.format("Document modifié - Code: %s - %s", codeDocument, modifications);
        logAction("modification_document", details, "info");
    }
    
    /**
     * Enregistre la suppression d'un document (version avec ID)
     */
    public void logSuppressionDocument(int documentId, String codeDocument) {
        String details = String.format("Document supprimé - Code: %s", codeDocument);
        logAction("suppression_document", details, "avertissement");
    }
    
    /**
     * Enregistre le déplacement d'un document (version avec 3 paramètres)
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
    
    // ==================== NOUVELLES MÉTHODES POUR DOCUMENTS (SIGNATURES SIMPLIFIÉES) ====================
    
    /**
     * Enregistre l'importation d'un document (signature simplifiée)
     * @param codeDocument Code du document
     * @param nomFichier Nom du fichier importé
     * @param nomUtilisateur Nom de l'utilisateur
     */
    public void logImportDocument(String codeDocument, String nomFichier, String nomUtilisateur) {
        String details = String.format("Document importé - Code: %s - Fichier: %s - Par: %s", 
            codeDocument, nomFichier, nomUtilisateur);
        logAction("import_document", details, "succes");
    }
    
    /**
     * Enregistre la modification d'un document (signature simplifiée à 2 paramètres)
     * @param codeDocument Code du document
     * @param nomUtilisateur Nom de l'utilisateur
     */
    public void logModificationDocument(String codeDocument, String nomUtilisateur) {
        String details = String.format("Document modifié - Code: %s - Par: %s", 
            codeDocument, nomUtilisateur);
        logAction("modification_document", details, "info");
    }
    
    /**
     * Enregistre la suppression d'un document (signature simplifiée)
     * @param codeDocument Code du document
     * @param nomUtilisateur Nom de l'utilisateur
     */
    public void logSuppressionDocument(String codeDocument, String nomUtilisateur) {
        String details = String.format("Document supprimé - Code: %s - Par: %s", 
            codeDocument, nomUtilisateur);
        logAction("suppression_document", details, "avertissement");
    }
    
    /**
     * Enregistre le déplacement d'un document (signature simplifiée à 2 paramètres)
     * @param codeDocument Code du document
     * @param nouveauDossier Nom du nouveau dossier
     */
    public void logDeplacementDocument(String codeDocument, String nouveauDossier) {
        String details = String.format("Document déplacé - Code: %s - Vers: %s", 
            codeDocument, nouveauDossier);
        logAction("deplacement_document", details, "info");
    }
    
    /**
     * Enregistre le téléchargement d'un document
     * @param codeDocument Code du document
     * @param nomUtilisateur Nom de l'utilisateur
     */
    public void logTelechargerDocument(String codeDocument, String nomUtilisateur) {
        String details = String.format("Document téléchargé - Code: %s - Par: %s", 
            codeDocument, nomUtilisateur);
        logAction("telechargement_document", details, "info");
    }
    
    // ==================== NOUVELLES MÉTHODES POUR DOSSIERS ====================
    
    /**
     * Enregistre la création d'un dossier
     * @param codeDossier Code du dossier
     * @param nomUtilisateur Nom de l'utilisateur
     */
    public void logCreationDossier(String codeDossier, String nomUtilisateur) {
        String details = String.format("Dossier créé - Code: %s - Par: %s", 
            codeDossier, nomUtilisateur);
        logAction("creation_dossier", details, "succes");
    }
    
    /**
     * Enregistre la modification d'un dossier
     * @param codeDossier Code du dossier
     * @param nomUtilisateur Nom de l'utilisateur
     */
    public void logModificationDossier(String codeDossier, String nomUtilisateur) {
        String details = String.format("Dossier modifié - Code: %s - Par: %s", 
            codeDossier, nomUtilisateur);
        logAction("modification_dossier", details, "info");
    }
    
    /**
     * Enregistre la suppression d'un dossier
     * @param codeDossier Code du dossier
     * @param nomUtilisateur Nom de l'utilisateur
     */
    public void logSuppressionDossier(String codeDossier, String nomUtilisateur) {
        String details = String.format("Dossier supprimé - Code: %s - Par: %s", 
            codeDossier, nomUtilisateur);
        logAction("suppression_dossier", details, "avertissement");
    }
    
    /**
     * Enregistre la création d'un sous-dossier
     * @param codeDossier Code du sous-dossier
     * @param codeDossierParent Code du dossier parent
     * @param nomUtilisateur Nom de l'utilisateur
     */
    public void logCreationSousDossier(String codeDossier, String codeDossierParent, String nomUtilisateur) {
        String details = String.format("Sous-dossier créé - Code: %s - Parent: %s - Par: %s", 
            codeDossier, codeDossierParent, nomUtilisateur);
        logAction("creation_sous_dossier", details, "succes");
    }
    
    // ==================== MÉTHODES GÉNÉRIQUES ====================
    
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
     * Enregistre une recherche
     * @param typeRecherche Type de recherche (document, dossier, etc.)
     * @param termeRecherche Terme recherché
     * @param nombreResultats Nombre de résultats trouvés
     */
    public void logRecherche(String typeRecherche, String termeRecherche, int nombreResultats) {
        String details = String.format("Recherche %s - Terme: '%s' - Résultats: %d", 
            typeRecherche, termeRecherche, nombreResultats);
        logAction("recherche", details, "info");
    }
    
    /**
     * Enregistre une exportation de données
     * @param typeExport Type d'export (Excel, PDF, etc.)
     * @param nombreElements Nombre d'éléments exportés
     */
    public void logExport(String typeExport, int nombreElements) {
        String details = String.format("Export %s - %d éléments", typeExport, nombreElements);
        logAction("export", details, "info");
    }
    
    // ==================== MÉTHODES PRIVÉES ====================
    
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
    
    // ==================== MÉTHODES DE CONSULTATION ====================
    
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
    
    /**
     * Compte le nombre de logs par utilisateur
     * @param userId ID de l'utilisateur
     * @return Nombre de logs
     */
    public int compterLogsUtilisateur(int userId) {
        String query = "SELECT COUNT(*) FROM logs_activite WHERE user_id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur comptage logs: " + e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * Compte le nombre de logs par type d'action
     * @param action Type d'action
     * @return Nombre de logs
     */
    public int compterLogsParAction(String action) {
        String query = "SELECT COUNT(*) FROM logs_activite WHERE action = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, action);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur comptage logs par action: " + e.getMessage());
        }
        
        return 0;
    }
}