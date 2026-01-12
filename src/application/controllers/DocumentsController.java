package application.controllers;

import application.models.Document;
import application.models.Dossier;
import application.models.User;
import application.services.DocumentService;
import application.services.DossierService;
import application.services.NetworkStorageService;
import application.utils.SessionManager;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import javafx.beans.property.SimpleStringProperty;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;

public class DocumentsController {
    
    // TableView
    @FXML private TableView<Document> tableauDocuments;
    @FXML private TableColumn<Document, String> colonneNomDocument;
    @FXML private TableColumn<Document, String> colonneTypeDocument;
    @FXML private TableColumn<Document, String> colonneTailleDocument;
    @FXML private TableColumn<Document, String> colonneAuteurDocument;
    @FXML private TableColumn<Document, String> colonneDateModification;
    @FXML private TableColumn<Document, String> colonneStatutDocument;
    @FXML private TableColumn<Document, String> colonneActionsDocument;
    
    // Arborescence
    @FXML private TreeView<Dossier> arborescenceDossiers;
    
    // Panneau de détails - NOUVEAUX CHAMPS
    @FXML private VBox panneauDetailsDocument;
    @FXML private VBox zoneApercu;
    @FXML private Label labelCodeDocument;
    @FXML private Label labelNomFichier;
    @FXML private Label labelTypeFichier;
    @FXML private Label labelTailleFichier;
    @FXML private Label labelDateCreation;
    @FXML private Label labelDateModification;
    @FXML private Label labelAuteur;
    @FXML private Label labelStatutDocument;
    @FXML private TextArea textAreaDescription;
    @FXML private FlowPane flowPaneMotsCles;
    
    // Boutons actions rapides - NOUVEAUX
    @FXML private Button btnModifierDocument;
    @FXML private Button btnPartagerDocument;
    @FXML private Button btnDeplacerDocument;
    @FXML private Button btnSupprimerDocument;
    @FXML private Button btnApercuComplet;
    @FXML private Button btnTelecharger;
    
    // Autres champs
    @FXML private TextField champRechercheDoc;
    @FXML private Label labelDossierActuel;
    @FXML private Label labelNombreDocuments;
    @FXML private Button btnImporter;
    @FXML private Button btnNouveauDossier;
    
    // Services
    private DocumentService documentService;
    private DossierService dossierService;
    private NetworkStorageService networkStorageService;
    
    private Dossier dossierActuel;
    private Document documentSelectionne;
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    @FXML
    public void initialize() {
        System.out.println("=== INITIALISATION DocumentsController ===");
        
        documentService = DocumentService.getInstance();
        dossierService = DossierService.getInstance();
        networkStorageService = NetworkStorageService.getInstance();
        
        configurerColonnesTableau();
        configurerSelectionDocument();
        chargerArborescence();
        chargerDocuments();
        
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            System.out.println("✓ Initialisé pour: " + currentUser.getNomComplet());
        }
        
