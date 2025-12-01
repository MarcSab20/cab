package application.controllers;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
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
 * Contrôleur pour le suivi des workflows - VERSION ROBUSTE
 * Gère les erreurs silencieuses et l'absence de données
 */
public class WorkflowSuiviController implements Initializable {
    
    // Filtres
    @FXML private ComboBox<String> filtreStatut;
    @FXML private ComboBox<String> filtrePriorite;
    @FXML private ComboBox<String> filtreService;
    @FXML private TextField champRecherche;
    @FXML private DatePicker dateDebut;
    @FXML private DatePicker dateFin;
    
    // TableView et colonnes
    @FXML private TableView<Courrier> tableWorkflow;
    @FXML private TableColumn<Courrier, String> colNumero;
    @FXML private TableColumn<Courrier, String> colObjet;
    @FXML private TableColumn<Courrier, String> colStatut;
    @FXML private TableColumn<Courrier, String> colService;
    @FXML private TableColumn<Courrier, String> colPriorite;
    @FXML private TableColumn<Courrier, String> colDate;
    @FXML private TableColumn<Courrier, String> colActions;
    
    // Détails du courrier sélectionné
    @FXML private VBox detailsContainer;
    @FXML private Label detailNumero;
    @FXML private Label detailObjet;
    @FXML private Label detailStatut;
    @FXML private Label detailPriorite;
    @FXML private Label detailExpediteur;
    @FXML private Label detailDateReception;
    @FXML private Label detailServiceActuel;
    @FXML private TextArea detailCommentaire;
    
    // Timeline du workflow
    @FXML private VBox timelineContainer;
    
    // Statistiques
    @FXML private Label statTotal;
    @FXML private Label statEnCours;
    @FXML private Label statEnAttente;
    @FXML private Label statTermines;
    
    // Boutons d'action
    @FXML private Button btnTransferer;
    @FXML private Button btnTraiter;
    @FXML private Button btnArchiver;
    @FXML private Button btnAjouterCommentaire;
    
    // Services
    private User currentUser;
    private WorkflowService workflowService;
    private CourrierService courrierService;
    
    // Données
    private ServiceHierarchy userService;
    private ObservableList<Courrier> courriersObservable;
    private List<Courrier> tousLesCourriers;
    private Courrier courrierSelectionne;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("=== WorkflowSuiviController.initialize() - DÉBUT ===");
        
