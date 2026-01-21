package application.controllers;

import application.models.Courrier;
import application.models.Courrier.StatutCourrier;
import application.models.Courrier.TypeCourrier;
import application.models.CourrierNotificationInfo;
import application.models.Courrier.PrioriteCourrier;
import application.models.Document;
import application.models.User;
import application.services.CourrierService;
import application.services.DocumentService;
import application.services.LogService;
import application.services.NotificationCourrierService;
import application.utils.SessionManager;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.beans.property.SimpleStringProperty;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;
import java.util.Timer;
import java.util.TimerTask;

/**
 * Contrôleur pour la gestion des courriers avec système de notifications
 */
public class CourrierController {
    
    // ==================== COMPOSANTS FXML - GÉNÉRAL ====================
    
    @FXML private TabPane tabPaneCourrier;
    @FXML private Tab tabNotifications;
    
    // Badge de notifications
    @FXML private HBox badgeNotifications;
    @FXML private Label labelBadgeNotifications;
    
    // ==================== COMPOSANTS FXML - TOUS LES COURRIERS ====================
    
    @FXML private TableView<Courrier> tableauCourriers;
    @FXML private TableColumn<Courrier, String> colonneCode;
    @FXML private TableColumn<Courrier, String> colonneType;
    @FXML private TableColumn<Courrier, String> colonneObjet;
    @FXML private TableColumn<Courrier, String> colonneExpediteur;
    @FXML private TableColumn<Courrier, String> colonneDestinataire;
    @FXML private TableColumn<Courrier, String> colonneDateCourrier;
    @FXML private TableColumn<Courrier, String> colonnePriorite;
    @FXML private TableColumn<Courrier, String> colonneStatut;
    @FXML private TableColumn<Courrier, String> colonneActions;
    
    @FXML private Button btnNouveauCourrier;
    @FXML private Button btnActualiser;
    @FXML private Button btnRechercher;
    
    @FXML private ComboBox<String> filtreType;
    @FXML private ComboBox<String> filtreStatut;
    @FXML private ComboBox<String> filtrePriorite;
    @FXML private ComboBox<String> comboLimiteCourriers;
    
    @FXML private TextField champRecherche;
    
    @FXML private Label statNouveaux;
    @FXML private Label statEnCours;
    @FXML private Label statTraites;
    @FXML private Label statArchives;
    @FXML private Label labelInfo;
    
    // ==================== COMPOSANTS FXML - NOTIFICATIONS ====================
    
    @FXML private TableView<CourrierNotification> tableauNotifications;
    @FXML private TableColumn<CourrierNotification, String> colonneNotifStatutLu;
    @FXML private TableColumn<CourrierNotification, String> colonneNotifCode;
    @FXML private TableColumn<CourrierNotification, String> colonneNotifType;
    @FXML private TableColumn<CourrierNotification, String> colonneNotifObjet;
    @FXML private TableColumn<CourrierNotification, String> colonneNotifExpediteur;
    @FXML private TableColumn<CourrierNotification, String> colonneNotifDateNotification;
    @FXML private TableColumn<CourrierNotification, String> colonneNotifPriorite;
    @FXML private TableColumn<CourrierNotification, String> colonneNotifActions;
    
    @FXML private RadioButton radioNonLus;
    @FXML private Button btnMarquerToutLu;
    @FXML private Label labelInfoNotifications;
    
    // ==================== SERVICES ====================
    
    private CourrierService courrierService;
    private DocumentService documentService;
    private LogService logService;
    private NotificationCourrierService notificationService;
    
    private ObservableList<Courrier> courriers;
    private ObservableList<Courrier> courriersFiltrés;
    private ObservableList<CourrierNotification> notifications;
    
    private Timer refreshTimer;
    
    private static final DateTimeFormatter DATE_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
    
    // ==================== INITIALISATION ====================
    
