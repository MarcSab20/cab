package application.services;

import application.models.Courrier;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de gestion des courriers
 */
public class CourrierService {
    private static CourrierService instance;
    
    public CourrierService() {}
    
    public static synchronized CourrierService getInstance() {
        if (instance == null) {
            instance = new CourrierService();
        }
        return instance;
    }
    
    /**
     * Récupère tous les courriers en workflow actif
     */
    public List<Courrier> getAllCourriersEnWorkflow() {
        List<Courrier> courriers = new ArrayList<>();
        
        String sql = "SELECT * FROM courriers WHERE workflow_actif = 1 ORDER BY date_reception DESC";
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Courrier courrier = DatabaseService.mapResultSetToCourrier(rs);
                courriers.add(courrier);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return courriers;
    }
    
    /**
     * Récupère un courrier par son ID
     */
    public Courrier getCourrierById(int id) {
        String sql = "SELECT * FROM courriers WHERE id = ?";
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return DatabaseService.mapResultSetToCourrier(rs);
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return null;
    }
    
    /**
     * Récupère tous les courriers
     */
    public List<Courrier> getAllCourriers() {
        List<Courrier> courriers = new ArrayList<>();
        
        String sql = "SELECT * FROM courriers ORDER BY date_reception DESC";
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql);
             ResultSet rs = stmt.executeQuery()) {
            
            while (rs.next()) {
                Courrier courrier = DatabaseService.mapResultSetToCourrier(rs);
                courriers.add(courrier);
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return courriers;
    }
    
    /**
     * Enregistre un nouveau courrier
     */
    public boolean saveCourrier(Courrier courrier) {
        String sql = "INSERT INTO courriers (numero_courrier, type_courrier, objet, expediteur, " +
                     "destinataire, date_reception, date_document, statut, priorite, " +
                     "workflow_actif, service_actuel, etape_actuelle) " +
                     "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, courrier.getNumeroCourrier());
            stmt.setString(2, courrier.getTypeCourrier().toString());
            stmt.setString(3, courrier.getObjet());
            stmt.setString(4, courrier.getExpediteur());
            stmt.setString(5, courrier.getDestinataire());
            
            if (courrier.getDateReception() != null) {
                stmt.setTimestamp(6, Timestamp.valueOf(courrier.getDateReception()));
            } else {
                stmt.setTimestamp(6, Timestamp.valueOf(LocalDateTime.now()));
            }
            
            if (courrier.getDateDocument() != null) {
                stmt.setTimestamp(7, Timestamp.valueOf(courrier.getDateDocument()));
            } else {
                stmt.setNull(7, Types.TIMESTAMP);
            }
            
            stmt.setString(8, courrier.getStatut().toString());
            stmt.setString(9, courrier.getPriorite());
            stmt.setBoolean(10, courrier.isWorkflowActif());
            stmt.setString(11, courrier.getServiceActuel());
            
            if (courrier.getEtapeActuelle() != null) {
                stmt.setInt(12, courrier.getEtapeActuelle());
            } else {
                stmt.setNull(12, Types.INTEGER);
            }
            
            int affectedRows = stmt.executeUpdate();
            
            if (affectedRows > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        courrier.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Met à jour un courrier existant
     */
    public boolean updateCourrier(Courrier courrier) {
        String sql = "UPDATE courriers SET " +
                     "numero_courrier = ?, type_courrier = ?, objet = ?, expediteur = ?, " +
                     "destinataire = ?, date_reception = ?, date_document = ?, statut = ?, " +
                     "priorite = ?, workflow_actif = ?, service_actuel = ?, etape_actuelle = ?, " +
                     "workflow_termine = ? " +
                     "WHERE id = ?";
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setString(1, courrier.getNumeroCourrier());
            stmt.setString(2, courrier.getTypeCourrier().toString());
            stmt.setString(3, courrier.getObjet());
            stmt.setString(4, courrier.getExpediteur());
            stmt.setString(5, courrier.getDestinataire());
            
            if (courrier.getDateReception() != null) {
                stmt.setTimestamp(6, Timestamp.valueOf(courrier.getDateReception()));
            } else {
                stmt.setNull(6, Types.TIMESTAMP);
            }
            
            if (courrier.getDateDocument() != null) {
                stmt.setTimestamp(7, Timestamp.valueOf(courrier.getDateDocument()));
            } else {
                stmt.setNull(7, Types.TIMESTAMP);
            }
            
            stmt.setString(8, courrier.getStatut().toString());
            stmt.setString(9, courrier.getPriorite());
            stmt.setBoolean(10, courrier.isWorkflowActif());
            stmt.setString(11, courrier.getServiceActuel());
            
            if (courrier.getEtapeActuelle() != null) {
                stmt.setInt(12, courrier.getEtapeActuelle());
            } else {
                stmt.setNull(12, Types.INTEGER);
            }
            
            stmt.setBoolean(13, courrier.isWorkflowTermine());
            stmt.setInt(14, courrier.getId());
            
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Supprime un courrier
     */
    public boolean deleteCourrier(int id) {
        String sql = "DELETE FROM courriers WHERE id = ?";
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql)) {
            
            stmt.setInt(1, id);
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Recherche des courriers selon des critères
     */
    public List<Courrier> searchCourriers(String keyword, String statut, String priorite) {
        List<Courrier> courriers = new ArrayList<>();
        StringBuilder sql = new StringBuilder("SELECT * FROM courriers WHERE 1=1");
        
        if (keyword != null && !keyword.isEmpty()) {
            sql.append(" AND (numero_courrier LIKE ? OR objet LIKE ? OR expediteur LIKE ?)");
        }
        
        if (statut != null && !statut.isEmpty()) {
            sql.append(" AND statut = ?");
        }
        
        if (priorite != null && !priorite.isEmpty()) {
            sql.append(" AND priorite = ?");
        }
        
        sql.append(" ORDER BY date_reception DESC");
        
        try (Connection conn = DatabaseService.getInstance().getConnection();
             PreparedStatement stmt = conn.prepareStatement(sql.toString())) {
            
            int paramIndex = 1;
            
            if (keyword != null && !keyword.isEmpty()) {
                String searchPattern = "%" + keyword + "%";
                stmt.setString(paramIndex++, searchPattern);
                stmt.setString(paramIndex++, searchPattern);
                stmt.setString(paramIndex++, searchPattern);
            }
            
            if (statut != null && !statut.isEmpty()) {
                stmt.setString(paramIndex++, statut);
            }
            
            if (priorite != null && !priorite.isEmpty()) {
                stmt.setString(paramIndex++, priorite);
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    Courrier courrier = DatabaseService.mapResultSetToCourrier(rs);
                    courriers.add(courrier);
                }
            }
            
        } catch (SQLException e) {
            e.printStackTrace();
        }
        
        return courriers;
    }
}