package application.services;

import application.models.Message;
import application.models.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class MessageService {
    
    private static MessageService instance;
    private final DatabaseService databaseService;
    
    private MessageService() {
        this.databaseService = DatabaseService.getInstance();
    }
    
    public static synchronized MessageService getInstance() {
        if (instance == null) {
            instance = new MessageService();
        }
        return instance;
    }
    
    public List<Message> getMessagesForUser(int userId) {
        List<Message> messages = new ArrayList<>();
        String query = """
            SELECT m.*, 
                   exp.nom as exp_nom, exp.prenom as exp_prenom,
                   dest.nom as dest_nom, dest.prenom as dest_prenom
            FROM messages m
            LEFT JOIN users exp ON m.expediteur_id = exp.id
            LEFT JOIN users dest ON m.destinataire_id = dest.id
            WHERE m.destinataire_id = ?
            ORDER BY m.date_envoi DESC
        """;
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, userId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapResultSetToMessage(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des messages: " + e.getMessage());
        }
        
        return messages;
    }
    
    public Message getMessageById(int id) {
        String query = """
            SELECT m.*, 
                   exp.nom as exp_nom, exp.prenom as exp_prenom,
                   dest.nom as dest_nom, dest.prenom as dest_prenom
            FROM messages m
            LEFT JOIN users exp ON m.expediteur_id = exp.id
            LEFT JOIN users dest ON m.destinataire_id = dest.id
            WHERE m.id = ?
        """;
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToMessage(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération du message: " + e.getMessage());
        }
        
        return null;
    }
    
    public boolean saveMessage(Message message) {
        if (message.getId() == 0) {
            return insertMessage(message);
        } else {
            return updateMessage(message);
        }
    }
    
    private boolean insertMessage(Message message) {
        String query = """
            INSERT INTO messages (expediteur_id, destinataire_id, objet, contenu,
                                date_envoi, lu, priorite, type_message, important, archive)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setInt(1, message.getExpediteur().getId());
            stmt.setInt(2, message.getDestinataire().getId());
            stmt.setString(3, message.getObjet());
            stmt.setString(4, message.getContenu());
            stmt.setTimestamp(5, Timestamp.valueOf(message.getDateEnvoi()));
            stmt.setBoolean(6, message.isLu());
            stmt.setString(7, message.getPriorite().name().toLowerCase());
            stmt.setString(8, message.getTypeMessage().name().toLowerCase());
            stmt.setBoolean(9, message.isImportant());
            stmt.setBoolean(10, message.isArchive());
            
            int affected = stmt.executeUpdate();
            
            if (affected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        message.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'insertion du message: " + e.getMessage());
        }
        
        return false;
    }
    
    private boolean updateMessage(Message message) {
        String query = """
            UPDATE messages SET lu = ?, date_lecture = ?, important = ?, archive = ?
            WHERE id = ?
        """;
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setBoolean(1, message.isLu());
            stmt.setTimestamp(2, message.getDateLecture() != null ?
                Timestamp.valueOf(message.getDateLecture()) : null);
            stmt.setBoolean(3, message.isImportant());
            stmt.setBoolean(4, message.isArchive());
            stmt.setInt(5, message.getId());
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour du message: " + e.getMessage());
        }
        
        return false;
    }
    
    public boolean deleteMessage(int id) {
        String query = "DELETE FROM messages WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression du message: " + e.getMessage());
        }
        
        return false;
    }
    
    private Message mapResultSetToMessage(ResultSet rs) throws SQLException {
        Message message = new Message();
        message.setId(rs.getInt("id"));
        message.setObjet(rs.getString("objet"));
        message.setContenu(rs.getString("contenu"));
        
        Timestamp dateEnvoi = rs.getTimestamp("date_envoi");
        if (dateEnvoi != null) {
            message.setDateEnvoi(dateEnvoi.toLocalDateTime());
        }
        
        Timestamp dateLecture = rs.getTimestamp("date_lecture");
        if (dateLecture != null) {
            message.setDateLecture(dateLecture.toLocalDateTime());
        }
        
        message.setLu(rs.getBoolean("lu"));
        message.setImportant(rs.getBoolean("important"));
        message.setArchive(rs.getBoolean("archive"));
        
        // Création des utilisateurs expéditeur et destinataire
        User expediteur = new User();
        expediteur.setId(rs.getInt("expediteur_id"));
        expediteur.setNom(rs.getString("exp_nom"));
        expediteur.setPrenom(rs.getString("exp_prenom"));
        message.setExpediteur(expediteur);
        
        User destinataire = new User();
        destinataire.setId(rs.getInt("destinataire_id"));
        destinataire.setNom(rs.getString("dest_nom"));
        destinataire.setPrenom(rs.getString("dest_prenom"));
        message.setDestinataire(destinataire);
        
        return message;
    }
}