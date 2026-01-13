package application.controllers;

import application.models.User;
import application.services.DatabaseService;
import application.services.NetworkStorageService;
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
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Contrôleur pour l'administration système
 * Gestion des logs, configuration serveur, etc.
 */
public class AdministrationController {
    
    // ==================== COMPOSANTS FXML ====================
    
    @FXML private TabPane tabPaneAdmin;
    
    // Onglet Configuration Serveur
    @FXML private TextField txtCheminServeur;
    @FXML private Button btnParcourirServeur;
    @FXML private CheckBox chkServeurActif;
    @FXML private Button btnSauvegarderConfig;
    @FXML private Button btnTesterConnexion;
    @FXML private Label lblStatutServeur;
    @FXML private Label lblEspaceDisponible;
    @FXML private Label lblNombreFichiers;
    @FXML private Label lblTailleTotale;
    
    // Onglet Logs
    @FXML private TableView<LogEntry> tableLogs;
    @FXML private TableColumn<LogEntry, String> colLogDate;
    @FXML private TableColumn<LogEntry, String> colLogUtilisateur;
    @FXML private TableColumn<LogEntry, String> colLogAction;
    @FXML private TableColumn<LogEntry, String> colLogDetails;
    @FXML private TableColumn<LogEntry, String> colLogIP;
    
    @FXML private DatePicker dateDebutLogs;
    @FXML private DatePicker dateFinLogs;
    @FXML private ComboBox<String> cmbTypeAction;
    @FXML private TextField txtRechercheLog;
    @FXML private Button btnRechercherLogs;
    @FXML private Button btnExporterLogs;
    @FXML private Button btnViderLogs;
    
    // Onglet Connexions
    @FXML private TableView<ConnexionEntry> tableConnexions;
    @FXML private TableColumn<ConnexionEntry, String> colConnexionDate;
    @FXML private TableColumn<ConnexionEntry, String> colConnexionUtilisateur;
    @FXML private TableColumn<ConnexionEntry, String> colConnexionType;
    @FXML private TableColumn<ConnexionEntry, String> colConnexionIP;
    @FXML private TableColumn<ConnexionEntry, String> colConnexionStatut;
    
    @FXML private DatePicker dateDebutConnexions;
    @FXML private DatePicker dateFinConnexions;
    @FXML private ComboBox<String> cmbTypeConnexion;
    @FXML private Button btnRechercherConnexions;
    @FXML private Button btnExporterConnexions;
    
    // ==================== SERVICES ====================
    
    private DatabaseService databaseService;
    private NetworkStorageService networkStorageService;
    private static final DateTimeFormatter DATE_TIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");
    
    // ==================== INITIALISATION ====================
    
    @FXML
    public void initialize() {
        System.out.println("=== INITIALISATION AdministrationController ===");
        
        databaseService = DatabaseService.getInstance();
        networkStorageService = NetworkStorageService.getInstance();
        
        // Vérifier les permissions
        User currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null || !currentUser.getRole().getNom().equals("Administrateur")) {
            showError("Accès refusé", "Seul un administrateur peut accéder à cette section");
            return;
        }
        
        initialiserOngletConfiguration();
        initialiserOngletLogs();
        initialiserOngletConnexions();
        