        try {
            // Initialisation des services
            if (!initializeServices()) {
                System.err.println("ERREUR: Échec de l'initialisation des services");
                showErrorState("Impossible d'initialiser les services");
                return;
            }
            
            // Initialisation de la TableView
            initializeTableView();
            
            // Initialisation des filtres
            initializeFilters();
            
            // Chargement des données
            loadDataSafely();
            
            // Configuration des listeners
            setupListeners();
            Platform.runLater(() -> {
                try {
                    if (tableWorkflow != null) {
                        tableWorkflow.setMinHeight(400);
                        tableWorkflow.setPrefHeight(Region.USE_COMPUTED_SIZE);
                        tableWorkflow.setMaxHeight(Double.MAX_VALUE);
                    }
                    if (detailsContainer != null) {
                        detailsContainer.setMinHeight(500);
                        detailsContainer.setPrefHeight(Region.USE_COMPUTED_SIZE);
                    }
                    if (timelineContainer != null) {
                        timelineContainer.setMinHeight(200);
                        timelineContainer.setPrefHeight(Region.USE_COMPUTED_SIZE);
                    }
                    System.out.println("✓ Dimensions des composants workflow configurées");
                } catch (Exception e) {
                    System.err.println("⚠ Erreur configuration dimensions workflow: " + e.getMessage());
                }
            });
            System.out.println("=== WorkflowSuiviController.initialize() - SUCCÈS ===");
            
        } catch (Exception e) {
            System.err.println("=== ERREUR CRITIQUE dans WorkflowSuiviController.initialize() ===");
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
     * Initialise la TableView
     */
    private void initializeTableView() {
        System.out.println("Initialisation de la TableView...");
        
        try {
            courriersObservable = FXCollections.observableArrayList();
            
            if (tableWorkflow == null) {
                System.err.println("⚠ tableWorkflow est null");
                return;
            }
            
            // Configuration des colonnes si elles existent
            if (colNumero != null) {
                colNumero.setCellValueFactory(data -> 
                    new SimpleStringProperty(data.getValue().getNumeroCourrier()));
                System.out.println("✓ colNumero configurée");
            }
            
            if (colObjet != null) {
                colObjet.setCellValueFactory(data -> 
                    new SimpleStringProperty(data.getValue().getObjet()));
                System.out.println("✓ colObjet configurée");
            }
            
            if (colStatut != null) {
                colStatut.setCellValueFactory(data -> 
                    new SimpleStringProperty(data.getValue().getStatut().getLibelle()));
                colStatut.setCellFactory(column -> new TableCell<Courrier, String>() {
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                            setStyle("");
                        } else {
                            setText(item);
                            Courrier courrier = getTableView().getItems().get(getIndex());
                            setStyle("-fx-background-color: " + courrier.getStatut().getCouleur() + 
                                   "; -fx-background-radius: 5; -fx-padding: 5;");
                        }
                    }
                });
                System.out.println("✓ colStatut configurée");
            }
            
            if (colService != null) {
                colService.setCellValueFactory(data -> 
                    new SimpleStringProperty(
                        data.getValue().getServiceActuel() != null ? 
                        data.getValue().getServiceActuel() : "N/A"
                    ));
                System.out.println("✓ colService configurée");
            }
            
            if (colPriorite != null) {
                colPriorite.setCellValueFactory(data -> 
                    new SimpleStringProperty(
                        data.getValue().getPrioriteIcone() + " " + 
                        data.getValue().getPriorite()
                    ));
                System.out.println("✓ colPriorite configurée");
            }
            
            if (colDate != null) {
                colDate.setCellValueFactory(data -> 
                    new SimpleStringProperty(
                        data.getValue().getDateReception().format(
                            DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                        )
                    ));
                System.out.println("✓ colDate configurée");
            }
            
            if (colActions != null) {
                colActions.setCellFactory(column -> new TableCell<Courrier, String>() {
                    private final Button btnDetails = new Button("👁");
                    
                    {
                        btnDetails.setStyle("-fx-background-color: transparent; -fx-cursor: hand;");
                        btnDetails.setTooltip(new Tooltip("Voir détails"));
                        btnDetails.setOnAction(event -> {
                            Courrier courrier = getTableView().getItems().get(getIndex());
                            afficherDetails(courrier);
                        });
                    }
                    
                    @Override
                    protected void updateItem(String item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty) {
                            setGraphic(null);
                        } else {
                            HBox container = new HBox(5);
                            container.setAlignment(Pos.CENTER);
                            container.getChildren().add(btnDetails);
                            setGraphic(container);
                        }
                    }
                });
                System.out.println("✓ colActions configurée");
            }
            
            tableWorkflow.setItems(courriersObservable);
            
            // Message si table vide
            tableWorkflow.setPlaceholder(new Label("Aucun courrier en workflow"));
            