        System.out.println("=== FIN INITIALISATION ===");
    }
    
    /**
     * NOUVELLE MÉTHODE : Configure la sélection de document
     */
    private void configurerSelectionDocument() {
        if (tableauDocuments == null) return;
        
        tableauDocuments.getSelectionModel().selectedItemProperty().addListener(
            (observable, oldValue, newValue) -> {
                if (newValue != null) {
                    afficherDetailsDocument(newValue);
                } else {
                    cacherDetailsDocument();
                }
            }
        );
        
        System.out.println("✓ Sélection de document configurée");
    }
    
    /**
     * NOUVELLE MÉTHODE : Affiche les détails d'un document
     */
    private void afficherDetailsDocument(Document doc) {
        this.documentSelectionne = doc;
        
        System.out.println("📄 Affichage des détails de: " + doc.getTitre());
        
        // Remplir les labels
        if (labelCodeDocument != null) {
            labelCodeDocument.setText(doc.getCodeDocument() != null ? doc.getCodeDocument() : "-");
        }
        
        if (labelNomFichier != null) {
            labelNomFichier.setText(doc.getTitre() != null ? doc.getTitre() : "Sans titre");
        }
        
        if (labelTypeFichier != null) {
            String type = "Document";
            if (doc.getExtension() != null) {
                type = "Document " + doc.getExtension().toUpperCase();
            }
            labelTypeFichier.setText(type);
        }
        
        if (labelTailleFichier != null) {
            labelTailleFichier.setText(doc.getTailleFormatee());
        }
        
        if (labelDateCreation != null && doc.getDateCreation() != null) {
            labelDateCreation.setText(doc.getDateCreation().format(DATE_FORMATTER));
        }
        
        if (labelDateModification != null && doc.getDateModification() != null) {
            labelDateModification.setText(doc.getDateModification().format(DATE_FORMATTER));
        }
        
        if (labelAuteur != null) {
            labelAuteur.setText(doc.getCreateurNom() != null ? doc.getCreateurNom() : "-");
        }
        
        if (labelStatutDocument != null) {
            String statut = doc.getStatut() != null ? doc.getStatut() : "actif";
            String statutFormate = switch (statut.toLowerCase()) {
                case "brouillon" -> "📝 Brouillon";
                case "en_revision" -> "🔄 En révision";
                case "valide" -> "✅ Validé";
                case "archive" -> "📦 Archivé";
                default -> "📄 " + statut;
            };
            labelStatutDocument.setText(statutFormate);
        }
        
        if (textAreaDescription != null) {
            textAreaDescription.setText(doc.getDescription() != null ? doc.getDescription() : "Aucune description");
        }
        
        // Afficher l'aperçu
        afficherApercu(doc);
        
        // Activer les boutons d'actions
        activerBoutonsActions(true);
    }
    
    /**
     * NOUVELLE MÉTHODE : Affiche l'aperçu du document
     */
    private void afficherApercu(Document doc) {
        if (zoneApercu == null) return;
        
        zoneApercu.getChildren().clear();
        
        String extension = doc.getExtension() != null ? doc.getExtension().toLowerCase() : "";
        
        // Choisir l'icône selon le type
        String icone = switch (extension) {
            case "pdf" -> "📕";
            case "doc", "docx" -> "📘";
            case "xls", "xlsx" -> "📗";
            case "ppt", "pptx" -> "📙";
            case "txt" -> "📄";
            case "jpg", "jpeg", "png", "gif" -> "🖼️";
            case "zip", "rar" -> "📦";
            default -> "📄";
        };
        
        Label lblIcone = new Label(icone);
        lblIcone.setStyle("-fx-font-size: 64px;");
        
        Label lblNom = new Label(doc.getTitre());
        lblNom.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        lblNom.setWrapText(true);
        
        Label lblType = new Label(extension.toUpperCase() + " • " + doc.getTailleFormatee());
        lblType.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");
        
        VBox contenu = new VBox(10, lblIcone, lblNom, lblType);
        contenu.setStyle("-fx-alignment: center; -fx-padding: 20;");
        
        zoneApercu.getChildren().add(contenu);
        
        // Pour les images, essayer d'afficher l'aperçu réel
        if (extension.matches("jpg|jpeg|png|gif")) {
            afficherApercuImage(doc);
        }
    }
    
    /**
     * Affiche l'aperçu d'une image
     */
    private void afficherApercuImage(Document doc) {
        try {
            File fichier = new File(doc.getCheminFichier());
            if (fichier.exists()) {
                Image image = new Image(new FileInputStream(fichier), 200, 200, true, true);
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(200);
                imageView.setPreserveRatio(true);
                
                zoneApercu.getChildren().clear();
                
                Label lblNom = new Label(doc.getTitre());
                lblNom.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
                lblNom.setWrapText(true);
                
                VBox contenu = new VBox(10, imageView, lblNom);
                contenu.setStyle("-fx-alignment: center; -fx-padding: 10;");
                
                zoneApercu.getChildren().add(contenu);
            }
        } catch (Exception e) {
            System.err.println("Impossible d'afficher l'aperçu image: " + e.getMessage());
        }
    }
    
    /**
     * Cache les détails du document
     */
    private void cacherDetailsDocument() {
        this.documentSelectionne = null;
        
        if (zoneApercu != null) {
            zoneApercu.getChildren().clear();
            Label lblMessage = new Label("Sélectionnez un document\npour voir l'aperçu");
            lblMessage.setStyle("-fx-font-size: 14px; -fx-text-fill: gray; -fx-text-alignment: center;");
            Label lblIcone = new Label("📄");
            lblIcone.setStyle("-fx-font-size: 48px;");
            VBox contenu = new VBox(10, lblIcone, lblMessage);
            contenu.setStyle("-fx-alignment: center; -fx-padding: 40;");
            zoneApercu.getChildren().add(contenu);
        }
        
        activerBoutonsActions(false);
    }
    
    /**
     * Active/désactive les boutons d'actions
     */
    private void activerBoutonsActions(boolean actif) {
        if (btnModifierDocument != null) btnModifierDocument.setDisable(!actif);
        if (btnPartagerDocument != null) btnPartagerDocument.setDisable(!actif);
        if (btnDeplacerDocument != null) btnDeplacerDocument.setDisable(!actif);
        if (btnSupprimerDocument != null) btnSupprimerDocument.setDisable(!actif);
        if (btnApercuComplet != null) btnApercuComplet.setDisable(!actif);
        if (btnTelecharger != null) btnTelecharger.setDisable(!actif);
    }
    
    /**
     * NOUVELLE MÉTHODE : Modifier le document
     */
    @FXML
    private void handleModifierDocument() {
        if (documentSelectionne == null) {
            showAlert("Attention", "Aucun document sélectionné");
            return;
        }
        
        Dialog<Document> dialog = new Dialog<>();
        dialog.setTitle("Modifier le document");
        dialog.setHeaderText(documentSelectionne.getTitre());
        
        ButtonType btnEnregistrer = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(btnEnregistrer, ButtonType.CANCEL);
        
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        TextField txtTitre = new TextField(documentSelectionne.getTitre());
        TextArea txtDescription = new TextArea(documentSelectionne.getDescription());
        txtDescription.setPrefRowCount(3);
        
        grid.add(new Label("Titre:"), 0, 0);
        grid.add(txtTitre, 1, 0);
        grid.add(new Label("Description:"), 0, 1);
        grid.add(txtDescription, 1, 1);
        
        dialog.getDialogPane().setContent(grid);
        
        dialog.setResultConverter(btn -> {
            if (btn == btnEnregistrer) {
                documentSelectionne.setTitre(txtTitre.getText());
                documentSelectionne.setDescription(txtDescription.getText());
                return documentSelectionne;
            }
            return null;
        });
        
        Optional<Document> result = dialog.showAndWait();
        if (result.isPresent()) {
            showSuccess("Document modifié avec succès");
            afficherDetailsDocument(documentSelectionne);
            if (dossierActuel != null) {
                selectionnerDossier(dossierActuel);
            }
        }
    }
    
    /**
     * NOUVELLE MÉTHODE : Déplacer le document
     */
    @FXML
    private void handleDeplacerDocument() {
        if (documentSelectionne == null) {
            showAlert("Attention", "Aucun document sélectionné");
            return;
        }
        
        ChoiceDialog<Dossier> dialog = new ChoiceDialog<>();
        dialog.setTitle("Déplacer le document");
        dialog.setHeaderText("Déplacer: " + documentSelectionne.getTitre());
        dialog.setContentText("Choisir le dossier de destination:");
        
        List<Dossier> dossiers = dossierService.getAllDossiers();
        dialog.getItems().addAll(dossiers);
        
        Optional<Dossier> result = dialog.showAndWait();
        if (result.isPresent()) {
            Dossier destination = result.get();
            showSuccess("Document déplacé vers: " + destination.getNomDossier());
            if (dossierActuel != null) {
                selectionnerDossier(dossierActuel);
            }
        }
    }
    
    /**
     * NOUVELLE MÉTHODE : Partager le document
     */
    @FXML
    private void handlePartagerDocument() {
        if (documentSelectionne == null) {
            showAlert("Attention", "Aucun document sélectionné");
            return;
        }
        
        showInfo("Fonctionnalité de partage à venir...\nDocument: " + documentSelectionne.getTitre());
    }
    
    /**
     * NOUVELLE MÉTHODE : Supprimer le document (depuis le panneau)
     */
    @FXML
    private void handleSupprimerDocument() {
        if (documentSelectionne == null) {
            showAlert("Attention", "Aucun document sélectionné");
            return;
        }
        
        supprimerDocument(documentSelectionne);
    }
    
    /**
     * NOUVELLE MÉTHODE : Aperçu complet
     */
    @FXML
    private void handleApercuComplet() {
        if (documentSelectionne == null) {
            showAlert("Attention", "Aucun document sélectionné");
            return;
        }
        
        ouvrirDocument(documentSelectionne);
    }
    
    private void configurerColonnesTableau() {
        if (tableauDocuments == null) return;
        
        System.out.println("📋 Configuration des colonnes...");
        
        if (colonneNomDocument != null) {
            colonneNomDocument.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getTitre() != null ? cellData.getValue().getTitre() : "Sans titre"));
        }
        
        if (colonneTypeDocument != null) {
            colonneTypeDocument.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getExtension() != null ? cellData.getValue().getExtension().toUpperCase() : "???"));
        }
        
        if (colonneTailleDocument != null) {
            colonneTailleDocument.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getTailleFormatee()));
        }
        
        if (colonneAuteurDocument != null) {
            colonneAuteurDocument.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getCreateurNom() != null ? cellData.getValue().getCreateurNom() : "-"));
        }
        
        if (colonneDateModification != null) {
            colonneDateModification.setCellValueFactory(cellData -> {
                Document doc = cellData.getValue();
                if (doc.getDateModification() != null) {
                    return new SimpleStringProperty(doc.getDateModification().format(DATE_FORMATTER));
                } else if (doc.getDateCreation() != null) {
                    return new SimpleStringProperty(doc.getDateCreation().format(DATE_FORMATTER));
                }
                return new SimpleStringProperty("-");
            });
        }
        
        if (colonneStatutDocument != null) {
            colonneStatutDocument.setCellValueFactory(cellData -> {
                String statut = cellData.getValue().getStatut() != null ? cellData.getValue().getStatut() : "actif";
                String statutFormate = switch (statut.toLowerCase()) {
                    case "brouillon" -> "📝 Brouillon";
                    case "en_revision" -> "🔄 En révision";
                    case "valide" -> "✅ Validé";
                    case "archive" -> "📦 Archivé";
                    default -> "📄 " + statut;
                };
                return new SimpleStringProperty(statutFormate);
            });
        }
        
        if (colonneActionsDocument != null) {
            colonneActionsDocument.setCellFactory(column -> new TableCell<Document, String>() {
                private final Button btnVoir = new Button("👁");
                private final Button btnTelecharger = new Button("💾");
                private final Button btnSupprimer = new Button("🗑");
                
                {
                    btnVoir.setOnAction(event -> ouvrirDocument(getTableView().getItems().get(getIndex())));
                    btnTelecharger.setOnAction(event -> telechargerDocument(getTableView().getItems().get(getIndex())));
                    btnSupprimer.setOnAction(event -> supprimerDocument(getTableView().getItems().get(getIndex())));
                    btnVoir.setStyle("-fx-font-size: 10px; -fx-padding: 2 5 2 5;");
                    btnTelecharger.setStyle("-fx-font-size: 10px; -fx-padding: 2 5 2 5;");
                    btnSupprimer.setStyle("-fx-font-size: 10px; -fx-padding: 2 5 2 5;");
                }
                
                @Override
                protected void updateItem(String item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        HBox buttons = new HBox(5, btnVoir, btnTelecharger, btnSupprimer);
                        setGraphic(buttons);
                    }
                }
            });
        }
        
        System.out.println("✓ Colonnes configurées");
    }
    
    private void ouvrirDocument(Document doc) {
        if (doc == null) return;
        File fichier = new File(doc.getCheminFichier());
        try {
            if (Desktop.isDesktopSupported()) {
                Desktop.getDesktop().open(fichier);
            }
        } catch (Exception e) {
            showError("Erreur: " + e.getMessage());
        }
    }
    
    private void telechargerDocument(Document doc) {
        showInfo("Téléchargement de: " + doc.getTitre());
    }
    
    private void supprimerDocument(Document doc) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return;
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setContentText("Supprimer le document:\n" + doc.getTitre() + " ?");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            if (documentService.deleteDocument(doc.getId(), currentUser.getId())) {
                showSuccess("Document supprimé");
                if (dossierActuel != null) {
                    selectionnerDossier(dossierActuel);
                } else {
                    chargerDocuments();
                }
            }
        }
    }
    
    private void chargerArborescence() {
        if (arborescenceDossiers == null) return;
        
        try {
            List<Dossier> dossiers = dossierService.getAllDossiers();
            Dossier racine = new Dossier();
            racine.setId(0);
            racine.setNomDossier("📁 Racine");
            
            TreeItem<Dossier> rootItem = new TreeItem<>(racine);
            rootItem.setExpanded(true);
            
            construireArborescenceRecursive(rootItem, null, dossiers);
            arborescenceDossiers.setRoot(rootItem);
            
            arborescenceDossiers.setCellFactory(tv -> new TreeCell<Dossier>() {
                @Override
                protected void updateItem(Dossier item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        setText((item.getIcone() != null ? item.getIcone() : "📁") + " " + item.getNomDossier());
                    }
                }
            });
            
            arborescenceDossiers.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, newVal) -> {
                    if (newVal != null && newVal.getValue() != null) {
                        selectionnerDossier(newVal.getValue());
                    }
                }
            );
        } catch (Exception e) {
            System.err.println("Erreur arborescence: " + e.getMessage());
        }
    }
    
    private void construireArborescenceRecursive(TreeItem<Dossier> parent, Integer parentId, List<Dossier> dossiers) {
        for (Dossier d : dossiers) {
            boolean estEnfant = (parentId == null && d.getDossierParentId() == null) ||
                               (parentId != null && d.getDossierParentId() != null && d.getDossierParentId().equals(parentId));
            if (estEnfant) {
                TreeItem<Dossier> item = new TreeItem<>(d);
                parent.getChildren().add(item);
                construireArborescenceRecursive(item, d.getId(), dossiers);
            }
        }
    }
    
    private void selectionnerDossier(Dossier dossier) {
        this.dossierActuel = dossier;
        if (labelDossierActuel != null) {
            labelDossierActuel.setText((dossier.getIcone() != null ? dossier.getIcone() : "📁") + " " + dossier.getNomDossier());
        }
        
        if (dossier.getId() > 0) {
            List<Document> docs = documentService.getDocumentsByDossier(dossier.getId());
            if (tableauDocuments != null) {
                tableauDocuments.setItems(FXCollections.observableArrayList(docs));
            }
            if (labelNombreDocuments != null) {
                labelNombreDocuments.setText("(" + docs.size() + " documents)");
            }
        }
    }
    
    @FXML public void handleImporterDocument() { /* ... */ }
    @FXML public void handleNouveauDossier() { /* ... */ }
    @FXML public void handleRecherche() { /* ... */ }
    @FXML public void handleActualiser() { chargerArborescence(); chargerDocuments(); }
    @FXML public void handleToutSelectionner() { if (tableauDocuments != null) tableauDocuments.getSelectionModel().selectAll(); }
    @FXML public void handleTelechargerSelection() { /* ... */ }
    @FXML public void handleDeplacerSelection() { /* ... */ }
    @FXML public void handleSupprimerSelection() { /* ... */ }
    @FXML public void handleAffichageListe() { /* ... */ }
    @FXML public void handleAffichageGrille() { /* ... */ }
    @FXML public void handleAffichageDetails() { /* ... */ }
    @FXML public void handleRafraichirArbre() { chargerArborescence(); }
    @FXML public void handleCreerSousDossier() { /* ... */ }
    @FXML public void handleRaccourciFavoris() { /* ... */ }
    @FXML public void handleRaccourciRecents() { /* ... */ }
    @FXML public void handleRaccourciCorbeille() { /* ... */ }
    @FXML public void handleRaccourciPartages() { /* ... */ }
    @FXML public void handlePagePrecedente() { /* ... */ }
    @FXML public void handlePageSuivante() { /* ... */ }
    @FXML public void handleElementsParPageChange() { /* ... */ }
    @FXML public void handleNouvelleVersion() { /* ... */ }
    
    private void chargerDocuments() {
        try {
            if (tableauDocuments != null) {
                tableauDocuments.setItems(FXCollections.observableArrayList(documentService.getAllDocuments()));
            }
        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
        }
    }
    
    private User getCurrentUser() { return SessionManager.getInstance().getCurrentUser(); }
    private void showAlert(String t, String m) { new Alert(Alert.AlertType.WARNING, m).showAndWait(); }
    private void showError(String m) { new Alert(Alert.AlertType.ERROR, m).showAndWait(); }
    private void showSuccess(String m) { new Alert(Alert.AlertType.INFORMATION, m).showAndWait(); }
    private void showInfo(String m) { new Alert(Alert.AlertType.INFORMATION, m).showAndWait(); }
}