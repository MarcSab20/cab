package application.controllers;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import application.models.Dossier;

/**
 * Dialogue pour créer ou modifier un dossier
 */
public class DossierFormDialog extends Dialog<Dossier> {
    
    private final TextField champNomDossier;
    private final TextArea textAreaDescription;
    private final ComboBox<String> comboIcone;
    
    private final Dossier dossier;
    private final Dossier dossierParent;
    
    /**
     * Constructeur
     */
    public DossierFormDialog(Dossier dossier, Dossier dossierParent) {
        this.dossier = dossier;
        this.dossierParent = dossierParent;
        
        // Configuration du dialogue
        setTitle(dossier == null ? "Nouveau dossier" : "Modifier le dossier");
        setHeaderText(dossier == null ? "Créer un nouveau dossier" : 
                                       "Modifier les informations du dossier");
        initModality(Modality.APPLICATION_MODAL);
        
        // Boutons
        ButtonType btnValider = new ButtonType(dossier == null ? "Créer" : "Modifier", 
                                               ButtonBar.ButtonData.OK_DONE);
        ButtonType btnAnnuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(btnValider, btnAnnuler);
        
        // Création du formulaire
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // Champs du formulaire
        champNomDossier = new TextField();
        champNomDossier.setPromptText("Nom du dossier");
        champNomDossier.setPrefColumnCount(30);
        
        textAreaDescription = new TextArea();
        textAreaDescription.setPromptText("Description du dossier");
        textAreaDescription.setPrefRowCount(3);
        textAreaDescription.setWrapText(true);
        
        comboIcone = new ComboBox<>();
        comboIcone.setItems(FXCollections.observableArrayList(
            "📁", "📂", "📋", "📊", "📝", "📄", "📑", "📕", "📗", "📘", "📙", "🗂️", "🗄️", "📦"
        ));
        comboIcone.setValue("📁");
        
        // Ajout des champs au grid
        int row = 0;
        
        // Information sur le dossier parent
        if (dossierParent != null) {
            Label labelParent = new Label("Dossier parent: " + dossierParent.getNomDossier());
            labelParent.setStyle("-fx-font-weight: bold; -fx-text-fill: #7f8c8d;");
            grid.add(labelParent, 0, row++, 2, 1);
            
            grid.add(new Separator(), 0, row++, 2, 1);
        }
        
        grid.add(new Label("Nom:*"), 0, row);
        grid.add(champNomDossier, 1, row++);
        
        grid.add(new Label("Icône:"), 0, row);
        grid.add(comboIcone, 1, row++);
        
        grid.add(new Label("Description:"), 0, row);
        grid.add(textAreaDescription, 1, row++);
        
        grid.add(new Label(""), 0, row);
        Label noteLabel = new Label("* Champs obligatoires");
        noteLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 11px;");
        grid.add(noteLabel, 1, row++);
        
        getDialogPane().setContent(grid);
        
        // Si modification, remplir les champs
        if (dossier != null) {
            champNomDossier.setText(dossier.getNomDossier());
            textAreaDescription.setText(dossier.getDescription());
            comboIcone.setValue(dossier.getIcone());
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
                return createDossierFromForm();
            }
            return null;
        });
    }
    
    /**
     * Valide le formulaire
     */
    private boolean validateForm() {
        // Validation du nom
        if (champNomDossier.getText().trim().isEmpty()) {
            showError("Le nom du dossier est obligatoire");
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
     * Crée un dossier à partir des valeurs du formulaire
     */
    private Dossier createDossierFromForm() {
        Dossier nouveauDossier = dossier != null ? dossier : new Dossier();
        
        nouveauDossier.setNomDossier(champNomDossier.getText().trim());
        nouveauDossier.setDescription(textAreaDescription.getText().trim());
        nouveauDossier.setIcone(comboIcone.getValue());
        
        if (dossierParent != null) {
            nouveauDossier.setDossierParentId(dossierParent.getId());
        }
        
        return nouveauDossier;
    }
}