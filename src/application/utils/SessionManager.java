package application.utils;

import application.models.User;
import application.models.Permission;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

/**
 * Gestionnaire de session utilisateur
 */
public class SessionManager {
    
    private static SessionManager instance;
    private User currentUser;
    private LocalDateTime sessionStart;
    private Map<String, Object> sessionData;
    private boolean sessionActive;
    
    private SessionManager() {
        this.sessionData = new HashMap<>();
        this.sessionActive = false;
    }
    
    public static synchronized SessionManager getInstance() {
        if (instance == null) {
            instance = new SessionManager();
        }
        return instance;
    }
    
    /**
     * Démarre une nouvelle session
     */
    public void setCurrentUser(User user) {
        this.currentUser = user;
        this.sessionStart = LocalDateTime.now();
        this.sessionActive = true;
        this.sessionData.clear();
        
        System.out.println("Session démarrée pour: " + user.getNomComplet());
    }
    
    /**
     * Termine la session courante
     */
    public void clearSession() {
        if (currentUser != null) {
            System.out.println("Session terminée pour: " + currentUser.getNomComplet());
        }
        
        this.currentUser = null;
        this.sessionStart = null;
        this.sessionActive = false;
        this.sessionData.clear();
    }
    
    /**
     * Vérifie si l'utilisateur a une permission spécifique
     */
    public boolean hasPermission(Permission permission) {
        return currentUser != null && 
               currentUser.getRole() != null && 
               currentUser.getRole().hasPermission(permission);
    }
    
    /**
     * Vérifie si l'utilisateur a une permission par nom
     */
    public boolean hasPermission(String permissionName) {
        return currentUser != null && 
               currentUser.getRole() != null && 
               currentUser.getRole().hasPermission(permissionName);
    }
    
    /**
     * Stocke une donnée dans la session
     */
    public void setSessionData(String key, Object value) {
        sessionData.put(key, value);
    }
    
    /**
     * Récupère une donnée de la session
     */
    @SuppressWarnings("unchecked")
    public <T> T getSessionData(String key, Class<T> type) {
        Object value = sessionData.get(key);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }
    
    /**
     * Supprime une donnée de la session
     */
    public void removeSessionData(String key) {
        sessionData.remove(key);
    }
    
    /**
     * Vérifie si la session est valide et active
     */
    public boolean isSessionValid() {
        return sessionActive && 
               currentUser != null && 
               currentUser.isActif() &&
               sessionStart != null;
    }
    
    /**
     * Calcule la durée de la session en minutes
     */
    public long getSessionDurationMinutes() {
        if (sessionStart != null) {
            return java.time.Duration.between(sessionStart, LocalDateTime.now()).toMinutes();
        }
        return 0;
    }
    
    // Getters
    public User getCurrentUser() {
        return currentUser;
    }
    
    public LocalDateTime getSessionStart() {
        return sessionStart;
    }
    
    public boolean isSessionActive() {
        return sessionActive;
    }
    
    public String getCurrentUserCode() {
        return currentUser != null ? currentUser.getCode() : null;
    }
    
    public String getCurrentUserRole() {
        return currentUser != null && currentUser.getRole() != null ? 
               currentUser.getRole().getNom() : null;
    }
}