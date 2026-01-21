package application.controllers;

import application.models.Courrier;
import application.models.Courrier.StatutCourrier;
import application.models.Courrier.TypeCourrier;
import application.models.Courrier.PrioriteCourrier;
import application.models.Document;
import application.models.User;
import application.services.CourrierService;
import application.services.DocumentService;
import application.services.LogService;
import application.utils.SessionManager;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.HBox;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

/**
 * Contrôleur pour la gestion des courriers
 * 
 * Workflow:
 * 1. Création courrier (NOUVEAU) avec document obligatoire
 * 2. Traitement externe → statut EN_COURS puis TRAITE
 * 3. Quand TRAITE → document archivé automatiquement
 */
public class CourrierController {
    
    // ==================== COMPOSANTS FXML ====================
    
    @FXML private TableView<Courrier> tableauCourriers;
    @FXML private TableColumn<Courrier, String> colonneCode;
    @FXML private TableColumn<Courrier, String> colonneType;
    @FXML private TableColumn<Courrier, String> colonneObjet;
    @FXML private TableColumn<Courrier, String> colonneExpediteur;
    @FXML private TableColumn<Courrier, String> colonneDestinataire;
    @FXML private TableColumn<Courrier, String> colonneDateCourrier;
    @FXML private TableColumn<Courrier, String> colonnePriorite;
    @FXML private TableColumn<Courrier, String> colonneStatut;
    @FXML private TableColumn<Courrier, String> colonneActions;
    
    @FXML private Button btnNouveauCourrier;
    @FXML private Button btnActualiser;
    @FXML private Button btnRechercher;
    
    @FXML private ComboBox<String> filtreType;
    @FXML private ComboBox<String> filtreStatut;
    @FXML private ComboBox<String> filtrePriorite;
    @FXML private ComboBox<String> comboLimiteCourriers;
    
    @FXML private TextField champRecherche;
    
    @FXML private Label statNouveaux;
    @FXML private Label statEnCours;
    @FXML private Label statTraites;
    @FXML private Label statArchives;
    @FXML private Label labelInfo;
    
    // ==================== SERVICES ====================
    
    private CourrierService courrierService;
    private DocumentService documentService;
    private LogService logService;
    
    private ObservableList<Courrier> courriers;
    private ObservableList<Courrier> courriersFiltrés;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    
    // ==================== INITIALISATION ====================
    
    @FXML
    public void initialize() {
        System.out.println("=== INITIALISATION CourrierController ===");
        
        courrierService = CourrierService.getInstance();
        documentService = DocumentService.getInstance();
        logService = LogService.getInstance();
        
        courriers = FXCollections.observableArrayList();
        courriersFiltrés = FXCollections.observableArrayList();
        
        configurerColonnesTableau();
        configurerFiltres();
        chargerCourriers();
        mettreAJourStatistiques();
        
        System.out.println("=== FIN INITIALISATION ===");
    }
    
