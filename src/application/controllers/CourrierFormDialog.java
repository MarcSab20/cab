package application.controllers ;

import application.models.Courrier;
import application.models.Courrier.TypeCourrier;
import application.models.Courrier.PrioriteCourrier;
import application.models.Document;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import java.time.LocalDate;

/**
 * Dialogue de création/édition de courrier
 */
public class CourrierFormDialog extends Dialog<Courrier> {
    
    private TextField txtObjet;
    private ComboBox<TypeCourrier> cmbType;
    private TextField txtExpediteur;
    private TextField txtDestinataire;
    private TextField txtReference;
    private DatePicker dpDateCourrier;
    private ComboBox<PrioriteCourrier> cmbPriorite;
    private TextArea txtObservations;
    private CheckBox chkConfidentiel;
    
    private Document document;
    
    public CourrierFormDialog(Document document) {
        this.document = document;
        
        setTitle("Créer un courrier");
        setHeaderText("Document: " + document.getTitre());
        
        // Boutons
        ButtonType btnCreer = new ButtonType("Créer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnAnnuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(btnCreer, btnAnnuler);
        
        // Formulaire
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // Objet (obligatoire)
        txtObjet = new TextField();
        txtObjet.setPromptText("Objet du courrier *");
        txtObjet.setPrefWidth(400);
        grid.add(new Label("Objet *:"), 0, 0);
        grid.add(txtObjet, 1, 0);
        
        // Type (obligatoire)
        cmbType = new ComboBox<>();
        cmbType.getItems().addAll(TypeCourrier.values());
        cmbType.setValue(TypeCourrier.ENTRANT);
        grid.add(new Label("Type *:"), 0, 1);
        grid.add(cmbType, 1, 1);
        
        // Expéditeur
        txtExpediteur = new TextField();
        txtExpediteur.setPromptText("Expéditeur");
        grid.add(new Label("Expéditeur:"), 0, 2);
        grid.add(txtExpediteur, 1, 2);
        
        // Destinataire
        txtDestinataire = new TextField();
        txtDestinataire.setPromptText("Destinataire");
        grid.add(new Label("Destinataire:"), 0, 3);
        grid.add(txtDestinataire, 1, 3);
        
        // Référence
        txtReference = new TextField();
        txtReference.setPromptText("Référence");
        grid.add(new Label("Référence:"), 0, 4);
        grid.add(txtReference, 1, 4);
        
        // Date
        dpDateCourrier = new DatePicker();
        dpDateCourrier.setValue(LocalDate.now());
        grid.add(new Label("Date:"), 0, 5);
        grid.add(dpDateCourrier, 1, 5);
        
        // Priorité
        cmbPriorite = new ComboBox<>();
        cmbPriorite.getItems().addAll(PrioriteCourrier.values());
        cmbPriorite.setValue(PrioriteCourrier.NORMALE);
        grid.add(new Label("Priorité:"), 0, 6);
        grid.add(cmbPriorite, 1, 6);
        
        // Observations
        txtObservations = new TextArea();
        txtObservations.setPromptText("Observations");
        txtObservations.setPrefRowCount(3);
        grid.add(new Label("Observations:"), 0, 7);
        grid.add(txtObservations, 1, 7);
        
        // Confidentiel
        chkConfidentiel = new CheckBox("Confidentiel");
        grid.add(chkConfidentiel, 1, 8);
        
        getDialogPane().setContent(grid);
        
        // Validation
        Button btnCreerNode = (Button) getDialogPane().lookupButton(btnCreer);
        btnCreerNode.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!validerFormulaire()) {
                event.consume();
            }
        });
        
        // Convertisseur résultat
        setResultConverter(dialogButton -> {
            if (dialogButton == btnCreer) {
                return creerCourrierFromForm();
            }
            return null;
        });
    }
    
    private boolean validerFormulaire() {
        if (txtObjet.getText() == null || txtObjet.getText().trim().isEmpty()) {
            showAlert("L'objet du courrier est obligatoire");
            return false;
        }
        
        if (cmbType.getValue() == null) {
            showAlert("Le type de courrier est obligatoire");
            return false;
        }
        
        return true;
    }
    
    private Courrier creerCourrierFromForm() {
        Courrier courrier = new Courrier();
        
        courrier.setDocumentId(document.getId());
        courrier.setObjet(txtObjet.getText().trim());
        courrier.setTypeCourrier(cmbType.getValue());
        courrier.setExpediteur(txtExpediteur.getText());
        courrier.setDestinataire(txtDestinataire.getText());
        courrier.setReference(txtReference.getText());
        courrier.setDateCourrier(dpDateCourrier.getValue());
        courrier.setPriorite(cmbPriorite.getValue());
        courrier.setObservations(txtObservations.getText());
        courrier.setConfidentiel(chkConfidentiel.isSelected());
        
        return courrier;
    }
    
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Validation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}