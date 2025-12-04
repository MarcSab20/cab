package application.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.Line;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;
import javafx.stage.Modality;
import javafx.stage.Stage;
import application.models.*;
import application.services.WorkflowService;
import application.utils.AlertUtils;
import application.utils.SessionManager;
import javafx.geometry.Orientation;
import javafx.scene.control.Separator;
import javafx.scene.control.Slider;
import javafx.scene.layout.BorderPane;
import javafx.scene.layout.StackPane;
import java.util.function.Consumer;
import java.net.URL;
import java.util.*;
import java.util.stream.Collectors;


/**
 * Contrôleur AMÉLIORÉ pour la gestion de la hiérarchie administrative
 * NOUVELLES FONCTIONNALITÉS:
 * - Édition complète des services
 * - Sélection de service parent existant
 * - Visualisation graphique de l'organigramme
 */
public class AdminHierarchyController implements Initializable {
    
    // TreeView pour la hiérarchie
    @FXML private TreeView<ServiceHierarchy> hierarchyTreeView;
    
    // Tableau des services
    @FXML private TableView<ServiceHierarchy> servicesTable;
    @FXML private TableColumn<ServiceHierarchy, String> colServiceCode;
    @FXML private TableColumn<ServiceHierarchy, String> colServiceName;
    @FXML private TableColumn<ServiceHierarchy, String> colParent;
    @FXML private TableColumn<ServiceHierarchy, String> colNiveau;
    @FXML private TableColumn<ServiceHierarchy, String> colActif;
    
    // Champs du formulaire
    @FXML private TextField tfServiceCode;
    @FXML private TextField tfServiceName;
    @FXML private ComboBox<ServiceHierarchy> cbParentService;
    @FXML private ComboBox<String> cbNiveau;
    @FXML private TextField tfOrdreAffichage;
    @FXML private CheckBox chkActif;
    
    // Recherche et filtres
    @FXML private TextField tfSearch;
    @FXML private ComboBox<String> cbFilterNiveau;
    @FXML private CheckBox chkFilterActifs;
    
    // Boutons
    @FXML private Button btnSaveService;
    @FXML private Button btnDeleteService;
    @FXML private Button btnNewService;
    
    // Statistiques
    @FXML private Label statTotalServices;
    @FXML private Label statServicesActifs;
    @FXML private Label statNiveaux;
    
    // Visualisation
    @FXML private VBox hierarchyVisualization;
    
    // Services
    private WorkflowService workflowService;
    private User currentUser;
    
    // Données
    private ObservableList<ServiceHierarchy> servicesData;
    private ServiceHierarchy selectedService;
    private boolean isEditMode = false;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("AdminHierarchyController.initialize()");
        
        try {
            workflowService = WorkflowService.getInstance();
            currentUser = SessionManager.getInstance().getCurrentUser();
            servicesData = FXCollections.observableArrayList();
            
            if (currentUser == null) {
                System.err.println("ERREUR: Aucun utilisateur en session");
                return;
            }
            
            // Configuration de l'interface
            setupTreeView();
            setupTableView();
            setupForm();
            setupFilters();
            
            // Chargement des données
            loadHierarchy();
            updateStatistics();
            
            System.out.println("✅ AdminHierarchyController initialisé avec succès");
            
        } catch (Exception e) {
            System.err.println("Erreur dans AdminHierarchyController.initialize(): " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur d'initialisation", e.getMessage());
        }
    }
    
