package application.controllers;

import javafx.application.Platform;
import javafx.beans.property.SimpleStringProperty;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.collections.transformation.FilteredList;
import javafx.collections.transformation.SortedList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.stage.FileChooser;
import application.models.*;
import application.services.AdminService;
import application.services.UserService;
import application.services.NetworkService;
import application.utils.AlertUtils;
import application.utils.SessionManager;

import java.io.File;
import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contrôleur amélioré pour la page d'administration
 * NOUVELLES FONCTIONNALITÉS :
 * - Édition complète des rôles avec permissions
 * - Statistiques réelles dynamiques
 * - Logs d'activité avec IP réelle
 */
public class AdminController implements Initializable {
    
    // === ONGLETS ===
    @FXML private TabPane tabsAdmin;
    
    // === SECTION UTILISATEURS ===
    @FXML private ComboBox<String> filtreStatutUtilisateur;
    @FXML private ComboBox<String> filtreRoleUtilisateur;
    @FXML private TextField champRechercheUtilisateur;
    @FXML private TableView<User> tableauUtilisateurs;
    @FXML private TableColumn<User, String> colonneCodeUtilisateur;
    @FXML private TableColumn<User, String> colonneNomUtilisateur;
    @FXML private TableColumn<User, String> colonnePrenomUtilisateur;
    @FXML private TableColumn<User, String> colonneEmailUtilisateur;
    @FXML private TableColumn<User, String> colonneRoleUtilisateur;
    @FXML private TableColumn<User, String> colonneStatutUtilisateur;
    @FXML private TableColumn<User, String> colonneDernierAcces;
    @FXML private TableColumn<User, Void> colonneActionsUtilisateur;
    
    // === SECTION RÔLES ===
    @FXML private ListView<Role> listeRoles;
    @FXML private TextField champNomRole;
    @FXML private TextArea textAreaDescriptionRole;
    @FXML private Label labelNombreUtilisateurs;
    @FXML private CheckBox checkRoleActif;
    @FXML private Button btnSauvegarderRole;
    @FXML private Button btnAnnulerRole;
    @FXML private Button btnSupprimerRole;
    @FXML private Button btnModifierRole;  // NOUVEAU
    
    // Permissions - Accès général
    @FXML private CheckBox permAccueil;
    @FXML private CheckBox permDashboard;
    
    // Permissions - Courrier
    @FXML private CheckBox permCourrierLecture;
    @FXML private CheckBox permCourrierCreation;
    @FXML private CheckBox permCourrierModification;
    @FXML private CheckBox permCourrierSuppression;
    @FXML private CheckBox permCourrierArchivage;
    @FXML private CheckBox permCourrierValidation;
    
    // Permissions - Documents
    @FXML private CheckBox permDocumentLecture;
    @FXML private CheckBox permDocumentCreation;
    @FXML private CheckBox permDocumentModification;
    @FXML private CheckBox permDocumentSuppression;
    @FXML private CheckBox permDocumentPartage;
    
    // Permissions - Réunions
    @FXML private CheckBox permReunionLecture;
    @FXML private CheckBox permReunionCreation;
    @FXML private CheckBox permReunionModification;
    @FXML private CheckBox permReunionSuppression;
    @FXML private CheckBox permReunionAnimation;
    
    // Permissions - Messages
    @FXML private CheckBox permMessageLecture;
    @FXML private CheckBox permMessageEnvoi;
    @FXML private CheckBox permMessageSuppression;
    @FXML private CheckBox permMessageDiffusion;
    
    // Permissions - Administration
    @FXML private CheckBox permAdminUtilisateurs;
    @FXML private CheckBox permAdminRoles;
    @FXML private CheckBox permAdminSysteme;
    @FXML private CheckBox permAdminLogs;
    @FXML private CheckBox permAdminSauvegarde;
    
    // Permissions - Recherche
    @FXML private CheckBox permRecherche;
    @FXML private CheckBox permRechercheAvancee;
    @FXML private CheckBox permArchivageLecture;
    @FXML private CheckBox permArchivageGestion;
    
    // Permissions - Rapports
    @FXML private CheckBox permRapportLecture;
    @FXML private CheckBox permRapportCreation;
    @FXML private CheckBox permRapportExport;
    @FXML private CheckBox permStatistiques;
    
    // === SECTION LOGS ===
    @FXML private ComboBox<String> filtreTypeAction;
    @FXML private ComboBox<String> filtreUtilisateurLog;
    @FXML private DatePicker dateDebutLogs;
    @FXML private DatePicker dateFinLogs;
    @FXML private TableView<LogActivity> tableauLogs;
    @FXML private TableColumn<LogActivity, String> colonneTimestamp;
    @FXML private TableColumn<LogActivity, String> colonneUtilisateurLog;
    @FXML private TableColumn<LogActivity, String> colonneActionLog;
    @FXML private TableColumn<LogActivity, String> colonneDetailsLog;
    @FXML private TableColumn<LogActivity, String> colonneIPLog;
    @FXML private TableColumn<LogActivity, String> colonneStatutLog;
    
