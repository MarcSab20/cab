package application.services;

import application.models.User;
import application.models.Role;

import java.sql.SQLException;
import java.util.List;

/**
 * Service pour la gestion des utilisateurs
 */
public class UserService {
    
    private static UserService instance;
    private final DatabaseService databaseService;
    
    private UserService() {
        this.databaseService = DatabaseService.getInstance();
    }
    
    public static synchronized UserService getInstance() {
        if (instance == null) {
            instance = new UserService();
        }
        return instance;
    }
    
    /**
     * Récupère un utilisateur par son code
     */
    public User getUserByCode(String code) {
        try {
            return databaseService.getUserByCode(code);
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération de l'utilisateur: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Récupère un utilisateur par son token de session
     */
    public User getUserBySessionToken(String sessionToken) {
        try {
            return databaseService.getUserBySessionToken(sessionToken);
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération de l'utilisateur par token: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Met à jour un utilisateur
     */
    public boolean updateUser(User user) {
        try {
            databaseService.updateUser(user);
            return true;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour de l'utilisateur: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Récupère tous les rôles actifs
     */
    public List<Role> getRolesActifs() {
        try {
            return databaseService.getActiveRoles();
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des rôles: " + e.getMessage());
            return List.of(); // Retourne une liste vide en cas d'erreur
        }
    }
}