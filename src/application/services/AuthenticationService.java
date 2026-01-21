package application.services;

import application.models.Role;
import application.models.User;
import application.utils.PasswordUtils;
import application.utils.SessionManager;

import java.security.SecureRandom;
import java.time.LocalDateTime;
import java.util.Base64;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.atomic.AtomicInteger;

/**
 * Service d'authentification sécurisé avec gestion des tentatives de connexion
 */
public class AuthenticationService {
    
    private static AuthenticationService instance;
    private final UserService userService;
    private final NetworkService networkService;
    
    // Gestion des tentatives de connexion échouées
    private final Map<String, AtomicInteger> failedAttempts = new ConcurrentHashMap<>();
    private final Map<String, LocalDateTime> lastFailedAttempt = new ConcurrentHashMap<>();
    private final int MAX_FAILED_ATTEMPTS = 5;
    private final int LOCKOUT_DURATION_MINUTES = 15;
    
    // Générateur sécurisé pour les tokens
    private final SecureRandom secureRandom = new SecureRandom();
    
    private AuthenticationService() {
        this.userService = UserService.getInstance();
        this.networkService = NetworkService.getInstance();
    }
    
    public static synchronized AuthenticationService getInstance() {
        if (instance == null) {
            instance = new AuthenticationService();
        }
        return instance;
    }
    
    /**
     * Authentifie un utilisateur avec code, mot de passe et rôle
     */
    public User authenticate(String code, String password, Role role) throws AuthenticationException {
        // Vérification des paramètres
        if (code == null || code.trim().isEmpty()) {
            throw new AuthenticationException("Code utilisateur requis");
        }
        
        if (password == null || password.isEmpty()) {
            throw new AuthenticationException("Mot de passe requis");
        }
        
        if (role == null) {
            throw new AuthenticationException("Rôle requis");
        }
        
        String normalizedCode = code.trim().toLowerCase();
        
        // Vérification du verrouillage du compte
        if (isAccountLocked(normalizedCode)) {
            throw new AuthenticationException(
                "Compte temporairement verrouillé suite à trop de tentatives de connexion. " +
                "Réessayez dans " + LOCKOUT_DURATION_MINUTES + " minutes."
            );
        }
        
        try {
            // Récupération de l'utilisateur
            User user = userService.getUserByCode(normalizedCode);
            
            if (user == null) {
                logFailedAttempt(normalizedCode);
                throw new AuthenticationException("Utilisateur introuvable");
            }
            
            // Vérification que l'utilisateur est actif
            if (!user.isActif()) {
                throw new AuthenticationException("Compte utilisateur désactivé");
            }
            
            // Vérification du mot de passe
            if (!PasswordUtils.verifyPassword(password, user.getPassword())) {
                logFailedAttempt(normalizedCode);
                throw new AuthenticationException("Mot de passe incorrect");
            }
            
            // Vérification du rôle
            if (!user.getRole().equals(role)) {
                logFailedAttempt(normalizedCode);
                throw new AuthenticationException("Rôle incorrect pour cet utilisateur");
            }
            
            // Authentification réussie
            return handleSuccessfulAuthentication(user);
            
        } catch (AuthenticationException e) {
            throw e;
        } catch (Exception e) {
            logFailedAttempt(normalizedCode);
            throw new AuthenticationException("Erreur lors de l'authentification: " + e.getMessage());
        }
    }
    
    /**
     * Traite une authentification réussie
     * @throws AuthenticationException 
     */
    private User handleSuccessfulAuthentication(User user) throws AuthenticationException {
        try {
            // Réinitialisation des tentatives échouées
            String normalizedCode = user.getCode().toLowerCase();
            failedAttempts.remove(normalizedCode);
            lastFailedAttempt.remove(normalizedCode);
            
            // Génération d'un token de session sécurisé
            String sessionToken = generateSessionToken();
            user.setSessionToken(sessionToken);
            
            // Mise à jour de la date de dernier accès
            user.setDernierAcces(LocalDateTime.now());
            userService.updateUser(user);
            
            // Notification de connexion via le réseau si nécessaire
            networkService.notifyUserLogin(user);
            
            // Log de la connexion réussie
            System.out.println("Connexion réussie pour l'utilisateur: " + user.getCode() + 
                             " (" + user.getNomComplet() + ") - Rôle: " + user.getRole().getNom());
            
            return user;
            
        } catch (Exception e) {
            System.err.println("Erreur lors du traitement de l'authentification: " + e.getMessage());
            throw new AuthenticationException("Erreur lors de la finalisation de la connexion");
        }
    }
    
