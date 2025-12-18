package application.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.stage.Stage;
import javafx.scene.Scene;
import application.models.Message;
import application.models.MessageGroup;
import application.models.PrioriteMessage;
import application.models.Role;
import application.models.StatutMessage;
import application.models.User;
import application.models.UserPresence;
import application.services.DatabaseService;
import application.services.MessageService;
import application.services.MessageSyncService;
import application.utils.SessionManager;
import application.utils.AlertUtils;

import java.net.URL;
import java.sql.Connection;
import java.sql.PreparedStatement;
import java.sql.ResultSet;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.ResourceBundle;

import javafx.stage.FileChooser;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.StandardCopyOption;
import java.nio.file.Path;
import java.nio.file.Paths;

public class MessagesController implements Initializable, MessageSyncService.MessageListener {
    
    // Filtres
    @FXML private ComboBox<String> filtreDossier;
    @FXML private ComboBox<String> filtreStatut;
    @FXML private ComboBox<String> filtrePrioriteMsg;
    @FXML private TextField champRechercheMsg;
    
    // Boutons d'actions principales
    @FXML private Button btnNouveauMessage;
    @FXML private Button btnActualiser;
    @FXML private Button btnStatistiques;
    
    // Liste des messages
    @FXML private VBox listeMessages;
    @FXML private Label nombreMessages;
    @FXML private Label labelDossierActuel;
    @FXML private CheckBox checkboxSelectAll;
    
    // Zone vide
    @FXML private VBox zoneVide;
    
    // Zone de lecture
    @FXML private ScrollPane scrollZoneMessage;
    @FXML private VBox zoneMessage;
    @FXML private Label labelObjetMessage;
    @FXML private Label labelPrioriteMessage;
    @FXML private Label labelExpediteurMsg;
    @FXML private Label labelDestinataireMsg;
    @FXML private Label labelDateMessage;
    @FXML private Label labelObjetDetail;
    @FXML private ScrollPane scrollPaneContenu;
    
    // Boutons d'action sur message
    @FXML private Button btnRepondre;
    @FXML private Button btnRepondreATous;
    @FXML private Button btnTransferer;
    @FXML private Button btnMarquerImportant;
    @FXML private Button btnImprimer;
    @FXML private Button btnArchiver;
    @FXML private Button btnSupprimer;
    
    // Zone de composition
    @FXML private ScrollPane scrollZoneComposition;
    @FXML private VBox zoneComposition;
    @FXML private ComboBox<User> comboDestinataire;
    @FXML private TextField champCc;
    @FXML private TextField champObjetComposition;
    @FXML private ComboBox<String> comboPrioriteComposition;
    @FXML private TextArea textAreaMessage;
    @FXML private Button btnEnvoyer;
    @FXML private Button btnSauvegarderBrouillon;
    @FXML private Button btnAnnuler;
    @FXML private Button btnAnnulerComposition;
    @FXML private Button btnEnvoyerMessage;
    @FXML private VBox containerDestinataires;
    
    private User currentUser;
    private MultipleRecipientsSelector recipientsSelector;
    private List<User> selectedRecipients = new ArrayList<>();
    private MessageService messageService;
    private MessageSyncService messageSyncService;
    private ObservableList<User> listeUtilisateurs;
    private ObservableList<Message> messages;
    private Message selectedMessage;
    private String currentDossier = "Boîte de réception";
    private File selectedAttachment = null;
    private static final String ATTACHMENTS_DIR = "attachments/messages/";
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("MessagesController.initialize() appelé");
        
