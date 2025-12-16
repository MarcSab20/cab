package application.controllers;

import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Cursor;
import javafx.scene.Group;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.scene.text.Text;
import javafx.scene.transform.Scale;
import javafx.stage.Stage;
import application.models.*;
import application.services.*;
import application.utils.SessionManager;
import application.utils.AlertUtils;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contrôleur pour la visualisation dynamique et interactive du workflow
 * Affiche les flux de courriers entre services avec statistiques en temps réel
 */
public class WorkflowSuiviController implements Initializable {
    
    // === Contrôles FXML ===
    @FXML private ComboBox<String> cbTypeFlux;
    @FXML private DatePicker dpDebut;
    @FXML private DatePicker dpFin;
    @FXML private CheckBox chkAfficherStatistiques;
    @FXML private CheckBox chkAfficherGoulots;
    @FXML private Slider sliderZoom;
    @FXML private ScrollPane graphScrollPane;
    @FXML private Pane graphPane;
    
    // Statistiques
    @FXML private Label statTotalCourriers;
    @FXML private Label statServicesActifs;
    @FXML private Label statDureeMoyenne;
    @FXML private Label statGoulotsDetectes;
    @FXML private VBox statsDetailContainer;
    
    // Tableau détaillé
    @FXML private TableView<ServiceFlowStats> tableFluxDetails;
    @FXML private TableColumn<ServiceFlowStats, String> colService;
    @FXML private TableColumn<ServiceFlowStats, Number> colEntrants;
    @FXML private TableColumn<ServiceFlowStats, Number> colSortants;
    @FXML private TableColumn<ServiceFlowStats, Number> colInternes;
    @FXML private TableColumn<ServiceFlowStats, String> colDuree;
    @FXML private TableColumn<ServiceFlowStats, String> colStatut;
    
    // Services
    private User currentUser;
    private WorkflowService workflowService;
    private CourrierService courrierService;
    
    // Données
    private List<ServiceHierarchy> servicesAutorises;
    private Map<String, ServiceFlowStats> fluxStats;
    private List<FluxCourrier> fluxCourriers;
    
    // Constantes de dessin
    private static final double NODE_WIDTH = 150;
    private static final double NODE_HEIGHT = 60;
    private static final double VERTICAL_SPACING = 120;
    private static final double HORIZONTAL_SPACING = 300;
    private static final double MIN_ARROW_WIDTH = 2;
    private static final double MAX_ARROW_WIDTH = 20;
    
    // Zoom
    private Scale scaleTransform;
    private double currentZoom = 1.0;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("=== WorkflowVisualizationController.initialize() ===");
        
