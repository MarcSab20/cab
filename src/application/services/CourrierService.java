package application.services;

import application.models.Courrier;
import application.models.StatutCourrier;
import application.models.TypeCourrier;
import application.models.PrioriteCourrier;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

public class CourrierService {
    
    private static CourrierService instance;
    private final DatabaseService databaseService;
    
    private CourrierService() {
        this.databaseService = DatabaseService.getInstance();
    }
    
    public static synchronized CourrierService getInstance() {
        if (instance == null) {
            instance = new CourrierService();
        }
        return instance;
    }
    
    public List<Courrier> getAllCourriers() {
        List<Courrier> courriers = new ArrayList<>();
        String query = "SELECT * FROM courriers ORDER BY date_reception DESC";
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                courriers.add(mapResultSetToCourrier(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des courriers: " + e.getMessage());
        }
        
        return courriers;
    }
    
    public Courrier getCourrierById(int id) {
        String query = "SELECT * FROM courriers WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToCourrier(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération du courrier: " + e.getMessage());
        }
        
        return null;
    }
    
    public boolean saveCourrier(Courrier courrier) {
        if (courrier.getId() == 0) {
            return insertCourrier(courrier);
        } else {
            return updateCourrier(courrier);
        }
    }
    
    private boolean insertCourrier(Courrier courrier) {
        String query = """
            INSERT INTO courriers (numero_courrier, type_courrier, objet, expediteur, 
                                 destinataire, date_courrier, date_reception, statut, priorite, notes)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, courrier.getNumeroCourrier());
            stmt.setString(2, courrier.getTypeCourrier().name().toLowerCase());
            stmt.setString(3, courrier.getObjet());
            stmt.setString(4, courrier.getExpediteur());
            stmt.setString(5, courrier.getDestinataire());
            stmt.setTimestamp(6, courrier.getDateCourrier() != null ? 
                Timestamp.valueOf(courrier.getDateCourrier()) : null);
            stmt.setTimestamp(7, Timestamp.valueOf(courrier.getDateReception()));
            stmt.setString(8, courrier.getStatut().name().toLowerCase());
            stmt.setString(9, courrier.getPriorite().name().toLowerCase());
            stmt.setString(10, courrier.getNotes());
            
            int affected = stmt.executeUpdate();
            
            if (affected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        courrier.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de l'insertion du courrier: " + e.getMessage());
        }
        
        return false;
    }
    
    private boolean updateCourrier(Courrier courrier) {
        String query = """
            UPDATE courriers SET objet = ?, expediteur = ?, destinataire = ?,
                               statut = ?, priorite = ?, notes = ?, date_traitement = ?
            WHERE id = ?
        """;
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, courrier.getObjet());
            stmt.setString(2, courrier.getExpediteur());
            stmt.setString(3, courrier.getDestinataire());
            stmt.setString(4, courrier.getStatut().name().toLowerCase());
            stmt.setString(5, courrier.getPriorite().name().toLowerCase());
            stmt.setString(6, courrier.getNotes());
            stmt.setTimestamp(7, courrier.getDateTraitement() != null ?
                Timestamp.valueOf(courrier.getDateTraitement()) : null);
            stmt.setInt(8, courrier.getId());
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour du courrier: " + e.getMessage());
        }
        
        return false;
    }
    
    public boolean deleteCourrier(int id) {
        String query = "DELETE FROM courriers WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression du courrier: " + e.getMessage());
        }
        
        return false;
    }
    
    private Courrier mapResultSetToCourrier(ResultSet rs) throws SQLException {
        Courrier courrier = new Courrier();
        courrier.setId(rs.getInt("id"));
        courrier.setNumeroCourrier(rs.getString("numero_courrier"));
        
        String type = rs.getString("type_courrier");
        courrier.setTypeCourrier(TypeCourrier.valueOf(type.toUpperCase()));
        
        courrier.setObjet(rs.getString("objet"));
        courrier.setExpediteur(rs.getString("expediteur"));
        courrier.setDestinataire(rs.getString("destinataire"));
        
        Timestamp dateCourrier = rs.getTimestamp("date_courrier");
        if (dateCourrier != null) {
            courrier.setDateCourrier(dateCourrier.toLocalDateTime());
        }
        
        Timestamp dateReception = rs.getTimestamp("date_reception");
        if (dateReception != null) {
            courrier.setDateReception(dateReception.toLocalDateTime());
        }
        
        Timestamp dateTraitement = rs.getTimestamp("date_traitement");
        if (dateTraitement != null) {
            courrier.setDateTraitement(dateTraitement.toLocalDateTime());
        }
        
        String statut = rs.getString("statut");
        courrier.setStatut(StatutCourrier.valueOf(statut.toUpperCase()));
        
        String priorite = rs.getString("priorite");
        courrier.setPriorite(PrioriteCourrier.valueOf(priorite.toUpperCase()));
        
        courrier.setNotes(rs.getString("notes"));
        
        return courrier;
    }
}