            System.out.println("✓ TableView initialisée");
            
        } catch (Exception e) {
            System.err.println("Erreur initialisation TableView: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Initialise les filtres
     */
    private void initializeFilters() {
        System.out.println("Initialisation des filtres...");
        
        try {
            // Filtre statut
            if (filtreStatut != null) {
                ObservableList<String> statuts = FXCollections.observableArrayList(
                    "Tous",
                    StatutCourrier.EN_ATTENTE.getLibelle(),
                    StatutCourrier.EN_COURS.getLibelle(),
                    StatutCourrier.TRAITE.getLibelle(),
                    StatutCourrier.ARCHIVE.getLibelle()
                );
                filtreStatut.setItems(statuts);
                filtreStatut.setValue("Tous");
                System.out.println("✓ filtreStatut configuré");
            }
            
            // Filtre priorité
            if (filtrePriorite != null) {
                ObservableList<String> priorites = FXCollections.observableArrayList(
                    "Toutes",
                    PrioriteCourrier.NORMALE.getLibelle(),
                    PrioriteCourrier.URGENTE.getLibelle(),
                    PrioriteCourrier.TRES_URGENTE.getLibelle()
                );
                filtrePriorite.setItems(priorites);
                filtrePriorite.setValue("Toutes");
                System.out.println("✓ filtrePriorite configuré");
            }
            
            // Filtre service
            if (filtreService != null) {
                ObservableList<String> services = FXCollections.observableArrayList("Tous");
                
                if (workflowService != null) {
                    try {
                        List<ServiceHierarchy> tousServices = workflowService.getAllServices();
                        services.addAll(tousServices.stream()
                            .map(ServiceHierarchy::getServiceName)
                            .collect(Collectors.toList()));
                    } catch (Exception e) {
                        System.err.println("⚠ Erreur chargement services pour filtre: " + e.getMessage());
                    }
                }
                
                filtreService.setItems(services);
                filtreService.setValue("Tous");
                System.out.println("✓ filtreService configuré");
            }
            
            System.out.println("✓ Filtres initialisés");
            
        } catch (Exception e) {
            System.err.println("Erreur initialisation filtres: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Charge les données de manière sécurisée
     */
    private void loadDataSafely() {
        System.out.println("Chargement des données...");
        
        try {
            tousLesCourriers = getCourriersVisibles();
            System.out.println("✓ " + tousLesCourriers.size() + " courriers chargés");
            
            appliquerFiltres();
            updateStatistics();
            
        } catch (Exception e) {
            System.err.println("Erreur chargement données: " + e.getMessage());
            e.printStackTrace();
            
            // Initialiser avec une liste vide
            tousLesCourriers = new ArrayList<>();
            courriersObservable.clear();
        }
    }
    
    /**
     * Configure les listeners
     */
    private void setupListeners() {
        System.out.println("Configuration des listeners...");
        
        try {
            // Listener sur sélection dans la table
            if (tableWorkflow != null) {
                tableWorkflow.getSelectionModel().selectedItemProperty().addListener(
                    (obs, oldSelection, newSelection) -> {
                        if (newSelection != null) {
                            afficherDetails(newSelection);
                        }
                    }
                );
                System.out.println("✓ Listener sélection table configuré");
            }
            
            // Listeners sur les filtres
            if (filtreStatut != null) {
                filtreStatut.setOnAction(e -> appliquerFiltres());
            }
            if (filtrePriorite != null) {
                filtrePriorite.setOnAction(e -> appliquerFiltres());
            }
            if (filtreService != null) {
                filtreService.setOnAction(e -> appliquerFiltres());
            }
            
            // Listener sur champ recherche
            if (champRecherche != null) {
                champRecherche.textProperty().addListener((obs, oldVal, newVal) -> 
                    appliquerFiltres()
                );
            }
            
            System.out.println("✓ Listeners configurés");
            
        } catch (Exception e) {
            System.err.println("Erreur configuration listeners: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Récupère les courriers visibles pour l'utilisateur
     */
    private List<Courrier> getCourriersVisibles() {
        try {
            if (workflowService == null || currentUser == null) {
                System.out.println("⚠ Impossible de charger les courriers (service ou user null)");
                return new ArrayList<>();
            }
            
            return workflowService.getCourriersVisiblesPourUtilisateur(currentUser);
            
        } catch (Exception e) {
            System.err.println("Erreur récupération courriers: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    /**
     * Applique les filtres sur la liste des courriers
     */
    @FXML
    private void appliquerFiltres() {
        try {
            if (tousLesCourriers == null) {
                return;
            }
            
            List<Courrier> courriersFiltres = new ArrayList<>(tousLesCourriers);
            
            // Filtre par statut
            if (filtreStatut != null && !filtreStatut.getValue().equals("Tous")) {
                String statutSelectionne = filtreStatut.getValue();
                courriersFiltres = courriersFiltres.stream()
                    .filter(c -> c.getStatut().getLibelle().equals(statutSelectionne))
                    .collect(Collectors.toList());
            }
            
            // Filtre par priorité
            if (filtrePriorite != null && !filtrePriorite.getValue().equals("Toutes")) {
                String prioriteSelectionnee = filtrePriorite.getValue();
                courriersFiltres = courriersFiltres.stream()
                    .filter(c -> c.getPriorite().equals(prioriteSelectionnee))
                    .collect(Collectors.toList());
            }
            
            // Filtre par service
            if (filtreService != null && !filtreService.getValue().equals("Tous")) {
                String serviceSelectionne = filtreService.getValue();
                courriersFiltres = courriersFiltres.stream()
                    .filter(c -> serviceSelectionne.equals(c.getServiceActuel()))
                    .collect(Collectors.toList());
            }
            
            // Recherche textuelle
            if (champRecherche != null && !champRecherche.getText().trim().isEmpty()) {
                String recherche = champRecherche.getText().toLowerCase();
                courriersFiltres = courriersFiltres.stream()
                    .filter(c -> 
                        c.getNumeroCourrier().toLowerCase().contains(recherche) ||
                        c.getObjet().toLowerCase().contains(recherche) ||
                        (c.getExpediteur() != null && c.getExpediteur().toLowerCase().contains(recherche))
                    )
                    .collect(Collectors.toList());
            }
            
            // Mettre à jour la table
            courriersObservable.clear();
            courriersObservable.addAll(courriersFiltres);
            
            System.out.println("✓ Filtres appliqués: " + courriersFiltres.size() + " courriers affichés");
            
        } catch (Exception e) {
            System.err.println("Erreur application filtres: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Réinitialise les filtres
     */
    @FXML
    private void handleReinitialiserFiltres() {
        try {
            if (filtreStatut != null) filtreStatut.setValue("Tous");
            if (filtrePriorite != null) filtrePriorite.setValue("Toutes");
            if (filtreService != null) filtreService.setValue("Tous");
            if (champRecherche != null) champRecherche.clear();
            if (dateDebut != null) dateDebut.setValue(null);
            if (dateFin != null) dateFin.setValue(null);
            
            appliquerFiltres();
            
        } catch (Exception e) {
            System.err.println("Erreur réinitialisation filtres: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Affiche les détails d'un courrier
     */
    private void afficherDetails(Courrier courrier) {
        try {
            courrierSelectionne = courrier;
            
            if (detailsContainer == null) {
                System.err.println("⚠ detailsContainer est null");
                return;
            }
            
            // Remplir les champs de détails
            if (detailNumero != null) {
                detailNumero.setText(courrier.getNumeroCourrier());
            }
            if (detailObjet != null) {
                detailObjet.setText(courrier.getObjet());
            }
            if (detailStatut != null) {
                detailStatut.setText(courrier.getStatut().getLibelle());
            }
            if (detailPriorite != null) {
                detailPriorite.setText(courrier.getPriorite());
            }
            if (detailExpediteur != null) {
                detailExpediteur.setText(courrier.getExpediteur() != null ? courrier.getExpediteur() : "N/A");
            }
            if (detailDateReception != null) {
                detailDateReception.setText(
                    courrier.getDateReception().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                );
            }
            if (detailServiceActuel != null) {
                detailServiceActuel.setText(
                    courrier.getServiceActuel() != null ? courrier.getServiceActuel() : "N/A"
                );
            }
            
            // Afficher la timeline
            afficherTimeline(courrier);
            
            // Activer/désactiver les boutons d'action
            updateActionButtons(courrier);
            
            System.out.println("✓ Détails affichés pour: " + courrier.getNumeroCourrier());
            
        } catch (Exception e) {
            System.err.println("Erreur affichage détails: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Affiche la timeline du workflow
     */
    private void afficherTimeline(Courrier courrier) {
        try {
            if (timelineContainer == null) {
                return;
            }
            
            timelineContainer.getChildren().clear();
            
            // Créer une timeline simple
            Label timelineTitle = new Label("Historique du workflow");
            timelineTitle.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
            
            VBox timelineBox = new VBox(10);
            timelineBox.setPadding(new Insets(10));
            
            // Événement: Réception
            VBox eventBox1 = createTimelineEvent(
                "📨 Réception",
                courrier.getDateReception().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                "Courrier reçu"
            );
            timelineBox.getChildren().add(eventBox1);
            
            // Si en cours ou traité
            if (courrier.getStatut() != StatutCourrier.EN_ATTENTE) {
                VBox eventBox2 = createTimelineEvent(
                    "🔄 En cours de traitement",
                    LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")),
                    "Service: " + (courrier.getServiceActuel() != null ? courrier.getServiceActuel() : "N/A")
                );
                timelineBox.getChildren().add(eventBox2);
            }
            
            timelineContainer.getChildren().addAll(timelineTitle, timelineBox);
            
        } catch (Exception e) {
            System.err.println("Erreur affichage timeline: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Crée un événement de timeline
     */
    private VBox createTimelineEvent(String titre, String date, String description) {
        VBox eventBox = new VBox(5);
        eventBox.setPadding(new Insets(10));
        eventBox.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 5;");
        
        Label titreLabel = new Label(titre);
        titreLabel.setStyle("-fx-font-weight: bold;");
        
        Label dateLabel = new Label(date);
        dateLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
        
        Label descLabel = new Label(description);
        descLabel.setStyle("-fx-font-size: 12px;");
        
        eventBox.getChildren().addAll(titreLabel, dateLabel, descLabel);
        
        return eventBox;
    }
    
    /**
     * Met à jour les boutons d'action
     */
    private void updateActionButtons(Courrier courrier) {
        try {
            boolean peutTransferer = courrier.getStatut() != StatutCourrier.ARCHIVE;
            boolean peutTraiter = courrier.getStatut() == StatutCourrier.EN_COURS;
            boolean peutArchiver = courrier.getStatut() == StatutCourrier.TRAITE;
            
            if (btnTransferer != null) {
                btnTransferer.setDisable(!peutTransferer);
            }
            if (btnTraiter != null) {
                btnTraiter.setDisable(!peutTraiter);
            }
            if (btnArchiver != null) {
                btnArchiver.setDisable(!peutArchiver);
            }
            
        } catch (Exception e) {
            System.err.println("Erreur mise à jour boutons: " + e.getMessage());
        }
    }
    
    /**
     * Met à jour les statistiques
     */
    private void updateStatistics() {
        try {
            if (tousLesCourriers == null) {
                return;
            }
            
            int total = tousLesCourriers.size();
            long enCours = tousLesCourriers.stream()
                .filter(c -> c.getStatut() == StatutCourrier.EN_COURS)
                .count();
            long enAttente = tousLesCourriers.stream()
                .filter(c -> c.getStatut() == StatutCourrier.EN_ATTENTE)
                .count();
            long termines = tousLesCourriers.stream()
                .filter(c -> c.getStatut() == StatutCourrier.TRAITE || c.getStatut() == StatutCourrier.ARCHIVE)
                .count();
            
            if (statTotal != null) statTotal.setText(String.valueOf(total));
            if (statEnCours != null) statEnCours.setText(String.valueOf(enCours));
            if (statEnAttente != null) statEnAttente.setText(String.valueOf(enAttente));
            if (statTermines != null) statTermines.setText(String.valueOf(termines));
            
            System.out.println("✓ Statistiques mises à jour");
            
        } catch (Exception e) {
            System.err.println("Erreur mise à jour statistiques: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Rafraîchit les données
     */
    @FXML
    private void handleActualiser() {
        System.out.println("Actualisation des données...");
        loadDataSafely();
    }
    
    /**
     * Gestion des actions (stubs pour l'instant)
     */
    @FXML
    private void handleTransferer() {
        if (courrierSelectionne != null) {
            AlertUtils.showInfo("Transfert", "Fonction de transfert en cours de développement");
        }
    }
    
    @FXML
    private void handleTraiter() {
        if (courrierSelectionne != null) {
            AlertUtils.showInfo("Traitement", "Fonction de traitement en cours de développement");
        }
    }
    
    @FXML
    private void handleArchiver() {
        if (courrierSelectionne != null) {
            AlertUtils.showInfo("Archivage", "Fonction d'archivage en cours de développement");
        }
    }
    
    @FXML
    private void handleAjouterCommentaire() {
        if (courrierSelectionne != null) {
            AlertUtils.showInfo("Commentaire", "Fonction d'ajout de commentaire en cours de développement");
        }
    }
    
    /**
     * Affiche un état d'erreur dans l'interface
     */
    private void showErrorState(String message) {
        System.err.println("AFFICHAGE ÉTAT D'ERREUR: " + message);
    }
}