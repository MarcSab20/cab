package application.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import application.models.*;
import application.services.*;
import application.utils.SessionManager;
import application.utils.AlertUtils;

import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;

public class RechercheController implements Initializable {
    
    // Recherche principale
    @FXML private TextField champRechercheGlobale;
    @FXML private Button btnRechercherGlobal;
    @FXML private CheckBox checkDocuments;
    @FXML private CheckBox checkCourriers;
    @FXML private CheckBox checkMessages;
    @FXML private CheckBox checkReunions;
    @FXML private DatePicker dateDebutRecherche;
    @FXML private DatePicker dateFinRecherche;
    
    // Filtres avancés
    @FXML private ComboBox<String> comboTypeContenu;
    @FXML private TextField champAuteur;
    @FXML private TextField champMotsCles;
    @FXML private TextField champTailleMin;
    @FXML private TextField champTailleMax;
    @FXML private ComboBox<String> comboUniteTaille;
    
    // Résultats
    @FXML private Label labelNombreResultats;
    @FXML private Label labelStatsResultats;
    @FXML private ComboBox<String> comboTriResultats;
    @FXML private TabPane tabsResultats;
    @FXML private Tab tabTous;
    @FXML private Tab tabDocuments;
    @FXML private Tab tabCourriers;
    @FXML private Tab tabMessages;
    @FXML private Tab tabReunions;
    
    private User currentUser;
    private DocumentService documentService;
    private CourrierService courrierService;
    private MessageService messageService;
    private ReunionService reunionService;
    
    private List<Object> searchResults;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("RechercheController.initialize() appelé");
        
