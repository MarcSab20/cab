package application.controllers;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import application.models.Reunion;
import application.models.User;
import application.services.ReunionService;
import application.services.ReunionSyncService;
import application.services.DatabaseService;
import application.utils.SessionManager;
import application.utils.AlertUtils;

import java.io.IOException;
import java.net.URL;
import java.sql.*;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class ReunionsController implements Initializable, ReunionSyncService.ReunionListener {
    
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
    @FXML private Button btnNouvelleReunion;
    @FXML private Button btnReunionInstantanee;
    @FXML private Button btnModifierReunion;
    @FXML private Button btnSupprimerReunion;
    @FXML private Button btnDemarrerReunion;
    @FXML private Button btnRejoindreVisio;
    @FXML private Button btnTerminerReunion;
    @FXML private Button btnReporterReunion;
    @FXML private Button btnAnnulerReunion;
    
    private User currentUser;
    private ReunionService reunionService;
    private ReunionSyncService reunionSyncService;
    private DatabaseService databaseService;
    private ObservableList<Reunion> reunions;
    private Reunion selectedReunion;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("ReunionsController.initialize() appelé");
        
        try {
            currentUser = SessionManager.getInstance().getCurrentUser();
            reunionService = ReunionService.getInstance();
            reunionSyncService = ReunionSyncService.getInstance();
            databaseService = DatabaseService.getInstance();
            reunions = FXCollections.observableArrayList();
            
            // Enregistrer comme listener
            reunionSyncService.addReunionListener(this);
            
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
    
    @Override
    public void onReunionChanged(Reunion reunion, String action) {
        javafx.application.Platform.runLater(() -> {
            loadReunions();
            showReunionNotification(reunion, action);
        });
    }
    
    @Override
    public void onRefreshRequest() {
        javafx.application.Platform.runLater(() -> {
            loadReunions();
        });
    }
    
    private void showReunionNotification(Reunion reunion, String action) {
        String message = switch (action) {
            case "CREATED" -> "Nouvelle réunion: " + reunion.getTitre();
            case "STARTED" -> "Réunion démarrée: " + reunion.getTitre();
            case "ENDED" -> "Réunion terminée: " + reunion.getTitre();
            default -> "Réunion mise à jour: " + reunion.getTitre();
        };
        
        System.out.println("📅 " + message);
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
        if (btnNouvelleReunion != null) {
            btnNouvelleReunion.setOnAction(e -> handleNouvelleReunion());
        }
        if (btnReunionInstantanee != null) {
            btnReunionInstantanee.setOnAction(e -> handleReunionInstantanee());
        }
        if (btnModifierReunion != null) {
            btnModifierReunion.setOnAction(e -> handleModifier());
        }
        if (btnSupprimerReunion != null) {
            btnSupprimerReunion.setOnAction(e -> handleSupprimer());
        }
        if (btnDemarrerReunion != null) {
            btnDemarrerReunion.setOnAction(e -> handleDemarrer());
        }
        if (btnRejoindreVisio != null) {
            btnRejoindreVisio.setOnAction(e -> handleRejoindreVisio());
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
    
    /**
     * NOUVELLE MÉTHODE: Ouvre la fenêtre pour créer une nouvelle réunion
     */
    @FXML
    private void handleNouvelleReunion() {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/views/creer_reunion.fxml"));
            Parent root = loader.load();
            
            Stage stage = new Stage();
            stage.setTitle("Programmer une réunion");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            
            // Charger le CSS
            URL cssUrl = getClass().getResource("/application/styles/application.css");
            if (cssUrl != null) {
                stage.getScene().getStylesheets().add(cssUrl.toExternalForm());
            }
            
            stage.showAndWait();
            
            // Recharger les réunions après fermeture
            loadReunions();
            
        } catch (IOException e) {
            System.err.println("Erreur lors de l'ouverture de la fenêtre: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Impossible d'ouvrir la fenêtre de création");
        }
    }
    
    /**
     * NOUVELLE MÉTHODE: Lance une réunion instantanée avec les utilisateurs actifs
     */
    @FXML
    private void handleReunionInstantanee() {
        try {
            // Récupérer les utilisateurs en ligne
            List<User> usersOnline = getOnlineUsers();
            
            if (usersOnline.isEmpty()) {
                AlertUtils.showWarning("Aucun utilisateur n'est actuellement en ligne");
                return;
            }
            
            // Afficher une boîte de dialogue pour sélectionner les participants
            String titre = AlertUtils.showTextInput(
                "Réunion instantanée",
                "Titre de la réunion:",
                "Réunion instantanée - " + LocalDateTime.now().format(DateTimeFormatter.ofPattern("dd/MM HH:mm"))
            ).orElse(null);
            
            if (titre == null || titre.trim().isEmpty()) {
                return;
            }
            
            // Créer la réunion
            Reunion reunion = new Reunion();
            reunion.setTitre(titre);
            reunion.setDescription("Réunion instantanée");
            reunion.setDateReunion(LocalDateTime.now());
            reunion.setDureeMinutes(60);
            reunion.setLieu("Visioconférence");
            reunion.setOrganisateur(currentUser);
            reunion.setStatut(application.models.StatutReunion.EN_COURS);
            
            // Générer le lien Jitsi
            String roomName = "EMAA_Instant_" + System.currentTimeMillis();
            reunion.setLienVisio(roomName);
            
            // Sauvegarder
            if (reunionSyncService.creerReunion(reunion, usersOnline)) {
                AlertUtils.showInfo("Réunion instantanée créée!\nLes participants seront notifiés.");
                
                // Ouvrir directement la visioconférence
                ouvrirVisioconference(reunion);
                
                loadReunions();
            } else {
                AlertUtils.showError("Erreur lors de la création de la réunion");
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la création de la réunion instantanée: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur: " + e.getMessage());
        }
    }
    
    /**
     * NOUVELLE MÉTHODE: Récupère les utilisateurs en ligne
     */
    private List<User> getOnlineUsers() {
        List<User> users = new ArrayList<>();
        
        try {
            String query = """
                SELECT u.*, r.nom as role_nom, r.description as role_desc, 
                       r.permissions, r.actif as role_actif
                FROM users u 
                LEFT JOIN roles r ON u.role_id = r.id 
                INNER JOIN user_presence up ON u.id = up.user_id
                WHERE u.actif = 1 AND up.statut = 'online' AND u.id != ?
                ORDER BY u.nom, u.prenom
            """;
            
            try (Connection conn = databaseService.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                
                stmt.setInt(1, currentUser.getId());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        User user = new User();
                        user.setId(rs.getInt("id"));
                        user.setCode(rs.getString("code"));
                        user.setNom(rs.getString("nom"));
                        user.setPrenom(rs.getString("prenom"));
                        user.setEmail(rs.getString("email"));
                        
                        application.models.Role role = new application.models.Role();
                        role.setId(rs.getInt("role_id"));
                        role.setNom(rs.getString("role_nom"));
                        user.setRole(role);
                        
                        users.add(user);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur SQL: " + e.getMessage());
            e.printStackTrace();
        }
        
        return users;
    }
    
    /**
     * NOUVELLE MÉTHODE: Ouvre la fenêtre de visioconférence
     */
    @FXML
    private void handleRejoindreVisio() {
        if (selectedReunion == null) {
            AlertUtils.showWarning("Veuillez sélectionner une réunion");
            return;
        }
        
        if (selectedReunion.getLienVisio() == null || selectedReunion.getLienVisio().isEmpty()) {
            AlertUtils.showWarning("Cette réunion n'a pas de lien de visioconférence");
            return;
        }
        
        ouvrirVisioconference(selectedReunion);
    }
    
    /**
     * NOUVELLE MÉTHODE: Ouvre la visioconférence pour une réunion
     */
    private void ouvrirVisioconference(Reunion reunion) {
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/views/visioconference.fxml"));
            Parent root = loader.load();
            
            // Récupérer le contrôleur et passer la réunion
            VisioconferenceController controller = loader.getController();
            controller.chargerReunion(reunion);
            
            Stage stage = new Stage();
            stage.setTitle("Visioconférence - " + reunion.getTitre());
            stage.setScene(new Scene(root));
            stage.setMaximized(true);
            
            // Charger le CSS
            URL cssUrl = getClass().getResource("/application/styles/application.css");
            if (cssUrl != null) {
                stage.getScene().getStylesheets().add(cssUrl.toExternalForm());
            }
            
            // Nettoyer à la fermeture
            stage.setOnCloseRequest(e -> controller.cleanup());
            
            stage.show();
            
        } catch (IOException e) {
            System.err.println("Erreur lors de l'ouverture de la visioconférence: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Impossible d'ouvrir la visioconférence");
        }
    }
    
    private void loadReunions() {
        try {
            List<Reunion> list = reunionService.getAllReunions();
            
            // Charger les participants pour chaque réunion
            for (Reunion r : list) {
                chargerParticipants(r);
            }
            
            reunions.clear();
            reunions.addAll(list);
            tableauReunions.setItems(reunions);
            
            System.out.println("Réunions chargées: " + reunions.size());
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des réunions: " + e.getMessage());
            AlertUtils.showError("Erreur lors du chargement des réunions");
        }
    }
    
    /**
     * Charge les participants d'une réunion
     */
    private void chargerParticipants(Reunion reunion) {
        try {
            String query = """
                SELECT u.* FROM users u
                INNER JOIN reunion_participants rp ON u.id = rp.user_id
                WHERE rp.reunion_id = ?
            """;
            
            try (Connection conn = databaseService.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                
                stmt.setInt(1, reunion.getId());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    List<User> participants = new ArrayList<>();
                    
                    while (rs.next()) {
                        User user = new User();
                        user.setId(rs.getInt("id"));
                        user.setCode(rs.getString("code"));
                        user.setNom(rs.getString("nom"));
                        user.setPrenom(rs.getString("prenom"));
                        participants.add(user);
                    }
                    
                    reunion.setParticipants(participants);
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors du chargement des participants: " + e.getMessage());
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
                chargerParticipants(r);
                boolean matches = true;
                
                if (!statutFilter.equals("Toutes")) {
                    if (!r.getStatut().getLibelle().equals(statutFilter)) {
                        matches = false;
                    }
                }
                
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
            String lieu = reunion.getLieu();
            if (reunion.getLienVisio() != null && !reunion.getLienVisio().isEmpty()) {
                lieu += " 📹 (Visioconférence disponible)";
            }
            labelLieuReunion.setText(lieu);
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
        
        // Afficher le bouton de visio si disponible
        if (btnRejoindreVisio != null) {
            btnRejoindreVisio.setDisable(
                reunion.getLienVisio() == null || reunion.getLienVisio().isEmpty()
            );
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
        
        try {
            FXMLLoader loader = new FXMLLoader(getClass().getResource("/application/views/creer_reunion.fxml"));
            Parent root = loader.load();
            
            CreerReunionController controller = loader.getController();
            controller.setReunionAModifier(selectedReunion);
            
            Stage stage = new Stage();
            stage.setTitle("Modifier la réunion");
            stage.initModality(Modality.APPLICATION_MODAL);
            stage.setScene(new Scene(root));
            
            URL cssUrl = getClass().getResource("/application/styles/application.css");
            if (cssUrl != null) {
                stage.getScene().getStylesheets().add(cssUrl.toExternalForm());
            }
            
            stage.showAndWait();
            loadReunions();
            
        } catch (IOException e) {
            System.err.println("Erreur lors de l'ouverture de la fenêtre: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Impossible d'ouvrir la fenêtre de modification");
        }
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
        
        if (reunionSyncService.demarrerReunion(selectedReunion)) {
            AlertUtils.showInfo("Réunion démarrée");
            loadReunions();
            
            // Proposer d'ouvrir la visio si disponible
            if (selectedReunion.getLienVisio() != null && !selectedReunion.getLienVisio().isEmpty()) {
                boolean ouvrir = AlertUtils.showConfirmation(
                    "Visioconférence",
                    "Voulez-vous rejoindre la visioconférence ?"
                );
                
                if (ouvrir) {
                    ouvrirVisioconference(selectedReunion);
                }
            }
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