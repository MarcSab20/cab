package application.services;

import application.models.Dossier;
import application.models.User;

import java.sql.*;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Service d'initialisation de la structure documentaire
 * Crée automatiquement la hiérarchie de dossiers système
 * Gère l'encodage UTF-8 pour les emojis
 */
public class StructureInitService {
    
    private static StructureInitService instance;
    private final DatabaseService databaseService;
    private final DossierService dossierService;
    
    // Structure de dossiers à créer
    private static final Map<String, DossierDefinition> STRUCTURE = new HashMap<>();
    
    static {
        // Définition de la structure complète
        
        // RACINE
        STRUCTURE.put("ROOT", new DossierDefinition(
            "ROOT", "Racine", null, "🏠", 
            "Dossier racine du système", 0, true
        ));
        
        // NIVEAU 1 - Dossiers principaux
        STRUCTURE.put("COURRIERS_ENTRANTS", new DossierDefinition(
            "COURRIERS_ENTRANTS", "Courriers Entrants", "ROOT", "📥",
            "Tous les courriers entrants", 1, true
        ));
        
        STRUCTURE.put("COURRIERS_SORTANTS", new DossierDefinition(
            "COURRIERS_SORTANTS", "Courriers Sortants", "ROOT", "📤",
            "Tous les courriers sortants", 2, true
        ));
        
        STRUCTURE.put("DOCUMENTS_INTERNES", new DossierDefinition(
            "DOCUMENTS_INTERNES", "Documents Internes", "ROOT", "📋",
            "Documents internes de service", 3, true
        ));
        
        STRUCTURE.put("ARCHIVES_ANNUELLES", new DossierDefinition(
            "ARCHIVES_ANNUELLES", "Archives Annuelles", "ROOT", "📦",
            "Archives classées par année", 4, true
        ));
        
        STRUCTURE.put("CORBEILLE", new DossierDefinition(
            "CORBEILLE", "Corbeille", "ROOT", "🗑️",
            "Documents supprimés", 99, true
        ));
        
        // NIVEAU 2 - Sous-dossiers de COURRIERS_ENTRANTS
        STRUCTURE.put("PERM", new DossierDefinition(
            "PERM", "Permissions", "COURRIERS_ENTRANTS", "📝",
            "Demandes de permissions", 1, false
        ));
        
        STRUCTURE.put("AUTH", new DossierDefinition(
            "AUTH", "Autorisations", "COURRIERS_ENTRANTS", "✅",
            "Autorisations diverses", 2, false
        ));
        
        STRUCTURE.put("DEM", new DossierDefinition(
            "DEM", "Demandes", "COURRIERS_ENTRANTS", "📄",
            "Demandes administratives", 3, false
        ));
        
        STRUCTURE.put("MP_IN", new DossierDefinition(
            "MP_IN", "Messages Porte", "COURRIERS_ENTRANTS", "📨",
            "Messages porte entrants", 4, false
        ));
        
        STRUCTURE.put("CORR", new DossierDefinition(
            "CORR", "Correspondances", "COURRIERS_ENTRANTS", "✉️",
            "Correspondances officielles", 5, false
        ));
        
        // NIVEAU 2 - Sous-dossiers de COURRIERS_SORTANTS
        STRUCTURE.put("COMM", new DossierDefinition(
            "COMM", "Communiqués", "COURRIERS_SORTANTS", "📢",
            "Communiqués officiels", 1, false
        ));
        
        STRUCTURE.put("CIRC", new DossierDefinition(
            "CIRC", "Circulaires", "COURRIERS_SORTANTS", "📃",
            "Circulaires administratives", 2, false
        ));
        
        STRUCTURE.put("MP_OUT", new DossierDefinition(
            "MP_OUT", "Messages Porte", "COURRIERS_SORTANTS", "📤",
            "Messages porte sortants", 3, false
        ));
        
        STRUCTURE.put("CONV", new DossierDefinition(
            "CONV", "Convocations", "COURRIERS_SORTANTS", "📅",
            "Convocations officielles", 4, false
        ));
        
        STRUCTURE.put("REP", new DossierDefinition(
            "REP", "Réponses Officielles", "COURRIERS_SORTANTS", "↩️",
            "Réponses aux courriers", 5, false
        ));
        
        STRUCTURE.put("NOTIF", new DossierDefinition(
            "NOTIF", "Notifications", "COURRIERS_SORTANTS", "🔔",
            "Notifications administratives", 6, false
        ));
        
        // NIVEAU 2 - Sous-dossiers de DOCUMENTS_INTERNES
        STRUCTURE.put("RAPP", new DossierDefinition(
            "RAPP", "Rapports Mensuels", "DOCUMENTS_INTERNES", "📊",
            "Rapports mensuels d'activité", 1, false
        ));
        
        STRUCTURE.put("PV", new DossierDefinition(
            "PV", "Procès-Verbaux", "DOCUMENTS_INTERNES", "📋",
            "Procès-verbaux de réunions", 2, false
        ));
        
        STRUCTURE.put("NOTE", new DossierDefinition(
            "NOTE", "Notes de Service", "DOCUMENTS_INTERNES", "📝",
            "Notes de service internes", 3, false
        ));
        
        STRUCTURE.put("CR", new DossierDefinition(
            "CR", "Comptes Rendus", "DOCUMENTS_INTERNES", "📄",
            "Comptes rendus divers", 4, false
        ));
        
        // NIVEAU 2 - Sous-dossiers de ARCHIVES_ANNUELLES
        int currentYear = java.time.Year.now().getValue();
        for (int year = currentYear - 1; year <= currentYear + 1; year++) {
            STRUCTURE.put("ARCH_" + year, new DossierDefinition(
                "ARCH_" + year, String.valueOf(year), "ARCHIVES_ANNUELLES", "📁",
                "Archives de l'année " + year, year - currentYear + 2, false
            ));
        }
    }
    
