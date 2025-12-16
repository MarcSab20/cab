package application.utils;

import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.stage.Modality;
import javafx.stage.Stage;
import application.models.User;

import java.io.IOException;

/**
 * Utilitaire pour ouvrir la fenêtre de visualisation dynamique du workflow
 */
public class WorkflowVisualizationHelper {
    
    /**
     * Ouvre la fenêtre de visualisation dynamique du workflow
     * 
     * @param currentUser L'utilisateur courant (pour les permissions)
     * @throws IOException Si le fichier FXML ne peut pas être chargé
     */
    public static void openVisualizationWindow(User currentUser) throws IOException {
        // Créer une nouvelle fenêtre
        Stage visualizationStage = new Stage();
        visualizationStage.initModality(Modality.NONE); // Fenêtre indépendante
        visualizationStage.setTitle("📊 Visualisation Dynamique des Flux de Courriers");
        
        // Charger le FXML
        FXMLLoader loader = new FXMLLoader(
            WorkflowVisualizationHelper.class.getResource("/application/views/workflow_visualization.fxml")
        );
        Parent root = loader.load();
        
        // Créer la scène
        Scene scene = new Scene(root);
        
        // Charger le CSS si disponible
        try {
            scene.getStylesheets().add(
                WorkflowVisualizationHelper.class.getResource("/application/styles/application.css").toExternalForm()
            );
        } catch (Exception e) {
            System.err.println("⚠️ CSS non trouvé, style par défaut appliqué");
        }
        
        // Configurer la fenêtre
        visualizationStage.setScene(scene);
        visualizationStage.setMaximized(false);
        visualizationStage.setResizable(true);
        
        // Afficher la fenêtre
        visualizationStage.show();
        
        System.out.println("✅ Fenêtre de visualisation ouverte pour l'utilisateur: " + currentUser.getNomComplet());
    }
    
    /**
     * Ouvre la fenêtre de visualisation avec gestion des erreurs
     * 
     * @param currentUser L'utilisateur courant
     */
    public static void openVisualizationWindowSafe(User currentUser) {
        try {
            openVisualizationWindow(currentUser);
        } catch (IOException e) {
            System.err.println("❌ Erreur lors de l'ouverture de la fenêtre de visualisation: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError(
                "Erreur d'ouverture",
                "Impossible d'ouvrir la fenêtre de visualisation:\n" + e.getMessage()
            );
        }
    }
}