        try {
            currentUser = SessionManager.getInstance().getCurrentUser();
            
            if (currentUser == null) {
                System.err.println("ERREUR: Aucun utilisateur en session");
                return;
            }
            
            // Initialiser les services
            documentService = DocumentService.getInstance();
            courrierService = CourrierService.getInstance();
            messageService = MessageService.getInstance();
            reunionService = ReunionService.getInstance();
            
            searchResults = new ArrayList<>();
            
            setupFilters();
            setupButtons();
            
        } catch (Exception e) {
            System.err.println("Erreur dans RechercheController.initialize(): " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupFilters() {
        // Types de contenu
        if (comboTypeContenu != null) {
            comboTypeContenu.setItems(FXCollections.observableArrayList(
                "Tous les types", "Documents PDF", "Documents Word", "Feuilles de calcul",
                "Images", "Courriers entrants", "Courriers sortants", 
                "Messages internes", "Comptes-rendus"
            ));
            comboTypeContenu.setValue("Tous les types");
        }
        
        // Unités de taille
        if (comboUniteTaille != null) {
            comboUniteTaille.setItems(FXCollections.observableArrayList("KB", "MB", "GB"));
            comboUniteTaille.setValue("KB");
        }
        
        // Tri des résultats
        if (comboTriResultats != null) {
            comboTriResultats.setItems(FXCollections.observableArrayList(
                "Pertinence", "Date (récent)", "Date (ancien)", 
                "Nom (A-Z)", "Nom (Z-A)", "Taille", "Type"
            ));
            comboTriResultats.setValue("Pertinence");
            comboTriResultats.setOnAction(e -> sortResults());
        }
        
        // Cocher toutes les catégories par défaut
        if (checkDocuments != null) checkDocuments.setSelected(true);
        if (checkCourriers != null) checkCourriers.setSelected(true);
        if (checkMessages != null) checkMessages.setSelected(true);
        if (checkReunions != null) checkReunions.setSelected(true);
    }
    
    private void setupButtons() {
        if (btnRechercherGlobal != null) {
            btnRechercherGlobal.setOnAction(e -> handleRechercheGlobale());
        }
    }
    
    @FXML
    private void handleRechercheGlobale() {
        try {
            String searchText = champRechercheGlobale != null ? 
                champRechercheGlobale.getText().trim() : "";
            
            if (searchText.isEmpty()) {
                AlertUtils.showWarning("Veuillez saisir un terme de recherche");
                return;
            }
            
            searchResults.clear();
            long startTime = System.currentTimeMillis();
            
            // Rechercher dans les documents
            if (checkDocuments != null && checkDocuments.isSelected()) {
                searchInDocuments(searchText);
            }
            
            // Rechercher dans les courriers
            if (checkCourriers != null && checkCourriers.isSelected()) {
                searchInCourriers(searchText);
            }
            
            // Rechercher dans les messages
            if (checkMessages != null && checkMessages.isSelected()) {
                searchInMessages(searchText);
            }
            
            // Rechercher dans les réunions
            if (checkReunions != null && checkReunions.isSelected()) {
                searchInReunions(searchText);
            }
            
            long endTime = System.currentTimeMillis();
            double duration = (endTime - startTime) / 1000.0;
            
            // Afficher les résultats
            displayResults(duration);
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la recherche: " + e.getMessage());
            AlertUtils.showError("Erreur lors de la recherche");
        }
    }
    
    private void searchInDocuments(String searchText) {
        try {
            List<Document> documents = documentService.getAllDocuments();
            
            for (Document doc : documents) {
                if (matchesSearch(doc, searchText)) {
                    searchResults.add(doc);
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la recherche dans les documents: " + e.getMessage());
        }
    }
    
    private void searchInCourriers(String searchText) {
        try {
            List<Courrier> courriers = courrierService.getAllCourriers();
            
            for (Courrier courrier : courriers) {
                if (matchesSearch(courrier, searchText)) {
                    searchResults.add(courrier);
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la recherche dans les courriers: " + e.getMessage());
        }
    }
    
    private void searchInMessages(String searchText) {
        try {
            List<Message> messages = messageService.getMessagesForUser(currentUser.getId());
            
            for (Message message : messages) {
                if (matchesSearch(message, searchText)) {
                    searchResults.add(message);
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la recherche dans les messages: " + e.getMessage());
        }
    }
    
    private void searchInReunions(String searchText) {
        try {
            List<Reunion> reunions = reunionService.getAllReunions();
            
            for (Reunion reunion : reunions) {
                if (matchesSearch(reunion, searchText)) {
                    searchResults.add(reunion);
                }
            }
        } catch (Exception e) {
            System.err.println("Erreur lors de la recherche dans les réunions: " + e.getMessage());
        }
    }
    
    private boolean matchesSearch(Object item, String searchText) {
        String search = searchText.toLowerCase();
        
        if (item instanceof Document) {
            Document doc = (Document) item;
            return (doc.getTitre() != null && doc.getTitre().toLowerCase().contains(search)) ||
                   (doc.getDescription() != null && doc.getDescription().toLowerCase().contains(search)) ||
                   (doc.getMotsCles() != null && doc.getMotsCles().toLowerCase().contains(search));
        }
        
        if (item instanceof Courrier) {
            Courrier courrier = (Courrier) item;
            return (courrier.getObjet() != null && courrier.getObjet().toLowerCase().contains(search)) ||
                   (courrier.getExpediteur() != null && courrier.getExpediteur().toLowerCase().contains(search)) ||
                   (courrier.getNumeroCourrier() != null && courrier.getNumeroCourrier().toLowerCase().contains(search));
        }
        
        if (item instanceof Message) {
            Message message = (Message) item;
            return (message.getObjet() != null && message.getObjet().toLowerCase().contains(search)) ||
                   (message.getContenu() != null && message.getContenu().toLowerCase().contains(search));
        }
        
        if (item instanceof Reunion) {
            Reunion reunion = (Reunion) item;
            return (reunion.getTitre() != null && reunion.getTitre().toLowerCase().contains(search)) ||
                   (reunion.getDescription() != null && reunion.getDescription().toLowerCase().contains(search));
        }
        
        return false;
    }
    
    private void displayResults(double duration) {
        if (labelNombreResultats != null) {
            labelNombreResultats.setText("Résultats de recherche");
        }
        
        if (labelStatsResultats != null) {
            labelStatsResultats.setText(String.format("(%d résultats trouvés en %.2fs)", 
                searchResults.size(), duration));
        }
        
        // Créer la vue des résultats
        ScrollPane scrollPane = new ScrollPane();
        scrollPane.setFitToWidth(true);
        VBox resultsBox = new VBox(10);
        resultsBox.setPadding(new Insets(10));
        
        for (Object result : searchResults) {
            HBox resultBox = createResultBox(result);
            resultsBox.getChildren().add(resultBox);
        }
        
        scrollPane.setContent(resultsBox);
        
        // Mettre à jour l'onglet "Tous"
        if (tabTous != null) {
            tabTous.setContent(scrollPane);
            tabTous.setText("Tous (" + searchResults.size() + ")");
        }
        
        // Mettre à jour les autres onglets
        updateTabCounts();
    }
    
    private HBox createResultBox(Object result) {
        HBox box = new HBox(15);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(15));
        box.getStyleClass().add("card");
        box.setStyle("-fx-background-color: white; -fx-background-radius: 8; -fx-cursor: hand;");
        
        // Icône et type
        VBox iconBox = new VBox();
        iconBox.setAlignment(Pos.CENTER);
        Label icon = new Label(getResultIcon(result));
        icon.setStyle("-fx-font-size: 32px;");
        Label type = new Label(getResultType(result));
        type.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 10px;");
        iconBox.getChildren().addAll(icon, type);
        
        // Contenu
        VBox content = new VBox(5);
        HBox.setHgrow(content, Priority.ALWAYS);
        
        Label title = new Label(getResultTitle(result));
        title.setStyle("-fx-font-weight: bold; -fx-font-size: 16px;");
        
        Label description = new Label(getResultDescription(result));
        description.setStyle("-fx-text-fill: #7f8c8d;");
        description.setWrapText(true);
        
        HBox metadata = new HBox(15);
        metadata.getChildren().addAll(getResultMetadata(result));
        
        content.getChildren().addAll(title, description, metadata);
        
        // Boutons d'action
        VBox actions = new VBox(5);
        actions.setAlignment(Pos.CENTER);
        
        Button btnVoir = new Button("👁️ Aperçu");
        btnVoir.getStyleClass().add("button-primary");
        
        Button btnOuvrir = new Button("📂 Ouvrir");
        
        actions.getChildren().addAll(btnVoir, btnOuvrir);
        
        box.getChildren().addAll(iconBox, content, actions);
        
        return box;
    }
    
    private String getResultIcon(Object result) {
        if (result instanceof Document) return "📄";
        if (result instanceof Courrier) return "📧";
        if (result instanceof Message) return "💬";
        if (result instanceof Reunion) return "👥";
        return "📋";
    }
    
    private String getResultType(Object result) {
        if (result instanceof Document) return "DOCUMENT";
        if (result instanceof Courrier) return "COURRIER";
        if (result instanceof Message) return "MESSAGE";
        if (result instanceof Reunion) return "RÉUNION";
        return "AUTRE";
    }
    
    private String getResultTitle(Object result) {
        if (result instanceof Document) {
            return ((Document) result).getTitre();
        }
        if (result instanceof Courrier) {
            return ((Courrier) result).getObjet();
        }
        if (result instanceof Message) {
            return ((Message) result).getObjet();
        }
        if (result instanceof Reunion) {
            return ((Reunion) result).getTitre();
        }
        return "Sans titre";
    }
    
    private String getResultDescription(Object result) {
        if (result instanceof Document) {
            Document doc = (Document) result;
            return doc.getDescription() != null ? doc.getDescription() : "Aucune description";
        }
        if (result instanceof Courrier) {
            Courrier courrier = (Courrier) result;
            return "De: " + courrier.getExpediteur();
        }
        if (result instanceof Message) {
            Message message = (Message) result;
            return message.getApercu(100);
        }
        if (result instanceof Reunion) {
            Reunion reunion = (Reunion) result;
            return reunion.getDescription() != null ? reunion.getDescription() : "Aucune description";
        }
        return "";
    }
    
    private List<Label> getResultMetadata(Object result) {
        List<Label> metadata = new ArrayList<>();
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd MMM yyyy");
        
        if (result instanceof Document) {
            Document doc = (Document) result;
            metadata.add(createMetadataLabel("📁 Documents/" + doc.getTypeDocument()));
            if (doc.getCreePar() != null) {
                metadata.add(createMetadataLabel("👤 " + doc.getCreePar().getNomComplet()));
            }
            if (doc.getDateCreation() != null) {
                metadata.add(createMetadataLabel("📅 " + doc.getDateCreation().format(formatter)));
            }
            metadata.add(createMetadataLabel("📦 " + doc.getTailleFormatee()));
        }
        
        if (result instanceof Courrier) {
            Courrier courrier = (Courrier) result;
            metadata.add(createMetadataLabel("📧 " + courrier.getTypeCourrier().getLibelle()));
            metadata.add(createMetadataLabel("👤 " + courrier.getExpediteur()));
            if (courrier.getDateReception() != null) {
                metadata.add(createMetadataLabel("📅 " + courrier.getDateReception().format(formatter)));
            }
            metadata.add(createStatutLabel(courrier.getStatut().getLibelle()));
        }
        
        if (result instanceof Message) {
            Message message = (Message) result;
            metadata.add(createMetadataLabel("💬 Message interne"));
            metadata.add(createMetadataLabel("👤 " + message.getExpediteur().getNomComplet()));
            if (message.getDateEnvoi() != null) {
                metadata.add(createMetadataLabel("📅 " + message.getDateEnvoi().format(formatter)));
            }
            if (message.getPriorite() != null && message.getPriorite() != PrioriteMessage.NORMALE) {
                metadata.add(createPriorityLabel(message.getPriorite().getLibelle()));
            }
        }
        
        if (result instanceof Reunion) {
            Reunion reunion = (Reunion) result;
            metadata.add(createMetadataLabel("👥 Réunion"));
            if (reunion.getOrganisateur() != null) {
                metadata.add(createMetadataLabel("👤 " + reunion.getOrganisateur().getNomComplet()));
            }
            if (reunion.getDateReunion() != null) {
                metadata.add(createMetadataLabel("📅 " + reunion.getDateReunion().format(formatter)));
            }
            metadata.add(createStatutLabel(reunion.getStatut().getLibelle()));
        }
        
        return metadata;
    }
    
    private Label createMetadataLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        return label;
    }
    
    private Label createStatutLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-background-color: #f39c12; -fx-text-fill: white; " +
                      "-fx-padding: 2 6; -fx-background-radius: 8; -fx-font-size: 10px;");
        return label;
    }
    
    private Label createPriorityLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-background-color: #e74c3c; -fx-text-fill: white; " +
                      "-fx-padding: 2 6; -fx-background-radius: 8; -fx-font-size: 10px;");
        return label;
    }
    
    private void updateTabCounts() {
        int docCount = 0, courrierCount = 0, messageCount = 0, reunionCount = 0;
        
        for (Object result : searchResults) {
            if (result instanceof Document) docCount++;
            else if (result instanceof Courrier) courrierCount++;
            else if (result instanceof Message) messageCount++;
            else if (result instanceof Reunion) reunionCount++;
        }
        
        if (tabDocuments != null) tabDocuments.setText("Documents (" + docCount + ")");
        if (tabCourriers != null) tabCourriers.setText("Courriers (" + courrierCount + ")");
        if (tabMessages != null) tabMessages.setText("Messages (" + messageCount + ")");
        if (tabReunions != null) tabReunions.setText("Réunions (" + reunionCount + ")");
    }
    
    private void sortResults() {
        // TODO: Implémenter le tri des résultats
        AlertUtils.showInfo("Tri des résultats en cours de développement");
    }
}