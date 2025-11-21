package application.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import application.models.Document;
import application.models.User;
import application.services.DocumentService;
import application.utils.SessionManager;
import application.utils.AlertUtils;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class DocumentsController implements Initializable {
    
    // Filtres et recherche
    @FXML private ComboBox<String> filtreType;
    @FXML private ComboBox<String> filtreStatutDoc;
    @FXML private TextField champRechercheDoc;
    
    // Navigation
    @FXML private Hyperlink breadcrumbRoot;
    @FXML private TreeView<String> arborescenceDossiers;
    
    // Affichage
    @FXML private ToggleGroup toggleAffichage;
    @FXML private ToggleButton btnAffichageListe;
    @FXML private ToggleButton btnAffichageGrille;
    @FXML private ToggleButton btnAffichageDetails;
    @FXML private TableView<Document> tableauDocuments;
    @FXML private ScrollPane vueGrille;
    @FXML private FlowPane grilleDocuments;
    
    // Colonnes du tableau
    @FXML private TableColumn<Document, String> colonneNomDocument;
    @FXML private TableColumn<Document, String> colonneTypeDocument;
    @FXML private TableColumn<Document, String> colonneTailleDocument;
    @FXML private TableColumn<Document, String> colonneAuteurDocument;
    @FXML private TableColumn<Document, String> colonneDateModification;
    @FXML private TableColumn<Document, String> colonneStatutDocument;
    
    // Tri et pagination
    @FXML private ComboBox<String> comboTriDocuments;
    @FXML private Label labelNombreDocuments;
    @FXML private Label labelDossierActuel;
    
    // Détails du document
    @FXML private VBox panneauDetailsDocument;
    @FXML private VBox zoneApercu;
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
    
    // Boutons d'action
    @FXML private Button btnApercuComplet;
    @FXML private Button btnTelecharger;
    @FXML private Button btnModifierDocument;
    @FXML private Button btnPartagerDocument;
    @FXML private Button btnDeplacerDocument;
    @FXML private Button btnSupprimerDocument;
    @FXML private Button btnFermerDetails;
    
    private User currentUser;
    private DocumentService documentService;
    private ObservableList<Document> documents;
    private Document selectedDocument;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("DocumentsController.initialize() appelé");
        
        try {
            currentUser = SessionManager.getInstance().getCurrentUser();
            documentService = DocumentService.getInstance();
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
            loadDocuments();
            
        } catch (Exception e) {
            System.err.println("Erreur dans DocumentsController.initialize(): " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupInterface() {
        try {
            // Configuration de l'affichage par défaut
            if (toggleAffichage != null && btnAffichageListe != null) {
                btnAffichageListe.setSelected(true);
                toggleAffichage.selectedToggleProperty().addListener((obs, oldToggle, newToggle) -> {
                    if (newToggle != null) {
                        handleAffichageChange();
                    }
                });
            }
            
            // Configuration de la recherche
            if (champRechercheDoc != null) {
                champRechercheDoc.setOnAction(e -> handleRecherche());
            }
            
            // Configuration du tri
            if (comboTriDocuments != null) {
                comboTriDocuments.setItems(FXCollections.observableArrayList(
                    "Nom", "Date modification", "Date création", "Taille", "Type"
                ));
                comboTriDocuments.setValue("Nom");
                comboTriDocuments.setOnAction(e -> handleTriChange());
            }
            
            // Masquer les détails par défaut
            hideDocumentDetails();
            
            System.out.println("Interface des documents configurée");
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la configuration de l'interface: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupTableColumns() {
        colonneNomDocument.setCellValueFactory(new PropertyValueFactory<>("titre"));
        colonneTypeDocument.setCellValueFactory(new PropertyValueFactory<>("extension"));
        colonneTailleDocument.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getTailleFormatee()
            )
        );
        colonneAuteurDocument.setCellValueFactory(cellData -> {
            if (cellData.getValue().getCreePar() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                    cellData.getValue().getCreePar().getNomComplet()
                );
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });
        colonneDateModification.setCellValueFactory(cellData -> {
            if (cellData.getValue().getDateModification() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                return new javafx.beans.property.SimpleStringProperty(
                    cellData.getValue().getDateModification().format(formatter)
                );
            } else if (cellData.getValue().getDateCreation() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                return new javafx.beans.property.SimpleStringProperty(
                    cellData.getValue().getDateCreation().format(formatter)
                );
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });
        colonneStatutDocument.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getStatut().getLibelle()
            )
        );
        
        // Listener pour la sélection
        tableauDocuments.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    showDocumentDetails(newSelection);
                }
            }
        );
    }
    
    private void setupFilters() {
        if (filtreType != null) {
            filtreType.setItems(FXCollections.observableArrayList(
                "Tous", "PDF", "Word", "Excel", "PowerPoint", "Images", "Archives"
            ));
            filtreType.setValue("Tous");
            filtreType.setOnAction(e -> applyFilters());
        }
        
        if (filtreStatutDoc != null) {
            filtreStatutDoc.setItems(FXCollections.observableArrayList(
                "Tous", "Brouillon", "En révision", "Validé", "Archivé"
            ));
            filtreStatutDoc.setValue("Tous");
            filtreStatutDoc.setOnAction(e -> applyFilters());
        }
    }
    
    private void setupButtons() {
        if (btnApercuComplet != null) {
            btnApercuComplet.setOnAction(e -> handleApercuComplet());
        }
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
            btnFermerDetails.setOnAction(e -> handleFermerDetails());
        }
    }
    
    private void loadDocuments() {
        try {
            List<Document> list = documentService.getAllDocuments();
            documents.clear();
            documents.addAll(list);
            tableauDocuments.setItems(documents);
            
            if (labelNombreDocuments != null) {
                labelNombreDocuments.setText("(" + documents.size() + " documents)");
            }
            
            System.out.println("Documents chargés: " + documents.size());
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des documents: " + e.getMessage());
            AlertUtils.showError("Erreur lors du chargement des documents");
        }
    }
    
    @FXML
    private void handleRecherche() {
        applyFilters();
    }
    
    private void applyFilters() {
        try {
            String typeFilter = filtreType.getValue();
            String statutFilter = filtreStatutDoc.getValue();
            String searchText = champRechercheDoc != null ? champRechercheDoc.getText().toLowerCase() : "";
            
            List<Document> allDocuments = documentService.getAllDocuments();
            ObservableList<Document> filtered = FXCollections.observableArrayList();
            
            for (Document d : allDocuments) {
                boolean matches = true;
                
                // Filtre type
                if (!typeFilter.equals("Tous")) {
                    String ext = d.getExtension() != null ? d.getExtension().toLowerCase() : "";
                    boolean typeMatch = false;
                    
                    switch (typeFilter) {
                        case "PDF":
                            typeMatch = ext.equals("pdf");
                            break;
                        case "Word":
                            typeMatch = ext.equals("doc") || ext.equals("docx");
                            break;
                        case "Excel":
                            typeMatch = ext.equals("xls") || ext.equals("xlsx");
                            break;
                        case "PowerPoint":
                            typeMatch = ext.equals("ppt") || ext.equals("pptx");
                            break;
                        case "Images":
                            typeMatch = ext.equals("jpg") || ext.equals("jpeg") || ext.equals("png") || ext.equals("gif");
                            break;
                        case "Archives":
                            typeMatch = ext.equals("zip") || ext.equals("rar");
                            break;
                    }
                    
                    if (!typeMatch) {
                        matches = false;
                    }
                }
                
                // Recherche textuelle
                if (!searchText.isEmpty()) {
                    boolean textMatch = d.getTitre().toLowerCase().contains(searchText) ||
                                      (d.getDescription() != null && d.getDescription().toLowerCase().contains(searchText)) ||
                                      (d.getMotsCles() != null && d.getMotsCles().toLowerCase().contains(searchText));
                    if (!textMatch) {
                        matches = false;
                    }
                }
                
                if (matches) {
                    filtered.add(d);
                }
            }
            
            documents.clear();
            documents.addAll(filtered);
            
            if (labelNombreDocuments != null) {
                labelNombreDocuments.setText("(" + documents.size() + " documents)");
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'application des filtres: " + e.getMessage());
        }
    }
    
    private void handleAffichageChange() {
        try {
            if (btnAffichageListe != null && btnAffichageListe.isSelected()) {
                tableauDocuments.setVisible(true);
                tableauDocuments.setManaged(true);
                if (vueGrille != null) {
                    vueGrille.setVisible(false);
                    vueGrille.setManaged(false);
                }
            } else if (btnAffichageGrille != null && btnAffichageGrille.isSelected()) {
                tableauDocuments.setVisible(false);
                tableauDocuments.setManaged(false);
                if (vueGrille != null) {
                    vueGrille.setVisible(true);
                    vueGrille.setManaged(true);
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du changement d'affichage: " + e.getMessage());
        }
    }
    
    private void handleTriChange() {
        try {
            String tri = comboTriDocuments.getValue();
            System.out.println("Changement de tri: " + tri);
            
            // Trier la liste selon le critère
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
                        String ext1 = d1.getExtension() != null ? d1.getExtension() : "";
                        String ext2 = d2.getExtension() != null ? d2.getExtension() : "";
                        return ext1.compareToIgnoreCase(ext2);
                    });
                    break;
            }
            
            tableauDocuments.refresh();
            
        } catch (Exception e) {
            System.err.println("Erreur lors du changement de tri: " + e.getMessage());
        }
    }
    
    private void showDocumentDetails(Document document) {
        selectedDocument = document;
        
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
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");
            labelDateCreation.setText(document.getDateCreation().format(formatter));
        }
        
        if (labelDateModification != null) {
            if (document.getDateModification() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");
                labelDateModification.setText(document.getDateModification().format(formatter));
            } else if (document.getDateCreation() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");
                labelDateModification.setText(document.getDateCreation().format(formatter));
            }
        }
        
        if (labelAuteur != null && document.getCreePar() != null) {
            labelAuteur.setText(document.getCreePar().getNomComplet());
        }
        
        if (labelStatutDocument != null) {
            labelStatutDocument.setText(getStatutIcon(document.getStatut()) + " " + document.getStatut().getLibelle());
        }
        
        if (textAreaDescription != null) {
            textAreaDescription.setText(document.getDescription() != null ? document.getDescription() : "");
        }
        
        // Afficher les mots-clés
        if (flowPaneMotsCles != null && document.getMotsCles() != null) {
            flowPaneMotsCles.getChildren().clear();
            String[] mots = document.getMotsCles().split(",");
            for (String mot : mots) {
                Label tag = new Label(mot.trim());
                tag.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; " +
                           "-fx-padding: 2 6; -fx-background-radius: 10; -fx-font-size: 11px;");
                flowPaneMotsCles.getChildren().add(tag);
            }
        }
    }
    
    private String getStatutIcon(application.models.StatutDocument statut) {
        switch (statut) {
            case ACTIF: return "✅";
            case ARCHIVE: return "📁";
            case BROUILLON: return "📝";
            case EN_COURS: return "🔄";
            case VALIDE: return "✔️";
            case EXPIRE: return "⏰";
            case SUSPENDU: return "⏸️";
            default: return "";
        }
    }
    
    @FXML
    private void handleApercuComplet() {
        if (selectedDocument == null) {
            AlertUtils.showWarning("Veuillez sélectionner un document");
            return;
        }
        AlertUtils.showInfo("Fonction d'aperçu en cours de développement");
    }
    
    @FXML
    private void handleTelecharger() {
        if (selectedDocument == null) {
            AlertUtils.showWarning("Veuillez sélectionner un document");
            return;
        }
        AlertUtils.showInfo("Téléchargement de: " + selectedDocument.getTitre());
    }
    
    @FXML
    private void handleModifierDocument() {
        if (selectedDocument == null) {
            AlertUtils.showWarning("Veuillez sélectionner un document");
            return;
        }
        AlertUtils.showInfo("Fonction de modification en cours de développement");
    }
    
    @FXML
    private void handlePartagerDocument() {
        if (selectedDocument == null) {
            AlertUtils.showWarning("Veuillez sélectionner un document");
            return;
        }
        AlertUtils.showInfo("Fonction de partage en cours de développement");
    }
    
    @FXML
    private void handleDeplacerDocument() {
        if (selectedDocument == null) {
            AlertUtils.showWarning("Veuillez sélectionner un document");
            return;
        }
        AlertUtils.showInfo("Fonction de déplacement en cours de développement");
    }
    
    @FXML
    private void handleSupprimerDocument() {
        if (selectedDocument == null) {
            AlertUtils.showWarning("Veuillez sélectionner un document");
            return;
        }
        
        boolean confirm = AlertUtils.showConfirmation(
            "Confirmation",
            "Êtes-vous sûr de vouloir supprimer ce document ?"
        );
        
        if (confirm) {
            if (documentService.deleteDocument(selectedDocument.getId())) {
                AlertUtils.showInfo("Document supprimé avec succès");
                loadDocuments();
            } else {
                AlertUtils.showError("Erreur lors de la suppression");
            }
        }
    }
    
    @FXML
    private void handleFermerDetails() {
        hideDocumentDetails();
    }
    
    private void hideDocumentDetails() {
        selectedDocument = null;
        if (labelNomFichier != null) labelNomFichier.setText("");
        if (textAreaDescription != null) textAreaDescription.setText("");
    }
}