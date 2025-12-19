package application.services;

import application.models.Dossier;
import application.models.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

/**
 * Service de gestion des dossiers documentaires
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
     * Récupère tous les dossiers actifs avec statistiques
     */
    public List<Dossier> getAllDossiers() {
        List<Dossier> dossiers = new ArrayList<>();
        String query = "SELECT * FROM v_arborescence_dossiers ORDER BY ordre_affichage";
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                dossiers.add(mapResultSetToDossier(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des dossiers: " + e.getMessage());
        }
        
        return dossiers;
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
            System.err.println("Erreur lors de la récupération du dossier: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Récupère un dossier par son code
     */
    public Dossier getDossierByCode(String code) {
        String query = "SELECT * FROM v_arborescence_dossiers WHERE code_dossier = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, code);
            
            try (ResultSet rs = stmt.executeQuery()) {
                if (rs.next()) {
                    return mapResultSetToDossier(rs);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération du dossier par code: " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Récupère les sous-dossiers d'un dossier parent
     */
    public List<Dossier> getSousDossiers(int parentId) {
        List<Dossier> sousDossiers = new ArrayList<>();
        String query = "SELECT * FROM v_arborescence_dossiers WHERE dossier_parent_id = ? ORDER BY ordre_affichage";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, parentId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sousDossiers.add(mapResultSetToDossier(rs));
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des sous-dossiers: " + e.getMessage());
        }
        
        return sousDossiers;
    }
    
    /**
     * Récupère les dossiers racines (sans parent)
     */
    public List<Dossier> getDossiersRacine() {
        List<Dossier> dossiers = new ArrayList<>();
        String query = "SELECT * FROM v_arborescence_dossiers WHERE dossier_parent_id = 1 ORDER BY ordre_affichage";
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                dossiers.add(mapResultSetToDossier(rs));
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la récupération des dossiers racines: " + e.getMessage());
        }
        
        return dossiers;
    }
    
    /**
     * Crée un nouveau dossier
     */
    public boolean createDossier(Dossier dossier, User user) {
        // Générer le code et le chemin si nécessaire
        if (dossier.getCodeDossier() == null || dossier.getCodeDossier().isEmpty()) {
            dossier.setCodeDossier(genererCodeDossier(dossier.getNomDossier()));
        }
        
        if (dossier.getCheminComplet() == null) {
            dossier.setCheminComplet(construireCheminComplet(dossier));
        }
        
        String query = """
            INSERT INTO dossiers (code_dossier, nom_dossier, dossier_parent_id, chemin_complet, 
                                 description, icone, ordre_affichage, actif, systeme, cree_par)
            VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            stmt.setString(1, dossier.getCodeDossier());
            stmt.setString(2, dossier.getNomDossier());
            
            if (dossier.getDossierParentId() != null) {
                stmt.setInt(3, dossier.getDossierParentId());
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            
            stmt.setString(4, dossier.getCheminComplet());
            stmt.setString(5, dossier.getDescription());
            stmt.setString(6, dossier.getIcone());
            stmt.setInt(7, dossier.getOrdreAffichage());
            stmt.setBoolean(8, dossier.isActif());
            stmt.setBoolean(9, dossier.isSysteme());
            
            if (user != null) {
                stmt.setInt(10, user.getId());
            } else {
                stmt.setNull(10, Types.INTEGER);
            }
            
            int affected = stmt.executeUpdate();
            
            if (affected > 0) {
                try (ResultSet generatedKeys = stmt.getGeneratedKeys()) {
                    if (generatedKeys.next()) {
                        dossier.setId(generatedKeys.getInt(1));
                    }
                }
                return true;
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la création du dossier: " + e.getMessage());
            e.printStackTrace();
        }
        
        return false;
    }
    
    /**
     * Met à jour un dossier
     */
    public boolean updateDossier(Dossier dossier) {
        String query = """
            UPDATE dossiers SET 
                nom_dossier = ?, description = ?, icone = ?, ordre_affichage = ?, 
                date_modification = ?
            WHERE id = ? AND systeme = FALSE
        """;
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, dossier.getNomDossier());
            stmt.setString(2, dossier.getDescription());
            stmt.setString(3, dossier.getIcone());
            stmt.setInt(4, dossier.getOrdreAffichage());
            stmt.setTimestamp(5, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(6, dossier.getId());
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la mise à jour du dossier: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Supprime un dossier (uniquement si non-système et vide)
     */
    public boolean deleteDossier(int dossierId, User user) {
        // Vérifier si le dossier peut être supprimé
        Dossier dossier = getDossierById(dossierId);
        
        if (dossier == null) {
            System.err.println("Dossier introuvable");
            return false;
        }
        
        if (dossier.isSysteme()) {
            System.err.println("Impossible de supprimer un dossier système");
            return false;
        }
        
        if (!dossier.estVide()) {
            System.err.println("Le dossier doit être vide pour être supprimé");
            return false;
        }
        
        // Vérifier les permissions (niveau 0 ou 1 uniquement)
        if (user == null || user.getNiveauAutorite() > 1) {
            System.err.println("Permissions insuffisantes pour supprimer un dossier");
            return false;
        }
        
        String query = "DELETE FROM dossiers WHERE id = ? AND systeme = FALSE";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, dossierId);
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors de la suppression du dossier: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Déplace un dossier vers un nouveau parent
     */
    public boolean deplacerDossier(int dossierId, int nouveauParentId) {
        Dossier dossier = getDossierById(dossierId);
        
        if (dossier == null || dossier.isSysteme()) {
            return false;
        }
        
        // Construire le nouveau chemin
        String nouveauChemin = construireNouveauChemin(dossierId, nouveauParentId);
        
        String query = """
            UPDATE dossiers SET 
                dossier_parent_id = ?, chemin_complet = ?, date_modification = ?
            WHERE id = ? AND systeme = FALSE
        """;
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, nouveauParentId);
            stmt.setString(2, nouveauChemin);
            stmt.setTimestamp(3, Timestamp.valueOf(LocalDateTime.now()));
            stmt.setInt(4, dossierId);
            
            return stmt.executeUpdate() > 0;
        } catch (SQLException e) {
            System.err.println("Erreur lors du déplacement du dossier: " + e.getMessage());
        }
        
        return false;
    }
    
    // Méthodes utilitaires privées
    
    /**
     * Génère un code de dossier à partir du nom
     */
    private String genererCodeDossier(String nomDossier) {
        String code = nomDossier
            .toUpperCase()
            .replaceAll("[^A-Z0-9]", "_")
            .replaceAll("_+", "_");
        
        // Vérifier l'unicité
        int suffix = 1;
        String codeBase = code;
        
        while (getDossierByCode(code) != null) {
            code = codeBase + "_" + suffix++;
        }
        
        return code;
    }
    
    /**
     * Construit le chemin complet d'un dossier
     */
    private String construireCheminComplet(Dossier dossier) {
        if (dossier.getDossierParentId() == null) {
            return "/ROOT/" + dossier.getCodeDossier();
        }
        
        Dossier parent = getDossierById(dossier.getDossierParentId());
        
        if (parent == null) {
            return "/ROOT/" + dossier.getCodeDossier();
        }
        
        return parent.getCheminComplet() + "/" + dossier.getCodeDossier();
    }
    
    /**
     * Construit le nouveau chemin lors d'un déplacement
     */
    private String construireNouveauChemin(int dossierId, int nouveauParentId) {
        Dossier dossier = getDossierById(dossierId);
        Dossier nouveauParent = getDossierById(nouveauParentId);
        
        if (dossier == null || nouveauParent == null) {
            return null;
        }
        
        return nouveauParent.getCheminComplet() + "/" + dossier.getCodeDossier();
    }
    
    /**
     * Mappe un ResultSet vers un objet Dossier
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
        dossier.setIcone(rs.getString("icone"));
        dossier.setOrdreAffichage(rs.getInt("ordre_affichage"));
        dossier.setActif(rs.getBoolean("actif"));
        dossier.setSysteme(rs.getBoolean("systeme"));
        dossier.setNombreDocuments(rs.getInt("nombre_documents"));
        dossier.setNombreSousDossiers(rs.getInt("nombre_sous_dossiers"));
        
        return dossier;
    }
}