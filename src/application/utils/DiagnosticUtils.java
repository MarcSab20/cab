package application.utils;

import java.net.URL;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.sql.SQLException;

import application.services.DatabaseService;

/**
 * Utilitaires de diagnostic complet du système
 * Vérifie l'état du système et initialise automatiquement les composants
 * Version améliorée avec vérifications ressources, classes, base de données et structure documentaire
 */
public class DiagnosticUtils {
    
    private static final String SEPARATEUR = "═".repeat(65);
    private static final String LIGNE = "─".repeat(65);
    
    /**
     * Exécute un diagnostic complet de l'application
     */
    public static void diagnosticComplet() {
        System.out.println();
        System.out.println("╔" + SEPARATEUR + "╗");
        System.out.println("║  DIAGNOSTIC COMPLET - Système de Gestion Documentaire          ║");
        System.out.println("╚" + SEPARATEUR + "╝");
        System.out.println();
        
        // 1. Environnement Java
        diagnosticEnvironnementJava();
        
        // 2. Ressources de l'application
        diagnosticRessources();
        
        // 3. Classes essentielles
        diagnosticClasses();
        
        // 4. Base de données
        diagnosticDatabase();
        
        // 5. Services applicatifs
        diagnosticServices();
        
        System.out.println();
        System.out.println("╔" + SEPARATEUR + "╗");
        System.out.println("║  FIN DU DIAGNOSTIC - Système prêt                               ║");
        System.out.println("╚" + SEPARATEUR + "╝");
        System.out.println();
    }
    
    /**
     * Diagnostic de l'environnement Java et JavaFX
     */
    private static void diagnosticEnvironnementJava() {
        afficherTitre("1. ENVIRONNEMENT JAVA");
        
        String javaVersion = System.getProperty("java.version");
        String javaVendor = System.getProperty("java.vendor");
        String javaHome = System.getProperty("java.home");
        String javafxVersion = System.getProperty("javafx.version", "Non détecté");
        String osName = System.getProperty("os.name");
        String osVersion = System.getProperty("os.version");
        String osArch = System.getProperty("os.arch");
        String userDir = System.getProperty("user.dir");
        String fileEncoding = System.getProperty("file.encoding");
        
        afficherInfo("Version Java", javaVersion);
        afficherInfo("Fournisseur", javaVendor);
        afficherInfo("JAVA_HOME", javaHome);
        afficherInfo("JavaFX Version", javafxVersion);
        afficherInfo("Système", osName + " " + osVersion + " (" + osArch + ")");
        afficherInfo("Répertoire", userDir);
        afficherInfo("Encodage fichiers", fileEncoding);
        
        // Vérification de l'encodage
        if (!fileEncoding.toLowerCase().contains("utf")) {
            System.out.println();
            afficherWarning("L'encodage n'est pas UTF-8. Les emojis peuvent ne pas s'afficher correctement.");
            afficherInfo("Solution", "Ajoutez -Dfile.encoding=UTF-8 aux arguments de la JVM");
        }
        
        System.out.println();
    }
    
    /**
     * Diagnostic des ressources de l'application (FXML, CSS, images)
     */
    private static void diagnosticRessources() {
        afficherTitre("2. RESSOURCES DE L'APPLICATION");
        
        int trouvees = 0;
        int totales = 0;
        
        // Fichiers FXML
        trouvees += verifierRessource("/application/views/login.fxml", "Login FXML");
        totales++;
        trouvees += verifierRessource("/application/views/main.fxml", "Main FXML");
        totales++;
        trouvees += verifierRessource("/application/views/documents.fxml", "Documents FXML");
        totales++;
        trouvees += verifierRessource("/application/views/courriers.fxml", "Courriers FXML");
        totales++;
        trouvees += verifierRessource("/application/views/reunions.fxml", "Réunions FXML");
        totales++;
        trouvees += verifierRessource("/application/views/messages.fxml", "Messages FXML");
        totales++;
        trouvees += verifierRessource("/application/views/users.fxml", "Users FXML");
        totales++;
        
        // Fichiers CSS
        trouvees += verifierRessource("/application/styles/application.css", "CSS principal");
        totales++;
        
        // Images
        trouvees += verifierRessource("/application/images/logo.png", "Logo");
        totales++;
        
        System.out.println();
        afficherInfo("Résumé ressources", trouvees + "/" + totales + " fichiers trouvés");
        
        System.out.println();
    }
    
