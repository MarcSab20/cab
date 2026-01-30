package application.services;

import application.models.CategorieExpediteur;
import application.models.SousCategorieExpediteur;
import application.models.DestinataireStandard;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service pour gérer les expéditeurs et destinataires
 */
public class ExpediteurDestinataireService {
    
    private static ExpediteurDestinataireService instance;
    private final DatabaseService databaseService;
    
    private ExpediteurDestinataireService() {
        this.databaseService = DatabaseService.getInstance();
    }
    
    public static synchronized ExpediteurDestinataireService getInstance() {
        if (instance == null) {
            instance = new ExpediteurDestinataireService();
        }
        return instance;
    }
    
    // ==================== GESTION DES CATÉGORIES D'EXPÉDITEURS ====================
    
    /**
     * Récupère toutes les catégories d'expéditeurs actives
     */
    public List<CategorieExpediteur> getCategoriesExpediteurs() {
        List<CategorieExpediteur> categories = new ArrayList<>();
        String query = "SELECT * FROM categories_expediteurs WHERE actif = TRUE " +
                      "ORDER BY ordre_affichage, libelle";
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                categories.add(mapResultSetToCategorie(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération catégories: " + e.getMessage());
            e.printStackTrace();
        }
        
        return categories;
    }
    
    /**
     * Récupère les sous-catégories pour une catégorie donnée
     */
    public List<SousCategorieExpediteur> getSousCategories(int categorieId) {
        List<SousCategorieExpediteur> sousCategories = new ArrayList<>();
        String query = "SELECT * FROM sous_categories_expediteurs " +
                      "WHERE categorie_id = ? AND actif = TRUE " +
                      "ORDER BY ordre_affichage, libelle";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, categorieId);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    sousCategories.add(mapResultSetToSousCategorie(rs));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération sous-catégories: " + e.getMessage());
            e.printStackTrace();
        }
        
