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
import application.models.PrioriteMessage;
import application.models.User;
import application.services.MessageService;
import application.utils.SessionManager;
import application.utils.AlertUtils;

import java.net.URL;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;

public class MessagesController implements Initializable {
    
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
    @FXML private TextField champDestinataire;
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
    private ObservableList<Message> messages;
    private Message selectedMessage;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("MessagesController.initialize() appelé");
        
        try {
            currentUser = SessionManager.getInstance().getCurrentUser();
            messageService = MessageService.getInstance();
            messages = FXCollections.observableArrayList();
            
            if (currentUser == null) {
                System.err.println("ERREUR: Aucun utilisateur en session");
                return;
            }
            
            setupFilters();
            setupButtons();
            setupComposition();
            loadMessages();
            
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
        
        if (champDestinataire != null) {
            champDestinataire.setText(selectedMessage.getExpediteur().getNomComplet());
        }
        if (champObjetComposition != null) {
            champObjetComposition.setText("RE: " + selectedMessage.getObjet());
        }
        if (textAreaMessage != null) {
            textAreaMessage.setText("\n\n--- Message original ---\n" + selectedMessage.getContenu());
        }
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
        
        if (champObjetComposition != null) {
            champObjetComposition.setText("TR: " + selectedMessage.getObjet());
        }
        if (textAreaMessage != null) {
            textAreaMessage.setText("\n\n--- Message transféré ---\n" + selectedMessage.getContenu());
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
    
    @FXML
    private void handleEnvoyerMessage() {
        try {
            // Validation
            if (champDestinataire == null || champDestinataire.getText().isEmpty()) {
                AlertUtils.showWarning("Veuillez saisir un destinataire");
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
            
            // Créer le message
            Message message = new Message();
            message.setExpediteur(currentUser);
            // TODO: Récupérer le destinataire réel par son nom
            message.setDestinataire(currentUser); // Temporaire
            message.setObjet(champObjetComposition.getText());
            message.setContenu(textAreaMessage.getText());
            
            if (messageService.saveMessage(message)) {
                AlertUtils.showInfo("Message envoyé avec succès");
                clearCompositionForm();
                loadMessages();
            } else {
                AlertUtils.showError("Erreur lors de l'envoi du message");
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'envoi: " + e.getMessage());
            AlertUtils.showError("Erreur lors de l'envoi du message");
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
    
    private void clearCompositionForm() {
        if (champDestinataire != null) champDestinataire.clear();
        if (champCc != null) champCc.clear();
        if (champObjetComposition != null) champObjetComposition.clear();
        if (textAreaMessage != null) textAreaMessage.clear();
        
        if (zoneComposition != null) {
            zoneComposition.setVisible(false);
            zoneComposition.setManaged(false);
        }
    }
}