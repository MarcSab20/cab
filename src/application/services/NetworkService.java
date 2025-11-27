package application.services;

import java.io.*;
import java.net.*;
import java.time.LocalDateTime;
import java.util.*;
import java.util.concurrent.*;

import application.models.User;

/**
 * Service de communication réseau pour synchroniser les mises à jour entre instances
 */
public class NetworkService {
    private static NetworkService instance;
    
    private static final int MULTICAST_PORT = 9876;
    private static final String MULTICAST_GROUP = "230.0.0.1";
    
    private MulticastSocket multicastSocket;
    private InetAddress group;
    private ExecutorService executorService;
    private boolean running;
    private boolean initialized;
    
    private List<WorkflowUpdateListener> listeners;
    
    private NetworkService() {
        listeners = new CopyOnWriteArrayList<>();
        executorService = Executors.newSingleThreadExecutor();
        initialized = false;
    }
    
    public static synchronized NetworkService getInstance() {
        if (instance == null) {
            instance = new NetworkService();
        }
        return instance;
    }
    
    /**
     * Initialise le service réseau
     * Cette méthode doit être appelée avant d'utiliser le service
     */
    public void initialize() {
        if (initialized) {
            System.out.println("⚠️ NetworkService déjà initialisé");
            return;
        }
        
        initializeMulticast();
        initialized = true;
    }
    
    /**
     * Vérifie si le service est initialisé
     */
    public boolean isInitialized() {
        return initialized;
    }
    
