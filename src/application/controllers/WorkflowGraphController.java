package application.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import application.models.*;
import application.services.*;
import application.utils.SessionManager;
import application.utils.AlertUtils;
import application.utils.WorkflowVisualizationHelper;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contrôleur pour la visualisation par graphes du workflow des courriers
 * Avec permissions hiérarchiques strictes
 */
public class WorkflowGraphController implements Initializable {
    
    // Filtres et sélection
    @FXML private ComboBox<String> cbModVue;
    @FXML private ComboBox<Courrier> cbCourrierIndividuel;
    @FXML private DatePicker dpDebut;
    @FXML private DatePicker dpFin;
    @FXML private CheckBox chkAfficherStatistiques;
    
    // Zone de visualisation du graphe
    @FXML private ScrollPane graphScrollPane;
    @FXML private Pane graphPane;
    
    // Zone des statistiques
    @FXML private VBox statsContainer;
    @FXML private Label statTotalCourriers;
    @FXML private Label statDureeMoyenne;
    @FXML private Label statTauxReussite;
    @FXML private Label statGoulots;
    
    // Tableaux de statistiques détaillées
    @FXML private TableView<WorkflowStats> tableStats;
    @FXML private TableColumn<WorkflowStats, String> colService;
    @FXML private TableColumn<WorkflowStats, Number> colTotal;
    @FXML private TableColumn<WorkflowStats, Number> colEnCours;
    @FXML private TableColumn<WorkflowStats, String> colDuree;
    @FXML private TableColumn<WorkflowStats, Number> colScore;
    
    // Services
    private User currentUser;
    private WorkflowService workflowService;
    private CourrierService courrierService;
    
    // Données
    private ServiceHierarchy userService;
    private List<ServiceHierarchy> servicesAutorises;
    private Map<String, WorkflowStats> statistiques;
    private List<Courrier> courriersDisponibles;
    
    private int currentUserId;
    private String currentUserService;
    private int currentUserNiveau;
    
    // Constantes pour le dessin du graphe
    private static final double NODE_RADIUS = 40;
    private static final double HORIZONTAL_SPACING = 200;
    private static final double VERTICAL_SPACING = 150;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("WorkflowGraphController.initialize() - Début");
        
