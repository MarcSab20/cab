package application.controllers;

import application.models.User;
import application.services.DatabaseService;
import application.services.NetworkStorageService;
import application.services.NotificationCourrierService;
import application.services.LogService;
import application.utils.SessionManager;
import application.utils.AlertUtils;

import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.*;
import javafx.beans.property.SimpleStringProperty;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.sql.*;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur pour l'administration système - VERSION AMÉLIORÉE
 * Gestion des logs, connexions, configuration serveur réseau
 */
public class AdministrationController {
    
    // ==================== COMPOSANTS FXML ====================
    
    @FXML private TabPane tabPaneAdmin;
    
    // Onglet Configuration Serveur
    @FXML private TextField txtCheminServeur;
    @FXML private TextField txtAdresseServeur;
    @FXML private TextField txtPortServeur;
    @FXML private TextField txtUtilisateurServeur;
    @FXML private PasswordField txtMotDePasseServeur;
    @FXML private Button btnParcourirServeur;
    @FXML private CheckBox chkServeurActif;
    @FXML private CheckBox chkServeurDistant;
    @FXML private Button btnSauvegarderConfig;
    @FXML private Button btnTesterConnexion;
    @FXML private Label lblStatutServeur;
    @FXML private Label lblEspaceDisponible;
    @FXML private Label lblNombreFichiers;
    @FXML private Label lblTailleTotale;
    @FXML private ProgressBar progressBarEspace;
    @FXML private VBox panelServeurLocal;
    @FXML private VBox panelServeurDistant;
    
    // Onglet Logs
    @FXML private TableView<LogEntry> tableLogs;
    @FXML private TableColumn<LogEntry, String> colLogDate;
    @FXML private TableColumn<LogEntry, String> colLogUtilisateur;
    @FXML private TableColumn<LogEntry, String> colLogAction;
    @FXML private TableColumn<LogEntry, String> colLogDetails;
    @FXML private TableColumn<LogEntry, String> colLogIP;
    @FXML private TableColumn<LogEntry, String> colLogStatut;
    
    @FXML private DatePicker dateDebutLogs;
    @FXML private DatePicker dateFinLogs;
    @FXML private ComboBox<String> cmbTypeAction;
    @FXML private TextField txtRechercheLog;
    @FXML private Button btnRechercherLogs;
    @FXML private Button btnExporterLogs;
    @FXML private Button btnViderLogs;
    @FXML private Label lblTotalLogs;
    
    // Onglet Connexions
    @FXML private TableView<ConnexionEntry> tableConnexions;
    @FXML private TableColumn<ConnexionEntry, String> colConnexionDate;
    @FXML private TableColumn<ConnexionEntry, String> colConnexionUtilisateur;
    @FXML private TableColumn<ConnexionEntry, String> colConnexionRole;
    @FXML private TableColumn<ConnexionEntry, String> colConnexionType;
    @FXML private TableColumn<ConnexionEntry, String> colConnexionIP;
    @FXML private TableColumn<ConnexionEntry, String> colConnexionStatut;
    @FXML private TableColumn<ConnexionEntry, String> colConnexionDuree;
    
    @FXML private DatePicker dateDebutConnexions;
    @FXML private DatePicker dateFinConnexions;
    @FXML private ComboBox<String> cmbTypeConnexion;
    @FXML private TextField txtRechercheConnexion;
    @FXML private Button btnRechercherConnexions;
    @FXML private Button btnExporterConnexions;
    @FXML private Label lblTotalConnexions;
    @FXML private Label lblConnexionsReussies;
    @FXML private Label lblConnexionsEchouees;
    
    // Onglet Statistiques
    @FXML private Label lblTotalUtilisateurs;
    @FXML private Label lblUtilisateursActifs;
    @FXML private Label lblTotalDocuments;
    @FXML private Label lblTotalDossiers;
    @FXML private Label lblEspaceUtilise;
    
    @FXML private ComboBox<User> cmbResponsableCourrier;
    @FXML private Button btnDefinirResponsable;
    @FXML private Button btnSupprimerResponsable;
    @FXML private Button btnActualiserResponsable;
    @FXML private Label lblResponsableActuel;
    @FXML private Label lblTotalNotifications;
    @FXML private Label lblNotificationsNonLues;

    private NotificationCourrierService notificationService;
    
    // ==================== SERVICES ====================
    
    private DatabaseService databaseService;
    private NetworkStorageService networkStorageService;
    private LogService logService;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    
    // ==================== INITIALISATION ====================
    
    @FXML
    public void initialize() {
        System.out.println("=== INITIALISATION AdministrationController ===");
        
        databaseService = DatabaseService.getInstance();
        networkStorageService = NetworkStorageService.getInstance();
        logService = LogService.getInstance();
        notificationService = NotificationCourrierService.getInstance();
        
        // Vérifier les permissions STRICTEMENT
        if (!verifierPermissionsAdmin()) {
            afficherErreurAccesRefuse();
            return;
        }
        
        initialiserOngletConfiguration();
        initialiserOngletLogs();
        initialiserOngletConnexions();
        initialiserOngletStatistiques();
        initialiserOngletResponsable();
        
        // Logger l'accès à l'administration
        logService.logAction("acces_administration", "Accès au panneau d'administration");
        
        System.out.println("=== FIN INITIALISATION ===");
    }
    
    /**
     * Vérifie strictement que l'utilisateur est administrateur
     */
    private boolean verifierPermissionsAdmin() {
        User currentUser = SessionManager.getInstance().getCurrentUser();
        
        if (currentUser == null) {
            System.err.println("❌ Aucun utilisateur en session");
            return false;
        }
        
        // Vérification stricte du rôle administrateur
        if (currentUser.getRole() == null) {
            System.err.println("❌ Rôle utilisateur null");
            return false;
        }
        
        String roleCode = currentUser.getRole().getCode();
        String roleNom = currentUser.getRole().getNom();
        
        boolean estAdmin = "ADMIN".equalsIgnoreCase(roleCode) || 
                          "Administrateur".equalsIgnoreCase(roleNom) ||
                          currentUser.getRole().hasPermission("ADMIN_SYSTEME");
        
        if (!estAdmin) {
            System.err.println("❌ Utilisateur " + currentUser.getCode() + " n'est pas administrateur");
            logService.logAction("tentative_acces_admin_refuse", 
                "Tentative d'accès refusée pour " + currentUser.getCode());
        }
        
        return estAdmin;
    }
    