        try {
            currentUser = SessionManager.getInstance().getCurrentUser();
            messageService = MessageService.getInstance();
            messages = FXCollections.observableArrayList();
            messageSyncService = MessageSyncService.getInstance();
            messageSyncService.addMessageListener(this);
            messageSyncService.updatePresence(currentUser.getId(), UserPresence.Statut.ONLINE);
            
            if (currentUser == null) {
                System.err.println("ERREUR: Aucun utilisateur en session");
                return;
            }
            
            setupFilters();
            setupButtons();
            setupComposition();
            loadMessages();
            chargerListeUtilisateurs();
            
            // Afficher la zone vide par défaut
            showEmptyZone();
            
        } catch (Exception e) {
            System.err.println("Erreur dans MessagesController.initialize(): " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupFilters() {
        // Les ComboBox sont déjà initialisés dans le FXML
        if (filtreDossier != null) {
            filtreDossier.setOnAction(e -> {
                currentDossier = filtreDossier.getValue();
                if (labelDossierActuel != null) {
                    labelDossierActuel.setText(currentDossier);
                }
                applyFilters();
            });
        }
        
        if (filtreStatut != null) {
            filtreStatut.setOnAction(e -> applyFilters());
        }
        
        if (filtrePrioriteMsg != null) {
            filtrePrioriteMsg.setOnAction(e -> applyFilters());
        }
        
        // Recherche en temps réel
        if (champRechercheMsg != null) {
            champRechercheMsg.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal != null && !newVal.equals(oldVal)) {
                    applyFilters();
                }
            });
        }
    }
    
    private void setupButtons() {
        if (btnRepondre != null) {
            btnRepondre.setOnAction(e -> handleRepondre());
        }
        if (btnRepondreATous != null) {
            btnRepondreATous.setOnAction(e -> handleRepondreATous());
        }
        if (btnTransferer != null) {
            btnTransferer.setOnAction(e -> handleTransferer());
        }
        if (btnMarquerImportant != null) {
            btnMarquerImportant.setOnAction(e -> handleMarquerImportant());
        }
        if (btnArchiver != null) {
            btnArchiver.setOnAction(e -> handleArchiver());
        }
        if (btnSupprimer != null) {
            btnSupprimer.setOnAction(e -> handleSupprimer());
        }
    }
    
    private void setupComposition() {
        if (comboPrioriteComposition != null) {
            // Déjà initialisé dans le FXML
        }
        
        if (btnEnvoyer != null) {
            btnEnvoyer.setOnAction(e -> handleEnvoyerMessage());
        }
        if (btnEnvoyerMessage != null) {
            btnEnvoyerMessage.setOnAction(e -> handleEnvoyerMessage());
        }
        if (btnSauvegarderBrouillon != null) {
            btnSauvegarderBrouillon.setOnAction(e -> handleSauvegarderBrouillon());
        }
        if (btnAnnuler != null) {
            btnAnnuler.setOnAction(e -> handleAnnulerComposition());
        }
        if (btnAnnulerComposition != null) {
            btnAnnulerComposition.setOnAction(e -> handleAnnulerComposition());
        }
        
        // Créer le répertoire des pièces jointes s'il n'existe pas
        try {
            Path attachmentsPath = Paths.get(ATTACHMENTS_DIR);
            if (!Files.exists(attachmentsPath)) {
                Files.createDirectories(attachmentsPath);
                System.out.println("✅ Répertoire des pièces jointes créé: " + ATTACHMENTS_DIR);
            }
        } catch (Exception e) {
            System.err.println("⚠️ Erreur lors de la création du répertoire des pièces jointes: " + e.getMessage());
        }
    }
    
    /**
     * Affiche la zone vide par défaut
     */
    private void showEmptyZone() {
        if (zoneVide != null) {
            zoneVide.setVisible(true);
            zoneVide.setManaged(true);
        }
        if (scrollZoneMessage != null) {
            scrollZoneMessage.setVisible(false);
            scrollZoneMessage.setManaged(false);
        }
        if (scrollZoneComposition != null) {
            scrollZoneComposition.setVisible(false);
            scrollZoneComposition.setManaged(false);
        }
    }
    
    /**
     * NOUVEAU: Gère le bouton "Nouveau message"
     */
    @FXML
    private void handleNouveauMessage() {
        showCompositionForm();
        
        // Réinitialiser le formulaire
        if (recipientsSelector != null) {
            recipientsSelector.setSelectedUsers(new ArrayList<>());
        }
        if (champCc != null) champCc.clear();
        if (champObjetComposition != null) champObjetComposition.clear();
        if (textAreaMessage != null) textAreaMessage.clear();
        if (comboPrioriteComposition != null) comboPrioriteComposition.setValue("Normale");
        
        selectedAttachment = null;
    }
    
    /**
     * NOUVEAU: Gère le bouton "Actualiser"
     */
    @FXML
    private void handleActualiser() {
        System.out.println("🔄 Actualisation des messages...");
        loadMessages();
        AlertUtils.showInfo("Messages actualisés", "La liste des messages a été mise à jour.");
    }
    
    /**
     * NOUVEAU: Gère le bouton "Joindre"
     */
    @FXML
    private void handleJoindre() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Sélectionner un fichier à joindre");
        
        // Filtres pour les types de fichiers courants
        fileChooser.getExtensionFilters().addAll(
            new FileChooser.ExtensionFilter("Tous les fichiers", "*.*"),
            new FileChooser.ExtensionFilter("Documents", "*.pdf", "*.doc", "*.docx", "*.txt"),
            new FileChooser.ExtensionFilter("Images", "*.jpg", "*.jpeg", "*.png", "*.gif"),
            new FileChooser.ExtensionFilter("Archives", "*.zip", "*.rar", "*.7z")
        );
        
        // Ouvrir le dialogue
        javafx.stage.Stage stage = (javafx.stage.Stage) textAreaMessage.getScene().getWindow();
        File file = fileChooser.showOpenDialog(stage);
        
        if (file != null) {
            // Vérifier la taille du fichier (limite: 10 MB)
            long fileSizeInMB = file.length() / (1024 * 1024);
            
            if (fileSizeInMB > 10) {
                AlertUtils.showWarning("Fichier trop volumineux", 
                    "La taille du fichier ne doit pas dépasser 10 MB.\n" +
                    "Taille actuelle: " + fileSizeInMB + " MB");
                return;
            }
            
            selectedAttachment = file;
            
            // Afficher une notification
            AlertUtils.showInfo("Fichier sélectionné", 
                "Fichier: " + file.getName() + "\n" +
                "Taille: " + String.format("%.2f", fileSizeInMB) + " MB");
            
            System.out.println("📎 Pièce jointe sélectionnée: " + file.getName());
        }
    }
    
    /**
     * NOUVEAU: Ouvre un brouillon pour modification
     */
    private void openDraftForEditing(Message draft) {
        showCompositionForm();
        
        // Remplir les destinataires (un seul pour un brouillon classique)
        if (recipientsSelector != null && draft.getDestinataire() != null) {
            List<User> recipients = new ArrayList<>();
            
            // Si le contenu contient [DESTINATAIRES:...], extraire la liste
            String contenu = draft.getContenu();
            if (contenu.startsWith("[DESTINATAIRES:")) {
                // Extraire les noms et essayer de retrouver les utilisateurs
                // (Pour simplifier, on ne gère que le premier destinataire ici)
                recipients.add(draft.getDestinataire());
            } else {
                recipients.add(draft.getDestinataire());
            }
            
            recipientsSelector.setSelectedUsers(recipients);
        }
        
        if (champObjetComposition != null) {
            champObjetComposition.setText(draft.getObjet());
        }
        
        if (textAreaMessage != null) {
            String contenu = draft.getContenu();
            // Retirer la note [DESTINATAIRES:...] si présente
            if (contenu.startsWith("[DESTINATAIRES:")) {
                int endIndex = contenu.indexOf("]\n\n");
                if (endIndex != -1) {
                    contenu = contenu.substring(endIndex + 3);
                }
            }
            textAreaMessage.setText(contenu);
        }
        
        if (comboPrioriteComposition != null && draft.getPriorite() != null) {
            comboPrioriteComposition.setValue(draft.getPriorite().getLibelle());
        }
    }
    
    /**
     * NOUVEAU: Gère le bouton "Statistiques"
     */
    @FXML
    private void handleStatistiques() {
        try {
            List<Message> allMessages = messageService.getMessagesForUser(currentUser.getId());
            
            int total = allMessages.size();
            int nonLus = (int) allMessages.stream().filter(m -> !m.isLu()).count();
            int importants = (int) allMessages.stream().filter(Message::isImportant).count();
            int archives = (int) allMessages.stream().filter(Message::isArchive).count();
            
            String stats = String.format(
                "📊 STATISTIQUES DE MESSAGERIE\n\n" +
                "Total de messages: %d\n" +
                "Messages non lus: %d\n" +
                "Messages importants: %d\n" +
                "Messages archivés: %d\n\n" +
                "Taux de lecture: %.1f%%",
                total,
                nonLus,
                importants,
                archives,
                total > 0 ? ((total - nonLus) * 100.0 / total) : 0.0
            );
            
            AlertUtils.showInfo("Statistiques", stats);
            
        } catch (Exception e) {
            System.err.println("Erreur lors du calcul des statistiques: " + e.getMessage());
            AlertUtils.showError("Impossible de calculer les statistiques.");
        }
    }
    
    /**
     * NOUVEAU: Gère l'impression du message
     */
    @FXML
    private void handleImprimer() {
        if (selectedMessage == null) {
            AlertUtils.showWarning("Veuillez sélectionner un message à imprimer");
            return;
        }
        
        // Pour l'instant, juste une notification
        // Dans une vraie implémentation, on utiliserait PrinterJob de JavaFX
        AlertUtils.showInfo("Impression", 
            "Fonctionnalité d'impression en cours de développement.\n" +
            "Message: " + selectedMessage.getObjet());
    }
    
    /**
     * NOUVEAU: Marque les messages sélectionnés comme lus
     */
    @FXML
    private void handleMarquerLuSelection() {
        // Pour l'instant, marquer tous les messages non lus du dossier courant
        int count = 0;
        for (Message msg : messages) {
            if (!msg.isLu()) {
                msg.marquerCommeLu();
                if (messageService.saveMessage(msg)) {
                    count++;
                }
            }
        }
        
        if (count > 0) {
            AlertUtils.showInfo(count + " message(s) marqué(s) comme lu(s)");
            loadMessages();
        } else {
            AlertUtils.showInfo("Tous les messages sont déjà lus");
        }
    }
    
    /**
     * NOUVEAU: Archive les messages sélectionnés
     */
    @FXML
    private void handleArchiverSelection() {
        boolean confirm = AlertUtils.showConfirmation(
            "Confirmation",
            "Voulez-vous archiver tous les messages affichés ?"
        );
        
        if (confirm) {
            int count = 0;
            for (Message msg : messages) {
                if (!msg.isArchive()) {
                    msg.setArchive(true);
                    if (messageService.saveMessage(msg)) {
                        count++;
                    }
                }
            }
            
            if (count > 0) {
                AlertUtils.showInfo(count + " message(s) archivé(s)");
                loadMessages();
            }
        }
    }
    
    /**
     * NOUVEAU: Supprime les messages sélectionnés
     */
    @FXML
    private void handleSupprimerSelection() {
        boolean confirm = AlertUtils.showConfirmation(
            "Confirmation",
            "Voulez-vous supprimer tous les messages affichés ?\n" +
            "Cette action est irréversible."
        );
        
        if (confirm) {
            int count = 0;
            List<Integer> idsToDelete = new ArrayList<>();
            
            for (Message msg : messages) {
                idsToDelete.add(msg.getId());
            }
            
            for (int id : idsToDelete) {
                if (messageService.deleteMessage(id)) {
                    count++;
                }
            }
            
            if (count > 0) {
                AlertUtils.showInfo(count + " message(s) supprimé(s)");
                loadMessages();
                showEmptyZone();
            }
        }
    }
    
    private void loadMessages() {
        try {
            List<Message> list = messageService.getMessagesForUser(currentUser.getId());
            messages.clear();
            messages.addAll(list);
            
            // Appliquer les filtres immédiatement
            applyFilters();
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des messages: " + e.getMessage());
            AlertUtils.showError("Erreur lors du chargement des messages");
        }
    }
    
    private void displayMessagesList() {
        if (listeMessages == null) return;
        
        listeMessages.getChildren().clear();
        
        if (messages.isEmpty()) {
            Label emptyLabel = new Label("Aucun message dans ce dossier");
            emptyLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-padding: 30; -fx-font-size: 14px;");
            listeMessages.getChildren().add(emptyLabel);
            return;
        }
        
        for (Message message : messages) {
            HBox messageBox = createMessageBox(message);
            listeMessages.getChildren().add(messageBox);
        }
    }
    
    private HBox createMessageBox(Message message) {
        HBox box = new HBox(10);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(12));
        box.getStyleClass().add("message-item");
        box.setCursor(javafx.scene.Cursor.HAND);
        
        // Style selon le statut
        if (!message.isLu()) {
            box.setStyle("-fx-background-color: #e3f2fd; -fx-background-radius: 6;");
        } else {
            box.setStyle("-fx-background-color: white; -fx-background-radius: 6;");
        }
        
        // Checkbox
        CheckBox checkBox = new CheckBox();
        
        // Indicateurs
        VBox indicators = new VBox(2);
        Label priorityLabel = new Label(getPriorityIcon(message.getPriorite()));
        priorityLabel.setStyle("-fx-font-size: 8px;");
        Label importantLabel = new Label(message.isImportant() ? "⭐" : "");
        importantLabel.setStyle("-fx-font-size: 8px;");
        indicators.getChildren().addAll(priorityLabel, importantLabel);
        
        // Contenu
        VBox content = new VBox(3);
        HBox.setHgrow(content, Priority.ALWAYS);
        
        HBox header = new HBox(8);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label expediteur = new Label(message.getExpediteur().getNomComplet());
        if (!message.isLu()) {
            expediteur.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        } else {
            expediteur.setStyle("-fx-text-fill: #7f8c8d;");
        }
        
        Region spacer = new Region();
        HBox.setHgrow(spacer, Priority.ALWAYS);
        
        Label date = new Label(message.getTempsEcoule());
        date.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
        
        header.getChildren().addAll(expediteur, spacer, date);
        
        Label objet = new Label(message.getObjet());
        if (!message.isLu()) {
            objet.setStyle("-fx-font-weight: bold;");
        } else {
            objet.setStyle("-fx-text-fill: #7f8c8d;");
        }
        
        Label apercu = new Label(message.getApercu(80));
        apercu.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        
        content.getChildren().addAll(header, objet, apercu);
        
        // Icône pièce jointe
        VBox attachment = new VBox();
        attachment.setAlignment(Pos.CENTER);
        if (message.hasPieceJointe()) {
            Label attachIcon = new Label("📎");
            attachIcon.setStyle("-fx-font-size: 16px;");
            attachment.getChildren().add(attachIcon);
        }
        
        box.getChildren().addAll(checkBox, indicators, content, attachment);
        
        // Événement de clic
        box.setOnMouseClicked(e -> {
            showMessageDetails(message);
        });
        
        return box;
    }
    
    private String getPriorityIcon(PrioriteMessage priorite) {
        if (priorite == null) return "";
        switch (priorite) {
            case TRES_HAUTE: return "🔴";
            case HAUTE: return "🟠";
            case NORMALE: return "";
            case BASSE: return "🔵";
            default: return "";
        }
    }
    
    private void showMessageDetails(Message message) {
        selectedMessage = message;
        
        // Marquer comme lu si non lu
        if (!message.isLu()) {
            message.marquerCommeLu();
            messageService.saveMessage(message);
            displayMessagesList(); // Rafraîchir la liste
        }
        
        if (message.getStatut() == StatutMessage.BROUILLON) {
            openDraftForEditing(message);
            return;
        }
        
        // Masquer les autres zones
        if (zoneVide != null) {
            zoneVide.setVisible(false);
            zoneVide.setManaged(false);
        }
        if (scrollZoneComposition != null) {
            scrollZoneComposition.setVisible(false);
            scrollZoneComposition.setManaged(false);
        }
        
        // Afficher la zone de message
        if (scrollZoneMessage != null) {
            scrollZoneMessage.setVisible(true);
            scrollZoneMessage.setManaged(true);
        }
        
        // Remplir les détails
        if (labelObjetMessage != null) {
            labelObjetMessage.setText(message.getObjet());
        }
        
        // Afficher la priorité si nécessaire
        if (labelPrioriteMessage != null) {
            if (message.getPriorite() != null && message.getPriorite().isCritique()) {
                labelPrioriteMessage.setText(message.getPriorite().getIcone() + " " + message.getPriorite().getLibelle().toUpperCase());
                labelPrioriteMessage.setStyle(
                    "-fx-background-color: " + message.getPriorite().getCouleur() + 
                    "; -fx-text-fill: white; -fx-padding: 4 8; -fx-background-radius: 12; -fx-font-size: 10px;"
                );
                labelPrioriteMessage.setVisible(true);
            } else {
                labelPrioriteMessage.setVisible(false);
            }
        }
        
        if (labelExpediteurMsg != null) {
            labelExpediteurMsg.setText(message.getExpediteur().getNomComplet() + 
                " <" + message.getExpediteur().getEmail() + ">");
        }
        
        if (labelDestinataireMsg != null) {
            labelDestinataireMsg.setText(message.getDestinataire().getNomComplet());
        }
        
        if (labelDateMessage != null) {
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMMM yyyy, HH:mm");
            labelDateMessage.setText(message.getDateEnvoi().format(formatter));
        }
        
        if (labelObjetDetail != null) {
            labelObjetDetail.setText(message.getObjet());
        }
        
        // Afficher le contenu du message
        if (scrollPaneContenu != null) {
            VBox contenuBox = new VBox(10);
            contenuBox.setPadding(new Insets(15));
            contenuBox.setStyle("-fx-background-color: #f8f9fa;");
            
            // Diviser le contenu en paragraphes
            String[] paragraphes = message.getContenu().split("\n");
            for (String paragraphe : paragraphes) {
                if (!paragraphe.trim().isEmpty()) {
                    Label label = new Label(paragraphe);
                    label.setWrapText(true);
                    label.setStyle("-fx-font-size: 14px;");
                    contenuBox.getChildren().add(label);
                }
            }
            
            scrollPaneContenu.setContent(contenuBox);
        }
        
        // Mettre à jour le bouton Important
        if (btnMarquerImportant != null) {
            if (message.isImportant()) {
                btnMarquerImportant.setText("⭐ Retirer important");
            } else {
                btnMarquerImportant.setText("⭐ Marquer important");
            }
        }
    }
    
    @FXML
    private void handleRecherche() {
        applyFilters();
    }
    
    /**
     * AMÉLIORÉ: Applique tous les filtres
     */
    private void applyFilters() {
        try {
            String dossierFilter = filtreDossier != null ? filtreDossier.getValue() : "Boîte de réception";
            String statutFilter = filtreStatut != null ? filtreStatut.getValue() : "Tous";
            String prioriteFilter = filtrePrioriteMsg != null ? filtrePrioriteMsg.getValue() : "Toutes";
            String searchText = champRechercheMsg != null ? champRechercheMsg.getText().toLowerCase() : "";
            
            List<Message> allMessages = messageService.getMessagesForUser(currentUser.getId());
            ObservableList<Message> filtered = FXCollections.observableArrayList();
            
            for (Message m : allMessages) {
                boolean matches = true;
                
                // Filtre de dossier
                switch (dossierFilter) {
                    case "Boîte de réception":
                        // Messages reçus non archivés et non supprimés
                        if (m.getDestinataire().getId() != currentUser.getId() || 
                            m.isArchive() || 
                            m.getStatut() == StatutMessage.SUPPRIME ||
                            m.getStatut() == StatutMessage.BROUILLON) {
                            matches = false;
                        }
                        break;
                        
                    case "Messages envoyés":
                        // CORRECTION: Messages envoyés par l'utilisateur courant
                        if (m.getExpediteur().getId() != currentUser.getId() ||
                            m.getStatut() == StatutMessage.BROUILLON ||
                            m.getStatut() == StatutMessage.SUPPRIME) {
                            matches = false;
                        }
                        break;
                        
                    case "Brouillons":
                        // CORRECTION: Uniquement les brouillons de l'utilisateur
                        if (m.getStatut() != StatutMessage.BROUILLON ||
                            m.getExpediteur().getId() != currentUser.getId()) {
                            matches = false;
                        }
                        break;
                        
                    case "Messages importants":
                        if (!m.isImportant() || m.isArchive()) {
                            matches = false;
                        }
                        break;
                        
                    case "Archive":
                        if (!m.isArchive()) {
                            matches = false;
                        }
                        break;
                        
                    case "Corbeille":
                        if (m.getStatut() != StatutMessage.SUPPRIME) {
                            matches = false;
                        }
                        break;
                }
                
                // Filtre statut
                if (matches) {
                    switch (statutFilter) {
                        case "Non lus":
                            if (m.isLu()) matches = false;
                            break;
                        case "Lus":
                            if (!m.isLu()) matches = false;
                            break;
                        case "Importants":
                            if (!m.isImportant()) matches = false;
                            break;
                    }
                }
                
                // Filtre priorité
                if (matches && !prioriteFilter.equals("Toutes")) {
                    application.models.PrioriteMessage priorite = 
                        application.models.PrioriteMessage.fromLibelle(prioriteFilter);
                    if (priorite != null && m.getPriorite() != priorite) {
                        matches = false;
                    }
                }
                
                // Recherche textuelle
                if (matches && !searchText.isEmpty()) {
                    boolean textMatch = m.getObjet().toLowerCase().contains(searchText) ||
                                      m.getContenu().toLowerCase().contains(searchText) ||
                                      m.getExpediteur().getNomComplet().toLowerCase().contains(searchText);
                    if (!textMatch) {
                        matches = false;
                    }
                }
                
                if (matches) {
                    filtered.add(m);
                }
            }
            
            messages.clear();
            messages.addAll(filtered);
            
            if (nombreMessages != null) {
                nombreMessages.setText("(" + messages.size() + " messages)");
            }
            
            displayMessagesList();
            
            System.out.println("🔍 Filtrage appliqué - Dossier: " + dossierFilter + 
                             ", Résultats: " + messages.size());
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'application des filtres: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleRepondre() {
        if (selectedMessage == null) {
            AlertUtils.showWarning("Veuillez sélectionner un message");
            return;
        }

        showCompositionForm();

        // Sélectionner l'expéditeur du message comme destinataire unique
        if (recipientsSelector != null) {
            List<User> recipients = new ArrayList<>();
            recipients.add(selectedMessage.getExpediteur());
            recipientsSelector.setSelectedUsers(recipients);
        }
        
        if (champObjetComposition != null) {
            String objet = selectedMessage.getObjet();
            if (!objet.startsWith("RE:")) {
                champObjetComposition.setText("RE: " + objet);
            } else {
                champObjetComposition.setText(objet);
            }
        }
        
        if (textAreaMessage != null) {
            textAreaMessage.setText("\n\n--- Message original ---\n" + selectedMessage.getContenu());
            textAreaMessage.positionCaret(0);
        }
    }
    
    @Override
    public void onNewMessage(Message message) {
        javafx.application.Platform.runLater(() -> {
            loadMessages();
            
            // Afficher une notification
            if (message.getDestinataire().getId() == currentUser.getId()) {
                showNewMessageNotification(message);
            }
        });
    }
    
    /**
     * Affiche une notification pour un nouveau message
     */
    private void showNewMessageNotification(Message message) {
        System.out.println("📬 Nouveau message de: " + message.getExpediteur().getNomComplet());
        // Vous pouvez ajouter une notification visuelle ici
    }
    
    @Override
    public void onRefreshRequest() {
        javafx.application.Platform.runLater(() -> {
            loadMessages();
            chargerListeUtilisateurs();
        });
    }
    
    @FXML
    private void handleRepondreATous() {
        if (selectedMessage == null) {
            AlertUtils.showWarning("Veuillez sélectionner un message");
            return;
        }
        
        // Pour l'instant, même comportement que Répondre
        handleRepondre();
        
        AlertUtils.showInfo("La fonction 'Répondre à tous' sera disponible prochainement pour les groupes.");
    }
    
    @FXML
    private void handleTransferer() {
        if (selectedMessage == null) {
            AlertUtils.showWarning("Veuillez sélectionner un message");
            return;
        }

        showCompositionForm();

        // Laisser les destinataires vides (l'utilisateur choisira)
        if (recipientsSelector != null) {
            recipientsSelector.setSelectedUsers(new ArrayList<>());
        }
        
        if (champObjetComposition != null) {
            String objet = selectedMessage.getObjet();
            if (!objet.startsWith("TR:")) {
                champObjetComposition.setText("TR: " + objet);
            } else {
                champObjetComposition.setText(objet);
            }
        }
        
        if (textAreaMessage != null) {
            String transfertInfo = String.format(
                "--- Message transféré ---\n" +
                "De: %s\n" +
                "Date: %s\n" +
                "Objet: %s\n" +
                "---\n\n%s",
                selectedMessage.getExpediteur().getNomComplet(),
                selectedMessage.getDateEnvoi(),
                selectedMessage.getObjet(),
                selectedMessage.getContenu()
            );
            textAreaMessage.setText(transfertInfo);
            textAreaMessage.positionCaret(0);
        }
    }
    
    @FXML
    private void handleMarquerImportant() {
        if (selectedMessage == null) {
            AlertUtils.showWarning("Veuillez sélectionner un message");
            return;
        }
        
        selectedMessage.setImportant(!selectedMessage.isImportant());
        
        if (messageService.saveMessage(selectedMessage)) {
            String action = selectedMessage.isImportant() ? "important" : "non important";
            AlertUtils.showInfo("Message marqué comme " + action);
            
            // Mettre à jour l'affichage
            displayMessagesList();
            showMessageDetails(selectedMessage);
        } else {
            AlertUtils.showError("Erreur lors de la mise à jour");
        }
    }
    
    @FXML
    private void handleArchiver() {
        if (selectedMessage == null) {
            AlertUtils.showWarning("Veuillez sélectionner un message");
            return;
        }
        
        selectedMessage.setArchive(true);
        
        if (messageService.saveMessage(selectedMessage)) {
            AlertUtils.showInfo("Message archivé avec succès");
            loadMessages();
            showEmptyZone();
        } else {
            AlertUtils.showError("Erreur lors de l'archivage");
        }
    }
    
    @FXML
    private void handleSupprimer() {
        if (selectedMessage == null) {
            AlertUtils.showWarning("Veuillez sélectionner un message");
            return;
        }
        
        boolean confirm = AlertUtils.showConfirmation(
            "Confirmation",
            "Êtes-vous sûr de vouloir supprimer ce message ?"
        );
        
        if (confirm) {
            if (messageService.deleteMessage(selectedMessage.getId())) {
                AlertUtils.showInfo("Message supprimé avec succès");
                loadMessages();
                showEmptyZone();
            } else {
                AlertUtils.showError("Erreur lors de la suppression");
            }
        }
    }
    
    private void showCompositionForm() {
        // Masquer les autres zones
        if (zoneVide != null) {
            zoneVide.setVisible(false);
            zoneVide.setManaged(false);
        }
        if (scrollZoneMessage != null) {
            scrollZoneMessage.setVisible(false);
            scrollZoneMessage.setManaged(false);
        }
        
        // Afficher la zone de composition
        if (scrollZoneComposition != null) {
            scrollZoneComposition.setVisible(true);
            scrollZoneComposition.setManaged(true);
        }
    }
    
    /**
     * Charge la liste de tous les utilisateurs actifs
     */
    private void chargerListeUtilisateurs() {
        try {
            List<User> users = getAllActiveUsers();
            listeUtilisateurs = FXCollections.observableArrayList(users);
            
            // Initialiser le sélecteur multiple
            if (recipientsSelector == null) {
                recipientsSelector = new MultipleRecipientsSelector(users, messageSyncService);
                
                // Ajouter au conteneur si présent
                if (containerDestinataires != null) {
                    containerDestinataires.getChildren().clear();
                    containerDestinataires.getChildren().add(recipientsSelector.getView());
                }
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des utilisateurs: " + e.getMessage());
        }
    }
    
    
    /**
     * Récupère tous les utilisateurs actifs sauf l'utilisateur courant
     */
    private List<User> getAllActiveUsers() {
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
            
            DatabaseService dbService = DatabaseService.getInstance();
            
            try (Connection conn = dbService.getConnection();
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
                        user.setActif(rs.getBoolean("actif"));
                        
                        // Mapper le rôle si nécessaire
                        String roleNom = rs.getString("role_nom");
                        if (roleNom != null) {
                            Role role = new Role();
                            role.setId(rs.getInt("role_id"));
                            role.setNom(roleNom);
                            role.setDescription(rs.getString("role_desc"));
                            user.setRole(role);
                        }
                        
                        users.add(user);
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des utilisateurs: " + e.getMessage());
            e.printStackTrace();
        }
        
        return users;
    }
    
    @FXML
    private void handleEnvoyerMessage() {
        try {
            // Récupérer les destinataires sélectionnés
            List<User> destinataires = recipientsSelector != null ? 
                                       recipientsSelector.getSelectedUsers() : 
                                       new ArrayList<>();
            
            // Validation des destinataires
            if (destinataires.isEmpty()) {
                AlertUtils.showWarning("Veuillez sélectionner au moins un destinataire");
                return;
            }
            
            if (champObjetComposition == null || champObjetComposition.getText().isEmpty()) {
                AlertUtils.showWarning("Veuillez saisir un objet");
                return;
            }
            
            if (textAreaMessage == null || textAreaMessage.getText().isEmpty()) {
                AlertUtils.showWarning("Veuillez saisir un message");
                return;
            }
            
            // Récupérer la priorité
            application.models.PrioriteMessage priorite = application.models.PrioriteMessage.NORMALE;
            if (comboPrioriteComposition != null && comboPrioriteComposition.getValue() != null) {
                String prioriteStr = comboPrioriteComposition.getValue();
                application.models.PrioriteMessage p = 
                    application.models.PrioriteMessage.fromLibelle(prioriteStr);
                if (p != null) {
                    priorite = p;
                }
            }
            
            // Sauvegarder la pièce jointe une seule fois
            String attachmentPath = null;
            if (selectedAttachment != null) {
                attachmentPath = saveAttachment(selectedAttachment);
                if (attachmentPath == null) {
                    AlertUtils.showError("Erreur lors de la sauvegarde de la pièce jointe");
                    return;
                }
            }
            
            // Envoyer un message à chaque destinataire
            int successCount = 0;
            int failCount = 0;
            
            for (User destinataire : destinataires) {
                // Créer un message pour ce destinataire
                Message message = new Message();
                message.setExpediteur(currentUser);
                message.setDestinataire(destinataire);
                message.setObjet(champObjetComposition.getText());
                message.setContenu(textAreaMessage.getText());
                message.setStatut(StatutMessage.ENVOYE);
                message.setPriorite(priorite);
                
                // Ajouter la pièce jointe
                if (attachmentPath != null) {
                    message.setPieceJointe(attachmentPath);
                }
                
                // Envoyer via le service de synchronisation
                if (messageSyncService.envoyerMessage(message)) {
                    successCount++;
                } else {
                    failCount++;
                }
            }
            
            // Afficher le résultat
            if (successCount > 0) {
                String confirmMsg = String.format(
                    "Message envoyé avec succès à %d destinataire%s",
                    successCount,
                    successCount > 1 ? "s" : ""
                );
                
                if (failCount > 0) {
                    confirmMsg += String.format("\n%d échec%s", failCount, failCount > 1 ? "s" : "");
                }
                
                if (selectedAttachment != null) {
                    confirmMsg += "\nPièce jointe: " + selectedAttachment.getName();
                }
                
                AlertUtils.showInfo("Message envoyé", confirmMsg);
                clearCompositionForm();
                loadMessages();
                showEmptyZone();
            } else {
                AlertUtils.showError("Échec de l'envoi à tous les destinataires");
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur lors de l'envoi du message: " + e.getMessage());
        }
    }

    
    /**
     * NOUVEAU: Sauvegarde une pièce jointe
     */
    private String saveAttachment(File file) {
        try {
            // Générer un nom unique pour éviter les conflits
            String uniqueFileName = System.currentTimeMillis() + "_" + file.getName();
            Path targetPath = Paths.get(ATTACHMENTS_DIR + uniqueFileName);
            
            // Copier le fichier
            Files.copy(file.toPath(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            
            System.out.println("✅ Pièce jointe sauvegardée: " + targetPath.toString());
            return targetPath.toString();
            
        } catch (Exception e) {
            System.err.println("❌ Erreur lors de la sauvegarde de la pièce jointe: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Impossible de sauvegarder la pièce jointe");
            return null;
        }
    }
    
    /**
     * Efface le formulaire de composition
     */
    private void clearCompositionForm() {
        if (recipientsSelector != null) {
            recipientsSelector.setSelectedUsers(new ArrayList<>());
        }
        if (champCc != null) champCc.clear();
        if (champObjetComposition != null) champObjetComposition.clear();
        if (textAreaMessage != null) textAreaMessage.clear();
        if (comboPrioriteComposition != null) comboPrioriteComposition.setValue("Normale");
        
        // Réinitialiser la pièce jointe
        selectedAttachment = null;
    }
    
    @FXML
    private void handleSauvegarderBrouillon() {
        try {
            // Validation minimale
            if (champObjetComposition == null || champObjetComposition.getText().trim().isEmpty()) {
                AlertUtils.showWarning("Veuillez saisir au moins un objet pour sauvegarder le brouillon");
                return;
            }
            
            // Récupérer les destinataires
            List<User> destinataires = recipientsSelector != null ? 
                                       recipientsSelector.getSelectedUsers() : 
                                       new ArrayList<>();
            
            // Si plusieurs destinataires, créer un brouillon pour chacun
            // Sinon, créer un seul brouillon avec l'expéditeur comme destinataire temporaire
            
            if (destinataires.isEmpty()) {
                // Aucun destinataire : brouillon simple
                saveSingleDraft(currentUser);
            } else if (destinataires.size() == 1) {
                // Un seul destinataire : brouillon simple
                saveSingleDraft(destinataires.get(0));
            } else {
                // Plusieurs destinataires : créer un brouillon "collectif"
                // On stocke le premier destinataire, les autres seront en Cc ou dans l'objet
                User firstRecipient = destinataires.get(0);
                
                // Ajouter une note dans le contenu pour indiquer les autres destinataires
                String contenuOriginal = textAreaMessage != null ? textAreaMessage.getText() : "";
                StringBuilder contenuAvecDestinataires = new StringBuilder();
                contenuAvecDestinataires.append("[DESTINATAIRES: ");
                for (int i = 0; i < destinataires.size(); i++) {
                    if (i > 0) contenuAvecDestinataires.append(", ");
                    contenuAvecDestinataires.append(destinataires.get(i).getNomComplet());
                }
                contenuAvecDestinataires.append("]\n\n");
                contenuAvecDestinataires.append(contenuOriginal);
                
                saveSingleDraft(firstRecipient, contenuAvecDestinataires.toString());
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la sauvegarde du brouillon: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur lors de la sauvegarde: " + e.getMessage());
        }
    }
    
    /**
     * NOUVEAU: Sauvegarde un brouillon unique
     */
    private void saveSingleDraft(User destinataire) {
        String contenu = textAreaMessage != null ? textAreaMessage.getText() : "";
        saveSingleDraft(destinataire, contenu);
    }
    
    /**
     * NOUVEAU: Sauvegarde un brouillon unique avec contenu spécifique
     */
    private void saveSingleDraft(User destinataire, String contenu) {
        try {
            Message message = new Message();
            message.setExpediteur(currentUser);
            message.setDestinataire(destinataire);
            message.setObjet(champObjetComposition.getText());
            message.setContenu(contenu.isEmpty() ? "(Brouillon vide)" : contenu);
            
            // Définir la priorité
            if (comboPrioriteComposition != null && comboPrioriteComposition.getValue() != null) {
                String prioriteStr = comboPrioriteComposition.getValue();
                application.models.PrioriteMessage priorite = 
                    application.models.PrioriteMessage.fromLibelle(prioriteStr);
                if (priorite != null) {
                    message.setPriorite(priorite);
                }
            }
            
            // IMPORTANT: Définir le statut comme BROUILLON
            message.setStatut(StatutMessage.BROUILLON);
            
            // Copier la pièce jointe si présente
            if (selectedAttachment != null) {
                String attachmentPath = saveAttachment(selectedAttachment);
                if (attachmentPath != null) {
                    message.setPieceJointe(attachmentPath);
                }
            }
            
            // Sauvegarder via le service
            if (messageService.saveMessage(message)) {
                AlertUtils.showInfo("Brouillon sauvegardé", 
                    "Le message a été sauvegardé en brouillon.\n" +
                    "Vous pouvez le retrouver dans le dossier 'Brouillons'.");
                
                clearCompositionForm();
                loadMessages();
                showEmptyZone();
            } else {
                AlertUtils.showError("Erreur lors de la sauvegarde du brouillon");
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la sauvegarde: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    @FXML
    private void handleAnnulerComposition() {
        boolean confirm = AlertUtils.showConfirmation(
            "Confirmation",
            "Voulez-vous annuler la composition du message ?"
        );
        
        if (confirm) {
            clearCompositionForm();
            showEmptyZone();
        }
    }
    
}