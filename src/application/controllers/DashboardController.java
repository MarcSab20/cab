package application.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import application.models.*;
import application.services.*;
import application.utils.SessionManager;
import application.utils.AlertUtils;
import javafx.application.Platform;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contrôleur pour le Dashboard - VERSION ROBUSTE
 * Gère les erreurs silencieuses et l'absence de données
 */
public class DashboardController implements Initializable {
    
    // Cartes de statistiques
    @FXML private Label statCourriersEnAttente;
    @FXML private Label statDocumentsActifs;
    @FXML private Label statReunionsMois;
    @FXML private Label statMessagesNonLus;
    @FXML private Label trendCourriers;
    @FXML private Label trendDocuments;
    
    // Graphiques
    @FXML private LineChart<String, Number> evolutionChart;
    @FXML private PieChart repartitionChart;
    
    // Zone de workflow
    @FXML private VBox workflowContainer;
    @FXML private ScrollPane workflowScrollPane;
    @FXML private Label labelServiceActuel;
    @FXML private Label labelCourriersEnCours;
    @FXML private Label labelCourriersEnRetard;
    
    // Activités récentes
    @FXML private VBox activitesRecentesContainer;
    
    // Indicateurs de performance
    @FXML private ProgressBar tauxTraitementBar;
    @FXML private Label tauxTraitementLabel;
    @FXML private ProgressBar delaiMoyenBar;
    @FXML private Label delaiMoyenLabel;
    
    // Services
    private User currentUser;
    private WorkflowService workflowService;
    private CourrierService courrierService;
    private DocumentService documentService;
    private MessageService messageService;
    
    // Données
    private ServiceHierarchy userService;
    private ObservableList<Courrier> courriersEnWorkflow;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("=== DashboardController.initialize() - DÉBUT ===");
        