    /**
     * Affiche un message d'erreur et bloque l'accès
     */
    private void afficherErreurAccesRefuse() {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Accès refusé");
        alert.setHeaderText("Permissions insuffisantes");
        alert.setContentText("Seuls les administrateurs peuvent accéder à cette section.\n\n" +
                           "Cette tentative d'accès a été enregistrée dans les logs.");
        alert.showAndWait();
        
        // Désactiver tous les composants
        if (tabPaneAdmin != null) {
            tabPaneAdmin.setDisable(true);
        }
    }
    
    // ==================== CONFIGURATION SERVEUR ====================
    
    /**
     * Initialise l'onglet de configuration
     */
    private void initialiserOngletConfiguration() {
        // Charger la configuration actuelle
        chargerConfiguration();
        
        // Configurer le mode serveur (local/distant)
        if (chkServeurDistant != null) {
            chkServeurDistant.setOnAction(e -> basculerModeServeur());
        }
        
        // Configurer les listeners
        if (btnParcourirServeur != null) {
            btnParcourirServeur.setOnAction(e -> parcourirRepertoireServeur());
        }
        
        if (btnSauvegarderConfig != null) {
            btnSauvegarderConfig.setOnAction(e -> sauvegarderConfiguration());
        }
        
        if (btnTesterConnexion != null) {
            btnTesterConnexion.setOnAction(e -> testerConnexionServeur());
        }
    }
    
    /**
     * Bascule entre mode serveur local et distant
     */
    private void basculerModeServeur() {
        boolean estDistant = chkServeurDistant.isSelected();
        
        if (panelServeurLocal != null) {
            panelServeurLocal.setVisible(!estDistant);
            panelServeurLocal.setManaged(!estDistant);
        }
        
        if (panelServeurDistant != null) {
            panelServeurDistant.setVisible(estDistant);
            panelServeurDistant.setManaged(estDistant);
        }
    }
    
    /**
     * Charge la configuration du serveur
     */
    private void chargerConfiguration() {
        try {
            String query = "SELECT cle, valeur FROM config_serveur WHERE cle IN " +
                          "('serveur_stockage_actif', 'serveur_stockage_chemin', " +
                          "'serveur_distant', 'serveur_adresse', 'serveur_port', " +
                          "'serveur_utilisateur', 'serveur_mot_de_passe')";
            
            try (Connection conn = databaseService.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                
                while (rs.next()) {
                    String cle = rs.getString("cle");
                    String valeur = rs.getString("valeur");
                    
                    switch (cle) {
                        case "serveur_stockage_chemin":
                            if (txtCheminServeur != null) txtCheminServeur.setText(valeur);
                            break;
                        case "serveur_stockage_actif":
                            if (chkServeurActif != null) chkServeurActif.setSelected(Boolean.parseBoolean(valeur));
                            break;
                        case "serveur_distant":
                            if (chkServeurDistant != null) {
                                chkServeurDistant.setSelected(Boolean.parseBoolean(valeur));
                                basculerModeServeur();
                            }
                            break;
                        case "serveur_adresse":
                            if (txtAdresseServeur != null) txtAdresseServeur.setText(valeur);
                            break;
                        case "serveur_port":
                            if (txtPortServeur != null) txtPortServeur.setText(valeur);
                            break;
                        case "serveur_utilisateur":
                            if (txtUtilisateurServeur != null) txtUtilisateurServeur.setText(valeur);
                            break;
                        case "serveur_mot_de_passe":
                            if (txtMotDePasseServeur != null) txtMotDePasseServeur.setText(valeur);
                            break;
                    }
                }
            }
            
            // Charger les statistiques
            chargerStatistiquesServeur();
            
        } catch (SQLException e) {
            System.err.println("Erreur chargement configuration: " + e.getMessage());
            showError("Erreur", "Impossible de charger la configuration");
        }
    }
    
