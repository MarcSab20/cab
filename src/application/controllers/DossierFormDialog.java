package application.controllers;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import application.models.Dossier;

/**
 * Dialogue AMÉLIORÉ pour créer ou modifier un dossier
 * AJOUT: Champ code_dossier pour la nomenclature des documents
 */
public class DossierFormDialog extends Dialog<Dossier> {
    
    private final TextField champCodeDossier;      // NOUVEAU
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
        champCodeDossier = new TextField();
        champCodeDossier.setPromptText("Ex: PERS, ADMIN, OPS...");
        champCodeDossier.setPrefColumnCount(15);
        champCodeDossier.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        // Convertir automatiquement en majuscules et limiter à 10 caractères
        champCodeDossier.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                String filtered = newVal.toUpperCase().replaceAll("[^A-Z0-9_-]", "");
                if (filtered.length() > 10) {
                    filtered = filtered.substring(0, 10);
                }
                if (!filtered.equals(newVal)) {
                    champCodeDossier.setText(filtered);
                }
            }
        });
        
        champNomDossier = new TextField();
        champNomDossier.setPromptText("Nom du dossier");
        champNomDossier.setPrefColumnCount(30);
        
        textAreaDescription = new TextArea();
        textAreaDescription.setPromptText("Description du dossier");
        textAreaDescription.setPrefRowCount(3);
        textAreaDescription.setWrapText(true);
        
        comboIcone = new ComboBox<>();
        comboIcone.setItems(FXCollections.observableArrayList(
            "📁", "📂", "📋", "📊", "📌", "📄", "📑", "📕", "📗", "📘", 
            "📙", "🗂️", "🗄️", "📦", "🏢", "👥", "⚙️", "📮", "🔒", "⭐"
        ));
        comboIcone.setValue("📁");
        
        // Style du combo pour afficher les icônes plus grandes
        comboIcone.setStyle("-fx-font-size: 16px;");
        comboIcone.setCellFactory(param -> new ListCell<String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item);
                    setStyle("-fx-font-size: 16px;");
                }
            }
        });
        comboIcone.setButtonCell(comboIcone.getCellFactory().call(null));
        
        // Ajout des champs au grid
        int row = 0;
        
        // Information sur le dossier parent
        if (dossierParent != null) {
            Label labelParent = new Label("📂 Dossier parent: " + dossierParent.getNomDossier());
            labelParent.setStyle("-fx-font-weight: bold; -fx-text-fill: #7f8c8d; -fx-font-size: 13px;");
            grid.add(labelParent, 0, row++, 2, 1);
            
            Separator sep = new Separator();
            sep.setStyle("-fx-padding: 5 0 5 0;");
            grid.add(sep, 0, row++, 2, 1);
        }
        
        // Code du dossier
        Label lblCode = new Label("Code:*");
        lblCode.setStyle("-fx-font-weight: bold;");
        grid.add(lblCode, 0, row);
        
        VBox vboxCode = new VBox(5);
        vboxCode.getChildren().add(champCodeDossier);
        Label lblHintCode = new Label("Ce code apparaîtra dans la nomenclature des documents");
        lblHintCode.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 10px; -fx-font-style: italic;");
        vboxCode.getChildren().add(lblHintCode);
        grid.add(vboxCode, 1, row++);
        
        // Nom du dossier
        grid.add(new Label("Nom:*"), 0, row);
        grid.add(champNomDossier, 1, row++);
        
        // Icône
        grid.add(new Label("Icône:"), 0, row);
        grid.add(comboIcone, 1, row++);
        
        // Description
        grid.add(new Label("Description:"), 0, row);
        grid.add(textAreaDescription, 1, row++);
        
        // Note
        grid.add(new Label(""), 0, row);
        Label noteLabel = new Label("* Champs obligatoires");
        noteLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 11px;");
        grid.add(noteLabel, 1, row++);
        
        // Exemple de nomenclature
        if (dossier == null) {
            grid.add(new Label(""), 0, row);
            Label lblExemple = new Label("Exemple: DOC-[CODE]-2025-001-SERVICE");
            lblExemple.setStyle("-fx-text-fill: #3498db; -fx-font-size: 11px; " +
                              "-fx-font-weight: bold; -fx-font-style: italic;");
            grid.add(lblExemple, 1, row++);
        }
        
        getDialogPane().setContent(grid);
        
        // Si modification, remplir les champs
        if (dossier != null) {
            champCodeDossier.setText(dossier.getCodeDossier());
            champNomDossier.setText(dossier.getNomDossier());
            textAreaDescription.setText(dossier.getDescription());
            
            String icone = dossier.getIcone();
            if (icone != null && !icone.equals("???") && !icone.trim().isEmpty()) {
                comboIcone.setValue(icone);
            }
        }
        
        // Auto-génération du code depuis le nom (pour nouveau dossier uniquement)
        if (dossier == null) {
            champNomDossier.textProperty().addListener((obs, oldVal, newVal) -> {
                if (champCodeDossier.getText().trim().isEmpty() && newVal != null && !newVal.trim().isEmpty()) {
                    // Générer un code à partir des 4 premières lettres du nom
                    String code = newVal.trim().toUpperCase()
                        .replaceAll("[^A-Z0-9]", "")
                        .substring(0, Math.min(4, newVal.replaceAll("[^A-Z0-9]", "").length()));
                    champCodeDossier.setText(code);
                }
            });
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
        
        // Focus sur le champ code
        champCodeDossier.requestFocus();
    }
    
    /**
     * Valide le formulaire
     */
    private boolean validateForm() {
        // Validation du code
        if (champCodeDossier.getText().trim().isEmpty()) {
            showError("Le code du dossier est obligatoire.\n\n" +
                     "Ce code sera utilisé dans la nomenclature des documents.");
            champCodeDossier.requestFocus();
            return false;
        }
        
        if (champCodeDossier.getText().trim().length() < 2) {
            showError("Le code doit contenir au moins 2 caractères.");
            champCodeDossier.requestFocus();
            return false;
        }
        
        // Validation du nom
        if (champNomDossier.getText().trim().isEmpty()) {
            showError("Le nom du dossier est obligatoire.");
            champNomDossier.requestFocus();
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
        alert.setHeaderText("⚠️ Veuillez corriger les erreurs suivantes:");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Crée un dossier à partir des valeurs du formulaire
     */
    private Dossier createDossierFromForm() {
        Dossier nouveauDossier = dossier != null ? dossier : new Dossier();
        
        // CODE DOSSIER - NOUVEAU ET OBLIGATOIRE
        nouveauDossier.setCodeDossier(champCodeDossier.getText().trim().toUpperCase());
        
        nouveauDossier.setNomDossier(champNomDossier.getText().trim());
        nouveauDossier.setDescription(textAreaDescription.getText().trim());
        
        // Icône avec valeur par défaut
        String icone = comboIcone.getValue();
        if (icone == null || icone.equals("???") || icone.trim().isEmpty()) {
            icone = "📁";
        }
        nouveauDossier.setIcone(icone);
        
        if (dossierParent != null) {
            nouveauDossier.setDossierParentId(dossierParent.getId());
        }
        
        return nouveauDossier;
    }
}