package application.services;

import application.models.Message;
import application.models.StatutMessage;
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
        
        // Requête qui récupère les messages REÇUS et ENVOYÉS
        String query = """
            SELECT m.*, 
                   exp.id as exp_id, exp.code as exp_code, exp.nom as exp_nom, 
                   exp.prenom as exp_prenom, exp.email as exp_email,
                   dest.id as dest_id, dest.code as dest_code, dest.nom as dest_nom, 
                   dest.prenom as dest_prenom, dest.email as dest_email
            FROM messages m
            LEFT JOIN users exp ON m.expediteur_id = exp.id
            LEFT JOIN users dest ON m.destinataire_id = dest.id
            WHERE (m.destinataire_id = ? OR m.expediteur_id = ?)
            ORDER BY m.date_envoi DESC
        """;
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, userId);
            stmt.setInt(2, userId); // IMPORTANT: Récupérer aussi les messages envoyés
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    messages.add(mapResultSetToMessage(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des messages: " + e.getMessage());
            e.printStackTrace();
        }
        
        return messages;
    }
    
    public Message getMessageById(int id) {
        String query = """
            SELECT m.*, 
                   exp.id as exp_id, exp.code as exp_code, exp.nom as exp_nom, 
                   exp.prenom as exp_prenom, exp.email as exp_email,
                   dest.id as dest_id, dest.code as dest_code, dest.nom as dest_nom, 
                   dest.prenom as dest_prenom, dest.email as dest_email
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
                                date_envoi, lu, priorite, type_message, important, 
                                archive, statut, piece_jointe, reponse_a)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
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
            
            // CORRECTION: Gérer le statut correctement
            String statut = message.getStatut() != null ? 
                           message.getStatut().name().toLowerCase() : 
                           StatutMessage.ENVOYE.name().toLowerCase();
            stmt.setString(11, statut);
            
            stmt.setString(12, message.getPieceJointe());
            
            if (message.getReponseA() != null) {
                stmt.setInt(13, message.getReponseA());
            } else {
                stmt.setNull(13, Types.INTEGER);
            }
            
            int affected = stmt.executeUpdate();
            
            if (affected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        message.setId(generatedKeys.getInt(1));
                    }
                }
                
                System.out.println("✅ Message inséré avec succès - ID: " + message.getId() + 
                                 ", Statut: " + statut);
                return true;
            }
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de l'insertion du message: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    private boolean updateMessage(Message message) {
        String query = """
            UPDATE messages SET 
                lu = ?, 
                date_lecture = ?, 
                important = ?, 
                archive = ?,
                statut = ?,
                objet = ?,
                contenu = ?,
                priorite = ?,
                piece_jointe = ?
            WHERE id = ?
        """;
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setBoolean(1, message.isLu());
            stmt.setTimestamp(2, message.getDateLecture() != null ?
                Timestamp.valueOf(message.getDateLecture()) : null);
            stmt.setBoolean(3, message.isImportant());
            stmt.setBoolean(4, message.isArchive());
            
            String statut = message.getStatut() != null ? 
                           message.getStatut().name().toLowerCase() : 
                           StatutMessage.ENVOYE.name().toLowerCase();
            stmt.setString(5, statut);
            
            stmt.setString(6, message.getObjet());
            stmt.setString(7, message.getContenu());
            stmt.setString(8, message.getPriorite().name().toLowerCase());
            stmt.setString(9, message.getPieceJointe());
            stmt.setInt(10, message.getId());
            
            int affected = stmt.executeUpdate();
            System.out.println("✅ Message mis à jour - ID: " + message.getId() + 
                             ", Lignes affectées: " + affected);
            
            return affected > 0;
            
        } catch (SQLException e) {
            System.err.println("❌ Erreur lors de la mise à jour du message: " + e.getMessage());
            e.printStackTrace();
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
        
        // CORRECTION: Mapper le statut correctement
        String statutStr = rs.getString("statut");
        if (statutStr != null) {
            message.setStatut(StatutMessage.fromDatabase(statutStr));
        }
        
        // Mapper la priorité
        String prioriteStr = rs.getString("priorite");
        if (prioriteStr != null) {
            message.setPriorite(application.models.PrioriteMessage.fromDatabase(prioriteStr));
        }
        
        // Mapper le type
        String typeStr = rs.getString("type_message");
        if (typeStr != null) {
            message.setTypeMessage(application.models.TypeMessage.fromDatabase(typeStr));
        }
        
        // Pièce jointe
        message.setPieceJointe(rs.getString("piece_jointe"));
        
        // Réponse à
        int reponseA = rs.getInt("reponse_a");
        if (!rs.wasNull()) {
            message.setReponseA(reponseA);
        }
        
        // Création des utilisateurs expéditeur et destinataire
        User expediteur = new User();
        expediteur.setId(rs.getInt("exp_id"));
        expediteur.setCode(rs.getString("exp_code"));
        expediteur.setNom(rs.getString("exp_nom"));
        expediteur.setPrenom(rs.getString("exp_prenom"));
        expediteur.setEmail(rs.getString("exp_email"));
        message.setExpediteur(expediteur);
        
        User destinataire = new User();
        destinataire.setId(rs.getInt("dest_id"));
        destinataire.setCode(rs.getString("dest_code"));
        destinataire.setNom(rs.getString("dest_nom"));
        destinataire.setPrenom(rs.getString("dest_prenom"));
        destinataire.setEmail(rs.getString("dest_email"));
        message.setDestinataire(destinataire);
        
        return message;
    }
}