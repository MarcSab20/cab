package application.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.scene.input.MouseEvent;
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
    
    // Boutons d'action principaux
    @FXML private Button btnNouveauDossier;
    @FXML private Button btnNouveauDocument;
    @FXML private Button btnImporter;
    @FXML private Button btnRechercher;
    
    // Boutons d'actions rapides
    @FXML private Button btnToutSelectionner;
    @FXML private Button btnTelechargerSelection;
    @FXML private Button btnDeplacerSelection;
    @FXML private Button btnSupprimerSelection;
    @FXML private Button btnRafraichirArbre;
    @FXML private Button btnCreerSousDossier;
    @FXML private Button btnActualiser;
    @FXML private Button btnAjouterPremierDoc;
    
    // Boutons de pagination
    @FXML private Button btnPagePrecedente;
    @FXML private Button btnPageSuivante;
    @FXML private Label labelPagination;
    @FXML private ComboBox<String> comboElementsParPage;
    
    // Boutons d'affichage
    @FXML private ToggleButton btnAffichageListe;
    @FXML private ToggleButton btnAffichageGrille;
    @FXML private ToggleButton btnAffichageDetails;
    
    // Raccourcis
    @FXML private HBox raccourciFavoris;
    @FXML private HBox raccourciRecents;
    @FXML private HBox raccourciCorbeille;
    @FXML private HBox raccourciPartages;
    
    // Filtres et recherche
    @FXML private ComboBox<String> filtreType;
    @FXML private ComboBox<String> filtreStatutDoc;
    @FXML private TextField champRechercheDoc;
    
    // Navigation
    @FXML private TreeView<Dossier> arborescenceDossiers;
    @FXML private Label labelDossierActuel;
    
    // Breadcrumbs
    @FXML private Hyperlink breadcrumbRoot;
    @FXML private Label breadcrumbSeparator1;
    @FXML private Hyperlink breadcrumbLevel1;
    @FXML private Label breadcrumbSeparator2;
    @FXML private Hyperlink breadcrumbLevel2;
    
    // Affichage
    @FXML private ToggleGroup toggleAffichage;
    @FXML private TableView<Document> tableauDocuments;
    @FXML private ScrollPane vueGrille;
    @FXML private FlowPane grilleDocuments;
    
    // Colonnes
    @FXML private TableColumn<Document, String> colonneSelection;
    @FXML private TableColumn<Document, String> colonneNomDocument;
    @FXML private TableColumn<Document, String> colonneTypeDocument;
    @FXML private TableColumn<Document, String> colonneTailleDocument;
    @FXML private TableColumn<Document, String> colonneAuteurDocument;
    @FXML private TableColumn<Document, String> colonneDateModification;
    @FXML private TableColumn<Document, String> colonneStatutDocument;
    @FXML private TableColumn<Document, Void> colonneActionsDocument;
    
    // Tri et pagination
    @FXML private ComboBox<String> comboTriDocuments;
    @FXML private Label labelNombreDocuments;
    
    // Détails du document
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
    @FXML private VBox listeVersions;
    @FXML private VBox listePermissions;
    @FXML private VBox listeActivites;
    
    // Boutons de détails
    @FXML private Button btnApercuComplet;
    @FXML private Button btnTelecharger;
    @FXML private Button btnModifierDocument;
    @FXML private Button btnPartagerDocument;
    @FXML private Button btnPartagerDetails;
    @FXML private Button btnDeplacerDocument;
    @FXML private Button btnSupprimerDocument;
    @FXML private Button btnFermerDetails;
    @FXML private Button btnNouvelleVersion;
    
    private User currentUser;
    private DocumentService documentService;
    private DossierService dossierService;
    private ObservableList<Document> documents;
    private Document selectedDocument;
    private Dossier dossierActuel;
    
    // Variables de pagination
    private int currentPage = 1;
    private int elementsPerPage = 25;
    private int totalPages = 1;
    
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
            setupBreadcrumbs();
            setupPagination();
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
        
        // Configuration de la vue grille (masquée par défaut)
        if (vueGrille != null) {
            vueGrille.setVisible(false);
            vueGrille.setManaged(false);
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
     * Configure le fil d'Ariane (breadcrumbs)
     */
    private void setupBreadcrumbs() {
        if (breadcrumbRoot != null) {
            breadcrumbRoot.setOnAction(e -> naviguerVersDossier(null));
        }
    }
    
    /**
     * Configure la pagination
     */
    private void setupPagination() {
        if (comboElementsParPage != null) {
            comboElementsParPage.setValue(String.valueOf(elementsPerPage));
        }
        updatePaginationControls();
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
                        updateBreadcrumbs();
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
            
            updatePaginationAndDisplay();
            
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
            
            updatePaginationAndDisplay();
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des documents: " + e.getMessage());
            AlertUtils.showError("Erreur lors du chargement des documents");
        }
    }
    
    /**
     * Met à jour le fil d'Ariane
     */
    private void updateBreadcrumbs() {
        if (dossierActuel == null) {
            // Racine seulement
            if (breadcrumbSeparator1 != null) breadcrumbSeparator1.setVisible(false);
            if (breadcrumbLevel1 != null) breadcrumbLevel1.setVisible(false);
            if (breadcrumbSeparator2 != null) breadcrumbSeparator2.setVisible(false);
            if (breadcrumbLevel2 != null) breadcrumbLevel2.setVisible(false);
            return;
        }
        
        // TODO: Implémenter la logique du fil d'Ariane complet
    }
    
    /**
     * Met à jour la pagination et l'affichage
     */
    private void updatePaginationAndDisplay() {
        totalPages = (int) Math.ceil((double) documents.size() / elementsPerPage);
        if (totalPages == 0) totalPages = 1;
        
        currentPage = Math.min(currentPage, totalPages);
        
        updatePaginationControls();
        updateTableDisplay();
        
        if (labelNombreDocuments != null) {
            labelNombreDocuments.setText("(" + documents.size() + " documents)");
        }
    }
    
    /**
     * Met à jour les contrôles de pagination
     */
    private void updatePaginationControls() {
        if (labelPagination != null) {
            labelPagination.setText("Page " + currentPage + " sur " + totalPages);
        }
        
        if (btnPagePrecedente != null) {
            btnPagePrecedente.setDisable(currentPage <= 1);
        }
        
        if (btnPageSuivante != null) {
            btnPageSuivante.setDisable(currentPage >= totalPages);
        }
    }
    
    /**
     * Met à jour l'affichage du tableau avec la pagination
     */
    private void updateTableDisplay() {
        int startIndex = (currentPage - 1) * elementsPerPage;
        int endIndex = Math.min(startIndex + elementsPerPage, documents.size());
        
        ObservableList<Document> pageDocuments = FXCollections.observableArrayList(
            documents.subList(startIndex, endIndex)
        );
        
        tableauDocuments.setItems(pageDocuments);
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
            
            currentPage = 1;
            updatePaginationAndDisplay();
            
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
        
        updateTableDisplay();
    }
    
    // ==================== GESTIONNAIRES D'ÉVÉNEMENTS ====================
    
    /**
     * Gère le bouton Nouveau Dossier
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
    
    /**
     * Gère le bouton Importer Document
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
     * Gère le bouton Tout Sélectionner
     */
    @FXML
    private void handleToutSelectionner() {
        tableauDocuments.getSelectionModel().selectAll();
    }
    
    /**
     * Gère le téléchargement de la sélection
     */
    @FXML
    private void handleTelechargerSelection() {
        ObservableList<Document> selection = tableauDocuments.getSelectionModel().getSelectedItems();
        
        if (selection.isEmpty()) {
            AlertUtils.showWarning("Aucun document sélectionné");
            return;
        }
        
        AlertUtils.showInfo("Téléchargement de " + selection.size() + " document(s) en cours...");
        // TODO: Implémenter le téléchargement multiple
    }
    
    /**
     * Gère le déplacement de la sélection
     */
    @FXML
    private void handleDeplacerSelection() {
        ObservableList<Document> selection = tableauDocuments.getSelectionModel().getSelectedItems();
        
        if (selection.isEmpty()) {
            AlertUtils.showWarning("Aucun document sélectionné");
            return;
        }
        
        AlertUtils.showInfo("Déplacement de " + selection.size() + " document(s)...");
        // TODO: Implémenter le déplacement multiple
    }
    
    /**
     * Gère la suppression de la sélection
     */
    @FXML
    private void handleSupprimerSelection() {
        ObservableList<Document> selection = tableauDocuments.getSelectionModel().getSelectedItems();
        
        if (selection.isEmpty()) {
            AlertUtils.showWarning("Aucun document sélectionné");
            return;
        }
        
        boolean confirm = AlertUtils.showConfirmation(
            "Confirmation",
            "Êtes-vous sûr de vouloir supprimer " + selection.size() + " document(s) ?"
        );
        
        if (confirm) {
            // TODO: Implémenter la suppression multiple
            AlertUtils.showInfo("Suppression en cours...");
        }
    }
    
    /**
     * Gère l'affichage en mode liste
     */
    @FXML
    private void handleAffichageListe() {
        if (tableauDocuments != null) {
            tableauDocuments.setVisible(true);
            tableauDocuments.setManaged(true);
        }
        if (vueGrille != null) {
            vueGrille.setVisible(false);
            vueGrille.setManaged(false);
        }
    }
    
    /**
     * Gère l'affichage en mode grille
     */
    @FXML
    private void handleAffichageGrille() {
        if (tableauDocuments != null) {
            tableauDocuments.setVisible(false);
            tableauDocuments.setManaged(false);
        }
        if (vueGrille != null) {
            vueGrille.setVisible(true);
            vueGrille.setManaged(true);
        }
        // TODO: Peupler la grille avec les documents
    }
    
    /**
     * Gère l'affichage en mode détails
     */
    @FXML
    private void handleAffichageDetails() {
        AlertUtils.showInfo("Vue détaillée en cours de développement");
    }
    
    /**
     * Gère le rafraîchissement de l'arbre
     */
    @FXML
    private void handleRafraichirArbre() {
        loadArborescenceDossiers();
    }
    
    /**
     * Gère la création d'un sous-dossier
     */
    @FXML
    private void handleCreerSousDossier() {
        if (dossierActuel == null) {
            AlertUtils.showWarning("Veuillez sélectionner un dossier parent");
            return;
        }
        handleNouveauDossier();
    }
    
    /**
     * Gère l'actualisation de la liste
     */
    @FXML
    private void handleActualiser() {
        if (dossierActuel != null) {
            loadDocumentsDossier(dossierActuel.getId());
        } else {
            loadDocuments();
        }
    }
    
    /**
     * Gère le changement du nombre d'éléments par page
     */
    @FXML
    private void handleElementsParPageChange() {
        if (comboElementsParPage != null && comboElementsParPage.getValue() != null) {
            elementsPerPage = Integer.parseInt(comboElementsParPage.getValue());
            currentPage = 1;
            updatePaginationAndDisplay();
        }
    }
    
    /**
     * Gère la navigation vers la page précédente
     */
    @FXML
    private void handlePagePrecedente() {
        if (currentPage > 1) {
            currentPage--;
            updatePaginationAndDisplay();
        }
    }
    
    /**
     * Gère la navigation vers la page suivante
     */
    @FXML
    private void handlePageSuivante() {
        if (currentPage < totalPages) {
            currentPage++;
            updatePaginationAndDisplay();
        }
    }
    
    /**
     * Gère le raccourci Documents favoris
     */
    @FXML
    private void handleRaccourciFavoris(MouseEvent event) {
        AlertUtils.showInfo("Affichage des documents favoris");
        // TODO: Implémenter le filtre favoris
    }
    
    /**
     * Gère le raccourci Récemment modifiés
     */
    @FXML
    private void handleRaccourciRecents(MouseEvent event) {
        // Trier par date de modification
        comboTriDocuments.setValue("Date modification");
        handleTriChange();
    }
    
    /**
     * Gère le raccourci Corbeille
     */
    @FXML
    private void handleRaccourciCorbeille(MouseEvent event) {
        Dossier corbeille = dossierService.getDossierByCode("CORBEILLE");
        if (corbeille != null) {
            loadDocumentsDossier(corbeille.getId());
        }
    }
    
    /**
     * Gère le raccourci Partagés avec moi
     */
    @FXML
    private void handleRaccourciPartages(MouseEvent event) {
        AlertUtils.showInfo("Affichage des documents partagés");
        // TODO: Implémenter le filtre partages
    }
    
    /**
     * Gère l'aperçu complet d'un document
     */
    @FXML
    private void handleApercuComplet() {
        if (selectedDocument == null) {
            AlertUtils.showWarning("Aucun document sélectionné");
            return;
        }
        AlertUtils.showInfo("Aperçu complet en cours de développement");
    }
    
    /**
     * Gère la création d'une nouvelle version
     */
    @FXML
    private void handleNouvelleVersion() {
        handleModifierDocument();
    }
    
    /**
     * Navigation vers un dossier
     */
    private void naviguerVersDossier(Dossier dossier) {
        if (dossier == null) {
            // Retour à la racine
            dossierActuel = null;
            loadDocuments();
        } else {
            dossierActuel = dossier;
            loadDocumentsDossier(dossier.getId());
        }
        updateBreadcrumbs();
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
}