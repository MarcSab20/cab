package application.controllers;

import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import application.models.*;
import application.services.WorkflowService;
import application.utils.AlertUtils;
import application.utils.SessionManager;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.*;

/**
 * Contrôleur pour la gestion de la hiérarchie administrative
 * Permet de configurer l'organigramme et les relations entre services
 */
public class AdminHierarchyController implements Initializable {
    
    // TreeView pour la hiérarchie
    @FXML private TreeView<ServiceHierarchy> treeHierarchie;
    
    // Détails du service sélectionné
    @FXML private TextField champServiceCode;
    @FXML private TextField champServiceName;
    @FXML private ComboBox<ServiceHierarchy> comboParent;
    @FXML private Spinner<Integer> spinnerNiveau;
    @FXML private Spinner<Integer> spinnerOrdre;
    @FXML private CheckBox checkActif;
    @FXML private Label labelDateCreation;
    @FXML private Label labelNombreEnfants;
    
    // Boutons d'action
    @FXML private Button btnSauvegarder;
    @FXML private Button btnAnnuler;
    @FXML private Button btnSupprimer;
    @FXML private Button btnNouveauService;
    
    // Tableau des utilisateurs du service
    @FXML private TableView<User> tableauUtilisateurs;
    @FXML private TableColumn<User, String> colonneCode;
    @FXML private TableColumn<User, String> colonneNom;
    @FXML private TableColumn<User, String> colonnePrenom;
    @FXML private TableColumn<User, String> colonneRole;
    
    // Statistiques
    @FXML private Label statTotalServices;
    @FXML private Label statServicesActifs;
    @FXML private Label statNiveaux;
    
    // Services
    private WorkflowService workflowService;
    private User currentUser;
    
    // Données
    private ObservableList<ServiceHierarchy> servicesData;
    private ServiceHierarchy selectedService;
    private boolean isEditing = false;
    
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
            setupDetailsForm();
            setupTableUtilisateurs();
            
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
        // Configuration du rendu des cellules
        treeHierarchie.setCellFactory(tv -> new TreeCell<>() {
            @Override
            protected void updateItem(ServiceHierarchy service, boolean empty) {
                super.updateItem(service, empty);
                
                if (empty || service == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(service.getIcone() + " " + service.getServiceName() + 
                           " (" + service.getServiceCode() + ")");
                    
                    // Style selon le niveau
                    setStyle("-fx-text-fill: " + service.getCouleur() + ";");
                }
            }
        });
        
