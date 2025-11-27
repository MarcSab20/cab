package application.services;

import application.models.User;
import application.models.Document;
import application.models.Message;
import application.utils.JsonUtils;
import application.utils.SessionManager;

import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.concurrent.*;
import java.security.MessageDigest;
import javax.crypto.Cipher;
import javax.crypto.spec.SecretKeySpec;
import java.util.Base64;

/**
 * Service de communication réseau sécurisé pour l'application
 * Gère les communications inter-machines via intranet/internet
 */
public class NetworkService {
    
    private static NetworkService instance;
    private ServerSocket serverSocket;
    private ExecutorService threadPool;
    private boolean isRunning = false;
    
    // Configuration réseau
    private static final int DEFAULT_PORT = 8080;
    private static final String ENCRYPTION_KEY = "DocumentMgr2025!"; // À changer en production
    private static final int MAX_CONNECTIONS = 50;
    private static final int TIMEOUT_MS = 30000;
    
    // Clients connectés
    private final Map<String, ClientConnection> connectedClients = new ConcurrentHashMap<>();
    private final Map<String, Long> lastHeartbeat = new ConcurrentHashMap<>();
    
    // Configuration du réseau local
    private InetAddress localAddress;
    private int serverPort;
    private boolean isServer = false;
    
    private NetworkService() {
        threadPool = Executors.newFixedThreadPool(MAX_CONNECTIONS);
        serverPort = DEFAULT_PORT;
    }
    
    public static synchronized NetworkService getInstance() {
        if (instance == null) {
            instance = new NetworkService();
        }
        return instance;
    }
    
    /**
     * Initialise le service réseau
     */
    public void initialize() throws NetworkException {
        try {
            // Détection de l'adresse IP locale
            localAddress = InetAddress.getLocalHost();
            System.out.println("Adresse IP locale: " + localAddress.getHostAddress());
            
            // Tentative de démarrage en mode serveur
            startAsServer();
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation réseau: " + e.getMessage());
            throw new NetworkException("Impossible d'initialiser le service réseau", e);
        }
    }
    
    /**
     * Démarre le service en mode serveur
     */
    private void startAsServer() {
        try {
            serverSocket = new ServerSocket(serverPort);
            isServer = true;
            isRunning = true;
            
            System.out.println("Serveur démarré sur le port " + serverPort);
            
            // Thread d'écoute des connexions
            threadPool.submit(this::acceptConnections);
            
            // Thread de surveillance des connexions
            threadPool.submit(this::monitorConnections);
            
        } catch (IOException e) {
            System.out.println("Impossible de démarrer en mode serveur: " + e.getMessage());
            System.out.println("Fonctionnement en mode client uniquement");
            isServer = false;
        }
    }
    
    /**
     * Accepte les connexions entrantes
     */
    private void acceptConnections() {
        while (isRunning && isServer) {
            try {
                Socket clientSocket = serverSocket.accept();
                String clientId = clientSocket.getInetAddress().getHostAddress() + 
                                 ":" + clientSocket.getPort();
                
                System.out.println("Nouvelle connexion: " + clientId);
                
                ClientConnection connection = new ClientConnection(clientSocket, clientId);
                connectedClients.put(clientId, connection);
                
                threadPool.submit(connection);
                
            } catch (IOException e) {
                if (isRunning) {
                    System.err.println("Erreur lors de l'acceptation de connexion: " + e.getMessage());
                }
            }
        }
    }
    
    /**
     * Surveille les connexions actives
     */
    private void monitorConnections() {
        while (isRunning) {
            try {
                long currentTime = System.currentTimeMillis();
                
                // Vérification des heartbeats
                lastHeartbeat.entrySet().removeIf(entry -> {
                    if (currentTime - entry.getValue() > TIMEOUT_MS) {
                        String clientId = entry.getKey();
                        System.out.println("Timeout pour le client: " + clientId);
                        disconnectClient(clientId);
                        return true;
                    }
                    return false;
                });
                
                Thread.sleep(10000); // Vérification toutes les 10 secondes
                
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
                break;
            }
        }
    }
    
