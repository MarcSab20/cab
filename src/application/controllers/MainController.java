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
import javafx.scene.Node;
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
 * Contrôleur principal de l'application
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
    @FXML private ToggleGroup vueToggle;
    @FXML private ToggleGroup toggleAffichage;
    @FXML private ToggleGroup toggleTheme;
    
    // Boutons de navigation
    @FXML private Button btnAccueil;
    @FXML private Button btnDashboard;
    @FXML private Button btnCourrier;
    @FXML private Button btnDocuments;
    @FXML private Button btnReunions;
    @FXML private Button btnMessages;
    @FXML private Button btnRecherche;
    @FXML private Button btnParametres;
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
            
            if (btnAdmin != null) {
                btnAdmin.setOnAction(e -> loadView("admin"));
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
     * Configure les permissions selon le rôle de l'utilisateur
     */
    private void setupPermissions() {
        try {
            String roleName = currentUser.getRole().getNom().toLowerCase();
            
            // Masquer les fonctions d'administration pour les non-administrateurs
            if (btnAdmin != null) {
                boolean isAdmin = roleName.contains("admin") || roleName.contains("administrateur");
                btnAdmin.setVisible(isAdmin);
                btnAdmin.setManaged(isAdmin);
                
                if (!isAdmin) {
                    System.out.println("Fonctions d'administration masquées pour l'utilisateur: " + currentUser.getNomComplet());
                }
            }
            
            // Autres restrictions selon les permissions...
            // TODO: Implémenter la logique de permissions détaillée
            
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
            
            // Remplacement du contenu
            if (contentArea != null) {
                contentArea.setCenter(view);
                currentView = viewName;
                
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
            Button[] buttons = {btnAccueil, btnDashboard, btnCourrier, btnDocuments, 
                              btnReunions, btnMessages, btnRecherche, btnParametres, btnAdmin};
            
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
            case "courrier": return btnCourrier;
            case "documents": return btnDocuments;
            case "reunions": return btnReunions;
            case "messages": return btnMessages;
            case "recherche": return btnRecherche;
            case "parametres": return btnParametres;
            case "admin": return btnAdmin;
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
                
                // Log de déconnexion
                // authService.logDisconnection(currentUser.getCode());
                
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
    
    @FXML
    private void handleOpenDashboard() {
        try {
            // Charger la vue FXML
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/application/views/dashboard.fxml")
            );
            Parent root = loader.load();
            
            // Créer une nouvelle fenêtre
            Stage stage = new Stage();
            stage.setTitle("📊 Tableau de bord - Workflow des Courriers");
            
            // Créer la scène avec une taille adaptée
            Scene scene = new Scene(root, 1400, 900);
            
            // Ajouter le fichier CSS
            scene.getStylesheets().add(
                getClass().getResource("/application/resources/css/workflow-dashboard.css")
                    .toExternalForm()
            );
            
            stage.setScene(scene);
            stage.setMaximized(false); // Ou true pour maximiser automatiquement
            stage.show();
            
            System.out.println("Dashboard ouvert avec succès");
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'ouverture du tableau de bord: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError(
                "Erreur d'ouverture",
                "Impossible d'ouvrir le tableau de bord.\n" +
                "Erreur: " + e.getMessage()
            );
        }
    }
    
    /**
     * Ouvre l'interface d'administration de la hiérarchie
     * Accessible uniquement aux utilisateurs de niveau 0 (CEMAA, CSP)
     * 
     * À ajouter dans votre MainController.java
     */
    @FXML
    private void handleOpenAdminHierarchy() {
        try {
            // Vérifier les permissions de l'utilisateur
            User currentUser = SessionManager.getInstance().getCurrentUser();
            
            if (currentUser == null) {
                AlertUtils.showWarning(
                    "Session invalide",
                    "Aucun utilisateur connecté. Veuillez vous reconnecter."
                );
                return;
            }
            
            // Seuls les utilisateurs de niveau 0 peuvent administrer
            if (currentUser.getNiveauAutorite() > 0) {
                AlertUtils.showWarning(
                    "🔒 Accès refusé",
                    "Vous n'avez pas les permissions nécessaires.\n\n" +
                    "Seuls les utilisateurs de niveau 0 (CEMAA, CSP) " +
                    "peuvent administrer la hiérarchie des services.\n\n" +
                    "Votre niveau: " + currentUser.getNiveauAutorite() + "\n" +
                    "Service: " + (currentUser.getServiceCode() != null ? 
                                  currentUser.getServiceCode() : "Non défini")
                );
                return;
            }
            
            // Charger la vue FXML
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/application/views/admin_hierarchy.fxml")
            );
            Parent root = loader.load();
            
            // Créer une nouvelle fenêtre
            Stage stage = new Stage();
            stage.setTitle("⚙️ Administration - Hiérarchie des Services");
            
            // Créer la scène avec une taille adaptée
            Scene scene = new Scene(root, 1600, 900);
            
            // Ajouter le fichier CSS
            scene.getStylesheets().add(
                getClass().getResource("/application/resources/css/workflow-dashboard.css")
                    .toExternalForm()
            );
            
            stage.setScene(scene);
            stage.setMaximized(false);
            stage.show();
            
            System.out.println("Interface d'administration ouverte avec succès");
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'ouverture de l'administration: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError(
                "Erreur d'ouverture",
                "Impossible d'ouvrir l'interface d'administration.\n" +
                "Erreur: " + e.getMessage()
            );
        }
    }
    
    /**
     * Ouvre rapidement le workflow pour un courrier spécifique
     * Utile pour ajouter un bouton "Voir workflow" dans la liste des courriers
     * 
     * À ajouter dans votre CourrierController.java ou similaire
     */
    @FXML
    private void handleVoirWorkflowCourrier(int courrierId) {
        try {
            // Charger le dashboard
            FXMLLoader loader = new FXMLLoader(
                getClass().getResource("/application/views/dashboard.fxml")
            );
            Parent root = loader.load();
            
            // Récupérer le contrôleur
            DashboardController controller = loader.getController();
            
            // Créer la fenêtre
            Stage stage = new Stage();
            stage.setTitle("📊 Workflow - Courrier #" + courrierId);
            
            Scene scene = new Scene(root, 1400, 900);
            scene.getStylesheets().add(
                getClass().getResource("/application/resources/css/workflow-dashboard.css")
                    .toExternalForm()
            );
            
            stage.setScene(scene);
            stage.show();
            
            // TODO: Faire défiler jusqu'au courrier spécifique
            // controller.scrollToCourrier(courrierId);
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'ouverture du workflow: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur lors de l'ouverture du workflow du courrier");
        }
    }
    
    /**
     * Démarre un workflow pour un courrier
     * À appeler lors de l'enregistrement d'un nouveau courrier
     * 
     * À ajouter dans votre méthode de création de courrier
     */
    private void demarrerWorkflowPourCourrier(application.models.Courrier courrier) {
        try {
            application.services.WorkflowService workflowService = 
                application.services.WorkflowService.getInstance();
            
            // Démarrer le workflow au Service Courrier
            boolean success = workflowService.startWorkflow(courrier, "SERVICE_COURRIER");
            
            if (success) {
                System.out.println("Workflow démarré avec succès pour le courrier #" + 
                                 courrier.getNumeroCourrier());
                
                // Option: Afficher une notification
                AlertUtils.showInfo(
                    "Workflow démarré",
                    "Le courrier a été enregistré et le workflow a démarré.\n" +
                    "Numéro: " + courrier.getNumeroCourrier()
                );
            } else {
                System.err.println("Échec du démarrage du workflow");
                AlertUtils.showWarning(
                    "Attention",
                    "Le courrier a été créé mais le workflow n'a pas pu démarrer.\n" +
                    "Veuillez le démarrer manuellement."
                );
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors du démarrage du workflow: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Vérifie si l'utilisateur peut accéder au workflow
     * Utile pour activer/désactiver des boutons dans l'interface
     * 
     * @return true si l'utilisateur a accès au workflow
     */
    private boolean canAccessWorkflow() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        
        if (currentUser == null) {
            return false;
        }
        
        // Vérifier que l'utilisateur a un service assigné
        String serviceCode = currentUser.getServiceCode();
        
        return serviceCode != null && !serviceCode.isEmpty();
    }
    
    /**
     * Vérifie si l'utilisateur peut administrer la hiérarchie
     * 
     * @return true si l'utilisateur est de niveau 0
     */
    private boolean canAdministerHierarchy() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        
        if (currentUser == null) {
            return false;
        }
        
        return currentUser.getNiveauAutorite() == 0;
    }
    
    /**
     * Retourne à l'écran de connexion
     */
    private void returnToLogin() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/views/login.fxml"));
            Parent loginRoot = loader.load();
            
            javafx.scene.Scene loginScene = new javafx.scene.Scene(loginRoot);
            
            // Chargement du CSS
            URL cssUrl = getClass().getResource("/application/styles/application.css");
            if (cssUrl != null) {
                loginScene.getStylesheets().add(cssUrl.toExternalForm());
            }
            
            javafx.stage.Stage currentStage = (javafx.stage.Stage) mainContainer.getScene().getWindow();
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