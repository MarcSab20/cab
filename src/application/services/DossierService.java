package application.services;

import application.models.Dossier;
import application.models.User;

import java.sql.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de gestion des dossiers avec contrôle des permissions
 */
public class DossierService {
    
    private static DossierService instance;
    private final DatabaseService databaseService;
    
    private DossierService() {
        this.databaseService = DatabaseService.getInstance();
    }
    
    public static synchronized DossierService getInstance() {
        if (instance == null) {
            instance = new DossierService();
        }
        return instance;
    }
    
    /**
     * Vérifie si un utilisateur peut créer des dossiers
     * Niveaux 0 et 1 uniquement
     */
    public boolean peutCreerDossier(User user) {
        return user.getNiveauAutorite() <= 1;
    }
    
    /**
     * Vérifie si un utilisateur peut supprimer des dossiers
     * Niveau 0 uniquement
     */
    public boolean peutSupprimerDossier(User user) {
        return user.getNiveauAutorite() == 0;
    }
    
    /**
     * Vérifie si un utilisateur peut accéder à un dossier
     * Dossier CONFIDENTIEL : niveau 0 uniquement
     */
    public boolean peutAccederDossier(User user, Dossier dossier) {
        if (dossier.getCodeDossier().equals("CONFIDENTIEL")) {
            return user.getNiveauAutorite() == 0;
        }
        return true;
    }
    
    /**
     * Crée un nouveau dossier
     */
    public Dossier createDossier(Dossier dossier, User user) throws Exception {
        // Vérifier les permissions
        if (!peutCreerDossier(user)) {
            throw new Exception("Vous n'avez pas les permissions pour créer un dossier");
        }
        
        // Validation
        if (dossier.getCodeDossier() == null || dossier.getCodeDossier().trim().isEmpty()) {
            throw new Exception("Le code du dossier est obligatoire");
        }
        
        if (dossier.getNomDossier() == null || dossier.getNomDossier().trim().isEmpty()) {
            throw new Exception("Le nom du dossier est obligatoire");
        }
        
        // Construire le chemin complet
        if (dossier.getDossierParentId() != null && dossier.getDossierParentId() > 0) {
            Dossier parent = getDossierById(dossier.getDossierParentId());
            if (parent != null) {
                dossier.setCheminComplet(parent.getCheminComplet() + "/" + dossier.getCodeDossier());
            }
        } else {
            dossier.setCheminComplet("/ROOT/" + dossier.getCodeDossier());
        }
        
        // Insérer en base
        insertDossier(dossier, user.getId());
        
        System.out.println("✓ Dossier créé: " + dossier.getCodeDossier());
        
        return dossier;
    }
    
    /**
     * Insère un dossier en base de données
     */
    private void insertDossier(Dossier dossier, int userId) throws SQLException {
        String query = "INSERT INTO dossiers (code_dossier, nom_dossier, dossier_parent_id, " +
                      "chemin_complet, description, icone, ordre_affichage, systeme, cree_par) " +
                      "VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?)";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, dossier.getCodeDossier());
            stmt.setString(2, dossier.getNomDossier());
            
