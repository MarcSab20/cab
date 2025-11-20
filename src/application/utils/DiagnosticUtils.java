package application.utils;

import java.net.URL;

/**
 * Classe utilitaire pour diagnostiquer les problèmes de configuration
 */
public class DiagnosticUtils {
    
    /**
     * Vérifie la disponibilité des ressources de l'application
     */
    public static void verifierRessources() {
        System.out.println("=== DIAGNOSTIC DES RESSOURCES ===");
        
        // Vérification des fichiers FXML
        verifierFichier("/application/views/login.fxml", "Login FXML");
        verifierFichier("/views/login.fxml", "Login FXML (chemin alternatif)");
        verifierFichier("/application/views/main.fxml", "Main FXML");
        verifierFichier("/views/main.fxml", "Main FXML (chemin alternatif)");
        
        // Vérification des fichiers CSS
        verifierFichier("/application/styles/application.css", "CSS principal");
        verifierFichier("/styles/application.css", "CSS (chemin alternatif)");
        
        // Vérification des images
        verifierFichier("/application/images/logo.png", "Logo");
        verifierFichier("/images/logo.png", "Logo (chemin alternatif)");
        
        System.out.println("=== FIN DU DIAGNOSTIC ===");
    }
    
    /**
     * Vérifie si un fichier ressource existe
     */
    private static void verifierFichier(String chemin, String description) {
        try {
            URL ressource = DiagnosticUtils.class.getResource(chemin);
            if (ressource != null) {
                System.out.println("✅ " + description + " : TROUVÉ - " + ressource);
            } else {
                System.out.println("❌ " + description + " : NON TROUVÉ - " + chemin);
            }
        } catch (Exception e) {
            System.out.println("⚠️ " + description + " : ERREUR - " + e.getMessage());
        }
    }
    
    /**
     * Affiche des informations sur l'environnement
     */
    public static void afficherInfoEnvironnement() {
        System.out.println("=== INFORMATIONS ENVIRONNEMENT ===");
        System.out.println("Java Version: " + System.getProperty("java.version"));
        System.out.println("JavaFX Version: " + System.getProperty("javafx.version"));
        System.out.println("OS: " + System.getProperty("os.name"));
        System.out.println("Répertoire de travail: " + System.getProperty("user.dir"));
        System.out.println("Classpath: " + System.getProperty("java.class.path"));
        System.out.println("=== FIN INFORMATIONS ===");
    }
    
    /**
     * Teste la disponibilité des classes essentielles
     */
    public static void verifierClasses() {
        System.out.println("=== VÉRIFICATION DES CLASSES ===");
        
        verifierClasse("application.models.User", "Modèle User");
        verifierClasse("application.models.Role", "Modèle Role");
        verifierClasse("application.services.AuthenticationService", "Service d'authentification");
        verifierClasse("application.services.UserService", "Service utilisateur");
        verifierClasse("application.utils.SessionManager", "Gestionnaire de session");
        verifierClasse("application.controllers.MainController", "Contrôleur principal");
        
        System.out.println("=== FIN VÉRIFICATION CLASSES ===");
    }
    
    /**
     * Vérifie si une classe existe
     */
    private static void verifierClasse(String nomClasse, String description) {
        try {
            Class.forName(nomClasse);
            System.out.println("✅ " + description + " : TROUVÉE - " + nomClasse);
        } catch (ClassNotFoundException e) {
            System.out.println("❌ " + description + " : NON TROUVÉE - " + nomClasse);
        } catch (Exception e) {
            System.out.println("⚠️ " + description + " : ERREUR - " + e.getMessage());
        }
    }
    
    /**
     * Exécute un diagnostic complet
     */
    public static void diagnosticComplet() {
        System.out.println("\n" + "=".repeat(50));
        System.out.println("DIAGNOSTIC COMPLET DE L'APPLICATION");
        System.out.println("=".repeat(50));
        
        afficherInfoEnvironnement();
        System.out.println();
        
        verifierClasses();
        System.out.println();
        
        verifierRessources();
        System.out.println();
        
        System.out.println("=".repeat(50));
        System.out.println("FIN DU DIAGNOSTIC");
        System.out.println("=".repeat(50) + "\n");
    }
}