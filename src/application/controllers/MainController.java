package application.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Stage;
import application.models.User;
import application.services.AuthenticationService;
import application.utils.SessionManager;
import application.utils.AlertUtils;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Contrôleur principal de l'application - VERSION avec support Courrier
 */
public class MainController implements Initializable {
    
    // === ÉLÉMENTS DE L'INTERFACE ===
    @FXML private BorderPane mainContainer;
    @FXML private VBox sidebar;
    @FXML private BorderPane contentArea;
    @FXML private Label userNameLabel;
    @FXML private Label userRoleLabel;
    @FXML private Label currentTimeLabel;
    @FXML private Label statusLabel;
    
    // Boutons de navigation
    @FXML private Button btnAccueil;
    @FXML private Button btnCourrier;
    @FXML private Button btnDocuments;
    @FXML private Button btnAdmin;
    @FXML private Button btnDeconnexion;
    
    // Services et données
    private User currentUser;
    private AuthenticationService authService;
    private String currentView = "";
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("MainController.initialize() - Début de l'initialisation");
        
        try {
            // Initialisation des services
            authService = AuthenticationService.getInstance();
            currentUser = SessionManager.getInstance().getCurrentUser();
            
            if (currentUser == null) {
                System.err.println("ERREUR: Aucun utilisateur en session");
                showErrorAndExit("Erreur de session", "Aucun utilisateur connecté");
                return;
            }
            
            System.out.println("Utilisateur connecté: " + currentUser.getNomComplet());
            
            // Configuration de l'interface utilisateur
            setupUserInterface();
            setupNavigation();
            
            // Chargement de la vue par défaut
            loadView("accueil");
            
            // Démarrage des tâches en arrière-plan
            startBackgroundTasks();
            
            System.out.println("MainController.initialize() - Initialisation terminée avec succès");
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation du MainController: " + e.getMessage());
            e.printStackTrace();
            showErrorAndExit("Erreur d'initialisation", "Impossible d'initialiser l'interface: " + e.getMessage());
        }
    }
    
    /**
     * Configure les informations utilisateur dans l'interface
     */
    private void setupUserInterface() {
        try {
            // Affichage des informations utilisateur
            if (userNameLabel != null) {
                userNameLabel.setText(currentUser.getNomComplet());
            }
            
            if (userRoleLabel != null) {
                userRoleLabel.setText(currentUser.getRole().getNom());
            }
            
            // Mise à jour de l'heure actuelle
            updateCurrentTime();
            
            // Message de bienvenue
            if (statusLabel != null) {
                statusLabel.setText("Bienvenue, " + currentUser.getPrenom() + " !");
            }
            
            System.out.println("Interface utilisateur configurée pour: " + currentUser.getNomComplet());
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la configuration de l'interface utilisateur: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Configure la navigation et les événements des boutons
     */
    private void setupNavigation() {
        try {
            // Configuration des boutons de navigation
            if (btnAccueil != null) {
                btnAccueil.setOnAction(e -> loadView("accueil"));
            }
            
            if (btnCourrier != null) {
                btnCourrier.setOnAction(e -> loadView("courrier"));
            }
            
            if (btnDocuments != null) {
                btnDocuments.setOnAction(e -> loadView("documents"));
            }
            
            if (btnAdmin != null) {
                btnAdmin.setOnAction(e -> loadView("administration"));
            }
            
            if (btnDeconnexion != null) {
                btnDeconnexion.setOnAction(e -> handleDeconnexion());
            }
            
            System.out.println("Navigation configurée");
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la configuration de la navigation: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Charge une vue dans la zone de contenu principale
     */
    private void loadView(String viewName) {
        try {
            System.out.println("Chargement de la vue: " + viewName);
            
            // Mise à jour du statut des boutons de navigation
            updateNavigationButtons(viewName);
            
            // Chargement du fichier FXML correspondant
            String fxmlPath = "/application/views/" + viewName + ".fxml";
            URL fxmlUrl = getClass().getResource(fxmlPath);
            
            if (fxmlUrl == null) {
                System.err.println("Fichier FXML non trouvé: " + fxmlPath);
                showTemporaryMessage("Vue non disponible: " + viewName);
                return;
            }
            
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent view = loader.load();
            
            // Récupération du contrôleur et passage de la référence au MainController
            Object controller = loader.getController();
            if (controller != null) {
                try {
                    controller.getClass().getMethod("setMainController", MainController.class)
                        .invoke(controller, this);
                    System.out.println("✓ Référence MainController passée au contrôleur");
                } catch (NoSuchMethodException e) {
                    System.out.println("ℹ Le contrôleur n'a pas de méthode setMainController");
                } catch (Exception e) {
                    System.err.println("⚠ Erreur lors de l'appel de setMainController: " + e.getMessage());
                }
            }
            
            // Remplacement du contenu
            if (contentArea != null) {
                if (view instanceof Region) {
                    Region region = (Region) view;
                    region.setMinWidth(800);
                    region.setMinHeight(600);
                    region.setPrefWidth(Region.USE_COMPUTED_SIZE);
                    region.setPrefHeight(Region.USE_COMPUTED_SIZE);
                    region.setMaxWidth(Double.MAX_VALUE);
                    region.setMaxHeight(Double.MAX_VALUE);
                }
                
                contentArea.setCenter(view);
                currentView = viewName;
                
                Platform.runLater(() -> {
                    try {
                        contentArea.layout();
                        if (view instanceof Region) {
                            ((Region) view).layout();
                        }
                    } catch (Exception e) {
                        System.err.println("⚠ Erreur rafraîchissement layout: " + e.getMessage());
                    }
                });
                
                // Mise à jour du statut
                if (statusLabel != null) {
                    statusLabel.setText("Vue active: " + capitalizeFirst(viewName));
                }
                
                System.out.println("Vue chargée avec succès: " + viewName);
            }
            
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de la vue " + viewName + ": " + e.getMessage());
            e.printStackTrace();
            showTemporaryMessage("Erreur lors du chargement de la vue: " + viewName);
        } catch (Exception e) {
            System.err.println("Erreur inattendue lors du chargement de la vue " + viewName + ": " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Met à jour l'état visuel des boutons de navigation
     */
    private void updateNavigationButtons(String activeView) {
        try {
            Button[] buttons = {btnAccueil, btnCourrier, btnDocuments, btnAdmin};
            
            for (Button btn : buttons) {
                if (btn != null) {
                    btn.getStyleClass().removeAll("sidebar-button-active");
                    btn.getStyleClass().add("sidebar-button");
                }
            }
            
            // Marquer le bouton actif
            Button activeButton = getButtonForView(activeView);
            if (activeButton != null) {
                activeButton.getStyleClass().removeAll("sidebar-button");
                activeButton.getStyleClass().add("sidebar-button-active");
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la mise à jour des boutons de navigation: " + e.getMessage());
        }
    }
    
    /**
     * Retourne le bouton correspondant à une vue
     */
    private Button getButtonForView(String viewName) {
        switch (viewName.toLowerCase()) {
            case "accueil": return btnAccueil;
            case "courrier": return btnCourrier;
            case "documents": return btnDocuments;
            case "administration": return btnAdmin;
            default: return null;
        }
    }
    
    /**
     * Gère la déconnexion de l'utilisateur
     */
    @FXML
    private void handleDeconnexion() {
        try {
            System.out.println("Déconnexion demandée par: " + currentUser.getNomComplet());
            
            boolean confirm = AlertUtils.showConfirmation(
                "Déconnexion", 
                "Êtes-vous sûr de vouloir vous déconnecter ?"
            );
            
            if (confirm) {
                SessionManager.getInstance().clearSession();
                returnToLogin();
                System.out.println("Déconnexion effectuée avec succès");
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la déconnexion: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur lors de la déconnexion: " + e.getMessage());
        }
    }
    
    /**
     * Retourne à l'écran de connexion
     */
    private void returnToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/views/login.fxml"));
            Parent loginRoot = loader.load();
            
            Scene loginScene = new Scene(loginRoot);
            
            URL cssUrl = getClass().getResource("/application/styles/application.css");
            if (cssUrl != null) {
                loginScene.getStylesheets().add(cssUrl.toExternalForm());
            }
            
            Stage currentStage = (Stage) mainContainer.getScene().getWindow();
            currentStage.setScene(loginScene);
            currentStage.centerOnScreen();
            
        } catch (Exception e) {
            System.err.println("Erreur lors du retour à l'écran de connexion: " + e.getMessage());
            e.printStackTrace();
            Platform.exit();
        }
    }
    
    /**
     * Démarre les tâches en arrière-plan
     */
    private void startBackgroundTasks() {
        Thread timeUpdater = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(1000);
                    Platform.runLater(this::updateCurrentTime);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        timeUpdater.setDaemon(true);
        timeUpdater.start();
    }
    
    /**
     * Met à jour l'affichage de l'heure actuelle
     */
    private void updateCurrentTime() {
        if (currentTimeLabel != null) {
            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            currentTimeLabel.setText(now.format(formatter));
        }
    }
    
    /**
     * Affiche un message temporaire dans la zone de statut
     */
    private void showTemporaryMessage(String message) {
        if (statusLabel != null) {
            statusLabel.setText(message);
            
            Thread clearMessage = new Thread(() -> {
                try {
                    Thread.sleep(3000);
                    Platform.runLater(() -> {
                        if (statusLabel != null) {
                            statusLabel.setText("Prêt");
                        }
                    });
                } catch (InterruptedException e) {
                    // Ignorer
                }
            });
            clearMessage.setDaemon(true);
            clearMessage.start();
        }
    }
    
    /**
     * Affiche une erreur critique et ferme l'application
     */
    private void showErrorAndExit(String title, String message) {
        Platform.runLater(() -> {
            AlertUtils.showError(title + "\n\n" + message);
            Platform.exit();
        });
    }
    
    /**
     * Met en majuscule la première lettre d'une chaîne
     */
    private String capitalizeFirst(String str) {
        if (str == null || str.isEmpty()) {
            return str;
        }
        return str.substring(0, 1).toUpperCase() + str.substring(1);
    }
    
    // === MÉTHODES PUBLIQUES POUR L'ACCÈS EXTERNE ===
    
    public User getCurrentUser() {
        return currentUser;
    }
    
    public String getCurrentView() {
        return currentView;
    }
    
    public void navigateToView(String viewName) {
        loadView(viewName);
    }
    
    public void refreshUserInfo() {
        setupUserInterface();
    }
}