    private StructureInitService() {
        this.databaseService = DatabaseService.getInstance();
        this.dossierService = DossierService.getInstance();
    }
    
    public static synchronized StructureInitService getInstance() {
        if (instance == null) {
            instance = new StructureInitService();
        }
        return instance;
    }
    
    /**
     * Initialise la structure complète des dossiers
     * @param forceReinit Force la réinitialisation même si la structure existe
     * @return true si succès
     */
    public boolean initialiserStructure(boolean forceReinit) {
        try {
            System.out.println("=== Initialisation de la structure documentaire ===");
            
            // Vérifier si la structure existe déjà
            if (!forceReinit && structureExiste()) {
                System.out.println("✓ Structure déjà initialisée");
                return true;
            }
            
            if (forceReinit) {
                System.out.println("⚠️ Réinitialisation forcée de la structure");
            }
            
            // Créer les dossiers dans l'ordre hiérarchique
            Map<String, Integer> dossiersCreated = new HashMap<>();
            
            // Créer d'abord ROOT
            Integer rootId = creerDossierAvecEncodageUTF8("ROOT", null, dossiersCreated);
            if (rootId == null) {
                System.err.println("✗ Échec de création du dossier ROOT");
                return false;
            }
            
            // Créer les dossiers de niveau 1
            for (String code : STRUCTURE.keySet()) {
                DossierDefinition def = STRUCTURE.get(code);
                if (def.parentCode != null && def.parentCode.equals("ROOT") && !code.equals("ROOT")) {
                    creerDossierAvecEncodageUTF8(code, dossiersCreated.get("ROOT"), dossiersCreated);
                }
            }
            
            // Créer les dossiers de niveau 2 et +
            for (String code : STRUCTURE.keySet()) {
                DossierDefinition def = STRUCTURE.get(code);
                if (def.parentCode != null && !def.parentCode.equals("ROOT") && !dossiersCreated.containsKey(code)) {
                    Integer parentId = dossiersCreated.get(def.parentCode);
                    if (parentId != null) {
                        creerDossierAvecEncodageUTF8(code, parentId, dossiersCreated);
                    }
                }
            }
            
            System.out.println("✓ Structure créée: " + dossiersCreated.size() + " dossiers");
            afficherStructure();
            
            return true;
            
        } catch (Exception e) {
            System.err.println("✗ Erreur lors de l'initialisation: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Crée un dossier avec encodage UTF-8 correct pour les emojis
     */
    private Integer creerDossierAvecEncodageUTF8(String code, Integer parentId, Map<String, Integer> created) {
        DossierDefinition def = STRUCTURE.get(code);
        if (def == null) return null;
        
        // Vérifier si le dossier existe déjà
        Dossier existant = dossierService.getDossierByCode(code);
        if (existant != null) {
            created.put(code, existant.getId());
            
            // Mettre à jour l'icône si nécessaire (correction d'encodage)
            if (existant.getIcone() == null || existant.getIcone().contains("?")) {
                updateIconeUTF8(existant.getId(), def.icone);
            }
            
            return existant.getId();
        }
        
        // Construire le chemin complet
        String cheminComplet = construireCheminComplet(code, parentId, created);
        
        String query = """
            INSERT INTO dossiers (
                code_dossier, nom_dossier, dossier_parent_id, chemin_complet,
                description, icone, ordre_affichage, actif, systeme, cree_par
            ) VALUES (?, ?, ?, ?, ?, ?, ?, ?, ?, ?)
        """;
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query, Statement.RETURN_GENERATED_KEYS)) {
            
            // S'assurer que la connexion utilise UTF-8
            try (Statement setCharset = conn.createStatement()) {
                setCharset.execute("SET NAMES utf8mb4");
                setCharset.execute("SET CHARACTER SET utf8mb4");
            }
            
            stmt.setString(1, def.code);
            stmt.setString(2, def.nom);
            
            if (parentId != null) {
                stmt.setInt(3, parentId);
            } else {
                stmt.setNull(3, Types.INTEGER);
            }
            
            stmt.setString(4, cheminComplet);
            stmt.setString(5, def.description);
            stmt.setString(6, def.icone); // L'emoji sera correctement encodé en UTF-8
            stmt.setInt(7, def.ordreAffichage);
            stmt.setBoolean(8, true); // actif
            stmt.setBoolean(9, def.systeme);
            stmt.setInt(10, 1); // user admin
            
            stmt.executeUpdate();
            
            try (ResultSet rs = stmt.getGeneratedKeys()) {
                if (rs.next()) {
                    int id = rs.getInt(1);
                    created.put(code, id);
                    System.out.println("✓ Créé: " + def.icone + " " + def.nom + " (ID: " + id + ")");
                    return id;
                }
            }
            
        } catch (SQLException e) {
            System.err.println("✗ Erreur création dossier " + code + ": " + e.getMessage());
        }
        
        return null;
    }
    
    /**
     * Met à jour l'icône d'un dossier avec encodage UTF-8
     */
    private void updateIconeUTF8(int dossierId, String icone) {
        String query = "UPDATE dossiers SET icone = ? WHERE id = ?";
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query)) {
            
            // S'assurer que la connexion utilise UTF-8
            try (Statement setCharset = conn.createStatement()) {
                setCharset.execute("SET NAMES utf8mb4");
                setCharset.execute("SET CHARACTER SET utf8mb4");
            }
            
            stmt.setString(1, icone);
            stmt.setInt(2, dossierId);
            stmt.executeUpdate();
            
        } catch (SQLException e) {
            System.err.println("Erreur mise à jour icône: " + e.getMessage());
        }
    }
    
    /**
     * Construit le chemin complet d'un dossier
     */
    private String construireCheminComplet(String code, Integer parentId, Map<String, Integer> created) {
        if (code.equals("ROOT")) {
            return "/ROOT";
        }
        
        DossierDefinition def = STRUCTURE.get(code);
        if (def.parentCode == null) {
            return "/ROOT/" + code;
        }
        
        // Trouver le chemin du parent
        if (def.parentCode.equals("ROOT")) {
            return "/ROOT/" + code;
        }
        
        // Récupérer le chemin du parent depuis la base
        if (parentId != null) {
            Dossier parent = dossierService.getDossierById(parentId);
            if (parent != null) {
                return parent.getCheminComplet() + "/" + code;
            }
        }
        
        return "/ROOT/" + def.parentCode + "/" + code;
    }
    
    /**
     * Vérifie si la structure existe déjà
     */
    private boolean structureExiste() {
        try {
            Dossier root = dossierService.getDossierByCode("ROOT");
            if (root == null) return false;
            
            // Vérifier quelques dossiers clés
            return dossierService.getDossierByCode("COURRIERS_ENTRANTS") != null &&
                   dossierService.getDossierByCode("COURRIERS_SORTANTS") != null &&
                   dossierService.getDossierByCode("DOCUMENTS_INTERNES") != null;
                   
        } catch (Exception e) {
            return false;
        }
    }
    
    /**
     * Affiche la structure créée
     */
    public void afficherStructure() {
        System.out.println("\n=== Structure des dossiers ===");
        afficherDossierRecursif(dossierService.getDossierByCode("ROOT"), 0);
    }
    
    /**
     * Affiche un dossier et ses enfants récursivement
     */
    private void afficherDossierRecursif(Dossier dossier, int niveau) {
        if (dossier == null) return;
        
        String indent = "  ".repeat(niveau);
        System.out.println(indent + dossier.getIcone() + " " + dossier.getNomDossier() + 
                          " (" + dossier.getNombreDocuments() + " docs)");
        
        // Afficher les sous-dossiers
        List<Dossier> sousDossiers = dossierService.getSousDossiers(dossier.getId());
        for (Dossier sd : sousDossiers) {
            afficherDossierRecursif(sd, niveau + 1);
        }
    }
    
    /**
     * Répare l'encodage des icônes existantes
     */
    public boolean reparerEncodageIcones() {
        System.out.println("=== Réparation de l'encodage des icônes ===");
        
        try {
            List<Dossier> dossiers = dossierService.getAllDossiers();
            int repaired = 0;
            
            for (Dossier dossier : dossiers) {
                DossierDefinition def = STRUCTURE.get(dossier.getCodeDossier());
                
                if (def != null) {
                    String iconeActuelle = dossier.getIcone();
                    
                    // Vérifier si l'icône est corrompue
                    if (iconeActuelle == null || iconeActuelle.contains("?") || 
                        iconeActuelle.equals("�") || iconeActuelle.isEmpty()) {
                        
                        updateIconeUTF8(dossier.getId(), def.icone);
                        repaired++;
                        System.out.println("✓ Réparé: " + dossier.getNomDossier() + " → " + def.icone);
                    }
                }
            }
            
            System.out.println("✓ " + repaired + " icônes réparées");
            return true;
            
        } catch (Exception e) {
            System.err.println("✗ Erreur lors de la réparation: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Classe interne pour définir un dossier
     */
    private static class DossierDefinition {
        String code;
        String nom;
        String parentCode;
        String icone;
        String description;
        int ordreAffichage;
        boolean systeme;
        
        DossierDefinition(String code, String nom, String parentCode, String icone, 
                         String description, int ordreAffichage, boolean systeme) {
            this.code = code;
            this.nom = nom;
            this.parentCode = parentCode;
            this.icone = icone;
            this.description = description;
            this.ordreAffichage = ordreAffichage;
            this.systeme = systeme;
        }
    }
}