    /**
     * Se connecte à un autre serveur
     */
    public boolean connectToServer(String hostname, int port) {
        try {
            Socket socket = new Socket();
            socket.connect(new InetSocketAddress(hostname, port), TIMEOUT_MS);
            
            String serverId = hostname + ":" + port;
            ClientConnection connection = new ClientConnection(socket, serverId);
            connectedClients.put(serverId, connection);
            
            threadPool.submit(connection);
            
            // Authentification
            sendAuthenticationRequest(connection);
            
            System.out.println("Connecté au serveur: " + serverId);
            return true;
            
        } catch (IOException e) {
            System.err.println("Impossible de se connecter à " + hostname + ":" + port + 
                             " - " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Envoie une notification de connexion utilisateur
     */
    public void notifyUserLogin(User user) {
        NetworkMessage message = new NetworkMessage(
            NetworkMessage.Type.USER_LOGIN,
            createUserInfo(user)
        );
        
        broadcastMessage(message);
    }
    
    /**
     * Envoie une notification de déconnexion utilisateur
     */
    public void notifyUserLogout(User user) {
        NetworkMessage message = new NetworkMessage(
            NetworkMessage.Type.USER_LOGOUT,
            createUserInfo(user)
        );
        
        broadcastMessage(message);
    }
    
    /**
     * Synchronise un document avec les autres machines
     */
    public void synchronizeDocument(Document document) {
        NetworkMessage message = new NetworkMessage(
            NetworkMessage.Type.DOCUMENT_SYNC,
            JsonUtils.toJson(document)
        );
        
        broadcastMessage(message);
    }
    
    /**
     * Envoie un message à un utilisateur spécifique
     */
    public boolean sendMessage(String recipientCode, Message message) {
        NetworkMessage networkMessage = new NetworkMessage(
            NetworkMessage.Type.USER_MESSAGE,
            JsonUtils.toJson(message)
        );
        
        // Recherche du client correspondant à l'utilisateur
        for (ClientConnection connection : connectedClients.values()) {
            if (connection.getUserCode() != null && 
                connection.getUserCode().equals(recipientCode)) {
                return connection.sendMessage(networkMessage);
            }
        }
        
        return false;
    }
    
    /**
     * Diffuse un message à tous les clients connectés
     */
    private void broadcastMessage(NetworkMessage message) {
        connectedClients.values().parallelStream()
            .forEach(connection -> connection.sendMessage(message));
    }
    
    /**
     * Déconnecte un client
     */
    private void disconnectClient(String clientId) {
        ClientConnection connection = connectedClients.remove(clientId);
        if (connection != null) {
            connection.close();
        }
        lastHeartbeat.remove(clientId);
    }
    
    /**
     * Ferme le service réseau
     */
    public void shutdown() {
        isRunning = false;
        
        try {
            // Fermeture du serveur
            if (serverSocket != null && !serverSocket.isClosed()) {
                serverSocket.close();
            }
            
            // Fermeture des connexions clients
            connectedClients.values().forEach(ClientConnection::close);
            connectedClients.clear();
            
            // Arrêt du pool de threads
            threadPool.shutdown();
            if (!threadPool.awaitTermination(5, TimeUnit.SECONDS)) {
                threadPool.shutdownNow();
            }
            
            System.out.println("Service réseau arrêté");
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'arrêt du service réseau: " + e.getMessage());
        }
    }
    
    private String createUserInfo(User user) {
        Map<String, Object> userInfo = new HashMap<>();
        userInfo.put("code", user.getCode());
        userInfo.put("nom", user.getNomComplet());
        userInfo.put("role", user.getRole().getNom());
        userInfo.put("timestamp", System.currentTimeMillis());
        
        return JsonUtils.toJson(userInfo);
    }
    
    private void sendAuthenticationRequest(ClientConnection connection) {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null) {
            NetworkMessage authMessage = new NetworkMessage(
                NetworkMessage.Type.AUTHENTICATION,
                createUserInfo(currentUser)
            );
            connection.sendMessage(authMessage);
        }
    }
    
    // Getters
    public boolean isServer() { return isServer; }
    public boolean isRunning() { return isRunning; }
    public String getLocalAddress() { 
        return localAddress != null ? localAddress.getHostAddress() : "unknown"; 
    }
    public int getServerPort() { return serverPort; }
    public int getConnectedClientsCount() { return connectedClients.size(); }
    
    /**
     * Classe représentant une connexion client
     */
    private class ClientConnection implements Runnable {
        private final Socket socket;
        private final String clientId;
        private PrintWriter out;
        private BufferedReader in;
        private String userCode;
        
        public ClientConnection(Socket socket, String clientId) {
            this.socket = socket;
            this.clientId = clientId;
            
            try {
                this.out = new PrintWriter(
                    new OutputStreamWriter(socket.getOutputStream(), StandardCharsets.UTF_8), true);
                this.in = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.UTF_8));
                
            } catch (IOException e) {
                System.err.println("Erreur lors de l'initialisation de la connexion: " + e.getMessage());
            }
        }
        
        @Override
        public void run() {
            try {
                String inputLine;
                while ((inputLine = in.readLine()) != null) {
                    processMessage(inputLine);
                    lastHeartbeat.put(clientId, System.currentTimeMillis());
                }
            } catch (IOException e) {
                System.out.println("Connexion fermée: " + clientId);
            } finally {
                close();
            }
        }
        
        private void processMessage(String messageData) {
            try {
                NetworkMessage message = JsonUtils.fromJson(messageData, NetworkMessage.class);
                
                switch (message.getType()) {
                    case AUTHENTICATION:
                        handleAuthentication(message);
                        break;
                    case USER_LOGIN:
                        handleUserLogin(message);
                        break;
                    case USER_LOGOUT:
                        handleUserLogout(message);
                        break;
                    case DOCUMENT_SYNC:
                        handleDocumentSync(message);
                        break;
                    case USER_MESSAGE:
                        handleUserMessage(message);
                        break;
                    case HEARTBEAT:
                        // Déjà géré par la mise à jour du timestamp
                        break;
                }
                
            } catch (Exception e) {
                System.err.println("Erreur lors du traitement du message: " + e.getMessage());
            }
        }
        
        private void handleAuthentication(NetworkMessage message) {
            // Traitement de l'authentification du client distant
            System.out.println("Authentification reçue de: " + clientId);
        }
        
        private void handleUserLogin(NetworkMessage message) {
            System.out.println("Utilisateur connecté sur: " + clientId);
        }
        
        private void handleUserLogout(NetworkMessage message) {
            System.out.println("Utilisateur déconnecté de: " + clientId);
        }
        
        private void handleDocumentSync(NetworkMessage message) {
            // Synchronisation de document
            System.out.println("Synchronisation de document depuis: " + clientId);
        }
        
        private void handleUserMessage(NetworkMessage message) {
            // Réception d'un message utilisateur
            System.out.println("Message utilisateur reçu de: " + clientId);
        }
        
        public boolean sendMessage(NetworkMessage message) {
            if (out != null) {
                try {
                    String jsonMessage = JsonUtils.toJson(message);
                    out.println(jsonMessage);
                    return !out.checkError();
                } catch (Exception e) {
                    System.err.println("Erreur lors de l'envoi du message: " + e.getMessage());
                }
            }
            return false;
        }
        
        /**
         * Notifie une mise à jour du workflow d'un courrier
         */
        public void notifyWorkflowUpdate(int courrierId, String serviceCode) {
            Map<String, Object> data = new HashMap<>();
            data.put("courrier_id", courrierId);
            data.put("service_code", serviceCode);
            data.put("type", "workflow_update");
            data.put("timestamp", System.currentTimeMillis());
            
            NetworkMessage message = new NetworkMessage(
                NetworkMessage.Type.DOCUMENT_SYNC,
                JsonUtils.toJson(data)
            );
            
            broadcastMessage(message);
        }

        /**
         * Notifie la complétion d'un workflow
         */
        public void notifyWorkflowComplete(int courrierId) {
            Map<String, Object> data = new HashMap<>();
            data.put("courrier_id", courrierId);
            data.put("type", "workflow_complete");
            data.put("timestamp", System.currentTimeMillis());
            
            NetworkMessage message = new NetworkMessage(
                NetworkMessage.Type.DOCUMENT_SYNC,
                JsonUtils.toJson(data)
            );
            
            broadcastMessage(message);
        }

        
        public void close() {
            try {
                if (socket != null && !socket.isClosed()) {
                    socket.close();
                }
            } catch (IOException e) {
                System.err.println("Erreur lors de la fermeture de la connexion: " + e.getMessage());
            }
        }
        
        public String getUserCode() { return userCode; }
        public void setUserCode(String userCode) { this.userCode = userCode; }
    }
    
    /**
     * Classe représentant un message réseau
     */
    public static class NetworkMessage {
        public enum Type {
            AUTHENTICATION, USER_LOGIN, USER_LOGOUT, 
            DOCUMENT_SYNC, USER_MESSAGE, HEARTBEAT
        }
        
        private Type type;
        private String data;
        private long timestamp;
        
        public NetworkMessage() {}
        
        public NetworkMessage(Type type, String data) {
            this.type = type;
            this.data = data;
            this.timestamp = System.currentTimeMillis();
        }
        
        // Getters et setters
        public Type getType() { return type; }
        public void setType(Type type) { this.type = type; }
        public String getData() { return data; }
        public void setData(String data) { this.data = data; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}

/**
 * Exception pour les erreurs réseau
 */
class NetworkException extends Exception {
    public NetworkException(String message) {
        super(message);
    }
    
    public NetworkException(String message, Throwable cause) {
        super(message, cause);
    }
}