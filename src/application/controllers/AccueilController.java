package application.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Label;
import javafx.scene.input.MouseEvent;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import application.models.User;
import application.services.*;
import application.utils.SessionManager;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur pour la page d'accueil
 */
public class AccueilController implements Initializable {
    
    @FXML private Label labelBienvenue;
    @FXML private Label statCourriersTotal;
    @FXML private Label statCourriersEnCours;
    @FXML private Label statDocuments;
    @FXML private Label statMessages;
    @FXML private Label labelInfoSysteme;
    @FXML private VBox activitesContainer;
    
    private User currentUser;
    private CourrierService courrierService;
    private DocumentService documentService;
    private MainController mainController;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("AccueilController.initialize() - Début");
        
        try {
            // Récupération de l'utilisateur courant
            currentUser = SessionManager.getInstance().getCurrentUser();
            
            if (currentUser == null) {
                System.err.println("ERREUR: Aucun utilisateur en session");
                return;
            }
            
            // Initialisation des services
            courrierService = CourrierService.getInstance();
            documentService = DocumentService.getInstance();
            
            // Affichage du message de bienvenue personnalisé
            labelBienvenue.setText("Bienvenue, " + currentUser.getPrenom() + " " + currentUser.getNom());
            
            // Chargement des statistiques
            loadStatistics();
            
            System.out.println("AccueilController.initialize() - Succès");
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation de AccueilController: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Définit le MainController parent (appelé par le MainController après le chargement)
     */
    public void setMainController(MainController mainController) {
        this.mainController = mainController;
    }
    
    /**
     * Charge les statistiques
     */
    private void loadStatistics() {
        try {
            // Documents
            int totalDocuments = documentService.getAllDocuments().size();
            statDocuments.setText(String.valueOf(totalDocuments));
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des statistiques: " + e.getMessage());
            e.printStackTrace();
        }
    }
    

    
    /**
     * Récupère le MainController depuis la scène actuelle
     */
    private MainController getMainControllerFromScene() {
        try {
            Stage stage = (Stage) labelBienvenue.getScene().getWindow();
            Scene scene = stage.getScene();
            Parent root = scene.getRoot();
            
            // Le MainController devrait être accessible depuis la racine via getUserData
            Object userData = root.getUserData();
            if (userData instanceof MainController) {
                return (MainController) userData;
            }
            
            System.err.println("AVERTISSEMENT: MainController non trouvé dans getUserData");
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération du MainController: " + e.getMessage());
        }
        return null;
    }
    
    /**
     * Rafraîchit les données affichées (peut être appelé par le MainController)
     */
    public void refresh() {
        loadStatistics();
    }
}