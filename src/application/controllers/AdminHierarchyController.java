package application.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import application.models.*;
import application.services.WorkflowService;
import application.utils.AlertUtils;
import application.utils.SessionManager;

import java.io.File;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Contrôleur pour la gestion de la hiérarchie administrative
 * VERSION CORRIGÉE avec toutes les méthodes du FXML
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
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("AdminHierarchyController.initialize()");
        
        try {
            // Initialisation
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
            
            System.out.println("AdminHierarchyController initialisé avec succès");
            
        } catch (Exception e) {
            System.err.println("Erreur dans AdminHierarchyController.initialize(): " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur d'initialisation", e.getMessage());
        }
    }
    
    /**
     * Configure le TreeView de la hiérarchie
     */
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
                    setText(service.getIcone() + " " + service.getServiceName() + 
                           " (" + service.getServiceCode() + ")");
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
    
    /**
     * Configure le TableView
     */
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
     * Configure le formulaire
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
        
        // Désactiver les champs par défaut
        setFieldsEditable(false);
    }
    
    /**
     * Configure les filtres
     */
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
    
    /**
     * Charge la hiérarchie complète
     */
    private void loadHierarchy() {
        try {
            // Charger depuis le cache
            workflowService.loadHierarchyCache();
            
            // Charger le TreeView
            loadTreeView();
            
            // Charger le tableau
            loadTableView();
            
            System.out.println("Hiérarchie chargée avec succès");
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de la hiérarchie: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur", "Impossible de charger la hiérarchie: " + e.getMessage());
        }
    }
    
    /**
     * Charge le TreeView
     */
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
    
    /**
     * Crée un TreeItem récursivement
     */
    private TreeItem<ServiceHierarchy> createTreeItem(ServiceHierarchy service) {
        TreeItem<ServiceHierarchy> item = new TreeItem<>(service);
        item.setExpanded(false);
        
        for (ServiceHierarchy enfant : service.getEnfants()) {
            TreeItem<ServiceHierarchy> childItem = createTreeItem(enfant);
            item.getChildren().add(childItem);
        }
        
        return item;
    }
    
    /**
     * Charge le tableau
     */
    private void loadTableView() {
        if (servicesTable == null) return;
        
        List<ServiceHierarchy> allServices = workflowService.getAllServices();
        servicesData.clear();
        servicesData.addAll(allServices);
        
        applyFilters();
    }
    
    /**
     * Charge les détails d'un service
     */
    private void loadServiceDetails(ServiceHierarchy service) {
        selectedService = service;
        
        if (tfServiceCode != null) tfServiceCode.setText(service.getServiceCode());
        if (tfServiceName != null) tfServiceName.setText(service.getServiceName());
        
        // Parent
        if (cbParentService != null) {
            List<ServiceHierarchy> allServices = workflowService.getAllServices();
            allServices.remove(service);
            cbParentService.setItems(FXCollections.observableArrayList(allServices));
            
            if (service.getParent() != null) {
                cbParentService.setValue(service.getParent());
            }
        }
        
        // Niveau
        if (cbNiveau != null) {
            cbNiveau.setValue("Niveau " + service.getNiveau() + ": ...");
        }
        
        if (tfOrdreAffichage != null) {
            tfOrdreAffichage.setText(String.valueOf(service.getOrdreAffichage()));
        }
        
        if (chkActif != null) {
            chkActif.setSelected(service.isActif());
        }
        
        // Activer le bouton supprimer
        if (btnDeleteService != null) {
            btnDeleteService.setDisable(false);
        }
        
        setFieldsEditable(false);
    }
    
    /**
     * Active/désactive l'édition des champs
     */
    private void setFieldsEditable(boolean editable) {
        if (tfServiceCode != null) tfServiceCode.setEditable(editable);
        if (tfServiceName != null) tfServiceName.setEditable(editable);
        if (cbParentService != null) cbParentService.setDisable(!editable);
        if (cbNiveau != null) cbNiveau.setDisable(!editable);
        if (tfOrdreAffichage != null) tfOrdreAffichage.setDisable(!editable);
        if (chkActif != null) chkActif.setDisable(!editable);
    }
    
    /**
     * Met à jour les statistiques
     */
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
    
    // ==================== HANDLERS POUR LES BOUTONS ====================
    
    /**
     * CORRECTION: Renommer en handleRefresh pour correspondre au FXML
     */
    @FXML
    private void handleRefresh() {
        loadHierarchy();
        updateStatistics();
        AlertUtils.showInfo("Hiérarchie actualisée avec succès");
    }
    
    @FXML
    private void handleExportHierarchy() {
        AlertUtils.showInfo("Fonction d'export en cours de développement");
    }
    
    @FXML
    private void handleImportHierarchy() {
        AlertUtils.showInfo("Fonction d'import en cours de développement");
    }
    
    @FXML
    private void handleShowVisualization() {
        AlertUtils.showInfo("Fonction de visualisation en cours de développement");
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
        
        // Vider les champs
        if (tfServiceCode != null) tfServiceCode.clear();
        if (tfServiceName != null) tfServiceName.clear();
        if (cbParentService != null) cbParentService.setValue(null);
        if (cbNiveau != null) cbNiveau.setValue(null);
        if (tfOrdreAffichage != null) tfOrdreAffichage.setText("1");
        if (chkActif != null) chkActif.setSelected(true);
        
        // Charger la liste des services parents
        if (cbParentService != null) {
            List<ServiceHierarchy> allServices = workflowService.getAllServices();
            cbParentService.setItems(FXCollections.observableArrayList(allServices));
        }
        
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
                // Nouveau service
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
                    "La sauvegarde en base de données nécessite une implémentation.\n" +
                    "Fonctionnalité en cours de développement.");
                
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
    
    /**
     * Applique les filtres
     */
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