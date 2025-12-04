package application.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import application.models.Courrier;
import application.models.ServiceHierarchy;
import application.models.User;
import application.services.WorkflowService;
import application.utils.SessionManager;

import java.time.LocalDateTime;
import java.util.List;

/**
 * Dialogue pour transférer un courrier vers un autre service
 */
public class TransfertCourrierDialog extends Dialog<TransfertInfo> {
    
    private final ComboBox<ServiceHierarchy> comboServiceDestination;
    private final TextArea textAreaCommentaire;
    private final DatePicker dateEcheance;
    private final Spinner<Integer> spinnerDelaiHeures;
    private final CheckBox checkUrgent;
    
    private final Courrier courrier;
    private final User currentUser;
    
    /**
     * Constructeur
     * @param courrier Le courrier à transférer
     */
    public TransfertCourrierDialog(Courrier courrier) {
        this.courrier = courrier;
        this.currentUser = SessionManager.getInstance().getCurrentUser();
        
        // Configuration du dialogue
        setTitle("Transférer un courrier");
        setHeaderText("Transférer le courrier N° " + courrier.getNumeroCourrier() + 
                     "\nvers un autre service");
        initModality(Modality.APPLICATION_MODAL);
        
        // Boutons
        ButtonType btnTransferer = new ButtonType("Transférer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnAnnuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(btnTransferer, btnAnnuler);
        
        // Création du formulaire
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // Champs du formulaire
        comboServiceDestination = new ComboBox<>();
        loadServices();
        comboServiceDestination.setPromptText("Sélectionner un service");
        comboServiceDestination.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(ServiceHierarchy item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getIcone() + " " + item.getServiceName() + 
                           " (Niveau " + item.getNiveau() + ")");
                }
            }
        });
        comboServiceDestination.setButtonCell(comboServiceDestination.getCellFactory().call(null));
        
        textAreaCommentaire = new TextArea();
        textAreaCommentaire.setPromptText("Motif du transfert et instructions pour le service destinataire...");
        textAreaCommentaire.setPrefRowCount(4);
        textAreaCommentaire.setWrapText(true);
        
        dateEcheance = new DatePicker();
        dateEcheance.setValue(java.time.LocalDate.now().plusDays(2));
        dateEcheance.setPromptText("Date d'échéance");
        
        spinnerDelaiHeures = new Spinner<>(1, 720, 48);
        spinnerDelaiHeures.setEditable(true);
        
        checkUrgent = new CheckBox("Transfert urgent (prioritaire)");
        checkUrgent.setSelected(courrier.getPriorite() != null && 
                               (courrier.getPriorite().equalsIgnoreCase("Haute") || 
                                courrier.getPriorite().equalsIgnoreCase("Urgente")));
        
        // Ajout des champs au grid
        int row = 0;
        
        // Informations sur le courrier actuel
        Label labelInfo = new Label("Service actuel: " + 
            (courrier.getServiceActuel() != null ? courrier.getServiceActuel() : "N/A") + "\n" +
            "Priorité: " + courrier.getPriorite() + "\n" +
            "Statut: " + courrier.getStatut().getLibelle());
        labelInfo.setStyle("-fx-text-fill: #2c3e50; -fx-font-size: 12px; -fx-font-weight: bold;");
        grid.add(labelInfo, 0, row++, 2, 1);
        
        grid.add(new Separator(), 0, row++, 2, 1);
        
        grid.add(new Label("Service destination:*"), 0, row);
        grid.add(comboServiceDestination, 1, row++);
        
        grid.add(new Label("Commentaire:*"), 0, row);
        grid.add(textAreaCommentaire, 1, row++);
        
        grid.add(new Label("Échéance:"), 0, row);
        grid.add(dateEcheance, 1, row++);
        
        grid.add(new Label("Délai (heures):"), 0, row);
        grid.add(spinnerDelaiHeures, 1, row++);
        
        grid.add(new Label(""), 0, row);
        grid.add(checkUrgent, 1, row++);
        
        grid.add(new Label(""), 0, row);
        Label noteLabel = new Label("* Champs obligatoires");
        noteLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 11px;");
        grid.add(noteLabel, 1, row++);
        
        getDialogPane().setContent(grid);
        
        // Synchronisation du délai avec la date d'échéance
        spinnerDelaiHeures.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                dateEcheance.setValue(java.time.LocalDate.now().plusDays(newVal / 24));
            }
        });
        
        // Validation
        Button btnValidate = (Button) getDialogPane().lookupButton(btnTransferer);
        btnValidate.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!validateForm()) {
                event.consume();
            }
        });
        
        // Conversion du résultat
        setResultConverter(dialogButton -> {
            if (dialogButton == btnTransferer) {
                return createTransfertInfo();
            }
            return null;
        });
    }
    
    /**
     * Charge la liste des services disponibles pour le transfert
     */
    private void loadServices() {
        try {
            WorkflowService workflowService = WorkflowService.getInstance();
            List<ServiceHierarchy> services = workflowService.getTransferableServices(currentUser);
            
            // Filtrer le service actuel du courrier
            if (courrier.getServiceActuel() != null) {
                services.removeIf(s -> s.getServiceCode().equals(courrier.getServiceActuel()));
            }
            
            ObservableList<ServiceHierarchy> servicesList = FXCollections.observableArrayList(services);
            comboServiceDestination.setItems(servicesList);
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des services: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Valide le formulaire
     */
    private boolean validateForm() {
        // Validation du service destination
        if (comboServiceDestination.getValue() == null) {
            showError("Veuillez sélectionner un service destination");
            return false;
        }
        
        // Validation du commentaire
        if (textAreaCommentaire.getText().trim().isEmpty()) {
            showError("Veuillez indiquer le motif du transfert");
            return false;
        }
        
        // Validation de la date d'échéance
        if (dateEcheance.getValue() == null) {
            showError("Veuillez spécifier une date d'échéance");
            return false;
        }
        
        if (dateEcheance.getValue().isBefore(java.time.LocalDate.now())) {
            showError("La date d'échéance ne peut pas être dans le passé");
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
     * Crée l'objet TransfertInfo à partir des valeurs du formulaire
     */
    private TransfertInfo createTransfertInfo() {
        TransfertInfo info = new TransfertInfo();
        info.setServiceDestination(comboServiceDestination.getValue());
        info.setCommentaire(textAreaCommentaire.getText().trim());
        info.setDateEcheance(dateEcheance.getValue().atStartOfDay());
        info.setDelaiHeures(spinnerDelaiHeures.getValue());
        info.setUrgent(checkUrgent.isSelected());
        return info;
    }
    
}