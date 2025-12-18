package application.services;

import application.models.*;
import application.services.NetworkService.WorkflowUpdateListener;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * Service de synchronisation pour les réunions en temps réel
 * Gère la synchronisation entre instances via multicast
 */
public class ReunionSyncService implements WorkflowUpdateListener {
    
    private static ReunionSyncService instance;
    private final DatabaseService databaseService;
    private final NetworkService networkService;
    private final ReunionService reunionService;
    
    // Listeners pour les mises à jour de réunions
    private final List<ReunionListener> reunionListeners;
    
    // Cache pour éviter les duplications
    private final Map<String, LocalDateTime> reunionSyncCache;
    private final ScheduledExecutorService scheduler;
    
    private ReunionSyncService() {
        this.databaseService = DatabaseService.getInstance();
        this.networkService = NetworkService.getInstance();
        this.reunionService = ReunionService.getInstance();
        this.reunionListeners = new CopyOnWriteArrayList<>();
        this.reunionSyncCache = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(1);
        
        initialize();
    }
    
    public static synchronized ReunionSyncService getInstance() {
        if (instance == null) {
            instance = new ReunionSyncService();
        }
        return instance;
    }
    
    /**
     * Initialise le service
     */
    private void initialize() {
        // Enregistrer comme listener du NetworkService
        networkService.addWorkflowUpdateListener(this);
        
        // Démarrer le nettoyage périodique du cache
        scheduler.scheduleAtFixedRate(this::cleanupCache, 5, 5, TimeUnit.MINUTES);
        
        // Démarrer les rappels automatiques
        scheduler.scheduleAtFixedRate(this::checkUpcomingReunions, 1, 1, TimeUnit.MINUTES);
        
        System.out.println("✅ ReunionSyncService initialisé");
    }
    
    // ==================== GESTION DES RÉUNIONS ====================
    