    private void setupTreeView() {
        if (hierarchyTreeView == null) return;
        
        hierarchyTreeView.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(ServiceHierarchy service, boolean empty) {
                super.updateItem(service, empty);
                
                if (empty || service == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String icon = service.getIcone();
                    String text = icon + " " + service.getServiceName() + " (" + service.getServiceCode() + ")";
                    setText(text);
                    setStyle("-fx-text-fill: " + service.getCouleur() + ";");
                }
            }
        });
        
        hierarchyTreeView.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null && newVal.getValue() != null) {
                    loadServiceDetails(newVal.getValue());
                }
            }
        );
    }
    
    private void setupTableView() {
        if (servicesTable == null) return;
        
        colServiceCode.setCellValueFactory(new PropertyValueFactory<>("serviceCode"));
        colServiceName.setCellValueFactory(new PropertyValueFactory<>("serviceName"));
        
        colParent.setCellValueFactory(cellData -> {
            String parentCode = cellData.getValue().getParentServiceCode();
            return new SimpleStringProperty(parentCode != null ? parentCode : "Racine");
        });
        
        colNiveau.setCellValueFactory(cellData -> 
            new SimpleStringProperty("Niveau " + cellData.getValue().getNiveau())
        );
        
        colActif.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().isActif() ? "✅ Oui" : "❌ Non")
        );
        
        servicesTable.setItems(servicesData);
        
        servicesTable.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null) {
                    loadServiceDetails(newVal);
                }
            }
        );
    }
    
    /**
     * AMÉLIORATION: Configure le formulaire avec services parents existants uniquement
     */
    private void setupForm() {
        // Niveaux hiérarchiques
        if (cbNiveau != null) {
            cbNiveau.setItems(FXCollections.observableArrayList(
                "Niveau -1: Service Courrier",
                "Niveau 0: CEMAA, CSP",
                "Niveau 1: MAGE, CSA",
                "Niveau 2: Sous-directions",
                "Niveau 3: Cellules",
                "Niveau 4: Chefs d'Équipe",
                "Niveau 5: Chefs d'Équipe Adjoints"
            ));
        }
        
        // Service parent - avec affichage personnalisé
        if (cbParentService != null) {
            cbParentService.setCellFactory(param -> new ListCell<>() {
                @Override
                protected void updateItem(ServiceHierarchy service, boolean empty) {
                    super.updateItem(service, empty);
                    if (empty || service == null) {
                        setText(null);
                    } else {
                        setText(service.getIcone() + " " + service.getServiceName() + 
                               " (" + service.getServiceCode() + ") - Niveau " + service.getNiveau());
                    }
                }
            });
            
            cbParentService.setButtonCell(new ListCell<>() {
                @Override
                protected void updateItem(ServiceHierarchy service, boolean empty) {
                    super.updateItem(service, empty);
                    if (empty || service == null) {
                        setText("Aucun (Service racine)");
                    } else {
                        setText(service.getServiceName() + " (" + service.getServiceCode() + ")");
                    }
                }
            });
        }
        
        // Désactiver les champs par défaut
        setFieldsEditable(false);
    }
    
    private void setupFilters() {
        if (cbFilterNiveau != null) {
            cbFilterNiveau.setItems(FXCollections.observableArrayList(
                "Tous les niveaux",
                "Niveau -1", "Niveau 0", "Niveau 1", "Niveau 2", 
                "Niveau 3", "Niveau 4", "Niveau 5"
            ));
            cbFilterNiveau.setValue("Tous les niveaux");
        }
        
        if (chkFilterActifs != null) {
            chkFilterActifs.setSelected(true);
        }
    }
    
    private void loadHierarchy() {
        try {
            workflowService.loadHierarchyCache();
            loadTreeView();
            loadTableView();
            
            System.out.println("✅ Hiérarchie chargée avec succès");
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de la hiérarchie: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur", "Impossible de charger la hiérarchie: " + e.getMessage());
        }
    }
    
    private void loadTreeView() {
        if (hierarchyTreeView == null) return;
        
        TreeItem<ServiceHierarchy> root = new TreeItem<>(null);
        root.setExpanded(true);
        
        List<ServiceHierarchy> rootServices = workflowService.getRootServices();
        
        for (ServiceHierarchy service : rootServices) {
            TreeItem<ServiceHierarchy> item = createTreeItem(service);
            root.getChildren().add(item);
        }
        
        hierarchyTreeView.setRoot(root);
        hierarchyTreeView.setShowRoot(false);
    }
    
    private TreeItem<ServiceHierarchy> createTreeItem(ServiceHierarchy service) {
        TreeItem<ServiceHierarchy> item = new TreeItem<>(service);
        item.setExpanded(false);
        
        for (ServiceHierarchy enfant : service.getEnfants()) {
            TreeItem<ServiceHierarchy> childItem = createTreeItem(enfant);
            item.getChildren().add(childItem);
        }
        
        return item;
    }
    
    private void loadTableView() {
        if (servicesTable == null) return;
        
        List<ServiceHierarchy> allServices = workflowService.getAllServices();
        servicesData.clear();
        servicesData.addAll(allServices);
        
        applyFilters();
    }
    
    /**
     * AMÉLIORATION: Charge les détails d'un service avec possibilité d'édition
     */
    private void loadServiceDetails(ServiceHierarchy service) {
        selectedService = service;
        isEditMode = false;
        
        if (tfServiceCode != null) tfServiceCode.setText(service.getServiceCode());
        if (tfServiceName != null) tfServiceName.setText(service.getServiceName());
        
        // AMÉLIORATION: Charger TOUS les services existants pour le parent
        if (cbParentService != null) {
            List<ServiceHierarchy> allServices = workflowService.getAllServices();
            
            // Exclure le service lui-même et ses descendants
            List<ServiceHierarchy> availableParents = allServices.stream()
                .filter(s -> !s.equals(service))
                .filter(s -> !s.estDescendantDe(service))
                .filter(ServiceHierarchy::isActif)
                .collect(Collectors.toList());
            
            cbParentService.setItems(FXCollections.observableArrayList(availableParents));
            
            // Sélectionner le parent actuel
            if (service.getParent() != null) {
                cbParentService.setValue(service.getParent());
            } else {
                cbParentService.setValue(null);
            }
        }
        
        // Niveau
        if (cbNiveau != null) {
            String niveauStr = "Niveau " + service.getNiveau();
            for (String item : cbNiveau.getItems()) {
                if (item.startsWith(niveauStr)) {
                    cbNiveau.setValue(item);
                    break;
                }
            }
        }
        
        if (tfOrdreAffichage != null) {
            tfOrdreAffichage.setText(String.valueOf(service.getOrdreAffichage()));
        }
        
        if (chkActif != null) {
            chkActif.setSelected(service.isActif());
        }
        
        // Activer les boutons
        if (btnDeleteService != null) {
            btnDeleteService.setDisable(false);
        }
        
        if (btnSaveService != null) {
            btnSaveService.setText("💾 Modifier le Service");
        }
        
        setFieldsEditable(true);
    }
    
    private void setFieldsEditable(boolean editable) {
        if (tfServiceCode != null) tfServiceCode.setEditable(editable);
        if (tfServiceName != null) tfServiceName.setEditable(editable);
        if (cbParentService != null) cbParentService.setDisable(!editable);
        if (cbNiveau != null) cbNiveau.setDisable(!editable);
        if (tfOrdreAffichage != null) tfOrdreAffichage.setDisable(!editable);
        if (chkActif != null) chkActif.setDisable(!editable);
    }
    
    private void updateStatistics() {
        List<ServiceHierarchy> allServices = workflowService.getAllServices();
        
        if (statTotalServices != null) {
            statTotalServices.setText(String.valueOf(allServices.size()));
        }
        
        if (statServicesActifs != null) {
            long actifs = allServices.stream().filter(ServiceHierarchy::isActif).count();
            statServicesActifs.setText(String.valueOf(actifs));
        }
        
        if (statNiveaux != null) {
            int maxNiveau = allServices.stream()
                .mapToInt(ServiceHierarchy::getNiveau)
                .max()
                .orElse(0) + 1;
            statNiveaux.setText(String.valueOf(maxNiveau));
        }
    }
    
    // ==================== HANDLERS ====================
    
    @FXML
    private void handleRefresh() {
        loadHierarchy();
        updateStatistics();
        AlertUtils.showInfo("Hiérarchie actualisée avec succès");
    }
    
    @FXML
    private void handleExportHierarchy() {
        AlertUtils.showInfo("Fonction d'export", 
            "L'export de la hiérarchie sera disponible prochainement.\n" +
            "Format : CSV avec tous les services et leurs relations.");
    }
    
    @FXML
    private void handleImportHierarchy() {
        AlertUtils.showInfo("Fonction d'import", 
            "L'import de la hiérarchie sera disponible prochainement.\n" +
            "Format accepté : CSV avec colonnes code, nom, parent, niveau.");
    }
    
    /**
     * NOUVEAU: Affiche une visualisation graphique de l'organigramme
     */
    @FXML
    private void handleShowVisualization() {
        try {
            // Créer une nouvelle fenêtre modale
            Stage visualizationStage = new Stage();
            visualizationStage.initModality(Modality.APPLICATION_MODAL);
            visualizationStage.setTitle("📊 Visualisation de la Structure Hiérarchique");
            
            // ═══════════════════════════════════════════════════════════════
            // CRÉATION DU CONTENEUR PRINCIPAL
            // ═══════════════════════════════════════════════════════════════
            
            BorderPane root = new BorderPane();
            root.setStyle("-fx-background-color: #f5f5f5;");
            
            // ═══════════════════════════════════════════════════════════════
            // BARRE D'OUTILS DE ZOOM (EN HAUT)
            // ═══════════════════════════════════════════════════════════════
            
            HBox toolBar = new HBox(15);
            toolBar.setPadding(new Insets(10));
            toolBar.setAlignment(Pos.CENTER);
            toolBar.setStyle("-fx-background-color: white; -fx-border-color: #ddd; " +
                           "-fx-border-width: 0 0 1 0;");
            
            // Bouton Zoom In
            Button btnZoomIn = new Button("🔍+ Zoom In");
            btnZoomIn.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                             "-fx-font-weight: bold; -fx-cursor: hand;");
            
            // Bouton Zoom Out
            Button btnZoomOut = new Button("🔍- Zoom Out");
            btnZoomOut.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; " +
                              "-fx-font-weight: bold; -fx-cursor: hand;");
            
            // Bouton Reset
            Button btnResetZoom = new Button("↻ Reset (100%)");
            btnResetZoom.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                                "-fx-font-weight: bold; -fx-cursor: hand;");
            
            // Slider de zoom
            Slider zoomSlider = new Slider(0.25, 2.0, 1.0);
            zoomSlider.setShowTickLabels(false);
            zoomSlider.setShowTickMarks(true);
            zoomSlider.setMajorTickUnit(0.25);
            zoomSlider.setMinorTickCount(0);
            zoomSlider.setPrefWidth(200);
            zoomSlider.setStyle("-fx-cursor: hand;");
            
            // Label du niveau de zoom
            Label zoomLabel = new Label("Zoom: 100%");
            zoomLabel.setStyle("-fx-font-size: 14px; -fx-font-weight: bold; " +
                             "-fx-text-fill: #2c3e50;");
            
            // Séparateur
            Separator sep1 = new Separator(Orientation.VERTICAL);
            Separator sep2 = new Separator(Orientation.VERTICAL);
            
            // Label d'aide
            Label helpLabel = new Label("💡 Astuce: Ctrl + Molette pour zoomer");
            helpLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #7f8c8d; " +
                             "-fx-font-style: italic;");
            
            toolBar.getChildren().addAll(
                btnZoomOut, btnZoomIn, btnResetZoom,
                sep1,
                new Label("Niveau:"), zoomSlider, zoomLabel,
                sep2,
                helpLabel
            );
            
            // ═══════════════════════════════════════════════════════════════
            // ZONE DE VISUALISATION AVEC SCROLLPANE
            // ═══════════════════════════════════════════════════════════════
            
            ScrollPane scrollPane = new ScrollPane();
            scrollPane.setFitToWidth(false);  // Important pour le zoom
            scrollPane.setFitToHeight(false); // Important pour le zoom
            scrollPane.setPannable(true);     // Permet de déplacer avec la souris
            scrollPane.setStyle("-fx-background-color: #f5f5f5;");
            
            // Pane conteneur pour l'organigramme (celui qu'on va zoomer)
            VBox organigrammeContainer = createOrganigramme();
            organigrammeContainer.setStyle("-fx-background-color: #f5f5f5; " +
                                          "-fx-padding: 50;");
            
            // Wrapper pour centrer l'organigramme
            StackPane wrapper = new StackPane(organigrammeContainer);
            wrapper.setStyle("-fx-background-color: #f5f5f5;");
            wrapper.setMinWidth(1500);  // Largeur minimale pour éviter le collapse
            wrapper.setMinHeight(1000); // Hauteur minimale
            
            scrollPane.setContent(wrapper);
            
            // ═══════════════════════════════════════════════════════════════
            // BARRE D'INFORMATIONS (EN BAS)
            // ═══════════════════════════════════════════════════════════════
            
            HBox infoBar = new HBox(20);
            infoBar.setPadding(new Insets(10));
            infoBar.setAlignment(Pos.CENTER);
            infoBar.setStyle("-fx-background-color: white; -fx-border-color: #ddd; " +
                           "-fx-border-width: 1 0 0 0;");
            
            int totalServices = workflowService.getAllServices().size();
            Label infoLabel = new Label("📦 Total de services: " + totalServices);
            infoLabel.setStyle("-fx-font-size: 12px; -fx-text-fill: #2c3e50;");
            
            Button btnExport = new Button("💾 Exporter (PNG)");
            btnExport.setStyle("-fx-background-color: #27ae60; -fx-text-fill: white; " +
                             "-fx-cursor: hand;");
            btnExport.setOnAction(e -> {
                AlertUtils.showInfo("Fonctionnalité d'export en cours de développement");
            });
            
            Button btnClose = new Button("✖ Fermer");
            btnClose.setStyle("-fx-background-color: #95a5a6; -fx-text-fill: white; " +
                            "-fx-cursor: hand;");
            btnClose.setOnAction(e -> visualizationStage.close());
            
            infoBar.getChildren().addAll(infoLabel, btnExport, btnClose);
            
            // ═══════════════════════════════════════════════════════════════
            // LOGIQUE DE ZOOM
            // ═══════════════════════════════════════════════════════════════
            
            // Fonction pour appliquer le zoom
            Consumer<Double> applyZoom = (zoomLevel) -> {
                // Appliquer la transformation Scale
                organigrammeContainer.setScaleX(zoomLevel);
                organigrammeContainer.setScaleY(zoomLevel);
                
                // Mettre à jour le label
                zoomLabel.setText(String.format("Zoom: %.0f%%", zoomLevel * 100));
                
                // Mettre à jour le slider
                zoomSlider.setValue(zoomLevel);
                
                // Ajuster la taille du wrapper pour éviter le clipping
                double newWidth = 1500 * zoomLevel;
                double newHeight = 1000 * zoomLevel;
                wrapper.setMinWidth(newWidth);
                wrapper.setMinHeight(newHeight);
            };
            
            // Bouton Zoom In (+25%)
            btnZoomIn.setOnAction(e -> {
                double currentZoom = zoomSlider.getValue();
                double newZoom = Math.min(currentZoom + 0.25, 2.0);
                applyZoom.accept(newZoom);
            });
            
            // Bouton Zoom Out (-25%)
            btnZoomOut.setOnAction(e -> {
                double currentZoom = zoomSlider.getValue();
                double newZoom = Math.max(currentZoom - 0.25, 0.25);
                applyZoom.accept(newZoom);
            });
            
            // Bouton Reset (100%)
            btnResetZoom.setOnAction(e -> applyZoom.accept(1.0));
            
            // Slider de zoom
            zoomSlider.valueProperty().addListener((obs, oldVal, newVal) -> {
                applyZoom.accept(newVal.doubleValue());
            });
            
            // Zoom avec la molette de la souris (Ctrl + Scroll)
            scrollPane.setOnScroll(event -> {
                if (event.isControlDown()) {
                    event.consume(); // Empêcher le scroll normal
                    
                    double currentZoom = zoomSlider.getValue();
                    double delta = event.getDeltaY() > 0 ? 0.1 : -0.1;
                    double newZoom = Math.max(0.25, Math.min(2.0, currentZoom + delta));
                    
                    applyZoom.accept(newZoom);
                }
            });
            
            // ═══════════════════════════════════════════════════════════════
            // ASSEMBLAGE ET AFFICHAGE
            // ═══════════════════════════════════════════════════════════════
            
            root.setTop(toolBar);
            root.setCenter(scrollPane);
            root.setBottom(infoBar);
            
            Scene scene = new Scene(root, 1400, 900);
            visualizationStage.setScene(scene);
            visualizationStage.setMaximized(false);
            
            // Afficher la fenêtre
            visualizationStage.show();
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la visualisation: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Impossible d'afficher la visualisation");
        }
    }
    
    /**
     * NOUVEAU: Crée une représentation graphique de l'organigramme
     */
    private VBox createOrganigramme() {
        VBox container = new VBox(30);
        container.setPadding(new Insets(40));
        container.setAlignment(Pos.TOP_CENTER);
        container.setStyle("-fx-background-color: #f8f9fa;");
        
        // Titre
        Label titre = new Label("📊 ORGANIGRAMME HIÉRARCHIQUE");
        titre.setFont(Font.font("System", FontWeight.BOLD, 24));
        titre.setStyle("-fx-text-fill: #2c3e50;");
        container.getChildren().add(titre);
        
        // Récupérer les services racines
        List<ServiceHierarchy> rootServices = workflowService.getRootServices();
        
        // Créer une visualisation pour chaque branche
        for (ServiceHierarchy root : rootServices) {
            VBox branche = createBrancheVisuelle(root, 0);
            container.getChildren().add(branche);
            
            // Ajouter un séparateur si ce n'est pas le dernier
            if (rootServices.indexOf(root) < rootServices.size() - 1) {
                Separator sep = new Separator();
                sep.setPrefWidth(800);
                container.getChildren().add(sep);
            }
        }
        
        return container;
    }
    
    /**
     * NOUVEAU: Crée une branche visuelle de l'organigramme
     */
    private VBox createBrancheVisuelle(ServiceHierarchy service, int profondeur) {
        VBox branche = new VBox(15);
        branche.setAlignment(Pos.TOP_CENTER);
        
        // Créer la carte du service
        VBox serviceCard = createServiceCard(service);
        branche.getChildren().add(serviceCard);
        
        // Si le service a des enfants, créer leurs branches
        List<ServiceHierarchy> enfants = service.getEnfants();
        if (!enfants.isEmpty()) {
            // Ligne de connexion
            Line connector = new Line();
            connector.setStartX(0);
            connector.setStartY(0);
            connector.setEndX(0);
            connector.setEndY(20);
            connector.setStroke(Color.web("#95a5a6"));
            connector.setStrokeWidth(2);
            branche.getChildren().add(connector);
            
            // Conteneur pour les enfants (horizontal)
            HBox enfantsContainer = new HBox(20);
            enfantsContainer.setAlignment(Pos.TOP_CENTER);
            
            for (ServiceHierarchy enfant : enfants) {
                VBox brancheEnfant = createBrancheVisuelle(enfant, profondeur + 1);
                enfantsContainer.getChildren().add(brancheEnfant);
            }
            
            branche.getChildren().add(enfantsContainer);
        }
        
        return branche;
    }
    
    /**
     * NOUVEAU: Crée une carte visuelle pour un service
     */
    private VBox createServiceCard(ServiceHierarchy service) {
        VBox card = new VBox(8);
        card.setAlignment(Pos.CENTER);
        card.setPadding(new Insets(15));
        card.setMaxWidth(200);
        card.setMinWidth(200);
        
        // Style selon le niveau
        String backgroundColor = getBackgroundColorForLevel(service.getNiveau());
        card.setStyle(
            "-fx-background-color: " + backgroundColor + ";" +
            "-fx-background-radius: 10;" +
            "-fx-border-color: " + service.getCouleur() + ";" +
            "-fx-border-width: 3;" +
            "-fx-border-radius: 10;" +
            "-fx-effect: dropshadow(three-pass-box, rgba(0,0,0,0.3), 10, 0, 0, 2);"
        );
        
        // Icône et nom
        Label icon = new Label(service.getIcone());
        icon.setFont(Font.font(32));
        
        Label code = new Label(service.getServiceCode());
        code.setFont(Font.font("System", FontWeight.BOLD, 14));
        code.setStyle("-fx-text-fill: #2c3e50;");
        
        Label name = new Label(service.getServiceName());
        name.setWrapText(true);
        name.setMaxWidth(180);
        name.setAlignment(Pos.CENTER);
        name.setFont(Font.font(11));
        name.setStyle("-fx-text-fill: #34495e;");
        
        Label niveau = new Label("Niveau " + service.getNiveau());
        niveau.setFont(Font.font(10));
        niveau.setStyle("-fx-text-fill: #7f8c8d;");
        
        card.getChildren().addAll(icon, code, name, niveau);
        
        // Indiquer si inactif
        if (!service.isActif()) {
            Label inactif = new Label("❌ INACTIF");
            inactif.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
            card.getChildren().add(inactif);
        }
        
        return card;
    }
    
    /**
     * Retourne une couleur de fond selon le niveau hiérarchique
     */
    private String getBackgroundColorForLevel(int niveau) {
        switch (niveau) {
            case -1: return "#fef5e7";  // Jaune pâle (courrier)
            case 0:  return "#fadbd8";  // Rouge pâle (direction)
            case 1:  return "#d6eaf8";  // Bleu pâle (sous-direction)
            case 2:  return "#d5f4e6";  // Vert pâle (services)
            case 3:  return "#fdebd0";  // Orange pâle (cellules)
            case 4:  return "#e8daef";  // Violet pâle (chefs équipe)
            case 5:  return "#d5dbdb";  // Gris pâle (adjoints)
            default: return "#ffffff";
        }
    }
    
    @FXML
    private void handleSearch() {
        applyFilters();
    }
    
    @FXML
    private void handleClearFilters() {
        if (tfSearch != null) tfSearch.clear();
        if (cbFilterNiveau != null) cbFilterNiveau.setValue("Tous les niveaux");
        if (chkFilterActifs != null) chkFilterActifs.setSelected(true);
        applyFilters();
    }
    
    @FXML
    private void handleNewService() {
        selectedService = null;
        isEditMode = false;
        
        // Vider les champs
        if (tfServiceCode != null) tfServiceCode.clear();
        if (tfServiceName != null) tfServiceName.clear();
        if (cbParentService != null) {
            // Charger TOUS les services actifs comme parents potentiels
            List<ServiceHierarchy> allServices = workflowService.getAllServices()
                .stream()
                .filter(ServiceHierarchy::isActif)
                .collect(Collectors.toList());
            cbParentService.setItems(FXCollections.observableArrayList(allServices));
            cbParentService.setValue(null);
        }
        if (cbNiveau != null) cbNiveau.setValue(null);
        if (tfOrdreAffichage != null) tfOrdreAffichage.setText("1");
        if (chkActif != null) chkActif.setSelected(true);
        
        setFieldsEditable(true);
        
        if (btnSaveService != null) {
            btnSaveService.setText("💾 Créer le Service");
        }
        
        if (btnDeleteService != null) {
            btnDeleteService.setDisable(true);
        }
    }
    
    @FXML
    private void handleSaveService() {
        try {
            // Validation
            if (tfServiceCode == null || tfServiceCode.getText().trim().isEmpty()) {
                AlertUtils.showWarning("Le code du service est obligatoire");
                return;
            }
            
            if (tfServiceName == null || tfServiceName.getText().trim().isEmpty()) {
                AlertUtils.showWarning("Le nom du service est obligatoire");
                return;
            }
            
            if (cbNiveau == null || cbNiveau.getValue() == null) {
                AlertUtils.showWarning("Le niveau hiérarchique est obligatoire");
                return;
            }
            
            // Extraire le numéro de niveau
            String niveauStr = cbNiveau.getValue();
            int niveau = Integer.parseInt(niveauStr.substring(niveauStr.indexOf("Niveau ") + 7, 
                                         niveauStr.indexOf(":")));
            
            // Créer ou mettre à jour
            if (selectedService == null) {
                AlertUtils.showInfo("Création de service", 
                    "La création de service nécessite une implémentation en base de données.\n" +
                    "Fonctionnalité en cours de développement.");
            } else {
                // Mise à jour
                selectedService.setServiceCode(tfServiceCode.getText().trim());
                selectedService.setServiceName(tfServiceName.getText().trim());
                selectedService.setNiveau(niveau);
                
                if (cbParentService.getValue() != null) {
                    selectedService.setParentServiceCode(cbParentService.getValue().getServiceCode());
                }
                
                if (tfOrdreAffichage.getText() != null && !tfOrdreAffichage.getText().trim().isEmpty()) {
                    selectedService.setOrdreAffichage(Integer.parseInt(tfOrdreAffichage.getText().trim()));
                }
                
                selectedService.setActif(chkActif.isSelected());
                
                AlertUtils.showInfo("Service sauvegardé", 
                    "Les modifications ont été appliquées.\n" +
                    "Note: La persistance en base nécessite une implémentation supplémentaire.");
                
                loadHierarchy();
                setFieldsEditable(false);
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la sauvegarde: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur", "Erreur lors de la sauvegarde: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleDeleteService() {
        if (selectedService == null) {
            AlertUtils.showWarning("Aucun service sélectionné");
            return;
        }
        
        // Vérifier qu'il n'a pas d'enfants
        if (!selectedService.getEnfants().isEmpty()) {
            AlertUtils.showWarning("Impossible de supprimer ce service car il a des services enfants");
            return;
        }
        
        boolean confirm = AlertUtils.showConfirmation(
            "Confirmation",
            "Êtes-vous sûr de vouloir supprimer le service " + selectedService.getServiceName() + " ?"
        );
        
        if (confirm) {
            AlertUtils.showInfo("Suppression de service", 
                "La suppression en base de données nécessite une implémentation.\n" +
                "Fonctionnalité en cours de développement.");
            loadHierarchy();
        }
    }
    
    private void applyFilters() {
        if (servicesTable == null) return;
        
        List<ServiceHierarchy> allServices = workflowService.getAllServices();
        List<ServiceHierarchy> filtered = new ArrayList<>();
        
        String searchText = tfSearch != null ? tfSearch.getText().toLowerCase() : "";
        String niveauFilter = cbFilterNiveau != null ? cbFilterNiveau.getValue() : "Tous les niveaux";
        boolean onlyActifs = chkFilterActifs != null && chkFilterActifs.isSelected();
        
        for (ServiceHierarchy service : allServices) {
            boolean matches = true;
            
            // Filtre recherche
            if (!searchText.isEmpty()) {
                if (!service.getServiceCode().toLowerCase().contains(searchText) &&
                    !service.getServiceName().toLowerCase().contains(searchText)) {
                    matches = false;
                }
            }
            
            // Filtre niveau
            if (niveauFilter != null && !niveauFilter.equals("Tous les niveaux")) {
                int niveau = Integer.parseInt(niveauFilter.replace("Niveau ", ""));
                if (service.getNiveau() != niveau) {
                    matches = false;
                }
            }
            
            // Filtre actifs uniquement
            if (onlyActifs && !service.isActif()) {
                matches = false;
            }
            
            if (matches) {
                filtered.add(service);
            }
        }
        
        servicesData.clear();
        servicesData.addAll(filtered);
    }
}