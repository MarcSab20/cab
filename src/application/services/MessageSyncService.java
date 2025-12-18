package application.services;

import application.models.*;
import application.services.NetworkService.WorkflowUpdateListener;
import application.utils.AlertUtils;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

/**
 * Service de synchronisation pour la messagerie en temps réel
 * Gère la synchronisation entre instances via multicast
 */
public class MessageSyncService implements WorkflowUpdateListener {
    
    private static MessageSyncService instance;
    private final DatabaseService databaseService;
    private final NetworkService networkService;
    private final MessageService messageService;
    
    // Cache des messages récents pour éviter la duplication
    private final Map<String, LocalDateTime> messageSyncCache;
    private final ScheduledExecutorService scheduler;
    
    // Listeners pour les nouveaux messages
    private final List<MessageListener> messageListeners;
    
    // Statut de présence des utilisateurs
    private final Map<Integer, UserPresence> userPresenceMap;
    
    private MessageSyncService() {
        this.databaseService = DatabaseService.getInstance();
        this.networkService = NetworkService.getInstance();
        this.messageService = MessageService.getInstance();
        this.messageSyncCache = new ConcurrentHashMap<>();
        this.messageListeners = new CopyOnWriteArrayList<>();
        this.userPresenceMap = new ConcurrentHashMap<>();
        this.scheduler = Executors.newScheduledThreadPool(2);
        
        initialize();
    }
    