    /**
     * Charge les statistiques du serveur
     */
    private void chargerStatistiquesServeur() {
        Map<String, Object> stats = networkStorageService.getStatistiquesStockage();
        
        if (lblStatutServeur != null) {
            boolean actif = (boolean) stats.getOrDefault("actif", false);
            lblStatutServeur.setText(actif ? "🟢 Actif et opérationnel" : "🔴 Inactif");
            lblStatutServeur.setStyle(actif ? 
                "-fx-text-fill: #27ae60; -fx-font-weight: bold;" : 
                "-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        }
        
        if (lblNombreFichiers != null) {
            lblNombreFichiers.setText(String.format("%,d fichiers", stats.getOrDefault("nombreFichiers", 0)));
        }
        
        if (lblTailleTotale != null) {
            long taille = (long) stats.getOrDefault("tailleTotale", 0L);
            lblTailleTotale.setText(formatTaille(taille));
        }
        
        if (lblEspaceDisponible != null) {
            long espace = (long) stats.getOrDefault("espaceDisponible", 0L);
            lblEspaceDisponible.setText(formatTaille(espace));
        }
        
        // Calculer le pourcentage d'utilisation
        if (progressBarEspace != null) {
            long tailleTotale = (long) stats.getOrDefault("tailleTotale", 0L);
            long espaceDisponible = (long) stats.getOrDefault("espaceDisponible", 0L);
            
            if (espaceDisponible > 0) {
                double pourcentage = (double) tailleTotale / (tailleTotale + espaceDisponible);
                progressBarEspace.setProgress(pourcentage);
                
                // Changer la couleur selon le niveau
                if (pourcentage > 0.9) {
                    progressBarEspace.setStyle("-fx-accent: #e74c3c;"); // Rouge
                } else if (pourcentage > 0.7) {
                    progressBarEspace.setStyle("-fx-accent: #f39c12;"); // Orange
                } else {
                    progressBarEspace.setStyle("-fx-accent: #27ae60;"); // Vert
                }
            }
        }
    }
    
    /**
     * Parcourir pour sélectionner le répertoire serveur
     */
    private void parcourirRepertoireServeur() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Sélectionner le répertoire du serveur de stockage");
        
        File repertoire = dirChooser.showDialog(btnParcourirServeur.getScene().getWindow());
        
        if (repertoire != null) {
            txtCheminServeur.setText(repertoire.getAbsolutePath());
        }
    }
    
    /**
     * Teste la connexion au serveur
     */
    private void testerConnexionServeur() {
        try {
            boolean estDistant = chkServeurDistant != null && chkServeurDistant.isSelected();
            
            if (estDistant) {
                // Test de connexion réseau
                String adresse = txtAdresseServeur.getText();
                String port = txtPortServeur.getText();
                String utilisateur = txtUtilisateurServeur.getText();
                String motDePasse = txtMotDePasseServeur.getText();
                
                if (adresse == null || adresse.trim().isEmpty()) {
                    showWarning("Validation", "Veuillez saisir l'adresse du serveur");
                    return;
                }
                
                // Tester la connexion (vous devrez implémenter la logique réseau)
                boolean connexionReussie = networkStorageService.testerConnexionDistante(
                    adresse, port, utilisateur, motDePasse);
                
                if (connexionReussie) {
                    showSuccess("✅ Connexion au serveur distant réussie !\n\n" +
                               "Adresse: " + adresse + "\n" +
                               "Port: " + port);
                    logService.logAction("test_connexion_serveur", "Test connexion distant réussi: " + adresse);
                } else {
                    showError("Échec de connexion", 
                             "Impossible de se connecter au serveur distant.\n\n" +
                             "Vérifiez l'adresse, le port et les identifiants.");
                    logService.logAction("test_connexion_serveur", "Test connexion distant échoué: " + adresse);
                }
                
            } else {
                // Test de connexion locale
                String chemin = txtCheminServeur.getText();
                
                if (chemin == null || chemin.trim().isEmpty()) {
                    showWarning("Validation", "Veuillez saisir le chemin du serveur");
                    return;
                }
                
                File repertoire = new File(chemin);
                
                if (!repertoire.exists()) {
                    showWarning("Chemin invalide", 
                               "Le répertoire spécifié n'existe pas.\n\n" +
                               "Voulez-vous le créer ?");
                    return;
                }
                
                if (!repertoire.canRead() || !repertoire.canWrite()) {
                    showError("Permissions insuffisantes", 
                             "Le répertoire n'a pas les permissions de lecture/écriture nécessaires.");
                    return;
                }
                
                showSuccess("✅ Test de connexion réussi !\n\n" +
                           "Chemin: " + chemin + "\n" +
                           "Lecture: ✓\n" +
                           "Écriture: ✓");
                
                logService.logAction("test_connexion_serveur", "Test connexion local réussi: " + chemin);
            }
            
        } catch (Exception e) {
            System.err.println("Erreur test connexion: " + e.getMessage());
            showError("Erreur", "Erreur lors du test de connexion: " + e.getMessage());
        }
    }
    
    /**
     * Sauvegarde la configuration
     */
    private void sauvegarderConfiguration() {
        try {
            boolean estDistant = chkServeurDistant != null && chkServeurDistant.isSelected();
            String chemin = txtCheminServeur.getText();
            boolean actif = chkServeurActif.isSelected();
            
            // Validation
            if (!estDistant && (chemin == null || chemin.trim().isEmpty())) {
                showWarning("Validation", "Veuillez saisir le chemin du serveur");
                return;
            }
            
            if (estDistant) {
                String adresse = txtAdresseServeur.getText();
                if (adresse == null || adresse.trim().isEmpty()) {
                    showWarning("Validation", "Veuillez saisir l'adresse du serveur");
                    return;
                }
            }
            
            // Sauvegarder en base de données
            String query = "INSERT INTO config_serveur (cle, valeur, description) VALUES (?, ?, ?) " +
                          "ON DUPLICATE KEY UPDATE valeur = VALUES(valeur)";
            
            try (Connection conn = databaseService.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(query)) {
                
                // Serveur actif
                stmt.setString(1, "serveur_stockage_actif");
                stmt.setString(2, String.valueOf(actif));
                stmt.setString(3, "Activation du serveur de stockage");
                stmt.executeUpdate();
                
                // Chemin du serveur
                stmt.setString(1, "serveur_stockage_chemin");
                stmt.setString(2, chemin);
                stmt.setString(3, "Chemin du répertoire de stockage");
                stmt.executeUpdate();
                
                // Mode distant
                stmt.setString(1, "serveur_distant");
                stmt.setString(2, String.valueOf(estDistant));
                stmt.setString(3, "Utilisation d'un serveur distant");
                stmt.executeUpdate();
                
                if (estDistant) {
                    // Adresse serveur
                    stmt.setString(1, "serveur_adresse");
                    stmt.setString(2, txtAdresseServeur.getText());
                    stmt.setString(3, "Adresse IP ou nom d'hôte du serveur");
                    stmt.executeUpdate();
                    
                    // Port
                    stmt.setString(1, "serveur_port");
                    stmt.setString(2, txtPortServeur.getText());
                    stmt.setString(3, "Port de connexion");
                    stmt.executeUpdate();
                    
                    // Utilisateur
                    stmt.setString(1, "serveur_utilisateur");
                    stmt.setString(2, txtUtilisateurServeur.getText());
                    stmt.setString(3, "Nom d'utilisateur");
                    stmt.executeUpdate();
                    
                    // Mot de passe (encrypté dans une vraie application!)
                    stmt.setString(1, "serveur_mot_de_passe");
                    stmt.setString(2, txtMotDePasseServeur.getText());
                    stmt.setString(3, "Mot de passe");
                    stmt.executeUpdate();
                }
                
                showSuccess("✅ Configuration sauvegardée avec succès !");
                
                // Logger l'action
                logService.logAction("modification_config_serveur", 
                    "Configuration serveur modifiée - Mode: " + (estDistant ? "Distant" : "Local"));
                
                // Recharger la configuration dans le service
                networkStorageService.rechargerConfiguration();
                chargerStatistiquesServeur();
                
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur sauvegarde configuration: " + e.getMessage());
            showError("Erreur", "Impossible de sauvegarder la configuration");
        }
    }
    
    // =================== RESPONSABLE COURRIER ===================
    
    /**
     * Initialise l'onglet de configuration du responsable
	 */
	private void initialiserOngletResponsable() {
	    // Configurer le ComboBox
	    if (cmbResponsableCourrier != null) {
	        // Personnaliser l'affichage
	        cmbResponsableCourrier.setCellFactory(param -> new javafx.scene.control.ListCell<User>() {
	            @Override
	            protected void updateItem(User user, boolean empty) {
	                super.updateItem(user, empty);
	                if (empty || user == null) {
	                    setText(null);
	                } else {
	                    String roleNom = user.getRole() != null ? user.getRole().getNom() : "Sans rôle";
	                    setText(String.format("%s - %s (%s)", 
	                        user.getCode(), 
	                        user.getNomComplet(), 
	                        roleNom));
	                }
	            }
	        });
	        
	        cmbResponsableCourrier.setButtonCell(new javafx.scene.control.ListCell<User>() {
	            @Override
	            protected void updateItem(User user, boolean empty) {
	                super.updateItem(user, empty);
	                if (empty || user == null) {
	                    setText("Choisir un utilisateur...");
	                } else {
	                    setText(user.getCode() + " - " + user.getNomComplet());
	                }
	            }
	        });
	    }
	    
	    // Charger les données
	    chargerUtilisateursDisponibles();
	    chargerResponsableActuel();
	    chargerStatistiquesNotifications();
	    
	    // Configurer les boutons
	    if (btnDefinirResponsable != null) {
	        btnDefinirResponsable.setOnAction(e -> definirResponsable());
	    }
	    
	    if (btnSupprimerResponsable != null) {
	        btnSupprimerResponsable.setOnAction(e -> supprimerResponsable());
	    }
	    
	    if (btnActualiserResponsable != null) {
	        btnActualiserResponsable.setOnAction(e -> actualiserResponsable());
	    }
	}
	
	/**
	 * Charge la liste des utilisateurs disponibles
	 */
	private void chargerUtilisateursDisponibles() {
	    try {
	        List<User> users = notificationService.getTousUtilisateursActifs();
	        
	        if (cmbResponsableCourrier != null) {
	            cmbResponsableCourrier.setItems(
	                javafx.collections.FXCollections.observableArrayList(users));
	        }
	        
	        System.out.println("✓ " + users.size() + " utilisateurs chargés");
	        
	    } catch (Exception e) {
	        System.err.println("Erreur chargement utilisateurs: " + e.getMessage());
	        showError("Erreur", "Impossible de charger la liste des utilisateurs");
	    }
	}
	
	/**
	 * Charge le responsable actuel
	 */
	private void chargerResponsableActuel() {
	    try {
	        User responsable = notificationService.getResponsableCourrier();
	        
	        if (lblResponsableActuel != null) {
	            if (responsable != null) {
	                String roleNom = responsable.getRole() != null ? 
	                    responsable.getRole().getNom() : "Sans rôle";
	                
	                lblResponsableActuel.setText(String.format("✅ %s - %s (%s)", 
	                    responsable.getCode(), 
	                    responsable.getNomComplet(), 
	                    roleNom));
	                lblResponsableActuel.setStyle(
	                    "-fx-text-fill: #27ae60; -fx-font-weight: bold;");
	            } else {
	                lblResponsableActuel.setText("❌ Aucun responsable configuré");
	                lblResponsableActuel.setStyle(
	                    "-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
	            }
	        }
	        
	    } catch (Exception e) {
	        System.err.println("Erreur chargement responsable: " + e.getMessage());
	    }
	}
	
	/*
	 * Charge les statistiques des notifications
	 */
	private void chargerStatistiquesNotifications() {
	    try {
	        User responsable = notificationService.getResponsableCourrier();
	        
	        if (responsable != null) {
	            // Total des notifications
	            List<application.models.CourrierNotificationInfo> courriersNotifies = 
	                notificationService.getCourriersNotifiesAvecInfos(responsable.getId(), false);
	            
	            if (lblTotalNotifications != null) {
	                lblTotalNotifications.setText(String.valueOf(courriersNotifies.size()));
	            }
	            
	            // Notifications non lues
	            int nonLus = notificationService.compterCourriersNonLus(responsable.getId());
	            
	            if (lblNotificationsNonLues != null) {
	                lblNotificationsNonLues.setText(String.valueOf(nonLus));
	            }
	        } else {
	            if (lblTotalNotifications != null) {
	                lblTotalNotifications.setText("0");
	            }
	            if (lblNotificationsNonLues != null) {
	                lblNotificationsNonLues.setText("0");
	            }
	        }
	        
	    } catch (Exception e) {
	        System.err.println("Erreur chargement statistiques: " + e.getMessage());
	    }
	}
	
	/**
	 * Définit le responsable sélectionné
	 */
	private void definirResponsable() {
	    User selectedUser = cmbResponsableCourrier.getValue();
	    
	    if (selectedUser == null) {
	        showWarning("Validation", "Veuillez sélectionner un utilisateur");
	        return;
	    }
	    
	    // Confirmation
	    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
	    confirm.setTitle("Confirmation");
	    confirm.setHeaderText("Définir le responsable des courriers");
	    confirm.setContentText(String.format(
	        "Voulez-vous définir %s comme responsable des courriers ?\n\n" +
	        "Cette personne recevra automatiquement tous les nouveaux courriers créés.",
	        selectedUser.getNomComplet()));
	    
	    if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
	        try {
	            boolean success = notificationService.setResponsableCourrier(selectedUser.getId());
	            
	            if (success) {
	                showSuccess(String.format("✅ %s défini comme responsable des courriers", 
	                    selectedUser.getNomComplet()));
	                
	                logService.logAction("definition_responsable_courrier", 
	                    "Responsable défini: " + selectedUser.getCode());
	                
	                actualiserResponsable();
	            } else {
	                showError("Erreur", "Impossible de définir le responsable");
	            }
	            
	        } catch (Exception e) {
	            System.err.println("Erreur définition responsable: " + e.getMessage());
	            showError("Erreur", "Erreur lors de la définition du responsable");
	        }
	    }
	}
	
	/**
	 * Supprime le responsable actuel
	 */
	private void supprimerResponsable() {
	    User responsableActuel = notificationService.getResponsableCourrier();
	    
	    if (responsableActuel == null) {
	        showInfo("Aucun responsable configuré");
	        return;
	    }
	    
	    // Confirmation
	    Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
	    confirm.setTitle("Confirmation");
	    confirm.setHeaderText("Supprimer le responsable des courriers");
	    confirm.setContentText(String.format(
	        "Voulez-vous supprimer %s comme responsable des courriers ?\n\n" +
	        "Les nouveaux courriers ne seront plus notifiés automatiquement.",
	        responsableActuel.getNomComplet()));
	    
	    if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
	        try {
	            boolean success = notificationService.supprimerResponsable();
	            
	            if (success) {
	                showSuccess("✅ Responsable supprimé avec succès");
	                
	                logService.logAction("suppression_responsable_courrier", 
	                    "Responsable supprimé: " + responsableActuel.getCode());
	                
	                actualiserResponsable();
	            } else {
	                showError("Erreur", "Impossible de supprimer le responsable");
	            }
	            
	        } catch (Exception e) {
	            System.err.println("Erreur suppression responsable: " + e.getMessage());
	            showError("Erreur", "Erreur lors de la suppression du responsable");
	        }
	    }
	}
	
	/**
	 * Actualise les informations du responsable
	 */
	private void actualiserResponsable() {
	    chargerUtilisateursDisponibles();
	    chargerResponsableActuel();
	    chargerStatistiquesNotifications();
	    showInfo("Informations actualisées");
	}

    
    // ==================== GESTION DES LOGS ====================
    
    /**
     * Initialise l'onglet des logs
     */
    private void initialiserOngletLogs() {
        if (tableLogs != null) {
            colLogDate.setCellValueFactory(new PropertyValueFactory<>("date"));
            colLogUtilisateur.setCellValueFactory(new PropertyValueFactory<>("utilisateur"));
            colLogAction.setCellValueFactory(new PropertyValueFactory<>("action"));
            colLogDetails.setCellValueFactory(new PropertyValueFactory<>("details"));
            colLogIP.setCellValueFactory(new PropertyValueFactory<>("ipAddress"));
            colLogStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
            
            chargerLogs();
        }
        
        if (cmbTypeAction != null) {
            cmbTypeAction.setItems(FXCollections.observableArrayList(
                "Tous", "Connexion", "Déconnexion", "Création document", 
                "Modification document", "Suppression document", "Accès refusé",
                "Configuration", "Erreur"
            ));
            cmbTypeAction.setValue("Tous");
        }
        
        if (btnRechercherLogs != null) {
            btnRechercherLogs.setOnAction(e -> rechercherLogs());
        }
        
        if (btnExporterLogs != null) {
            btnExporterLogs.setOnAction(e -> exporterLogs());
        }
        
        if (btnViderLogs != null) {
            btnViderLogs.setOnAction(e -> viderLogsAnciens());
        }
    }
    
    /**
     * Charge les logs d'activité
     */
    private void chargerLogs() {
        List<LogEntry> logs = new ArrayList<>();
        
        String query = "SELECT la.*, u.nom, u.prenom, u.code FROM logs_activite la " +
                      "LEFT JOIN users u ON la.user_id = u.id " +
                      "ORDER BY la.timestamp DESC LIMIT 1000";
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                LogEntry log = new LogEntry();
                
                Timestamp timestamp = rs.getTimestamp("timestamp");
                if (timestamp != null) {
                    log.setDate(timestamp.toLocalDateTime().format(DATE_TIME_FORMATTER));
                }
                
                String code = rs.getString("code");
                String nom = rs.getString("nom");
                String prenom = rs.getString("prenom");
                log.setUtilisateur(code != null ? code + " - " + prenom + " " + nom : "Système");
                
                log.setAction(rs.getString("action"));
                log.setDetails(rs.getString("details"));
                log.setIpAddress(rs.getString("ip_address"));
                
                String statut = rs.getString("statut");
                log.setStatut(statut != null ? statut : "info");
                
                logs.add(log);
            }
            
            tableLogs.setItems(FXCollections.observableArrayList(logs));
            
            if (lblTotalLogs != null) {
                lblTotalLogs.setText(logs.size() + " entrées chargées");
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur chargement logs: " + e.getMessage());
        }
    }
    