    @FXML
    public void initialize() {
        System.out.println("=== INITIALISATION CourrierController ===");
        
        courrierService = CourrierService.getInstance();
        documentService = DocumentService.getInstance();
        logService = LogService.getInstance();
        notificationService = NotificationCourrierService.getInstance();
        
        courriers = FXCollections.observableArrayList();
        courriersFiltrés = FXCollections.observableArrayList();
        notifications = FXCollections.observableArrayList();
        
        configurerColonnesTableauCourriers();
        configurerColonnesTableauNotifications();
        configurerFiltres();
        
        chargerCourriers();
        chargerCourriersNotifies();
        mettreAJourStatistiques();
        afficherBadgeNotifications();
        
        // Rafraîchissement automatique toutes les 30 secondes
        demarrerRafraichissementAutomatique();
        
        // Listener pour le changement d'onglet
        if (tabPaneCourrier != null) {
            tabPaneCourrier.getSelectionModel().selectedItemProperty().addListener(
                (obs, oldTab, newTab) -> {
                    if (newTab == tabNotifications) {
                        chargerCourriersNotifies();
                    }
                }
            );
        }
        
        // Listener pour le filtre non lus
        if (radioNonLus != null) {
            radioNonLus.selectedProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal) {
                    chargerCourriersNotifies();
                }
            });
        }
        
        System.out.println("=== FIN INITIALISATION ===");
    }
    
    /**
     * Configure les colonnes du tableau principal
     */
    private void configurerColonnesTableauCourriers() {
        colonneCode.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getCodeCourrier()));
        
        colonneType.setCellValueFactory(cellData -> {
            TypeCourrier type = cellData.getValue().getTypeCourrier();
            return new SimpleStringProperty(type != null ? type.getIcone() + " " + type.getLibelle() : "");
        });
        
        colonneObjet.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getObjet()));
        
        colonneExpediteur.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getExpediteur()));
        
        colonneDestinataire.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getDestinataire()));
        
        colonneDateCourrier.setCellValueFactory(cellData -> {
            if (cellData.getValue().getDateCourrier() != null) {
                return new SimpleStringProperty(
                    cellData.getValue().getDateCourrier().format(DATE_FORMATTER));
            }
            return new SimpleStringProperty("");
        });
        
        colonnePriorite.setCellValueFactory(cellData -> {
            PrioriteCourrier priorite = cellData.getValue().getPriorite();
            return new SimpleStringProperty(
                priorite != null ? priorite.getIcone() + " " + priorite.getLibelle() : "");
        });
        
        colonneStatut.setCellValueFactory(cellData -> {
            StatutCourrier statut = cellData.getValue().getStatut();
            return new SimpleStringProperty(
                statut != null ? statut.getIcone() + " " + statut.getLibelle() : "");
        });
        
        colonneActions.setCellFactory(column -> new TableCell<Courrier, String>() {
            private final Button btnVoir = new Button("👁");
            private final Button btnModifier = new Button("✏️");
            private final Button btnArchiver = new Button("📁");
            
            {
                btnVoir.setOnAction(event -> voirCourrier(getTableView().getItems().get(getIndex())));
                btnModifier.setOnAction(event -> changerStatutCourrier(getTableView().getItems().get(getIndex())));
                btnArchiver.setOnAction(event -> archiverCourrier(getTableView().getItems().get(getIndex())));
                
                btnVoir.setStyle("-fx-font-size: 12px; -fx-padding: 4 8;");
                btnModifier.setStyle("-fx-font-size: 12px; -fx-padding: 4 8;");
                btnArchiver.setStyle("-fx-font-size: 12px; -fx-padding: 4 8;");
            }
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Courrier courrier = getTableView().getItems().get(getIndex());
                    HBox buttons = new HBox(5);
                    
                    buttons.getChildren().addAll(btnVoir, btnModifier);
                    
                    if (courrier.getStatut() == StatutCourrier.TRAITE) {
                        buttons.getChildren().add(btnArchiver);
                    }
                    
                    setGraphic(buttons);
                }
            }
        });
    }
    
    /**
     * Configure les colonnes du tableau des notifications
     */
    private void configurerColonnesTableauNotifications() {
        colonneNotifStatutLu.setCellValueFactory(cellData -> {
            boolean lu = cellData.getValue().isLu();
            String statut = lu ? "✅ Lu" : "🔴 Non lu";
            return new SimpleStringProperty(statut);
        });
        
        // Style personnalisé pour la colonne statut
        colonneNotifStatutLu.setCellFactory(column -> new TableCell<CourrierNotification, String>() {
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                    setStyle("");
                } else {
                    setText(item);
                    if (item.contains("Non lu")) {
                        setStyle("-fx-font-weight: bold; -fx-text-fill: #e74c3c;");
                    } else {
                        setStyle("-fx-text-fill: #27ae60;");
                    }
                }
            }
        });
        
        colonneNotifCode.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getCourrier().getCodeCourrier()));
        
        colonneNotifType.setCellValueFactory(cellData -> {
            TypeCourrier type = cellData.getValue().getCourrier().getTypeCourrier();
            return new SimpleStringProperty(type != null ? type.getIcone() + " " + type.getLibelle() : "");
        });
        
        colonneNotifObjet.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getCourrier().getObjet()));
        
        colonneNotifExpediteur.setCellValueFactory(cellData -> 
            new SimpleStringProperty(cellData.getValue().getCourrier().getExpediteur()));
        
        colonneNotifDateNotification.setCellValueFactory(cellData -> {
            if (cellData.getValue().getDateNotification() != null) {
                return new SimpleStringProperty(
                    cellData.getValue().getDateNotification().format(DATETIME_FORMATTER));
            }
            return new SimpleStringProperty("");
        });
        
        colonneNotifPriorite.setCellValueFactory(cellData -> {
            PrioriteCourrier priorite = cellData.getValue().getCourrier().getPriorite();
            return new SimpleStringProperty(
                priorite != null ? priorite.getIcone() + " " + priorite.getLibelle() : "");
        });
        
        colonneNotifActions.setCellFactory(column -> new TableCell<CourrierNotification, String>() {
            private final Button btnOuvrir = new Button("📖 Ouvrir");
            private final Button btnMarquerLu = new Button("✅ Marquer lu");
            
            {
                btnOuvrir.setOnAction(event -> {
                    CourrierNotification notif = getTableView().getItems().get(getIndex());
                    ouvrirCourrierDepuisNotification(notif);
                });
                
                btnMarquerLu.setOnAction(event -> {
                    CourrierNotification notif = getTableView().getItems().get(getIndex());
                    marquerCommeLu(notif);
                });
                
                btnOuvrir.setStyle("-fx-font-size: 11px; -fx-padding: 4 8;");
                btnMarquerLu.setStyle("-fx-font-size: 11px; -fx-padding: 4 8;");
            }
            
            @Override
            protected void updateItem(String item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    CourrierNotification notif = getTableView().getItems().get(getIndex());
                    HBox buttons = new HBox(5);
                    
                    buttons.getChildren().add(btnOuvrir);
                    
                    if (!notif.isLu()) {
                        buttons.getChildren().add(btnMarquerLu);
                    }
                    
                    setGraphic(buttons);
                }
            }
        });
        
        // Style pour les lignes non lues
        tableauNotifications.setRowFactory(tv -> new TableRow<CourrierNotification>() {
            @Override
            protected void updateItem(CourrierNotification notif, boolean empty) {
                super.updateItem(notif, empty);
                if (empty || notif == null) {
                    setStyle("");
                } else if (!notif.isLu()) {
                    setStyle("-fx-background-color: #fff3cd; -fx-font-weight: bold;");
                } else {
                    setStyle("");
                }
            }
        });
    }
    
    /**
     * Configure les filtres
     */
    private void configurerFiltres() {
        if (filtreType != null) {
            filtreType.setValue("Tous");
            filtreType.setOnAction(e -> appliquerFiltres());
        }
        
        if (filtreStatut != null) {
            filtreStatut.setValue("Tous");
            filtreStatut.setOnAction(e -> appliquerFiltres());
        }
        
        if (filtrePriorite != null) {
            filtrePriorite.setValue("Tous");
            filtrePriorite.setOnAction(e -> appliquerFiltres());
        }
        
        if (comboLimiteCourriers != null) {
            comboLimiteCourriers.setValue("50");
            comboLimiteCourriers.setOnAction(e -> chargerCourriers());
        }
    }
    
    // ==================== CHARGEMENT DES DONNÉES ====================
    
    /**
     * Charge tous les courriers
     */
    private void chargerCourriers() {
        try {
            List<Courrier> listeCourriers = courrierService.rechercherCourriers("");
            
            courriers.clear();
            courriers.addAll(listeCourriers);
            
            appliquerFiltres();
            
            System.out.println("✓ " + listeCourriers.size() + " courrier(s) chargé(s)");
            
        } catch (Exception e) {
            System.err.println("Erreur chargement courriers: " + e.getMessage());
            showError("Erreur lors du chargement des courriers");
        }
    }
    
    /**
     * Charge les courriers notifiés pour l'utilisateur courant
     */
    private void chargerCourriersNotifies() {
        User currentUser = getCurrentUser();
        if (currentUser == null) return;
        
        try {
            boolean seulementNonLus = radioNonLus != null && radioNonLus.isSelected();
            
            // Utiliser la nouvelle méthode qui retourne les infos complètes
            List<CourrierNotificationInfo> infosNotifications = 
                notificationService.getCourriersNotifiesAvecInfos(currentUser.getId(), seulementNonLus);
            
            // Créer les objets CourrierNotification pour l'affichage
            notifications.clear();
            
            for (CourrierNotificationInfo info : infosNotifications) {
                CourrierNotification notif = new CourrierNotification(info.getCourrier());
                notif.setLu(info.isLu());
                notif.setDateNotification(info.getDateNotification());
                notif.setDateLecture(info.getDateLecture());
                
                notifications.add(notif);
            }
            
            tableauNotifications.setItems(notifications);
            
            if (labelInfoNotifications != null) {
                String texte = notifications.size() + " notification(s)";
                if (seulementNonLus) {
                    texte += " non lue(s)";
                }
                labelInfoNotifications.setText(texte);
            }
            
            System.out.println("✓ " + infosNotifications.size() + " courrier(s) notifié(s) chargé(s)");
            
        } catch (Exception e) {
            System.err.println("Erreur chargement courriers notifiés: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Affiche le badge de notifications
     */
    private void afficherBadgeNotifications() {
        User currentUser = getCurrentUser();
        if (currentUser == null) return;
        
        try {
            int nbNonLus = notificationService.compterCourriersNonLus(currentUser.getId());
            
            if (badgeNotifications != null && labelBadgeNotifications != null) {
                if (nbNonLus > 0) {
                    badgeNotifications.setVisible(true);
                    badgeNotifications.setManaged(true);
                    labelBadgeNotifications.setText(String.valueOf(nbNonLus));
                    
                    // Animation clic sur le badge
                    badgeNotifications.setOnMouseClicked(e -> {
                        tabPaneCourrier.getSelectionModel().select(tabNotifications);
                    });
                    
                    System.out.println("🔔 " + nbNonLus + " nouveau(x) courrier(s) non lu(s)");
                } else {
                    badgeNotifications.setVisible(false);
                    badgeNotifications.setManaged(false);
                }
            }
            
        } catch (Exception e) {
            System.err.println("Erreur affichage badge: " + e.getMessage());
        }
    }
    
    /**
     * Applique les filtres
     */
    private void appliquerFiltres() {
        courriersFiltrés.clear();
        
        String typeFiltre = filtreType != null ? filtreType.getValue() : "Tous";
        String statutFiltre = filtreStatut != null ? filtreStatut.getValue() : "Tous";
        String prioriteFiltre = filtrePriorite != null ? filtrePriorite.getValue() : "Tous";
        
        List<Courrier> resultats = courriers.stream()
            .filter(c -> "Tous".equals(typeFiltre) || 
                        c.getTypeCourrier().getLibelle().equals(typeFiltre))
            .filter(c -> "Tous".equals(statutFiltre) || 
                        c.getStatut().getLibelle().equals(statutFiltre))
            .filter(c -> "Tous".equals(prioriteFiltre) || 
                        c.getPriorite().getLibelle().equals(prioriteFiltre))
            .collect(Collectors.toList());
        
        courriersFiltrés.addAll(resultats);
        tableauCourriers.setItems(courriersFiltrés);
        
        if (labelInfo != null) {
            labelInfo.setText(courriersFiltrés.size() + " courrier(s)");
        }
    }
    
    /**
     * Met à jour les statistiques
     */
    private void mettreAJourStatistiques() {
        try {
            int nouveaux = courrierService.getCourriersByStatut(StatutCourrier.NOUVEAU).size();
            int enCours = courrierService.getCourriersByStatut(StatutCourrier.EN_COURS).size();
            int traites = courrierService.getCourriersByStatut(StatutCourrier.TRAITE).size();
            int archives = courrierService.getCourriersByStatut(StatutCourrier.ARCHIVE).size();
            
            if (statNouveaux != null) statNouveaux.setText(String.valueOf(nouveaux));
            if (statEnCours != null) statEnCours.setText(String.valueOf(enCours));
            if (statTraites != null) statTraites.setText(String.valueOf(traites));
            if (statArchives != null) statArchives.setText(String.valueOf(archives));
            
        } catch (Exception e) {
            System.err.println("Erreur mise à jour statistiques: " + e.getMessage());
        }
    }
    
    // ==================== ACTIONS ====================
    
    /**
     * Ouvre un courrier depuis une notification
     */
    private void ouvrirCourrierDepuisNotification(CourrierNotification notification) {
        if (notification == null) return;
        
        User currentUser = getCurrentUser();
        if (currentUser == null) return;
        
        // Marquer comme lu
        if (!notification.isLu()) {
            boolean marked = notificationService.marquerCommeLu(
                notification.getCourrier().getId(), 
                currentUser.getId());
            
            if (marked) {
                notification.setLu(true);
                tableauNotifications.refresh();
                afficherBadgeNotifications();
            }
        }
        
        // Afficher les détails du courrier
        voirCourrier(notification.getCourrier());
        
        // Passer à l'onglet principal
        if (tabPaneCourrier != null) {
            tabPaneCourrier.getSelectionModel().selectFirst();
        }
    }
    
    /**
     * Marque une notification comme lue
     */
    private void marquerCommeLu(CourrierNotification notification) {
        if (notification == null || notification.isLu()) return;
        
        User currentUser = getCurrentUser();
        if (currentUser == null) return;
        
        boolean success = notificationService.marquerCommeLu(
            notification.getCourrier().getId(), 
            currentUser.getId());
        
        if (success) {
            notification.setLu(true);
            tableauNotifications.refresh();
            afficherBadgeNotifications();
            showInfo("✅ Courrier marqué comme lu");
        } else {
            showError("Erreur lors du marquage");
        }
    }
    
    /**
     * Marque toutes les notifications comme lues
     */
    @FXML
    private void handleMarquerToutLu() {
        User currentUser = getCurrentUser();
        if (currentUser == null) return;
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Marquer tout comme lu");
        confirm.setContentText("Voulez-vous marquer toutes vos notifications comme lues ?");
        
        if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
            int marquees = 0;
            
            for (CourrierNotification notif : notifications) {
                if (!notif.isLu()) {
                    boolean success = notificationService.marquerCommeLu(
                        notif.getCourrier().getId(), 
                        currentUser.getId());
                    
                    if (success) {
                        notif.setLu(true);
                        marquees++;
                    }
                }
            }
            
            tableauNotifications.refresh();
            afficherBadgeNotifications();
            showSuccess("✅ " + marquees + " notification(s) marquée(s) comme lue(s)");
        }
    }
    
    /**
     * Démarre le rafraîchissement automatique
     */
    private void demarrerRafraichissementAutomatique() {
        refreshTimer = new Timer(true);
        refreshTimer.scheduleAtFixedRate(new TimerTask() {
            @Override
            public void run() {
                Platform.runLater(() -> {
                    afficherBadgeNotifications();
                    
                    // Si l'onglet notifications est ouvert, rafraîchir
                    if (tabPaneCourrier.getSelectionModel().getSelectedItem() == tabNotifications) {
                        chargerCourriersNotifies();
                    }
                });
            }
        }, 30000, 30000); // Toutes les 30 secondes
    }
    
// ==================== ACTIONS ====================
    
    /**
     * Crée un nouveau courrier
     */
    @FXML
    private void handleNouveauCourrier() {
        System.out.println("Action: Nouveau courrier");
        
        try {
            // 1. Sélectionner un document (OBLIGATOIRE)
            Document document = selectionnerDocument();
            
            if (document == null) {
                showWarning("Un document doit être sélectionné pour créer un courrier");
                return;
            }
            
            // 2. Afficher le dialogue de création
            CourrierFormDialog dialog = new CourrierFormDialog(document);
            Optional<Courrier> result = dialog.showAndWait();
            
            if (result.isPresent()) {
                Courrier courrier = result.get();
                
                // 3. Créer le courrier
                User currentUser = getCurrentUser();
                if (currentUser != null) {
                    courrier.setCreePar(currentUser.getId());
                }
                
                Courrier courrierCree = courrierService.createCourrier(courrier);
                
                // 4. Logger l'action
                logService.logAction("creation_courrier", 
                    "Courrier créé: " + courrierCree.getCodeCourrier() + 
                    " - Document: " + document.getCodeDocument());
                
                showSuccess("✅ Courrier créé avec succès !\n\nCode: " + 
                           courrierCree.getCodeCourrier());
                
                // 5. Rafraîchir la liste
                chargerCourriers();
                mettreAJourStatistiques();
            }
            
        } catch (Exception e) {
            System.err.println("Erreur création courrier: " + e.getMessage());
            e.printStackTrace();
            showError("Erreur lors de la création du courrier:\n" + e.getMessage());
        }
    }
    
    /**
     * Sélectionne un document pour le courrier
     */
    private Document selectionnerDocument() {
        try {
            List<Document> documents = documentService.getAllDocuments();
            
            if (documents.isEmpty()) {
                showWarning("Aucun document disponible.\nVeuillez d'abord créer un document.");
                return null;
            }
            
            // Créer une boîte de dialogue de sélection
            ChoiceDialog<Document> dialog = new ChoiceDialog<>(documents.get(0), documents);
            dialog.setTitle("Sélectionner un document");
            dialog.setHeaderText("Sélectionnez le document à associer au courrier");
            dialog.setContentText("Document:");
            
            // Personnaliser l'affichage des documents
            dialog.getDialogPane().setContent(createDocumentSelector(documents));
            
            Optional<Document> result = dialog.showAndWait();
            return result.orElse(null);
            
        } catch (Exception e) {
            System.err.println("Erreur sélection document: " + e.getMessage());
            return null;
        }
    }
    
    /**
     * Crée un sélecteur de document personnalisé
     */
    private javafx.scene.Node createDocumentSelector(List<Document> documents) {
        ListView<Document> listView = new ListView<>();
        listView.getItems().addAll(documents);
        
        listView.setCellFactory(param -> new ListCell<Document>() {
            @Override
            protected void updateItem(Document doc, boolean empty) {
                super.updateItem(doc, empty);
                if (empty || doc == null) {
                    setText(null);
                    setGraphic(null);
                } else {
                    setText(String.format("📄 %s - %s (%s)", 
                        doc.getCodeDocument(), 
                        doc.getTitre(),
                        doc.getTailleFormatee()));
                }
            }
        });
        
        listView.setPrefHeight(300);
        return listView;
    }
    
    /**
     * Voir les détails d'un courrier
     */
    private void voirCourrier(Courrier courrier) {
        if (courrier == null) return;
        
        try {
            // Récupérer le document associé
            Document document = documentService.getDocumentById(courrier.getDocumentId());
            
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Détails du courrier");
            alert.setHeaderText(courrier.getCodeCourrier());
            
            String details = String.format(
                "Type: %s\n" +
                "Objet: %s\n" +
                "Expéditeur: %s\n" +
                "Destinataire: %s\n" +
                "Référence: %s\n" +
                "Date: %s\n" +
                "Priorité: %s\n" +
                "Statut: %s\n" +
                "Confidentiel: %s\n\n" +
                "Document associé:\n" +
                "Code: %s\n" +
                "Titre: %s\n\n" +
                "Observations:\n%s",
                courrier.getTypeCourrier().getLibelle(),
                courrier.getObjet(),
                courrier.getExpediteur() != null ? courrier.getExpediteur() : "-",
                courrier.getDestinataire() != null ? courrier.getDestinataire() : "-",
                courrier.getReference() != null ? courrier.getReference() : "-",
                courrier.getDateCourrier() != null ? 
                    courrier.getDateCourrier().format(DATE_FORMATTER) : "-",
                courrier.getPriorite().getLibelle(),
                courrier.getStatut().getLibelle(),
                courrier.isConfidentiel() ? "Oui" : "Non",
                document != null ? document.getCodeDocument() : "?",
                document != null ? document.getTitre() : "Document introuvable",
                courrier.getObservations() != null ? courrier.getObservations() : "Aucune observation"
            );
            
            alert.setContentText(details);
            alert.showAndWait();
            
            logService.logAction("consultation_courrier", "Courrier consulté: " + courrier.getCodeCourrier());
            
        } catch (Exception e) {
            System.err.println("Erreur affichage détails: " + e.getMessage());
            showError("Erreur lors de l'affichage des détails");
        }
    }
    
    /**
     * Change le statut d'un courrier
     */
    private void changerStatutCourrier(Courrier courrier) {
        if (courrier == null) return;
        
        try {
            // Créer un dialogue de sélection du statut
            ChoiceDialog<StatutCourrier> dialog = new ChoiceDialog<>(
                courrier.getStatut(), 
                StatutCourrier.values()
            );
            
            dialog.setTitle("Changer le statut");
            dialog.setHeaderText("Courrier: " + courrier.getCodeCourrier());
            dialog.setContentText("Nouveau statut:");
            
            Optional<StatutCourrier> result = dialog.showAndWait();
            
            if (result.isPresent()) {
                StatutCourrier nouveauStatut = result.get();
                
                if (nouveauStatut == courrier.getStatut()) {
                    showInfo("Le statut est déjà " + nouveauStatut.getLibelle());
                    return;
                }
                
                // Mettre à jour le statut
                boolean succes = courrierService.updateStatut(courrier.getId(), nouveauStatut);
                
                if (succes) {
                    showSuccess("✅ Statut changé vers: " + nouveauStatut.getLibelle());
                    
                    logService.logAction("changement_statut_courrier", 
                        "Courrier " + courrier.getCodeCourrier() + 
                        " - Statut: " + courrier.getStatut() + " → " + nouveauStatut);
                    
                    // Rafraîchir
                    chargerCourriers();
                    mettreAJourStatistiques();
                } else {
                    showError("Échec du changement de statut");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Erreur changement statut: " + e.getMessage());
            showError("Erreur lors du changement de statut");
        }
    }
    
    /**
     * Archive un courrier (et son document)
     */
    private void archiverCourrier(Courrier courrier) {
        if (courrier == null) return;
        
        // Vérifier que le courrier est au statut TRAITE
        if (courrier.getStatut() != StatutCourrier.TRAITE) {
            showWarning("Le courrier doit être au statut TRAITE pour être archivé");
            return;
        }
        
        try {
            // Sélectionner le dossier d'archivage
            DossierSelectorDialog selectorDialog = new DossierSelectorDialog("Sélectionner le dossier d'archivage");
            Optional<Integer> dossierResult = selectorDialog.showAndWait();
            
            if (dossierResult.isEmpty()) {
                return; // Annulé
            }
            
            int dossierId = dossierResult.get();
            
            // Confirmer l'archivage
            Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
            confirm.setTitle("Confirmation");
            confirm.setHeaderText("Archiver le courrier");
            confirm.setContentText("Cette action va archiver le courrier ET son document associé.\n\n" +
                                  "Courrier: " + courrier.getCodeCourrier() + "\n" +
                                  "Confirmer l'archivage ?");
            
            if (confirm.showAndWait().orElse(ButtonType.CANCEL) == ButtonType.OK) {
                User currentUser = getCurrentUser();
                
                boolean succes = courrierService.archiverCourrier(
                    courrier.getId(), 
                    dossierId, 
                    currentUser != null ? currentUser.getId() : 0
                );
                
                if (succes) {
                    showSuccess("✅ Courrier et document archivés avec succès !");
                    
                    logService.logAction("archivage_courrier", 
                        "Courrier archivé: " + courrier.getCodeCourrier() + 
                        " - Dossier ID: " + dossierId);
                    
                    // Rafraîchir
                    chargerCourriers();
                    mettreAJourStatistiques();
                } else {
                    showError("Échec de l'archivage");
                }
            }
            
        } catch (Exception e) {
            System.err.println("Erreur archivage: " + e.getMessage());
            e.printStackTrace();
            showError("Erreur lors de l'archivage:\n" + e.getMessage());
        }
    }
    
    /**
     * Recherche de courriers
     */
    @FXML
    private void handleRechercher() {
        String recherche = champRecherche != null ? champRecherche.getText().trim() : "";
        
        if (recherche.isEmpty()) {
            chargerCourriers();
            return;
        }
        
        try {
            List<Courrier> resultats = courrierService.rechercherCourriers(recherche);
            
            courriers.clear();
            courriers.addAll(resultats);
            
            appliquerFiltres();
            
            showInfo(resultats.size() + " courrier(s) trouvé(s)");
            
        } catch (Exception e) {
            System.err.println("Erreur recherche: " + e.getMessage());
            showError("Erreur lors de la recherche");
        }
    }
    
    @FXML
    private void handleActualiser() {
        chargerCourriers();
        chargerCourriersNotifies();
        mettreAJourStatistiques();
        afficherBadgeNotifications();
        showInfo("Liste actualisée");
    }
    
    // ==================== MÉTHODES UTILITAIRES ====================
    
    private User getCurrentUser() {
        return SessionManager.getInstance().getCurrentUser();
    }
    
    private void showSuccess(String message) {
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private void showWarning(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Attention");
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
    
    // ==================== CLASSE INTERNE ====================
    
    /**
     * Classe pour encapsuler un courrier avec ses infos de notification
     */
    public static class CourrierNotification {
        private Courrier courrier;
        private boolean lu;
        private java.time.LocalDateTime dateNotification;
        private java.time.LocalDateTime dateLecture;
        
        public CourrierNotification(Courrier courrier) {
            this.courrier = courrier;
            this.lu = false;
            this.dateNotification = java.time.LocalDateTime.now();
        }
        
        public Courrier getCourrier() { return courrier; }
        public void setCourrier(Courrier courrier) { this.courrier = courrier; }
        
        public boolean isLu() { return lu; }
        public void setLu(boolean lu) { this.lu = lu; }
        
        public java.time.LocalDateTime getDateNotification() { return dateNotification; }
        public void setDateNotification(java.time.LocalDateTime dateNotification) { 
            this.dateNotification = dateNotification; 
        }
        
        public java.time.LocalDateTime getDateLecture() { return dateLecture; }
        public void setDateLecture(java.time.LocalDateTime dateLecture) { 
            this.dateLecture = dateLecture; 
        }
    }
}