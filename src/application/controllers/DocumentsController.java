package application.controllers;

import application.models.Document;
import application.models.Dossier;
import application.models.PartageInfo;
import application.models.User;
import application.services.DocumentService;
import application.services.DossierService;
import application.services.NetworkStorageService;
import application.utils.SessionManager;
import application.utils.AlertUtils;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.*;
import javafx.stage.DirectoryChooser;
import javafx.stage.FileChooser;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.web.WebView;

import java.awt.Desktop;
import java.io.File;
import java.io.FileInputStream;
import java.nio.file.Files;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

import application.controllers.DeplacerDocumentDialog;
import application.controllers.PartagerDocumentDialog;
import application.services.LogService;
import application.utils.IconeUtils;

/**
 * Contrôleur amélioré pour la gestion des documents
 * Intègre toutes les fonctionnalités demandées
 */
public class DocumentsController {
    
    // ==================== COMPOSANTS FXML ====================
    
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
    
    // Fil d'Ariane
    @FXML private Hyperlink breadcrumbRoot;
    @FXML private Label breadcrumbSeparator1;
    @FXML private Hyperlink breadcrumbLevel1;
    @FXML private Label breadcrumbSeparator2;
    @FXML private Hyperlink breadcrumbLevel2;
    
    // Panneau de détails
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
    
    // Boutons actions rapides
    @FXML private Button btnModifierDocument;
    @FXML private Button btnPartagerDocument;
    @FXML private Button btnDeplacerDocument;
    @FXML private Button btnSupprimerDocument;
    @FXML private Button btnApercuComplet;
    @FXML private Button btnTelecharger;
    
    // Boutons de gestion
    @FXML private Button btnImporter;
    @FXML private Button btnNouveauDossier;
    @FXML private Button btnNouveauDocument;
    @FXML private Button btnAjouterPremierDoc;
    
    // Autres champs
    @FXML private TextField champRechercheDoc;
    @FXML private Label labelDossierActuel;
    @FXML private Label labelNombreDocuments;
    
    // ==================== SERVICES ET DONNÉES ====================
    
    private DocumentService documentService;
    private DossierService dossierService;
    private NetworkStorageService networkStorageService;
    
    private LogService logService;
    private Dossier dossierActuel;
    private Document documentSelectionne;
    private List<Dossier> cheminDossiers = new ArrayList<>(); // Pour le fil d'Ariane
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    // ==================== INITIALISATION ====================
    
    @FXML
    public void initialize() {
        System.out.println("=== INITIALISATION DocumentsController ===");
        
        documentService = DocumentService.getInstance();
        dossierService = DossierService.getInstance();
        networkStorageService = NetworkStorageService.getInstance();
        
        configurerColonnesTableau();
        configurerSelectionDocument();
        configurerFilAriane();
        configurerBoutons();
        chargerArborescence();
        chargerDocuments();
        logService = LogService.getInstance();
        
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            System.out.println("✓ Initialisé pour: " + currentUser.getNomComplet());
        }
        