        try {
            // Initialiser les services
            currentUser = SessionManager.getInstance().getCurrentUser();
            workflowService = WorkflowService.getInstance();
            courrierService = CourrierService.getInstance();
            
            if (currentUser == null) {
                AlertUtils.showError("Aucun utilisateur connecté");
                return;
            }
            
            // Charger les services autorisés
            loadServicesAutorises();
            
            // Initialiser les composants
            initializeComponents();
            
            // Charger les données initiales
            loadInitialData();
            
            System.out.println("✓ WorkflowVisualizationController initialisé");
            
        } catch (Exception e) {
            System.err.println("Erreur initialisation: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur d'initialisation: " + e.getMessage());
        }
    }
    
    /**
     * Initialise les composants de l'interface
     */
    private void initializeComponents() {
        // Types de flux
        if (cbTypeFlux != null) {
            cbTypeFlux.getItems().addAll(
                "Tous les flux",
                "Flux entrants uniquement",
                "Flux sortants uniquement",
                "Flux internes uniquement"
            );
            cbTypeFlux.setValue("Tous les flux");
            cbTypeFlux.setOnAction(e -> regenerateGraph());
        }
        
        // Dates par défaut
        if (dpFin != null) {
            dpFin.setValue(LocalDate.now());
        }
        if (dpDebut != null) {
            dpDebut.setValue(LocalDate.now().minusMonths(1));
        }
        
        // Checkboxes
        if (chkAfficherStatistiques != null) {
            chkAfficherStatistiques.setSelected(true);
            chkAfficherStatistiques.setOnAction(e -> updateStatisticsVisibility());
        }
        
        if (chkAfficherGoulots != null) {
            chkAfficherGoulots.setSelected(true);
            chkAfficherGoulots.setOnAction(e -> regenerateGraph());
        }
        
        // Zoom
        setupZoom();
        
        // Configuration du graphPane
        if (graphPane != null) {
            graphPane.setMinSize(2000, 1500);
            graphPane.setStyle("-fx-background-color: #f8f9fa;");
        }
        
        // Configuration de la table
        setupTable();
    }
    
    /**
     * Configure le système de zoom
     */
    private void setupZoom() {
        if (sliderZoom != null && graphPane != null) {
            scaleTransform = new Scale(1.0, 1.0);
            graphPane.getTransforms().add(scaleTransform);
            
            sliderZoom.setMin(0.25);
            sliderZoom.setMax(3.0);
            sliderZoom.setValue(1.0);
            sliderZoom.setShowTickMarks(true);
            sliderZoom.setShowTickLabels(true);
            sliderZoom.setMajorTickUnit(0.5);
            
            sliderZoom.valueProperty().addListener((obs, oldVal, newVal) -> {
                currentZoom = newVal.doubleValue();
                scaleTransform.setX(currentZoom);
                scaleTransform.setY(currentZoom);
                
                // Ajuster la taille du pane
                graphPane.setMinWidth(2000 * currentZoom);
                graphPane.setMinHeight(1500 * currentZoom);
            });
        }
        
        // Zoom avec la molette
        if (graphScrollPane != null) {
            graphScrollPane.setOnScroll(event -> {
                if (event.isControlDown()) {
                    event.consume();
                    double delta = event.getDeltaY() > 0 ? 0.1 : -0.1;
                    double newZoom = Math.max(0.25, Math.min(3.0, currentZoom + delta));
                    sliderZoom.setValue(newZoom);
                }
            });
        }
    }
    
    /**
     * Configure la table des détails
     */
    private void setupTable() {
        if (tableFluxDetails == null) return;
        
        colService.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getServiceName()));
        
        colEntrants.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleIntegerProperty(data.getValue().getFluxEntrants()));
        
        colSortants.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleIntegerProperty(data.getValue().getFluxSortants()));
        
        colInternes.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleIntegerProperty(data.getValue().getFluxInternes()));
        
        colDuree.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getDureeMoyenneFormatee()));
        
        colStatut.setCellValueFactory(data -> 
            new javafx.beans.property.SimpleStringProperty(data.getValue().getStatutDescription()));
        
        // Colorier la colonne statut
        colStatut.setCellFactory(column -> new TableCell<ServiceFlowStats, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    ServiceFlowStats stats = getTableView().getItems().get(getIndex());
                    if (stats.estGoulot()) {
                        setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
                    } else if (stats.getScorePerformance() >= 80) {
                        setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
                    } else {
                        setStyle("-fx-text-fill: #f39c12;");
                    }
                }
            }
        });
    }
    
    /**
     * Charge les services autorisés selon le niveau hiérarchique
     */
    private void loadServicesAutorises() {
        servicesAutorises = new ArrayList<>();
        int niveauAutorite = currentUser.getNiveauAutorite();
        
        if (niveauAutorite == 0) {
            // Niveau 0 : voir tout
            servicesAutorises.addAll(workflowService.getAllServices());
        } else if (niveauAutorite >= 1) {
            // Autres niveaux : voir sa hiérarchie
            String serviceCode = currentUser.getServiceCode();
            if (serviceCode != null) {
                ServiceHierarchy userService = workflowService.getServiceByCode(serviceCode);
                if (userService != null) {
                    servicesAutorises.add(userService);
                    servicesAutorises.addAll(userService.getTousLesDescendants());
                }
            }
        }
        
        System.out.println("✓ " + servicesAutorises.size() + " services autorisés pour " + currentUser.getCode());
    }
    
    /**
     * Charge les données initiales
     */
    private void loadInitialData() {
        calculateFluxStatistics();
        generateGraph();
        updateStatistics();
        updateTable();
    }
    
    /**
     * Calcule les statistiques des flux
     */
    private void calculateFluxStatistics() {
        fluxStats = new HashMap<>();
        fluxCourriers = new ArrayList<>();
        
        LocalDateTime debut = dpDebut.getValue() != null ? 
            dpDebut.getValue().atStartOfDay() : LocalDateTime.now().minusMonths(1);
        LocalDateTime fin = dpFin.getValue() != null ? 
            dpFin.getValue().atTime(23, 59, 59) : LocalDateTime.now();
        
        // Récupérer tous les courriers de la période
        List<Courrier> courriers = courrierService.getAllCourriers().stream()
            .filter(c -> c.getDateReception() != null)
            .filter(c -> !c.getDateReception().isBefore(debut))
            .filter(c -> !c.getDateReception().isAfter(fin))
            .collect(Collectors.toList());
        
        // Pour chaque courrier, analyser ses étapes de workflow
        for (Courrier courrier : courriers) {
            List<WorkflowStep> steps = workflowService.getWorkflowHistory(courrier.getId());
            
            if (steps.isEmpty()) continue;
            
            // Identifier le type de courrier
            TypeCourrier typeCourrier = courrier.getTypeCourrier();
            
            for (int i = 0; i < steps.size(); i++) {
                WorkflowStep step = steps.get(i);
                String serviceCode = step.getServiceCode();
                
                // Vérifier si ce service est autorisé
                boolean isAuthorized = servicesAutorises.stream()
                    .anyMatch(s -> s.getServiceCode().equals(serviceCode));
                
                if (!isAuthorized) continue;
                
                // Obtenir ou créer les stats pour ce service
                ServiceFlowStats stats = fluxStats.computeIfAbsent(serviceCode, 
                    k -> new ServiceFlowStats(serviceCode, getServiceName(serviceCode)));
                
                // Déterminer le type de flux
                if (i == 0) {
                    // Première étape = flux entrant
                    stats.incrementFluxEntrants();
                } else if (i == steps.size() - 1 && step.getStatutEtape() == StatutEtapeWorkflow.TERMINE) {
                    // Dernière étape terminée = flux sortant
                    stats.incrementFluxSortants();
                } else {
                    // Étape intermédiaire = flux interne
                    stats.incrementFluxInternes();
                }
                
                // Calculer la durée de traitement
                if (i < steps.size() - 1) {
                    WorkflowStep nextStep = steps.get(i + 1);
                    long heures = java.time.Duration.between(
                        step.getDateAction(),
                        nextStep.getDateAction()
                    ).toHours();
                    
                    stats.ajouterDureeTraitement(heures);
                    
                    // Enregistrer le flux
                    FluxCourrier flux = new FluxCourrier(
                        courrier.getId(),
                        courrier.getNumeroCourrier(),
                        typeCourrier,
                        serviceCode,
                        nextStep.getServiceCode(),
                        heures,
                        step.getDateAction()
                    );
                    fluxCourriers.add(flux);
                }
                
                // Détecter les retards
                if (step.isEnRetard()) {
                    stats.incrementRetards();
                }
            }
        }
        
        System.out.println("✓ Statistiques calculées pour " + fluxStats.size() + " services");
    }
    
    /**
     * Obtient le nom d'un service
     */
    private String getServiceName(String serviceCode) {
        ServiceHierarchy service = workflowService.getServiceByCode(serviceCode);
        return service != null ? service.getServiceName() : serviceCode;
    }
    
    /**
     * Génère le graphe de visualisation
     */
    private void generateGraph() {
        if (graphPane == null) return;
        
        Platform.runLater(() -> {
            graphPane.getChildren().clear();
            
            try {
                // Filtrer les flux selon le type sélectionné
                String typeFlux = cbTypeFlux.getValue();
                List<FluxCourrier> fluxFiltres = filterFluxByType(fluxCourriers, typeFlux);
                
                if (fluxFiltres.isEmpty()) {
                    showEmptyGraphMessage();
                    return;
                }
                
                // Identifier les services uniques impliqués
                Set<String> servicesImpliques = new HashSet<>();
                for (FluxCourrier flux : fluxFiltres) {
                    servicesImpliques.add(flux.getServiceSource());
                    servicesImpliques.add(flux.getServiceDestination());
                }
                
                // Filtrer pour garder uniquement les services autorisés
                servicesImpliques = servicesImpliques.stream()
                    .filter(code -> servicesAutorises.stream()
                        .anyMatch(s -> s.getServiceCode().equals(code)))
                    .collect(Collectors.toSet());
                
                // Calculer les positions des nœuds
                Map<String, Point2D> positions = calculateNodePositions(new ArrayList<>(servicesImpliques));
                
                // Dessiner les flux (arêtes)
                drawFlows(fluxFiltres, positions, typeFlux);
                
                // Dessiner les nœuds
                drawNodes(servicesImpliques, positions);
                
                System.out.println("✓ Graphe généré avec " + servicesImpliques.size() + " services");
                
            } catch (Exception e) {
                System.err.println("Erreur génération graphe: " + e.getMessage());
                e.printStackTrace();
            }
        });
    }
    
    /**
     * Filtre les flux selon le type sélectionné
     */
    private List<FluxCourrier> filterFluxByType(List<FluxCourrier> flux, String typeFlux) {
        if (typeFlux == null || typeFlux.equals("Tous les flux")) {
            return new ArrayList<>(flux);
        }
        
        return flux.stream().filter(f -> {
            switch (typeFlux) {
                case "Flux entrants uniquement":
                    return f.getTypeCourrier() == TypeCourrier.ENTRANT;
                case "Flux sortants uniquement":
                    return f.getTypeCourrier() == TypeCourrier.SORTANT;
                case "Flux internes uniquement":
                    return f.getTypeCourrier() == TypeCourrier.INTERNE;
                default:
                    return true;
            }
        }).collect(Collectors.toList());
    }
    
    /**
     * Calcule les positions des nœuds dans le graphe
     */
    private Map<String, Point2D> calculateNodePositions(List<String> serviceCodes) {
        Map<String, Point2D> positions = new HashMap<>();
        
        // Grouper les services par niveau hiérarchique
        Map<Integer, List<String>> parNiveau = serviceCodes.stream()
            .collect(Collectors.groupingBy(code -> {
                ServiceHierarchy service = workflowService.getServiceByCode(code);
                return service != null ? service.getNiveau() : 999;
            }));
        
        double startY = 150;
        int niveauIndex = 0;
        
        List<Integer> niveaux = new ArrayList<>(parNiveau.keySet());
        Collections.sort(niveaux);
        
        for (Integer niveau : niveaux) {
            List<String> services = parNiveau.get(niveau);
            
            double totalWidth = services.size() * HORIZONTAL_SPACING;
            double startX = Math.max(200, (graphPane.getWidth() - totalWidth) / 2);
            
            for (int i = 0; i < services.size(); i++) {
                String serviceCode = services.get(i);
                double x = startX + i * HORIZONTAL_SPACING;
                double y = startY + niveauIndex * VERTICAL_SPACING;
                
                positions.put(serviceCode, new Point2D(x, y));
            }
            
            niveauIndex++;
        }
        
        return positions;
    }
    
    /**
     * Dessine les flux entre services
     */
    private void drawFlows(List<FluxCourrier> flux, Map<String, Point2D> positions, String typeFlux) {
        // Regrouper les flux par paire source-destination
        Map<String, List<FluxCourrier>> fluxGroupes = flux.stream()
            .collect(Collectors.groupingBy(f -> f.getServiceSource() + "->" + f.getServiceDestination()));
        
        for (Map.Entry<String, List<FluxCourrier>> entry : fluxGroupes.entrySet()) {
            List<FluxCourrier> fluxGroupe = entry.getValue();
            FluxCourrier premier = fluxGroupe.get(0);
            
            Point2D posSource = positions.get(premier.getServiceSource());
            Point2D posDest = positions.get(premier.getServiceDestination());
            
            if (posSource == null || posDest == null) continue;
            
            // Calculer l'épaisseur selon le nombre de courriers
            int nombreCourriers = fluxGroupe.size();
            double epaisseur = calculateArrowWidth(nombreCourriers);
            
            // Déterminer la couleur selon le type
            String couleur = getFlowColor(premier.getTypeCourrier(), typeFlux);
            
            // Calculer la durée moyenne
            double dureeMoyenne = fluxGroupe.stream()
                .mapToLong(FluxCourrier::getDureeHeures)
                .average()
                .orElse(0);
            
            // Dessiner la flèche
            drawCurvedArrow(posSource, posDest, epaisseur, couleur, nombreCourriers, dureeMoyenne);
        }
    }
    
    /**
     * Calcule la largeur de la flèche selon le nombre de courriers
     */
    private double calculateArrowWidth(int nombreCourriers) {
        if (nombreCourriers <= 1) return MIN_ARROW_WIDTH;
        if (nombreCourriers >= 50) return MAX_ARROW_WIDTH;
        
        return MIN_ARROW_WIDTH + (MAX_ARROW_WIDTH - MIN_ARROW_WIDTH) * 
               Math.log(nombreCourriers) / Math.log(50);
    }
    
    /**
     * Obtient la couleur du flux selon le type
     */
    private String getFlowColor(TypeCourrier type, String filtreType) {
        if (filtreType != null && filtreType.contains("entrants")) {
            return "#e67e22"; // Orange
        } else if (filtreType != null && filtreType.contains("sortants")) {
            return "#3498db"; // Bleu
        } else if (filtreType != null && filtreType.contains("internes")) {
            return "#95a5a6"; // Gris
        }
        
        // Couleur par défaut selon le type
        switch (type) {
            case ENTRANT: return "#e67e22"; // Orange
            case SORTANT: return "#3498db"; // Bleu
            case INTERNE: return "#95a5a6"; // Gris
            default: return "#34495e";
        }
    }
    
    /**
     * Dessine une flèche courbe entre deux points
     */
    private void drawCurvedArrow(Point2D start, Point2D end, double width, String color, 
                                 int count, double duree) {
        Group arrowGroup = new Group();
        
        // Calcul du point de contrôle pour la courbe de Bézier
        double midX = (start.getX() + end.getX()) / 2;
        double midY = (start.getY() + end.getY()) / 2;
        
        // Décalage perpendiculaire pour créer la courbe
        double dx = end.getX() - start.getX();
        double dy = end.getY() - start.getY();
        double length = Math.sqrt(dx * dx + dy * dy);
        double curvature = 0.2; // 20% de courbure
        
        double controlX = midX - curvature * length * (dy / length);
        double controlY = midY + curvature * length * (dx / length);
        
        // Créer le chemin courbe
        Path path = new Path();
        path.getElements().add(new MoveTo(start.getX() + NODE_WIDTH, start.getY() + NODE_HEIGHT / 2));
        path.getElements().add(new QuadCurveTo(
            controlX, controlY,
            end.getX(), end.getY() + NODE_HEIGHT / 2
        ));
        
        path.setStroke(Color.web(color));
        path.setStrokeWidth(width);
        path.setFill(null);
        path.setOpacity(0.7);
        
        // Ajouter une lueur si le flux est important
        if (count > 20) {
            javafx.scene.effect.Glow glow = new javafx.scene.effect.Glow();
            glow.setLevel(0.5);
            path.setEffect(glow);
        }
        
        arrowGroup.getChildren().add(path);
        
        // Ajouter le label avec le nombre si > 5
        if (count > 5) {
            Label countLabel = new Label(String.valueOf(count));
            countLabel.setLayoutX(controlX - 15);
            countLabel.setLayoutY(controlY - 25);
            countLabel.setStyle(
                "-fx-background-color: white; " +
                "-fx-padding: 3 8; " +
                "-fx-border-color: " + color + "; " +
                "-fx-border-width: 2; " +
                "-fx-border-radius: 10; " +
                "-fx-background-radius: 10; " +
                "-fx-font-weight: bold; " +
                "-fx-font-size: 11px;"
            );
            arrowGroup.getChildren().add(countLabel);
        }
        
        // Tooltip avec détails
        String tooltipText = String.format(
            "%d courrier%s\nDurée moyenne: %.1fh",
            count, count > 1 ? "s" : "", duree
        );
        Tooltip tooltip = new Tooltip(tooltipText);
        Tooltip.install(path, tooltip);
        
        // Interactivité
        path.setCursor(Cursor.HAND);
        path.setOnMouseEntered(e -> {
            path.setStrokeWidth(width * 1.5);
            path.setOpacity(1.0);
        });
        path.setOnMouseExited(e -> {
            path.setStrokeWidth(width);
            path.setOpacity(0.7);
        });
        
        graphPane.getChildren().add(arrowGroup);
    }
    
    /**
     * Dessine les nœuds représentant les services
     */
    private void drawNodes(Set<String> serviceCodes, Map<String, Point2D> positions) {
        for (String serviceCode : serviceCodes) {
            Point2D pos = positions.get(serviceCode);
            if (pos == null) continue;
            
            ServiceHierarchy service = workflowService.getServiceByCode(serviceCode);
            ServiceFlowStats stats = fluxStats.get(serviceCode);
            
            if (service == null) continue;
            
            VBox nodeBox = createServiceNode(service, stats, pos.getX(), pos.getY());
            graphPane.getChildren().add(nodeBox);
        }
    }
    
    /**
     * Crée un nœud visuel pour un service
     */
    private VBox createServiceNode(ServiceHierarchy service, ServiceFlowStats stats, double x, double y) {
        VBox node = new VBox(8);
        node.setLayoutX(x);
        node.setLayoutY(y);
        node.setPrefWidth(NODE_WIDTH);
        node.setMinHeight(NODE_HEIGHT);
        node.setAlignment(Pos.CENTER);
        node.setPadding(new Insets(12));
        
        // Style selon les performances
        String borderColor = "#3498db";
        String backgroundColor = "#ffffff";
        
        if (stats != null) {
            if (stats.estGoulot() && chkAfficherGoulots.isSelected()) {
                borderColor = "#e74c3c";
                backgroundColor = "#fde6e6";
            } else if (stats.getScorePerformance() >= 80) {
                borderColor = "#27ae60";
                backgroundColor = "#e8f8f5";
            } else if (stats.getScorePerformance() < 60) {
                borderColor = "#f39c12";
                backgroundColor = "#fef5e7";
            }
        }
        
        node.setStyle(
            "-fx-background-color: " + backgroundColor + ";" +
            "-fx-border-color: " + borderColor + ";" +
            "-fx-border-width: 3;" +
            "-fx-border-radius: 12;" +
            "-fx-background-radius: 12;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 8, 0, 0, 3);"
        );
        
        // Icône
        Label iconLabel = new Label(service.getIcone());
        iconLabel.setFont(Font.font(24));
        
        // Code du service
        Label codeLabel = new Label(service.getServiceCode());
        codeLabel.setFont(Font.font("System", FontWeight.BOLD, 13));
        codeLabel.setStyle("-fx-text-fill: #2c3e50;");
        
        // Nom du service (tronqué si trop long)
        String serviceName = service.getServiceName();
        if (serviceName.length() > 20) {
            serviceName = serviceName.substring(0, 17) + "...";
        }
        Label nameLabel = new Label(serviceName);
        nameLabel.setFont(Font.font(10));
        nameLabel.setStyle("-fx-text-fill: #7f8c8d;");
        nameLabel.setWrapText(true);
        nameLabel.setMaxWidth(NODE_WIDTH - 20);
        
        node.getChildren().addAll(iconLabel, codeLabel, nameLabel);
        
        // Statistiques si disponibles
        if (stats != null) {
            HBox statsBox = new HBox(8);
            statsBox.setAlignment(Pos.CENTER);
            
            if (stats.getFluxEntrants() > 0) {
                Label entrants = new Label("↓" + stats.getFluxEntrants());
                entrants.setStyle("-fx-font-size: 9px; -fx-text-fill: #e67e22; -fx-font-weight: bold;");
                statsBox.getChildren().add(entrants);
            }
            
            if (stats.getFluxSortants() > 0) {
                Label sortants = new Label("↑" + stats.getFluxSortants());
                sortants.setStyle("-fx-font-size: 9px; -fx-text-fill: #3498db; -fx-font-weight: bold;");
                statsBox.getChildren().add(sortants);
            }
            
            if (stats.getFluxInternes() > 0) {
                Label internes = new Label("↔" + stats.getFluxInternes());
                internes.setStyle("-fx-font-size: 9px; -fx-text-fill: #95a5a6; -fx-font-weight: bold;");
                statsBox.getChildren().add(internes);
            }
            
            if (!statsBox.getChildren().isEmpty()) {
                node.getChildren().add(statsBox);
            }
        }
        
        // Tooltip détaillé
        if (stats != null) {
            String tooltipText = String.format(
                "%s\n\n" +
                "Flux entrants: %d\n" +
                "Flux sortants: %d\n" +
                "Flux internes: %d\n" +
                "Durée moyenne: %s\n" +
                "Score: %d%%\n" +
                "%s",
                service.getServiceName(),
                stats.getFluxEntrants(),
                stats.getFluxSortants(),
                stats.getFluxInternes(),
                stats.getDureeMoyenneFormatee(),
                stats.getScorePerformance(),
                stats.estGoulot() ? "⚠️ GOULOT DÉTECTÉ" : "✓ Flux normal"
            );
            Tooltip tooltip = new Tooltip(tooltipText);
            Tooltip.install(node, tooltip);
        }
        
        // Interactivité
        node.setCursor(Cursor.HAND);
        node.setOnMouseEntered(e -> {
            node.setScaleX(1.1);
            node.setScaleY(1.1);
        });
        node.setOnMouseExited(e -> {
            node.setScaleX(1.0);
            node.setScaleY(1.0);
        });
        
        return node;
    }
    
    /**
     * Met à jour les statistiques globales
     */
    private void updateStatistics() {
        int totalCourriers = fluxCourriers.stream()
            .map(FluxCourrier::getCourrierId)
            .collect(Collectors.toSet())
            .size();
        
        int servicesActifs = fluxStats.size();
        
        double dureeMoyenne = fluxStats.values().stream()
            .mapToDouble(ServiceFlowStats::getDureeMoyenne)
            .filter(d -> d > 0)
            .average()
            .orElse(0);
        
        long goulotsDetectes = fluxStats.values().stream()
            .filter(ServiceFlowStats::estGoulot)
            .count();
        
        if (statTotalCourriers != null) {
            statTotalCourriers.setText(String.valueOf(totalCourriers));
        }
        
        if (statServicesActifs != null) {
            statServicesActifs.setText(String.valueOf(servicesActifs));
        }
        
        if (statDureeMoyenne != null) {
            if (dureeMoyenne < 1) {
                statDureeMoyenne.setText(String.format("%.0f min", dureeMoyenne * 60));
            } else if (dureeMoyenne < 24) {
                statDureeMoyenne.setText(String.format("%.1f h", dureeMoyenne));
            } else {
                statDureeMoyenne.setText(String.format("%.1f j", dureeMoyenne / 24));
            }
        }
        
        if (statGoulotsDetectes != null) {
            statGoulotsDetectes.setText(String.valueOf(goulotsDetectes));
            if (goulotsDetectes > 0) {
                statGoulotsDetectes.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            } else {
                statGoulotsDetectes.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold;");
            }
        }
    }
    
    /**
     * Met à jour la table des détails
     */
    private void updateTable() {
        if (tableFluxDetails == null) return;
        
        List<ServiceFlowStats> statsList = new ArrayList<>(fluxStats.values());
        statsList.sort((a, b) -> {
            // Trier par goulots d'abord, puis par score
            if (a.estGoulot() != b.estGoulot()) {
                return a.estGoulot() ? -1 : 1;
            }
            return Integer.compare(b.getScorePerformance(), a.getScorePerformance());
        });
        
        tableFluxDetails.getItems().clear();
        tableFluxDetails.getItems().addAll(statsList);
    }
    
    /**
     * Affiche un message quand le graphe est vide
     */
    private void showEmptyGraphMessage() {
        VBox emptyBox = new VBox(20);
        emptyBox.setAlignment(Pos.CENTER);
        emptyBox.setLayoutX(graphPane.getWidth() / 2 - 200);
        emptyBox.setLayoutY(graphPane.getHeight() / 2 - 100);
        emptyBox.setPrefWidth(400);
        
        Label iconLabel = new Label("📊");
        iconLabel.setFont(Font.font(64));
        iconLabel.setStyle("-fx-text-fill: #bdc3c7;");
        
        Label messageLabel = new Label("Aucun flux de courriers pour la période sélectionnée");
        messageLabel.setFont(Font.font(16));
        messageLabel.setStyle("-fx-text-fill: #7f8c8d;");
        messageLabel.setWrapText(true);
        messageLabel.setMaxWidth(380);
        
        Label hintLabel = new Label("Essayez de modifier les filtres ou la période");
        hintLabel.setFont(Font.font(12));
        hintLabel.setStyle("-fx-text-fill: #95a5a6;");
        
        emptyBox.getChildren().addAll(iconLabel, messageLabel, hintLabel);
        graphPane.getChildren().add(emptyBox);
    }
    
    // === HANDLERS ===
    
    @FXML
    private void handleGenerate() {
        calculateFluxStatistics();
        generateGraph();
        updateStatistics();
        updateTable();
    }
    
    @FXML
    private void handleExport() {
        AlertUtils.showInfo("Export", "Fonctionnalité d'export en cours de développement");
    }
    
    @FXML
    private void handleZoomIn() {
        if (sliderZoom != null) {
            sliderZoom.setValue(Math.min(3.0, currentZoom + 0.25));
        }
    }
    
    @FXML
    private void handleZoomOut() {
        if (sliderZoom != null) {
            sliderZoom.setValue(Math.max(0.25, currentZoom - 0.25));
        }
    }
    
    @FXML
    private void handleResetZoom() {
        if (sliderZoom != null) {
            sliderZoom.setValue(1.0);
        }
    }
    
    private void regenerateGraph() {
        generateGraph();
    }
    
    private void updateStatisticsVisibility() {
        boolean visible = chkAfficherStatistiques != null && chkAfficherStatistiques.isSelected();
        if (statsDetailContainer != null) {
            statsDetailContainer.setVisible(visible);
            statsDetailContainer.setManaged(visible);
        }
    }
    
    // === CLASSES INTERNES ===
    
    /**
     * Classe pour représenter un point 2D
     */
    private static class Point2D {
        private final double x, y;
        
        public Point2D(double x, double y) {
            this.x = x;
            this.y = y;
        }
        
        public double getX() { return x; }
        public double getY() { return y; }
    }
    
    /**
     * Classe pour représenter un flux de courrier
     */
    private static class FluxCourrier {
        private final int courrierId;
        private final String numeroCourrier;
        private final TypeCourrier typeCourrier;
        private final String serviceSource;
        private final String serviceDestination;
        private final long dureeHeures;
        private final LocalDateTime dateFlux;
        
        public FluxCourrier(int courrierId, String numeroCourrier, TypeCourrier typeCourrier,
                           String serviceSource, String serviceDestination, long dureeHeures,
                           LocalDateTime dateFlux) {
            this.courrierId = courrierId;
            this.numeroCourrier = numeroCourrier;
            this.typeCourrier = typeCourrier;
            this.serviceSource = serviceSource;
            this.serviceDestination = serviceDestination;
            this.dureeHeures = dureeHeures;
            this.dateFlux = dateFlux;
        }
        
        public int getCourrierId() { return courrierId; }
        public String getNumeroCourrier() { return numeroCourrier; }
        public TypeCourrier getTypeCourrier() { return typeCourrier; }
        public String getServiceSource() { return serviceSource; }
        public String getServiceDestination() { return serviceDestination; }
        public long getDureeHeures() { return dureeHeures; }
        public LocalDateTime getDateFlux() { return dateFlux; }
    }
    
    /**
     * Classe pour les statistiques de flux d'un service
     */
    public static class ServiceFlowStats {
        private final String serviceCode;
        private final String serviceName;
        private int fluxEntrants;
        private int fluxSortants;
        private int fluxInternes;
        private double dureeMoyenne;
        private int nombreDurees;
        private int retards;
        
        public ServiceFlowStats(String serviceCode, String serviceName) {
            this.serviceCode = serviceCode;
            this.serviceName = serviceName;
        }
        
        public void incrementFluxEntrants() { fluxEntrants++; }
        public void incrementFluxSortants() { fluxSortants++; }
        public void incrementFluxInternes() { fluxInternes++; }
        public void incrementRetards() { retards++; }
        
        public void ajouterDureeTraitement(long heures) {
            dureeMoyenne = (dureeMoyenne * nombreDurees + heures) / (nombreDurees + 1);
            nombreDurees++;
        }
        
        public String getServiceCode() { return serviceCode; }
        public String getServiceName() { return serviceName; }
        public int getFluxEntrants() { return fluxEntrants; }
        public int getFluxSortants() { return fluxSortants; }
        public int getFluxInternes() { return fluxInternes; }
        public double getDureeMoyenne() { return dureeMoyenne; }
        public int getRetards() { return retards; }
        
        public String getDureeMoyenneFormatee() {
            if (dureeMoyenne < 1) {
                return String.format("%.0f min", dureeMoyenne * 60);
            } else if (dureeMoyenne < 24) {
                return String.format("%.1f h", dureeMoyenne);
            } else {
                return String.format("%.1f j", dureeMoyenne / 24);
            }
        }
        
        public int getScorePerformance() {
            int total = fluxEntrants + fluxSortants + fluxInternes;
            if (total == 0) return 100;
            
            double tauxRetard = (double) retards / total;
            double score = 100 - (tauxRetard * 50);
            
            if (dureeMoyenne > 48) {
                score -= 20;
            } else if (dureeMoyenne > 24) {
                score -= 10;
            }
            
            return Math.max(0, Math.min(100, (int) score));
        }
        
        public boolean estGoulot() {
            int total = fluxEntrants + fluxSortants + fluxInternes;
            return dureeMoyenne > 24 || (total > 0 && retards > total * 0.3);
        }
        
        public String getStatutDescription() {
            if (estGoulot()) {
                return "⚠️ Goulot";
            } else if (getScorePerformance() >= 80) {
                return "✓ Excellent";
            } else if (getScorePerformance() >= 60) {
                return "◐ Satisfaisant";
            } else {
                return "◯ À améliorer";
            }
        }
    }
}