    /**
     * Diagnostic des classes essentielles
     */
    private static void diagnosticClasses() {
        afficherTitre("3. CLASSES ESSENTIELLES");
        
        int trouvees = 0;
        int totales = 0;
        
        // Modèles
        trouvees += verifierClasse("application.models.User", "User");
        totales++;
        trouvees += verifierClasse("application.models.Role", "Role");
        totales++;
        trouvees += verifierClasse("application.models.Document", "Document");
        totales++;
        trouvees += verifierClasse("application.models.Dossier", "Dossier");
        totales++;
        trouvees += verifierClasse("application.models.Courrier", "Courrier");
        totales++;
        
        // Services
        trouvees += verifierClasse("application.services.DatabaseService", "DatabaseService");
        totales++;
        trouvees += verifierClasse("application.services.AuthenticationService", "AuthenticationService");
        totales++;
        trouvees += verifierClasse("application.services.UserService", "UserService");
        totales++;
        trouvees += verifierClasse("application.services.DocumentService", "DocumentService");
        totales++;
        trouvees += verifierClasse("application.services.DossierService", "DossierService");
        totales++;
        trouvees += verifierClasse("application.services.StructureInitService", "StructureInitService");
        totales++;
        
        // Utilitaires
        trouvees += verifierClasse("application.utils.SessionManager", "SessionManager");
        totales++;
        trouvees += verifierClasse("application.utils.AlertUtils", "AlertUtils");
        totales++;
        
        // Contrôleurs
        trouvees += verifierClasse("application.controllers.LoginController", "LoginController");
        totales++;
        trouvees += verifierClasse("application.controllers.MainController", "MainController");
        totales++;
        trouvees += verifierClasse("application.controllers.DocumentsController", "DocumentsController");
        totales++;
        
        System.out.println();
        afficherInfo("Résumé classes", trouvees + "/" + totales + " classes trouvées");
        
        System.out.println();
    }
    
    /**
     * Diagnostic de la connexion à la base de données
     */
    private static void diagnosticDatabase() {
        afficherTitre("4. BASE DE DONNÉES");
        
        try {
            DatabaseService dbService = DatabaseService.getInstance();
            
            if (!dbService.isInitialized()) {
                afficherWarning("Base de données non initialisée");
                System.out.print("  → Initialisation en cours... ");
                dbService.initialize();
                afficherSucces("OK");
            }
            
            try (Connection conn = dbService.getConnection()) {
                DatabaseMetaData metaData = conn.getMetaData();
                
                afficherInfo("Connexion", "✓ Active");
                afficherInfo("SGBD", metaData.getDatabaseProductName());
                afficherInfo("Version", metaData.getDatabaseProductVersion());
                afficherInfo("Driver JDBC", metaData.getDriverName() + " " + metaData.getDriverVersion());
                afficherInfo("URL", metaData.getURL());
                afficherInfo("Utilisateur", metaData.getUserName());
                
                // Vérifier l'encodage de la base
                try (ResultSet rs = conn.createStatement().executeQuery(
                    "SELECT @@character_set_database, @@collation_database")) {
                    if (rs.next()) {
                        String charset = rs.getString(1);
                        String collation = rs.getString(2);
                        afficherInfo("Encodage DB", charset + " / " + collation);
                        
                        if (!charset.equalsIgnoreCase("utf8mb4")) {
                            System.out.println();
                            afficherWarning("L'encodage de la base devrait être utf8mb4 pour supporter les emojis");
                        }
                    }
                }
                
                // Compter les tables
                int tableCount = 0;
                try (ResultSet tables = metaData.getTables(null, null, "%", new String[]{"TABLE"})) {
                    while (tables.next()) {
                        tableCount++;
                    }
                }
                afficherInfo("Tables", String.valueOf(tableCount));
                
                // Compter les données
                afficherStatistiquesBase(conn);
                
            }
            
        } catch (Exception e) {
            afficherErreur("Erreur: " + e.getMessage());
            e.printStackTrace();
        }
        
        System.out.println();
    }
    
