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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ArrayList;

import application.controllers.DeplacerDocumentDialog;
import application.controllers.PartagerDocumentDialog;
import application.controllers.DossierFormDialog;
import application.services.LogService;
import application.utils.IconeUtils;

/**
 * Contrôleur AMÉLIORÉ pour la gestion des documents
 * NOUVELLES FONCTIONNALITÉS:
 * - Recherche de documents
 * - Barre de recherche de dossiers  
 * - Modification du nom d'un dossier via menu contextuel
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
    
    // NOUVEAU: Barre de recherche de dossiers
    @FXML private TextField champRechercheDossier;
    
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
    
    // Champ de recherche de documents
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
    private List<Dossier> cheminDossiers = new ArrayList<>();
    private List<Dossier> tousLesDossiers = new ArrayList<>(); // NOUVEAU: pour la recherche
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    // ==================== INITIALISATION ====================
    
    @FXML
    public void initialize() {
        System.out.println("=== INITIALISATION DocumentsController AMÉLIORÉ ===");
        
        documentService = DocumentService.getInstance();
        dossierService = DossierService.getInstance();
        networkStorageService = NetworkStorageService.getInstance();
        logService = LogService.getInstance();
        
        configurerColonnesTableau();
        configurerSelectionDocument();
        configurerFilAriane();
        configurerBoutons();
        configurerRechercheDossier();     // NOUVEAU
        configurerRechercheDocument();    // NOUVEAU
        configurerMenuContextuelDossier(); // NOUVEAU
        chargerArborescence();
        chargerDocuments();
        
        User currentUser = getCurrentUser();
        if (currentUser != null) {
            System.out.println("✓ Initialisé pour: " + currentUser.getNomComplet());
        }
        
        System.out.println("=== FIN INITIALISATION ===");
    }
    
    // ==================== NOUVELLES FONCTIONNALITÉS ====================
    
    /**
     * Configure la barre de recherche de dossiers
     */
    private void configurerRechercheDossier() {
        if (champRechercheDossier == null) return;
        
        champRechercheDossier.setPromptText("🔍 Rechercher un dossier...");
        champRechercheDossier.textProperty().addListener((obs, oldVal, newVal) -> {
            rechercherDossiers(newVal);
        });
        
        System.out.println("✓ Recherche de dossiers configurée");
    }
    
    /**
     * Recherche de dossiers en temps réel
     */
    private void rechercherDossiers(String recherche) {
        if (recherche == null || recherche.trim().isEmpty()) {
            // Recharger l'arborescence complète
            chargerArborescence();
            return;
        }
        
        try {
            List<Dossier> resultats = dossierService.rechercherDossiers(recherche);
            
            // Construire une arborescence filtrée
            Dossier racine = new Dossier();
            racine.setId(0);
            racine.setNomDossier("📁 Résultats (" + resultats.size() + ")");
            racine.setIcone("🔍");
            
            TreeItem<Dossier> rootItem = new TreeItem<>(racine);
            rootItem.setExpanded(true);
            
            // Ajouter les résultats comme enfants directs
            for (Dossier d : resultats) {
                TreeItem<Dossier> item = new TreeItem<>(d);
                rootItem.getChildren().add(item);
            }
            
            arborescenceDossiers.setRoot(rootItem);
            
            System.out.println("🔍 Recherche dossiers: " + resultats.size() + " résultat(s)");
            
        } catch (Exception e) {
            System.err.println("Erreur recherche dossiers: " + e.getMessage());
        }
    }
    
    /**
     * Configure la barre de recherche de documents
     */
    private void configurerRechercheDocument() {
        if (champRechercheDoc == null) return;
        
        champRechercheDoc.setPromptText("🔍 Rechercher un document...");
        champRechercheDoc.textProperty().addListener((obs, oldVal, newVal) -> {
            rechercherDocuments(newVal);
        });
        
        System.out.println("✓ Recherche de documents configurée");
    }
    
    /**
     * Recherche de documents en temps réel
     */
    private void rechercherDocuments(String recherche) {
        if (recherche == null || recherche.trim().isEmpty()) {
            // Recharger les documents du dossier actuel ou tous
            if (dossierActuel != null && dossierActuel.getId() > 0) {
                List<Document> docs = documentService.getDocumentsByDossier(dossierActuel.getId());
                tableauDocuments.setItems(FXCollections.observableArrayList(docs));
            } else {
                chargerDocuments();
            }
            return;
        }
        
        try {
            List<Document> resultats = documentService.rechercherDocuments(recherche);
            tableauDocuments.setItems(FXCollections.observableArrayList(resultats));
            
            if (labelNombreDocuments != null) {
                labelNombreDocuments.setText("(" + resultats.size() + " résultat(s))");
            }
            
            System.out.println("🔍 Recherche documents: " + resultats.size() + " résultat(s)");
            
        } catch (Exception e) {
            System.err.println("Erreur recherche documents: " + e.getMessage());
        }
    }
    
    /**
     * Configure le menu contextuel pour l'arborescence des dossiers
     * Permet de modifier le nom d'un dossier
     */
    private void configurerMenuContextuelDossier() {
        if (arborescenceDossiers == null) return;
        
        ContextMenu menuContextuel = new ContextMenu();
        
        // Menu "Modifier le dossier"
        MenuItem itemModifier = new MenuItem("✏️ Modifier le dossier");
        itemModifier.setOnAction(e -> {
            TreeItem<Dossier> selectedItem = arborescenceDossiers.getSelectionModel().getSelectedItem();
            if (selectedItem != null && selectedItem.getValue() != null) {
                Dossier dossier = selectedItem.getValue();
                if (dossier.getId() > 0) { // Ne pas modifier la racine
                    handleModifierDossier(dossier);
                }
            }
        });
        
        // Menu "Créer un sous-dossier"
        MenuItem itemNouveauSous = new MenuItem("📁 Créer un sous-dossier");
        itemNouveauSous.setOnAction(e -> {
            TreeItem<Dossier> selectedItem = arborescenceDossiers.getSelectionModel().getSelectedItem();
            if (selectedItem != null && selectedItem.getValue() != null) {
                Dossier parent = selectedItem.getValue();
                if (parent.getId() > 0) {
                    handleNouveauSousDossier(parent);
                }
            }
        });
        
        // Menu "Supprimer le dossier"
        MenuItem itemSupprimer = new MenuItem("🗑️ Supprimer le dossier");
        itemSupprimer.setOnAction(e -> {
            TreeItem<Dossier> selectedItem = arborescenceDossiers.getSelectionModel().getSelectedItem();
            if (selectedItem != null && selectedItem.getValue() != null) {
                Dossier dossier = selectedItem.getValue();
                if (dossier.getId() > 0 && !dossier.isSysteme()) {
                    handleSupprimerDossier(dossier);
                }
            }
        });
        
        menuContextuel.getItems().addAll(itemModifier, itemNouveauSous, new SeparatorMenuItem(), itemSupprimer);
        
        // Afficher le menu contextuel sur clic droit
        arborescenceDossiers.setOnContextMenuRequested(event -> {
            TreeItem<Dossier> selectedItem = arborescenceDossiers.getSelectionModel().getSelectedItem();
            if (selectedItem != null && selectedItem.getValue() != null && selectedItem.getValue().getId() > 0) {
                Dossier dossier = selectedItem.getValue();
                
                // Désactiver "Supprimer" si c'est un dossier système
                itemSupprimer.setDisable(dossier.isSysteme());
                
                // Vérifier les permissions
                User currentUser = getCurrentUser();
                if (currentUser != null) {
                    itemModifier.setDisable(!dossierService.peutCreerDossier(currentUser));
                    itemNouveauSous.setDisable(!dossierService.peutCreerDossier(currentUser));
                    itemSupprimer.setDisable(!dossierService.peutSupprimerDossier(currentUser));
                }
                
                menuContextuel.show(arborescenceDossiers, event.getScreenX(), event.getScreenY());
            }
        });
        
        System.out.println("✓ Menu contextuel dossiers configuré");
    }
    
    /**
     * Ouvre le dialogue de modification d'un dossier
     */
    private void handleModifierDossier(Dossier dossier) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            showError("Utilisateur non connecté");
            return;
        }
        
        // Vérifier les permissions
        if (!dossierService.peutCreerDossier(currentUser)) {
            showError("Vous n'avez pas les permissions pour modifier ce dossier");
            return;
        }
        
        // Ouvrir le dialogue de modification
        DossierFormDialog dialog = new DossierFormDialog(dossier, null);
        Optional<Dossier> result = dialog.showAndWait();
        
        result.ifPresent(dossierModifie -> {
            try {
                // Mettre à jour le dossier
                if (dossierService.updateDossier(dossierModifie)) {
                    // Logger l'opération
                    logService.logModificationDossier(
                        dossierModifie.getCodeDossier(),
                        currentUser.getNomComplet()
                    );
                    
                    // Recharger l'arborescence
                    chargerArborescence();
                    
                    showSuccess("✅ Dossier modifié avec succès !");
                    
                } else {
                    showError("Erreur lors de la modification du dossier");
                }
                
            } catch (Exception e) {
                showError("Erreur : " + e.getMessage());
            }
        });
    }
    
    /**
     * Crée un nouveau sous-dossier
     */
    private void handleNouveauSousDossier(Dossier parent) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            showError("Utilisateur non connecté");
            return;
        }
        
        if (!dossierService.peutCreerDossier(currentUser)) {
            showError("Vous n'avez pas les permissions pour créer un dossier");
            return;
        }
        
        DossierFormDialog dialog = new DossierFormDialog(null, parent);
        Optional<Dossier> result = dialog.showAndWait();
        
        result.ifPresent(nouveauDossier -> {
            try {
                dossierService.createDossier(nouveauDossier, currentUser);
                
                logService.logCreationDossier(
                    nouveauDossier.getCodeDossier(),
                    currentUser.getNomComplet()
                );
                
                chargerArborescence();
                showSuccess("✅ Sous-dossier créé avec succès !");
                
            } catch (Exception e) {
                showError("Erreur : " + e.getMessage());
            }
        });
    }
    
    /**
     * Supprime un dossier
     */
    private void handleSupprimerDossier(Dossier dossier) {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            showError("Utilisateur non connecté");
            return;
        }
        
        // Confirmation
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer le dossier ?");
        confirmation.setContentText("Êtes-vous sûr de vouloir supprimer le dossier '" + 
                                   dossier.getNomDossier() + "' ?");
        
        Optional<ButtonType> resultat = confirmation.showAndWait();
        if (resultat.isPresent() && resultat.get() == ButtonType.OK) {
            try {
                if (dossierService.deleteDossier(dossier.getId(), currentUser)) {
                    logService.logSuppressionDossier(
                        dossier.getCodeDossier(),
                        currentUser.getNomComplet()
                    );
                    
                    chargerArborescence();
                    showSuccess("✅ Dossier supprimé avec succès !");
                } else {
                    showError("Erreur lors de la suppression");
                }
            } catch (Exception e) {
                showError("Erreur : " + e.getMessage());
            }
        }
    }
    
    // ==================== MÉTHODES EXISTANTES (conservées) ====================
    
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
    
    /**
     * Navigation vers la racine
     */
    private void naviguerVersRacine() {
        dossierActuel = null;
        chargerDocuments();
        mettreAJourFilAriane(null);
    }
    
    /**
     * Navigation vers un niveau du fil d'Ariane
     */
    private void naviguerVersNiveau(int niveau) {
        if (niveau < cheminDossiers.size()) {
            Dossier dossier = cheminDossiers.get(niveau);
            selectionnerDossier(dossier);
        }
    }
    
    /**
     * Met à jour le fil d'Ariane
     */
    private void mettreAJourFilAriane(Dossier dossier) {
        cheminDossiers.clear();
        construireCheminDossier(dossier);
        
        if (breadcrumbSeparator1 != null) breadcrumbSeparator1.setVisible(false);
        if (breadcrumbLevel1 != null) breadcrumbLevel1.setVisible(false);
        if (breadcrumbSeparator2 != null) breadcrumbSeparator2.setVisible(false);
        if (breadcrumbLevel2 != null) breadcrumbLevel2.setVisible(false);
        
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
        
        if (dossier.getDossierParentId() != null && dossier.getDossierParentId() > 0) {
            Dossier parent = dossierService.getDossierById(dossier.getDossierParentId());
            if (parent != null) {
                construireCheminDossier(parent);
            }
        }
        
        cheminDossiers.add(dossier);
    }
    
    /**
     * Configure la sélection dans la table
     */
    private void configurerSelectionDocument() {
        if (tableauDocuments != null) {
            tableauDocuments.getSelectionModel().selectedItemProperty().addListener(
                (obs, old, newVal) -> {
                    if (newVal != null) {
                        afficherDetailsDocument(newVal);
                    }
                }
            );
        }
    }
    
    /**
     * Affiche les détails d'un document
     */
    private void afficherDetailsDocument(Document doc) {
        this.documentSelectionne = doc;
        
        if (panneauDetailsDocument == null) return;
        
        panneauDetailsDocument.setVisible(true);
        
        if (labelCodeDocument != null) labelCodeDocument.setText(doc.getCodeDocument());
        if (labelNomFichier != null) labelNomFichier.setText(doc.getTitre());
        if (labelTypeFichier != null) labelTypeFichier.setText(doc.getExtension() != null ? doc.getExtension().toUpperCase() : "N/A");
        if (labelTailleFichier != null) labelTailleFichier.setText(formatTaille(doc.getTailleFichier()));
        
        if (labelDateCreation != null && doc.getDateCreation() != null) {
            labelDateCreation.setText(doc.getDateCreation().format(DATE_FORMATTER));
        }
        
        if (labelDateModification != null && doc.getDateModification() != null) {
            labelDateModification.setText(doc.getDateModification().format(DATE_FORMATTER));
        }
        
        if (labelAuteur != null) labelAuteur.setText(doc.getNomAuteur() != null ? doc.getNomAuteur() : "N/A");
        if (labelStatutDocument != null) labelStatutDocument.setText(doc.getStatut() != null ? doc.getStatut().toString() : "N/A");
        if (textAreaDescription != null) textAreaDescription.setText(doc.getDescription() != null ? doc.getDescription() : "");
        
        if (flowPaneMotsCles != null) {
            flowPaneMotsCles.getChildren().clear();
            if (doc.getMotsCles() != null && !doc.getMotsCles().isEmpty()) {
                String[] mots = doc.getMotsCles().split(",");
                for (String mot : mots) {
                    Label lblMot = new Label(mot.trim());
                    lblMot.setStyle("-fx-background-color: #3498db; -fx-text-fill: white; " +
                                   "-fx-padding: 5 10; -fx-background-radius: 15;");
                    flowPaneMotsCles.getChildren().add(lblMot);
                }
            }
        }
    }
    
    @FXML
    public void handleImporterDocument() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            showError("Utilisateur non connecté");
            return;
        }
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner un fichier");
        File fichier = fileChooser.showOpenDialog(null);
        
        if (fichier != null) {
            DocumentFormDialog dialog = new DocumentFormDialog(null, dossierActuel, currentUser);
            Optional<Document> result = dialog.showAndWait();
            
            result.ifPresent(document -> {
                try {
                    documentService.createDocument(document, fichier, currentUser);
                    
                    logService.logImportDocument(
                        document.getCodeDocument(),
                        fichier.getName(),
                        currentUser.getNomComplet()
                    );
                    
                    chargerDocuments();
                    showSuccess("✅ Document importé avec succès !");
                    
                } catch (Exception e) {
                    showError("Erreur d'importation : " + e.getMessage());
                }
            });
        }
    }
    
    @FXML
    public void handleNouveauDossier() {
        User currentUser = getCurrentUser();
        if (currentUser == null) {
            showError("Utilisateur non connecté");
            return;
        }
        
        if (!dossierService.peutCreerDossier(currentUser)) {
            showError("Vous n'avez pas les permissions pour créer un dossier");
            return;
        }
        
        DossierFormDialog dialog = new DossierFormDialog(null, dossierActuel);
        Optional<Dossier> result = dialog.showAndWait();
        
        result.ifPresent(dossier -> {
            try {
                dossierService.createDossier(dossier, currentUser);
                
                logService.logCreationDossier(
                    dossier.getCodeDossier(),
                    currentUser.getNomComplet()
                );
                
                chargerArborescence();
                showSuccess("✅ Dossier créé avec succès !");
                
            } catch (Exception e) {
                showError("Erreur : " + e.getMessage());
            }
        });
    }
    
    @FXML
    public void handleModifierDocument() {
        Document doc = tableauDocuments.getSelectionModel().getSelectedItem();
        User currentUser = getCurrentUser();
        
        if (doc == null) {
            showError("Veuillez sélectionner un document");
            return;
        }
        
        if (currentUser == null) {
            showError("Utilisateur non connecté");
            return;
        }
        
        DocumentFormDialog dialog = new DocumentFormDialog(doc, dossierActuel, currentUser);
        Optional<Document> result = dialog.showAndWait();
        
        result.ifPresent(docModifie -> {
            try {
                documentService.updateDocument(docModifie, currentUser.getId());
                
                logService.logModificationDocument(
                    docModifie.getCodeDocument(),
                    currentUser.getNomComplet()
                );
                
                chargerDocuments();
                showSuccess("✅ Document modifié avec succès !");
                
            } catch (Exception e) {
                showError("Erreur : " + e.getMessage());
            }
        });
    }
    
    @FXML
    public void handleSupprimerDocument() {
        Document doc = tableauDocuments.getSelectionModel().getSelectedItem();
        User currentUser = getCurrentUser();
        
        if (doc == null) {
            showError("Veuillez sélectionner un document");
            return;
        }
        
        if (currentUser == null) {
            showError("Utilisateur non connecté");
            return;
        }
        
        Alert confirmation = new Alert(Alert.AlertType.CONFIRMATION);
        confirmation.setTitle("Confirmation");
        confirmation.setHeaderText("Supprimer le document ?");
        confirmation.setContentText("Le document sera déplacé vers la corbeille.");
        
        Optional<ButtonType> resultat = confirmation.showAndWait();
        if (resultat.isPresent() && resultat.get() == ButtonType.OK) {
            if (documentService.supprimerDocument(doc.getId(), currentUser.getId())) {
                logService.logSuppressionDocument(
                    doc.getCodeDocument(),
                    currentUser.getNomComplet()
                );
                
                chargerDocuments();
                showSuccess("✅ Document supprimé avec succès !");
            } else {
                showError("Erreur lors de la suppression");
            }
        }
    }
    
    @FXML
    public void handleTelechargerDocument() {
        Document doc = documentSelectionne != null ? documentSelectionne : 
                      tableauDocuments.getSelectionModel().getSelectedItem();
        
        if (doc == null) {
            showError("Veuillez sélectionner un document");
            return;
        }
        
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choisir le dossier de destination");
        File destination = dirChooser.showDialog(null);
        
        if (destination != null) {
            try {
                File source = new File(doc.getCheminFichier());
                File target = new File(destination, source.getName());
                Files.copy(source.toPath(), target.toPath());
                
                showSuccess("✅ Document téléchargé avec succès !");
                
            } catch (Exception e) {
                showError("Erreur de téléchargement : " + e.getMessage());
            }
        }
    }
    
    @FXML
    public void handleDeplacerDocument() {
        Document doc = tableauDocuments.getSelectionModel().getSelectedItem();
        User currentUser = getCurrentUser();
        
        if (doc == null) {
            showError("Veuillez sélectionner un document");
            return;
        }
        
        if (currentUser == null) {
            showError("Utilisateur non connecté");
            return;
        }
        
        DeplacerDocumentDialog dialog = new DeplacerDocumentDialog(doc);
        Optional<Dossier> result = dialog.showAndWait();
        
        result.ifPresent(dossierDestination -> {
            try {
                documentService.deplacerDocument(
                    doc.getId(),
                    dossierDestination.getId(),
                    currentUser.getId()
                );
                
                logService.logDeplacementDocument(
                    doc.getCodeDocument(),
                    dossierDestination.getNomDossier()
                );
                
                chargerDocuments();
                showSuccess("✅ Document déplacé avec succès !");
                
            } catch (Exception e) {
                showError("Erreur : " + e.getMessage());
            }
        });
    }
    
    /**
     * Configure les colonnes du tableau
     */
    private void configurerColonnesTableau() {
        if (tableauDocuments == null) return;
        
        if (colonneNomDocument != null) {
            colonneNomDocument.setCellValueFactory(data -> {
                Document doc = data.getValue();
                String icone = getIconeParExtension(doc.getExtension());
                return new SimpleStringProperty(icone + " " + doc.getTitre());
            });
        }
        
        if (colonneTypeDocument != null) {
            colonneTypeDocument.setCellValueFactory(data -> {
                Document doc = data.getValue();
                String type = doc.getTypeDocument();
                
                // Si le type est vide ou null, le déduire de l'extension
                if (type == null || type.trim().isEmpty()) {
                    String extension = doc.getExtension();
                    if (extension != null && !extension.isEmpty()) {
                        type = deduireTypeDepuisExtension(extension);
                    } else {
                        type = "AUTRE";
                    }
                }
                
                return new SimpleStringProperty(type);
            });
        }
        
        if (colonneTailleDocument != null) {
            colonneTailleDocument.setCellValueFactory(data -> 
                new SimpleStringProperty(formatTaille(data.getValue().getTailleFichier())));
        }
        
        if (colonneAuteurDocument != null) {
            colonneAuteurDocument.setCellValueFactory(data -> {
                Document doc = data.getValue();
                
                // Priorité 1: Utiliser nomAuteur si disponible et valide
                String nomAuteur = doc.getNomAuteur();
                if (nomAuteur != null && !nomAuteur.isEmpty() 
                    && !nomAuteur.equals("Utilisateur") 
                    && !nomAuteur.equals("0") 
                    && !nomAuteur.trim().equals("0")) {
                    return new SimpleStringProperty(nomAuteur);
                }
                
                // Priorité 2: Utiliser SessionManager pour l'utilisateur actuel
                // (si c'est un document récemment créé/modifié)
                User currentUser = SessionManager.getInstance().getCurrentUser();
                if (currentUser != null) {
                    // Vérifier si c'est l'utilisateur actuel qui a créé le document
                    if (doc.getCreePar() != null && doc.getCreePar().equals(currentUser.getId())) {
                        return new SimpleStringProperty(currentUser.getNomComplet());
                    }
                }
                
                // Priorité 3: Afficher "Utilisateur" avec l'ID si disponible
                if (doc.getCreePar() != null && doc.getCreePar() > 0) {
                    return new SimpleStringProperty("Utilisateur #" + doc.getCreePar());
                }
                
                // Par défaut
                return new SimpleStringProperty("Système");
            });
        }
        
        if (colonneDateModification != null) {
            colonneDateModification.setCellValueFactory(data -> {
                Document doc = data.getValue();
                
                // Priorité 1: date_modification
                LocalDateTime date = doc.getDateModification();
                
                // Priorité 2: Si date_modification est null, utiliser date_creation
                if (date == null) {
                    date = doc.getDateCreation();
                }
                
                // Formater la date ou retourner "N/A"
                return new SimpleStringProperty(date != null ? 
                    date.format(DATE_FORMATTER) : "N/A");
            });
        }
        
        if (colonneStatutDocument != null) {
            colonneStatutDocument.setCellValueFactory(data -> 
                new SimpleStringProperty(data.getValue().getStatut() != null ? 
                    data.getValue().getStatut().toString() : "N/A"));
        }
        
        if (colonneActionsDocument != null) {
            colonneActionsDocument.setCellFactory(col -> new TableCell<>() {
                private final Button btnVoir = new Button("👁️");
                private final Button btnTelecharger = new Button("⬇️");
                private final Button btnSupprimer = new Button("🗑️");
                
                {
                    btnVoir.setOnAction(e -> {
                        Document doc = getTableView().getItems().get(getIndex());
                        ouvrirDocument(doc);
                    });
                    
                    btnTelecharger.setOnAction(e -> {
                        Document doc = getTableView().getItems().get(getIndex());
                        telechargerDocument(doc);
                    });
                    
                    btnSupprimer.setOnAction(e -> {
                        Document doc = getTableView().getItems().get(getIndex());
                        tableauDocuments.getSelectionModel().select(doc);
                        handleSupprimerDocument();
                    });
                    
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
            tousLesDossiers = new ArrayList<>(dossiers); // Conserver pour la recherche
            
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
    
    /**
     * Formate la taille d'un fichier
     */
    private String formatTaille(long taille) {
        if (taille < 1024) return taille + " o";
        if (taille < 1024 * 1024) return String.format("%.2f Ko", taille / 1024.0);
        if (taille < 1024 * 1024 * 1024) return String.format("%.2f Mo", taille / (1024.0 * 1024));
        return String.format("%.2f Go", taille / (1024.0 * 1024 * 1024));
    }
    
    
    // ==================== MÉTHODES POUR ACTIONS MULTIPLES ====================
    
    /**
     * Sélectionne tous les documents dans le tableau
     */
    @FXML
    public void handleToutSelectionner() {
        if (tableauDocuments != null) {
            tableauDocuments.getSelectionModel().selectAll();
            int count = tableauDocuments.getSelectionModel().getSelectedItems().size();
            showInfo(count + " document(s) sélectionné(s)");
        }
    }
    
    /**
     * Télécharge les documents sélectionnés
     */
    @FXML
    public void handleTelechargerSelection() {
        if (tableauDocuments == null) return;
        
        var selectedItems = tableauDocuments.getSelectionModel().getSelectedItems();
        
        if (selectedItems.isEmpty()) {
            showError("Veuillez sélectionner au moins un document");
            return;
        }
        
        if (selectedItems.size() == 1) {
            // Un seul document : utiliser le handler existant
            this.documentSelectionne = selectedItems.get(0);
            handleTelechargerDocument();
            return;
        }
        
        // Plusieurs documents : choisir un dossier de destination
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Choisir le dossier de destination");
        File destination = dirChooser.showDialog(null);
        
        if (destination != null) {
            int success = 0;
            int errors = 0;
            
            for (Document doc : selectedItems) {
                try {
                    File source = new File(doc.getCheminFichier());
                    File target = new File(destination, source.getName());
                    Files.copy(source.toPath(), target.toPath(), 
                              java.nio.file.StandardCopyOption.REPLACE_EXISTING);
                    success++;
                } catch (Exception e) {
                    System.err.println("Erreur téléchargement " + doc.getTitre() + ": " + e.getMessage());
                    errors++;
                }
            }
            
            if (errors == 0) {
                showSuccess("✅ " + success + " document(s) téléchargé(s) avec succès !");
            } else {
                showAlert("Téléchargement partiel", 
                         success + " document(s) téléchargé(s), " + errors + " erreur(s)");
            }
        }
    }
    
    // Handlers pour les autres actions
    @FXML public void handleRecherche() { /* Déjà implémenté via listener */ }
    @FXML public void handleActualiser() { 
        chargerArborescence(); 
        chargerDocuments(); 
        showInfo("✅ Données actualisées");
    }
    @FXML public void handleDeplacerSelection() { handleDeplacerDocument(); }
    @FXML public void handleSupprimerSelection() { handleSupprimerDocument(); }
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
        Optional<PartageInfo> result = dialog.showAndWait();
        
        result.ifPresent(partageInfo -> {
            try {
                for (User user : partageInfo.getUtilisateurs()) {
                    // TODO: Implémenter dans DocumentService
                }
                
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
    
    @FXML public void handleApercuComplet() { 
        if (documentSelectionne != null) ouvrirDocument(documentSelectionne); 
    }
    
    
    /**
     * Déduit le type de document depuis son extension
     */
    private String deduireTypeDepuisExtension(String extension) {
        if (extension == null || extension.isEmpty()) {
            return "AUTRE";
        }
        
        // Normaliser l'extension (minuscules, sans point)
        extension = extension.toLowerCase().trim();
        if (extension.startsWith(".")) {
            extension = extension.substring(1);
        }
        
        // Mapper l'extension vers un type
        return switch (extension) {
            case "pdf" -> "PDF";
            case "doc", "docx" -> "WORD";
            case "xls", "xlsx", "csv" -> "EXCEL";
            case "ppt", "pptx" -> "POWERPOINT";
            case "jpg", "jpeg", "png", "gif", "bmp", "svg" -> "IMAGE";
            case "txt", "log", "md" -> "TEXT";
            case "zip", "rar", "7z", "tar", "gz" -> "ARCHIVE";
            case "mp4", "avi", "mkv", "mov" -> "VIDEO";
            case "mp3", "wav", "flac", "ogg" -> "AUDIO";
            case "html", "htm", "css", "js" -> "WEB";
            case "java", "py", "cpp", "c", "php" -> "CODE";
            default -> "AUTRE";
        };
    }
    
    // Méthodes utilitaires
    private User getCurrentUser() { return SessionManager.getInstance().getCurrentUser(); }
    private void showAlert(String t, String m) { new Alert(Alert.AlertType.WARNING, m).showAndWait(); }
    private void showError(String m) { new Alert(Alert.AlertType.ERROR, m).showAndWait(); }
    private void showSuccess(String m) { new Alert(Alert.AlertType.INFORMATION, m).showAndWait(); }
    private void showInfo(String m) { new Alert(Alert.AlertType.INFORMATION, m).showAndWait(); }
}