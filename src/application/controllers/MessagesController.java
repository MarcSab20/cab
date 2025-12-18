package application.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import application.models.Message;
import application.models.MessageGroup;
import application.models.PrioriteMessage;
import application.models.Role;
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

public class MessagesController implements Initializable, MessageSyncService.MessageListener {
    
    // Filtres
    @FXML private ComboBox<String> filtreDossier;
    @FXML private ComboBox<String> filtreStatut;
    @FXML private ComboBox<String> filtrePrioriteMsg;
    @FXML private TextField champRechercheMsg;
    
    // Liste des messages
    @FXML private VBox listeMessages;
    @FXML private Label nombreMessages;
    @FXML private CheckBox checkboxSelectAll;
    
    // Zone de lecture
    @FXML private VBox zoneMessage;
    @FXML private Label labelObjetMessage;
    @FXML private Label labelExpediteurMsg;
    @FXML private Label labelDestinataireMsg;
    @FXML private Label labelDateMessage;
    @FXML private Label labelObjetDetail;
    @FXML private ScrollPane scrollPaneContenu;
    
    // Boutons d'action
    @FXML private Button btnRepondre;
    @FXML private Button btnRepondreATous;
    @FXML private Button btnTransferer;
    @FXML private Button btnMarquerImportant;
    @FXML private Button btnArchiver;
    @FXML private Button btnSupprimer;
    
    // Zone de composition
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
    
