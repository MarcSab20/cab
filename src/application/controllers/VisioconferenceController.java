package application.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.web.WebEngine;
import javafx.scene.web.WebView;
import javafx.stage.Stage;
import javafx.concurrent.Worker;
import application.models.Reunion;
import application.models.User;
import application.models.StatutReunion;
import application.services.ReunionService;
import application.services.ReunionSyncService;
import application.utils.SessionManager;
import application.utils.AlertUtils;

import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur pour la visioconférence avec WebView et Jitsi Meet
 */
public class VisioconferenceController implements Initializable {
    
    @FXML private BorderPane mainContainer;
    @FXML private WebView webView;
    @FXML private ProgressBar progressBar;
    @FXML private Label labelStatut;
    @FXML private Label labelTitreReunion;
    @FXML private Label labelParticipants;
    @FXML private Button btnQuitter;
    @FXML private Button btnRecharger;
    @FXML private Button btnPartagerEcran;
    @FXML private Button btnMicrophone;
    @FXML private Button btnCamera;
    
    private WebEngine webEngine;
    private Reunion reunion;
    private User currentUser;
    private ReunionService reunionService;
    private ReunionSyncService reunionSyncService;
    private boolean reunionDemarree = false;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        currentUser = SessionManager.getInstance().getCurrentUser();
        reunionService = ReunionService.getInstance();
        reunionSyncService = ReunionSyncService.getInstance();
        
        // Configuration du WebView
        setupWebView();
        
