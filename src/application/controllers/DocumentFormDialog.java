package application.controllers;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import application.models.*;
import application.services.DossierService;

import java.util.List;

/**
 * Dialogue pour créer ou modifier les métadonnées d'un document
 */
public class DocumentFormDialog extends Dialog<Document> {
    
    private final TextField champTitre;
    private final ComboBox<String> comboTypeDocument;
    private final ComboBox<Dossier> comboDossier;
    private final TextArea textAreaDescription;
    private final TextField champMotsCles;
    private final CheckBox checkConfidentiel;
    
    private final Document document;
    private final Dossier dossierActuel;
    private final User currentUser;
    
    /**
     * Constructeur
     */
    public DocumentFormDialog(Document document, Dossier dossierActuel, User currentUser) {
        this.document = document;
        this.dossierActuel = dossierActuel;
        this.currentUser = currentUser;
        
        // Configuration du dialogue
        setTitle(document == null ? "Nouveau document" : "Modifier le document");
        setHeaderText(document == null ? "Informations du nouveau document" : 
                                        "Modifier les informations du document");
        initModality(Modality.APPLICATION_MODAL);
        
        // Boutons
        ButtonType btnValider = new ButtonType(document == null ? "Importer" : "Modifier", 
                                               ButtonBar.ButtonData.OK_DONE);
        ButtonType btnAnnuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(btnValider, btnAnnuler);
        
        // Création du formulaire
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // Champs du formulaire
        champTitre = new TextField();
        champTitre.setPromptText("Titre du document");
        champTitre.setPrefColumnCount(30);
        
        comboTypeDocument = new ComboBox<>();
        comboTypeDocument.setItems(FXCollections.observableArrayList(
            "PERM", "AUTH", "DEM", "MP", "CORR", 
            "COMM", "CIRC", "CONV", "REP", "NOTIF",
            "RAPP", "PV", "NOTE", "CR", "AUTRE"
        ));
        comboTypeDocument.setValue("AUTRE");
        comboTypeDocument.setPromptText("Type de document");
        
        comboDossier = new ComboBox<>();
        loadDossiers();
        comboDossier.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Dossier item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getIcone() + " " + item.getNomDossier());
                }
            }
        });
        comboDossier.setButtonCell(comboDossier.getCellFactory().call(null));
        
        textAreaDescription = new TextArea();
        textAreaDescription.setPromptText("Description du document");
        textAreaDescription.setPrefRowCount(3);
        textAreaDescription.setWrapText(true);
        
        champMotsCles = new TextField();
        champMotsCles.setPromptText("Mots-clés séparés par des virgules");
        
        checkConfidentiel = new CheckBox("Document confidentiel");
        checkConfidentiel.setSelected(false);
        
        // Ajout des champs au grid
        int row = 0;
        
        grid.add(new Label("Titre:*"), 0, row);
        grid.add(champTitre, 1, row++);
        
        grid.add(new Label("Type:*"), 0, row);
        grid.add(comboTypeDocument, 1, row++);
        
        grid.add(new Label("Dossier:*"), 0, row);
        grid.add(comboDossier, 1, row++);
        
        grid.add(new Label("Description:"), 0, row);
        grid.add(textAreaDescription, 1, row++);
        
        grid.add(new Label("Mots-clés:"), 0, row);
        grid.add(champMotsCles, 1, row++);
        
        grid.add(new Label(""), 0, row);
        grid.add(checkConfidentiel, 1, row++);
        
        grid.add(new Label(""), 0, row);
        Label noteLabel = new Label("* Champs obligatoires");
        noteLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 11px;");
        grid.add(noteLabel, 1, row++);
        
        getDialogPane().setContent(grid);
        
        // Si modification, remplir les champs
        if (document != null) {
            champTitre.setText(document.getTitre());
            comboTypeDocument.setValue(document.getTypeDocument());
            textAreaDescription.setText(document.getDescription());
            champMotsCles.setText(document.getMotsCles());
            checkConfidentiel.setSelected(document.isConfidentiel());
            
            if (document.getDossierId() != null) {
                Dossier dossier = DossierService.getInstance().getDossierById(document.getDossierId());
                if (dossier != null) {
                    comboDossier.setValue(dossier);
                }
            }
        } else if (dossierActuel != null) {
            // Pré-sélectionner le dossier actuel
            comboDossier.setValue(dossierActuel);
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
                return createDocumentFromForm();
            }
            return null;
        });
    }
    
    /**
     * Charge la liste des dossiers disponibles
     */
    private void loadDossiers() {
        try {
            DossierService dossierService = DossierService.getInstance();
            List<Dossier> dossiers = dossierService.getAllDossiers();
            
            // Filtrer les dossiers système non pertinents
            dossiers = dossiers.stream()
                .filter(d -> !d.getCodeDossier().equals("ROOT") && 
                           !d.getCodeDossier().equals("CORBEILLE"))
                .collect(java.util.stream.Collectors.toList());
            
            comboDossier.setItems(FXCollections.observableArrayList(dossiers));
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des dossiers: " + e.getMessage());
        }
    }
    
    /**
     * Valide le formulaire
     */
    private boolean validateForm() {
        // Validation du titre
        if (champTitre.getText().trim().isEmpty()) {
            showError("Le titre du document est obligatoire");
            return false;
        }
        
        // Validation du type
        if (comboTypeDocument.getValue() == null) {
            showError("Le type de document est obligatoire");
            return false;
        }
        
        // Validation du dossier
        if (comboDossier.getValue() == null) {
            showError("Veuillez sélectionner un dossier");
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
     * Crée un document à partir des valeurs du formulaire
     */
    private Document createDocumentFromForm() {
        Document doc = document != null ? document : new Document();
        
        doc.setTitre(champTitre.getText().trim());
        doc.setTypeDocument(comboTypeDocument.getValue());
        doc.setDescription(textAreaDescription.getText().trim());
        doc.setMotsCles(champMotsCles.getText().trim());
        doc.setConfidentiel(checkConfidentiel.isSelected());
        
        Dossier dossierSelectionne = comboDossier.getValue();
        if (dossierSelectionne != null) {
            doc.setDossierId(dossierSelectionne.getId());
        }
        
        return doc;
    }
}