    /**
     * Recherche dans les logs
     */
    private void rechercherLogs() {
        List<LogEntry> logs = new ArrayList<>();
        
        StringBuilder query = new StringBuilder(
            "SELECT la.*, u.nom, u.prenom, u.code FROM logs_activite la " +
            "LEFT JOIN users u ON la.user_id = u.id WHERE 1=1");
        
        List<Object> params = new ArrayList<>();
        
        // Filtre par dates
        if (dateDebutLogs != null && dateDebutLogs.getValue() != null) {
            query.append(" AND DATE(la.timestamp) >= ?");
            params.add(Date.valueOf(dateDebutLogs.getValue()));
        }
        
        if (dateFinLogs != null && dateFinLogs.getValue() != null) {
            query.append(" AND DATE(la.timestamp) <= ?");
            params.add(Date.valueOf(dateFinLogs.getValue()));
        }
        
        // Filtre par type d'action
        if (cmbTypeAction != null && !"Tous".equals(cmbTypeAction.getValue())) {
            String action = cmbTypeAction.getValue().toLowerCase().replace(" ", "_");
            query.append(" AND la.action LIKE ?");
            params.add("%" + action + "%");
        }
        
        // Filtre par recherche textuelle
        if (txtRechercheLog != null && !txtRechercheLog.getText().trim().isEmpty()) {
            query.append(" AND (la.details LIKE ? OR u.nom LIKE ? OR u.prenom LIKE ?)");
            String recherche = "%" + txtRechercheLog.getText().trim() + "%";
            params.add(recherche);
            params.add(recherche);
            params.add(recherche);
        }
        
        query.append(" ORDER BY la.timestamp DESC LIMIT 1000");
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query.toString())) {
            
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    LogEntry log = new LogEntry();
                    
                    Timestamp timestamp = rs.getTimestamp("timestamp");
                    if (timestamp != null) {
                        log.setDate(timestamp.toLocalDateTime().format(DATE_TIME_FORMATTER));
                    }
                    
                    String code = rs.getString("code");
                    String nom = rs.getString("nom");
                    String prenom = rs.getString("prenom");
                    log.setUtilisateur(code != null ? code + " - " + prenom + " " + nom : "Système");
                    
                    log.setAction(rs.getString("action"));
                    log.setDetails(rs.getString("details"));
                    log.setIpAddress(rs.getString("ip_address"));
                    
                    String statut = rs.getString("statut");
                    log.setStatut(statut != null ? statut : "info");
                    
                    logs.add(log);
                }
            }
            
            tableLogs.setItems(FXCollections.observableArrayList(logs));
            
            if (lblTotalLogs != null) {
                lblTotalLogs.setText(logs.size() + " résultats trouvés");
            }
            
            showInfo(logs.size() + " entrées correspondantes trouvées");
            
        } catch (SQLException e) {
            System.err.println("Erreur recherche logs: " + e.getMessage());
            showError("Erreur", "Erreur lors de la recherche dans les logs");
        }
    }
    
    /**
     * Exporte les logs vers un fichier CSV
     */
    private void exporterLogs() {
        try {
            String nomFichier = "logs_export_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
            
            File fichier = new File(System.getProperty("user.home") + File.separator + nomFichier);
            
            try (java.io.PrintWriter writer = new java.io.PrintWriter(fichier)) {
                // En-têtes
                writer.println("Date,Utilisateur,Action,Détails,Adresse IP,Statut");
                
                // Données
                for (LogEntry log : tableLogs.getItems()) {
                    writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                        log.getDate(),
                        log.getUtilisateur(),
                        log.getAction(),
                        log.getDetails(),
                        log.getIpAddress(),
                        log.getStatut());
                }
            }
            
            showSuccess("✅ Logs exportés avec succès !\n\nFichier: " + fichier.getAbsolutePath());
            logService.logAction("export_logs", "Export des logs vers " + nomFichier);
            
        } catch (Exception e) {
            System.err.println("Erreur export logs: " + e.getMessage());
            showError("Erreur", "Impossible d'exporter les logs");
        }
    }
    
    /**
     * Vide les logs anciens (> 3 mois)
     */
    private void viderLogsAnciens() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Vider les logs anciens");
        confirm.setContentText("Cette action supprimera tous les logs de plus de 3 mois.\n\n" +
                              "Voulez-vous continuer ?");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                String query = "DELETE FROM logs_activite WHERE timestamp < DATE_SUB(NOW(), INTERVAL 3 MONTH)";
                
                try (Connection conn = databaseService.getConnection();
                     Statement stmt = conn.createStatement()) {
                    
                    int deleted = stmt.executeUpdate(query);
                    showSuccess(deleted + " entrées supprimées");
                    logService.logAction("vidage_logs", deleted + " logs supprimés (> 3 mois)");
                    chargerLogs();
                }
                
            } catch (SQLException e) {
                showError("Erreur", "Impossible de vider les logs");
            }
        }
    }
    
    // ==================== GESTION DES CONNEXIONS ====================
    
    /**
     * Initialise l'onglet des connexions
     */
    private void initialiserOngletConnexions() {
        if (tableConnexions != null) {
            colConnexionDate.setCellValueFactory(new PropertyValueFactory<>("date"));
            colConnexionUtilisateur.setCellValueFactory(new PropertyValueFactory<>("utilisateur"));
            colConnexionRole.setCellValueFactory(new PropertyValueFactory<>("role"));
            colConnexionType.setCellValueFactory(new PropertyValueFactory<>("type"));
            colConnexionIP.setCellValueFactory(new PropertyValueFactory<>("ipAddress"));
            colConnexionStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
            colConnexionDuree.setCellValueFactory(new PropertyValueFactory<>("duree"));
            
            chargerConnexions();
        }
        
        if (cmbTypeConnexion != null) {
            cmbTypeConnexion.setItems(FXCollections.observableArrayList(
                "Tous", "Connexion", "Déconnexion", "Tentative échouée"
            ));
            cmbTypeConnexion.setValue("Tous");
        }
        
        if (btnRechercherConnexions != null) {
            btnRechercherConnexions.setOnAction(e -> rechercherConnexions());
        }
        
        if (btnExporterConnexions != null) {
            btnExporterConnexions.setOnAction(e -> exporterConnexions());
        }
        
        // Charger les statistiques
        chargerStatistiquesConnexions();
    }
    
    /**
     * Charge les connexions
     */
    private void chargerConnexions() {
        List<ConnexionEntry> connexions = new ArrayList<>();
        
        String query = "SELECT la.*, u.nom, u.prenom, u.code, r.nom as role_nom FROM logs_activite la " +
                      "LEFT JOIN users u ON la.user_id = u.id " +
                      "LEFT JOIN roles r ON u.role_id = r.id " +
                      "WHERE la.action IN ('connexion', 'deconnexion', 'tentative_connexion') " +
                      "ORDER BY la.timestamp DESC LIMIT 500";
        
        try (Connection conn = databaseService.getConnection();
             Statement stmt = conn.createStatement();
             ResultSet rs = stmt.executeQuery(query)) {
            
            while (rs.next()) {
                ConnexionEntry connexion = new ConnexionEntry();
                
                Timestamp timestamp = rs.getTimestamp("timestamp");
                if (timestamp != null) {
                    connexion.setDate(timestamp.toLocalDateTime().format(DATE_TIME_FORMATTER));
                }
                
                String code = rs.getString("code");
                String nom = rs.getString("nom");
                String prenom = rs.getString("prenom");
                connexion.setUtilisateur(code + " - " + prenom + " " + nom);
                connexion.setRole(rs.getString("role_nom"));
                
                String action = rs.getString("action");
                connexion.setType(action);
                
                if ("connexion".equals(action)) {
                    connexion.setStatut("✅ Réussi");
                } else if ("deconnexion".equals(action)) {
                    connexion.setStatut("🚪 Déconnexion");
                } else {
                    connexion.setStatut("❌ Échec");
                }
                
                connexion.setIpAddress(rs.getString("ip_address"));
                connexion.setDuree("N/A"); // À calculer si nécessaire
                
                connexions.add(connexion);
            }
            
            tableConnexions.setItems(FXCollections.observableArrayList(connexions));
            
            if (lblTotalConnexions != null) {
                lblTotalConnexions.setText(String.valueOf(connexions.size()));
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur chargement connexions: " + e.getMessage());
        }
    }
    
    /**
     * Charge les statistiques de connexions
     */
    private void chargerStatistiquesConnexions() {
        try {
            String query = "SELECT " +
                          "SUM(CASE WHEN action = 'connexion' THEN 1 ELSE 0 END) as reussies, " +
                          "SUM(CASE WHEN action = 'tentative_connexion' THEN 1 ELSE 0 END) as echouees " +
                          "FROM logs_activite " +
                          "WHERE action IN ('connexion', 'tentative_connexion') " +
                          "AND DATE(timestamp) = CURDATE()";
            
            try (Connection conn = databaseService.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                
                if (rs.next()) {
                    if (lblConnexionsReussies != null) {
                        lblConnexionsReussies.setText(String.valueOf(rs.getInt("reussies")));
                    }
                    if (lblConnexionsEchouees != null) {
                        lblConnexionsEchouees.setText(String.valueOf(rs.getInt("echouees")));
                    }
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur chargement stats connexions: " + e.getMessage());
        }
    }
    
    /**
     * Recherche des connexions
     */
    private void rechercherConnexions() {
        List<ConnexionEntry> connexions = new ArrayList<>();
        
        StringBuilder query = new StringBuilder(
            "SELECT la.*, u.nom, u.prenom, u.code, r.nom as role_nom FROM logs_activite la " +
            "LEFT JOIN users u ON la.user_id = u.id " +
            "LEFT JOIN roles r ON u.role_id = r.id " +
            "WHERE la.action IN ('connexion', 'deconnexion', 'tentative_connexion')");
        
        List<Object> params = new ArrayList<>();
        
        // Filtre par dates
        if (dateDebutConnexions != null && dateDebutConnexions.getValue() != null) {
            query.append(" AND DATE(la.timestamp) >= ?");
            params.add(Date.valueOf(dateDebutConnexions.getValue()));
        }
        
        if (dateFinConnexions != null && dateFinConnexions.getValue() != null) {
            query.append(" AND DATE(la.timestamp) <= ?");
            params.add(Date.valueOf(dateFinConnexions.getValue()));
        }
        
        // Filtre par type
        if (cmbTypeConnexion != null && !"Tous".equals(cmbTypeConnexion.getValue())) {
            String type = cmbTypeConnexion.getValue().toLowerCase();
            if ("tentative échouée".equals(type)) {
                query.append(" AND la.action = 'tentative_connexion'");
            } else {
                query.append(" AND la.action = ?");
                params.add(type);
            }
        }
        
        // Filtre par recherche
        if (txtRechercheConnexion != null && !txtRechercheConnexion.getText().trim().isEmpty()) {
            query.append(" AND (u.nom LIKE ? OR u.prenom LIKE ? OR u.code LIKE ?)");
            String recherche = "%" + txtRechercheConnexion.getText().trim() + "%";
            params.add(recherche);
            params.add(recherche);
            params.add(recherche);
        }
        
        query.append(" ORDER BY la.timestamp DESC LIMIT 500");
        
        try (Connection conn = databaseService.getConnection();
             PreparedStatement stmt = conn.prepareStatement(query.toString())) {
            
            for (int i = 0; i < params.size(); i++) {
                stmt.setObject(i + 1, params.get(i));
            }
            
            try (ResultSet rs = stmt.executeQuery()) {
                while (rs.next()) {
                    ConnexionEntry connexion = new ConnexionEntry();
                    
                    Timestamp timestamp = rs.getTimestamp("timestamp");
                    if (timestamp != null) {
                        connexion.setDate(timestamp.toLocalDateTime().format(DATE_TIME_FORMATTER));
                    }
                    
                    String code = rs.getString("code");
                    String nom = rs.getString("nom");
                    String prenom = rs.getString("prenom");
                    connexion.setUtilisateur(code + " - " + prenom + " " + nom);
                    connexion.setRole(rs.getString("role_nom"));
                    
                    String action = rs.getString("action");
                    connexion.setType(action);
                    
                    if ("connexion".equals(action)) {
                        connexion.setStatut("✅ Réussi");
                    } else if ("deconnexion".equals(action)) {
                        connexion.setStatut("🚪 Déconnexion");
                    } else {
                        connexion.setStatut("❌ Échec");
                    }
                    
                    connexion.setIpAddress(rs.getString("ip_address"));
                    connexion.setDuree("N/A");
                    
                    connexions.add(connexion);
                }
            }
            
            tableConnexions.setItems(FXCollections.observableArrayList(connexions));
            showInfo(connexions.size() + " connexions trouvées");
            
        } catch (SQLException e) {
            System.err.println("Erreur recherche connexions: " + e.getMessage());
        }
    }
    
    /**
     * Exporte les connexions
     */
    private void exporterConnexions() {
        try {
            String nomFichier = "connexions_export_" + 
                LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss")) + ".csv";
            
            File fichier = new File(System.getProperty("user.home") + File.separator + nomFichier);
            
            try (java.io.PrintWriter writer = new java.io.PrintWriter(fichier)) {
                // En-têtes
                writer.println("Date,Utilisateur,Rôle,Type,Adresse IP,Statut,Durée");
                
                // Données
                for (ConnexionEntry conn : tableConnexions.getItems()) {
                    writer.printf("\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\",\"%s\"%n",
                        conn.getDate(),
                        conn.getUtilisateur(),
                        conn.getRole(),
                        conn.getType(),
                        conn.getIpAddress(),
                        conn.getStatut(),
                        conn.getDuree());
                }
            }
            
            showSuccess("✅ Connexions exportées avec succès !\n\nFichier: " + fichier.getAbsolutePath());
            logService.logAction("export_connexions", "Export des connexions vers " + nomFichier);
            
        } catch (Exception e) {
            System.err.println("Erreur export connexions: " + e.getMessage());
            showError("Erreur", "Impossible d'exporter les connexions");
        }
    }
    
    // ==================== STATISTIQUES GLOBALES ====================
    
    /**
     * Initialise l'onglet des statistiques
     */
    private void initialiserOngletStatistiques() {
        chargerStatistiquesGlobales();
    }
    
    /**
     * Charge les statistiques globales
     */
    private void chargerStatistiquesGlobales() {
        try (Connection conn = databaseService.getConnection()) {
            
            // Total utilisateurs
            String query = "SELECT COUNT(*) as total FROM users";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                if (rs.next() && lblTotalUtilisateurs != null) {
                    lblTotalUtilisateurs.setText(String.valueOf(rs.getInt("total")));
                }
            }
            
            // Utilisateurs actifs
            query = "SELECT COUNT(*) as total FROM users WHERE actif = TRUE";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                if (rs.next() && lblUtilisateursActifs != null) {
                    lblUtilisateursActifs.setText(String.valueOf(rs.getInt("total")));
                }
            }
            
            // Total documents
            query = "SELECT COUNT(*) as total FROM documents WHERE statut != 'supprime'";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                if (rs.next() && lblTotalDocuments != null) {
                    lblTotalDocuments.setText(String.format("%,d", rs.getInt("total")));
                }
            }
            
            // Total dossiers
            query = "SELECT COUNT(*) as total FROM dossiers WHERE actif = TRUE";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                if (rs.next() && lblTotalDossiers != null) {
                    lblTotalDossiers.setText(String.valueOf(rs.getInt("total")));
                }
            }
            
            // Espace utilisé
            query = "SELECT SUM(taille_fichier) as total FROM documents WHERE statut != 'supprime'";
            try (Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                if (rs.next() && lblEspaceUtilise != null) {
                    long taille = rs.getLong("total");
                    lblEspaceUtilise.setText(formatTaille(taille));
                }
            }
            
        } catch (SQLException e) {
            System.err.println("Erreur chargement statistiques: " + e.getMessage());
        }
    }
    
    // ==================== CLASSES INTERNES ====================
    
    /**
     * Classe pour représenter une entrée de log
     */
    public static class LogEntry {
        private String date;
        private String utilisateur;
        private String action;
        private String details;
        private String ipAddress;
        private String statut;
        
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        
        public String getUtilisateur() { return utilisateur; }
        public void setUtilisateur(String utilisateur) { this.utilisateur = utilisateur; }
        
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        
        public String getDetails() { return details; }
        public void setDetails(String details) { this.details = details; }
        
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
        
        public String getStatut() { return statut; }
        public void setStatut(String statut) { this.statut = statut; }
    }
    
    /**
     * Classe pour représenter une entrée de connexion
     */
    public static class ConnexionEntry {
        private String date;
        private String utilisateur;
        private String role;
        private String type;
        private String ipAddress;
        private String statut;
        private String duree;
        
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        
        public String getUtilisateur() { return utilisateur; }
        public void setUtilisateur(String utilisateur) { this.utilisateur = utilisateur; }
        
        public String getRole() { return role; }
        public void setRole(String role) { this.role = role; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
        
        public String getStatut() { return statut; }
        public void setStatut(String statut) { this.statut = statut; }
        
        public String getDuree() { return duree; }
        public void setDuree(String duree) { this.duree = duree; }
    }
    
    // ==================== MÉTHODES UTILITAIRES ====================
    
    /**
     * Formate une taille en octets
     */
    private String formatTaille(long octets) {
        if (octets == 0) return "0 B";
        
        String[] unites = {"B", "KB", "MB", "GB", "TB"};
        int uniteIndex = 0;
        double taille = octets;
        
        while (taille >= 1024 && uniteIndex < unites.length - 1) {
            taille /= 1024;
            uniteIndex++;
        }
        
        return String.format("%.2f %s", taille, unites[uniteIndex]);
    }
    
    private void showError(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
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