        // Configuration des boutons
        setupButtons();
    }
    
    /**
     * Configure le WebView et le WebEngine
     */
    private void setupWebView() {
        webEngine = webView.getEngine();
        
        // Activer JavaScript
        webEngine.setJavaScriptEnabled(true);
        
        // Désactiver les messages contextuels
        webEngine.setOnAlert(event -> {
            System.out.println("WebView Alert: " + event.getData());
        });
        
        // Gérer les erreurs
        webEngine.setOnError(event -> {
            System.err.println("WebView Error: " + event.getMessage());
            labelStatut.setText("⚠️ Erreur de chargement");
        });
        
        // Suivre le chargement
        webEngine.getLoadWorker().stateProperty().addListener((obs, oldState, newState) -> {
            switch (newState) {
                case SCHEDULED:
                    labelStatut.setText("📡 Connexion en cours...");
                    progressBar.setVisible(true);
                    break;
                    
                case RUNNING:
                    labelStatut.setText("⏳ Chargement...");
                    progressBar.setProgress(-1.0);
                    break;
                    
                case SUCCEEDED:
                    labelStatut.setText("✅ Connecté");
                    progressBar.setVisible(false);
                    injecterScriptJitsi();
                    break;
                    
                case FAILED:
                    labelStatut.setText("❌ Échec de la connexion");
                    progressBar.setVisible(false);
                    AlertUtils.showError("Impossible de charger la visioconférence.\n" +
                                       "Vérifiez votre connexion internet.");
                    break;
                    
                case CANCELLED:
                    labelStatut.setText("🛑 Chargement annulé");
                    progressBar.setVisible(false);
                    break;
            }
        });
        
        // Suivre la progression
        webEngine.getLoadWorker().progressProperty().addListener((obs, oldVal, newVal) -> {
            progressBar.setProgress(newVal.doubleValue());
        });
    }
    
    /**
     * Injecte du JavaScript personnalisé pour améliorer l'intégration Jitsi
     */
    private void injecterScriptJitsi() {
        try {
            // Script pour personnaliser l'interface Jitsi
            String script = """
                try {
                    // Cacher certains éléments de l'interface Jitsi si nécessaire
                    console.log('Jitsi Meet chargé dans EMAA Document Management');
                    
                    // Vous pouvez ajouter des personnalisations ici
                    // Par exemple, auto-mute au démarrage:
                    // api.executeCommand('toggleAudio');
                    
                } catch (e) {
                    console.error('Erreur script personnalisé:', e);
                }
            """;
            
            webEngine.executeScript(script);
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'injection du script: " + e.getMessage());
        }
    }
    
    /**
     * Configure les boutons
     */
    private void setupButtons() {
        btnQuitter.setOnAction(e -> handleQuitter());
        btnRecharger.setOnAction(e -> handleRecharger());
        
        // Désactiver temporairement les contrôles avancés
        // (ils nécessiteraient une intégration plus poussée avec l'API Jitsi)
        btnPartagerEcran.setDisable(true);
        btnMicrophone.setDisable(true);
        btnCamera.setDisable(true);
    }
    
    /**
     * Charge la réunion et démarre la visioconférence
     */
    public void chargerReunion(Reunion reunion) {
        this.reunion = reunion;
        
        // Mettre à jour l'interface
        labelTitreReunion.setText(reunion.getTitre());
        labelParticipants.setText(reunion.getParticipants().size() + " participant(s)");
        
        // Démarrer la réunion si ce n'est pas déjà fait
        if (reunion.getStatut() != StatutReunion.EN_COURS && !reunionDemarree) {
            demarrerReunion();
        }
        
        // Charger Jitsi Meet
        chargerJitsiMeet();
    }
    
    /**
     * Démarre la réunion (change le statut)
     */
    private void demarrerReunion() {
        try {
            reunion.setStatut(StatutReunion.EN_COURS);
            
            if (reunionSyncService.demarrerReunion(reunion)) {
                reunionDemarree = true;
                System.out.println("✅ Réunion démarrée: " + reunion.getTitre());
            } else {
                AlertUtils.showWarning("Impossible de démarrer la réunion");
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors du démarrage de la réunion: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Charge Jitsi Meet dans le WebView
     */
    private void chargerJitsiMeet() {
        try {
            String roomName = reunion.getLienVisio();
            
            if (roomName == null || roomName.isEmpty()) {
                AlertUtils.showError("Aucun lien de visioconférence disponible");
                return;
            }
            
            // Construire l'URL Jitsi Meet avec configuration
            String displayName = currentUser.getNomComplet();
            String subject = reunion.getTitre();
            
            // Utiliser le serveur Jitsi Meet public ou votre propre instance
            String jitsiServer = "meet.jit.si";
            
            // Construction de l'URL avec paramètres
            String jitsiUrl = String.format(
                "https://%s/%s#config.prejoinPageEnabled=false&" +
                "userInfo.displayName=%s&" +
                "config.subject=%s&" +
                "config.startWithAudioMuted=true&" +
                "config.startWithVideoMuted=false",
                jitsiServer,
                roomName,
                java.net.URLEncoder.encode(displayName, "UTF-8"),
                java.net.URLEncoder.encode(subject, "UTF-8")
            );
            
            System.out.println("📡 Chargement Jitsi Meet: " + jitsiUrl);
            
            // Charger l'URL
            webEngine.load(jitsiUrl);
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement de Jitsi: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur lors du chargement de la visioconférence");
        }
    }
    
    /**
     * Recharge la page Jitsi
     */
    @FXML
    private void handleRecharger() {
        webEngine.reload();
    }
    
    /**
     * Quitte la visioconférence
     */
    @FXML
    private void handleQuitter() {
        boolean confirmer = AlertUtils.showConfirmation(
            "Quitter la réunion",
            "Êtes-vous sûr de vouloir quitter la visioconférence ?"
        );
        
        if (confirmer) {
            // Proposer de terminer la réunion si c'est l'organisateur
            if (reunion.getOrganisateur().getId() == currentUser.getId() && 
                reunion.getStatut() == StatutReunion.EN_COURS) {
                
                boolean terminer = AlertUtils.showConfirmation(
                    "Terminer la réunion",
                    "Voulez-vous terminer la réunion pour tous les participants ?"
                );
                
                if (terminer) {
                    terminerReunion();
                }
            }
            
            fermerFenetre();
        }
    }
    
    /**
     * Termine la réunion
     */
    private void terminerReunion() {
        try {
            reunion.setStatut(StatutReunion.TERMINEE);
            
            if (reunionService.saveReunion(reunion)) {
                System.out.println("✅ Réunion terminée: " + reunion.getTitre());
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la terminaison de la réunion: " + e.getMessage());
        }
    }
    
    /**
     * Ferme la fenêtre
     */
    private void fermerFenetre() {
        Stage stage = (Stage) mainContainer.getScene().getWindow();
        stage.close();
    }
    
    /**
     * Nettoie les ressources avant fermeture
     */
    public void cleanup() {
        if (webEngine != null) {
            webEngine.load(null);
        }
    }
}