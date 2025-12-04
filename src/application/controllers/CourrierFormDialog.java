package application.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import application.models.*;
import application.services.WorkflowService;
import application.utils.SessionManager;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Dialogue pour créer ou modifier un courrier
 */
public class CourrierFormDialog extends Dialog<Courrier> {
    
    private final TextField champNumeroCourrier;
    private final ComboBox<TypeCourrier> comboTypeCourrier;
    private final TextField champObjet;
    private final TextField champExpediteur;
    private final TextField champDestinataire;
    private final DatePicker dateReception;
    private final DatePicker dateDocument;
    private final ComboBox<String> comboPriorite;
    private final ComboBox<ServiceHierarchy> comboServiceDestinataire;
    private final TextArea textAreaNotes;
    private final CheckBox checkDemarrerWorkflow;
    
    private final Courrier courrier;
    private final boolean isNewCourrier;
    
    /**
     * Constructeur
     * @param courrier Courrier à modifier (null pour créer un nouveau courrier)
     */
    public CourrierFormDialog(Courrier courrier) {
        this.courrier = courrier;
        this.isNewCourrier = (courrier == null);
        
        // Configuration du dialogue
        setTitle(isNewCourrier ? "Nouveau courrier" : "Modifier le courrier");
        setHeaderText(isNewCourrier ? "Enregistrer un nouveau courrier" : 
                                     "Modifier les informations du courrier");
        initModality(Modality.APPLICATION_MODAL);
        
        // Boutons
        ButtonType btnValider = new ButtonType(isNewCourrier ? "Enregistrer" : "Modifier", 
                                               ButtonBar.ButtonData.OK_DONE);
        ButtonType btnAnnuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(btnValider, btnAnnuler);
        
        // Création du formulaire
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // Champs du formulaire
        champNumeroCourrier = new TextField();
        champNumeroCourrier.setPromptText("CRR-2025-XXX (auto-généré si vide)");
        
        comboTypeCourrier = new ComboBox<>();
        comboTypeCourrier.setItems(FXCollections.observableArrayList(TypeCourrier.values()));
        comboTypeCourrier.setValue(TypeCourrier.ENTRANT);
        comboTypeCourrier.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(TypeCourrier item, boolean empty) {
                super.updateItem(item, empty);
                setText(empty || item == null ? null : item.getIcone() + " " + item.getLibelle());
            }
        });
        comboTypeCourrier.setButtonCell(comboTypeCourrier.getCellFactory().call(null));
        
        champObjet = new TextField();
        champObjet.setPromptText("Objet du courrier");
        champObjet.setPrefColumnCount(30);
        
        champExpediteur = new TextField();
        champExpediteur.setPromptText("Expéditeur du courrier");
        
        champDestinataire = new TextField();
        champDestinataire.setPromptText("Destinataire du courrier");
        
        dateReception = new DatePicker();
        dateReception.setValue(java.time.LocalDate.now());
        dateReception.setPromptText("Date de réception");
        
        dateDocument = new DatePicker();
        dateDocument.setPromptText("Date du document (optionnel)");
        
        comboPriorite = new ComboBox<>();
        comboPriorite.setItems(FXCollections.observableArrayList(
            "Normale", "Haute", "Urgente", "Basse"
        ));
        comboPriorite.setValue("Normale");
        
        comboServiceDestinataire = new ComboBox<>();
        loadServices();
        comboServiceDestinataire.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(ServiceHierarchy item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getIcone() + " " + item.getServiceName() + " (" + item.getServiceCode() + ")");
                }
            }
        });
        comboServiceDestinataire.setButtonCell(comboServiceDestinataire.getCellFactory().call(null));
        
        textAreaNotes = new TextArea();
        textAreaNotes.setPromptText("Notes et commentaires additionnels");
        textAreaNotes.setPrefRowCount(3);
        textAreaNotes.setWrapText(true);
        
        checkDemarrerWorkflow = new CheckBox("Démarrer le workflow automatiquement");
        checkDemarrerWorkflow.setSelected(true);
        
        // Ajout des champs au grid
        int row = 0;
        
        grid.add(new Label("N° Courrier:"), 0, row);
        grid.add(champNumeroCourrier, 1, row++);
        
        grid.add(new Label("Type:"), 0, row);
        grid.add(comboTypeCourrier, 1, row++);
        
        grid.add(new Label("Objet:*"), 0, row);
        grid.add(champObjet, 1, row++);
        
        grid.add(new Label("Expéditeur:*"), 0, row);
        grid.add(champExpediteur, 1, row++);
        
        grid.add(new Label("Destinataire:"), 0, row);
        grid.add(champDestinataire, 1, row++);
        
        grid.add(new Label("Date réception:*"), 0, row);
        grid.add(dateReception, 1, row++);
        
        grid.add(new Label("Date document:"), 0, row);
        grid.add(dateDocument, 1, row++);
        
        grid.add(new Label("Priorité:"), 0, row);
        grid.add(comboPriorite, 1, row++);
        
        grid.add(new Label("Service destinataire:*"), 0, row);
        grid.add(comboServiceDestinataire, 1, row++);
        
        grid.add(new Label("Notes:"), 0, row);
        grid.add(textAreaNotes, 1, row++);
        
        grid.add(new Label(""), 0, row);
        grid.add(checkDemarrerWorkflow, 1, row++);
        
        grid.add(new Label(""), 0, row);
        Label noteLabel = new Label("* Champs obligatoires");
        noteLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 11px;");
        grid.add(noteLabel, 1, row++);
        
        getDialogPane().setContent(grid);
        
        // Si modification, remplir les champs
        if (!isNewCourrier) {
            champNumeroCourrier.setText(courrier.getNumeroCourrier());
            champNumeroCourrier.setDisable(true);
            comboTypeCourrier.setValue(courrier.getTypeCourrier());
            champObjet.setText(courrier.getObjet());
            champExpediteur.setText(courrier.getExpediteur());
            champDestinataire.setText(courrier.getDestinataire());
            
            if (courrier.getDateReception() != null) {
                dateReception.setValue(courrier.getDateReception().toLocalDate());
            }
            
            if (courrier.getDateDocument() != null) {
                dateDocument.setValue(courrier.getDateDocument().toLocalDate());
            }
            
            comboPriorite.setValue(courrier.getPriorite());
            
            if (courrier.getServiceActuel() != null) {
                ServiceHierarchy service = WorkflowService.getInstance()
                    .getServiceByCode(courrier.getServiceActuel());
                if (service != null) {
                    comboServiceDestinataire.setValue(service);
                }
            }
            
            textAreaNotes.setText(courrier.getNotes());
            checkDemarrerWorkflow.setSelected(false);
            checkDemarrerWorkflow.setDisable(true);
        }
        
        // Validation
        Button btnValidate = (Button) getDialogPane().lookupButton(btnValider);
        btnValidate.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!validateForm()) {
                event.consume();
            }
        });
        
        // Conversion du résultat
        setResultConverter(dialogButton -> {
            if (dialogButton == btnValider) {
                return createCourrierFromForm();
            }
            return null;
        });
    }
    
    /**
     * Charge la liste des services disponibles
     */
    private void loadServices() {
        try {
            WorkflowService workflowService = WorkflowService.getInstance();
            User currentUser = SessionManager.getInstance().getCurrentUser();
            
            List<ServiceHierarchy> services;
            
            if (currentUser.getNiveauAutorite() == 0 || 
                currentUser.getServiceCode() == null || 
                currentUser.getServiceCode().isEmpty()) {
                // Utilisateurs niveau 0 voient tous les services
                services = workflowService.getAllServices();
            } else {
                // Autres utilisateurs voient les services vers lesquels ils peuvent transférer
                services = workflowService.getTransferableServices(currentUser);
            }
            
            ObservableList<ServiceHierarchy> servicesList = FXCollections.observableArrayList(services);
            comboServiceDestinataire.setItems(servicesList);
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des services: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Valide le formulaire
     */
    private boolean validateForm() {
        // Validation de l'objet
        if (champObjet.getText().trim().isEmpty()) {
            showError("L'objet du courrier est obligatoire");
            return false;
        }
        
        // Validation de l'expéditeur
        if (champExpediteur.getText().trim().isEmpty()) {
            showError("L'expéditeur du courrier est obligatoire");
            return false;
        }
        
        // Validation de la date de réception
        if (dateReception.getValue() == null) {
            showError("La date de réception est obligatoire");
            return false;
        }
        
        // Validation du service destinataire
        if (checkDemarrerWorkflow.isSelected() && comboServiceDestinataire.getValue() == null) {
            showError("Veuillez sélectionner un service destinataire pour démarrer le workflow");
            return false;
        }
        
        return true;
    }
    
    /**
     * Affiche un message d'erreur
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setTitle("Erreur de validation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Crée un courrier à partir des valeurs du formulaire
     */
    private Courrier createCourrierFromForm() {
        Courrier newCourrier;
        
        if (isNewCourrier) {
            newCourrier = new Courrier();
            
            // Générer un numéro de courrier si vide
            String numero = champNumeroCourrier.getText().trim();
            if (numero.isEmpty()) {
                numero = generateNumeroCourrier();
            }
            newCourrier.setNumeroCourrier(numero);
        } else {
            newCourrier = courrier;
        }
        
        newCourrier.setTypeCourrier(comboTypeCourrier.getValue());
        newCourrier.setObjet(champObjet.getText().trim());
        newCourrier.setExpediteur(champExpediteur.getText().trim());
        newCourrier.setDestinataire(champDestinataire.getText().trim());
        
        // Date de réception
        LocalDateTime dateRec = dateReception.getValue().atStartOfDay();
        newCourrier.setDateReception(dateRec);
        
        // Date du document
        if (dateDocument.getValue() != null) {
            LocalDateTime dateDoc = dateDocument.getValue().atStartOfDay();
            newCourrier.setDateDocument(dateDoc);
        }
        
        newCourrier.setPriorite(comboPriorite.getValue());
        newCourrier.setNotes(textAreaNotes.getText().trim());
        
        // Service destinataire et workflow
        if (checkDemarrerWorkflow.isSelected() && comboServiceDestinataire.getValue() != null) {
            ServiceHierarchy service = comboServiceDestinataire.getValue();
            newCourrier.setServiceActuel(service.getServiceCode());
            newCourrier.setWorkflowActif(true);
            newCourrier.setStatut(StatutCourrier.EN_COURS);
        } else {
            newCourrier.setStatut(StatutCourrier.NOUVEAU);
        }
        
        return newCourrier;
    }
    
    /**
     * Génère un numéro de courrier automatique
     */
    private String generateNumeroCourrier() {
        int year = java.time.LocalDate.now().getYear();
        int random = (int) (Math.random() * 10000);
        return String.format("CRR-%d-%04d", year, random);
    }
    
    /**
     * Récupère le service destinataire sélectionné
     */
    public ServiceHierarchy getServiceDestinataire() {
        return comboServiceDestinataire.getValue();
    }
    
    /**
     * Indique si le workflow doit être démarré
     */
    public boolean isDemarrerWorkflow() {
        return checkDemarrerWorkflow.isSelected();
    }
}