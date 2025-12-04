package application.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.print.*;
import javafx.scene.Node;
import javafx.scene.text.Text;
import application.models.*;
import application.services.*;
import application.utils.*;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Contrôleur amélioré pour la gestion des courriers
 * Avec toutes les fonctionnalités: Enregistrer, Filtrer, Actualiser, Traiter, Archiver, Transférer, Coter, Imprimer
 */
public class CourrierController implements Initializable {
    
    // Filtres
    @FXML private ComboBox<String> filtreStatut;
    @FXML private ComboBox<String> filtrePriorite;
    @FXML private DatePicker dateDebut;
    @FXML private DatePicker dateFin;
    @FXML private TextField champRecherche;
    
    // Tableau
    @FXML private TableView<Courrier> tableauCourriers;
    @FXML private TableColumn<Courrier, String> colonneNumero;
    @FXML private TableColumn<Courrier, String> colonneType;
    @FXML private TableColumn<Courrier, String> colonneObjet;
    @FXML private TableColumn<Courrier, String> colonneExpediteur;
    @FXML private TableColumn<Courrier, String> colonneDate;
    @FXML private TableColumn<Courrier, String> colonnePriorite;
    @FXML private TableColumn<Courrier, String> colonneStatut;
    
    // Détails
    @FXML private VBox panneauDetails;
    @FXML private Label labelNumero;
    @FXML private Label labelType;
    @FXML private Label labelObjet;
    @FXML private Label labelExpediteur;
    @FXML private Label labelDate;
    @FXML private Label labelPriorite;
    @FXML private Label labelStatut;
    @FXML private Label labelTraitePar;
    @FXML private Label labelDateDebut;
    @FXML private Label labelEcheance;
    @FXML private TextArea textAreaNotes;
    @FXML private VBox listePiecesJointes;
    
    // Boutons d'action
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;
    @FXML private Button btnMarquerTraite;
    @FXML private Button btnCoter;
    @FXML private Button btnArchiver;
    @FXML private Button btnTransferer;
    @FXML private Button btnImprimer;
    @FXML private Label nombreCourriers;
    
    private User currentUser;
    private CourrierService courrierService;
    private WorkflowService workflowService;
    private ObservableList<Courrier> courriers;
    private Courrier selectedCourrier;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("CourrierControllerAmeliore.initialize() appelé");
        