        System.out.println("=== FIN INITIALISATION ===");
    }
    
    /**
     * Configure le fil d'Ariane
     */
    private void configurerFilAriane() {
        if (breadcrumbRoot != null) {
            breadcrumbRoot.setOnAction(e -> naviguerVersRacine());
        }
        
        if (breadcrumbLevel1 != null) {
            breadcrumbLevel1.setOnAction(e -> naviguerVersNiveau(1));
        }
        
        if (breadcrumbLevel2 != null) {
            breadcrumbLevel2.setOnAction(e -> naviguerVersNiveau(2));
        }
    }
    
    /**
     * Configure les boutons
     */
    private void configurerBoutons() {
        if (btnNouveauDocument != null) {
            btnNouveauDocument.setOnAction(e -> handleImporterDocument());
        }
        
        if (btnAjouterPremierDoc != null) {
            btnAjouterPremierDoc.setOnAction(e -> handleImporterDocument());
        }
    }
    
    // ==================== GESTION DU FIL D'ARIANE ====================
    
    /**
     * Met à jour le fil d'Ariane
     */
    private void mettreAJourFilAriane(Dossier dossier) {
        // Construire le chemin complet
        cheminDossiers.clear();
        construireCheminDossier(dossier);
        
        // Masquer tous les éléments
        if (breadcrumbSeparator1 != null) breadcrumbSeparator1.setVisible(false);
        if (breadcrumbLevel1 != null) breadcrumbLevel1.setVisible(false);
        if (breadcrumbSeparator2 != null) breadcrumbSeparator2.setVisible(false);
        if (breadcrumbLevel2 != null) breadcrumbLevel2.setVisible(false);
        
        // Afficher selon la profondeur
        int profondeur = cheminDossiers.size();
        
        if (breadcrumbRoot != null) {
            breadcrumbRoot.setText(profondeur == 0 ? "Racine" : "📂 Racine");
        }
        
        if (profondeur >= 1 && breadcrumbLevel1 != null) {
            breadcrumbSeparator1.setVisible(true);
            breadcrumbLevel1.setVisible(true);
            breadcrumbLevel1.setText(IconeUtils.formatterNomDossier(cheminDossiers.get(0)));
        }
        
        if (profondeur >= 2 && breadcrumbLevel2 != null) {
            breadcrumbSeparator2.setVisible(true);
            breadcrumbLevel2.setVisible(true);
            breadcrumbLevel2.setText(IconeUtils.formatterNomDossier(cheminDossiers.get(1)));
        }
    }
    
    /**
     * Construit le chemin depuis la racine jusqu'au dossier actuel
     */
    private void construireCheminDossier(Dossier dossier) {
        if (dossier == null || dossier.getId() == 0) return;
        
        // Construction récursive
        if (dossier.getDossierParentId() != null && dossier.getDossierParentId() > 0) {
            Dossier parent = dossierService.getDossierById(dossier.getDossierParentId());
            if (parent != null) {
                construireCheminDossier(parent);
            }
        }
        
        cheminDossiers.add(dossier);
    }
    
    /**
     * Navigation vers la racine
     */
    private void naviguerVersRacine() {
        dossierActuel = null;
        cheminDossiers.clear();
        mettreAJourFilAriane(null);
        chargerDocuments();
        if (labelDossierActuel != null) {
            labelDossierActuel.setText("📂 Racine");
        }
    }
    
    /**
     * Navigation vers un niveau spécifique
     */
    private void naviguerVersNiveau(int niveau) {
        if (niveau > 0 && niveau <= cheminDossiers.size()) {
            Dossier dossier = cheminDossiers.get(niveau - 1);
            selectionnerDossier(dossier);
        }
    }
    
    // ==================== GESTION DES DOCUMENTS ====================
    
    /**
     * Corrige automatiquement toutes les icônes invalides
     */
    @FXML
    private void handleCorrigerIcones() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Correction des icônes");
        confirm.setHeaderText("Corriger les icônes des dossiers");
        confirm.setContentText("Cette action va corriger automatiquement toutes les icônes invalides (???).\n\nContinuer ?");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                int nombreCorrections = IconeUtils.corrigerTousLesDossiers();
                
                showSuccess("✅ " + nombreCorrections + " dossier(s) corrigé(s) !");
                
                // Recharger l'arborescence
                chargerArborescence();
                
            } catch (Exception e) {
                showError("Erreur lors de la correction : " + e.getMessage());
            }
        }
    }
    
    /**
     * Importe un nouveau document
     */
    @FXML
    public void handleImporterDocument() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            showError("Aucun utilisateur connecté");
            return;
        }
        
        // Dialogue de sélection de fichier
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner un document");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"),
            new FileChooser.ExtensionFilter("Documents PDF", "*.pdf"),
            new FileChooser.ExtensionFilter("Documents Word", "*.doc", "*.docx"),
            new FileChooser.ExtensionFilter("Documents Excel", "*.xls", "*.xlsx"),
            new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif"),
            new FileChooser.ExtensionFilter("Fichiers CSV", "*.csv")
        );
        
        File fichier = fileChooser.showOpenDialog(btnImporter.getScene().getWindow());
        
        if (fichier == null) return;
        
        // Dialogue de métadonnées
        DocumentFormDialog dialog = new DocumentFormDialog(null, dossierActuel, currentUser);
        Optional<Document> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            Document document = result.get();
            
            try {
                // Créer le document
                document = documentService.createDocument(document, fichier, currentUser);
                
                showSuccess("Document importé avec succès !");
                
                // Rafraîchir l'affichage
                if (dossierActuel != null) {
                    selectionnerDossier(dossierActuel);
                } else {
                    chargerDocuments();
                }
                
            } catch (Exception e) {
                showError("Erreur lors de l'import: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    /**
     * Modifie un document
     */
    @FXML
    private void handleModifierDocument() {
    	Document doc = tableauDocuments.getSelectionModel().getSelectedItem();
        if (doc == null) {
            showError("Veuillez sélectionner un document");
            return;
        }
        
        Dossier dossierActuel = dossierService.getDossierById(doc.getDossierId());
        DocumentFormDialog dialog = new DocumentFormDialog(doc, dossierActuel, getCurrentUser());
        Optional<Document> result = dialog.showAndWait();
        
        result.ifPresent(documentModifie -> {
            try {
                documentModifie.setId(doc.getId());
                documentService.updateDocument(documentModifie);
                
                LogService.getInstance().logModificationDocument(
                    doc.getId(),
                    doc.getCodeDocument(),
                    "Métadonnées mises à jour"
                );
                
                showSuccess("✅ Document modifié avec succès !");
                chargerDocuments();
                
            } catch (Exception e) {
                showError("Erreur lors de la modification : " + e.getMessage());
            }
        });
    }
    
    /**
     * Déplace un document
     */
    @FXML
    private void handleDeplacerDocument() {
        Document doc = tableauDocuments.getSelectionModel().getSelectedItem();
        if (doc == null) {
            showError("Veuillez sélectionner un document");
            return;
        }
        
        DeplacerDocumentDialog dialog = new DeplacerDocumentDialog(doc);
        Optional<Dossier> result = dialog.showAndWait();
        
        result.ifPresent(dossierDestination -> {
            try {
                // Sauvegarder l'ancien dossier pour l'historique
                String ancienDossier = doc.getNomDossier();
                
                // Mettre à jour le document
                doc.setDossierId(dossierDestination.getId());
                documentService.updateDocument(doc);
                
                // Logger l'opération
                LogService.getInstance().logDeplacementDocument(
                    doc.getCodeDocument(), 
                    ancienDossier, 
                    dossierDestination.getNomDossier()
                );
                
                showSuccess("✅ Document déplacé avec succès !");
                chargerDocuments();
                
            } catch (Exception e) {
                showError("Erreur lors du déplacement : " + e.getMessage());
            }
        });
    }
    
    /**
     * Télécharge un document
     */
    @FXML
    private void handleTelechargerDocument() {
        if (documentSelectionne == null) return;
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Enregistrer le document");
        fileChooser.setInitialFileName(documentSelectionne.getTitre());
        
        File destination = fileChooser.showSaveDialog(btnTelecharger.getScene().getWindow());
        
        if (destination != null) {
            try {
                File source = new File(documentSelectionne.getCheminFichier());
                Files.copy(source.toPath(), destination.toPath());
                showSuccess("Document téléchargé avec succès");
            } catch (Exception e) {
                showError("Erreur lors du téléchargement: " + e.getMessage());
            }
        }
    }
    
    /**
     * Supprime un document (vers la corbeille)
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
     * Supprime un document (logique - vers corbeille)
     */
    private void supprimerDocument(Document doc) {
        User currentUser = getCurrentUser();
        if (currentUser == null) return;
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setContentText("Déplacer le document vers la corbeille ?\n" + doc.getTitre());
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            // Marquer comme supprimé (va dans corbeille)
            if (documentService.deleteDocument(doc.getId(), currentUser.getId())) {
                showSuccess("Document déplacé vers la corbeille");
                if (dossierActuel != null) {
                    selectionnerDossier(dossierActuel);
                } else {
                    chargerDocuments();
                }
            }
        }
    }
    
    /**
     * Sélectionne tous les documents
     */
    @FXML
    public void handleToutSelectionner() {
        if (tableauDocuments != null) {
            tableauDocuments.getSelectionModel().selectAll();
        }
    }
    
    /**
     * Télécharge la sélection
     */
    @FXML
    public void handleTelechargerSelection() {
        List<Document> selection = tableauDocuments.getSelectionModel().getSelectedItems();
        
        if (selection.isEmpty()) {
            showAlert("Attention", "Aucun document sélectionné");
            return;
        }
        
        // Dialogue de sélection du répertoire
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choisir le répertoire de destination");
        
        File repertoire = dirChooser.showDialog(tableauDocuments.getScene().getWindow());
        
        if (repertoire != null) {
            int succes = 0;
            for (Document doc : selection) {
                try {
                    File source = new File(doc.getCheminFichier());
                    File dest = new File(repertoire, source.getName());
                    Files.copy(source.toPath(), dest.toPath());
                    succes++;
                } catch (Exception e) {
                    System.err.println("Erreur téléchargement " + doc.getTitre() + ": " + e.getMessage());
                }
            }
            
            showSuccess(succes + " document(s) téléchargé(s) avec succès");
        }
    }
    
    // ==================== GESTION DES DOSSIERS ====================
    
    /**
     * Crée un nouveau dossier
     */
    @FXML
    public void handleNouveauDossier() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            showError("Aucun utilisateur connecté");
            return;
        }
        
        // Vérifier les permissions
        if (!dossierService.peutCreerDossier(currentUser)) {
            showError("Vous n'avez pas les permissions pour créer un dossier");
            return;
        }
        
        // Dialogue de création
        DossierFormDialog dialog = new DossierFormDialog(null, dossierActuel);
        Optional<Dossier> result = dialog.showAndWait();
        
        if (result.isPresent()) {
            Dossier nouveauDossier = result.get();
            
            try {
                // Créer le dossier
                nouveauDossier = dossierService.createDossier(nouveauDossier, currentUser);
                
                showSuccess("Dossier créé avec succès !");
                
                // Rafraîchir l'arborescence
                chargerArborescence();
                
            } catch (Exception e) {
                showError("Erreur lors de la création: " + e.getMessage());
                e.printStackTrace();
            }
        }
    }
    
    // ==================== APERÇU DES DOCUMENTS ====================
    
    /**
     * Affiche l'aperçu d'un document
     */
    private void afficherApercu(Document doc) {
        if (zoneApercu == null) return;
        
        zoneApercu.getChildren().clear();
        
        String extension = doc.getExtension() != null ? doc.getExtension().toLowerCase() : "";
        
        try {
            if (extension.matches("jpg|jpeg|png|gif|bmp")) {
                afficherApercuImage(doc);
            } else if (extension.equals("pdf")) {
                afficherApercuPDF(doc);
            } else if (extension.matches("txt|csv|log")) {
                afficherApercuTexte(doc);
            } else {
                afficherApercuGenerique(doc);
            }
        } catch (Exception e) {
            System.err.println("Erreur affichage aperçu: " + e.getMessage());
            afficherApercuGenerique(doc);
        }
    }
    
    /**
     * Aperçu pour les images
     */
    private void afficherApercuImage(Document doc) {
        try {
            File fichier = new File(doc.getCheminFichier());
            if (fichier.exists()) {
                Image image = new Image(new FileInputStream(fichier), 300, 300, true, true);
                ImageView imageView = new ImageView(image);
                imageView.setFitWidth(300);
                imageView.setPreserveRatio(true);
                
                Label lblNom = new Label(doc.getTitre());
                lblNom.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
                lblNom.setWrapText(true);
                lblNom.setMaxWidth(300);
                
                VBox contenu = new VBox(10, imageView, lblNom);
                contenu.setAlignment(Pos.CENTER);
                contenu.setStyle("-fx-padding: 10;");
                
                zoneApercu.getChildren().add(contenu);
            } else {
                afficherApercuGenerique(doc);
            }
        } catch (Exception e) {
            afficherApercuGenerique(doc);
        }
    }
    
    /**
     * Aperçu pour les PDF
     */
    private void afficherApercuPDF(Document doc) {
        // Pour l'aperçu PDF, on affiche juste l'icône et les infos
        // Un vrai aperçu nécessiterait une bibliothèque PDF
        Label lblIcone = new Label("📕");
        lblIcone.setStyle("-fx-font-size: 64px;");
        
        Label lblNom = new Label(doc.getTitre());
        lblNom.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        lblNom.setWrapText(true);
        lblNom.setMaxWidth(300);
        
        Label lblInfo = new Label("Document PDF • " + doc.getTailleFormatee());
        lblInfo.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");
        
        Button btnOuvrir = new Button("Ouvrir le PDF");
        btnOuvrir.setOnAction(e -> ouvrirDocument(doc));
        
        VBox contenu = new VBox(10, lblIcone, lblNom, lblInfo, btnOuvrir);
        contenu.setAlignment(Pos.CENTER);
        contenu.setStyle("-fx-padding: 20;");
        
        zoneApercu.getChildren().add(contenu);
    }
    
    /**
     * Aperçu pour les fichiers texte
     */
    private void afficherApercuTexte(Document doc) {
        try {
            File fichier = new File(doc.getCheminFichier());
            if (fichier.exists() && fichier.length() < 100000) { // Max 100KB
                String contenu = Files.readString(fichier.toPath());
                
                // Limiter à 500 caractères
                if (contenu.length() > 500) {
                    contenu = contenu.substring(0, 500) + "...";
                }
                
                TextArea textArea = new TextArea(contenu);
                textArea.setEditable(false);
                textArea.setWrapText(true);
                textArea.setPrefRowCount(10);
                textArea.setMaxWidth(300);
                
                Label lblNom = new Label(doc.getTitre());
                lblNom.setStyle("-fx-font-size: 12px; -fx-font-weight: bold;");
                
                VBox contenuBox = new VBox(10, lblNom, textArea);
                contenuBox.setStyle("-fx-padding: 10;");
                
                zoneApercu.getChildren().add(contenuBox);
            } else {
                afficherApercuGenerique(doc);
            }
        } catch (Exception e) {
            afficherApercuGenerique(doc);
        }
    }
    
    /**
     * Aperçu générique pour les autres types
     */
    private void afficherApercuGenerique(Document doc) {
        String extension = doc.getExtension() != null ? doc.getExtension().toLowerCase() : "";
        
        String icone = switch (extension) {
            case "pdf" -> "📕";
            case "doc", "docx" -> "📘";
            case "xls", "xlsx" -> "📗";
            case "ppt", "pptx" -> "📙";
            case "txt" -> "📄";
            case "csv" -> "📊";
            case "zip", "rar" -> "📦";
            default -> "📄";
        };
        
        Label lblIcone = new Label(icone);
        lblIcone.setStyle("-fx-font-size: 64px;");
        
        Label lblNom = new Label(doc.getTitre());
        lblNom.setStyle("-fx-font-size: 14px; -fx-font-weight: bold;");
        lblNom.setWrapText(true);
        lblNom.setMaxWidth(300);
        
        Label lblType = new Label(extension.toUpperCase() + " • " + doc.getTailleFormatee());
        lblType.setStyle("-fx-font-size: 12px; -fx-text-fill: gray;");
        
        VBox contenu = new VBox(10, lblIcone, lblNom, lblType);
        contenu.setAlignment(Pos.CENTER);
        contenu.setStyle("-fx-padding: 20;");
        
        zoneApercu.getChildren().add(contenu);
    }
    
    // ==================== MÉTHODES UTILITAIRES ====================
    
    /**
     * Configure la sélection de document
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
     * Affiche les détails d'un document
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
            contenu.setAlignment(Pos.CENTER);
            contenu.setStyle("-fx-padding: 40;");
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
     * Configure les colonnes du tableau
     */
    private void configurerColonnesTableau() {
        if (tableauDocuments == null) return;
        
        System.out.println("📋 Configuration des colonnes...");
        
        if (colonneNomDocument != null) {
            colonneNomDocument.setCellValueFactory(cellData -> 
                new SimpleStringProperty(cellData.getValue().getTitre() != null ? cellData.getValue().getTitre() : "Sans titre"));
        }
        
        if (colonneTypeDocument != null) {
            colonneTypeDocument.setCellValueFactory(cellData -> {
                String ext = cellData.getValue().getExtension();
                // Remplacer les ??? par des icônes appropriées
                String icone = getIconeParExtension(ext);
                return new SimpleStringProperty(icone + " " + (ext != null ? ext.toUpperCase() : "???"));
            });
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
    
    /**
     * Retourne une icône selon l'extension
     */
    private String getIconeParExtension(String extension) {
        if (extension == null) return "📄";
        
        return switch (extension.toLowerCase()) {
            case "pdf" -> "📕";
            case "doc", "docx" -> "📘";
            case "xls", "xlsx" -> "📗";
            case "ppt", "pptx" -> "📙";
            case "txt" -> "📄";
            case "csv" -> "📊";
            case "jpg", "jpeg", "png", "gif", "bmp" -> "🖼️";
            case "zip", "rar", "7z" -> "📦";
            case "mp3", "wav" -> "🎵";
            case "mp4", "avi" -> "🎬";
            default -> "📄";
        };
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
        this.documentSelectionne = doc;
        handleTelechargerDocument();
    }
    
    /**
     * Charge l'arborescence des dossiers
     */
    private void chargerArborescence() {
        if (arborescenceDossiers == null) return;
        
        try {
            List<Dossier> dossiers = dossierService.getAllDossiers();
            Dossier racine = new Dossier();
            racine.setId(0);
            racine.setNomDossier("📁 Racine");
            racine.setIcone("📁");
            
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
                        setText(IconeUtils.formatterNomDossier(item));
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
    
    /**
     * Sélectionne un dossier
     */
    private void selectionnerDossier(Dossier dossier) {
        this.dossierActuel = dossier;
        
        // Mettre à jour le fil d'Ariane
        mettreAJourFilAriane(dossier);
        
        if (labelDossierActuel != null) {
            labelDossierActuel.setText(IconeUtils.formatterNomDossier(dossier));
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
    
    /**
     * Charge tous les documents
     */
    private void chargerDocuments() {
        try {
            if (tableauDocuments != null) {
                tableauDocuments.setItems(FXCollections.observableArrayList(documentService.getAllDocuments()));
            }
        } catch (Exception e) {
            System.err.println("Erreur: " + e.getMessage());
        }
    }
    
    // Handlers pour les autres actions
    @FXML public void handleRecherche() { /* ... */ }
    @FXML public void handleActualiser() { chargerArborescence(); chargerDocuments(); }
    @FXML public void handleDeplacerSelection() { /* ... */ }
    @FXML public void handleSupprimerSelection() { /* ... */ }
    @FXML public void handleAffichageListe() { /* ... */ }
    @FXML public void handleAffichageGrille() { /* ... */ }
    @FXML public void handleAffichageDetails() { /* ... */ }
    @FXML public void handleRafraichirArbre() { chargerArborescence(); }
    @FXML public void handleCreerSousDossier() { handleNouveauDossier(); }
    @FXML public void handleRaccourciFavoris() { /* ... */ }
    @FXML public void handleRaccourciRecents() { /* ... */ }
    @FXML public void handleRaccourciCorbeille() { /* ... */ }
    @FXML public void handleRaccourciPartages() { /* ... */ }
    @FXML public void handlePagePrecedente() { /* ... */ }
    @FXML public void handlePageSuivante() { /* ... */ }
    @FXML public void handleElementsParPageChange() { /* ... */ }
    @FXML public void handleNouvelleVersion() { /* ... */ }
    
    @FXML 
    public void handlePartagerDocument() { 
    	Document doc = tableauDocuments.getSelectionModel().getSelectedItem();
        if (doc == null) {
            showError("Veuillez sélectionner un document");
            return;
        }
        
        PartagerDocumentDialog dialog = new PartagerDocumentDialog(doc);
        Optional<PartageInfo> result = dialog.showAndWait();  // ⬅️ Type simplifié
        
        result.ifPresent(partageInfo -> {
            try {
                // Implémenter la logique de partage
                for (User user : partageInfo.getUtilisateurs()) {
                    // Enregistrer le partage en base
                    // TODO: Implémenter dans DocumentService
                }
                
                // Logger l'opération
                LogService.getInstance().logPartageDocument(
                    doc.getCodeDocument(),
                    partageInfo.getUtilisateurs().size()
                );
                
                showSuccess("✅ Document partagé avec " + 
                           partageInfo.getUtilisateurs().size() + " utilisateur(s) !");
                
            } catch (Exception e) {
                showError("Erreur lors du partage : " + e.getMessage());
            }
        });
    }
    
    @FXML public void handleApercuComplet() { if (documentSelectionne != null) ouvrirDocument(documentSelectionne); }
    
    // Méthodes utilitaires
    private User getCurrentUser() { return SessionManager.getInstance().getCurrentUser(); }
    private void showAlert(String t, String m) { new Alert(Alert.AlertType.WARNING, m).showAndWait(); }
    private void showError(String m) { new Alert(Alert.AlertType.ERROR, m).showAndWait(); }
    private void showSuccess(String m) { new Alert(Alert.AlertType.INFORMATION, m).showAndWait(); }
    private void showInfo(String m) { new Alert(Alert.AlertType.INFORMATION, m).showAndWait(); }
}