    /**
     * Vérifie si un compte est verrouillé
     */
    private boolean isAccountLocked(String code) {
        AtomicInteger attempts = failedAttempts.get(code);
        LocalDateTime lastAttempt = lastFailedAttempt.get(code);
        
        if (attempts == null || lastAttempt == null) {
            return false;
        }
        
        // Si le nombre max de tentatives est atteint
        if (attempts.get() >= MAX_FAILED_ATTEMPTS) {
            // Vérifier si la période de verrouillage est écoulée
            LocalDateTime unlockTime = lastAttempt.plusMinutes(LOCKOUT_DURATION_MINUTES);
            if (LocalDateTime.now().isBefore(unlockTime)) {
                return true;
            } else {
                // Réinitialiser les tentatives si la période est écoulée
                failedAttempts.remove(code);
                lastFailedAttempt.remove(code);
                return false;
            }
        }
        
        return false;
    }
    
    /**
     * Enregistre une tentative de connexion échouée
     */
    public void logFailedAttempt(String code) {
        String normalizedCode = code.toLowerCase();
        
        failedAttempts.computeIfAbsent(normalizedCode, k -> new AtomicInteger(0)).incrementAndGet();
        lastFailedAttempt.put(normalizedCode, LocalDateTime.now());
        
        int attempts = failedAttempts.get(normalizedCode).get();
        System.out.println("Tentative de connexion échouée pour: " + code + 
                         " (Tentative " + attempts + "/" + MAX_FAILED_ATTEMPTS + ")");
        
        if (attempts >= MAX_FAILED_ATTEMPTS) {
            System.out.println("Compte verrouillé pour: " + code + 
                             " - Déverrouillage dans " + LOCKOUT_DURATION_MINUTES + " minutes");
        }
    }
    
    /**
     * Génère un token de session sécurisé
     */
    private String generateSessionToken() {
        byte[] tokenBytes = new byte[32];
        secureRandom.nextBytes(tokenBytes);
        return Base64.getUrlEncoder().withoutPadding().encodeToString(tokenBytes);
    }
    
    /**
     * Déconnecte un utilisateur
     */
    public void logout(User user) {
        try {
            if (user != null) {
                // Invalidation du token de session
                user.setSessionToken(null);
                userService.updateUser(user);
                
                // Nettoyage de la session
                SessionManager.getInstance().clearSession();
                
                // Notification de déconnexion
                networkService.notifyUserLogout(user);
                
                System.out.println("Déconnexion de l'utilisateur: " + user.getCode());
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la déconnexion: " + e.getMessage());
        }
    }
    
    /**
     * Vérifie la validité d'un token de session
     */
    public boolean isValidSession(String sessionToken) {
        if (sessionToken == null || sessionToken.isEmpty()) {
            return false;
        }
        
        try {
            User user = userService.getUserBySessionToken(sessionToken);
            return user != null && user.isActif();
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Change le mot de passe d'un utilisateur
     */
    public void changePassword(User user, String oldPassword, String newPassword) throws AuthenticationException {
        if (!PasswordUtils.verifyPassword(oldPassword, user.getPassword())) {
            throw new AuthenticationException("Ancien mot de passe incorrect");
        }
        
        if (!PasswordUtils.isValidPassword(newPassword)) {
            throw new AuthenticationException("Le nouveau mot de passe ne respecte pas les critères de sécurité");
        }
        
        String hashedPassword = PasswordUtils.hashPassword(newPassword);
        user.setPassword(hashedPassword);
        userService.updateUser(user);
        
        System.out.println("Mot de passe modifié pour l'utilisateur: " + user.getCode());
    }
}

/**
 * Exception personnalisée pour les erreurs d'authentification
 */
class AuthenticationException extends Exception {
    public AuthenticationException(String message) {
        super(message);
    }
    
    public AuthenticationException(String message, Throwable cause) {
        super(message, cause);
    }
}