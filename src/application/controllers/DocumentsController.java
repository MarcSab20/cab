package application.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.FileChooser;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import application.models.*;
import application.services.*;
import application.utils.*;

import java.io.File;
import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Contrôleur complet pour la gestion des documents
 * Fonctionnalités:
 * - Upload et téléchargement
 * - Organisation en dossiers
 * - Recherche et filtres
 * - Gestion des versions
 * - Permissions basées sur niveau d'autorité
 */
public class DocumentsController implements Initializable {
    
    // Filtres et recherche
    @FXML private ComboBox<String> filtreType;
    @FXML private ComboBox<String> filtreStatutDoc;
    @FXML private TextField champRechercheDoc;
    
    // Navigation
    @FXML private TreeView<Dossier> arborescenceDossiers;
    @FXML private Label labelDossierActuel;
    
    // Affichage
    @FXML private ToggleGroup toggleAffichage;
    @FXML private ToggleButton btnAffichageListe;
    @FXML private TableView<Document> tableauDocuments;
    
    // Colonnes
    @FXML private TableColumn<Document, String> colonneNomDocument;
    @FXML private TableColumn<Document, String> colonneTypeDocument;
    @FXML private TableColumn<Document, String> colonneTailleDocument;
    @FXML private TableColumn<Document, String> colonneAuteurDocument;
    @FXML private TableColumn<Document, String> colonneDateModification;
    @FXML private TableColumn<Document, String> colonneStatutDocument;
    
    // Tri et pagination
    @FXML private ComboBox<String> comboTriDocuments;
    @FXML private Label labelNombreDocuments;
    
    // Détails du document
    @FXML private VBox panneauDetailsDocument;
    @FXML private Label labelNomFichier;
    @FXML private Label labelTypeFichier;
    @FXML private Label labelTailleFichier;
    @FXML private Label labelDateCreation;
    @FXML private Label labelDateModification;
    @FXML private Label labelAuteur;
    @FXML private Label labelStatutDocument;
    @FXML private Label labelCodeDocument;
    @FXML private TextArea textAreaDescription;
    @FXML private FlowPane flowPaneMotsCles;
    @FXML private VBox listeVersions;
    
    // Boutons d'action
    @FXML private Button btnTelecharger;
    @FXML private Button btnModifierDocument;
    @FXML private Button btnPartagerDocument;
    @FXML private Button btnDeplacerDocument;
    @FXML private Button btnSupprimerDocument;
    @FXML private Button btnFermerDetails;
    
    private User currentUser;
    private DocumentService documentService;
    private DossierService dossierService;
    private ObservableList<Document> documents;
    private Document selectedDocument;
    private Dossier dossierActuel;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("DocumentsController.initialize() appelé");
        
        try {
            currentUser = SessionManager.getInstance().getCurrentUser();
            documentService = DocumentService.getInstance();
            dossierService = DossierService.getInstance();
            documents = FXCollections.observableArrayList();
            
            if (currentUser == null) {
                System.err.println("ERREUR: Aucun utilisateur en session");
                return;
            }
            
            System.out.println("Initialisation de la gestion des documents pour: " + currentUser.getNomComplet());
            
            setupInterface();
            setupTableColumns();
            setupFilters();
            setupButtons();
            loadArborescenceDossiers();
            loadDocuments();
            
        } catch (Exception e) {
            System.err.println("Erreur dans DocumentsController.initialize(): " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Configure l'interface utilisateur
     */
    private void setupInterface() {
        // Masquer les détails par défaut
        if (panneauDetailsDocument != null) {
            panneauDetailsDocument.setVisible(false);
            panneauDetailsDocument.setManaged(false);
        }
        
        // Configuration du tri
        if (comboTriDocuments != null) {
            comboTriDocuments.setItems(FXCollections.observableArrayList(
                "Nom", "Date modification", "Date création", "Taille", "Type"
            ));
            comboTriDocuments.setValue("Date modification");
            comboTriDocuments.setOnAction(e -> handleTriChange());
        }
    }
    
    /**
     * Configure les colonnes du tableau
     */
    private void setupTableColumns() {
        // Icône + Nom
        colonneNomDocument.setCellValueFactory(cellData -> {
            Document doc = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                doc.getIcone() + " " + doc.getTitre()
            );
        });
        
        // Type
        colonneTypeDocument.setCellValueFactory(new PropertyValueFactory<>("typeDocument"));
        
        // Taille formatée
        colonneTailleDocument.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getTailleFormatee()
            )
        );
        