        // Listener pour la sélection
        treeHierarchie.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldVal, newVal) -> {
                if (newVal != null && newVal.getValue() != null) {
                    loadServiceDetails(newVal.getValue());
                }
            }
        );
    }
    
    /**
     * Configure le formulaire de détails
     */
    private void setupDetailsForm() {
        // Configuration du spinner de niveau
        if (spinnerNiveau != null) {
            spinnerNiveau.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 10, 0));
        }
        
        // Configuration du spinner d'ordre
        if (spinnerOrdre != null) {
            spinnerOrdre.setValueFactory(new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 100, 0));
        }
        
        // Désactiver les champs par défaut
        setFieldsEditable(false);
    }
    
    /**
     * Configure le tableau des utilisateurs
     */
    private void setupTableUtilisateurs() {
        if (colonneCode != null) {
            colonneCode.setCellValueFactory(new PropertyValueFactory<>("code"));
        }
        if (colonneNom != null) {
            colonneNom.setCellValueFactory(new PropertyValueFactory<>("nom"));
        }
        if (colonnePrenom != null) {
            colonnePrenom.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        }
        if (colonneRole != null) {
            colonneRole.setCellValueFactory(cellData -> {
                Role role = cellData.getValue().getRole();
                return new SimpleStringProperty(role != null ? role.getNom() : "");
            });
        }
    }
    
    /**
     * Charge la hiérarchie complète
     */
    private void loadHierarchy() {
        try {
            // Charger la hiérarchie depuis le cache
            workflowService.loadHierarchyCache();
            
            // Créer la racine du TreeView
            TreeItem<ServiceHierarchy> root = new TreeItem<>(null);
            root.setExpanded(true);
            
            // Récupérer les services racines
            List<ServiceHierarchy> rootServices = workflowService.getRootServices();
            
            for (ServiceHierarchy service : rootServices) {
                TreeItem<ServiceHierarchy> item = createTreeItem(service);
                root.getChildren().add(item);
            }
            
            treeHierarchie.setRoot(root);
            treeHierarchie.setShowRoot(false);
            
            System.out.println("Hiérarchie chargée: " + rootServices.size() + " services racines");
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de la hiérarchie: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Crée un TreeItem récursivement
     */
    private TreeItem<ServiceHierarchy> createTreeItem(ServiceHierarchy service) {
        TreeItem<ServiceHierarchy> item = new TreeItem<>(service);
        item.setExpanded(false);
        
        // Ajouter les enfants récursivement
        for (ServiceHierarchy enfant : service.getEnfants()) {
            TreeItem<ServiceHierarchy> childItem = createTreeItem(enfant);
            item.getChildren().add(childItem);
        }
        
        return item;
    }
    
    /**
     * Charge les détails d'un service
     */
    private void loadServiceDetails(ServiceHierarchy service) {
        selectedService = service;
        isEditing = false;
        
        if (champServiceCode != null) {
            champServiceCode.setText(service.getServiceCode());
        }
        if (champServiceName != null) {
            champServiceName.setText(service.getServiceName());
        }
        
        // Parent
        if (comboParent != null) {
            List<ServiceHierarchy> allServices = workflowService.getAllServices();
            allServices.remove(service); // Ne peut pas être son propre parent
            comboParent.setItems(FXCollections.observableArrayList(allServices));
            
            if (service.getParent() != null) {
                comboParent.setValue(service.getParent());
            }
        }
        
        if (spinnerNiveau != null) {
            spinnerNiveau.getValueFactory().setValue(service.getNiveau());
        }
        if (spinnerOrdre != null) {
            spinnerOrdre.getValueFactory().setValue(service.getOrdreAffichage());
        }
        if (checkActif != null) {
            checkActif.setSelected(service.isActif());
        }
        
        if (labelDateCreation != null && service.getDateCreation() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            labelDateCreation.setText(service.getDateCreation().format(formatter));
        }
        
        if (labelNombreEnfants != null) {
            labelNombreEnfants.setText(service.getEnfants().size() + " service(s) enfant(s)");
        }
        
        // Charger les utilisateurs de ce service
        loadUtilisateursService(service.getServiceCode());
        
        // Désactiver l'édition par défaut
        setFieldsEditable(false);
    }
    
    /**
     * Charge les utilisateurs d'un service
     */
    private void loadUtilisateursService(String serviceCode) {
        // À implémenter: récupérer les utilisateurs depuis la base de données
        // Pour l'instant, on laisse vide
        if (tableauUtilisateurs != null) {
            tableauUtilisateurs.setItems(FXCollections.observableArrayList());
        }
    }
    
    /**
     * Active/désactive l'édition des champs
     */
    private void setFieldsEditable(boolean editable) {
        if (champServiceCode != null) champServiceCode.setEditable(editable);
        if (champServiceName != null) champServiceName.setEditable(editable);
        if (comboParent != null) comboParent.setDisable(!editable);
        if (spinnerNiveau != null) spinnerNiveau.setDisable(!editable);
        if (spinnerOrdre != null) spinnerOrdre.setDisable(!editable);
        if (checkActif != null) checkActif.setDisable(!editable);
        
        if (btnSauvegarder != null) btnSauvegarder.setDisable(!editable);
        if (btnAnnuler != null) btnAnnuler.setDisable(!editable);
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
    
    /**
     * Gère la création d'un nouveau service
     */
    @FXML
    private void handleNouveauService() {
        // Créer un dialogue pour saisir les informations
        Dialog<ServiceHierarchy> dialog = new Dialog<>();
        dialog.setTitle("Nouveau service");
        dialog.setHeaderText("Créer un nouveau service");
        
        // Champs du dialogue
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        
        TextField codeField = new TextField();
        codeField.setPromptText("Code du service");
        TextField nameField = new TextField();
        nameField.setPromptText("Nom du service");
        
        ComboBox<ServiceHierarchy> parentCombo = new ComboBox<>();
        parentCombo.setItems(FXCollections.observableArrayList(workflowService.getAllServices()));
        parentCombo.setPromptText("Service parent (optionnel)");
        
        Spinner<Integer> niveauSpinner = new Spinner<>(0, 10, 0);
        Spinner<Integer> ordreSpinner = new Spinner<>(0, 100, 0);
        
        grid.add(new Label("Code:"), 0, 0);
        grid.add(codeField, 1, 0);
        grid.add(new Label("Nom:"), 0, 1);
        grid.add(nameField, 1, 1);
        grid.add(new Label("Parent:"), 0, 2);
        grid.add(parentCombo, 1, 2);
        grid.add(new Label("Niveau:"), 0, 3);
        grid.add(niveauSpinner, 1, 3);
        grid.add(new Label("Ordre:"), 0, 4);
        grid.add(ordreSpinner, 1, 4);
        
        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                ServiceHierarchy newService = new ServiceHierarchy();
                newService.setServiceCode(codeField.getText());
                newService.setServiceName(nameField.getText());
                if (parentCombo.getValue() != null) {
                    newService.setParentServiceCode(parentCombo.getValue().getServiceCode());
                }
                newService.setNiveau(niveauSpinner.getValue());
                newService.setOrdreAffichage(ordreSpinner.getValue());
                return newService;
            }
            return null;
        });
        
        Optional<ServiceHierarchy> result = dialog.showAndWait();
        result.ifPresent(service -> {
            // Enregistrer dans la base de données
            // À implémenter
            AlertUtils.showInfo("Service créé", "Le service a été créé avec succès");
            loadHierarchy();
        });
    }
    
    /**
     * Gère la sauvegarde des modifications
     */
    @FXML
    private void handleSauvegarder() {
        if (selectedService == null) {
            AlertUtils.showWarning("Aucun service sélectionné");
            return;
        }
        
        // Récupérer les valeurs des champs
        selectedService.setServiceCode(champServiceCode.getText());
        selectedService.setServiceName(champServiceName.getText());
        
        if (comboParent.getValue() != null) {
            selectedService.setParentServiceCode(comboParent.getValue().getServiceCode());
        }
        
        selectedService.setNiveau(spinnerNiveau.getValue());
        selectedService.setOrdreAffichage(spinnerOrdre.getValue());
        selectedService.setActif(checkActif.isSelected());
        
        // Enregistrer dans la base de données
        // À implémenter
        
        AlertUtils.showInfo("Service sauvegardé avec succès");
        setFieldsEditable(false);
        loadHierarchy();
    }
    
    /**
     * Gère l'annulation des modifications
     */
    @FXML
    private void handleAnnuler() {
        if (selectedService != null) {
            loadServiceDetails(selectedService);
        }
        setFieldsEditable(false);
    }
    
    /**
     * Gère la suppression d'un service
     */
    @FXML
    private void handleSupprimer() {
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
            // Supprimer de la base de données
            // À implémenter
            AlertUtils.showInfo("Service supprimé avec succès");
            loadHierarchy();
        }
    }
    
    /**
     * Active le mode édition
     */
    @FXML
    private void handleModifier() {
        setFieldsEditable(true);
        isEditing = true;
    }
    
    /**
     * Actualise la hiérarchie
     */
    @FXML
    private void handleActualiser() {
        loadHierarchy();
        updateStatistics();
    }
}