        try {
            currentUser = SessionManager.getInstance().getCurrentUser();
            courrierService = CourrierService.getInstance();
            workflowService = WorkflowService.getInstance();
            courriers = FXCollections.observableArrayList();
            
            if (currentUser == null) {
                System.err.println("ERREUR: Aucun utilisateur en session");
                return;
            }
            
            setupTableColumns();
            setupFilters();
            setupButtons();
            loadCourriers();
            
        } catch (Exception e) {
            System.err.println("Erreur dans CourrierControllerAmeliore.initialize(): " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupTableColumns() {
        colonneNumero.setCellValueFactory(new PropertyValueFactory<>("numeroCourrier"));
        colonneType.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getTypeCourrier().getIcone() + " " +
                cellData.getValue().getTypeCourrier().getLibelle()
            )
        );
        colonneObjet.setCellValueFactory(new PropertyValueFactory<>("objet"));
        colonneExpediteur.setCellValueFactory(new PropertyValueFactory<>("expediteur"));
        colonneDate.setCellValueFactory(cellData -> {
            if (cellData.getValue().getDateReception() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                return new javafx.beans.property.SimpleStringProperty(
                    cellData.getValue().getDateReception().format(formatter)
                );
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });
        colonnePriorite.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getPrioriteIcone() + " " + cellData.getValue().getPriorite()
            )
        );
        colonneStatut.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getStatutIcone() + " " + cellData.getValue().getStatut().getLibelle()
            )
        );
        
        // Listener pour la sélection
        tableauCourriers.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    showCourrierDetails(newSelection);
                }
            }
        );
    }
    
    private void setupFilters() {
        // Initialiser les ComboBox
        filtreStatut.setItems(FXCollections.observableArrayList(
            "Tous", "Nouveau", "En attente", "En cours", "Traité", "Archivé"
        ));
        filtreStatut.setValue("Tous");
        
        filtrePriorite.setItems(FXCollections.observableArrayList(
            "Toutes", "Urgente", "Haute", "Normale", "Basse"
        ));
        filtrePriorite.setValue("Toutes");
        
        // Listeners pour filtrage automatique
        filtreStatut.setOnAction(e -> applyFilters());
        filtrePriorite.setOnAction(e -> applyFilters());
    }
    
    private void setupButtons() {
        if (btnModifier != null) btnModifier.setOnAction(e -> handleModifier());
        if (btnSupprimer != null) btnSupprimer.setOnAction(e -> handleSupprimer());
        if (btnMarquerTraite != null) btnMarquerTraite.setOnAction(e -> handleMarquerTraite());
        if (btnCoter != null) btnCoter.setOnAction(e -> handleCoter());
        if (btnArchiver != null) btnArchiver.setOnAction(e -> handleArchiver());
        if (btnTransferer != null) btnTransferer.setOnAction(e -> handleTransferer());
        if (btnImprimer != null) btnImprimer.setOnAction(e -> handleImprimer());
    }
    
    private void loadCourriers() {
        try {
            List<Courrier> list = courrierService.getAllCourriers();
            courriers.clear();
            courriers.addAll(list);
            tableauCourriers.setItems(courriers);
            
            if (nombreCourriers != null) {
                nombreCourriers.setText("(" + courriers.size() + " courriers)");
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des courriers: " + e.getMessage());
            AlertUtils.showError("Erreur lors du chargement des courriers");
        }
    }
    
    @FXML
    private void handleNouveauCourrier() {
        System.out.println("Action: Nouveau courrier");
        
        CourrierFormDialog dialog = new CourrierFormDialog(null);
        Optional<Courrier> result = dialog.showAndWait();
        
        result.ifPresent(courrier -> {
            // Sauvegarder le courrier
            boolean saved = courrierService.saveCourrier(courrier);
            
            if (saved) {
                // Si workflow activé, démarrer le workflow
                if (dialog.isDemarrerWorkflow() && dialog.getServiceDestinataire() != null) {
                    boolean workflowStarted = workflowService.startWorkflow(
                        courrier, 
                        dialog.getServiceDestinataire().getServiceCode()
                    );
                    
                    if (workflowStarted) {
                        AlertUtils.showInfo("Courrier enregistré et workflow démarré avec succès!");
                    } else {
                        AlertUtils.showWarning("Courrier enregistré mais erreur lors du démarrage du workflow");
                    }
                } else {
                    AlertUtils.showInfo("Courrier enregistré avec succès!");
                }
                
                loadCourriers();
            } else {
                AlertUtils.showError("Erreur lors de l'enregistrement du courrier");
            }
        });
    }
    
    @FXML
    private void handleCourrierEntrant() {
        System.out.println("Action: Courrier entrant");
        
        CourrierFormDialog dialog = new CourrierFormDialog(null);
        Optional<Courrier> result = dialog.showAndWait();
        
        result.ifPresent(courrier -> {
            courrier.setTypeCourrier(TypeCourrier.ENTRANT);
            boolean saved = courrierService.saveCourrier(courrier);
            
            if (saved) {
                if (dialog.isDemarrerWorkflow() && dialog.getServiceDestinataire() != null) {
                    workflowService.startWorkflow(courrier, dialog.getServiceDestinataire().getServiceCode());
                }
                AlertUtils.showInfo("Courrier entrant enregistré avec succès!");
                loadCourriers();
            }
        });
    }
    
    @FXML
    private void handleCourrierSortant() {
        System.out.println("Action: Courrier sortant");
        
        CourrierFormDialog dialog = new CourrierFormDialog(null);
        Optional<Courrier> result = dialog.showAndWait();
        
        result.ifPresent(courrier -> {
            courrier.setTypeCourrier(TypeCourrier.SORTANT);
            boolean saved = courrierService.saveCourrier(courrier);
            
            if (saved) {
                AlertUtils.showInfo("Courrier sortant enregistré avec succès!");
                loadCourriers();
            }
        });
    }
    
    @FXML
    private void handleRecherche() {
        applyFilters();
    }
    
    @FXML
    private void handleActualiser() {
        System.out.println("Action: Actualiser");
        loadCourriers();
        AlertUtils.showInfo("Liste des courriers actualisée");
    }
    
    private void applyFilters() {
        try {
            String statutFilter = filtreStatut.getValue();
            String prioriteFilter = filtrePriorite.getValue();
            String searchText = champRecherche != null ? champRecherche.getText().toLowerCase() : "";
            LocalDate debut = dateDebut != null ? dateDebut.getValue() : null;
            LocalDate fin = dateFin != null ? dateFin.getValue() : null;
            
            List<Courrier> allCourriers = courrierService.getAllCourriers();
            List<Courrier> filtered = allCourriers.stream()
                .filter(c -> {
                    // Filtre statut
                    if (!statutFilter.equals("Tous")) {
                        if (!c.getStatut().getLibelle().equals(statutFilter)) {
                            return false;
                        }
                    }
                    
                    // Filtre priorité
                    if (!prioriteFilter.equals("Toutes")) {
                        if (!c.getPriorite().equalsIgnoreCase(prioriteFilter)) {
                            return false;
                        }
                    }
                    
                    // Filtre date
                    if (debut != null && c.getDateReception() != null) {
                        if (c.getDateReception().toLocalDate().isBefore(debut)) {
                            return false;
                        }
                    }
                    
                    if (fin != null && c.getDateReception() != null) {
                        if (c.getDateReception().toLocalDate().isAfter(fin)) {
                            return false;
                        }
                    }
                    
                    // Recherche textuelle
                    if (!searchText.isEmpty()) {
                        boolean textMatch = c.getObjet().toLowerCase().contains(searchText) ||
                                          (c.getExpediteur() != null && c.getExpediteur().toLowerCase().contains(searchText)) ||
                                          (c.getNumeroCourrier() != null && c.getNumeroCourrier().toLowerCase().contains(searchText));
                        if (!textMatch) {
                            return false;
                        }
                    }
                    
                    return true;
                })
                .collect(Collectors.toList());
            
            courriers.clear();
            courriers.addAll(filtered);
            
            if (nombreCourriers != null) {
                nombreCourriers.setText("(" + courriers.size() + " courriers)");
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'application des filtres: " + e.getMessage());
        }
    }
    
    private void showCourrierDetails(Courrier courrier) {
        selectedCourrier = courrier;
        
        if (labelNumero != null) labelNumero.setText(courrier.getNumeroCourrier());
        if (labelType != null) labelType.setText(courrier.getTypeCourrier().getIcone() + " " + courrier.getTypeCourrier().getLibelle());
        if (labelObjet != null) labelObjet.setText(courrier.getObjet());
        if (labelExpediteur != null) labelExpediteur.setText(courrier.getExpediteur());
        
        if (labelDate != null && courrier.getDateReception() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            labelDate.setText(courrier.getDateReception().format(formatter));
        }
        
        if (labelPriorite != null) {
            labelPriorite.setText(courrier.getPrioriteIcone() + " " + courrier.getPriorite());
        }
        
        if (labelStatut != null) {
            labelStatut.setText(courrier.getStatutIcone() + " " + courrier.getStatut().getLibelle());
        }
        
        if (textAreaNotes != null) {
            textAreaNotes.setText(courrier.getNotes() != null ? courrier.getNotes() : "");
        }
    }
    
    @FXML
    private void handleModifier() {
        if (selectedCourrier == null) {
            AlertUtils.showWarning("Veuillez sélectionner un courrier");
            return;
        }
        
        CourrierFormDialog dialog = new CourrierFormDialog(selectedCourrier);
        Optional<Courrier> result = dialog.showAndWait();
        
        result.ifPresent(courrier -> {
            boolean updated = courrierService.updateCourrier(courrier);
            
            if (updated) {
                AlertUtils.showInfo("Courrier modifié avec succès");
                loadCourriers();
            } else {
                AlertUtils.showError("Erreur lors de la modification");
            }
        });
    }
    
    @FXML
    private void handleSupprimer() {
        if (selectedCourrier == null) {
            AlertUtils.showWarning("Veuillez sélectionner un courrier");
            return;
        }
        
        boolean confirm = AlertUtils.showConfirmation(
            "Confirmation",
            "Êtes-vous sûr de vouloir supprimer ce courrier ?"
        );
        
        if (confirm) {
            if (courrierService.deleteCourrier(selectedCourrier.getId())) {
                AlertUtils.showInfo("Courrier supprimé avec succès");
                loadCourriers();
            } else {
                AlertUtils.showError("Erreur lors de la suppression");
            }
        }
    }
    
    @FXML
    private void handleMarquerTraite() {
        if (selectedCourrier == null) {
            AlertUtils.showWarning("Veuillez sélectionner un courrier");
            return;
        }
        
        TextInputDialog dialog = new TextInputDialog();
        dialog.setTitle("Marquer comme traité");
        dialog.setHeaderText("Marquer le courrier comme traité");
        dialog.setContentText("Commentaire final:");
        
        Optional<String> result = dialog.showAndWait();
        
        result.ifPresent(commentaire -> {
            selectedCourrier.setStatut(StatutCourrier.TRAITE);
            selectedCourrier.setDateTraitement(java.time.LocalDateTime.now());
            
            if (selectedCourrier.estEnWorkflow()) {
                workflowService.terminerWorkflow(selectedCourrier, currentUser, commentaire);
            }
            
            if (courrierService.updateCourrier(selectedCourrier)) {
                AlertUtils.showInfo("Courrier marqué comme traité");
                loadCourriers();
            } else {
                AlertUtils.showError("Erreur lors de la mise à jour");
            }
        });
    }
    
    @FXML
    private void handleCoter() {
        if (selectedCourrier == null) {
            AlertUtils.showWarning("Veuillez sélectionner un courrier");
            return;
        }
        
        CoterCourrierDialog dialog = new CoterCourrierDialog(selectedCourrier);
        Optional<CotationInfo> result = dialog.showAndWait();
        
        result.ifPresent(cotation -> {
            // Créer une étape de workflow pour la cotation
            String commentaire = "Coté à " + cotation.getUtilisateur().getNomComplet() + "\n" +
                                cotation.getCommentaire();
            
            // Si le courrier n'est pas en workflow, le démarrer
            if (!selectedCourrier.estEnWorkflow()) {
                String serviceCode = cotation.getUtilisateur().getServiceCode();
                if (serviceCode != null) {
                    workflowService.startWorkflow(selectedCourrier, serviceCode);
                }
            }
            
            // Transférer vers le service de l'utilisateur
            String serviceCode = cotation.getUtilisateur().getServiceCode();
            if (serviceCode != null) {
                boolean transferred = workflowService.transferCourrier(
                    selectedCourrier,
                    currentUser,
                    serviceCode,
                    commentaire,
                    cotation.getDateEcheance()
                );
                
                if (transferred) {
                    AlertUtils.showInfo("Courrier coté à " + cotation.getUtilisateur().getNomComplet());
                    loadCourriers();
                } else {
                    AlertUtils.showError("Erreur lors de la cotation");
                }
            } else {
                AlertUtils.showWarning("L'utilisateur sélectionné n'a pas de service assigné");
            }
        });
    }
    
    @FXML
    private void handleArchiver() {
        if (selectedCourrier == null) {
            AlertUtils.showWarning("Veuillez sélectionner un courrier");
            return;
        }
        
        boolean confirm = AlertUtils.showConfirmation(
            "Confirmation",
            "Êtes-vous sûr de vouloir archiver ce courrier ?"
        );
        
        if (confirm) {
            selectedCourrier.setStatut(StatutCourrier.ARCHIVE);
            selectedCourrier.setWorkflowActif(false);
            
            if (courrierService.updateCourrier(selectedCourrier)) {
                AlertUtils.showInfo("Courrier archivé avec succès");
                loadCourriers();
            } else {
                AlertUtils.showError("Erreur lors de l'archivage");
            }
        }
    }
    
    @FXML
    private void handleTransferer() {
        if (selectedCourrier == null) {
            AlertUtils.showWarning("Veuillez sélectionner un courrier");
            return;
        }
        
        TransfertCourrierDialog dialog = new TransfertCourrierDialog(selectedCourrier);
        Optional<TransfertInfo> result = dialog.showAndWait();
        
        result.ifPresent(transfert -> {
            // Démarrer le workflow si pas encore actif
            if (!selectedCourrier.estEnWorkflow()) {
                workflowService.startWorkflow(
                    selectedCourrier,
                    transfert.getServiceDestination().getServiceCode()
                );
            }
            
            // Transférer le courrier
            boolean transferred = workflowService.transferCourrier(
                selectedCourrier,
                currentUser,
                transfert.getServiceDestination().getServiceCode(),
                transfert.getCommentaire(),
                transfert.getDateEcheance()
            );
            
            if (transferred) {
                AlertUtils.showInfo("Courrier transféré vers " + 
                                  transfert.getServiceDestination().getServiceName());
                loadCourriers();
            } else {
                AlertUtils.showError("Erreur lors du transfert");
            }
        });
    }
    
    @FXML
    private void handleImprimer() {
        if (selectedCourrier == null) {
            AlertUtils.showWarning("Veuillez sélectionner un courrier");
            return;
        }
        
        try {
            // Créer le contenu à imprimer
            VBox printContent = createPrintContent(selectedCourrier);
            
            // Configurer l'impression
            PrinterJob printerJob = PrinterJob.createPrinterJob();
            
            if (printerJob != null && printerJob.showPrintDialog(tableauCourriers.getScene().getWindow())) {
                boolean success = printerJob.printPage(printContent);
                
                if (success) {
                    printerJob.endJob();
                    AlertUtils.showInfo("Impression lancée avec succès");
                } else {
                    AlertUtils.showError("Erreur lors de l'impression");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'impression: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur lors de l'impression: " + e.getMessage());
        }
    }
    
    /**
     * Crée le contenu à imprimer pour un courrier
     */
    private VBox createPrintContent(Courrier courrier) {
        VBox content = new VBox(15);
        content.setPadding(new javafx.geometry.Insets(20));
        content.setStyle("-fx-background-color: white;");
        
        // En-tête
        Text titre = new Text("FICHE DE COURRIER");
        titre.setStyle("-fx-font-size: 18px; -fx-font-weight: bold;");
        content.getChildren().add(titre);
        
        // Informations
        content.getChildren().add(new Text("N° Courrier: " + courrier.getNumeroCourrier()));
        content.getChildren().add(new Text("Type: " + courrier.getTypeCourrier().getLibelle()));
        content.getChildren().add(new Text("Objet: " + courrier.getObjet()));
        content.getChildren().add(new Text("Expéditeur: " + courrier.getExpediteur()));
        content.getChildren().add(new Text("Date: " + courrier.getDateReceptionFormatee()));
        content.getChildren().add(new Text("Priorité: " + courrier.getPriorite()));
        content.getChildren().add(new Text("Statut: " + courrier.getStatut().getLibelle()));
        
        if (courrier.getNotes() != null && !courrier.getNotes().isEmpty()) {
            content.getChildren().add(new Text("\nNotes:\n" + courrier.getNotes()));
        }
        
        return content;
    }
}