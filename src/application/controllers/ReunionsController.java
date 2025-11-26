package application.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import application.models.Reunion;
import application.models.User;
import application.services.ReunionService;
import application.utils.SessionManager;
import application.utils.AlertUtils;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class ReunionsController implements Initializable {
    
    // Filtres
    @FXML private ComboBox<String> filtreStatut;
    @FXML private DatePicker dateFiltre;
    @FXML private TextField champRecherche;
    @FXML private ToggleGroup vueToggle;

    
    // Tableau
    @FXML private TableView<Reunion> tableauReunions;
    @FXML private TableColumn<Reunion, String> colonneDate;
    @FXML private TableColumn<Reunion, String> colonneHeure;
    @FXML private TableColumn<Reunion, String> colonneTitre;
    @FXML private TableColumn<Reunion, String> colonneOrganisateur;
    @FXML private TableColumn<Reunion, String> colonneLieu;
    @FXML private TableColumn<Reunion, Integer> colonneParticipants;
    @FXML private TableColumn<Reunion, String> colonneStatutReunion;
    
    // Détails
    @FXML private VBox panneauDetailsReunion;
    @FXML private Label labelTitreReunion;
    @FXML private Label labelDateReunion;
    @FXML private Label labelHeureReunion;
    @FXML private Label labelLieuReunion;
    @FXML private Label labelOrganisateur;
    @FXML private Label labelStatutReunionDetail;
    @FXML private TextArea textAreaDescription;
    @FXML private VBox listeParticipants;
    @FXML private TextArea textAreaCompteRendu;
    
    // Boutons
    @FXML private Button btnModifierReunion;
    @FXML private Button btnSupprimerReunion;
    @FXML private Button btnDemarrerReunion;
    @FXML private Button btnTerminerReunion;
    @FXML private Button btnReporterReunion;
    @FXML private Button btnAnnulerReunion;
    
    private User currentUser;
    private ReunionService reunionService;
    private ObservableList<Reunion> reunions;
    private Reunion selectedReunion;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("ReunionsController.initialize() appelé");
        
        try {
            currentUser = SessionManager.getInstance().getCurrentUser();
            reunionService = ReunionService.getInstance();
            reunions = FXCollections.observableArrayList();
            
            if (currentUser == null) {
                System.err.println("ERREUR: Aucun utilisateur en session");
                return;
            }
            
            setupTableColumns();
            setupFilters();
            setupButtons();
            loadReunions();
            
        } catch (Exception e) {
            System.err.println("Erreur dans ReunionsController.initialize(): " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupTableColumns() {
        colonneDate.setCellValueFactory(cellData -> {
            if (cellData.getValue().getDateReunion() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy");
                return new javafx.beans.property.SimpleStringProperty(
                    cellData.getValue().getDateReunion().format(formatter)
                );
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });
        
        colonneHeure.setCellValueFactory(cellData -> {
            if (cellData.getValue().getDateReunion() != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
                return new javafx.beans.property.SimpleStringProperty(
                    cellData.getValue().getDateReunion().format(formatter)
                );
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });
        
        colonneTitre.setCellValueFactory(new PropertyValueFactory<>("titre"));
        
        colonneOrganisateur.setCellValueFactory(cellData -> {
            if (cellData.getValue().getOrganisateur() != null) {
                return new javafx.beans.property.SimpleStringProperty(
                    cellData.getValue().getOrganisateur().getNomComplet()
                );
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });
        
        colonneLieu.setCellValueFactory(new PropertyValueFactory<>("lieu"));
        
        colonneParticipants.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleIntegerProperty(
                cellData.getValue().getParticipants().size()
            ).asObject()
        );
        
        colonneStatutReunion.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getStatut().getLibelle()
            )
        );
        
        // Listener pour la sélection
        tableauReunions.getSelectionModel().selectedItemProperty().addListener(
            (obs, oldSelection, newSelection) -> {
                if (newSelection != null) {
                    showReunionDetails(newSelection);
                }
            }
        );
    }
    
    private void setupFilters() {
        if (filtreStatut != null) {
            filtreStatut.setItems(FXCollections.observableArrayList(
                "Toutes", "Programmée", "En cours", "Terminée", "Annulée", "Reportée"
            ));
            filtreStatut.setValue("Toutes");
            filtreStatut.setOnAction(e -> applyFilters());
        }
    }
    
    private void setupButtons() {
        if (btnModifierReunion != null) {
            btnModifierReunion.setOnAction(e -> handleModifier());
        }
        if (btnSupprimerReunion != null) {
            btnSupprimerReunion.setOnAction(e -> handleSupprimer());
        }
        if (btnDemarrerReunion != null) {
            btnDemarrerReunion.setOnAction(e -> handleDemarrer());
        }
        if (btnTerminerReunion != null) {
            btnTerminerReunion.setOnAction(e -> handleTerminer());
        }
        if (btnReporterReunion != null) {
            btnReporterReunion.setOnAction(e -> handleReporter());
        }
        if (btnAnnulerReunion != null) {
            btnAnnulerReunion.setOnAction(e -> handleAnnuler());
        }
    }
    
    private void loadReunions() {
        try {
            List<Reunion> list = reunionService.getAllReunions();
            reunions.clear();
            reunions.addAll(list);
            tableauReunions.setItems(reunions);
            
            System.out.println("Réunions chargées: " + reunions.size());
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des réunions: " + e.getMessage());
            AlertUtils.showError("Erreur lors du chargement des réunions");
        }
    }
    
    @FXML
    private void handleRecherche() {
        applyFilters();
    }
    
    private void applyFilters() {
        try {
            String statutFilter = filtreStatut.getValue();
            String searchText = champRecherche != null ? champRecherche.getText().toLowerCase() : "";
            
            List<Reunion> allReunions = reunionService.getAllReunions();
            ObservableList<Reunion> filtered = FXCollections.observableArrayList();
            
            for (Reunion r : allReunions) {
                boolean matches = true;
                
                // Filtre statut
                if (!statutFilter.equals("Toutes")) {
                    if (!r.getStatut().getLibelle().equals(statutFilter)) {
                        matches = false;
                    }
                }
                
                // Recherche textuelle
                if (!searchText.isEmpty()) {
                    boolean textMatch = r.getTitre().toLowerCase().contains(searchText) ||
                                      (r.getDescription() != null && r.getDescription().toLowerCase().contains(searchText)) ||
                                      (r.getLieu() != null && r.getLieu().toLowerCase().contains(searchText));
                    if (!textMatch) {
                        matches = false;
                    }
                }
                
                if (matches) {
                    filtered.add(r);
                }
            }
            
            reunions.clear();
            reunions.addAll(filtered);
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'application des filtres: " + e.getMessage());
        }
    }
    
    private void showReunionDetails(Reunion reunion) {
        selectedReunion = reunion;
        
        if (labelTitreReunion != null) {
            labelTitreReunion.setText(reunion.getTitre());
        }
        
        if (labelDateReunion != null && reunion.getDateReunion() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy");
            labelDateReunion.setText(reunion.getDateReunion().format(formatter));
        }
        
        if (labelHeureReunion != null && reunion.getDateReunion() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("HH:mm");
            String heureDebut = reunion.getDateReunion().format(formatter);
            String heureFin = reunion.getDateReunion().plusMinutes(reunion.getDureeMinutes()).format(formatter);
            labelHeureReunion.setText(heureDebut + " - " + heureFin + " (" + reunion.getDureeMinutes() + " min)");
        }
        
        if (labelLieuReunion != null) {
            labelLieuReunion.setText(reunion.getLieu());
        }
        
        if (labelOrganisateur != null && reunion.getOrganisateur() != null) {
            labelOrganisateur.setText(reunion.getOrganisateur().getNomComplet());
        }
        
        if (labelStatutReunionDetail != null) {
            labelStatutReunionDetail.setText(getStatutIcon(reunion.getStatut()) + " " + reunion.getStatut().getLibelle());
        }
        
        if (textAreaDescription != null) {
            textAreaDescription.setText(reunion.getDescription() != null ? reunion.getDescription() : "");
        }
        
        if (textAreaCompteRendu != null) {
            textAreaCompteRendu.setText(reunion.getCompteRendu() != null ? reunion.getCompteRendu() : "");
        }
    }
    
    private String getStatutIcon(application.models.StatutReunion statut) {
        switch (statut) {
            case PROGRAMMEE: return "🟡";
            case EN_COURS: return "🟢";
            case TERMINEE: return "✅";
            case ANNULEE: return "❌";
            case REPORTEE: return "🔄";
            default: return "";
        }
    }
    
    @FXML
    private void handleModifier() {
        if (selectedReunion == null) {
            AlertUtils.showWarning("Veuillez sélectionner une réunion");
            return;
        }
        AlertUtils.showInfo("Fonction de modification en cours de développement");
    }
    
    @FXML
    private void handleSupprimer() {
        if (selectedReunion == null) {
            AlertUtils.showWarning("Veuillez sélectionner une réunion");
            return;
        }
        
        boolean confirm = AlertUtils.showConfirmation(
            "Confirmation",
            "Êtes-vous sûr de vouloir supprimer cette réunion ?"
        );
        
        if (confirm) {
            if (reunionService.deleteReunion(selectedReunion.getId())) {
                AlertUtils.showInfo("Réunion supprimée avec succès");
                loadReunions();
            } else {
                AlertUtils.showError("Erreur lors de la suppression");
            }
        }
    }
    
    @FXML
    private void handleDemarrer() {
        if (selectedReunion == null) {
            AlertUtils.showWarning("Veuillez sélectionner une réunion");
            return;
        }
        
        selectedReunion.setStatut(application.models.StatutReunion.EN_COURS);
        
        if (reunionService.saveReunion(selectedReunion)) {
            AlertUtils.showInfo("Réunion démarrée");
            loadReunions();
        } else {
            AlertUtils.showError("Erreur lors du démarrage");
        }
    }
    
    @FXML
    private void handleTerminer() {
        if (selectedReunion == null) {
            AlertUtils.showWarning("Veuillez sélectionner une réunion");
            return;
        }
        
        selectedReunion.setStatut(application.models.StatutReunion.TERMINEE);
        
        if (reunionService.saveReunion(selectedReunion)) {
            AlertUtils.showInfo("Réunion terminée");
            loadReunions();
        } else {
            AlertUtils.showError("Erreur lors de la terminaison");
        }
    }
    
    @FXML
    private void handleReporter() {
        if (selectedReunion == null) {
            AlertUtils.showWarning("Veuillez sélectionner une réunion");
            return;
        }
        AlertUtils.showInfo("Fonction de report en cours de développement");
    }
    
    @FXML
    private void handleAnnuler() {
        if (selectedReunion == null) {
            AlertUtils.showWarning("Veuillez sélectionner une réunion");
            return;
        }
        
        boolean confirm = AlertUtils.showConfirmation(
            "Confirmation",
            "Êtes-vous sûr de vouloir annuler cette réunion ?"
        );
        
        if (confirm) {
            selectedReunion.setStatut(application.models.StatutReunion.ANNULEE);
            
            if (reunionService.saveReunion(selectedReunion)) {
                AlertUtils.showInfo("Réunion annulée");
                loadReunions();
            } else {
                AlertUtils.showError("Erreur lors de l'annulation");
            }
        }
    }
}