        System.out.println("=== FIN INITIALISATION ===");
    }
    
    // ==================== CONFIGURATION SERVEUR ====================
    
    /**
     * Initialise l'onglet de configuration
     */
    private void initialiserOngletConfiguration() {
        // Charger la configuration actuelle
        chargerConfiguration();
        
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
     * Charge la configuration du serveur
     */
    private void chargerConfiguration() {
        try {
            // Récupérer la configuration depuis la base de données
            String query = "SELECT cle, valeur FROM config_serveur WHERE cle IN ('serveur_stockage_actif', 'serveur_stockage_chemin')";
            
            try (Connection conn = databaseService.getConnection();
                 Statement stmt = conn.createStatement();
                 ResultSet rs = stmt.executeQuery(query)) {
                
                while (rs.next()) {
                    String cle = rs.getString("cle");
                    String valeur = rs.getString("valeur");
                    
                    if ("serveur_stockage_chemin".equals(cle) && txtCheminServeur != null) {
                        txtCheminServeur.setText(valeur);
                    } else if ("serveur_stockage_actif".equals(cle) && chkServeurActif != null) {
                        chkServeurActif.setSelected(Boolean.parseBoolean(valeur));
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
            lblStatutServeur.setText(actif ? "🟢 Actif" : "🔴 Inactif");
            lblStatutServeur.setStyle(actif ? "-fx-text-fill: green;" : "-fx-text-fill: red;");
        }
        
        if (lblNombreFichiers != null) {
            lblNombreFichiers.setText(String.valueOf(stats.getOrDefault("nombreFichiers", 0)));
        }
        
        if (lblTailleTotale != null) {
            long taille = (long) stats.getOrDefault("tailleTotale", 0L);
            lblTailleTotale.setText(formatTaille(taille));
        }
        
        if (lblEspaceDisponible != null) {
            long espace = (long) stats.getOrDefault("espaceDisponible", 0L);
            lblEspaceDisponible.setText(formatTaille(espace));
        }
    }
    
    /**
     * Parcourir pour sélectionner le répertoire serveur
     */
    private void parcourirRepertoireServeur() {
        DirectoryChooser dirChooser = new DirectoryChooser();
        dirChooser.setTitle("Sélectionner le répertoire du serveur");
        
        File repertoire = dirChooser.showDialog(btnParcourirServeur.getScene().getWindow());
        
        if (repertoire != null) {
            txtCheminServeur.setText(repertoire.getAbsolutePath());
        }
    }
    
    /**
     * Sauvegarde la configuration
     */
    private void sauvegarderConfiguration() {
        try {
            String chemin = txtCheminServeur.getText();
            boolean actif = chkServeurActif.isSelected();
            
            // Validation
            if (chemin == null || chemin.trim().isEmpty()) {
                showError("Erreur", "Le chemin du serveur est obligatoire");
                return;
            }
            
            // Sauvegarder dans la base
            String updateQuery = "INSERT INTO config_serveur (cle, valeur) VALUES (?, ?) " +
                               "ON DUPLICATE KEY UPDATE valeur = VALUES(valeur)";
            
            try (Connection conn = databaseService.getConnection();
                 PreparedStatement stmt = conn.prepareStatement(updateQuery)) {
                
                // Chemin
                stmt.setString(1, "serveur_stockage_chemin");
                stmt.setString(2, chemin);
                stmt.executeUpdate();
                
                // Actif
                stmt.setString(1, "serveur_stockage_actif");
                stmt.setString(2, String.valueOf(actif));
                stmt.executeUpdate();
            }
            
            showSuccess("Configuration sauvegardée avec succès");
            
            // Recharger la configuration
            chargerConfiguration();
            
        } catch (SQLException e) {
            System.err.println("Erreur sauvegarde configuration: " + e.getMessage());
            showError("Erreur", "Impossible de sauvegarder la configuration");
        }
    }
    
    /**
     * Teste la connexion au serveur
     */
    private void testerConnexionServeur() {
        String chemin = txtCheminServeur.getText();
        
        if (chemin == null || chemin.trim().isEmpty()) {
            showError("Erreur", "Veuillez saisir un chemin");
            return;
        }
        
        File repertoire = new File(chemin);
        
        if (!repertoire.exists()) {
            showError("Erreur", "Le répertoire n'existe pas");
            return;
        }
        
        if (!repertoire.isDirectory()) {
            showError("Erreur", "Le chemin ne pointe pas vers un répertoire");
            return;
        }
        
        if (!repertoire.canRead() || !repertoire.canWrite()) {
            showWarning("Avertissement", "Permissions insuffisantes sur le répertoire");
            return;
        }
        
        showSuccess("Connexion au serveur réussie !");
    }
    
    // ==================== GESTION DES LOGS ====================
    
    /**
     * Initialise l'onglet des logs
     */
    private void initialiserOngletLogs() {
        if (tableLogs != null) {
            // Configuration des colonnes
            colLogDate.setCellValueFactory(new PropertyValueFactory<>("date"));
            colLogUtilisateur.setCellValueFactory(new PropertyValueFactory<>("utilisateur"));
            colLogAction.setCellValueFactory(new PropertyValueFactory<>("action"));
            colLogDetails.setCellValueFactory(new PropertyValueFactory<>("details"));
            colLogIP.setCellValueFactory(new PropertyValueFactory<>("ipAddress"));
            
            // Charger les logs
            chargerLogs();
        }
        
        if (cmbTypeAction != null) {
            cmbTypeAction.setItems(FXCollections.observableArrayList(
                "Tous", "Connexion", "Déconnexion", "Création", "Modification", 
                "Suppression", "Téléchargement", "Partage"
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
            btnViderLogs.setOnAction(e -> viderLogs());
        }
    }
    
    /**
     * Charge les logs
     */
    private void chargerLogs() {
        List<LogEntry> logs = new ArrayList<>();
        
        String query = "SELECT la.*, u.nom, u.prenom FROM logs_activite la " +
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
                
                String nom = rs.getString("nom");
                String prenom = rs.getString("prenom");
                log.setUtilisateur(prenom + " " + nom);
                
                log.setAction(rs.getString("action"));
                log.setDetails(rs.getString("details"));
                log.setIpAddress(rs.getString("ip_address"));
                
                logs.add(log);
            }
            
            tableLogs.setItems(FXCollections.observableArrayList(logs));
            
        } catch (SQLException e) {
            System.err.println("Erreur chargement logs: " + e.getMessage());
        }
    }
    
    /**
     * Recherche des logs selon les critères
     */
    private void rechercherLogs() {
        // Implémenter la recherche filtrée
        chargerLogs();
    }
    
    /**
     * Exporte les logs
     */
    private void exporterLogs() {
        showInfo("Fonctionnalité d'export à venir...");
    }
    
    /**
     * Vide les anciens logs
     */
    private void viderLogs() {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setContentText("Voulez-vous vraiment vider tous les logs ?");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            try {
                String query = "DELETE FROM logs_activite WHERE timestamp < DATE_SUB(NOW(), INTERVAL 3 MONTH)";
                
                try (Connection conn = databaseService.getConnection();
                     Statement stmt = conn.createStatement()) {
                    
                    int deleted = stmt.executeUpdate(query);
                    showSuccess(deleted + " entrées supprimées");
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
            colConnexionType.setCellValueFactory(new PropertyValueFactory<>("type"));
            colConnexionIP.setCellValueFactory(new PropertyValueFactory<>("ipAddress"));
            colConnexionStatut.setCellValueFactory(new PropertyValueFactory<>("statut"));
            
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
    }
    
    /**
     * Charge les connexions
     */
    private void chargerConnexions() {
        List<ConnexionEntry> connexions = new ArrayList<>();
        
        String query = "SELECT la.*, u.nom, u.prenom FROM logs_activite la " +
                      "LEFT JOIN users u ON la.user_id = u.id " +
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
                
                String nom = rs.getString("nom");
                String prenom = rs.getString("prenom");
                connexion.setUtilisateur(prenom + " " + nom);
                
                String action = rs.getString("action");
                connexion.setType(action);
                connexion.setStatut("connexion".equals(action) ? "✅ Réussi" : 
                                   "deconnexion".equals(action) ? "🚪 Déconnexion" : "❌ Échec");
                
                connexion.setIpAddress(rs.getString("ip_address"));
                
                connexions.add(connexion);
            }
            
            tableConnexions.setItems(FXCollections.observableArrayList(connexions));
            
        } catch (SQLException e) {
            System.err.println("Erreur chargement connexions: " + e.getMessage());
        }
    }
    
    /**
     * Recherche des connexions
     */
    private void rechercherConnexions() {
        chargerConnexions();
    }
    
    /**
     * Exporte les connexions
     */
    private void exporterConnexions() {
        showInfo("Fonctionnalité d'export à venir...");
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
    }
    
    /**
     * Classe pour représenter une entrée de connexion
     */
    public static class ConnexionEntry {
        private String date;
        private String utilisateur;
        private String type;
        private String ipAddress;
        private String statut;
        
        public String getDate() { return date; }
        public void setDate(String date) { this.date = date; }
        
        public String getUtilisateur() { return utilisateur; }
        public void setUtilisateur(String utilisateur) { this.utilisateur = utilisateur; }
        
        public String getType() { return type; }
        public void setType(String type) { this.type = type; }
        
        public String getIpAddress() { return ipAddress; }
        public void setIpAddress(String ipAddress) { this.ipAddress = ipAddress; }
        
        public String getStatut() { return statut; }
        public void setStatut(String statut) { this.statut = statut; }
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
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showWarning(String title, String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle(title);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showInfo(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Information");
        alert.setContentText(message);
        alert.showAndWait();
    }
}