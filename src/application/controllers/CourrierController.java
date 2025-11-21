package application.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import application.models.Courrier;
import application.models.User;
import application.services.CourrierService;
import application.utils.SessionManager;
import application.utils.AlertUtils;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

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
    
    // Boutons
    @FXML private Button btnModifier;
    @FXML private Button btnSupprimer;
    @FXML private Button btnMarquerTraite;
    @FXML private Button btnArchiver;
    @FXML private Button btnTransferer;
    @FXML private Button btnImprimer;
    @FXML private Label nombreCourriers;
    
    private User currentUser;
    private CourrierService courrierService;
    private ObservableList<Courrier> courriers;
    private Courrier selectedCourrier;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("CourrierController.initialize() appelé");
        
        try {
            currentUser = SessionManager.getInstance().getCurrentUser();
            courrierService = CourrierService.getInstance();
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
            System.err.println("Erreur dans CourrierController.initialize(): " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupTableColumns() {
        colonneNumero.setCellValueFactory(new PropertyValueFactory<>("numeroCourrier"));
        colonneType.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
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
                cellData.getValue().getPriorite().getLibelle()
            )
        );
        colonneStatut.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getStatut().getLibelle()
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
            "Tous", "En attente", "En cours", "Traité", "Archivé"
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
        if (btnModifier != null) {
            btnModifier.setOnAction(e -> handleModifier());
        }
        if (btnSupprimer != null) {
            btnSupprimer.setOnAction(e -> handleSupprimer());
        }
        if (btnMarquerTraite != null) {
            btnMarquerTraite.setOnAction(e -> handleMarquerTraite());
        }
        if (btnArchiver != null) {
            btnArchiver.setOnAction(e -> handleArchiver());
        }
        if (btnTransferer != null) {
            btnTransferer.setOnAction(e -> handleTransferer());
        }
        if (btnImprimer != null) {
            btnImprimer.setOnAction(e -> handleImprimer());
        }
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
    private void handleRecherche() {
        applyFilters();
    }
    
    private void applyFilters() {
        try {
            String statutFilter = filtreStatut.getValue();
            String prioriteFilter = filtrePriorite.getValue();
            String searchText = champRecherche != null ? champRecherche.getText().toLowerCase() : "";
            
            List<Courrier> allCourriers = courrierService.getAllCourriers();
            ObservableList<Courrier> filtered = FXCollections.observableArrayList();
            
            for (Courrier c : allCourriers) {
                boolean matches = true;
                
                // Filtre statut
                if (!statutFilter.equals("Tous")) {
                    if (!c.getStatut().getLibelle().equals(statutFilter)) {
                        matches = false;
                    }
                }
                
                // Filtre priorité
                if (!prioriteFilter.equals("Toutes")) {
                    if (!c.getPriorite().getLibelle().equals(prioriteFilter)) {
                        matches = false;
                    }
                }
                
                // Recherche textuelle
                if (!searchText.isEmpty()) {
                    boolean textMatch = c.getObjet().toLowerCase().contains(searchText) ||
                                      (c.getExpediteur() != null && c.getExpediteur().toLowerCase().contains(searchText)) ||
                                      (c.getNumeroCourrier() != null && c.getNumeroCourrier().toLowerCase().contains(searchText));
                    if (!textMatch) {
                        matches = false;
                    }
                }
                
                if (matches) {
                    filtered.add(c);
                }
            }
            
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
        if (labelType != null) labelType.setText(courrier.getTypeCourrier().getLibelle());
        if (labelObjet != null) labelObjet.setText(courrier.getObjet());
        if (labelExpediteur != null) labelExpediteur.setText(courrier.getExpediteur());
        
        if (labelDate != null && courrier.getDateReception() != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
            labelDate.setText(courrier.getDateReception().format(formatter));
        }
        
        if (labelPriorite != null) {
            labelPriorite.setText(getPrioriteIcon(courrier.getPriorite()) + " " + courrier.getPriorite().getLibelle());
        }
        
        if (labelStatut != null) {
            labelStatut.setText(getStatutIcon(courrier.getStatut()) + " " + courrier.getStatut().getLibelle());
        }
        
        if (textAreaNotes != null) {
            textAreaNotes.setText(courrier.getNotes() != null ? courrier.getNotes() : "");
        }
    }
    
    private String getPrioriteIcon(application.models.PrioriteCourrier priorite) {
        switch (priorite) {
            case URGENTE: return "🔴";
            case HAUTE: return "🟠";
            case NORMALE: return "🟢";
            case BASSE: return "🔵";
            default: return "";
        }
    }
    
    private String getStatutIcon(application.models.StatutCourrier statut) {
        switch (statut) {
            case EN_ATTENTE: return "🟡";
            case EN_COURS: return "🟠";
            case TRAITE: return "✅";
            case ARCHIVE: return "📁";
            case REJETE: return "❌";
            default: return "";
        }
    }
    
    @FXML
    private void handleModifier() {
        if (selectedCourrier == null) {
            AlertUtils.showWarning("Veuillez sélectionner un courrier");
            return;
        }
        
        AlertUtils.showInfo("Fonction de modification en cours de développement");
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
        
        selectedCourrier.setStatut(application.models.StatutCourrier.TRAITE);
        selectedCourrier.setDateTraitement(java.time.LocalDateTime.now());
        
        if (courrierService.saveCourrier(selectedCourrier)) {
            AlertUtils.showInfo("Courrier marqué comme traité");
            loadCourriers();
        } else {
            AlertUtils.showError("Erreur lors de la mise à jour");
        }
    }
    
    @FXML
    private void handleArchiver() {
        if (selectedCourrier == null) {
            AlertUtils.showWarning("Veuillez sélectionner un courrier");
            return;
        }
        
        selectedCourrier.setStatut(application.models.StatutCourrier.ARCHIVE);
        
        if (courrierService.saveCourrier(selectedCourrier)) {
            AlertUtils.showInfo("Courrier archivé avec succès");
            loadCourriers();
        } else {
            AlertUtils.showError("Erreur lors de l'archivage");
        }
    }
    
    @FXML
    private void handleTransferer() {
        AlertUtils.showInfo("Fonction de transfert en cours de développement");
    }
    
    @FXML
    private void handleImprimer() {
        AlertUtils.showInfo("Fonction d'impression en cours de développement");
    }
}