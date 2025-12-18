package application.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.stage.Stage;
import application.models.User;
import application.models.Reunion;
import application.models.StatutReunion;
import application.services.ReunionService;
import application.services.ReunionSyncService;
import application.services.UserService;
import application.services.DatabaseService;
import application.utils.SessionManager;
import application.utils.AlertUtils;

import java.net.URL;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

/**
 * Contrôleur pour la création et modification de réunions
 */
public class CreerReunionController implements Initializable {
    
    // Informations générales
    @FXML private TextField champTitre;
    @FXML private TextArea champDescription;
    @FXML private DatePicker datePicker;
    @FXML private Spinner<Integer> spinnerHeure;
    @FXML private Spinner<Integer> spinnerMinute;
    @FXML private Spinner<Integer> spinnerDuree;
    @FXML private TextField champLieu;
    @FXML private CheckBox checkVisio;
    @FXML private TextArea champOrdreDuJour;
    
    // Sélection des participants
    @FXML private ListView<User> listeUtilisateursDisponibles;
    @FXML private ListView<User> listeParticipantsSelectionnes;
    @FXML private TextField champRechercheUtilisateurs;
    @FXML private CheckBox checkTousUtilisateurs;
    @FXML private CheckBox checkUtilisateursActifs;
    
    // Boutons
    @FXML private Button btnAjouterParticipant;
    @FXML private Button btnRetirerParticipant;
    @FXML private Button btnCreer;
    @FXML private Button btnAnnuler;
    
    // Services
    private ReunionService reunionService;
    private ReunionSyncService reunionSyncService;
    private UserService userService;
    private DatabaseService databaseService;
    private User currentUser;
    
    // Données
    private ObservableList<User> utilisateursDisponibles;
    private ObservableList<User> participantsSelectionnes;
    private Reunion reunionAModifier;
    private boolean modeModification = false;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        reunionService = ReunionService.getInstance();
        reunionSyncService = ReunionSyncService.getInstance();
        userService = UserService.getInstance();
        databaseService = DatabaseService.getInstance();
        currentUser = SessionManager.getInstance().getCurrentUser();
        
        // Initialisation des listes
        utilisateursDisponibles = FXCollections.observableArrayList();
        participantsSelectionnes = FXCollections.observableArrayList();
        
        listeUtilisateursDisponibles.setItems(utilisateursDisponibles);
        listeParticipantsSelectionnes.setItems(participantsSelectionnes);
        