    // === SECTION STATISTIQUES (NOUVEAUX ÉLÉMENTS) ===
    @FXML private Label statTotalUtilisateurs;
    @FXML private Label statUtilisateursActifs;
    @FXML private Label statConnexionsAujourdhui;
    @FXML private Label statRolesConfigures;
    @FXML private VBox repartitionRolesContainer;
    @FXML private VBox activiteRecenteContainer;
    
    // Services
    private AdminService adminService;
    private UserService userService;
    private NetworkService networkService;
    private User currentUser;
    
    // Données observables
    private ObservableList<User> usersData = FXCollections.observableArrayList();
    private FilteredList<User> filteredUsers;
    private ObservableList<Role> rolesData = FXCollections.observableArrayList();
    private ObservableList<LogActivity> logsData = FXCollections.observableArrayList();
    private FilteredList<LogActivity> filteredLogs;
    
    // Rôle actuellement sélectionné
    private Role selectedRole;
    private boolean isEditingRole = false;
    
    // Map des CheckBox de permissions
    private Map<Permission, CheckBox> permissionCheckBoxes;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("AdminController.initialize() - Début");
        
        try {
            // Initialisation des services
            adminService = AdminService.getInstance();
            userService = UserService.getInstance();
            networkService = NetworkService.getInstance();
            currentUser = SessionManager.getInstance().getCurrentUser();
            
            if (currentUser == null) {
                System.err.println("ERREUR: Aucun utilisateur en session");
                return;
            }
            
            // Vérification des permissions admin
            if (!hasAdminPermission()) {
                AlertUtils.showError("Accès refusé", "Vous n'avez pas les droits d'administration");
                return;
            }
            
            // Initialisation des composants
            initializeUsersSection();
            initializeRolesSection();
            initializeLogsSection();
            initializeStatisticsSection();
            
            // Chargement des données
            loadAllData();
            
            System.out.println("AdminController.initialize() - Succès");
            
        } catch (Exception e) {
            System.err.println("Erreur dans AdminController.initialize(): " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur d'initialisation", e.getMessage());
        }
    }
    
    // ===== INITIALISATION DES SECTIONS =====
    
    private void initializeUsersSection() {
        // Configuration des colonnes du tableau
        colonneCodeUtilisateur.setCellValueFactory(new PropertyValueFactory<>("code"));
        colonneNomUtilisateur.setCellValueFactory(new PropertyValueFactory<>("nom"));
        colonnePrenomUtilisateur.setCellValueFactory(new PropertyValueFactory<>("prenom"));
        colonneEmailUtilisateur.setCellValueFactory(new PropertyValueFactory<>("email"));
        
        colonneRoleUtilisateur.setCellValueFactory(cellData -> {
            Role role = cellData.getValue().getRole();
            return new SimpleStringProperty(role != null ? role.getNom() : "");
        });
        
        colonneStatutUtilisateur.setCellValueFactory(cellData -> {
            boolean actif = cellData.getValue().isActif();
            return new SimpleStringProperty(actif ? "Actif" : "Inactif");
        });
        
        colonneDernierAcces.setCellValueFactory(cellData -> {
            LocalDateTime dernierAcces = cellData.getValue().getDernierAcces();
            if (dernierAcces != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
                return new SimpleStringProperty(dernierAcces.format(formatter));
            }
            return new SimpleStringProperty("Jamais");
        });
        
        // Colonne d'actions avec boutons
        colonneActionsUtilisateur.setCellFactory(param -> new TableCell<>() {
            private final Button btnModifier = new Button("Modifier");
            private final Button btnSupprimer = new Button("Supprimer");
            private final Button btnSuspendre = new Button("Suspendre");
            private final HBox pane = new HBox(5, btnModifier, btnSuspendre, btnSupprimer);
            
            {
                btnModifier.getStyleClass().add("button-primary");
                btnSupprimer.getStyleClass().add("button-danger");
                btnSuspendre.getStyleClass().add("button-warning");
                
                btnModifier.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleModifierUtilisateur(user);
                });
                
                btnSupprimer.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleSupprimerUtilisateur(user);
                });
                