    /**
     * Affiche les statistiques de la base de données
     */
    private static void afficherStatistiquesBase(Connection conn) {
        try {
            System.out.println();
            System.out.println("  Statistiques:");
            
            // Utilisateurs actifs
            try (ResultSet rs = conn.createStatement().executeQuery(
                "SELECT COUNT(*) FROM users WHERE actif = 1")) {
                if (rs.next()) {
                    System.out.println("    • Utilisateurs actifs  : " + rs.getInt(1));
                }
            }
            
            // Dossiers
            try (ResultSet rs = conn.createStatement().executeQuery(
                "SELECT COUNT(*) FROM dossiers WHERE actif = 1")) {
                if (rs.next()) {
                    System.out.println("    • Dossiers            : " + rs.getInt(1));
                }
            }
            
            // Documents
            try (ResultSet rs = conn.createStatement().executeQuery(
                "SELECT COUNT(*) FROM documents WHERE statut != 'supprime'")) {
                if (rs.next()) {
                    System.out.println("    • Documents           : " + rs.getInt(1));
                }
            }
            
            // Courriers
            try (ResultSet rs = conn.createStatement().executeQuery(
                "SELECT COUNT(*) FROM courriers")) {
                if (rs.next()) {
                    System.out.println("    • Courriers           : " + rs.getInt(1));
                }
            }
            
        } catch (SQLException e) {
            // Ignorer si les tables n'existent pas encore
        }
    }
    