    private User currentUser;
    private MessageService messageService;
    private MessageSyncService messageSyncService;
    private ObservableList<User> listeUtilisateurs;
    private ObservableList<Message> messages;
    private Message selectedMessage;
    
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
            configurerComboDestinataire();
            
        } catch (Exception e) {
            System.err.println("Erreur dans MessagesController.initialize(): " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupFilters() {
        // Initialiser les ComboBox
        if (filtreDossier != null) {
            filtreDossier.setItems(FXCollections.observableArrayList(
                "Boîte de réception", "Messages envoyés", "Brouillons", 
                "Messages importants", "Archive", "Corbeille"
            ));
            filtreDossier.setValue("Boîte de réception");
            filtreDossier.setOnAction(e -> applyFilters());
        }
        
        if (filtreStatut != null) {
            filtreStatut.setItems(FXCollections.observableArrayList(
                "Tous", "Non lus", "Lus", "Importants"
            ));
            filtreStatut.setValue("Tous");
            filtreStatut.setOnAction(e -> applyFilters());
        }
        
        if (filtrePrioriteMsg != null) {
            filtrePrioriteMsg.setItems(FXCollections.observableArrayList(
                "Toutes", "Très haute", "Haute", "Normale", "Basse"
            ));
            filtrePrioriteMsg.setValue("Toutes");
            filtrePrioriteMsg.setOnAction(e -> applyFilters());
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
            comboPrioriteComposition.setItems(FXCollections.observableArrayList(
                "Basse", "Normale", "Haute", "Très haute"
            ));
            comboPrioriteComposition.setValue("Normale");
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
        
        // Masquer la zone de composition par défaut
        if (zoneComposition != null) {
            zoneComposition.setVisible(false);
            zoneComposition.setManaged(false);
        }
    }
    
    private void loadMessages() {
        try {
            List<Message> list = messageService.getMessagesForUser(currentUser.getId());
            messages.clear();
            messages.addAll(list);
            
            if (nombreMessages != null) {
                nombreMessages.setText("(" + messages.size() + " messages)");
            }
            
            displayMessagesList();
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des messages: " + e.getMessage());
            AlertUtils.showError("Erreur lors du chargement des messages");
        }
    }
    
    private void displayMessagesList() {
        if (listeMessages == null) return;
        
        listeMessages.getChildren().clear();
        
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
        
        if (labelObjetMessage != null) {
            labelObjetMessage.setText(message.getObjet());
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
            
            // Diviser le contenu en paragraphes
            String[] paragraphes = message.getContenu().split("\n");
            for (String paragraphe : paragraphes) {
                Label label = new Label(paragraphe);
                label.setWrapText(true);
                contenuBox.getChildren().add(label);
            }
            
            scrollPaneContenu.setContent(contenuBox);
        }
        
        // Afficher la zone de message
        if (zoneMessage != null) {
            zoneMessage.setVisible(true);
            zoneMessage.setManaged(true);
        }
        
        // Masquer la zone de composition
        if (zoneComposition != null) {
            zoneComposition.setVisible(false);
            zoneComposition.setManaged(false);
        }
    }
    
    @FXML
    private void handleRecherche() {
        applyFilters();
    }
    
    private void applyFilters() {
        try {
            String dossierFilter = filtreDossier != null ? filtreDossier.getValue() : "Boîte de réception";
            String statutFilter = filtreStatut != null ? filtreStatut.getValue() : "Tous";
            String searchText = champRechercheMsg != null ? champRechercheMsg.getText().toLowerCase() : "";
            
            List<Message> allMessages = messageService.getMessagesForUser(currentUser.getId());
            ObservableList<Message> filtered = FXCollections.observableArrayList();
            
            for (Message m : allMessages) {
                boolean matches = true;
                
                // Filtre statut
                if (statutFilter.equals("Non lus") && m.isLu()) {
                    matches = false;
                } else if (statutFilter.equals("Lus") && !m.isLu()) {
                    matches = false;
                } else if (statutFilter.equals("Importants") && !m.isImportant()) {
                    matches = false;
                }
                
                // Recherche textuelle
                if (!searchText.isEmpty()) {
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
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'application des filtres: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleRepondre() {
        if (selectedMessage == null) {
            AlertUtils.showWarning("Veuillez sélectionner un message");
            return;
        }

        showCompositionForm();

        // Utiliser comboDestinataire avec setValue()
        if (comboDestinataire != null) {
            // Sélectionner l'expéditeur du message comme destinataire
            User expediteur = selectedMessage.getExpediteur();
            comboDestinataire.setValue(expediteur);
        }
        
        if (champObjetComposition != null) {
            String objet = selectedMessage.getObjet();
            // Éviter les "RE: RE: RE:" multiples
            if (!objet.startsWith("RE:")) {
                champObjetComposition.setText("RE: " + objet);
            } else {
                champObjetComposition.setText(objet);
            }
        }
        
        if (textAreaMessage != null) {
            textAreaMessage.setText("\n\n--- Message original ---\n" + selectedMessage.getContenu());
            // Positionner le curseur au début pour faciliter la rédaction
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
            chargerListeUtilisateurs(); // Rafraîchir aussi la liste
        });
    }
    
    @FXML
    private void handleRepondreATous() {
        AlertUtils.showInfo("Fonction de réponse à tous en cours de développement");
    }
    
    @FXML
    private void handleTransferer() {
        if (selectedMessage == null) {
            AlertUtils.showWarning("Veuillez sélectionner un message");
            return;
        }

        showCompositionForm();

        // Laisser le destinataire vide (l'utilisateur choisira)
        if (comboDestinataire != null) {
            comboDestinataire.setValue(null);
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
            AlertUtils.showInfo("Message marqué comme " + 
                (selectedMessage.isImportant() ? "important" : "non important"));
            displayMessagesList();
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
                
                // Masquer les détails
                if (zoneMessage != null) {
                    zoneMessage.setVisible(false);
                    zoneMessage.setManaged(false);
                }
            } else {
                AlertUtils.showError("Erreur lors de la suppression");
            }
        }
    }
    
    private void showCompositionForm() {
        if (zoneComposition != null) {
            zoneComposition.setVisible(true);
            zoneComposition.setManaged(true);
        }
        
        if (zoneMessage != null) {
            zoneMessage.setVisible(false);
            zoneMessage.setManaged(false);
        }
    }
    
    /**
     * Charge la liste de tous les utilisateurs actifs
     */
    private void chargerListeUtilisateurs() {
        try {
            List<User> users = getAllActiveUsers();
            listeUtilisateurs = FXCollections.observableArrayList(users);
            
            if (comboDestinataire != null) {
                comboDestinataire.setItems(listeUtilisateurs);
            }
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des utilisateurs: " + e.getMessage());
        }
    }
    
    /**
     * Configure le ComboBox pour afficher les noms complets
     */
    private void configurerComboDestinataire() {
        if (comboDestinataire != null) {
            // Définir comment afficher les utilisateurs
            comboDestinataire.setCellFactory(param -> new ListCell<User>() {
                @Override
                protected void updateItem(User user, boolean empty) {
                    super.updateItem(user, empty);
                    if (empty || user == null) {
                        setText(null);
                    } else {
                        // Afficher avec icône de statut
                        UserPresence presence = messageSyncService.getUserPresence(user.getId());
                        String statut = presence != null ? presence.getStatut().getIcone() : "⚫";
                        setText(statut + " " + user.getNomComplet() + " (" + user.getCode() + ")");
                    }
                }
            });
            
            // Définir comment afficher l'élément sélectionné
            comboDestinataire.setButtonCell(new ListCell<User>() {
                @Override
                protected void updateItem(User user, boolean empty) {
                    super.updateItem(user, empty);
                    if (empty || user == null) {
                        setText(null);
                    } else {
                        setText(user.getNomComplet());
                    }
                }
            });
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
            // Validation du destinataire
            if (comboDestinataire == null || comboDestinataire.getValue() == null) {
                AlertUtils.showWarning("Veuillez sélectionner un destinataire");
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
            
            // Récupérer le destinataire sélectionné
            User destinataire = comboDestinataire.getValue();
            
            // Créer le message
            Message message = new Message();
            message.setExpediteur(currentUser);
            message.setDestinataire(destinataire);
            message.setObjet(champObjetComposition.getText());
            message.setContenu(textAreaMessage.getText());
            
            // Définir la priorité si sélectionnée
            if (comboPrioriteComposition != null && comboPrioriteComposition.getValue() != null) {
                String prioriteStr = comboPrioriteComposition.getValue();
                PrioriteMessage priorite = PrioriteMessage.fromLibelle(prioriteStr);
                if (priorite != null) {
                    message.setPriorite(priorite);
                }
            }
            
            // Envoyer via le service de synchronisation
            if (messageSyncService.envoyerMessage(message)) {
                AlertUtils.showInfo("Message envoyé avec succès à " + destinataire.getNomComplet());
                clearCompositionForm();
                loadMessages();
            } else {
                AlertUtils.showError("Erreur lors de l'envoi du message");
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi: " + e.getMessage());
            e.printStackTrace();
            AlertUtils.showError("Erreur lors de l'envoi du message: " + e.getMessage());
        }
    }
    
    /**
     * Efface le formulaire de composition
     */
    private void clearCompositionForm() {
        if (comboDestinataire != null) comboDestinataire.setValue(null);
        if (champCc != null) champCc.clear();
        if (champObjetComposition != null) champObjetComposition.clear();
        if (textAreaMessage != null) textAreaMessage.clear();
        if (comboPrioriteComposition != null) comboPrioriteComposition.setValue("Normale");
        
        if (zoneComposition != null) {
            zoneComposition.setVisible(false);
            zoneComposition.setManaged(false);
        }
    }
    
    @FXML
    private void handleCreerGroupe() {
        // Demander le nom du groupe
        Optional<String> result = AlertUtils.showTextInput(
            "Nouveau groupe",
            "Nom du groupe:",
            ""
        );
        
        if (result.isPresent() && !result.get().trim().isEmpty()) {
            MessageGroup groupe = new MessageGroup(result.get(), currentUser);
            groupe.setDescription("Groupe de discussion");
            groupe.setTypeGroupe(MessageGroup.TypeGroupe.PRIVE);
            
            if (messageSyncService.creerGroupe(groupe)) {
                AlertUtils.showInfo("Groupe créé avec succès");
                // Ajouter des membres...
            }
        }
    }
    
    @FXML
    private void handleEnvoyerMessageGroupe(MessageGroup groupe) {
        Message message = new Message();
        message.setExpediteur(currentUser);
        message.setObjet(champObjetComposition.getText());
        message.setContenu(textAreaMessage.getText());
        
        if (messageSyncService.envoyerMessageGroupe(message, groupe)) {
            AlertUtils.showInfo("Message envoyé au groupe");
        }
    }
    
    private void afficherStatutPresence(User user, Label statusLabel) {
        UserPresence presence = messageSyncService.getUserPresence(user.getId());
        
        if (presence != null) {
            statusLabel.setText(presence.getStatutAvecIcone());
            statusLabel.setStyle(presence.getStyleStatut());
        }
    }
    
    @FXML
    private void handleSauvegarderBrouillon() {
        AlertUtils.showInfo("Message sauvegardé en brouillon");
    }
    
    @FXML
    private void handleAnnulerComposition() {
        boolean confirm = AlertUtils.showConfirmation(
            "Confirmation",
            "Voulez-vous annuler la composition du message ?"
        );
        
        if (confirm) {
            clearCompositionForm();
        }
    }
    
}