        return sousCategories;
    }
    
    /**
     * Récupère toutes les catégories avec leurs sous-catégories
     */
    public Map<CategorieExpediteur, List<SousCategorieExpediteur>> getCategoriesAvecSousCategories() {
        Map<CategorieExpediteur, List<SousCategorieExpediteur>> result = new HashMap<>();
        
        List<CategorieExpediteur> categories = getCategoriesExpediteurs();
        for (CategorieExpediteur cat : categories) {
            if (cat.isASousCategories()) {
                result.put(cat, getSousCategories(cat.getId()));
            } else {
                result.put(cat, new ArrayList<>());
            }
        }
        
        return result;
    }
    
    // ==================== GESTION DES EXPÉDITEURS PERSONNALISÉS ====================
    
    /**
     * Récupère les expéditeurs personnalisés triés par fréquence d'utilisation
     */
    public List<String> getExpediteursPersonnalises(int limite) {
        List<String> expediteurs = new ArrayList<>();
        String query = "SELECT nom FROM expediteurs_personnalises WHERE actif = TRUE " +
                      "ORDER BY nombre_utilisations DESC, nom LIMIT ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, limite);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    expediteurs.add(rs.getString("nom"));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération expéditeurs personnalisés: " + e.getMessage());
        }
        
        return expediteurs;
    }
    
    /**
     * Ajoute un expéditeur personnalisé ou met à jour sa fréquence
     */
    public boolean ajouterOuMettreAJourExpediteur(String nom) {
        if (nom == null || nom.trim().isEmpty()) {
            return false;
        }
        
        String query = "INSERT INTO expediteurs_personnalises (nom, nombre_utilisations, derniere_utilisation) " +
                      "VALUES (?, 1, NOW()) " +
                      "ON DUPLICATE KEY UPDATE " +
                      "nombre_utilisations = nombre_utilisations + 1, " +
                      "derniere_utilisation = NOW()";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, nom.trim());
            stmt.executeUpdate();
            
            return true;
            
        } catch (SQLException e) {
            System.err.println("Erreur ajout expéditeur personnalisé: " + e.getMessage());
            return false;
        }
    }
    
    // ==================== GESTION DES DESTINATAIRES ====================
    
    /**
     * Récupère tous les destinataires standards
     */
    public List<DestinataireStandard> getDestinatairesStandards() {
        List<DestinataireStandard> destinataires = new ArrayList<>();
        String query = "SELECT * FROM destinataires_standards WHERE actif = TRUE " +
                      "ORDER BY nombre_utilisations DESC, ordre_affichage, libelle";
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                destinataires.add(mapResultSetToDestinataire(rs));
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération destinataires: " + e.getMessage());
            e.printStackTrace();
        }
        
        return destinataires;
    }
    
    /**
     * Récupère les destinataires personnalisés
     */
    public List<String> getDestinatairesPersonnalises(int limite) {
        List<String> destinataires = new ArrayList<>();
        String query = "SELECT nom FROM destinataires_personnalises WHERE actif = TRUE " +
                      "ORDER BY nombre_utilisations DESC, nom LIMIT ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setInt(1, limite);
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    destinataires.add(rs.getString("nom"));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur récupération destinataires personnalisés: " + e.getMessage());
        }
        
        return destinataires;
    }
    
    /**
     * Met à jour la fréquence d'utilisation d'un destinataire standard
     */
    public boolean incrementerUtilisationDestinataire(String code) {
        String query = "UPDATE destinataires_standards SET " +
                      "nombre_utilisations = nombre_utilisations + 1, " +
                      "derniere_utilisation = NOW() " +
                      "WHERE code = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, code);
            return stmt.executeUpdate() > 0;
            
        } catch (SQLException e) {
            System.err.println("Erreur mise à jour destinataire: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Ajoute un destinataire personnalisé ou met à jour sa fréquence
     */
    public boolean ajouterOuMettreAJourDestinataire(String nom) {
        if (nom == null || nom.trim().isEmpty()) {
            return false;
        }
        
        String query = "INSERT INTO destinataires_personnalises (nom, nombre_utilisations, derniere_utilisation) " +
                      "VALUES (?, 1, NOW()) " +
                      "ON DUPLICATE KEY UPDATE " +
                      "nombre_utilisations = nombre_utilisations + 1, " +
                      "derniere_utilisation = NOW()";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            stmt.setString(1, nom.trim());
            stmt.executeUpdate();
            
            return true;
            
        } catch (SQLException e) {
            System.err.println("Erreur ajout destinataire personnalisé: " + e.getMessage());
            return false;
        }
    }
    
    // ==================== MÉTHODES DE MAPPING ====================
    
    private CategorieExpediteur mapResultSetToCategorie(ResultSet rs) throws SQLException {
        CategorieExpediteur cat = new CategorieExpediteur();
        cat.setId(rs.getInt("id"));
        cat.setCode(rs.getString("code"));
        cat.setLibelle(rs.getString("libelle"));
        cat.setOrdreAffichage(rs.getInt("ordre_affichage"));
        cat.setASousCategories(rs.getBoolean("a_sous_categories"));
        cat.setActif(rs.getBoolean("actif"));
        return cat;
    }
    
    private SousCategorieExpediteur mapResultSetToSousCategorie(ResultSet rs) throws SQLException {
        SousCategorieExpediteur sousCat = new SousCategorieExpediteur();
        sousCat.setId(rs.getInt("id"));
        sousCat.setCategorieId(rs.getInt("categorie_id"));
        sousCat.setCode(rs.getString("code"));
        sousCat.setLibelle(rs.getString("libelle"));
        sousCat.setOrdreAffichage(rs.getInt("ordre_affichage"));
        sousCat.setActif(rs.getBoolean("actif"));
        return sousCat;
    }
    
    private DestinataireStandard mapResultSetToDestinataire(ResultSet rs) throws SQLException {
        DestinataireStandard dest = new DestinataireStandard();
        dest.setId(rs.getInt("id"));
        dest.setCode(rs.getString("code"));
        dest.setLibelle(rs.getString("libelle"));
        dest.setNombreUtilisations(rs.getInt("nombre_utilisations"));
        dest.setOrdreAffichage(rs.getInt("ordre_affichage"));
        
        Timestamp derniere = rs.getTimestamp("derniere_utilisation");
        if (derniere != null) {
            dest.setDerniereUtilisation(derniere.toLocalDateTime());
        }
        
        dest.setActif(rs.getBoolean("actif"));
        return dest;
    }
}