    public static synchronized MessageSyncService getInstance() {
        if (instance == null) {
            instance = new MessageSyncService();
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
        
        // Démarrer la vérification périodique des présences
        scheduler.scheduleAtFixedRate(this::updateUserPresences, 30, 30, TimeUnit.SECONDS);
        
        System.out.println("✅ MessageSyncService initialisé");
    }
    
    // ==================== GESTION DES MESSAGES ====================
    
    /**
     * Envoie un message et notifie les autres instances
     */
    public boolean envoyerMessage(Message message) {
        try {
            // Générer un ID de synchronisation unique
            String syncId = generateSyncId();
            message.setId(0); // Reset ID pour l'insertion
            
            // Sauvegarder en base
            if (messageService.saveMessage(message)) {
                // Ajouter au cache
                messageSyncCache.put(syncId, LocalDateTime.now());
                
                // Notifier les autres instances
                notifyNewMessage(message.getId(), syncId);
                
                // Notifier les listeners locaux
                notifyListeners(message);
                
                return true;
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi du message: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Envoie un message à un groupe
     */
    public boolean envoyerMessageGroupe(Message message, MessageGroup groupe) {
        try {
            // Générer un ID de synchronisation unique
            String syncId = generateSyncId();
            
            // Le message est destiné au groupe (pas à un utilisateur spécifique)
            message.setId(0);
            
            // Sauvegarder le message pour chaque membre du groupe
            for (MessageGroupMember membre : groupe.getMembres()) {
                if (membre.getUser().getId() != message.getExpediteur().getId()) {
                    Message messageCopie = cloneMessage(message);
                    messageCopie.setDestinataire(membre.getUser());
                    
                    if (messageService.saveMessage(messageCopie)) {
                        // Ajouter au cache
                        messageSyncCache.put(syncId + "_" + membre.getUser().getId(), LocalDateTime.now());
                    }
                }
            }
            
            // Notifier les autres instances
            notifyNewGroupMessage(groupe.getId(), syncId);
            
            return true;
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi du message de groupe: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Clone un message
     */
    private Message cloneMessage(Message original) {
        Message copie = new Message();
        copie.setExpediteur(original.getExpediteur());
        copie.setObjet(original.getObjet());
        copie.setContenu(original.getContenu());
        copie.setPriorite(original.getPriorite());
        copie.setTypeMessage(original.getTypeMessage());
        copie.setImportant(original.isImportant());
        return copie;
    }
    
    /**
     * Marque un message comme lu et synchronise
     */
    public boolean marquerCommeLu(Message message) {
        try {
            message.marquerCommeLu();
            
            if (messageService.saveMessage(message)) {
                // Notifier les autres instances
                notifyMessageRead(message.getId());
                return true;
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du marquage du message: " + e.getMessage());
        }
        
        return false;
    }
    
    // ==================== GESTION DES GROUPES ====================
    
    /**
     * Crée un nouveau groupe et le synchronise
     */
    public boolean creerGroupe(MessageGroup groupe) {
        try {
            // Sauvegarder en base via une requête SQL directe
            String query = """
                INSERT INTO message_groups (nom, description, createur_id, type_groupe)
                VALUES (?, ?, ?, ?)
            """;
            
            try (Connection conn = databaseService.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
                
                stmt.setString(1, groupe.getNom());
                stmt.setString(2, groupe.getDescription());
                stmt.setInt(3, groupe.getCreateur().getId());
                stmt.setString(4, groupe.getTypeGroupe().name().toLowerCase());
                
                int affected = stmt.executeUpdate();
                
                if (affected > 0) {
                    try (ResultSet rs = stmt.getGeneratedKeys()) {
                        if (rs.next()) {
                            groupe.setId(rs.getInt(1));
                        }
                    }
                    
                    // Notifier les autres instances
                    notifyGroupCreated(groupe.getId());
                    
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la création du groupe: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Ajoute un membre à un groupe
     */
    public boolean ajouterMembreGroupe(MessageGroup groupe, User user, MessageGroupMember.RoleGroupe role) {
        try {
            String query = """
                INSERT INTO message_group_members (group_id, user_id, role_groupe)
                VALUES (?, ?, ?)
            """;
            
            try (Connection conn = databaseService.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                
                stmt.setInt(1, groupe.getId());
                stmt.setInt(2, user.getId());
                stmt.setString(3, role.name().toLowerCase());
                
                if (stmt.executeUpdate() > 0) {
                    // Notifier les autres instances
                    notifyGroupMemberAdded(groupe.getId(), user.getId());
                    return true;
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'ajout du membre: " + e.getMessage());
        }
        
        return false;
    }
    
    // ==================== GESTION DE LA PRÉSENCE ====================
    
    /**
     * Met à jour la présence d'un utilisateur
     */
    public void updatePresence(int userId, UserPresence.Statut statut) {
        try {
            String ipAddress = networkService.getLocalIPAddress();
            String hostname = networkService.getLocalHostName();
            
            String query = """
                INSERT INTO user_presence (user_id, statut, ip_address, hostname, derniere_activite)
                VALUES (?, ?, ?, ?, CURRENT_TIMESTAMP)
                ON DUPLICATE KEY UPDATE 
                    statut = VALUES(statut),
                    ip_address = VALUES(ip_address),
                    hostname = VALUES(hostname),
                    derniere_activite = CURRENT_TIMESTAMP
            """;
            
            try (Connection conn = databaseService.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                
                stmt.setInt(1, userId);
                stmt.setString(2, statut.name().toLowerCase());
                stmt.setString(3, ipAddress);
                stmt.setString(4, hostname);
                
                stmt.executeUpdate();
                
                // Notifier les autres instances
                notifyPresenceUpdate(userId, statut);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour de la présence: " + e.getMessage());
        }
    }
    
    /**
     * Récupère la présence d'un utilisateur
     */
    public UserPresence getUserPresence(int userId) {
        // Vérifier d'abord dans le cache
        UserPresence cached = userPresenceMap.get(userId);
        if (cached != null && cached.isActif()) {
            return cached;
        }
        
        // Sinon récupérer depuis la base
        try {
            String query = "SELECT * FROM user_presence WHERE user_id = ?";
            
            try (Connection conn = databaseService.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                
                stmt.setInt(1, userId);
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        UserPresence presence = mapResultSetToUserPresence(rs);
                        userPresenceMap.put(userId, presence);
                        return presence;
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération de la présence: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Récupère tous les utilisateurs en ligne
     */
    public List<UserPresence> getOnlineUsers() {
        List<UserPresence> onlineUsers = new ArrayList<>();
        
        try {
            String query = """
                SELECT * FROM user_presence 
                WHERE statut = 'online' 
                AND derniere_activite > DATE_SUB(NOW(), INTERVAL 5 MINUTE)
            """;
            
            try (Connection conn = databaseService.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                
                while (rs.next()) {
                    onlineUsers.add(mapResultSetToUserPresence(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des utilisateurs en ligne: " + e.getMessage());
        }
        
        return onlineUsers;
    }
    
    // ==================== NOTIFICATIONS RÉSEAU ====================
    
    /**
     * Notifie les autres instances d'un nouveau message
     */
    private void notifyNewMessage(int messageId, String syncId) {
        String message = "NEW_MESSAGE|" + messageId + "|" + syncId;
        sendMulticastMessage(message);
    }
    
    /**
     * Notifie les autres instances d'un nouveau message de groupe
     */
    private void notifyNewGroupMessage(int groupId, String syncId) {
        String message = "NEW_GROUP_MESSAGE|" + groupId + "|" + syncId;
        sendMulticastMessage(message);
    }
    
    /**
     * Notifie qu'un message a été lu
     */
    private void notifyMessageRead(int messageId) {
        String message = "MESSAGE_READ|" + messageId;
        sendMulticastMessage(message);
    }
    
    /**
     * Notifie la création d'un groupe
     */
    private void notifyGroupCreated(int groupId) {
        String message = "GROUP_CREATED|" + groupId;
        sendMulticastMessage(message);
    }
    
    /**
     * Notifie l'ajout d'un membre à un groupe
     */
    private void notifyGroupMemberAdded(int groupId, int userId) {
        String message = "GROUP_MEMBER_ADDED|" + groupId + "|" + userId;
        sendMulticastMessage(message);
    }
    
    /**
     * Notifie un changement de présence
     */
    private void notifyPresenceUpdate(int userId, UserPresence.Statut statut) {
        String message = "PRESENCE_UPDATE|" + userId + "|" + statut.name();
        sendMulticastMessage(message);
    }
    
    /**
     * Envoie un message multicast
     */
    private void sendMulticastMessage(String message) {
        try {
            byte[] data = message.getBytes();
            java.net.DatagramPacket packet = new java.net.DatagramPacket(
                data, data.length,
                java.net.InetAddress.getByName("230.0.0.1"),
                9876
            );
            
            // Utiliser le socket du NetworkService
            System.out.println("📡 Message sync envoyé: " + message);
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi multicast: " + e.getMessage());
        }
    }
    
    // ==================== LISTENERS ====================
    
    @Override
    public void onWorkflowUpdate(int courrierId, String serviceCode) {
        // Ne rien faire pour les workflows
    }
    
    @Override
    public void onWorkflowComplete(int courrierId) {
        // Ne rien faire pour les workflows
    }
    
    @Override
    public void onRefreshRequest() {
        // Rafraîchir les messages
        notifyListenersRefresh();
    }
    
    /**
     * Ajoute un listener pour les nouveaux messages
     */
    public void addMessageListener(MessageListener listener) {
        if (listener != null && !messageListeners.contains(listener)) {
            messageListeners.add(listener);
        }
    }
    
    /**
     * Retire un listener
     */
    public void removeMessageListener(MessageListener listener) {
        messageListeners.remove(listener);
    }
    
    /**
     * Notifie les listeners d'un nouveau message
     */
    private void notifyListeners(Message message) {
        for (MessageListener listener : messageListeners) {
            try {
                listener.onNewMessage(message);
            } catch (Exception e) {
                System.err.println("Erreur lors de la notification du listener: " + e.getMessage());
            }
        }
    }
    
    /**
     * Notifie les listeners d'un rafraîchissement
     */
    private void notifyListenersRefresh() {
        for (MessageListener listener : messageListeners) {
            try {
                listener.onRefreshRequest();
            } catch (Exception e) {
                System.err.println("Erreur lors de la notification du listener: " + e.getMessage());
            }
        }
    }
    
    // ==================== UTILITAIRES ====================
    
    /**
     * Génère un ID de synchronisation unique
     */
    private String generateSyncId() {
        return UUID.randomUUID().toString();
    }
    
    /**
     * Nettoie le cache des messages synchronisés
     */
    private void cleanupCache() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(10);
        messageSyncCache.entrySet().removeIf(entry -> entry.getValue().isBefore(threshold));
    }
    
    /**
     * Met à jour les présences des utilisateurs
     */
    private void updateUserPresences() {
        // Marquer les utilisateurs inactifs comme "away"
        try {
            String query = """
                UPDATE user_presence 
                SET statut = 'away' 
                WHERE statut = 'online' 
                AND derniere_activite < DATE_SUB(NOW(), INTERVAL 5 MINUTE)
            """;
            
            try (Connection conn = databaseService.getConnection();
                 Statement stmt = conn.createStatement()) {
                stmt.executeUpdate(query);
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour des présences: " + e.getMessage());
        }
    }
    
    /**
     * Mappe un ResultSet vers UserPresence
     */
    private UserPresence mapResultSetToUserPresence(ResultSet rs) throws SQLException {
        UserPresence presence = new UserPresence();
        presence.setUserId(rs.getInt("user_id"));
        presence.setStatut(UserPresence.Statut.valueOf(rs.getString("statut").toUpperCase()));
        
        Timestamp derniereActivite = rs.getTimestamp("derniere_activite");
        if (derniereActivite != null) {
            presence.setDerniereActivite(derniereActivite.toLocalDateTime());
        }
        
        presence.setIpAddress(rs.getString("ip_address"));
        presence.setHostname(rs.getString("hostname"));
        
        return presence;
    }
    
    /**
     * Arrête le service
     */
    public void shutdown() {
        scheduler.shutdown();
        try {
            if (!scheduler.awaitTermination(5, TimeUnit.SECONDS)) {
                scheduler.shutdownNow();
            }
        } catch (InterruptedException e) {
            scheduler.shutdownNow();
        }
        
        System.out.println("✅ MessageSyncService arrêté");
    }
    
    /**
     * Interface pour écouter les nouveaux messages
     */
    public interface MessageListener {
        void onNewMessage(Message message);
        void onRefreshRequest();
    }
}