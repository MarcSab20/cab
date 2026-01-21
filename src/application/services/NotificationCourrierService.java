package application.services;

import application.models.Courrier;
import application.models.CourrierNotificationInfo;
import application.models.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de gestion des notifications de courriers
 * Permet d'assigner automatiquement les nouveaux courriers au responsable désigné
 */
public class NotificationCourrierService {
    
    private static NotificationCourrierService instance;
    private final DatabaseService databaseService;
    private final LogService logService;
    
    private NotificationCourrierService() {
        this.databaseService = DatabaseService.getInstance();
        this.logService = LogService.getInstance();
    }
    
    public static synchronized NotificationCourrierService getInstance() {
        if (instance == null) {
            instance = new NotificationCourrierService();
        }
        return instance;
    }
    
    /**
     * Définit le responsable des courriers
     */
    public boolean setResponsableCourrier(int userId) {
        String sql = "INSERT INTO config_responsable_courrier (user_id, actif) " +
                     "VALUES (?, TRUE) " +
                     "ON DUPLICATE KEY UPDATE user_id = VALUES(user_id), " +
                     "date_modification = NOW()";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            int result = stmt.executeUpdate();
            
            if (result > 0) {
                logService.logAction("config_responsable_courrier", 
                    "Responsable courrier défini: user_id=" + userId);
                System.out.println("✓ Responsable courrier configuré: user_id=" + userId);
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur configuration responsable: " + e.getMessage());
            logService.logErreur("config_responsable_courrier", e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Récupère le responsable actuel des courriers
     */
    public User getResponsableCourrier() {
        String sql = "SELECT u.* FROM config_responsable_courrier crc " +
                     "JOIN users u ON crc.user_id = u.id " +
                     "WHERE crc.actif = TRUE " +
                     "LIMIT 1";
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            if (rs.next()) {
                return mapResultSetToUser(rs);
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération responsable: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Notifie le responsable d'un nouveau courrier
     */
    public boolean notifierNouveauCourrier(Courrier courrier) {
        User responsable = getResponsableCourrier();
        
        if (responsable == null) {
            System.out.println("⚠ Aucun responsable configuré pour les courriers");
            return false;
        }
        
        String sql = "INSERT INTO notifications_courrier (courrier_id, user_id) " +
                     "VALUES (?, ?)";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, courrier.getId());
            stmt.setInt(2, responsable.getId());
            
            int result = stmt.executeUpdate();
            
            if (result > 0) {
                logService.logAction("notification_courrier", 
                    "Courrier " + courrier.getCodeCourrier() + 
                    " notifié à " + responsable.getCode());
                
                System.out.println("✓ Notification envoyée à " + responsable.getNomComplet() + 
                                 " pour le courrier " + courrier.getCodeCourrier());
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur notification courrier: " + e.getMessage());
            logService.logErreur("notification_courrier", e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Récupère les courriers notifiés avec toutes leurs informations de notification
     */
    public List<CourrierNotificationInfo> getCourriersNotifiesAvecInfos(int userId, boolean seulementNonLus) {
        List<CourrierNotificationInfo> result = new ArrayList<>();
        
        StringBuilder sql = new StringBuilder(
            "SELECT c.*, nc.lu, nc.date_notification, nc.date_lecture " +
            "FROM notifications_courrier nc " +
            "JOIN courriers c ON nc.courrier_id = c.id " +
            "WHERE nc.user_id = ?");
        
        if (seulementNonLus) {
            sql.append(" AND nc.lu = FALSE");
        }
        
        sql.append(" ORDER BY nc.date_notification DESC");
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Courrier courrier = mapResultSetToCourrier(rs);
                    
                    CourrierNotificationInfo info = new CourrierNotificationInfo();
                    info.setCourrier(courrier);
                    info.setLu(rs.getBoolean("lu"));
                    
                    Timestamp dateNotif = rs.getTimestamp("date_notification");
                    if (dateNotif != null) {
                        info.setDateNotification(dateNotif.toLocalDateTime());
                    }
                    
                    Timestamp dateLecture = rs.getTimestamp("date_lecture");
                    if (dateLecture != null) {
                        info.setDateLecture(dateLecture.toLocalDateTime());
                    }
                    
                    result.add(info);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération courriers notifiés avec infos: " + e.getMessage());
            e.printStackTrace();
        }
        
        return result;
    }
    
    /**
     * Compte les courriers non lus pour un utilisateur
     */
    public int compterCourriersNonLus(int userId) {
        String sql = "SELECT COUNT(*) FROM notifications_courrier " +
                     "WHERE user_id = ? AND lu = FALSE";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur comptage courriers non lus: " + e.getMessage());
        }
        
        return 0;
    }
    
    /**
     * Marque un courrier comme lu
     */
    public boolean marquerCommeLu(int courrierId, int userId) {
        String sql = "UPDATE notifications_courrier " +
                     "SET lu = TRUE, date_lecture = NOW() " +
                     "WHERE courrier_id = ? AND user_id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, courrierId);
            stmt.setInt(2, userId);
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Erreur marquage lecture: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Supprime le responsable actuel
     */
    public boolean supprimerResponsable() {
        String sql = "DELETE FROM config_responsable_courrier WHERE actif = TRUE";
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement()) {
            
            int result = stmt.executeUpdate(sql);
            
            if (result > 0) {
                logService.logAction("suppression_responsable_courrier", 
                    "Responsable courrier supprimé");
                return true;
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur suppression responsable: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Récupère tous les utilisateurs disponibles
     */
    public List<User> getTousUtilisateursActifs() {
        List<User> users = new ArrayList<>();
        String sql = "SELECT u.*, r.nom as role_nom, r.description as role_desc, " +
                     "r.permissions, r.actif as role_actif " +
                     "FROM users u " +
                     "LEFT JOIN roles r ON u.role_id = r.id " +
                     "WHERE u.actif = TRUE " +
                     "ORDER BY u.nom, u.prenom";
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(sql)) {
            
            while (rs.next()) {
                User user = mapResultSetToUser(rs);
                users.add(user);
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération utilisateurs: " + e.getMessage());
        }
        
        return users;
    }
    
    // Méthodes de mapping
    
    private User mapResultSetToUser(ResultSet rs) throws SQLException {
        User user = new User();
        user.setId(rs.getInt("id"));
        user.setCode(rs.getString("code"));
        user.setNom(rs.getString("nom"));
        user.setPrenom(rs.getString("prenom"));
        user.setEmail(rs.getString("email"));
        user.setActif(rs.getBoolean("actif"));
        
        // Mapping du rôle si présent
        try {
            String roleNom = rs.getString("role_nom");
            if (roleNom != null) {
                application.models.Role role = new application.models.Role();
                role.setNom(roleNom);
                role.setDescription(rs.getString("role_desc"));
                user.setRole(role);
            }
        } catch (SQLException e) {
            // Rôle optionnel
        }
        
        return user;
    }
    
    private Courrier mapResultSetToCourrier(ResultSet rs) throws SQLException {
        Courrier courrier = new Courrier();
        
        courrier.setId(rs.getInt("id"));
        courrier.setCodeCourrier(rs.getString("code_courrier"));
        courrier.setDocumentId(rs.getInt("document_id"));
        
        String typeCourrier = rs.getString("type_courrier");
        if (typeCourrier != null) {
            courrier.setTypeCourrier(
                application.models.Courrier.TypeCourrier.valueOf(typeCourrier));
        }
        
        courrier.setObjet(rs.getString("objet"));
        courrier.setExpediteur(rs.getString("expediteur"));
        courrier.setDestinataire(rs.getString("destinataire"));
        courrier.setReference(rs.getString("reference"));
        
        Date dateCourrier = rs.getDate("date_courrier");
        if (dateCourrier != null) {
            courrier.setDateCourrier(dateCourrier.toLocalDate());
        }
        
        String priorite = rs.getString("priorite");
        if (priorite != null) {
            courrier.setPriorite(
                application.models.Courrier.PrioriteCourrier.valueOf(priorite));
        }
        
        courrier.setObservations(rs.getString("observations"));
        courrier.setConfidentiel(rs.getBoolean("confidentiel"));
        
        String statut = rs.getString("statut");
        if (statut != null) {
            courrier.setStatut(
                application.models.Courrier.StatutCourrier.valueOf(statut.toUpperCase()));
        }
        
        Timestamp dateCreation = rs.getTimestamp("date_creation");
        if (dateCreation != null) {
            courrier.setDateCreation(dateCreation.toLocalDateTime());
        }
        
        return courrier;
    }
}