    /**
     * Initialise la socket multicast
     */
    private void initializeMulticast() {
        try {
            multicastSocket = new MulticastSocket(MULTICAST_PORT);
            group = InetAddress.getByName(MULTICAST_GROUP);
            
            // Rejoindre le groupe multicast
            NetworkInterface networkInterface = NetworkInterface.getByInetAddress(
                InetAddress.getLocalHost()
            );
            
            if (networkInterface != null) {
                multicastSocket.joinGroup(new InetSocketAddress(group, MULTICAST_PORT), networkInterface);
                running = true;
                
                // Démarrer le listener
                startListening();
                
                System.out.println("✅ NetworkService initialized on multicast " + MULTICAST_GROUP + ":" + MULTICAST_PORT);
            } else {
                System.err.println("⚠️ Aucune interface réseau disponible pour le multicast");
            }
            
        } catch (IOException e) {
            System.err.println("❌ Erreur lors de l'initialisation du NetworkService: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Démarre l'écoute des messages multicast
     */
    private void startListening() {
        executorService.submit(() -> {
            byte[] buffer = new byte[1024];
            
            while (running) {
                try {
                    DatagramPacket packet = new DatagramPacket(buffer, buffer.length);
                    multicastSocket.receive(packet);
                    
                    String message = new String(packet.getData(), 0, packet.getLength());
                    handleIncomingMessage(message);
                    
                } catch (IOException e) {
                    if (running) {
                        System.err.println("Erreur lors de la réception d'un message: " + e.getMessage());
                    }
                }
            }
        });
    }
    
    /**
     * Notifie le réseau qu'un utilisateur s'est connecté
     * 
     * @param user L'utilisateur qui s'est connecté
     */
    public void notifyUserLogin(User user) {
        if (!initialized) {
            System.err.println("⚠️ NetworkService non initialisé - Impossible d'envoyer la notification");
            return;
        }
        
        if (user == null) return;
        
        try {
            String message = String.format("USER_LOGIN|%s|%s|%s", 
                user.getCode(),
                user.getPrenom() + " " + user.getNom(),
                LocalDateTime.now().toString()
            );
            
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(
                buffer, buffer.length, group, MULTICAST_PORT
            );
            
            multicastSocket.send(packet);
            
            System.out.println("✅ Notification connexion envoyée: " + user.getCode());
            
        } catch (IOException e) {
            System.err.println("❌ Erreur lors de la notification de connexion: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Notifie le réseau qu'un utilisateur s'est déconnecté
     * 
     * @param user L'utilisateur qui s'est déconnecté
     */
    public void notifyUserLogout(User user) {
        if (!initialized) {
            System.err.println("⚠️ NetworkService non initialisé - Impossible d'envoyer la notification");
            return;
        }
        
        if (user == null) return;
        
        try {
            String message = String.format("USER_LOGOUT|%s|%s", 
                user.getCode(),
                LocalDateTime.now().toString()
            );
            
            byte[] buffer = message.getBytes();
            DatagramPacket packet = new DatagramPacket(
                buffer, buffer.length, group, MULTICAST_PORT
            );
            
            multicastSocket.send(packet);
            
            System.out.println("✅ Notification déconnexion envoyée: " + user.getCode());
            
        } catch (IOException e) {
            System.err.println("❌ Erreur lors de la notification de déconnexion: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Traite un message entrant
     */
    private void handleIncomingMessage(String message) {
        try {
            String[] parts = message.split("\\|");
            
            if (parts.length < 2) return;
            
            String messageType = parts[0];
            
            switch (messageType) {
                case "WORKFLOW_UPDATE":
                    if (parts.length >= 3) {
                        int courrierId = Integer.parseInt(parts[1]);
                        String serviceCode = parts[2];
                        notifyListeners(courrierId, serviceCode, false);
                    }
                    break;
                    
                case "WORKFLOW_COMPLETE":
                    if (parts.length >= 2) {
                        int courrierId = Integer.parseInt(parts[1]);
                        notifyListeners(courrierId, null, true);
                    }
                    break;
                    
                case "REFRESH_REQUEST":
                    notifyRefreshRequestToListeners();
                    break;
                    
                case "USER_LOGIN":
                    if (parts.length >= 3) {
                        String userCode = parts[1];
                        String userName = parts[2];
                        System.out.println("👤 Connexion: " + userName + " (" + userCode + ")");
                    }
                    break;
                    
                case "USER_LOGOUT":
                    if (parts.length >= 2) {
                        String userCode = parts[1];
                        System.out.println("👋 Déconnexion: " + userCode);
                    }
                    break;
                    
                default:
                    System.out.println("⚠️ Type de message inconnu: " + messageType);
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors du traitement du message: " + e.getMessage());
        }
    }
    
    /**
     * Envoie une notification de mise à jour du workflow
     */
    public void notifyWorkflowUpdate(int courrierId, String serviceCode) {
        if (!initialized) {
            System.err.println("⚠️ NetworkService non initialisé - Impossible d'envoyer la notification");
            return;
        }
        
        String message = "WORKFLOW_UPDATE|" + courrierId + "|" + serviceCode;
        sendMulticastMessage(message);
    }
    
    /**
     * Envoie une notification de workflow terminé
     */
    public void notifyWorkflowComplete(int courrierId) {
        if (!initialized) {
            System.err.println("⚠️ NetworkService non initialisé - Impossible d'envoyer la notification");
            return;
        }
        
        String message = "WORKFLOW_COMPLETE|" + courrierId;
        sendMulticastMessage(message);
    }
    
    /**
     * Envoie une demande de rafraîchissement global
     */
    public void notifyRefreshRequest() {
        if (!initialized) {
            System.err.println("⚠️ NetworkService non initialisé - Impossible d'envoyer la notification");
            return;
        }
        
        String message = "REFRESH_REQUEST";
        sendMulticastMessage(message);
    }
    
    /**
     * Envoie un message via multicast
     */
    private void sendMulticastMessage(String message) {
        try {
            byte[] data = message.getBytes();
            DatagramPacket packet = new DatagramPacket(data, data.length, group, MULTICAST_PORT);
            multicastSocket.send(packet);
            
            System.out.println("📡 Message envoyé: " + message);
            
        } catch (IOException e) {
            System.err.println("❌ Erreur lors de l'envoi du message: " + e.getMessage());
        }
    }
    
    /**
     * Ajoute un listener pour les mises à jour du workflow
     */
    public void addWorkflowUpdateListener(WorkflowUpdateListener listener) {
        if (listener != null && !listeners.contains(listener)) {
            listeners.add(listener);
        }
    }
    
    /**
     * Retire un listener
     */
    public void removeWorkflowUpdateListener(WorkflowUpdateListener listener) {
        listeners.remove(listener);
    }
    
    /**
     * Notifie tous les listeners d'une mise à jour
     */
    private void notifyListeners(int courrierId, String serviceCode, boolean completed) {
        for (WorkflowUpdateListener listener : listeners) {
            try {
                if (completed) {
                    listener.onWorkflowComplete(courrierId);
                } else {
                    listener.onWorkflowUpdate(courrierId, serviceCode);
                }
            } catch (Exception e) {
                System.err.println("Erreur lors de la notification du listener: " + e.getMessage());
            }
        }
    }
    
    /**
     * Notifie tous les listeners d'une demande de rafraîchissement
     */
    private void notifyRefreshRequestToListeners() {
        for (WorkflowUpdateListener listener : listeners) {
            try {
                listener.onRefreshRequest();
            } catch (Exception e) {
                System.err.println("Erreur lors de la notification du listener: " + e.getMessage());
            }
        }
    }
    
    /**
     * Ferme le service réseau
     */
    public void shutdown() {
        running = false;
        initialized = false;
        
        try {
            if (multicastSocket != null) {
                NetworkInterface networkInterface = NetworkInterface.getByInetAddress(
                    InetAddress.getLocalHost()
                );
                
                if (networkInterface != null) {
                    multicastSocket.leaveGroup(new InetSocketAddress(group, MULTICAST_PORT), networkInterface);
                }
                
                multicastSocket.close();
            }
            
            executorService.shutdown();
            
            if (!executorService.awaitTermination(5, TimeUnit.SECONDS)) {
                executorService.shutdownNow();
            }
            
            System.out.println("✅ NetworkService arrêté");
            
        } catch (IOException | InterruptedException e) {
            System.err.println("❌ Erreur lors de l'arrêt du NetworkService: " + e.getMessage());
        }
    }
    
    /**
     * Interface pour écouter les mises à jour du workflow
     */
    public interface WorkflowUpdateListener {
        /**
         * Appelé quand un workflow est mis à jour
         */
        void onWorkflowUpdate(int courrierId, String serviceCode);
        
        /**
         * Appelé quand un workflow est terminé
         */
        void onWorkflowComplete(int courrierId);
        
        /**
         * Appelé quand une demande de rafraîchissement est reçue
         */
        void onRefreshRequest();
    }
}