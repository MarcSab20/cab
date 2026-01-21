package application;

import javafx.application.Application;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Stage;
import application.services.DatabaseService;
import application.services.NetworkService;
import application.utils.DiagnosticUtils;

/**
 * Application principale de gestion des documents et du courrier
 * Architecture sécurisée avec gestion des rôles et communication réseau
 */
public class DocumentManagementApp extends Application {
    
    private static Stage primaryStage;
    private static final String APP_TITLE = "Système de Gestion Documentaire";
    private static final double WINDOW_WIDTH = 1200;
    private static final double WINDOW_HEIGHT = 800;
    
    @Override
    public void start(Stage stage) throws Exception {
        primaryStage = stage;
        
        // Initialisation des services
        initializeServices();
        
        // Chargement de la page de login
        loadLoginView();
        
        // Configuration de la fenêtre principale
        setupPrimaryStage();
    }
    
    private void initializeServices() {
        try {
            // Initialisation de la base de données
            DatabaseService.getInstance().initialize();
            
            // Initialisation du service réseau
            NetworkService.getInstance().initialize();
            
            System.out.println("Services initialisés avec succès");
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation des services: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadLoginView() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/views/login.fxml"));
            Parent root = loader.load();
            
            Scene scene = new Scene(root, WINDOW_WIDTH, WINDOW_HEIGHT);
            scene.getStylesheets().add(getClass().getResource("/application/styles/application.css").toExternalForm());
            
            primaryStage.setScene(scene);
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de la vue login: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupPrimaryStage() {
        primaryStage.setTitle(APP_TITLE);
        primaryStage.setMinWidth(1000);
        primaryStage.setMinHeight(700);
        primaryStage.setResizable(true);
        primaryStage.centerOnScreen();
        
        // Gestion de la fermeture de l'application
        primaryStage.setOnCloseRequest(event -> {
            try {
                NetworkService.getInstance().shutdown();
                DatabaseService.getInstance().close();
            } catch (Exception e) {
                System.err.println("Erreur lors de la fermeture: " + e.getMessage());
            }
        });
        
        primaryStage.show();
    }
    
    public static Stage getPrimaryStage() {
        return primaryStage;
    }
    
    public static void main(String[] args) {
    	DiagnosticUtils.diagnosticComplet();
        launch(args);
    }
}