    /**
     * Configure les colonnes du tableau
     */
    private void configurerColonnesTableau() {
        colonneCode.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getCodeCourrier()));
        
        colonneType.setCellValueFactory(cellData -> {
            TypeCourrier type = cellData.getValue().getTypeCourrier();
            return new SimpleStringProperty(type != null ? type.getIcone() + " " + type.getLibelle() : "");
        });
        
        colonneObjet.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getObjet()));
        
        colonneExpediteur.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getExpediteur()));
        
        colonneDestinataire.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getDestinataire()));
        
        colonneDateCourrier.setCellValueFactory(cellData -> {
            if (cellData.getValue().getDateCourrier() != null) {
                return new SimpleStringProperty(
                    cellData.getValue().getDateCourrier().format(DATE_FORMATTER));
            }
            return new SimpleStringProperty("");
        });
        
        colonnePriorite.setCellValueFactory(cellData -> {
            PrioriteCourrier priorite = cellData.getValue().getPriorite();
            return new SimpleStringProperty(
                priorite != null ? priorite.getIcone() + " " + priorite.getLibelle() : "");
        });
        
        colonneStatut.setCellValueFactory(cellData -> {
            StatutCourrier statut = cellData.getValue().getStatut();
            return new SimpleStringProperty(
                statut != null ? statut.getIcone() + " " + statut.getLibelle() : "");
        });
        
        colonneActions.setCellFactory(column -> new TableCell<Courrier, String>() {
            private final Button btnVoir = new Button("👁");
            private final Button btnModifier = new Button("✏️");
            private final Button btnArchiver = new Button("📁");
            
            {
                btnVoir.setOnAction(event -> voirCourrier(getTableView().getItems().get(getIndex())));
                btnModifier.setOnAction(event -> changerStatutCourrier(getTableView().getItems().get(getIndex())));
                btnArchiver.setOnAction(event -> archiverCourrier(getTableView().getItems().get(getIndex())));
                
                btnVoir.setStyle("-fx-font-size: 12px; -fx-padding: 4 8;");
                btnModifier.setStyle("-fx-font-size: 12px; -fx-padding: 4 8;");
                btnArchiver.setStyle("-fx-font-size: 12px; -fx-padding: 4 8;");
            }
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Courrier courrier = getTableView().getItems().get(getIndex());
                    HBox buttons = new HBox(5);
                    
                    buttons.getChildren().addAll(btnVoir, btnModifier);
                    
                    // Afficher le bouton d'archivage seulement si le courrier est TRAITE
                    if (courrier.getStatut() == StatutCourrier.TRAITE) {
                        buttons.getChildren().add(btnArchiver);
                    }
                    
                    setGraphic(buttons);
                }
            }
        });
    }
    
    /**
     * Configure les filtres
     */
    private void configurerFiltres() {
        if (filtreType != null) {
            filtreType.setValue("Tous");
            filtreType.setOnAction(e -> appliquerFiltres());
        }
        
        if (filtreStatut != null) {
            filtreStatut.setValue("Tous");
            filtreStatut.setOnAction(e -> appliquerFiltres());
        }
        
        if (filtrePriorite != null) {
            filtrePriorite.setValue("Tous");
            filtrePriorite.setOnAction(e -> appliquerFiltres());
        }
        
        if (comboLimiteCourriers != null) {
            comboLimiteCourriers.setValue("50");
            comboLimiteCourriers.setOnAction(e -> chargerCourriers());
        }
    }
    
    // ==================== CHARGEMENT DES DONNÉES ====================
    
    /**
     * Charge tous les courriers
     */
    private void chargerCourriers() {
        try {
            List<Courrier> listeCourriers = courrierService.rechercherCourriers("");
            
            courriers.clear();
            courriers.addAll(listeCourriers);
            
            appliquerFiltres();
            
            System.out.println("✓ " + listeCourriers.size() + " courrier(s) chargé(s)");
            
        } catch (Exception e) {
            System.err.println("Erreur chargement courriers: " + e.getMessage());
            showError("Erreur lors du chargement des courriers");
        }
    }
    
    /**
     * Applique les filtres
     */
    private void appliquerFiltres() {
        courriersFiltrés.clear();
        
        String typeFiltre = filtreType != null ? filtreType.getValue() : "Tous";
        String statutFiltre = filtreStatut != null ? filtreStatut.getValue() : "Tous";
        String prioriteFiltre = filtrePriorite != null ? filtrePriorite.getValue() : "Tous";
        
        List<Courrier> resultats = courriers.stream()
            .filter(c -> "Tous".equals(typeFiltre) || 
                        c.getTypeCourrier().getLibelle().equals(typeFiltre))
            .filter(c -> "Tous".equals(statutFiltre) || 
                        c.getStatut().getLibelle().equals(statutFiltre))
            .filter(c -> "Tous".equals(prioriteFiltre) || 
                        c.getPriorite().getLibelle().equals(prioriteFiltre))
            .collect(Collectors.toList());
        
        courriersFiltrés.addAll(resultats);
        tableauCourriers.setItems(courriersFiltrés);
        
        if (labelInfo != null) {
            labelInfo.setText(courriersFiltrés.size() + " courrier(s)");
        }
    }
    
    /**
     * Met à jour les statistiques
     */
    private void mettreAJourStatistiques() {
        try {
            int nouveaux = courrierService.getCourriersByStatut(StatutCourrier.NOUVEAU).size();
            int enCours = courrierService.getCourriersByStatut(StatutCourrier.EN_COURS).size();
            int traites = courrierService.getCourriersByStatut(StatutCourrier.TRAITE).size();
            int archives = courrierService.getCourriersByStatut(StatutCourrier.ARCHIVE).size();
            
            if (statNouveaux != null) statNouveaux.setText(String.valueOf(nouveaux));
            if (statEnCours != null) statEnCours.setText(String.valueOf(enCours));
            if (statTraites != null) statTraites.setText(String.valueOf(traites));
            if (statArchives != null) statArchives.setText(String.valueOf(archives));
            
        } catch (Exception e) {
            System.err.println("Erreur mise à jour statistiques: " + e.getMessage());
        }
    }
    
    // ==================== ACTIONS ====================
    
    /**
     * Crée un nouveau courrier
     */
    @FXML
    private void handleNouveauCourrier() {
        System.out.println("Action: Nouveau courrier");
        
        try {
            // 1. Sélectionner un document (OBLIGATOIRE)
            Document document = selectionnerDocument();
            
            if (document == null) {
                showWarning("Un document doit être sélectionné pour créer un courrier");
                return;
            }
            
            // 2. Afficher le dialogue de création
            CourrierFormDialog dialog = new CourrierFormDialog(document);
            Optional<Courrier> result = dialog.showAndWait();
            
            if (result.isPresent()) {
                Courrier courrier = result.get();
                
                // 3. Créer le courrier
                User currentUser = getCurrentUser();
                if (currentUser != null) {
                    courrier.setCreePar(currentUser.getId());
                }
                
                Courrier courrierCree = courrierService.createCourrier(courrier);
                
                // 4. Logger l'action
                logService.logAction("creation_courrier", 
                    "Courrier créé: " + courrierCree.getCodeCourrier() + 
                    " - Document: " + document.getCodeDocument());
                
                showSuccess("✅ Courrier créé avec succès !\n\nCode: " + 
                           courrierCree.getCodeCourrier());
                
                // 5. Rafraîchir la liste
                chargerCourriers();
                mettreAJourStatistiques();
            }
            
        } catch (Exception e) {
            System.err.println("Erreur création courrier: " + e.getMessage());
            e.printStackTrace();
            showError("Erreur lors de la création du courrier:\n" + e.getMessage());
        }
    }
    
    /**
     * Sélectionne un document pour le courrier
     */
    private Document selectionnerDocument() {
        try {
            List<Document> documents = documentService.getAllDocuments();
            
            if (documents.isEmpty()) {
                showWarning("Aucun document disponible.\nVeuillez d'abord créer un document.");
                return null;
            }
            
            // Créer une boîte de dialogue de sélection
            ChoiceDialog<Document> dialog = new ChoiceDialog<>(documents.get(0), documents);
            dialog.setTitle("Sélectionner un document");
            dialog.setHeaderText("Sélectionnez le document à associer au courrier");
            dialog.setContentText("Document:");
            
            // Personnaliser l'affichage des documents
            dialog.getDialogPane().setContent(createDocumentSelector(documents));
            
            Optional<Document> result = dialog.showAndWait();
            return result.orElse(null);
            
        } catch (Exception e) {
            System.err.println("Erreur sélection document: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Crée un sélecteur de document personnalisé
     */
    private javafx.scene.Node createDocumentSelector(List<Document> documents) {
        ListView<Document> listView = new ListView<>();
        listView.getItems().addAll(documents);
        
        listView.setCellFactory(param -> new ListCell<Document>() {
            @Override
            protected void updateItem(Document doc, boolean empty) {
                super.updateItem(doc, empty);
                if (empty || doc == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(String.format("📄 %s - %s (%s)", 
                        doc.getCodeDocument(), 
                        doc.getTitre(),
                        doc.getTailleFormatee()));
                }
            }
        });
        
        listView.setPrefHeight(300);
        return listView;
    }
    
    /**
     * Voir les détails d'un courrier
     */
    private void voirCourrier(Courrier courrier) {
        if (courrier == null) return;
        
        try {
            // Récupérer le document associé
            Document document = documentService.getDocumentById(courrier.getDocumentId());
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Détails du courrier");
            alert.setHeaderText(courrier.getCodeCourrier());
            
            String details = String.format(
                "Type: %s\n" +
                "Objet: %s\n" +
                "Expéditeur: %s\n" +
                "Destinataire: %s\n" +
                "Référence: %s\n" +
                "Date: %s\n" +
                "Priorité: %s\n" +
                "Statut: %s\n" +
                "Confidentiel: %s\n\n" +
                "Document associé:\n" +
                "Code: %s\n" +
                "Titre: %s\n\n" +
                "Observations:\n%s",
                courrier.getTypeCourrier().getLibelle(),
                courrier.getObjet(),
                courrier.getExpediteur() != null ? courrier.getExpediteur() : "-",
                courrier.getDestinataire() != null ? courrier.getDestinataire() : "-",
                courrier.getReference() != null ? courrier.getReference() : "-",
                courrier.getDateCourrier() != null ? 
                    courrier.getDateCourrier().format(DATE_FORMATTER) : "-",
                courrier.getPriorite().getLibelle(),
                courrier.getStatut().getLibelle(),
                courrier.isConfidentiel() ? "Oui" : "Non",
                document != null ? document.getCodeDocument() : "?",
                document != null ? document.getTitre() : "Document introuvable",
                courrier.getObservations() != null ? courrier.getObservations() : "Aucune observation"
            );
            
            alert.setContentText(details);
            alert.showAndWait();
            
            logService.logAction("consultation_courrier", "Courrier consulté: " + courrier.getCodeCourrier());
            
        } catch (Exception e) {
            System.err.println("Erreur affichage détails: " + e.getMessage());
            showError("Erreur lors de l'affichage des détails");
        }
    }
    
    /**
     * Change le statut d'un courrier
     */
    private void changerStatutCourrier(Courrier courrier) {
        if (courrier == null) return;
        
        try {
            // Créer un dialogue de sélection du statut
            ChoiceDialog<StatutCourrier> dialog = new ChoiceDialog<>(
                courrier.getStatut(), 
                StatutCourrier.values()
            );
            
            dialog.setTitle("Changer le statut");
            dialog.setHeaderText("Courrier: " + courrier.getCodeCourrier());
            dialog.setContentText("Nouveau statut:");
            
            Optional<StatutCourrier> result = dialog.showAndWait();
            
            if (result.isPresent()) {
                StatutCourrier nouveauStatut = result.get();
                
                if (nouveauStatut == courrier.getStatut()) {
                    showInfo("Le statut est déjà " + nouveauStatut.getLibelle());
                    return;
                }
                
                // Mettre à jour le statut
                boolean succes = courrierService.updateStatut(courrier.getId(), nouveauStatut);
                
                if (succes) {
                    showSuccess("✅ Statut changé vers: " + nouveauStatut.getLibelle());
                    
                    logService.logAction("changement_statut_courrier", 
                        "Courrier " + courrier.getCodeCourrier() + 
                        " - Statut: " + courrier.getStatut() + " → " + nouveauStatut);
                    
                    // Rafraîchir
                    chargerCourriers();
                    mettreAJourStatistiques();
                } else {
                    showError("Échec du changement de statut");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Erreur changement statut: " + e.getMessage());
            showError("Erreur lors du changement de statut");
        }
    }
    
    /**
     * Archive un courrier (et son document)
     */
    private void archiverCourrier(Courrier courrier) {
        if (courrier == null) return;
        
        // Vérifier que le courrier est au statut TRAITE
        if (courrier.getStatut() != StatutCourrier.TRAITE) {
            showWarning("Le courrier doit être au statut TRAITE pour être archivé");
            return;
        }
        
        try {
            // Sélectionner le dossier d'archivage
            DossierSelectorDialog selectorDialog = new DossierSelectorDialog("Sélectionner le dossier d'archivage");
            Optional<Integer> dossierResult = selectorDialog.showAndWait();
            
            if (dossierResult.isEmpty()) {
                return; // Annulé
            }
            
            int dossierId = dossierResult.get();
            
            // Confirmer l'archivage
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation");
            confirm.setHeaderText("Archiver le courrier");
            confirm.setContentText("Cette action va archiver le courrier ET son document associé.\n\n" +
                                  "Courrier: " + courrier.getCodeCourrier() + "\n" +
                                  "Confirmer l'archivage ?");
            
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                User currentUser = getCurrentUser();
                
                boolean succes = courrierService.archiverCourrier(
                    courrier.getId(), 
                    dossierId, 
                    currentUser != null ? currentUser.getId() : 0
                );
                
                if (succes) {
                    showSuccess("✅ Courrier et document archivés avec succès !");
                    
                    logService.logAction("archivage_courrier", 
                        "Courrier archivé: " + courrier.getCodeCourrier() + 
                        " - Dossier ID: " + dossierId);
                    
                    // Rafraîchir
                    chargerCourriers();
                    mettreAJourStatistiques();
                } else {
                    showError("Échec de l'archivage");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Erreur archivage: " + e.getMessage());
            e.printStackTrace();
            showError("Erreur lors de l'archivage:\n" + e.getMessage());
        }
    }
    
    /**
     * Recherche de courriers
     */
    @FXML
    private void handleRechercher() {
        String recherche = champRecherche != null ? champRecherche.getText().trim() : "";
        
        if (recherche.isEmpty()) {
            chargerCourriers();
            return;
        }
        
        try {
            List<Courrier> resultats = courrierService.rechercherCourriers(recherche);
            
            courriers.clear();
            courriers.addAll(resultats);
            
            appliquerFiltres();
            
            showInfo(resultats.size() + " courrier(s) trouvé(s)");
            
        } catch (Exception e) {
            System.err.println("Erreur recherche: " + e.getMessage());
            showError("Erreur lors de la recherche");
        }
    }
    
    /**
     * Actualise la liste
     */
    @FXML
    private void handleActualiser() {
        chargerCourriers();
        mettreAJourStatistiques();
        showInfo("Liste actualisée");
    }
    
    // ==================== MÉTHODES UTILITAIRES ====================
    
    private User getCurrentUser() {
        return SessionManager.getInstance().getCurrentUser();
    }
    
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Attention");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}