    /**
     * Diagnostic des services applicatifs
     */
    private static void diagnosticServices() {
        afficherTitre("5. SERVICES APPLICATIFS");
        
        try {
            // DatabaseService
            DatabaseService dbService = DatabaseService.getInstance();
            afficherInfo("DatabaseService", 
                dbService.isInitialized() ? "✓ Initialisé" : "✗ Non initialisé");
            
            // Test connexion
            boolean connected = dbService.testConnection();
            afficherInfo("Test connexion", connected ? "✓ OK" : "✗ Échec");
            
        } catch (Exception e) {
            afficherErreur("Erreur: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    // ========================================================================
    // MÉTHODES UTILITAIRES PRIVÉES
    // ========================================================================
    
    /**
     * Vérifie si une ressource existe
     * @return 1 si trouvée, 0 sinon
     */
    private static int verifierRessource(String chemin, String description) {
        try {
            URL ressource = DiagnosticUtils.class.getResource(chemin);
            if (ressource != null) {
                afficherInfo(description, "✓ Trouvé");
                return 1;
            } else {
                afficherInfo(description, "✗ Non trouvé");
                return 0;
            }
        } catch (Exception e) {
            afficherInfo(description, "⚠ Erreur: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Vérifie si une classe existe
     * @return 1 si trouvée, 0 sinon
     */
    private static int verifierClasse(String nomClasse, String description) {
        try {
            Class.forName(nomClasse);
            afficherInfo(description, "✓ Trouvée");
            return 1;
        } catch (ClassNotFoundException e) {
            afficherInfo(description, "✗ Non trouvée");
            return 0;
        } catch (Exception e) {
            afficherInfo(description, "⚠ Erreur: " + e.getMessage());
            return 0;
        }
    }
    
    /**
     * Affiche un titre de section
     */
    private static void afficherTitre(String titre) {
        System.out.println("┌" + LIGNE + "┐");
        System.out.println("│ " + String.format("%-63s", titre) + " │");
        System.out.println("└" + LIGNE + "┘");
    }
    
    /**
     * Affiche une information formatée
     */
    private static void afficherInfo(String label, String valeur) {
        String formattedLabel = String.format("%-20s", "  • " + label);
        System.out.println(formattedLabel + ": " + valeur);
    }
    
    /**
     * Affiche un message de succès
     */
    private static void afficherSucces(String message) {
        System.out.println("✓");
    }
    
    /**
     * Affiche un avertissement
     */
    private static void afficherWarning(String message) {
        System.out.println("  ⚠️  ATTENTION: " + message);
    }
    
    /**
     * Affiche une erreur
     */
    private static void afficherErreur(String message) {
        System.out.println("  ✗ " + message);
    }
    
    // ========================================================================
    // MÉTHODES PUBLIQUES SUPPLÉMENTAIRES
    // ========================================================================
    
    /**
     * Affiche uniquement les informations sur l'environnement
     */
    public static void afficherInfoEnvironnement() {
        afficherTitre("INFORMATIONS ENVIRONNEMENT");
        
        afficherInfo("Java Version", System.getProperty("java.version"));
        afficherInfo("JavaFX Version", System.getProperty("javafx.version", "Non détecté"));
        afficherInfo("OS", System.getProperty("os.name"));
        afficherInfo("Répertoire", System.getProperty("user.dir"));
        afficherInfo("Encodage", System.getProperty("file.encoding"));
        
        System.out.println();
    }
    
    /**
     * Vérifie uniquement les ressources
     */
    public static void verifierRessources() {
        diagnosticRessources();
    }
    
    /**
     * Vérifie uniquement les classes
     */
    public static void verifierClasses() {
        diagnosticClasses();
    }
    
    /**
     * Affiche uniquement les statistiques de la base
     */
    public static void afficherStatistiques() {
        afficherTitre("STATISTIQUES");
        
        try (Connection conn = DatabaseService.getInstance().getConnection()) {
            afficherStatistiquesBase(conn);
        } catch (SQLException e) {
            afficherErreur("Erreur: " + e.getMessage());
        }
        
        System.out.println();
    }
    
    /**
     * Vérifie la santé globale du système
     * @return true si tous les composants sont opérationnels
     */
    public static boolean verifierSanteSysteme() {
        try {
            DatabaseService dbService = DatabaseService.getInstance();
            
            // Vérifier la connexion
            if (!dbService.testConnection()) {
                return false;
            }
            
            // Vérifier que les tables essentielles existent
            try (Connection conn = dbService.getConnection()) {
                DatabaseMetaData metaData = conn.getMetaData();
                
                String[] tablesRequises = {"users", "roles", "dossiers", "documents"};
                
                for (String table : tablesRequises) {
                    try (ResultSet rs = metaData.getTables(null, null, table, null)) {
                        if (!rs.next()) {
                            System.err.println("✗ Table manquante: " + table);
                            return false;
                        }
                    }
                }
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("✗ Erreur lors de la vérification: " + e.getMessage());
            return false;
        }
    }
    
    /**
     * Test rapide de la configuration
     * @return true si la configuration de base est OK
     */
    public static boolean testRapide() {
        System.out.println("🔍 Test rapide de configuration...");
        
        boolean ok = true;
        
        // Test encodage
        String encoding = System.getProperty("file.encoding");
        if (!encoding.toLowerCase().contains("utf")) {
            System.out.println("  ✗ Encodage incorrect: " + encoding);
            ok = false;
        } else {
            System.out.println("  ✓ Encodage OK: " + encoding);
        }
        
        // Test connexion DB
        try {
            if (DatabaseService.getInstance().testConnection()) {
                System.out.println("  ✓ Base de données accessible");
            } else {
                System.out.println("  ✗ Base de données inaccessible");
                ok = false;
            }
        } catch (Exception e) {
            System.out.println("  ✗ Erreur DB: " + e.getMessage());
            ok = false;
        }
        
        System.out.println(ok ? "✅ Configuration OK" : "❌ Configuration incorrecte");
        System.out.println();
        
        return ok;
    }
}