                btnSuspendre.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    handleToggleSuspendreUtilisateur(user);
                });
            }
            
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                setGraphic(empty ? null : pane);
            }
        });
        
        // Configuration des filtres
        filtreStatutUtilisateur.setItems(FXCollections.observableArrayList(
            "Tous", "Actifs", "Inactifs", "Suspendus"
        ));
        filtreStatutUtilisateur.setValue("Tous");
        
        filtreRoleUtilisateur.setItems(FXCollections.observableArrayList("Tous les rôles"));
        filtreRoleUtilisateur.setValue("Tous les rôles");
        
        // Liste filtrée et triée
        filteredUsers = new FilteredList<>(usersData, p -> true);
        SortedList<User> sortedUsers = new SortedList<>(filteredUsers);
        sortedUsers.comparatorProperty().bind(tableauUtilisateurs.comparatorProperty());
        tableauUtilisateurs.setItems(sortedUsers);
        
        // Listeners pour les filtres
        filtreStatutUtilisateur.setOnAction(e -> applyUserFilters());
        filtreRoleUtilisateur.setOnAction(e -> applyUserFilters());
        champRechercheUtilisateur.textProperty().addListener((obs, oldVal, newVal) -> applyUserFilters());
    }
    
    private void initializeRolesSection() {
        // Configuration de la liste des rôles
        listeRoles.setItems(rolesData);
        listeRoles.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Role role, boolean empty) {
                super.updateItem(role, empty);
                if (empty || role == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    String statusIcon = role.isActif() ? "🟢" : "🔴";
                    setText(statusIcon + " " + role.getNom());
                }
            }
        });
        
        // Sélection d'un rôle
        listeRoles.getSelectionModel().selectedItemProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                loadRoleDetails(newVal);
            }
        });
        
        // Initialisation de la map des permissions
        initializePermissionCheckBoxes();
        
        // Désactiver les champs par défaut
        setRoleFieldsEditable(false);
    }
    
    private void initializePermissionCheckBoxes() {
        permissionCheckBoxes = new HashMap<>();
        
        // Mapper chaque permission à son CheckBox
        permissionCheckBoxes.put(Permission.ACCUEIL, permAccueil);
        permissionCheckBoxes.put(Permission.DASHBOARD, permDashboard);
        permissionCheckBoxes.put(Permission.COURRIER_LECTURE, permCourrierLecture);
        permissionCheckBoxes.put(Permission.COURRIER_CREATION, permCourrierCreation);
        permissionCheckBoxes.put(Permission.COURRIER_MODIFICATION, permCourrierModification);
        permissionCheckBoxes.put(Permission.COURRIER_SUPPRESSION, permCourrierSuppression);
        permissionCheckBoxes.put(Permission.COURRIER_ARCHIVAGE, permCourrierArchivage);
        permissionCheckBoxes.put(Permission.COURRIER_VALIDATION, permCourrierValidation);
        permissionCheckBoxes.put(Permission.DOCUMENT_LECTURE, permDocumentLecture);
        permissionCheckBoxes.put(Permission.DOCUMENT_CREATION, permDocumentCreation);
        permissionCheckBoxes.put(Permission.DOCUMENT_MODIFICATION, permDocumentModification);
        permissionCheckBoxes.put(Permission.DOCUMENT_SUPPRESSION, permDocumentSuppression);
        permissionCheckBoxes.put(Permission.DOCUMENT_PARTAGE, permDocumentPartage);
        permissionCheckBoxes.put(Permission.REUNIONS_LECTURE, permReunionLecture);
        permissionCheckBoxes.put(Permission.REUNIONS_CREATION, permReunionCreation);
        permissionCheckBoxes.put(Permission.REUNIONS_MODIFICATION, permReunionModification);
        permissionCheckBoxes.put(Permission.REUNIONS_SUPPRESSION, permReunionSuppression);
        permissionCheckBoxes.put(Permission.REUNIONS_ANIMATION, permReunionAnimation);
        permissionCheckBoxes.put(Permission.MESSAGES_LECTURE, permMessageLecture);
        permissionCheckBoxes.put(Permission.MESSAGES_ENVOI, permMessageEnvoi);
        permissionCheckBoxes.put(Permission.MESSAGES_SUPPRESSION, permMessageSuppression);
        permissionCheckBoxes.put(Permission.MESSAGES_DIFFUSION, permMessageDiffusion);
        permissionCheckBoxes.put(Permission.ADMIN_UTILISATEURS, permAdminUtilisateurs);
        permissionCheckBoxes.put(Permission.ADMIN_ROLES, permAdminRoles);
        permissionCheckBoxes.put(Permission.ADMIN_SYSTEME, permAdminSysteme);
        permissionCheckBoxes.put(Permission.ADMIN_LOGS, permAdminLogs);
        permissionCheckBoxes.put(Permission.ADMIN_SAUVEGARDE, permAdminSauvegarde);
        permissionCheckBoxes.put(Permission.RECHERCHE, permRecherche);
        permissionCheckBoxes.put(Permission.RECHERCHE_AVANCEE, permRechercheAvancee);
        permissionCheckBoxes.put(Permission.ARCHIVAGE_LECTURE, permArchivageLecture);
        permissionCheckBoxes.put(Permission.ARCHIVAGE_GESTION, permArchivageGestion);
        permissionCheckBoxes.put(Permission.RAPPORTS_LECTURE, permRapportLecture);
        permissionCheckBoxes.put(Permission.RAPPORTS_CREATION, permRapportCreation);
        permissionCheckBoxes.put(Permission.RAPPORTS_EXPORT, permRapportExport);
        permissionCheckBoxes.put(Permission.STATISTIQUES, permStatistiques);
    }
    
    private void initializeLogsSection() {
        // Configuration des colonnes
        colonneTimestamp.setCellValueFactory(cellData -> {
            LocalDateTime timestamp = cellData.getValue().getTimestamp();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
            return new SimpleStringProperty(timestamp.format(formatter));
        });
        
        colonneUtilisateurLog.setCellValueFactory(new PropertyValueFactory<>("userCode"));
        colonneActionLog.setCellValueFactory(new PropertyValueFactory<>("action"));
        colonneDetailsLog.setCellValueFactory(new PropertyValueFactory<>("details"));
        colonneIPLog.setCellValueFactory(new PropertyValueFactory<>("ipAddress"));
        colonneStatutLog.setCellValueFactory(cellData -> new SimpleStringProperty("Succès"));
        
        // Configuration des filtres
        filtreTypeAction.setItems(FXCollections.observableArrayList(
            "Toutes", "Connexion", "Déconnexion", "Création", "Modification", "Suppression", "Consultation"
        ));
        filtreTypeAction.setValue("Toutes");
        
        filtreUtilisateurLog.setItems(FXCollections.observableArrayList("Tous"));
        filtreUtilisateurLog.setValue("Tous");
        
        // Liste filtrée
        filteredLogs = new FilteredList<>(logsData, p -> true);
        SortedList<LogActivity> sortedLogs = new SortedList<>(filteredLogs);
        sortedLogs.comparatorProperty().bind(tableauLogs.comparatorProperty());
        tableauLogs.setItems(sortedLogs);
        
        // Listeners pour les filtres
        filtreTypeAction.setOnAction(e -> applyLogFilters());
        filtreUtilisateurLog.setOnAction(e -> applyLogFilters());
    }
    
    /**
     * NOUVEAU : Initialise les statistiques avec des données réelles
     */
    private void initializeStatisticsSection() {
        // Les statistiques seront chargées dynamiquement dans loadAllData()
        System.out.println("Section statistiques initialisée");
    }
    
    // ===== CHARGEMENT DES DONNÉES =====
    
    private void loadAllData() {
        loadUsers();
        loadRoles();
        loadLogs();
        updateStatistics();  // AMÉLIORÉ
    }
    
    private void loadUsers() {
        try {
            List<User> users = adminService.getAllUsers();
            usersData.setAll(users);
            
            // Mettre à jour les filtres de rôles
            Set<String> roleNames = users.stream()
                .map(u -> u.getRole() != null ? u.getRole().getNom() : "Sans rôle")
                .collect(Collectors.toSet());
            
            ObservableList<String> roleItems = FXCollections.observableArrayList("Tous les rôles");
            roleItems.addAll(roleNames);
            filtreRoleUtilisateur.setItems(roleItems);
            
            System.out.println("✅ Utilisateurs chargés: " + users.size());
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des utilisateurs: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadRoles() {
        try {
            List<Role> roles = adminService.getAllRoles();
            rolesData.setAll(roles);
            
            System.out.println("✅ Rôles chargés: " + roles.size());
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des rôles: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void loadLogs() {
        try {
            List<LogActivity> logs = adminService.getRecentLogs(100);
            logsData.setAll(logs);
            
            // Mettre à jour le filtre des utilisateurs
            Set<String> userCodes = logs.stream()
                .map(LogActivity::getUserCode)
                .filter(Objects::nonNull)
                .collect(Collectors.toSet());
            
            ObservableList<String> userItems = FXCollections.observableArrayList("Tous");
            userItems.addAll(userCodes);
            filtreUtilisateurLog.setItems(userItems);
            
            System.out.println("✅ Logs chargés: " + logs.size());
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des logs: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * AMÉLIORATION MAJEURE : Met à jour les statistiques avec des données RÉELLES
     */
    private void updateStatistics() {
        try {
            // 1. STATISTIQUES GÉNÉRALES
            Map<String, Integer> stats = adminService.getStatistiquesGenerales();
            
            if (statTotalUtilisateurs != null) {
                statTotalUtilisateurs.setText(String.valueOf(stats.get("totalUtilisateurs")));
            }
            
            if (statUtilisateursActifs != null) {
                statUtilisateursActifs.setText(String.valueOf(stats.get("utilisateursActifs")));
            }
            
            if (statConnexionsAujourdhui != null) {
                statConnexionsAujourdhui.setText(String.valueOf(stats.get("connexionsAujourdhui")));
            }
            
            if (statRolesConfigures != null) {
                statRolesConfigures.setText(String.valueOf(stats.get("rolesConfigures")));
            }
            
            // 2. RÉPARTITION PAR RÔLE
            updateRepartitionRoles();
            
            // 3. ACTIVITÉ RÉCENTE
            updateActiviteRecente();
            
            System.out.println("✅ Statistiques mises à jour");
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la mise à jour des statistiques: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * NOUVEAU : Met à jour la répartition des utilisateurs par rôle
     */
    private void updateRepartitionRoles() {
        if (repartitionRolesContainer == null) return;
        
        try {
            Map<String, Integer> repartition = adminService.getRepartitionParRole();
            int total = usersData.size();
            
            repartitionRolesContainer.getChildren().clear();
            
            for (Map.Entry<String, Integer> entry : repartition.entrySet()) {
                String roleName = entry.getKey();
                int count = entry.getValue();
                double pourcentage = total > 0 ? (double) count / total : 0;
                
                // Créer une barre de progression
                HBox roleBar = new HBox(15);
                roleBar.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
                
                Label roleLabel = new Label(roleName + ":");
                roleLabel.setPrefWidth(120);
                
                ProgressBar progressBar = new ProgressBar(pourcentage);
                progressBar.setPrefWidth(200);
                
                Label countLabel = new Label(String.format("%d (%.0f%%)", count, pourcentage * 100));
                countLabel.setStyle("-fx-font-weight: bold;");
                
                roleBar.getChildren().addAll(roleLabel, progressBar, countLabel);
                repartitionRolesContainer.getChildren().add(roleBar);
            }
            
        } catch (Exception e) {
            System.err.println("Erreur répartition rôles: " + e.getMessage());
        }
    }
    
    /**
     * NOUVEAU : Met à jour l'activité récente avec IP réelle
     */
    private void updateActiviteRecente() {
        if (activiteRecenteContainer == null) return;
        
        try {
            List<LogActivity> recentLogs = adminService.getRecentLogs(5);
            
            activiteRecenteContainer.getChildren().clear();
            
            for (LogActivity log : recentLogs) {
                VBox logItem = createLogItem(log);
                activiteRecenteContainer.getChildren().add(logItem);
            }
            
        } catch (Exception e) {
            System.err.println("Erreur activité récente: " + e.getMessage());
        }
    }
    
    /**
     * NOUVEAU : Crée un élément visuel pour un log
     */
    private VBox createLogItem(LogActivity log) {
        HBox logBox = new HBox(15);
        logBox.setAlignment(javafx.geometry.Pos.CENTER_LEFT);
        logBox.setStyle("-fx-padding: 10; -fx-background-color: #f8f9fa; -fx-background-radius: 5;");
        
        // Icône
        Label icon = new Label(log.getActionIcon());
        icon.setStyle("-fx-font-size: 16px;");
        
        // Détails
        VBox details = new VBox();
        HBox.setHgrow(details, javafx.scene.layout.Priority.ALWAYS);
        
        Label action = new Label(log.getUserCode() + " - " + log.getAction());
        action.setStyle("-fx-font-weight: bold;");
        
        String ipInfo = log.getIpAddress() != null ? log.getIpAddress() : "IP non disponible";
        Label info = new Label("IP: " + ipInfo + " • " + log.getDetails());
        info.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        
        details.getChildren().addAll(action, info);
        
        // Temps
        Label time = new Label(log.getTimeAgo());
        time.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
        
        logBox.getChildren().addAll(icon, details, time);
        
        VBox container = new VBox(logBox);
        container.setSpacing(8);
        
        return container;
    }
    
    // ===== GESTION DES UTILISATEURS =====
    
    @FXML
    private void handleNouvelUtilisateur() {
        try {
            UserFormDialog dialog = new UserFormDialog(null, rolesData);
            Optional<User> result = dialog.showAndWait();
            
            result.ifPresent(user -> {
                if (adminService.createUser(user)) {
                    AlertUtils.showInfo("Utilisateur créé avec succès");
                    loadUsers();
                    updateStatistics();  // Rafraîchir les stats
                    
                    // Logger l'action avec IP
                    String ip = networkService.getLocalIPAddress();
                    adminService.logAction(currentUser.getId(), "CREATION_UTILISATEUR", 
                        "Création de l'utilisateur: " + user.getCode(), ip);
                } else {
                    AlertUtils.showError("Erreur lors de la création de l'utilisateur");
                }
            });
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la création d'utilisateur: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur", e.getMessage());
        }
    }
    
    private void handleModifierUtilisateur(User user) {
        try {
            UserFormDialog dialog = new UserFormDialog(user, rolesData);
            Optional<User> result = dialog.showAndWait();
            
            result.ifPresent(modifiedUser -> {
                if (adminService.updateUser(modifiedUser)) {
                    AlertUtils.showInfo("Utilisateur modifié avec succès");
                    loadUsers();
                    updateStatistics();
                    
                    String ip = networkService.getLocalIPAddress();
                    adminService.logAction(currentUser.getId(), "MODIFICATION_UTILISATEUR", 
                        "Modification de l'utilisateur: " + user.getCode(), ip);
                } else {
                    AlertUtils.showError("Erreur lors de la modification de l'utilisateur");
                }
            });
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la modification d'utilisateur: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur", e.getMessage());
        }
    }
    
    private void handleSupprimerUtilisateur(User user) {
        try {
            if (user.getCode().equals("admin")) {
                AlertUtils.showWarning("Impossible de supprimer le compte administrateur principal");
                return;
            }
            
            if (user.getId() == currentUser.getId()) {
                AlertUtils.showWarning("Vous ne pouvez pas supprimer votre propre compte");
                return;
            }
            
            boolean confirm = AlertUtils.showConfirmation(
                "Confirmation", 
                "Êtes-vous sûr de vouloir supprimer l'utilisateur " + user.getNomComplet() + " ?"
            );
            
            if (confirm) {
                if (adminService.deleteUser(user.getId())) {
                    AlertUtils.showInfo("Utilisateur supprimé avec succès");
                    loadUsers();
                    updateStatistics();
                    
                    String ip = networkService.getLocalIPAddress();
                    adminService.logAction(currentUser.getId(), "SUPPRESSION_UTILISATEUR", 
                        "Suppression de l'utilisateur: " + user.getCode(), ip);
                } else {
                    AlertUtils.showError("Erreur lors de la suppression de l'utilisateur");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la suppression d'utilisateur: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur", e.getMessage());
        }
    }
    
    private void handleToggleSuspendreUtilisateur(User user) {
        try {
            boolean newStatus = !user.isActif();
            String action = newStatus ? "activer" : "suspendre";
            
            boolean confirm = AlertUtils.showConfirmation(
                "Confirmation", 
                "Voulez-vous " + action + " l'utilisateur " + user.getNomComplet() + " ?"
            );
            
            if (confirm) {
                user.setActif(newStatus);
                if (adminService.updateUser(user)) {
                    AlertUtils.showInfo("Statut de l'utilisateur modifié avec succès");
                    loadUsers();
                    updateStatistics();
                    
                    String ip = networkService.getLocalIPAddress();
                    adminService.logAction(currentUser.getId(), "MODIFICATION_STATUT_UTILISATEUR", 
                        action.toUpperCase() + " de l'utilisateur: " + user.getCode(), ip);
                } else {
                    AlertUtils.showError("Erreur lors de la modification du statut");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la modification du statut: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur", e.getMessage());
        }
    }
    
    @FXML
    private void handleRechercherUtilisateur() {
        applyUserFilters();
    }
    
    private void applyUserFilters() {
        filteredUsers.setPredicate(user -> {
            String statut = filtreStatutUtilisateur.getValue();
            if (statut != null && !statut.equals("Tous")) {
                boolean match = false;
                switch (statut) {
                    case "Actifs": match = user.isActif(); break;
                    case "Inactifs": match = !user.isActif(); break;
                    case "Suspendus": match = !user.isActif(); break;
                }
                if (!match) return false;
            }
            
            String role = filtreRoleUtilisateur.getValue();
            if (role != null && !role.equals("Tous les rôles")) {
                if (user.getRole() == null || !user.getRole().getNom().equals(role)) {
                    return false;
                }
            }
            
            String recherche = champRechercheUtilisateur.getText();
            if (recherche != null && !recherche.trim().isEmpty()) {
                String search = recherche.toLowerCase();
                return user.getCode().toLowerCase().contains(search) ||
                       user.getNom().toLowerCase().contains(search) ||
                       user.getPrenom().toLowerCase().contains(search) ||
                       (user.getEmail() != null && user.getEmail().toLowerCase().contains(search));
            }
            
            return true;
        });
    }
    
    @FXML
    private void handleGenererRapportUtilisateurs() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Enregistrer le rapport");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichier CSV", "*.csv")
            );
            fileChooser.setInitialFileName("rapport_utilisateurs_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv");
            
            File file = fileChooser.showSaveDialog(tableauUtilisateurs.getScene().getWindow());
            
            if (file != null) {
                if (adminService.exportUsersToCSV(file, new ArrayList<>(filteredUsers))) {
                    AlertUtils.showInfo("Rapport généré avec succès: " + file.getName());
                    
                    String ip = networkService.getLocalIPAddress();
                    adminService.logAction(currentUser.getId(), "EXPORT_RAPPORT", 
                        "Export du rapport utilisateurs", ip);
                } else {
                    AlertUtils.showError("Erreur lors de la génération du rapport");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la génération du rapport: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur", e.getMessage());
        }
    }
    
    // ===== GESTION DES RÔLES - AMÉLIORÉE =====
    
    @FXML
    private void handleNouveauRole() {
        try {
            RoleFormDialog dialog = new RoleFormDialog(null);
            Optional<Role> result = dialog.showAndWait();
            
            result.ifPresent(role -> {
                if (adminService.createRole(role)) {
                    AlertUtils.showInfo("Rôle créé avec succès");
                    loadRoles();
                    updateStatistics();
                    
                    String ip = networkService.getLocalIPAddress();
                    adminService.logAction(currentUser.getId(), "CREATION_ROLE", 
                        "Création du rôle: " + role.getNom(), ip);
                } else {
                    AlertUtils.showError("Erreur lors de la création du rôle");
                }
            });
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la création du rôle: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur", e.getMessage());
        }
    }
    
    /**
     * AMÉLIORATION : Charge les détails d'un rôle en mode lecture seule
     */
    private void loadRoleDetails(Role role) {
        selectedRole = role;
        isEditingRole = false;
        
        // Remplir les champs
        champNomRole.setText(role.getNom());
        textAreaDescriptionRole.setText(role.getDescription());
        checkRoleActif.setSelected(role.isActif());
        
        // Compter le nombre d'utilisateurs avec ce rôle
        long count = usersData.stream()
            .filter(u -> u.getRole() != null && u.getRole().getId() == role.getId())
            .count();
        labelNombreUtilisateurs.setText(count + " utilisateur(s) assigné(s)");
        
        // Cocher les permissions
        Set<Permission> permissions = role.getPermissions();
        permissionCheckBoxes.forEach((perm, checkBox) -> {
            checkBox.setSelected(permissions.contains(perm));
        });
        
        // Mode lecture seule
        setRoleFieldsEditable(false);
        
        // Activer le bouton Modifier
        if (btnModifierRole != null) {
            btnModifierRole.setDisable(false);
        }
    }
    
    /**
     * NOUVEAU : Active le mode édition pour le rôle
     */
    @FXML
    private void handleModifierRole() {
        if (selectedRole == null) {
            AlertUtils.showWarning("Veuillez sélectionner un rôle");
            return;
        }
        
        // Activer l'édition
        isEditingRole = true;
        setRoleFieldsEditable(true);
        
        // Le nom reste immuable
        champNomRole.setEditable(false);
        champNomRole.setStyle("-fx-background-color: #f0f0f0;");
        
        AlertUtils.showInfo("Mode édition", 
            "Vous pouvez maintenant modifier la description et les permissions.\n" +
            "Le nom du rôle ne peut pas être modifié.");
    }
    
    /**
     * AMÉLIORATION : Sauvegarde les modifications du rôle
     */
    @FXML
    private void handleSauvegarderRole() {
        try {
            if (selectedRole == null) {
                AlertUtils.showWarning("Veuillez sélectionner un rôle");
                return;
            }
            
            // Récupérer les valeurs des champs (nom immuable)
            selectedRole.setDescription(textAreaDescriptionRole.getText().trim());
            selectedRole.setActif(checkRoleActif.isSelected());
            
            // Récupérer les permissions cochées
            Set<Permission> permissions = new HashSet<>();
            permissionCheckBoxes.forEach((perm, checkBox) -> {
                if (checkBox.isSelected()) {
                    permissions.add(perm);
                }
            });
            selectedRole.setPermissions(permissions);
            
            // Sauvegarder
            if (adminService.updateRole(selectedRole)) {
                AlertUtils.showInfo("Rôle sauvegardé avec succès");
                loadRoles();
                setRoleFieldsEditable(false);
                isEditingRole = false;
                
                String ip = networkService.getLocalIPAddress();
                adminService.logAction(currentUser.getId(), "MODIFICATION_ROLE", 
                    "Modification du rôle: " + selectedRole.getNom() + 
                    " (" + permissions.size() + " permissions)", ip);
            } else {
                AlertUtils.showError("Erreur lors de la sauvegarde du rôle");
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la sauvegarde du rôle: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur", e.getMessage());
        }
    }
    
    @FXML
    private void handleAnnulerRole() {
        if (selectedRole != null) {
            loadRoleDetails(selectedRole);
        }
        isEditingRole = false;
        setRoleFieldsEditable(false);
    }
    
    @FXML
    private void handleSupprimerRole() {
        try {
            if (selectedRole == null) {
                AlertUtils.showWarning("Veuillez sélectionner un rôle");
                return;
            }
            
            // Vérifier si des utilisateurs utilisent ce rôle
            long count = usersData.stream()
                .filter(u -> u.getRole() != null && u.getRole().getId() == selectedRole.getId())
                .count();
            
            if (count > 0) {
                AlertUtils.showWarning("Impossible de supprimer ce rôle car " + count + 
                    " utilisateur(s) l'utilisent encore");
                return;
            }
            
            boolean confirm = AlertUtils.showConfirmation(
                "Confirmation", 
                "Êtes-vous sûr de vouloir supprimer le rôle " + selectedRole.getNom() + " ?"
            );
            
            if (confirm) {
                if (adminService.deleteRole(selectedRole.getId())) {
                    AlertUtils.showInfo("Rôle supprimé avec succès");
                    loadRoles();
                    clearRoleDetails();
                    updateStatistics();
                    
                    String ip = networkService.getLocalIPAddress();
                    adminService.logAction(currentUser.getId(), "SUPPRESSION_ROLE", 
                        "Suppression du rôle: " + selectedRole.getNom(), ip);
                } else {
                    AlertUtils.showError("Erreur lors de la suppression du rôle");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la suppression du rôle: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur", e.getMessage());
        }
    }
    
    @FXML
    private void handleToutSelectionner() {
        permissionCheckBoxes.values().forEach(cb -> cb.setSelected(true));
    }
    
    @FXML
    private void handleToutDeselectionner() {
        permissionCheckBoxes.values().forEach(cb -> cb.setSelected(false));
    }
    
    /**
     * Active/désactive l'édition des champs du rôle
     */
    private void setRoleFieldsEditable(boolean editable) {
        // Description éditable
        textAreaDescriptionRole.setEditable(editable);
        
        // Statut actif éditable
        checkRoleActif.setDisable(!editable);
        
        // Permissions éditables
        permissionCheckBoxes.values().forEach(cb -> cb.setDisable(!editable));
        
        // Boutons
        if (btnSauvegarderRole != null) btnSauvegarderRole.setDisable(!editable);
        if (btnAnnulerRole != null) btnAnnulerRole.setDisable(!editable);
        if (btnModifierRole != null) btnModifierRole.setDisable(editable);
    }
    
    private void clearRoleDetails() {
        selectedRole = null;
        champNomRole.clear();
        textAreaDescriptionRole.clear();
        checkRoleActif.setSelected(true);
        labelNombreUtilisateurs.setText("0 utilisateur(s) assigné(s)");
        permissionCheckBoxes.values().forEach(cb -> cb.setSelected(false));
        setRoleFieldsEditable(false);
        isEditingRole = false;
    }
    
    @FXML
    private void handleRoleDoubleClick(javafx.scene.input.MouseEvent event) {
        if (event.getClickCount() == 2) {
            Role selectedRole = listeRoles.getSelectionModel().getSelectedItem();
            if (selectedRole != null) {
                handleModifierRole();
            }
        }
    }
    
    // ===== GESTION DES LOGS =====
    
    @FXML
    private void handleFiltrerLogs() {
        applyLogFilters();
    }
    
    @FXML
    private void handleActualiserLogs() {
        loadLogs();
        updateActiviteRecente();
    }
    
    @FXML
    private void handleExporterLogs() {
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Exporter les logs");
            fileChooser.getExtensionFilters().add(
                new FileChooser.ExtensionFilter("Fichier CSV", "*.csv")
            );
            fileChooser.setInitialFileName("logs_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv");
            
            File file = fileChooser.showSaveDialog(tableauLogs.getScene().getWindow());
            
            if (file != null) {
                if (adminService.exportLogsToCSV(file, new ArrayList<>(filteredLogs))) {
                    AlertUtils.showInfo("Logs exportés avec succès: " + file.getName());
                    
                    String ip = networkService.getLocalIPAddress();
                    adminService.logAction(currentUser.getId(), "EXPORT_LOGS", 
                        "Export des logs d'activité", ip);
                } else {
                    AlertUtils.showError("Erreur lors de l'export des logs");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'export des logs: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur", e.getMessage());
        }
    }
    
    private void applyLogFilters() {
        filteredLogs.setPredicate(log -> {
            String typeAction = filtreTypeAction.getValue();
            if (typeAction != null && !typeAction.equals("Toutes")) {
                if (!log.getAction().toUpperCase().contains(typeAction.toUpperCase())) {
                    return false;
                }
            }
            
            String utilisateur = filtreUtilisateurLog.getValue();
            if (utilisateur != null && !utilisateur.equals("Tous")) {
                if (!utilisateur.equals(log.getUserCode())) {
                    return false;
                }
            }
            
            if (dateDebutLogs.getValue() != null) {
                if (log.getTimestamp().toLocalDate().isBefore(dateDebutLogs.getValue())) {
                    return false;
                }
            }
            
            if (dateFinLogs.getValue() != null) {
                if (log.getTimestamp().toLocalDate().isAfter(dateFinLogs.getValue())) {
                    return false;
                }
            }
            
            return true;
        });
    }
    
    // ===== UTILITAIRES =====
    
    private boolean hasAdminPermission() {
        return currentUser != null && 
               currentUser.getRole() != null && 
               currentUser.getRole().hasPermission(Permission.ADMIN_UTILISATEURS);
    }
}