        // Configuration des cell factories pour afficher les noms
        listeUtilisateursDisponibles.setCellFactory(lv -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(user.getNomComplet() + " (" + user.getRole().getNom() + ")");
                }
            }
        });
        
        listeParticipantsSelectionnes.setCellFactory(lv -> new ListCell<User>() {
            @Override
            protected void updateItem(User user, boolean empty) {
                super.updateItem(user, empty);
                if (empty || user == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(user.getNomComplet() + " (" + user.getRole().getNom() + ")");
                }
            }
        });
        
        // Configuration des spinners
        setupSpinners();
        
        // Configuration des valeurs par défaut
        setupDefaultValues();
        
        // Chargement des utilisateurs
        chargerUtilisateurs();
        
        // Configuration des événements
        setupEventHandlers();
    }
    
    /**
     * Configure les spinners pour l'heure et la durée
     */
    private void setupSpinners() {
        // Spinner heure (0-23)
        SpinnerValueFactory<Integer> heureFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 23, 9);
        spinnerHeure.setValueFactory(heureFactory);
        spinnerHeure.setEditable(true);
        
        // Spinner minute (0-59)
        SpinnerValueFactory<Integer> minuteFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(0, 59, 0, 5);
        spinnerMinute.setValueFactory(minuteFactory);
        spinnerMinute.setEditable(true);
        
        // Spinner durée (15-480 minutes par pas de 15)
        SpinnerValueFactory<Integer> dureeFactory = 
            new SpinnerValueFactory.IntegerSpinnerValueFactory(15, 480, 60, 15);
        spinnerDuree.setValueFactory(dureeFactory);
        spinnerDuree.setEditable(true);
    }
    
    /**
     * Configure les valeurs par défaut
     */
    private void setupDefaultValues() {
        // Date par défaut : aujourd'hui
        datePicker.setValue(LocalDate.now());
        
        // Lieu par défaut
        champLieu.setText("Salle de conférence");
        
        // Visio activée par défaut
        checkVisio.setSelected(true);
    }
    
    /**
     * Configure les gestionnaires d'événements
     */
    private void setupEventHandlers() {
        // Double-clic pour ajouter un participant
        listeUtilisateursDisponibles.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                ajouterParticipant();
            }
        });
        
        // Double-clic pour retirer un participant
        listeParticipantsSelectionnes.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                retirerParticipant();
            }
        });
        
        // Recherche d'utilisateurs
        champRechercheUtilisateurs.textProperty().addListener((obs, oldVal, newVal) -> {
            filtrerUtilisateurs();
        });
        
        // Filtres
        checkUtilisateursActifs.selectedProperty().addListener((obs, oldVal, newVal) -> {
            chargerUtilisateurs();
        });
    }
    
    /**
     * Charge la liste des utilisateurs
     */
    private void chargerUtilisateurs() {
        try {
            List<User> users = getAllUsers();
            
            // Filtrer si nécessaire
            if (checkUtilisateursActifs.isSelected()) {
                users = users.stream()
                    .filter(this::isUserOnline)
                    .collect(Collectors.toList());
            }
            
            // Exclure les participants déjà sélectionnés
            users.removeAll(participantsSelectionnes);
            
            utilisateursDisponibles.clear();
            utilisateursDisponibles.addAll(users);
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des utilisateurs: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Récupère tous les utilisateurs actifs
     */
    private List<User> getAllUsers() {
        List<User> users = new ArrayList<>();
        
        try {
            String query = """
                SELECT u.*, r.nom as role_nom, r.description as role_desc, 
                       r.permissions, r.actif as role_actif
                FROM users u 
                LEFT JOIN roles r ON u.role_id = r.id 
                WHERE u.actif = 1 AND u.id != ?
                ORDER BY u.nom, u.prenom
            """;
            
            try (Connection conn = databaseService.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                
                stmt.setInt(1, currentUser.getId());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    while (rs.next()) {
                        // Utiliser la méthode de mapping existante
                        User user = new User();
                        user.setId(rs.getInt("id"));
                        user.setCode(rs.getString("code"));
                        user.setNom(rs.getString("nom"));
                        user.setPrenom(rs.getString("prenom"));
                        user.setEmail(rs.getString("email"));
                        
                        // Mapper le rôle
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
     * Vérifie si un utilisateur est en ligne
     */
    private boolean isUserOnline(User user) {
        try {
            String query = "SELECT statut FROM user_presence WHERE user_id = ?";
            
            try (Connection conn = databaseService.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                
                stmt.setInt(1, user.getId());
                
                try (ResultSet rs = stmt.executeQuery()) {
                    if (rs.next()) {
                        String statut = rs.getString("statut");
                        return "online".equals(statut);
                    }
                }
            }
        } catch (SQLException e) {
            System.err.println("Erreur lors de la vérification du statut: " + e.getMessage());
        }
        
        return false;
    }
    
    /**
     * Filtre la liste des utilisateurs selon la recherche
     */
    private void filtrerUtilisateurs() {
        String recherche = champRechercheUtilisateurs.getText().toLowerCase();
        
        if (recherche.isEmpty()) {
            chargerUtilisateurs();
            return;
        }
        
        List<User> users = getAllUsers();
        users = users.stream()
            .filter(u -> u.getNomComplet().toLowerCase().contains(recherche) ||
                        u.getRole().getNom().toLowerCase().contains(recherche))
            .collect(Collectors.toList());
        
        users.removeAll(participantsSelectionnes);
        
        utilisateursDisponibles.clear();
        utilisateursDisponibles.addAll(users);
    }
    
    /**
     * Ajoute un participant sélectionné
     */
    @FXML
    private void ajouterParticipant() {
        User selected = listeUtilisateursDisponibles.getSelectionModel().getSelectedItem();
        if (selected != null) {
            participantsSelectionnes.add(selected);
            utilisateursDisponibles.remove(selected);
        }
    }
    
    /**
     * Retire un participant sélectionné
     */
    @FXML
    private void retirerParticipant() {
        User selected = listeParticipantsSelectionnes.getSelectionModel().getSelectedItem();
        if (selected != null) {
            utilisateursDisponibles.add(selected);
            participantsSelectionnes.remove(selected);
        }
    }
    
    /**
     * Ajoute tous les utilisateurs comme participants
     */
    @FXML
    private void ajouterTousUtilisateurs() {
        participantsSelectionnes.addAll(utilisateursDisponibles);
        utilisateursDisponibles.clear();
    }
    
    /**
     * Retire tous les participants
     */
    @FXML
    private void retirerTousParticipants() {
        utilisateursDisponibles.addAll(participantsSelectionnes);
        participantsSelectionnes.clear();
    }
    
    /**
     * Valide et crée/modifie la réunion
     */
    @FXML
    private void handleCreer() {
        // Validation
        if (!validerFormulaire()) {
            return;
        }
        
        try {
            // Créer l'objet Reunion
            Reunion reunion = modeModification ? reunionAModifier : new Reunion();
            
            reunion.setTitre(champTitre.getText().trim());
            reunion.setDescription(champDescription.getText().trim());
            
            // Construction de la date/heure
            LocalDate date = datePicker.getValue();
            LocalTime time = LocalTime.of(
                spinnerHeure.getValue(),
                spinnerMinute.getValue()
            );
            reunion.setDateReunion(LocalDateTime.of(date, time));
            
            reunion.setDureeMinutes(spinnerDuree.getValue());
            reunion.setLieu(champLieu.getText().trim());
            reunion.setOrganisateur(currentUser);
            reunion.setOrdreDuJour(champOrdreDuJour.getText().trim());
            reunion.setStatut(StatutReunion.PROGRAMMEE);
            
            // Générer le lien Jitsi si visio activée
            if (checkVisio.isSelected()) {
                String roomName = generateJitsiRoomName(reunion.getTitre());
                reunion.setLienVisio(roomName);
            }
            
            // Sauvegarder et ajouter les participants
            List<User> participants = new ArrayList<>(participantsSelectionnes);
            
            if (reunionSyncService.creerReunion(reunion, participants)) {
                AlertUtils.showInfo("Réunion créée avec succès!");
                fermerFenetre();
            } else {
                AlertUtils.showError("Erreur lors de la création de la réunion");
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la création de la réunion: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur: " + e.getMessage());
        }
    }
    
    /**
     * Génère un nom de salle Jitsi unique
     */
    private String generateJitsiRoomName(String titre) {
        // Nettoyer le titre pour créer un nom de salle valide
        String roomName = titre.replaceAll("[^a-zA-Z0-9]", "")
                               .toLowerCase();
        
        // Ajouter un timestamp pour unicité
        long timestamp = System.currentTimeMillis();
        
        return "EMAA_" + roomName + "_" + timestamp;
    }
    
    /**
     * Valide le formulaire
     */
    private boolean validerFormulaire() {
        // Vérifier le titre
        if (champTitre.getText().trim().isEmpty()) {
            AlertUtils.showWarning("Veuillez saisir un titre pour la réunion");
            champTitre.requestFocus();
            return false;
        }
        
        // Vérifier la date
        if (datePicker.getValue() == null) {
            AlertUtils.showWarning("Veuillez sélectionner une date");
            datePicker.requestFocus();
            return false;
        }
        
        // Vérifier que la date n'est pas dans le passé
        LocalDateTime dateReunion = LocalDateTime.of(
            datePicker.getValue(),
            LocalTime.of(spinnerHeure.getValue(), spinnerMinute.getValue())
        );
        
        if (dateReunion.isBefore(LocalDateTime.now())) {
            AlertUtils.showWarning("La date de la réunion ne peut pas être dans le passé");
            return false;
        }
        
        // Vérifier qu'il y a au moins un participant
        if (participantsSelectionnes.isEmpty()) {
            AlertUtils.showWarning("Veuillez sélectionner au moins un participant");
            return false;
        }
        
        return true;
    }
    
    /**
     * Annule et ferme la fenêtre
     */
    @FXML
    private void handleAnnuler() {
        fermerFenetre();
    }
    
    /**
     * Ferme la fenêtre
     */
    private void fermerFenetre() {
        Stage stage = (Stage) btnAnnuler.getScene().getWindow();
        stage.close();
    }
    
    /**
     * Configure le mode modification
     */
    public void setReunionAModifier(Reunion reunion) {
        this.reunionAModifier = reunion;
        this.modeModification = true;
        
        // Remplir les champs
        champTitre.setText(reunion.getTitre());
        champDescription.setText(reunion.getDescription());
        datePicker.setValue(reunion.getDateReunion().toLocalDate());
        spinnerHeure.getValueFactory().setValue(reunion.getDateReunion().getHour());
        spinnerMinute.getValueFactory().setValue(reunion.getDateReunion().getMinute());
        spinnerDuree.getValueFactory().setValue(reunion.getDureeMinutes());
        champLieu.setText(reunion.getLieu());
        champOrdreDuJour.setText(reunion.getOrdreDuJour());
        checkVisio.setSelected(reunion.getLienVisio() != null && !reunion.getLienVisio().isEmpty());
        
        // Charger les participants
        participantsSelectionnes.addAll(reunion.getParticipants());
        chargerUtilisateurs();
        
        // Changer le texte du bouton
        btnCreer.setText("Modifier");
    }
}