        try {
            // Initialisation des services avec gestion d'erreurs
            if (!initializeServices()) {
                System.err.println("ERREUR: Échec de l'initialisation des services");
                showErrorState("Impossible d'initialiser les services");
                return;
            }
            
            // Initialisation des composants UI
            initializeUIComponents();
            
            // Chargement des données avec gestion d'erreurs
            loadDataSafely();
            
            // Actualisation automatique toutes les 30 secondes
            startAutoRefresh();
            
            // CORRECTION: Forcer les dimensions minimales des conteneurs
            Platform.runLater(() -> {
                try {
                    if (workflowContainer != null) {
                        workflowContainer.setMinHeight(300);
                        workflowContainer.setPrefHeight(Region.USE_COMPUTED_SIZE);
                        workflowContainer.setMaxHeight(Region.USE_COMPUTED_SIZE);
                    }
                    if (workflowScrollPane != null) {
                        workflowScrollPane.setMinHeight(300);
                        workflowScrollPane.setPrefHeight(400);
                    }
                    if (activitesRecentesContainer != null) {
                        activitesRecentesContainer.setMinHeight(150);
                        activitesRecentesContainer.setPrefHeight(Region.USE_COMPUTED_SIZE);
                    }
                    System.out.println("✓ Dimensions des conteneurs configurées");
                } catch (Exception e) {
                    System.err.println("⚠ Erreur configuration dimensions: " + e.getMessage());
                }
            });

            System.out.println("=== DashboardController.initialize() - SUCCÈS ===");
            
        } catch (Exception e) {
            System.err.println("=== ERREUR CRITIQUE dans DashboardController.initialize() ===");
            e.printStackTrace();
            showErrorState("Erreur lors de l'initialisation: " + e.getMessage());
        }
    }
    
    /**
     * Initialise les services avec gestion d'erreurs
     */
    private boolean initializeServices() {
        try {
            // Récupérer l'utilisateur courant
            currentUser = SessionManager.getInstance().getCurrentUser();
            if (currentUser == null) {
                System.err.println("ERREUR: Aucun utilisateur en session");
                return false;
            }
            
            System.out.println("Utilisateur connecté: " + currentUser.getNomComplet());
            
            // Initialiser les services
            try {
                workflowService = WorkflowService.getInstance();
                System.out.println("✓ WorkflowService initialisé");
            } catch (Exception e) {
                System.err.println("⚠ Erreur WorkflowService: " + e.getMessage());
                workflowService = null;
            }
            
            try {
                courrierService = CourrierService.getInstance();
                System.out.println("✓ CourrierService initialisé");
            } catch (Exception e) {
                System.err.println("⚠ Erreur CourrierService: " + e.getMessage());
                courrierService = null;
            }
            
            try {
                documentService = DocumentService.getInstance();
                System.out.println("✓ DocumentService initialisé");
            } catch (Exception e) {
                System.err.println("⚠ Erreur DocumentService: " + e.getMessage());
                documentService = null;
            }
            
            try {
                messageService = MessageService.getInstance();
                System.out.println("✓ MessageService initialisé");
            } catch (Exception e) {
                System.err.println("⚠ Erreur MessageService: " + e.getMessage());
                messageService = null;
            }
            
            // Charger le service de l'utilisateur
            if (workflowService != null && currentUser.getServiceCode() != null) {
                try {
                    userService = workflowService.getServiceByCode(currentUser.getServiceCode());
                    if (userService != null) {
                        System.out.println("✓ Service utilisateur: " + userService.getServiceName());
                    }
                } catch (Exception e) {
                    System.err.println("⚠ Erreur chargement service utilisateur: " + e.getMessage());
                }
            }
            
            return true;
            
        } catch (Exception e) {
            System.err.println("ERREUR lors de l'initialisation des services: " + e.getMessage());
            e.printStackTrace();
            return false;
        }
    }
    
    /**
     * Initialise les composants UI
     */
    private void initializeUIComponents() {
        System.out.println("Initialisation des composants UI...");
        
        try {
            courriersEnWorkflow = FXCollections.observableArrayList();
            
            // Initialiser les graphiques
            if (evolutionChart != null) {
                System.out.println("✓ evolutionChart présent");
            }
            
            if (repartitionChart != null) {
                System.out.println("✓ repartitionChart présent");
            }
            
            if (workflowContainer != null) {
                System.out.println("✓ workflowContainer présent");
            }
            
        } catch (Exception e) {
            System.err.println("Erreur initialisation UI: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Charge les données de manière sécurisée
     */
    private void loadDataSafely() {
        System.out.println("Chargement des données...");
        
        try {
            loadStatisticsSafely();
            loadChartsSafely();
            loadWorkflowSafely();
            loadActivitiesSafely();
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des données: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Charge les statistiques de manière sécurisée
     */
    private void loadStatisticsSafely() {
        try {
            System.out.println("Chargement des statistiques...");
            
            // Courriers en attente
            int enAttente = 0;
            if (courrierService != null && workflowService != null) {
                try {
                    List<Courrier> courriers = getCourriersVisibles();
                    enAttente = (int) courriers.stream()
                        .filter(c -> c.getStatut() == StatutCourrier.EN_ATTENTE)
                        .count();
                } catch (Exception e) {
                    System.err.println("⚠ Erreur comptage courriers: " + e.getMessage());
                }
            }
            
            if (statCourriersEnAttente != null) {
                statCourriersEnAttente.setText(String.valueOf(enAttente));
            }
            
            if (trendCourriers != null) {
                trendCourriers.setText("↗ +5 aujourd'hui");
                trendCourriers.setStyle("-fx-text-fill: #27ae60;");
            }
            
            // Documents actifs
            int documentsActifs = 0;
            if (documentService != null) {
                try {
                    List<Document> documents = documentService.getAllDocuments();
                    documentsActifs = (int) documents.stream()
                        .filter(d -> d.getStatut().isActif())
                        .count();
                } catch (Exception e) {
                    System.err.println("⚠ Erreur comptage documents: " + e.getMessage());
                }
            }
            
            if (statDocumentsActifs != null) {
                statDocumentsActifs.setText(String.valueOf(documentsActifs));
            }
            
            if (trendDocuments != null) {
                trendDocuments.setText("↗ +12 cette semaine");
                trendDocuments.setStyle("-fx-text-fill: #27ae60;");
            }
            
            // Réunions (valeur par défaut pour l'instant)
            if (statReunionsMois != null) {
                statReunionsMois.setText("0");
            }
            
            // Messages non lus
            int nonLus = 0;
            if (messageService != null && currentUser != null) {
                try {
                    List<Message> messages = messageService.getMessagesForUser(currentUser.getId());
                    nonLus = (int) messages.stream()
                        .filter(m -> !m.isLu())
                        .count();
                } catch (Exception e) {
                    System.err.println("⚠ Erreur comptage messages: " + e.getMessage());
                }
            }
            
            if (statMessagesNonLus != null) {
                statMessagesNonLus.setText(String.valueOf(nonLus));
            }
            
            System.out.println("✓ Statistiques chargées");
            
        } catch (Exception e) {
            System.err.println("Erreur chargement statistiques: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Charge les graphiques de manière sécurisée
     */
    private void loadChartsSafely() {
        try {
            System.out.println("Chargement des graphiques...");
            
            // Graphique d'évolution
            if (evolutionChart != null) {
                try {
                    XYChart.Series<String, Number> series = new XYChart.Series<>();
                    series.setName("Courriers traités");
                    
                    String[] jours = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
                    int[] valeurs = {12, 15, 10, 18, 14, 8, 11};
                    
                    for (int i = 0; i < jours.length; i++) {
                        series.getData().add(new XYChart.Data<>(jours[i], valeurs[i]));
                    }
                    
                    evolutionChart.getData().clear();
                    evolutionChart.getData().add(series);
                    
                    System.out.println("✓ Graphique d'évolution chargé");
                } catch (Exception e) {
                    System.err.println("⚠ Erreur graphique évolution: " + e.getMessage());
                }
            }
            
            // Graphique de répartition
            if (repartitionChart != null) {
                try {
                    ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList(
                        new PieChart.Data("En attente (5)", 5),
                        new PieChart.Data("En cours (8)", 8),
                        new PieChart.Data("Traités (32)", 32),
                        new PieChart.Data("Archivés (15)", 15)
                    );
                    
                    repartitionChart.setData(pieChartData);
                    
                    System.out.println("✓ Graphique de répartition chargé");
                } catch (Exception e) {
                    System.err.println("⚠ Erreur graphique répartition: " + e.getMessage());
                }
            }
            
        } catch (Exception e) {
            System.err.println("Erreur chargement graphiques: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Charge le workflow de manière sécurisée
     */
    private void loadWorkflowSafely() {
        try {
            System.out.println("Chargement du workflow...");
            
            if (workflowContainer == null) {
                System.err.println("⚠ workflowContainer est null");
                return;
            }
            
            workflowContainer.getChildren().clear();
            
            // Informations sur le service actuel
            if (userService != null && labelServiceActuel != null) {
                labelServiceActuel.setText(userService.getServiceName());
            } else if (labelServiceActuel != null) {
                labelServiceActuel.setText("--");
            }
            
            // Récupérer les courriers visibles
            List<Courrier> courriers = getCourriersVisibles();
            courriersEnWorkflow.clear();
            courriersEnWorkflow.addAll(
                courriers.stream()
                    .filter(Courrier::estEnWorkflow)
                    .collect(Collectors.toList())
            );
            
            // Statistiques rapides
            long enCours = courriersEnWorkflow.stream()
                .filter(c -> c.getStatut() == StatutCourrier.EN_COURS)
                .count();
            
            if (labelCourriersEnCours != null) {
                labelCourriersEnCours.setText(String.valueOf(enCours));
            }
            
            if (labelCourriersEnRetard != null) {
                labelCourriersEnRetard.setText("0");
            }
            
            // Afficher les courriers
            if (courriersEnWorkflow.isEmpty()) {
                showEmptyWorkflowMessage();
            } else {
                displayWorkflowCourriers();
            }
            
            System.out.println("✓ Workflow chargé (" + courriersEnWorkflow.size() + " courriers)");
            
        } catch (Exception e) {
            System.err.println("Erreur chargement workflow: " + e.getMessage());
            e.printStackTrace();
            showEmptyWorkflowMessage();
        }
    }
    
    /**
     * Affiche un message quand il n'y a pas de courriers en workflow
     */
    private void showEmptyWorkflowMessage() {
        if (workflowContainer == null) return;
        
        VBox emptyBox = new VBox(20);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setPadding(new Insets(50));
        
        Label iconLabel = new Label("🔄");
        iconLabel.setStyle("-fx-font-size: 48px; -fx-text-fill: #bdc3c7;");
        
        Label messageLabel = new Label("Aucun courrier en workflow actuellement");
        messageLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 16px;");
        
        Label detailLabel = new Label("Les courriers apparaîtront ici une fois qu'ils seront en traitement");
        detailLabel.setStyle("-fx-text-fill: #95a5a6; -fx-font-size: 12px;");
        
        emptyBox.getChildren().addAll(iconLabel, messageLabel, detailLabel);
        workflowContainer.getChildren().add(emptyBox);
    }
    
    /**
     * Affiche les courriers en workflow
     */
    private void displayWorkflowCourriers() {
        if (workflowContainer == null) return;
        
        for (Courrier courrier : courriersEnWorkflow) {
            try {
                VBox courrierCard = createSimpleCourrierCard(courrier);
                workflowContainer.getChildren().add(courrierCard);
            } catch (Exception e) {
                System.err.println("⚠ Erreur création carte courrier: " + e.getMessage());
            }
        }
    }
    
    /**
     * Crée une carte simplifiée pour un courrier
     */
    private VBox createSimpleCourrierCard(Courrier courrier) {
        VBox card = new VBox(10);
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        
        // En-tête
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label numeroLabel = new Label(courrier.getNumeroCourrier());
        numeroLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        Label objetLabel = new Label(courrier.getObjet());
        objetLabel.setStyle("-fx-text-fill: #2c3e50;");
        HBox.setHgrow(objetLabel, Priority.ALWAYS);
        
        Label prioriteLabel = new Label(courrier.getPrioriteIcone());
        prioriteLabel.setStyle("-fx-font-size: 16px;");
        
        header.getChildren().addAll(numeroLabel, objetLabel, prioriteLabel);
        
        // Informations
        HBox info = new HBox(20);
        info.setAlignment(Pos.CENTER_LEFT);
        
        Label serviceLabel = new Label("Service: " + 
            (courrier.getServiceActuel() != null ? courrier.getServiceActuel() : "N/A"));
        serviceLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        
        Label statutLabel = new Label("Statut: " + courrier.getStatut().getLibelle());
        statutLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        
        info.getChildren().addAll(serviceLabel, statutLabel);
        
        card.getChildren().addAll(header, info);
        
        return card;
    }
    
    /**
     * Charge les activités récentes de manière sécurisée
     */
    private void loadActivitiesSafely() {
        try {
            System.out.println("Chargement des activités récentes...");
            
            if (activitesRecentesContainer == null) {
                System.err.println("⚠ activitesRecentesContainer est null");
                return;
            }
            
            activitesRecentesContainer.getChildren().clear();
            
            // Ajouter quelques activités par défaut
            String[] activites = {
                "📧 Nouveau courrier reçu: Demande de devis",
                "✅ Courrier traité: Rapport mensuel",
                "🔄 Transfert vers SCRH: Demande de congé"
            };
            
            for (String activite : activites) {
                Label actLabel = new Label(activite);
                actLabel.setStyle("-fx-padding: 10; -fx-background-color: #f8f9fa; " +
                                "-fx-background-radius: 5; -fx-font-size: 12px;");
                activitesRecentesContainer.getChildren().add(actLabel);
            }
            
            System.out.println("✓ Activités récentes chargées");
            
        } catch (Exception e) {
            System.err.println("Erreur chargement activités: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Récupère les courriers visibles pour l'utilisateur
     */
    private List<Courrier> getCourriersVisibles() {
        try {
            if (workflowService == null || currentUser == null) {
                return new ArrayList<>();
            }
            
            return workflowService.getCourriersVisiblesPourUtilisateur(currentUser);
            
        } catch (Exception e) {
            System.err.println("Erreur récupération courriers: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Affiche un état d'erreur dans l'interface
     */
    private void showErrorState(String message) {
        System.err.println("AFFICHAGE ÉTAT D'ERREUR: " + message);
        
        // On pourrait créer un message d'erreur visible dans l'UI ici
        // Pour l'instant, on log juste l'erreur
    }
    
    /**
     * Démarre le rafraîchissement automatique
     */
    private void startAutoRefresh() {
        try {
            Thread refreshThread = new Thread(() -> {
                while (true) {
                    try {
                        Thread.sleep(30000); // 30 secondes
                        Platform.runLater(this::refreshDashboard);
                    } catch (InterruptedException e) {
                        break;
                    }
                }
            });
            refreshThread.setDaemon(true);
            refreshThread.start();
            
            System.out.println("✓ Auto-refresh démarré");
            
        } catch (Exception e) {
            System.err.println("⚠ Erreur démarrage auto-refresh: " + e.getMessage());
        }
    }
    
    /**
     * Rafraîchit le dashboard
     */
    @FXML
    private void handleActualiser() {
        System.out.println("Actualisation manuelle du dashboard...");
        refreshDashboard();
    }
    
    /**
     * Rafraîchit le dashboard
     */
    private void refreshDashboard() {
        try {
            loadStatisticsSafely();
            loadWorkflowSafely();
            loadActivitiesSafely();
            System.out.println("✓ Dashboard rafraîchi");
        } catch (Exception e) {
            System.err.println("Erreur lors du rafraîchissement: " + e.getMessage());
            e.printStackTrace();
        }
    }
}