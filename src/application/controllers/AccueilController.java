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
            
            // Chargement des activités récentes
            loadRecentActivities();
            
            // Mise à jour des informations système
            updateSystemInfo();
            
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
            // Courriers actifs
            int totalCourriers = courrierService.getAllCourriers().size();
            statCourriersTotal.setText(String.valueOf(totalCourriers));
            
            // Courriers en cours
            int courriersEnCours = (int) courrierService.getAllCourriers().stream()
                .filter(c -> c.getStatut() == application.models.StatutCourrier.EN_COURS)
                .count();
            statCourriersEnCours.setText(String.valueOf(courriersEnCours));
            
            // Documents
            int totalDocuments = documentService.getAllDocuments().size();
            statDocuments.setText(String.valueOf(totalDocuments));
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des statistiques: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Charge les activités récentes
     */
    private void loadRecentActivities() {
        try {
            activitesContainer.getChildren().clear();
            
            // Ajouter quelques activités récentes (à remplacer par de vraies données)
            String[] activites = {
                "📧 Nouveau courrier reçu: Demande de devis",
                "✅ Courrier traité: Rapport mensuel",
                "📄 Document créé: Plan d'action 2025",
                "📤 Courrier transféré: Demande de congé"
            };
            
            for (String activite : activites) {
                Label actLabel = new Label(activite);
                actLabel.setStyle("-fx-padding: 8; -fx-text-fill: #2c3e50; -fx-font-size: 13px;");
                activitesContainer.getChildren().add(actLabel);
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des activités: " + e.getMessage());
        }
    }
    
    /**
     * Met à jour les informations système
     */
    private void updateSystemInfo() {
        try {
            String info = "Système opérationnel - " + 
                         currentUser.getRole().getNom() + " - " +
                         (currentUser.getServiceCode() != null ? 
                             "Service: " + currentUser.getServiceCode() : 
                             "Tous services");
            labelInfoSysteme.setText(info);
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la mise à jour des infos système: " + e.getMessage());
        }
    }
    
    /**
     * Gestion de l'action "Nouveau courrier"
     */
    @FXML
    private void handleNouveauCourrier(MouseEvent event) {
        System.out.println("Action: Nouveau courrier");
        navigateToView("courrier");
    }
    
    /**
     * Gestion de l'action "Tableau de bord"
     */
    @FXML
    private void handleTableauBord(MouseEvent event) {
        System.out.println("Action: Tableau de bord");
        navigateToView("dashboard");
    }
    
    /**
     * Gestion de l'action "Gestion des courriers"
     */
    @FXML
    private void handleGestionCourriers(MouseEvent event) {
        System.out.println("Action: Gestion des courriers");
        navigateToView("courrier");
    }
    
    /**
     * Gestion de l'action "Recherche"
     */
    @FXML
    private void handleRecherche(MouseEvent event) {
        System.out.println("Action: Recherche");
        navigateToView("recherche");
    }
    
    /**
     * Navigation vers une autre vue via le MainController
     */
    private void navigateToView(String viewName) {
        try {
            // Si le mainController a été défini, l'utiliser
            if (mainController != null) {
                mainController.navigateToView(viewName);
            } else {
                // Sinon, essayer de le récupérer depuis la scène
                MainController controller = getMainControllerFromScene();
                if (controller != null) {
                    controller.navigateToView(viewName);
                } else {
                    System.err.println("ERREUR: MainController non disponible pour la navigation");
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la navigation vers " + viewName + ": " + e.getMessage());
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
        loadRecentActivities();
        updateSystemInfo();
    }
}