        // Auteur
        colonneAuteurDocument.setCellValueFactory(cellData -> {
            Document doc = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(doc.getNomAuteur());
        });
        
        // Date de modification
        colonneDateModification.setCellValueFactory(cellData -> {
            Document doc = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                doc.getDateModificationFormatee()
            );
        });
        
        // Statut
        colonneStatutDocument.setCellValueFactory(cellData -> {
            Document doc = cellData.getValue();
            return new javafx.beans.property.SimpleStringProperty(
                doc.getStatut().getIcone() + " " + doc.getStatut().getLibelle()
            );
        });
        
        // Listener pour la sélection
        tableauDocuments.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    showDocumentDetails(newSelection);
                }
            }
        );
    }
    
    /**
     * Configure les filtres
     */
    private void setupFilters() {
        if (filtreType != null) {
            filtreType.setItems(FXCollections.observableArrayList(
                "Tous", "PERM", "AUTH", "DEM", "COMM", "CIRC", "CONV", "REP", "NOTIF", "RAPP", "PV", "NOTE", "CR"
            ));
            filtreType.setValue("Tous");
            filtreType.setOnAction(e -> applyFilters());
        }
        
        if (filtreStatutDoc != null) {
            filtreStatutDoc.setItems(FXCollections.observableArrayList(
                "Tous", "Actif", "Archivé", "Brouillon", "En cours", "Validé"
            ));
            filtreStatutDoc.setValue("Tous");
            filtreStatutDoc.setOnAction(e -> applyFilters());
        }
    }
    
    /**
     * Configure les boutons
     */
    private void setupButtons() {
        if (btnTelecharger != null) {
            btnTelecharger.setOnAction(e -> handleTelecharger());
        }
        if (btnModifierDocument != null) {
            btnModifierDocument.setOnAction(e -> handleModifierDocument());
        }
        if (btnPartagerDocument != null) {
            btnPartagerDocument.setOnAction(e -> handlePartagerDocument());
        }
        if (btnDeplacerDocument != null) {
            btnDeplacerDocument.setOnAction(e -> handleDeplacerDocument());
        }
        if (btnSupprimerDocument != null) {
            btnSupprimerDocument.setOnAction(e -> handleSupprimerDocument());
        }
        if (btnFermerDetails != null) {
            btnFermerDetails.setOnAction(e -> hideDocumentDetails());
        }
    }
    
    /**
     * Charge l'arborescence des dossiers
     */
    private void loadArborescenceDossiers() {
        if (arborescenceDossiers == null) return;
        
        try {
            // Créer le noeud racine
            Dossier racine = dossierService.getDossierByCode("ROOT");
            if (racine == null) {
                System.err.println("Dossier racine non trouvé");
                return;
            }
            
            TreeItem<Dossier> rootItem = new TreeItem<>(racine);
            rootItem.setExpanded(true);
            
            // Construire l'arbre récursivement
            construireArbre(rootItem, racine.getId());
            
            arborescenceDossiers.setRoot(rootItem);
            
            // Formatter les cellules
            arborescenceDossiers.setCellFactory(tv -> new TreeCell<>() {
                @Override
                protected void updateItem(Dossier item, boolean empty) {
                    super.updateItem(item, empty);
                    
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(item.getIcone() + " " + item.getNomDossier() + 
                               " (" + item.getNombreDocuments() + ")");
                    }
                }
            });
            
            // Listener pour la sélection
            arborescenceDossiers.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldVal, newVal) -> {
                    if (newVal != null) {
                        dossierActuel = newVal.getValue();
                        loadDocumentsDossier(dossierActuel.getId());
                    }
                }
            );
            
            System.out.println("Arborescence chargée avec succès");
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de l'arborescence: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Construit l'arbre des dossiers récursivement
     */
    private void construireArbre(TreeItem<Dossier> parentItem, int parentId) {
        List<Dossier> sousDossiers = dossierService.getSousDossiers(parentId);
        
        for (Dossier dossier : sousDossiers) {
            TreeItem<Dossier> item = new TreeItem<>(dossier);
            parentItem.getChildren().add(item);
            
            // Récursif pour les sous-dossiers
            if (dossier.getNombreSousDossiers() > 0) {
                construireArbre(item, dossier.getId());
            }
        }
    }
    
    /**
     * Charge les documents du dossier courant
     */
    private void loadDocumentsDossier(int dossierId) {
        try {
            Dossier dossier = dossierService.getDossierById(dossierId);
            
            if (dossier != null && labelDossierActuel != null) {
                labelDossierActuel.setText("📁 " + dossier.getNomDossier());
            }
            
            List<Document> list = documentService.getDocumentsByDossier(dossierId);
            documents.clear();
            documents.addAll(list);
            tableauDocuments.setItems(documents);
            
            if (labelNombreDocuments != null) {
                labelNombreDocuments.setText("(" + documents.size() + " documents)");
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des documents du dossier: " + e.getMessage());
        }
    }
    
    /**
     * Charge tous les documents
     */
    private void loadDocuments() {
        try {
            List<Document> list = documentService.getAllDocuments();
            documents.clear();
            documents.addAll(list);
            tableauDocuments.setItems(documents);
            
            if (labelNombreDocuments != null) {
                labelNombreDocuments.setText("(" + documents.size() + " documents)");
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des documents: " + e.getMessage());
            AlertUtils.showError("Erreur lors du chargement des documents");
        }
    }
    
    /**
     * Applique les filtres de recherche
     */
    @FXML
    private void handleRecherche() {
        applyFilters();
    }
    
    /**
     * Applique les filtres
     */
    private void applyFilters() {
        try {
            String recherche = champRechercheDoc != null ? champRechercheDoc.getText() : "";
            String typeFilter = filtreType != null ? filtreType.getValue() : "Tous";
            String statutFilter = filtreStatutDoc != null ? filtreStatutDoc.getValue() : "Tous";
            
            Integer dossierId = dossierActuel != null ? dossierActuel.getId() : null;
            
            List<Document> filtered = documentService.rechercherDocuments(
                recherche, 
                typeFilter, 
                statutFilter, 
                dossierId
            );
            
            documents.clear();
            documents.addAll(filtered);
            
            if (labelNombreDocuments != null) {
                labelNombreDocuments.setText("(" + documents.size() + " documents)");
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'application des filtres: " + e.getMessage());
        }
    }
    
    /**
     * Gère le changement de tri
     */
    private void handleTriChange() {
        if (comboTriDocuments == null) return;
        
        String tri = comboTriDocuments.getValue();
        
        switch (tri) {
            case "Nom":
                documents.sort((d1, d2) -> d1.getTitre().compareToIgnoreCase(d2.getTitre()));
                break;
            case "Date modification":
                documents.sort((d1, d2) -> {
                    if (d1.getDateModification() == null) return 1;
                    if (d2.getDateModification() == null) return -1;
                    return d2.getDateModification().compareTo(d1.getDateModification());
                });
                break;
            case "Date création":
                documents.sort((d1, d2) -> {
                    if (d1.getDateCreation() == null) return 1;
                    if (d2.getDateCreation() == null) return -1;
                    return d2.getDateCreation().compareTo(d1.getDateCreation());
                });
                break;
            case "Taille":
                documents.sort((d1, d2) -> Long.compare(d2.getTailleFichier(), d1.getTailleFichier()));
                break;
            case "Type":
                documents.sort((d1, d2) -> {
                    String type1 = d1.getTypeDocument() != null ? d1.getTypeDocument() : "";
                    String type2 = d2.getTypeDocument() != null ? d2.getTypeDocument() : "";
                    return type1.compareToIgnoreCase(type2);
                });
                break;
        }
        
        tableauDocuments.refresh();
    }
    
    /**
     * Affiche les détails d'un document
     */
    private void showDocumentDetails(Document document) {
        selectedDocument = document;
        
        if (panneauDetailsDocument != null) {
            panneauDetailsDocument.setVisible(true);
            panneauDetailsDocument.setManaged(true);
        }
        
        // Informations de base
        if (labelCodeDocument != null) {
            labelCodeDocument.setText(document.getCodeDocument());
        }
        
        if (labelNomFichier != null) {
            labelNomFichier.setText(document.getTitre());
        }
        
        if (labelTypeFichier != null) {
            String type = document.getExtension() != null ? 
                "Document " + document.getExtension().toUpperCase() : "Document";
            labelTypeFichier.setText(type);
        }
        
        if (labelTailleFichier != null) {
            labelTailleFichier.setText(document.getTailleFormatee());
        }
        
        if (labelDateCreation != null && document.getDateCreation() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy HH:mm");
            labelDateCreation.setText(document.getDateCreation().format(formatter));
        }
        
        if (labelDateModification != null) {
            labelDateModification.setText(document.getDateModificationFormatee());
        }
        
        if (labelAuteur != null) {
            labelAuteur.setText(document.getNomAuteur());
        }
        
        if (labelStatutDocument != null) {
            labelStatutDocument.setText(document.getStatut().getIcone() + " " + 
                                       document.getStatut().getLibelle());
        }
        
        if (textAreaDescription != null) {
            textAreaDescription.setText(document.getDescription() != null ? document.getDescription() : "");
        }
        
        // Mots-clés
        if (flowPaneMotsCles != null) {
            flowPaneMotsCles.getChildren().clear();
            if (document.getMotsCles() != null) {
                String[] mots = document.getMotsCles().split(",");
                for (String mot : mots) {
                    Label tag = new Label(mot.trim());
                    tag.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; " +
                               "-fx-padding: 2 6; -fx-background-radius: 10; -fx-font-size: 11px;");
                    flowPaneMotsCles.getChildren().add(tag);
                }
            }
        }
        
        // Charger les versions
        loadVersions(document.getId());
        
        // Gérer les permissions des boutons
        updateButtonPermissions();
    }
    
    /**
     * Charge les versions d'un document
     */
    private void loadVersions(int documentId) {
        if (listeVersions == null) return;
        
        listeVersions.getChildren().clear();
        
        List<VersionDocument> versions = documentService.getVersionsDocument(documentId);
        
        for (VersionDocument version : versions) {
            HBox versionBox = new HBox(10);
            versionBox.setStyle("-fx-padding: 8; -fx-background-color: " + 
                              (version.estVersionActuelle(selectedDocument.getVersion()) ? 
                               "#e8f5e8" : "white") + 
                              "; -fx-background-radius: 4;");
            
            VBox infoBox = new VBox(2);
            Label labelVersion = new Label(version.getLibelleVersion());
            labelVersion.setStyle("-fx-font-weight: bold;");
            
            Label labelDate = new Label(version.getDateCreationFormatee());
            labelDate.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
            
            infoBox.getChildren().addAll(labelVersion, labelDate);
            
            HBox.setHgrow(infoBox, Priority.ALWAYS);
            versionBox.getChildren().add(infoBox);
            
            listeVersions.getChildren().add(versionBox);
        }
    }
    
    /**
     * Met à jour les permissions des boutons
     */
    private void updateButtonPermissions() {
        if (selectedDocument == null) return;
        
        boolean estProprietaire = selectedDocument.getCreePar() != null && 
                                 selectedDocument.getCreePar().getId() == currentUser.getId();
        boolean estNiveau01 = currentUser.getNiveauAutorite() <= 1;
        
        // Modifier : propriétaire ou niveau 0/1
        if (btnModifierDocument != null) {
            btnModifierDocument.setDisable(!estProprietaire && !estNiveau01);
        }
        
        // Supprimer : uniquement niveau 0/1
        if (btnSupprimerDocument != null) {
            btnSupprimerDocument.setDisable(!estNiveau01);
        }
        
        // Télécharger : tout le monde
        if (btnTelecharger != null) {
            btnTelecharger.setDisable(false);
        }
    }
    
    /**
     * Masque les détails
     */
    private void hideDocumentDetails() {
        selectedDocument = null;
        
        if (panneauDetailsDocument != null) {
            panneauDetailsDocument.setVisible(false);
            panneauDetailsDocument.setManaged(false);
        }
    }
    
    /**
     * Gère l'import d'un nouveau document
     */
    @FXML
    private void handleImporterDocument() {
        // Dialogue pour sélectionner le fichier
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner un document");
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"),
            new FileChooser.ExtensionFilter("Documents PDF", "*.pdf"),
            new FileChooser.ExtensionFilter("Documents Word", "*.doc", "*.docx"),
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg")
        );
        
        File fichier = fileChooser.showOpenDialog(tableauDocuments.getScene().getWindow());
        
        if (fichier != null) {
            // Dialogue pour les informations du document
            DocumentFormDialog dialog = new DocumentFormDialog(null, dossierActuel, currentUser);
            Optional<Document> result = dialog.showAndWait();
            
            result.ifPresent(document -> {
                boolean success = documentService.createDocument(document, fichier, currentUser);
                
                if (success) {
                    AlertUtils.showInfo("Document importé avec succès!\nCode: " + document.getCodeDocument());
                    
                    // Recharger la liste
                    if (dossierActuel != null) {
                        loadDocumentsDossier(dossierActuel.getId());
                    } else {
                        loadDocuments();
                    }
                } else {
                    AlertUtils.showError("Erreur lors de l'importation du document");
                }
            });
        }
    }
    
    /**
     * Gère le téléchargement d'un document
     */
    @FXML
    private void handleTelecharger() {
        if (selectedDocument == null) {
            AlertUtils.showWarning("Veuillez sélectionner un document");
            return;
        }
        
        try {
            File fichierSource = new File(selectedDocument.getCheminFichier());
            
            if (!fichierSource.exists()) {
                AlertUtils.showError("Fichier source introuvable");
                return;
            }
            
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer le document");
            fileChooser.setInitialFileName(selectedDocument.getNomFichier());
            
            File destination = fileChooser.showSaveDialog(tableauDocuments.getScene().getWindow());
            
            if (destination != null) {
                java.nio.file.Files.copy(
                    fichierSource.toPath(), 
                    destination.toPath(), 
                    java.nio.file.StandardCopyOption.REPLACE_EXISTING
                );
                
                AlertUtils.showInfo("Document téléchargé avec succès!");
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du téléchargement: " + e.getMessage());
            AlertUtils.showError("Erreur lors du téléchargement du document");
        }
    }
    
    /**
     * Gère la modification d'un document
     */
    @FXML
    private void handleModifierDocument() {
        if (selectedDocument == null) {
            AlertUtils.showWarning("Veuillez sélectionner un document");
            return;
        }
        
        boolean confirm = AlertUtils.showConfirmation(
            "Modifier le document",
            "Voulez-vous remplacer le fichier actuel ?\n" +
            "Cela créera une nouvelle version (v" + (selectedDocument.getVersion() + 1) + ")."
        );
        
        if (!confirm) return;
        
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner le nouveau fichier");
        File nouveauFichier = fileChooser.showOpenDialog(tableauDocuments.getScene().getWindow());
        
        if (nouveauFichier != null) {
            boolean success = documentService.updateDocument(selectedDocument, nouveauFichier, currentUser);
            
            if (success) {
                AlertUtils.showInfo("Document mis à jour! Nouvelle version: v" + selectedDocument.getVersion());
                
                // Recharger
                if (dossierActuel != null) {
                    loadDocumentsDossier(dossierActuel.getId());
                } else {
                    loadDocuments();
                }
                
                showDocumentDetails(selectedDocument);
            } else {
                AlertUtils.showError("Erreur lors de la mise à jour");
            }
        }
    }
    
    /**
     * Gère le partage d'un document
     */
    @FXML
    private void handlePartagerDocument() {
        if (selectedDocument == null) {
            AlertUtils.showWarning("Veuillez sélectionner un document");
            return;
        }
        
        AlertUtils.showInfo("Fonctionnalité de partage en cours de développement");
    }
    
    /**
     * Gère le déplacement d'un document
     */
    @FXML
    private void handleDeplacerDocument() {
        if (selectedDocument == null) {
            AlertUtils.showWarning("Veuillez sélectionner un document");
            return;
        }
        
        // Dialogue pour sélectionner le dossier destination
        List<Dossier> dossiers = dossierService.getAllDossiers();
        
        ChoiceDialog<Dossier> dialog = new ChoiceDialog<>(dossierActuel, dossiers);
        dialog.setTitle("Déplacer le document");
        dialog.setHeaderText("Sélectionnez le dossier de destination");
        dialog.setContentText("Dossier:");
        
        Optional<Dossier> result = dialog.showAndWait();
        
        result.ifPresent(dossier -> {
            boolean success = documentService.deplacerDocument(selectedDocument.getId(), dossier.getId());
            
            if (success) {
                AlertUtils.showInfo("Document déplacé vers " + dossier.getNomDossier());
                
                if (dossierActuel != null) {
                    loadDocumentsDossier(dossierActuel.getId());
                } else {
                    loadDocuments();
                }
            } else {
                AlertUtils.showError("Erreur lors du déplacement");
            }
        });
    }
    
    /**
     * Gère la suppression d'un document
     */
    @FXML
    private void handleSupprimerDocument() {
        if (selectedDocument == null) {
            AlertUtils.showWarning("Veuillez sélectionner un document");
            return;
        }
        
        boolean confirm = AlertUtils.showConfirmation(
            "Confirmation",
            "Êtes-vous sûr de vouloir supprimer définitivement ce document ?\n" +
            "Cette action est irréversible!"
        );
        
        if (confirm) {
            boolean success = documentService.deleteDocument(selectedDocument.getId(), currentUser);
            
            if (success) {
                AlertUtils.showInfo("Document supprimé avec succès");
                hideDocumentDetails();
                
                if (dossierActuel != null) {
                    loadDocumentsDossier(dossierActuel.getId());
                } else {
                    loadDocuments();
                }
            } else {
                AlertUtils.showError("Erreur lors de la suppression.\n" +
                    "Vérifiez que vous avez les permissions nécessaires.");
            }
        }
    }
    
    /**
     * Gère la création d'un nouveau dossier
     */
    @FXML
    private void handleNouveauDossier() {
        DossierFormDialog dialog = new DossierFormDialog(null, dossierActuel);
        Optional<Dossier> result = dialog.showAndWait();
        
        result.ifPresent(dossier -> {
            boolean success = dossierService.createDossier(dossier, currentUser);
            
            if (success) {
                AlertUtils.showInfo("Dossier créé avec succès!");
                loadArborescenceDossiers();
            } else {
                AlertUtils.showError("Erreur lors de la création du dossier");
            }
        });
    }
}