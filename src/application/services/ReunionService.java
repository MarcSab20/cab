package application.services;

import application.models.Reunion;
import application.models.StatutReunion;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class ReunionService {
    
    private static ReunionService instance;
    private final DatabaseService databaseService;
    
    private ReunionService() {
        this.databaseService = DatabaseService.getInstance();
    }
    
    public static synchronized ReunionService getInstance() {
        if (instance == null) {
            instance = new ReunionService();
        }
        return instance;
    }
    
    public List<Reunion> getAllReunions() {
        List<Reunion> reunions = new ArrayList<>();
        String query = "SELECT * FROM reunions ORDER BY date_reunion DESC";
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                reunions.add(mapResultSetToReunion(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des réunions: " + e.getMessage());
        }
        
        return reunions;
    }
    
    public Reunion getReunionById(int id) {
        String query = "SELECT * FROM reunions WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToReunion(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération de la réunion: " + e.getMessage());
        }
        
        return null;
    }
    
    public boolean saveReunion(Reunion reunion) {
        if (reunion.getId() == 0) {
            return insertReunion(reunion);
        } else {
            return updateReunion(reunion);
        }
    }
    
    private boolean insertReunion(Reunion reunion) {
        String query = """
            INSERT INTO reunions (titre, description, date_reunion, duree_minutes,
                                lieu, organisateur_id, statut, lien_visio,
                                ordre_du_jour, compte_rendu)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, reunion.getTitre());
            stmt.setString(2, reunion.getDescription());
            stmt.setTimestamp(3, Timestamp.valueOf(reunion.getDateReunion()));
            stmt.setInt(4, reunion.getDureeMinutes());
            stmt.setString(5, reunion.getLieu());
            stmt.setInt(6, reunion.getOrganisateur() != null ? reunion.getOrganisateur().getId() : null);
            stmt.setString(7, reunion.getStatut().name().toLowerCase());
            stmt.setString(8, reunion.getLienVisio());
            stmt.setString(9, reunion.getOrdreDuJour());
            stmt.setString(10, reunion.getCompteRendu());
            
            int affected = stmt.executeUpdate();
            
            if (affected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        reunion.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'insertion de la réunion: " + e.getMessage());
        }
        
        return false;
    }
    
    private boolean updateReunion(Reunion reunion) {
        String query = """
            UPDATE reunions SET titre = ?, description = ?, date_reunion = ?,
                              duree_minutes = ?, lieu = ?, statut = ?,
                              lien_visio = ?, ordre_du_jour = ?, compte_rendu = ?
            WHERE id = ?
        """;
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, reunion.getTitre());
            stmt.setString(2, reunion.getDescription());
            stmt.setTimestamp(3, Timestamp.valueOf(reunion.getDateReunion()));
            stmt.setInt(4, reunion.getDureeMinutes());
            stmt.setString(5, reunion.getLieu());
            stmt.setString(6, reunion.getStatut().name().toLowerCase());
            stmt.setString(7, reunion.getLienVisio());
            stmt.setString(8, reunion.getOrdreDuJour());
            stmt.setString(9, reunion.getCompteRendu());
            stmt.setInt(10, reunion.getId());
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour de la réunion: " + e.getMessage());
        }
        
        return false;
    }
    
    public boolean deleteReunion(int id) {
        String query = "DELETE FROM reunions WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression de la réunion: " + e.getMessage());
        }
        
        return false;
    }
    
    private Reunion mapResultSetToReunion(ResultSet rs) throws SQLException {
        Reunion reunion = new Reunion();
        reunion.setId(rs.getInt("id"));
        reunion.setTitre(rs.getString("titre"));
        reunion.setDescription(rs.getString("description"));
        
        Timestamp dateReunion = rs.getTimestamp("date_reunion");
        if (dateReunion != null) {
            reunion.setDateReunion(dateReunion.toLocalDateTime());
        }
        
        reunion.setDureeMinutes(rs.getInt("duree_minutes"));
        reunion.setLieu(rs.getString("lieu"));
        
        String statut = rs.getString("statut");
        reunion.setStatut(StatutReunion.valueOf(statut.toUpperCase()));
        
        reunion.setLienVisio(rs.getString("lien_visio"));
        reunion.setOrdreDuJour(rs.getString("ordre_du_jour"));
        reunion.setCompteRendu(rs.getString("compte_rendu"));
        
        return reunion;
    }
}