            if (dossier.getDossierParentId() != null && dossier.getDossierParentId() > 0) {
                stmt.setInt(3, dossier.getDossierParentId());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            
            stmt.setString(4, dossier.getCheminComplet());
            stmt.setString(5, dossier.getDescription());
            stmt.setString(6, dossier.getIcone() != null ? dossier.getIcone() : "📁");
            stmt.setInt(7, dossier.getOrdreAffichage());
            stmt.setBoolean(8, dossier.isSysteme());
            stmt.setInt(9, userId);
            
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    dossier.setId(rs.getInt(1));
                }
            }
        }
    }
    
    /**
     * Supprime un dossier (soft delete)
     */
    public boolean deleteDossier(int dossierId, User user) throws Exception {
        // Vérifier les permissions
        if (!peutSupprimerDossier(user)) {
            throw new Exception("Seul le CEMAA peut supprimer des dossiers");
        }
        
        // Vérifier que ce n'est pas un dossier système
        Dossier dossier = getDossierById(dossierId);
        if (dossier != null && dossier.isSysteme()) {
            throw new Exception("Impossible de supprimer un dossier système");
        }
        
        // Vérifier qu'il n'y a pas de documents
        if (dossierContientDocuments(dossierId)) {
            throw new Exception("Le dossier contient des documents. Veuillez les déplacer avant de supprimer le dossier.");
        }
        
        // Suppression (marquer comme inactif)
        String query = "UPDATE dossiers SET actif = FALSE WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, dossierId);
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Erreur suppression dossier: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Récupère un dossier par son ID
     */
    public Dossier getDossierById(int id) {
        String query = "SELECT * FROM v_arborescence_dossiers WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, id);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToDossier(rs);
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération dossier: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Récupère tous les dossiers actifs
     */
    public List<Dossier> getAllDossiers() {
        List<Dossier> dossiers = new ArrayList<>();
        String query = "SELECT * FROM v_arborescence_dossiers ORDER BY chemin_complet, ordre_affichage";
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                dossiers.add(mapResultSetToDossier(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération dossiers: " + e.getMessage());
        }
        
        return dossiers;
    }
    
    /**
     * Récupère les dossiers racines (sans parent)
     */
    public List<Dossier> getDossiersRacines() {
        List<Dossier> dossiers = new ArrayList<>();
        String query = "SELECT * FROM v_arborescence_dossiers WHERE dossier_parent_id IS NULL " +
                      "ORDER BY ordre_affichage";
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                dossiers.add(mapResultSetToDossier(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération dossiers racines: " + e.getMessage());
        }
        
        return dossiers;
    }
    
    /**
     * Récupère les sous-dossiers d'un dossier parent
     */
    public List<Dossier> getSousDossiers(int parentId) {
        List<Dossier> dossiers = new ArrayList<>();
        String query = "SELECT * FROM v_arborescence_dossiers WHERE dossier_parent_id = ? " +
                      "ORDER BY ordre_affichage";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, parentId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    dossiers.add(mapResultSetToDossier(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération sous-dossiers: " + e.getMessage());
        }
        
        return dossiers;
    }
    
    /**
     * Recherche de dossiers
     */
    public List<Dossier> rechercherDossiers(String recherche) {
        List<Dossier> dossiers = new ArrayList<>();
        String query = "SELECT * FROM v_arborescence_dossiers WHERE " +
                      "code_dossier LIKE ? OR nom_dossier LIKE ? " +
                      "ORDER BY chemin_complet";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            String pattern = "%" + recherche + "%";
            stmt.setString(1, pattern);
            stmt.setString(2, pattern);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    dossiers.add(mapResultSetToDossier(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur recherche dossiers: " + e.getMessage());
        }
        
        return dossiers;
    }
    
    /**
     * Vérifie si un dossier contient des documents
     */
    private boolean dossierContientDocuments(int dossierId) {
        String query = "SELECT COUNT(*) FROM documents WHERE dossier_id = ? AND statut != 'supprime'";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, dossierId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return rs.getInt(1) > 0;
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur vérification documents: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Convertit un ResultSet en objet Dossier
     */
    private Dossier mapResultSetToDossier(ResultSet rs) throws SQLException {
        Dossier dossier = new Dossier();
        
        dossier.setId(rs.getInt("id"));
        dossier.setCodeDossier(rs.getString("code_dossier"));
        dossier.setNomDossier(rs.getString("nom_dossier"));
        
        int parentId = rs.getInt("dossier_parent_id");
        if (!rs.wasNull()) {
            dossier.setDossierParentId(parentId);
        }
        
        dossier.setCheminComplet(rs.getString("chemin_complet"));
        dossier.setDescription(rs.getString("description"));
        dossier.setIcone(rs.getString("icone"));
        dossier.setOrdreAffichage(rs.getInt("ordre_affichage"));
        dossier.setActif(rs.getBoolean("actif"));
        dossier.setSysteme(rs.getBoolean("systeme"));
        
        int creePar = rs.getInt("cree_par");
        if (!rs.wasNull()) {
            dossier.setCreePar(creePar);
        }
        
        Timestamp dateCreation = rs.getTimestamp("date_creation");
        if (dateCreation != null) {
            dossier.setDateCreation(dateCreation.toLocalDateTime());
        }
        
        Timestamp dateModification = rs.getTimestamp("date_modification");
        if (dateModification != null) {
            dossier.setDateModification(dateModification.toLocalDateTime());
        }
        
        // Informations supplémentaires de la vue
        dossier.setNombreDocuments(rs.getInt("nombre_documents"));
        dossier.setNombreSousDossiers(rs.getInt("nombre_sous_dossiers"));
        dossier.setNomDossierParent(rs.getString("nom_dossier_parent"));
        dossier.setNiveauHierarchie(rs.getInt("niveau_hierarchie"));
        
        return dossier;
    }
}