        try {            
            // INITIALISER LES SERVICES FØRST
            workflowService = new WorkflowService();
            courrierService = new CourrierService();
            
            // RÉCUPÉRER L'UTILISATEUR DEPUIS SESSION MANAGER
            currentUser = SessionManager.getInstance().getCurrentUser();
            
            if (currentUser == null) {
                System.err.println("ERREUR: Aucun utilisateur en session");
                AlertUtils.showError("Erreur de session\n\nAucun utilisateur connecté.\nVeuillez vous reconnecter.");
                return;
            }
            
            // Stocker les infos utilisateur
            this.currentUserId = currentUser.getId();
            this.currentUserService = currentUser.getServiceCode();
            this.currentUserNiveau = currentUser.getNiveauAutorite();
            
            System.out.println("=== Utilisateur récupéré ===");
            System.out.println("ID: " + currentUserId);
            System.out.println("Service: " + currentUserService);
            System.out.println("Niveau: " + currentUserNiveau);
            System.out.println("========================");
            
            // Récupérer le service de l'utilisateur
            if (currentUserService != null && !currentUserService.isEmpty()) {
                userService = workflowService.getServiceByCode(currentUserService);
                if (userService != null) {
                    System.out.println("Service utilisateur chargé: " + userService.getServiceName());
                }
            }
            
            // Charger les services autorisés
            loadServicesAutorises();
            
            // Initialiser les composants de l'interface
            initializeComponents();
            
            // Charger les données initiales
            loadInitialData();
            
            System.out.println("WorkflowGraphController.initialize() - Terminé avec succès");
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur d'initialisation\n\nImpossible d'initialiser le contrôleur:\n" + e.getMessage());
        }
    }
    
    /**
     * Initialise les composants de l'interface
     */
    private void initializeComponents() {
        // Mode de vue
        if (cbModVue != null) {
            ObservableList<String> modes = FXCollections.observableArrayList(
                "Vue individuelle (1 courrier)",
                "Vue collective (période)"
            );
            cbModVue.setItems(modes);
            cbModVue.setValue("Vue collective (période)");
            cbModVue.setOnAction(e -> handleModeVueChange());
        }
        
        // Dates par défaut (dernier mois)
        if (dpFin != null) {
            dpFin.setValue(LocalDate.now());
        }
        if (dpDebut != null) {
            dpDebut.setValue(LocalDate.now().minusMonths(1));
        }
        
        // Configuration du graphPane
        if (graphPane != null) {
            graphPane.setMinSize(800, 600);
            graphPane.setStyle("-fx-background-color: #f8f9fa;");
        }
        
        // Configuration de la table des statistiques
        if (tableStats != null) {
            colService.setCellValueFactory(data -> 
                new javafx.beans.property.SimpleStringProperty(data.getValue().getServiceName()));
            colTotal.setCellValueFactory(data -> 
                new javafx.beans.property.SimpleIntegerProperty(data.getValue().getTotalCourriersTraites()));
            colEnCours.setCellValueFactory(data -> 
                new javafx.beans.property.SimpleIntegerProperty(data.getValue().getCourriersEnCours()));
            colDuree.setCellValueFactory(data -> 
                new javafx.beans.property.SimpleStringProperty(data.getValue().getDureeMoyenneFormatee()));
            colScore.setCellValueFactory(data -> 
                new javafx.beans.property.SimpleIntegerProperty(data.getValue().getScorePerformance()));
            
            // Colorier la colonne score
            colScore.setCellFactory(column -> new TableCell<WorkflowStats, Number>() {
                @Override
                protected void updateItem(Number item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setStyle("");
                    } else {
                        setText(item.intValue() + "%");
                        int score = item.intValue();
                        String color;
                        if (score >= 80) {
                            color = "#27ae60";
                        } else if (score >= 60) {
                            color = "#f39c12";
                        } else {
                            color = "#e74c3c";
                        }
                        setStyle("-fx-text-fill: " + color + "; -fx-font-weight: bold;");
                    }
                }
            });
        }
    }
    
    /**
     * ✨ NOUVELLE MÉTHODE : Ouvre la fenêtre de visualisation collective
     */
    @FXML
    private void handleOpenCollectiveView() {
        try {
            System.out.println("Ouverture de la visualisation collective...");
            
            // Ouvrir la fenêtre de visualisation avec l'utilisateur courant
            WorkflowVisualizationHelper.openVisualizationWindow(currentUser);
            
            System.out.println("✅ Fenêtre de visualisation ouverte avec succès");
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de l'ouverture de la visualisation: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError(
                "Impossible d'ouvrir la visualisation",
                "Une erreur s'est produite:\n" + e.getMessage()
            );
        }
    }
    
    @FXML
    private void handleExportGraph() {
        AlertUtils.showInfo("Export", "La fonctionnalité d'export sera bientôt disponible");
    }
    
    /**
     * Charge les services autorisés selon le niveau hiérarchique
     */
    private void loadServicesAutorises() {
        servicesAutorises = new ArrayList<>();
        
        int niveauAutorite = currentUser.getNiveauAutorite();
        
        // Niveau -1 (Service Courrier) : aucun accès
        if (niveauAutorite == -1) {
            System.out.println("Utilisateur niveau -1 : aucune statistique accessible");
            return;
        }
        
        // Niveau 0 (CEMAA, CSP) : voir tout
        if (niveauAutorite == 0) {
            servicesAutorises.addAll(workflowService.getAllServices());
            System.out.println("Utilisateur niveau 0 : accès complet (" + servicesAutorises.size() + " services)");
            return;
        }
        
        // Autres niveaux : voir uniquement les services en dessous dans la hiérarchie
        if (userService != null) {
            servicesAutorises.add(userService); // Son propre service
            servicesAutorises.addAll(getServicesEnDessous(userService));
            System.out.println("Utilisateur niveau " + niveauAutorite + " : " + servicesAutorises.size() + " services accessibles");
        }
    }
    
    /**
     * Récupère récursivement tous les services en dessous d'un service donné
     */
    private List<ServiceHierarchy> getServicesEnDessous(ServiceHierarchy service) {
        List<ServiceHierarchy> result = new ArrayList<>();
        
        for (ServiceHierarchy enfant : service.getEnfants()) {
            if (enfant.isActif()) {
                result.add(enfant);
                result.addAll(getServicesEnDessous(enfant)); // Récursif
            }
        }
        
        return result;
    }
    
    /**
     * Charge les données initiales
     */
    private void loadInitialData() {
        // Charger les courriers disponibles pour la vue individuelle
        loadCourriersDisponibles();
        
        // Charger les statistiques
        loadStatistiques();
        
        // Générer le graphe initial
        if (cbModVue != null && cbModVue.getValue().contains("collective")) {
            genererGrapheCollectif();
        }
    }
    
    
    
    /**
     * Charge les courriers disponibles pour l'utilisateur
     */
    private void loadCourriersDisponibles() {
        try {
            courriersDisponibles = new ArrayList<>();
            
            int niveauAutorite = currentUser.getNiveauAutorite();
            
            if (niveauAutorite == 0) {
                // Niveau 0 : tous les courriers entrants
                courriersDisponibles = courrierService.getAllCourriers().stream()
                    .filter(c -> c.getTypeCourrier() == TypeCourrier.ENTRANT)
                    .filter(c -> c.estEnWorkflow() || c.isWorkflowTermine())
                    .collect(Collectors.toList());
            } else if (niveauAutorite >= 1) {
                // Autres niveaux : courriers passés par leur service ou services en dessous
                Set<String> servicesCodes = servicesAutorises.stream()
                    .map(ServiceHierarchy::getServiceCode)
                    .collect(Collectors.toSet());
                
                courriersDisponibles = courrierService.getAllCourriers().stream()
                    .filter(c -> c.getTypeCourrier() == TypeCourrier.ENTRANT)
                    .filter(c -> {
                        // Vérifier si le courrier est passé par un de ces services
                        List<WorkflowStep> steps = workflowService.getWorkflowHistory(c.getId());
                        return steps.stream().anyMatch(s -> servicesCodes.contains(s.getServiceCode()));
                    })
                    .collect(Collectors.toList());
            }
            
            // Mettre à jour le ComboBox
            if (cbCourrierIndividuel != null && !courriersDisponibles.isEmpty()) {
                ObservableList<Courrier> courriers = FXCollections.observableArrayList(courriersDisponibles);
                cbCourrierIndividuel.setItems(courriers);
                
                // Affichage personnalisé
                cbCourrierIndividuel.setCellFactory(param -> new ListCell<>() {
                    @Override
                    protected void updateItem(Courrier item, boolean empty) {
                        super.updateItem(item, empty);
                        if (empty || item == null) {
                            setText(null);
                        } else {
                            setText(item.getNumeroCourrier() + " - " + item.getObjet());
                        }
                    }
                });
                cbCourrierIndividuel.setButtonCell(cbCourrierIndividuel.getCellFactory().call(null));
            }
            
            System.out.println("Courriers disponibles: " + courriersDisponibles.size());
            
        } catch (Exception e) {
            System.err.println("Erreur chargement courriers: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Charge les statistiques pour les services autorisés
     */
    private void loadStatistiques() {
        statistiques = new HashMap<>();
        
        try {
            LocalDateTime debut = dpDebut.getValue() != null ? 
                dpDebut.getValue().atStartOfDay() : LocalDateTime.now().minusMonths(1);
            LocalDateTime fin = dpFin.getValue() != null ? 
                dpFin.getValue().atTime(23, 59, 59) : LocalDateTime.now();
            
            // Pour chaque service autorisé, calculer les statistiques
            for (ServiceHierarchy service : servicesAutorises) {
                WorkflowStats stats = calculateStatsForService(service, debut, fin);
                statistiques.put(service.getServiceCode(), stats);
            }
            
            // Mettre à jour l'affichage
            updateStatisticsDisplay();
            
            System.out.println("Statistiques calculées pour " + statistiques.size() + " services");
            
        } catch (Exception e) {
            System.err.println("Erreur calcul statistiques: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Calcule les statistiques pour un service donné
     */
    private WorkflowStats calculateStatsForService(ServiceHierarchy service, LocalDateTime debut, LocalDateTime fin) {
        WorkflowStats stats = new WorkflowStats(
            service.getServiceCode(),
            service.getServiceName(),
            service.getNiveau()
        );
        
        // Récupérer tous les courriers qui sont passés par ce service dans la période
        List<Courrier> courriers = courriersDisponibles.stream()
            .filter(c -> {
                if (c.getDateReception() == null) return false;
                return !c.getDateReception().isBefore(debut) && !c.getDateReception().isAfter(fin);
            })
            .collect(Collectors.toList());
        
        int total = 0;
        int enCours = 0;
        int enAttente = 0;
        int enRetard = 0;
        double dureeTotale = 0;
        int nombreAvecDuree = 0;
        double delaiTotalReception = 0;
        int nombreAvecDelai = 0;
        
        Map<String, Integer> transitions = new HashMap<>();
        
        for (Courrier courrier : courriers) {
            List<WorkflowStep> steps = workflowService.getWorkflowHistory(courrier.getId());
            
            for (WorkflowStep step : steps) {
                if (step.getServiceCode().equals(service.getServiceCode())) {
                    total++;
                    
                    // Statut
                    if (step.getStatutEtape() == StatutEtapeWorkflow.EN_COURS) {
                        enCours++;
                    } else if (step.getStatutEtape() == StatutEtapeWorkflow.EN_ATTENTE) {
                        enAttente++;
                    }
                    
                    // Retard
                    if (step.isEnRetard()) {
                        enRetard++;
                    }
                    
                    // Durée de traitement
                    if (step.getStatutEtape() == StatutEtapeWorkflow.TERMINE || 
                        step.getStatutEtape() == StatutEtapeWorkflow.TRANSFERE) {
                        
                        // Trouver l'étape suivante pour calculer la durée
                        int nextIndex = steps.indexOf(step) + 1;
                        if (nextIndex < steps.size()) {
                            WorkflowStep nextStep = steps.get(nextIndex);
                            long heures = java.time.Duration.between(
                                step.getDateAction(),
                                nextStep.getDateAction()
                            ).toHours();
                            
                            dureeTotale += heures;
                            nombreAvecDuree++;
                            
                            // Enregistrer la transition
                            String transition = step.getServiceCode() + " → " + nextStep.getServiceCode();
                            transitions.put(transition, transitions.getOrDefault(transition, 0) + 1);
                        }
                    }
                    
                    // Délai de réception (temps avant le début du traitement)
                    if (step.getEtapeNumero() == 1 && courrier.getDateReception() != null) {
                        long heures = java.time.Duration.between(
                            courrier.getDateReception(),
                            step.getDateAction()
                        ).toHours();
                        delaiTotalReception += heures;
                        nombreAvecDelai++;
                    }
                }
            }
        }
        
        stats.setTotalCourriersTraites(total);
        stats.setCourriersEnCours(enCours);
        stats.setCourriersEnAttente(enAttente);
        stats.setCourriersEnRetard(enRetard);
        
        if (nombreAvecDuree > 0) {
            stats.setDureeMoyenneTraitement(dureeTotale / nombreAvecDuree);
        }
        
        if (nombreAvecDelai > 0) {
            stats.setDelaiMoyenReception(delaiTotalReception / nombreAvecDelai);
        }
        
        if (total > 0) {
            stats.setTauxReussite(((double) (total - enRetard) / total) * 100);
        }
        
        return stats;
    }
    
    /**
     * Met à jour l'affichage des statistiques
     */
    private void updateStatisticsDisplay() {
        // Statistiques globales
        int totalGlobal = statistiques.values().stream()
            .mapToInt(WorkflowStats::getTotalCourriersTraites)
            .sum();
        
        double dureeMoyenneGlobale = statistiques.values().stream()
            .filter(s -> s.getTotalCourriersTraites() > 0)
            .mapToDouble(WorkflowStats::getDureeMoyenneTraitement)
            .average()
            .orElse(0);
        
        double tauxReussiteGlobal = statistiques.values().stream()
            .filter(s -> s.getTotalCourriersTraites() > 0)
            .mapToDouble(WorkflowStats::getTauxReussite)
            .average()
            .orElse(0);
        
        long nombreGoulots = statistiques.values().stream()
            .filter(WorkflowStats::estGoulot)
            .count();
        
        if (statTotalCourriers != null) {
            statTotalCourriers.setText(String.valueOf(totalGlobal));
        }
        
        if (statDureeMoyenne != null) {
            if (dureeMoyenneGlobale < 1) {
                statDureeMoyenne.setText(String.format("%.0f min", dureeMoyenneGlobale * 60));
            } else if (dureeMoyenneGlobale < 24) {
                statDureeMoyenne.setText(String.format("%.1f h", dureeMoyenneGlobale));
            } else {
                statDureeMoyenne.setText(String.format("%.1f j", dureeMoyenneGlobale / 24));
            }
        }
        
        if (statTauxReussite != null) {
            statTauxReussite.setText(String.format("%.1f%%", tauxReussiteGlobal));
        }
        
        if (statGoulots != null) {
            statGoulots.setText(String.valueOf(nombreGoulots));
            if (nombreGoulots > 0) {
                statGoulots.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            } else {
                statGoulots.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            }
        }
        
        // Tableau détaillé
        if (tableStats != null) {
            ObservableList<WorkflowStats> statsList = FXCollections.observableArrayList(
                statistiques.values().stream()
                    .sorted(Comparator.comparing(WorkflowStats::getNiveau)
                        .thenComparing(WorkflowStats::getServiceName))
                    .collect(Collectors.toList())
            );
            tableStats.setItems(statsList);
        }
    }
    
    /**
     * Génère le graphe collectif (tous les courriers d'une période)
     */
    private void genererGrapheCollectif() {
        if (graphPane == null) return;
        
        graphPane.getChildren().clear();
        
        try {
            // Calculer les flux entre services
            Map<String, Map<String, Integer>> flux = calculateFlux();
            
            // Positionner les nœuds par niveau
            Map<String, Point2D> positions = calculateNodePositions();
            
            // Dessiner les arêtes
            drawEdges(flux, positions);
            
            // Dessiner les nœuds
            drawNodes(positions);
            
            System.out.println("Graphe collectif généré");
            
        } catch (Exception e) {
            System.err.println("Erreur génération graphe: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Calcule les flux entre services
     */
    private Map<String, Map<String, Integer>> calculateFlux() {
        Map<String, Map<String, Integer>> flux = new HashMap<>();
        
        LocalDateTime debut = dpDebut.getValue() != null ? 
            dpDebut.getValue().atStartOfDay() : LocalDateTime.now().minusMonths(1);
        LocalDateTime fin = dpFin.getValue() != null ? 
            dpFin.getValue().atTime(23, 59, 59) : LocalDateTime.now();
        
        List<Courrier> courriers = courriersDisponibles.stream()
            .filter(c -> {
                if (c.getDateReception() == null) return false;
                return !c.getDateReception().isBefore(debut) && !c.getDateReception().isAfter(fin);
            })
            .collect(Collectors.toList());
        
        for (Courrier courrier : courriers) {
            List<WorkflowStep> steps = workflowService.getWorkflowHistory(courrier.getId());
            
            for (int i = 0; i < steps.size() - 1; i++) {
                String source = steps.get(i).getServiceCode();
                String dest = steps.get(i + 1).getServiceCode();
                
                flux.computeIfAbsent(source, k -> new HashMap<>())
                    .merge(dest, 1, Integer::sum);
            }
        }
        
        return flux;
    }
    
    /**
     * Calcule les positions des nœuds
     */
    private Map<String, Point2D> calculateNodePositions() {
        Map<String, Point2D> positions = new HashMap<>();
        
        // Grouper les services par niveau
        Map<Integer, List<ServiceHierarchy>> parNiveau = servicesAutorises.stream()
            .collect(Collectors.groupingBy(ServiceHierarchy::getNiveau));
        
        double startY = 100;
        
        for (Map.Entry<Integer, List<ServiceHierarchy>> entry : parNiveau.entrySet()) {
            int niveau = entry.getKey();
            List<ServiceHierarchy> services = entry.getValue();
            
            double totalWidth = services.size() * HORIZONTAL_SPACING;
            double startX = (graphPane.getWidth() - totalWidth) / 2;
            if (startX < 100) startX = 100;
            
            for (int i = 0; i < services.size(); i++) {
                ServiceHierarchy service = services.get(i);
                double x = startX + i * HORIZONTAL_SPACING;
                double y = startY + niveau * VERTICAL_SPACING;
                
                positions.put(service.getServiceCode(), new Point2D(x, y));
            }
        }
        
        // Ajuster la taille du graphPane
        double maxX = positions.values().stream().mapToDouble(Point2D::getX).max().orElse(800);
        double maxY = positions.values().stream().mapToDouble(Point2D::getY).max().orElse(600);
        
        graphPane.setMinSize(maxX + 200, maxY + 200);
        
        return positions;
    }
    
    /**
     * Dessine les arêtes (transitions)
     */
    private void drawEdges(Map<String, Map<String, Integer>> flux, Map<String, Point2D> positions) {
        for (Map.Entry<String, Map<String, Integer>> entry : flux.entrySet()) {
            String source = entry.getKey();
            Point2D posSource = positions.get(source);
            
            if (posSource == null) continue;
            
            for (Map.Entry<String, Integer> transition : entry.getValue().entrySet()) {
                String dest = transition.getKey();
                Integer count = transition.getValue();
                
                Point2D posDest = positions.get(dest);
                if (posDest == null) continue;
                
                // Créer une flèche
                Arrow arrow = new Arrow(
                    posSource.getX(), posSource.getY(),
                    posDest.getX(), posDest.getY(),
                    count
                );
                
                graphPane.getChildren().add(arrow.getGroup());
            }
        }
    }
    
    /**
     * Dessine les nœuds (services)
     */
    private void drawNodes(Map<String, Point2D> positions) {
        for (Map.Entry<String, Point2D> entry : positions.entrySet()) {
            String serviceCode = entry.getKey();
            Point2D pos = entry.getValue();
            
            ServiceHierarchy service = workflowService.getServiceByCode(serviceCode);
            if (service == null) continue;
            
            WorkflowStats stats = statistiques.get(serviceCode);
            
            // Créer le nœud
            ServiceNode node = new ServiceNode(service, stats, pos.getX(), pos.getY());
            graphPane.getChildren().add(node.getGroup());
        }
    }
    
    /**
     * Génère le graphe pour un courrier individuel
     */
    private void genererGrapheIndividuel(Courrier courrier) {
        if (graphPane == null) return;
        
        graphPane.getChildren().clear();
        
        try {
            List<WorkflowStep> steps = workflowService.getWorkflowHistory(courrier.getId());
            
            if (steps.isEmpty()) {
                showEmptyGraphMessage();
                return;
            }
            
            // Calculer les positions
            double startX = 150;
            double startY = 100;
            double spacing = 200;
            
            // Dessiner les étapes
            for (int i = 0; i < steps.size(); i++) {
                WorkflowStep step = steps.get(i);
                
                double x = startX + i * spacing;
                double y = startY;
                
                ServiceHierarchy service = workflowService.getServiceByCode(step.getServiceCode());
                if (service == null) continue;
                
                // Nœud
                ServiceNodeStep nodeStep = new ServiceNodeStep(step, service, x, y);
                graphPane.getChildren().add(nodeStep.getGroup());
                
                // Flèche vers le suivant
                if (i < steps.size() - 1) {
                    Arrow arrow = new Arrow(
                        x + NODE_RADIUS, y,
                        x + spacing - NODE_RADIUS, y,
                        1
                    );
                    graphPane.getChildren().add(arrow.getGroup());
                }
            }
            
            // Ajuster la taille
            graphPane.setMinSize(startX + steps.size() * spacing + 100, 400);
            
            System.out.println("Graphe individuel généré pour courrier " + courrier.getNumeroCourrier());
            
        } catch (Exception e) {
            System.err.println("Erreur génération graphe individuel: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Affiche un message quand le graphe est vide
     */
    private void showEmptyGraphMessage() {
        VBox emptyBox = new VBox(20);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setLayoutX(graphPane.getWidth() / 2 - 150);
        emptyBox.setLayoutY(graphPane.getHeight() / 2 - 100);
        
        Label iconLabel = new Label("📊");
        iconLabel.setFont(Font.font(64));
        iconLabel.setStyle("-fx-text-fill: #bdc3c7;");
        
        Label messageLabel = new Label("Aucune donnée de workflow à afficher");
        messageLabel.setFont(Font.font(16));
        messageLabel.setStyle("-fx-text-fill: #7f8c8d;");
        
        emptyBox.getChildren().addAll(iconLabel, messageLabel);
        graphPane.getChildren().add(emptyBox);
    }
    
    // ==================== HANDLERS ====================
    
    @FXML
    private void handleModeVueChange() {
        if (cbModVue == null) return;
        
        String mode = cbModVue.getValue();
        boolean individuel = mode.contains("individuelle");
        
        if (cbCourrierIndividuel != null) {
            cbCourrierIndividuel.setDisable(!individuel);
        }
        
        if (dpDebut != null) dpDebut.setDisable(individuel);
        if (dpFin != null) dpFin.setDisable(individuel);
        
        if (individuel) {
            if (cbCourrierIndividuel != null && cbCourrierIndividuel.getValue() != null) {
                genererGrapheIndividuel(cbCourrierIndividuel.getValue());
            }
        } else {
            genererGrapheCollectif();
        }
    }
    
    @FXML
    private void handleGenerateGraph() {
        String mode = cbModVue.getValue();
        
        if (mode.contains("individuelle")) {
            if (cbCourrierIndividuel == null || cbCourrierIndividuel.getValue() == null) {
                AlertUtils.showWarning("Veuillez sélectionner un courrier");
                return;
            }
            genererGrapheIndividuel(cbCourrierIndividuel.getValue());
        } else {
            genererGrapheCollectif();
        }
        
        loadStatistiques();
    }
    
    
    @FXML
    private void handleActualiser() {
        loadInitialData();
        handleGenerateGraph();
    }
    
    // ==================== CLASSES INTERNES POUR LE DESSIN ====================
    
    /**
     * Classe pour représenter un point 2D
     */
    private static class Point2D {
        private double x, y;
        
        public Point2D(double x, double y) {
            this.x = x;
            this.y = y;
        }
        
        public double getX() { return x; }
        public double getY() { return y; }
    }
    
    /**
     * Classe pour dessiner une flèche
     */
    private static class Arrow {
        private Group group;
        
        public Arrow(double startX, double startY, double endX, double endY, int weight) {
            group = new Group();
            
            // Ligne principale
            Line line = new Line(startX, startY, endX, endY);
            double thickness = Math.min(5, 1 + weight / 5.0);
            line.setStrokeWidth(thickness);
            line.setStroke(Color.web("#95a5a6"));
            
            // Tête de flèche
            double angle = Math.atan2(endY - startY, endX - startX);
            double arrowLength = 15;
            double arrowWidth = 8;
            
            double x1 = endX - arrowLength * Math.cos(angle - Math.PI / 6);
            double y1 = endY - arrowLength * Math.sin(angle - Math.PI / 6);
            double x2 = endX - arrowLength * Math.cos(angle + Math.PI / 6);
            double y2 = endY - arrowLength * Math.sin(angle + Math.PI / 6);
            
            Polygon arrowHead = new Polygon(
                endX, endY,
                x1, y1,
                x2, y2
            );
            arrowHead.setFill(Color.web("#95a5a6"));
            
            // Label du poids si significatif
            if (weight > 5) {
                double midX = (startX + endX) / 2;
                double midY = (startY + endY) / 2;
                
                Label weightLabel = new Label(String.valueOf(weight));
                weightLabel.setLayoutX(midX - 10);
                weightLabel.setLayoutY(midY - 20);
                weightLabel.setStyle("-fx-background-color: white; -fx-padding: 2 5; " +
                                   "-fx-border-color: #95a5a6; -fx-border-radius: 3; " +
                                   "-fx-font-size: 10px; -fx-font-weight: bold;");
                
                group.getChildren().add(weightLabel);
            }
            
            group.getChildren().addAll(line, arrowHead);
        }
        
        public Group getGroup() {
            return group;
        }
    }
    
    /**
     * Classe pour dessiner un nœud de service (vue collective)
     */
    private class ServiceNode {
        private Group group;
        
        public ServiceNode(ServiceHierarchy service, WorkflowStats stats, double x, double y) {
            group = new Group();
            
            // Cercle principal
            Circle circle = new Circle(x, y, NODE_RADIUS);
            
            // Couleur selon le score de performance
            String color;
            if (stats != null) {
                int score = stats.getScorePerformance();
                if (score >= 80) {
                    color = "#27ae60";
                } else if (score >= 60) {
                    color = "#f39c12";
                } else {
                    color = "#e74c3c";
                }
            } else {
                color = "#95a5a6";
            }
            
            circle.setFill(Color.web(color));
            circle.setStroke(Color.web("#2c3e50"));
            circle.setStrokeWidth(2);
            
            // Effet goulot d'étranglement
            if (stats != null && stats.estGoulot()) {
                circle.setStrokeWidth(4);
                circle.setStroke(Color.web("#c0392b"));
            }
            
            // Texte du code de service
            Text codeText = new Text(service.getServiceCode());
            codeText.setFont(Font.font("System", FontWeight.BOLD, 12));
            codeText.setFill(Color.WHITE);
            codeText.setX(x - codeText.getLayoutBounds().getWidth() / 2);
            codeText.setY(y + 5);
            
            // Nom du service en dessous
            Text nameText = new Text(service.getServiceName());
            nameText.setFont(Font.font("System", 10));
            nameText.setFill(Color.web("#2c3e50"));
            nameText.setX(x - Math.min(nameText.getLayoutBounds().getWidth() / 2, 50));
            nameText.setY(y + NODE_RADIUS + 20);
            
            // Tooltip avec statistiques
            if (stats != null) {
                Tooltip tooltip = new Tooltip(
                    String.format("%s\nTraités: %d\nEn cours: %d\nDurée moy: %s\nScore: %d%%",
                        service.getServiceName(),
                        stats.getTotalCourriersTraites(),
                        stats.getCourriersEnCours(),
                        stats.getDureeMoyenneFormatee(),
                        stats.getScorePerformance())
                );
                Tooltip.install(circle, tooltip);
            }
            
            // Interactivité
            circle.setCursor(Cursor.HAND);
            circle.setOnMouseEntered(e -> {
                circle.setScaleX(1.1);
                circle.setScaleY(1.1);
            });
            circle.setOnMouseExited(e -> {
                circle.setScaleX(1.0);
                circle.setScaleY(1.0);
            });
            
            group.getChildren().addAll(circle, codeText, nameText);
        }
        
        public Group getGroup() {
            return group;
        }
    }
    
    /**
     * Classe pour dessiner un nœud d'étape (vue individuelle)
     */
    private class ServiceNodeStep {
        private Group group;
        
        public ServiceNodeStep(WorkflowStep step, ServiceHierarchy service, double x, double y) {
            group = new Group();
            
            // Rectangle arrondi
            Rectangle rect = new Rectangle(x - 80, y - 40, 160, 80);
            rect.setArcWidth(15);
            rect.setArcHeight(15);
            
            // Couleur selon le statut
            String color = step.getStatutEtape().getCouleur();
            rect.setFill(Color.web(color, 0.2));
            rect.setStroke(Color.web(color));
            rect.setStrokeWidth(2);
            
            // Icône du statut
            Text iconText = new Text(step.getStatutEtape().getIcone());
            iconText.setFont(Font.font(24));
            iconText.setX(x - 60);
            iconText.setY(y - 5);
            
            // Informations de l'étape
            VBox infoBox = new VBox(3);
            infoBox.setLayoutX(x - 20);
            infoBox.setLayoutY(y - 30);
            
            Text serviceText = new Text(service.getServiceCode());
            serviceText.setFont(Font.font("System", FontWeight.BOLD, 12));
            
            Text dateText = new Text(step.getDateActionFormatee());
            dateText.setFont(Font.font("System", 9));
            dateText.setFill(Color.web("#7f8c8d"));
            
            Text statutText = new Text(step.getStatutEtape().getLibelle());
            statutText.setFont(Font.font("System", 10));
            
            infoBox.getChildren().addAll(serviceText, dateText, statutText);
            
            // Tooltip
            String tooltipText = String.format(
                "Service: %s\nÉtape: %d\nStatut: %s\nDate: %s",
                service.getServiceName(),
                step.getEtapeNumero(),
                step.getStatutEtape().getLibelle(),
                step.getDateActionFormatee()
            );
            
            if (step.getCommentaire() != null && !step.getCommentaire().isEmpty()) {
                tooltipText += "\nCommentaire: " + step.getCommentaire();
            }
            
            Tooltip tooltip = new Tooltip(tooltipText);
            Tooltip.install(rect, tooltip);
            
            group.getChildren().addAll(rect, iconText, infoBox);
        }
        
        public Group getGroup() {
            return group;
        }
    }
}