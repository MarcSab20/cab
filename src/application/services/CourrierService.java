package application.services;

import application.models.Courrier;
import application.models.Courrier.StatutCourrier;
import application.models.Courrier.TypeCourrier;
import application.models.Courrier.PrioriteCourrier;

import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de gestion des courriers
 * 
 * Workflow:
 * 1. Création courrier (statut NOUVEAU) - document obligatoire
 * 2. Traitement externe (statut EN_COURS puis TRAITE)
 * 3. Archivage (statut ARCHIVE) - choix dossier obligatoire
 */
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
    
    /**
     * Crée un nouveau courrier
     * Le document est OBLIGATOIRE
     * Le code est généré automatiquement: COU-ANNÉE-SÉQUENCE
     */
    public Courrier createCourrier(Courrier courrier) throws Exception {
        // Validation: document obligatoire
        if (courrier.getDocumentId() <= 0) {
            throw new Exception("Un document doit être associé au courrier");
        }
        
        // Validation: objet obligatoire
        if (courrier.getObjet() == null || courrier.getObjet().trim().isEmpty()) {
            throw new Exception("L'objet du courrier est obligatoire");
        }
        
        // Validation: type obligatoire
        if (courrier.getTypeCourrier() == null) {
            throw new Exception("Le type de courrier est obligatoire");
        }
        
        // Vérifier que le document existe
        if (!documentExiste(courrier.getDocumentId())) {
            throw new Exception("Le document spécifié n'existe pas");
        }
        
        // Générer le code courrier
        String codeCourrier = genererCodeCourrier();
        courrier.setCodeCourrier(codeCourrier);
        
        // Statut initial: NOUVEAU
        courrier.setStatut(StatutCourrier.NOUVEAU);
        
        String query = "INSERT INTO courriers (code_courrier, document_id, type_courrier, objet, " +
                      "expediteur, destinataire, reference, date_courrier, priorite, observations, " +
                      "confidentiel, statut, cree_par) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, courrier.getCodeCourrier());
            stmt.setInt(2, courrier.getDocumentId());
            stmt.setString(3, courrier.getTypeCourrier().name());
            stmt.setString(4, courrier.getObjet());
            stmt.setString(5, courrier.getExpediteur());
            stmt.setString(6, courrier.getDestinataire());
            stmt.setString(7, courrier.getReference());
            
            if (courrier.getDateCourrier() != null) {
                stmt.setDate(8, Date.valueOf(courrier.getDateCourrier()));
            } else {
                stmt.setNull(8, Types.DATE);
            }
            
            stmt.setString(9, courrier.getPriorite().name());
            stmt.setString(10, courrier.getObservations());
            stmt.setBoolean(11, courrier.isConfidentiel());
            stmt.setString(12, courrier.getStatut().name().toLowerCase());
            
            if (courrier.getCreePar() != null) {
                stmt.setInt(13, courrier.getCreePar());
            } else {
                stmt.setNull(13, Types.INTEGER);
            }
            
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    courrier.setId(rs.getInt(1));
                }
            }
            
            System.out.println("✓ Courrier créé: " + codeCourrier);
            
            if (courrier.getStatut() == StatutCourrier.NOUVEAU) {
                NotificationCourrierService notificationService = 
                    NotificationCourrierService.getInstance();
                
                boolean notified = notificationService.notifierNouveauCourrier(courrier);
                
                if (notified) {
                    System.out.println("✓ Responsable notifié pour le courrier: " + codeCourrier);
                }
            }
            return courrier;
            
        } catch (SQLException e) {
            System.err.println("✗ Erreur création courrier: " + e.getMessage());
            throw new Exception("Erreur lors de la création du courrier: " + e.getMessage());
        }
    }
    
    /**
     * Archive un courrier
     * Le courrier doit être au statut TRAITE
     * Le dossier de destination est OBLIGATOIRE
     */
    public boolean archiverCourrier(int courrierId, int dossierId, int userId) throws Exception {
        // Récupérer le courrier
        Courrier courrier = getCourrierById(courrierId);
        
        if (courrier == null) {
            throw new Exception("Courrier introuvable");
        }
        
        // Vérifier le statut
        if (!courrier.peutEtreArchive()) {
            throw new Exception("Le courrier doit être au statut TRAITE pour être archivé");
        }
        
        // Vérifier que le dossier est spécifié
        if (dossierId <= 0) {
            throw new Exception("Un dossier d'archivage doit être spécifié");
        }
        
        try (Connection conn = databaseService.getConnection()) {
            conn.setAutoCommit(false);
            
            try {
                // 1. Archiver le document lié
                String updateDoc = "UPDATE documents SET archive = TRUE, dossier_id = ?, " +
                                 "date_archivage = NOW() WHERE id = ?";
                
                try (PreparedStatement stmt = conn.prepareStatement(updateDoc)) {
                    stmt.setInt(1, dossierId);
                    stmt.setInt(2, courrier.getDocumentId());
                    stmt.executeUpdate();
                }
                
                // 2. Archiver le courrier
                String updateCourrier = "UPDATE courriers SET statut = 'archive', " +
                                      "date_archivage = NOW() WHERE id = ?";
                
                try (PreparedStatement stmt = conn.prepareStatement(updateCourrier)) {
                    stmt.setInt(1, courrierId);
                    stmt.executeUpdate();
                }
                
                // 3. Enregistrer l'activité
                String insertActivity = "INSERT INTO activites_documents (document_id, user_id, action, details) " +
                                      "VALUES (?, ?, 'archivage', 'Courrier archivé')";
                
                try (PreparedStatement stmt = conn.prepareStatement(insertActivity)) {
                    stmt.setInt(1, courrier.getDocumentId());
                    stmt.setInt(2, userId);
                    stmt.executeUpdate();
                }
                
                conn.commit();
                
                System.out.println("✓ Courrier " + courrier.getCodeCourrier() + " archivé");
                
                return true;
                
            } catch (SQLException e) {
                conn.rollback();
                throw e;
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Erreur archivage courrier: " + e.getMessage());
            throw new Exception("Erreur lors de l'archivage: " + e.getMessage());
        }
    }
    
    /**
     * Récupère un courrier par son ID
     */
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
            System.err.println("Erreur récupération courrier: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Récupère les courriers par statut
     */
    public List<Courrier> getCourriersByStatut(StatutCourrier statut) {
        List<Courrier> courriers = new ArrayList<>();
        String query = "SELECT * FROM courriers WHERE statut = ? ORDER BY date_creation DESC";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, statut.name().toLowerCase());
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    courriers.add(mapResultSetToCourrier(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération courriers: " + e.getMessage());
        }
        
        return courriers;
    }
    
    /**
     * Récupère les courriers prêts pour l'archivage (statut TRAITE)
     */
    public List<Courrier> getCourriersPretsArchivage() {
        return getCourriersByStatut(StatutCourrier.TRAITE);
    }
    
    /**
     * Recherche de courriers
     */
    public List<Courrier> rechercherCourriers(String recherche) {
        List<Courrier> courriers = new ArrayList<>();
        String query = "SELECT * FROM courriers WHERE " +
                      "code_courrier LIKE ? OR " +
                      "objet LIKE ? OR " +
                      "expediteur LIKE ? OR " +
                      "destinataire LIKE ? " +
                      "ORDER BY date_creation DESC";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            String pattern = "%" + recherche + "%";
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            stmt.setString(3, pattern);
            stmt.setString(4, pattern);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    courriers.add(mapResultSetToCourrier(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur recherche courriers: " + e.getMessage());
        }
        
        return courriers;
    }
    
    /**
     * Met à jour le statut d'un courrier
     */
    public boolean updateStatut(int courrierId, StatutCourrier nouveauStatut) {
        String query = "UPDATE courriers SET statut = ?, date_modification = NOW() WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, nouveauStatut.name().toLowerCase());
            stmt.setInt(2, courrierId);
            
            int rowsAffected = stmt.executeUpdate();
            return rowsAffected > 0;
            
        } catch (SQLException e) {
            System.err.println("Erreur mise à jour statut: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Génère un code courrier unique: COU-ANNÉE-SÉQUENCE
     */
    private String genererCodeCourrier() throws SQLException {
        int annee = java.time.Year.now().getValue();
        String prefix = "COU-" + annee + "-";
        
        String query = "SELECT MAX(CAST(SUBSTRING(code_courrier, ?) AS UNSIGNED)) " +
                      "FROM courriers WHERE code_courrier LIKE ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, prefix.length() + 1);
            stmt.setString(2, prefix + "%");
            
            try (ResultSet rs = stmt.executeQuery()) {
                int sequence = 1;
                if (rs.next()) {
                    int maxSeq = rs.getInt(1);
                    if (!rs.wasNull()) {
                        sequence = maxSeq + 1;
                    }
                }
                
                return prefix + String.format("%04d", sequence);
            }
        }
    }
    
    /**
     * Vérifie si un document existe
     */
    private boolean documentExiste(int documentId) {
        String query = "SELECT COUNT(*) FROM documents WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, documentId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur vérification document: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Convertit un ResultSet en objet Courrier
     */
    private Courrier mapResultSetToCourrier(ResultSet rs) throws SQLException {
        Courrier courrier = new Courrier();
        
        courrier.setId(rs.getInt("id"));
        courrier.setCodeCourrier(rs.getString("code_courrier"));
        courrier.setDocumentId(rs.getInt("document_id"));
        courrier.setTypeCourrier(TypeCourrier.valueOf(rs.getString("type_courrier")));
        courrier.setObjet(rs.getString("objet"));
        courrier.setExpediteur(rs.getString("expediteur"));
        courrier.setDestinataire(rs.getString("destinataire"));
        courrier.setReference(rs.getString("reference"));
        
        Date dateCourrier = rs.getDate("date_courrier");
        if (dateCourrier != null) {
            courrier.setDateCourrier(dateCourrier.toLocalDate());
        }
        
        courrier.setPriorite(PrioriteCourrier.valueOf(rs.getString("priorite")));
        courrier.setObservations(rs.getString("observations"));
        courrier.setConfidentiel(rs.getBoolean("confidentiel"));
        courrier.setStatut(StatutCourrier.valueOf(rs.getString("statut").toUpperCase()));
        
        Timestamp dateArchivage = rs.getTimestamp("date_archivage");
        if (dateArchivage != null) {
            courrier.setDateArchivage(dateArchivage.toLocalDateTime());
        }
        
        int creePar = rs.getInt("cree_par");
        if (!rs.wasNull()) {
            courrier.setCreePar(creePar);
        }
        
        Timestamp dateCreation = rs.getTimestamp("date_creation");
        if (dateCreation != null) {
            courrier.setDateCreation(dateCreation.toLocalDateTime());
        }
        
        Timestamp dateModification = rs.getTimestamp("date_modification");
        if (dateModification != null) {
            courrier.setDateModification(dateModification.toLocalDateTime());
        }
        
        return courrier;
    }
}