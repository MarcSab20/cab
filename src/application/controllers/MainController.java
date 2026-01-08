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
import application.utils.WorkflowVisualizationHelper;
import application.utils.AlertUtils;
import java.io.IOException;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ResourceBundle;

/**
 * Contrôleur principal de l'application - VERSION INTÉGRÉE avec Workflow et Admin Hiérarchie
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
    @FXML private Button btnDashboard;
    @FXML private Button btnWorkflowDashboard;
    @FXML private Button btnCourrier;
    @FXML private Button btnDocuments;
    @FXML private Button btnReunions;
    @FXML private Button btnMessages;
    @FXML private Button btnRecherche;
    @FXML private Button btnWorkflowGraph;
    @FXML private Button btnWorkflowVisualization;
    @FXML private Button btnParametres;
    @FXML private Button btnAdmin;
    @FXML private Button btnAdminHierarchy;
    @FXML private Button btnDeconnexion;
    
    // Services et données
    private User currentUser;
    private AuthenticationService authService;
    private String currentView = "";
    private int currentUserNiveau;
    
    
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
            setupPermissions();
            
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
            
            if (btnDashboard != null) {
                btnDashboard.setOnAction(e -> loadView("dashboard"));
            }
            
            if (btnWorkflowVisualization != null) {
                btnWorkflowVisualization.setOnAction(e -> {
                    WorkflowVisualizationHelper.openVisualizationWindowSafe(currentUser);
                });
            }
            
            if (btnWorkflowDashboard != null) {
                btnWorkflowDashboard.setOnAction(e -> loadView("workflow_suivi")); // Utilise le même dashboard
            }
            
            if (btnCourrier != null) {
                btnCourrier.setOnAction(e -> loadView("courrier"));
            }
            
            if (btnDocuments != null) {
                btnDocuments.setOnAction(e -> loadView("documents"));
            }
            
            if (btnReunions != null) {
                btnReunions.setOnAction(e -> loadView("reunions"));
            }
            
            if (btnMessages != null) {
                btnMessages.setOnAction(e -> loadView("messages"));
            }
            
            if (btnRecherche != null) {
                btnRecherche.setOnAction(e -> loadView("recherche"));
            }
            
            if (btnParametres != null) {
                btnParametres.setOnAction(e -> loadView("parametres"));
            }
            
            if (btnWorkflowGraph != null) {
                btnWorkflowGraph.setOnAction(e -> loadView("workflow_graph"));
            }
            
            if (btnAdmin != null) {
                btnAdmin.setOnAction(e -> loadView("admin"));
            }
            
            if (btnAdminHierarchy != null) {
                btnAdminHierarchy.setOnAction(e -> loadView("admin_hierarchy"));
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
     * Gère l'ouverture de la vue Analyse Workflow
     */
    @FXML
    private void handleWorkflowGraph() {
        try {
            System.out.println("Chargement de la vue Workflow Graph...");
            
            // Charger simplement la vue - le contrôleur gèrera tout
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/views/workflow_graph.fxml"));
            Parent root = loader.load();
            
            // Remplacer le contenu
            contentArea.setCenter(root);
            currentView = "workflow_graph";
            
            // Mise à jour du statut
            if (statusLabel != null) {
                statusLabel.setText("Vue active: Analyse Workflow");
            }
            
            // Mettre à jour les boutons de navigation
            updateNavigationButtons("workflow_graph");
            
            System.out.println("Vue Workflow Graph chargée avec succès");
            
        } catch (IOException e) {
            System.err.println("Erreur lors du chargement de la vue Analyse Workflow: " + e.getMessage());
            e.printStackTrace();
            showTemporaryMessage("Erreur lors du chargement de la vue Analyse Workflow");
            AlertUtils.showError("Impossible de charger la vue Analyse Workflow: " + e.getMessage());
        }
    }
    
    /**
     * Configure les permissions selon le rôle de l'utilisateur
     */
    private void setupPermissions() {
        try {
            String roleName = currentUser.getRole().getNom().toLowerCase();
            int niveauAutorite = currentUser.getNiveauAutorite();
            
            // Masquer les fonctions d'administration pour les non-administrateurs
            boolean isAdmin = roleName.contains("admin") || roleName.contains("administrateur");
            boolean isNiveauZero = niveauAutorite == 0;
            boolean isNiveauUn = niveauAutorite == -1;
            if (btnAdmin != null) {
                btnAdmin.setVisible(isAdmin || isNiveauZero);
                btnAdmin.setManaged(isAdmin || isNiveauZero);
            }
            
            if (btnWorkflowGraph != null) {
                // Masquer pour le Service Courrier (niveau -1)
                boolean accessible = currentUser.getNiveauAutorite() >= 0;
                btnWorkflowGraph.setVisible(accessible);
                btnWorkflowGraph.setManaged(accessible);
            }
            
            
            // La hiérarchie est accessible uniquement aux utilisateurs de niveau 0
            if (btnAdminHierarchy != null) {
                btnAdminHierarchy.setVisible(isNiveauZero);
                btnAdminHierarchy.setManaged(isNiveauZero);
                
                if (!isNiveauZero) {
                    System.out.println("Fonctions d'administration de hiérarchie masquées pour l'utilisateur: " + 
                                     currentUser.getNomComplet() + " (niveau " + niveauAutorite + ")");
                }
            }
            
            // Le workflow dashboard est visible pour tous
            if (btnWorkflowDashboard != null) {
                boolean hasService = currentUser.getServiceCode() != null && !currentUser.getServiceCode().isEmpty();
                if (!hasService) {
                    System.out.println("ATTENTION: L'utilisateur n'a pas de service assigné");
                }
            }
            
            if (btnWorkflowVisualization != null) {
                // Masquer pour le Service Courrier (niveau -1)
                boolean accessible = currentUser.getNiveauAutorite() >= 0;
                btnWorkflowVisualization.setVisible(accessible);
                btnWorkflowVisualization.setManaged(accessible);
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la configuration des permissions: " + e.getMessage());
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
                // Si le contrôleur a une méthode setMainController, l'appeler
                try {
                    controller.getClass().getMethod("setMainController", MainController.class)
                        .invoke(controller, this);
                    System.out.println("✓ Référence MainController passée au contrôleur");
                } catch (NoSuchMethodException e) {
                    // Le contrôleur n'a pas de méthode setMainController, ce n'est pas grave
                    System.out.println("ℹ Le contrôleur n'a pas de méthode setMainController");
                } catch (Exception e) {
                    System.err.println("⚠ Erreur lors de l'appel de setMainController: " + e.getMessage());
                }
            }
            
            // Remplacement du contenu
            if (contentArea != null) {
                // CORRECTION: Forcer les dimensions minimales de la vue
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
                
                // CORRECTION: Forcer le rafraîchissement du layout
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
            // Réinitialiser tous les boutons
            Button[] buttons = {btnAccueil, btnDashboard, btnWorkflowDashboard, btnCourrier, btnDocuments, 
                              btnReunions, btnMessages, btnRecherche, btnParametres, btnAdmin, btnAdminHierarchy};
            
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
            case "dashboard": return btnDashboard;
            case "workflow_dashboard": return btnWorkflowDashboard;
            case "courrier": return btnCourrier;
            case "documents": return btnDocuments;
            case "reunions": return btnReunions;
            case "messages": return btnMessages;
            case "recherche": return btnRecherche;
            case "workflow_graph": return btnWorkflowGraph;
            case "parametres": return btnParametres;
            case "admin": return btnAdmin;
            case "admin_hierarchy": return btnAdminHierarchy;
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
            
            // Confirmation de déconnexion
            boolean confirm = AlertUtils.showConfirmation(
                "Déconnexion", 
                "Êtes-vous sûr de vouloir vous déconnecter ?"
            );
            
            if (confirm) {
                // Nettoyage de la session
                SessionManager.getInstance().clearSession();
                
                // Retour à l'écran de connexion
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
            
            // Chargement du CSS
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
        // Mise à jour de l'heure toutes les secondes
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
            
            // Effacer le message après 3 secondes
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
    
    /**
     * Retourne l'utilisateur actuellement connecté
     */
    public User getCurrentUser() {
        return currentUser;
    }
    
    /**
     * Retourne la vue actuellement affichée
     */
    public String getCurrentView() {
        return currentView;
    }
    
    /**
     * Force le chargement d'une vue spécifique
     */
    public void navigateToView(String viewName) {
        loadView(viewName);
    }
    
    /**
     * Met à jour les informations utilisateur affichées
     */
    public void refreshUserInfo() {
        setupUserInterface();
    }
}