    /**
     * Crée une réunion et la synchronise
     */
    public boolean creerReunion(Reunion reunion, List<User> participants) {
        try {
            // Sauvegarder la réunion
            if (reunionService.saveReunion(reunion)) {
                // Ajouter les participants
                for (User participant : participants) {
                    ajouterParticipant(reunion.getId(), participant.getId());
                }
                
                // Notifier les autres instances
                notifyReunionCreated(reunion.getId());
                
                // Notifier les listeners locaux
                notifyListeners(reunion, "CREATED");
                
                // Envoyer des notifications aux participants
                envoyerNotificationsParticipants(reunion, participants, "invited");
                
                return true;
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la création de la réunion: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Met à jour une réunion et synchronise
     */
    public boolean mettreAJourReunion(Reunion reunion) {
        try {
            if (reunionService.saveReunion(reunion)) {
                // Notifier les autres instances
                notifyReunionUpdated(reunion.getId());
                
                // Notifier les listeners locaux
                notifyListeners(reunion, "UPDATED");
                
                return true;
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la mise à jour de la réunion: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Supprime une réunion et synchronise
     */
    public boolean supprimerReunion(int reunionId) {
        try {
            if (reunionService.deleteReunion(reunionId)) {
                // Notifier les autres instances
                notifyReunionDeleted(reunionId);
                
                return true;
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la suppression de la réunion: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Démarre une réunion et synchronise
     */
    public boolean demarrerReunion(Reunion reunion) {
        try {
            reunion.setStatut(StatutReunion.EN_COURS);
            
            if (reunionService.saveReunion(reunion)) {
                // Notifier les autres instances
                notifyReunionStarted(reunion.getId());
                
                // Notifier les listeners
                notifyListeners(reunion, "STARTED");
                
                // Envoyer des notifications
                List<User> participants = getParticipants(reunion.getId());
                envoyerNotificationsParticipants(reunion, participants, "started");
                
                return true;
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du démarrage de la réunion: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Termine une réunion et synchronise
     */
    public boolean terminerReunion(Reunion reunion) {
        try {
            reunion.setStatut(StatutReunion.TERMINEE);
            
            if (reunionService.saveReunion(reunion)) {
                // Notifier les autres instances
                notifyReunionEnded(reunion.getId());
                
                // Notifier les listeners
                notifyListeners(reunion, "ENDED");
                
                return true;
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la terminaison de la réunion: " + e.getMessage());
        }
        
        return false;
    }
    
    // ==================== GESTION DES PARTICIPANTS ====================
    
    /**
     * Ajoute un participant à une réunion
     */
    public boolean ajouterParticipant(int reunionId, int userId) {
        try {
            String query = """
                INSERT INTO reunion_participants (reunion_id, user_id, statut_participation)
                VALUES (?, ?, 'invite')
                ON DUPLICATE KEY UPDATE statut_participation = statut_participation
            """;
            
            try (Connection conn = databaseService.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                
                stmt.setInt(1, reunionId);
                stmt.setInt(2, userId);
                
                if (stmt.executeUpdate() > 0) {
                    // Notifier les autres instances
                    notifyParticipantAdded(reunionId, userId);
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout du participant: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Confirme la participation d'un utilisateur
     */
    public boolean confirmerParticipation(int reunionId, int userId) {
        return updateStatutParticipation(reunionId, userId, "confirme");
    }
    
    /**
     * Décline la participation d'un utilisateur
     */
    public boolean declinerParticipation(int reunionId, int userId, String commentaire) {
        boolean result = updateStatutParticipation(reunionId, userId, "decline");
        
        if (result && commentaire != null) {
            // Sauvegarder le commentaire
            try {
                String query = "UPDATE reunion_participants SET commentaire = ? WHERE reunion_id = ? AND user_id = ?";
                
                try (Connection conn = databaseService.getConnection();
                     PreparedStatement stmt = conn.prepareStatement(query)) {
                    
                    stmt.setString(1, commentaire);
                    stmt.setInt(2, reunionId);
                    stmt.setInt(3, userId);
                    stmt.executeUpdate();
                }
            } catch (SQLException e) {
                System.err.println("Erreur lors de la sauvegarde du commentaire: " + e.getMessage());
            }
        }
        
        return result;
    }
    
    /**
     * Met à jour le statut de participation
     */
    private boolean updateStatutParticipation(int reunionId, int userId, String statut) {
        try {
            String query = """
                UPDATE reunion_participants 
                SET statut_participation = ?, date_reponse = CURRENT_TIMESTAMP 
                WHERE reunion_id = ? AND user_id = ?
            """;
            
            try (Connection conn = databaseService.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                
                stmt.setString(1, statut);
                stmt.setInt(2, reunionId);
                stmt.setInt(3, userId);
                
                if (stmt.executeUpdate() > 0) {
                    // Notifier les autres instances
                    notifyParticipationStatusChanged(reunionId, userId, statut);
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour du statut: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Récupère les participants d'une réunion
     */
    public List<User> getParticipants(int reunionId) {
        List<User> participants = new ArrayList<>();
        
        try {
            String query = """
                SELECT u.* FROM users u
                INNER JOIN reunion_participants rp ON u.id = rp.user_id
                WHERE rp.reunion_id = ?
            """;
            
            try (Connection conn = databaseService.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                
                stmt.setInt(1, reunionId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        // Mapper vers User (méthode simplifiée)
                        User user = new User();
                        user.setId(rs.getInt("id"));
                        user.setCode(rs.getString("code"));
                        user.setNom(rs.getString("nom"));
                        user.setPrenom(rs.getString("prenom"));
                        user.setEmail(rs.getString("email"));
                        participants.add(user);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des participants: " + e.getMessage());
        }
        
        return participants;
    }
    
    /**
     * Récupère les participants avec leur statut
     */
    public List<ReunionParticipant> getParticipantsAvecStatut(int reunionId) {
        List<ReunionParticipant> participants = new ArrayList<>();
        
        try {
            String query = """
                SELECT rp.*, u.* FROM reunion_participants rp
                INNER JOIN users u ON rp.user_id = u.id
                WHERE rp.reunion_id = ?
            """;
            
            try (Connection conn = databaseService.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                
                stmt.setInt(1, reunionId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        ReunionParticipant participant = new ReunionParticipant();
                        participant.setId(rs.getInt("id"));
                        
                        User user = new User();
                        user.setId(rs.getInt("user_id"));
                        user.setNom(rs.getString("nom"));
                        user.setPrenom(rs.getString("prenom"));
                        user.setEmail(rs.getString("email"));
                        participant.setUser(user);
                        
                        String statut = rs.getString("statut_participation");
                        participant.setStatutParticipation(
                            ReunionParticipant.StatutParticipation.valueOf(statut.toUpperCase())
                        );
                        
                        Timestamp dateReponse = rs.getTimestamp("date_reponse");
                        if (dateReponse != null) {
                            participant.setDateReponse(dateReponse.toLocalDateTime());
                        }
                        
                        participant.setCommentaire(rs.getString("commentaire"));
                        
                        participants.add(participant);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des participants: " + e.getMessage());
        }
        
        return participants;
    }
    
    // ==================== NOTIFICATIONS ====================
    
    /**
     * Envoie des notifications aux participants
     */
    private void envoyerNotificationsParticipants(Reunion reunion, List<User> participants, String type) {
        for (User participant : participants) {
            try {
                String titre = "";
                String contenu = "";
                
                switch (type) {
                    case "invited":
                        titre = "Invitation à une réunion";
                        contenu = "Vous êtes invité à la réunion: " + reunion.getTitre();
                        break;
                    case "started":
                        titre = "Réunion démarrée";
                        contenu = "La réunion '" + reunion.getTitre() + "' a démarré";
                        break;
                    case "updated":
                        titre = "Réunion modifiée";
                        contenu = "La réunion '" + reunion.getTitre() + "' a été modifiée";
                        break;
                }
                
                creerNotification(participant.getId(), "reunion", titre, contenu, reunion.getId());
            } catch (Exception e) {
                System.err.println("Erreur lors de l'envoi de notification: " + e.getMessage());
            }
        }
    }
    
    /**
     * Crée une notification
     */
    private void creerNotification(int userId, String type, String titre, String contenu, int referenceId) {
        try {
            String query = """
                INSERT INTO notifications (user_id, type_notification, titre, contenu, reference_id, reference_type)
                VALUES (?, ?, ?, ?, ?, 'reunion')
            """;
            
            try (Connection conn = databaseService.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                
                stmt.setInt(1, userId);
                stmt.setString(2, type);
                stmt.setString(3, titre);
                stmt.setString(4, contenu);
                stmt.setInt(5, referenceId);
                
                stmt.executeUpdate();
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la création de notification: " + e.getMessage());
        }
    }
    
    // ==================== NOTIFICATIONS RÉSEAU ====================
    
    private void notifyReunionCreated(int reunionId) {
        sendMulticastMessage("REUNION_CREATED|" + reunionId);
    }
    
    private void notifyReunionUpdated(int reunionId) {
        sendMulticastMessage("REUNION_UPDATED|" + reunionId);
    }
    
    private void notifyReunionDeleted(int reunionId) {
        sendMulticastMessage("REUNION_DELETED|" + reunionId);
    }
    
    private void notifyReunionStarted(int reunionId) {
        sendMulticastMessage("REUNION_STARTED|" + reunionId);
    }
    
    private void notifyReunionEnded(int reunionId) {
        sendMulticastMessage("REUNION_ENDED|" + reunionId);
    }
    
    private void notifyParticipantAdded(int reunionId, int userId) {
        sendMulticastMessage("REUNION_PARTICIPANT_ADDED|" + reunionId + "|" + userId);
    }
    
    private void notifyParticipationStatusChanged(int reunionId, int userId, String statut) {
        sendMulticastMessage("REUNION_PARTICIPATION_STATUS|" + reunionId + "|" + userId + "|" + statut);
    }
    
    private void sendMulticastMessage(String message) {
        System.out.println("📡 Réunion sync: " + message);
        // Le NetworkService gérera l'envoi réel
    }
    
    // ==================== VÉRIFICATION DES RÉUNIONS À VENIR ====================
    
    /**
     * Vérifie les réunions à venir et envoie des rappels
     */
    private void checkUpcomingReunions() {
        try {
            // Réunions qui commencent dans les 15 prochaines minutes
            String query = """
                SELECT * FROM reunions 
                WHERE statut = 'programmee' 
                AND date_reunion BETWEEN NOW() AND DATE_ADD(NOW(), INTERVAL 15 MINUTE)
            """;
            
            try (Connection conn = databaseService.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                
                while (rs.next()) {
                    int reunionId = rs.getInt("id");
                    String titre = rs.getString("titre");
                    
                    // Envoyer des rappels aux participants
                    List<User> participants = getParticipants(reunionId);
                    for (User participant : participants) {
                        creerNotification(
                            participant.getId(),
                            "reunion",
                            "Rappel de réunion",
                            "La réunion '" + titre + "' commence dans moins de 15 minutes",
                            reunionId
                        );
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification des réunions: " + e.getMessage());
        }
    }
    
    // ==================== LISTENERS ====================
    
    @Override
    public void onWorkflowUpdate(int courrierId, String serviceCode) {
        // Ne rien faire
    }
    
    @Override
    public void onWorkflowComplete(int courrierId) {
        // Ne rien faire
    }
    
    @Override
    public void onRefreshRequest() {
        notifyListenersRefresh();
    }
    
    public void addReunionListener(ReunionListener listener) {
        if (listener != null && !reunionListeners.contains(listener)) {
            reunionListeners.add(listener);
        }
    }
    
    public void removeReunionListener(ReunionListener listener) {
        reunionListeners.remove(listener);
    }
    
    private void notifyListeners(Reunion reunion, String action) {
        for (ReunionListener listener : reunionListeners) {
            try {
                listener.onReunionChanged(reunion, action);
            } catch (Exception e) {
                System.err.println("Erreur lors de la notification du listener: " + e.getMessage());
            }
        }
    }
    
    private void notifyListenersRefresh() {
        for (ReunionListener listener : reunionListeners) {
            try {
                listener.onRefreshRequest();
            } catch (Exception e) {
                System.err.println("Erreur lors de la notification du listener: " + e.getMessage());
            }
        }
    }
    
    // ==================== UTILITAIRES ====================
    
    private void cleanupCache() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);
        reunionSyncCache.entrySet().removeIf(entry -> entry.getValue().isBefore(threshold));
    }
    
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
        
        System.out.println("✅ ReunionSyncService arrêté");
    }
    
    /**
     * Interface pour écouter les changements de réunions
     */
    public interface ReunionListener {
        void onReunionChanged(Reunion reunion, String action);
        void onRefreshRequest();
    }
}