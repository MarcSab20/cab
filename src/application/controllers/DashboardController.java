package application.controllers;

import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.*;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.shape.*;
import javafx.geometry.Pos;
import javafx.geometry.Insets;
import application.models.*;
import application.services.*;
import application.utils.SessionManager;
import application.utils.AlertUtils;

import java.net.URL;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Contrôleur pour le Dashboard avec visualisation du workflow
 */
public class DashboardController implements Initializable {
    
    // Cartes de statistiques
    @FXML private Label statCourriersEnAttente;
    @FXML private Label statDocumentsActifs;
    @FXML private Label statReunionsMois;
    @FXML private Label statMessagesNonLus;
    @FXML private Label trendCourriers;
    @FXML private Label trendDocuments;
    
    // Graphiques
    @FXML private LineChart<String, Number> evolutionChart;
    @FXML private PieChart repartitionChart;
    
    // Zone de workflow
    @FXML private VBox workflowContainer;
    @FXML private ScrollPane workflowScrollPane;
    @FXML private Label labelServiceActuel;
    @FXML private Label labelCourriersEnCours;
    @FXML private Label labelCourriersEnRetard;
    
    // Activités récentes
    @FXML private VBox activitesRecentesContainer;
    
    // Indicateurs de performance
    @FXML private ProgressBar tauxTraitementBar;
    @FXML private Label tauxTraitementLabel;
    @FXML private ProgressBar delaiMoyenBar;
    @FXML private Label delaiMoyenLabel;
    
    // Services
    private User currentUser;
    private WorkflowService workflowService;
    private CourrierService courrierService;
    private DocumentService documentService;
    private MessageService messageService;
    
    // Données
    private ServiceHierarchy userService;
    private ObservableList<Courrier> courriersEnWorkflow;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("DashboardController.initialize() - Début");
        
        try {
            // Initialisation des services
            currentUser = SessionManager.getInstance().getCurrentUser();
            workflowService = WorkflowService.getInstance();
            courrierService = CourrierService.getInstance();
            documentService = DocumentService.getInstance();
            messageService = MessageService.getInstance();
            
            if (currentUser == null) {
                System.err.println("ERREUR: Aucun utilisateur en session");
                return;
            }
            
            // Charger le service de l'utilisateur
            if (currentUser.getServiceCode() != null) {
                userService = workflowService.getServiceByCode(currentUser.getServiceCode());
            }
            
            // Initialisation des composants
            initializeStatistics();
            initializeCharts();
            initializeWorkflowVisualization();
            initializeActivitesRecentes();
            
            // Actualisation automatique toutes les 30 secondes
            startAutoRefresh();
            
            System.out.println("DashboardController.initialize() - Terminé avec succès");
            
        } catch (Exception e) {
            System.err.println("Erreur dans DashboardController.initialize(): " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    // ========================================
    // STATISTIQUES
    // ========================================
    
    private void initializeStatistics() {
        try {
            // Courriers en attente
            List<Courrier> courriersService = getCourriersVisibles();
            long enAttente = courriersService.stream()
                .filter(c -> c.getStatut() == StatutCourrier.EN_ATTENTE)
                .count();
            
            if (statCourriersEnAttente != null) {
                statCourriersEnAttente.setText(String.valueOf(enAttente));
            }
            
            if (trendCourriers != null) {
                trendCourriers.setText("↗ +5 aujourd'hui");
                trendCourriers.setStyle("-fx-text-fill: #27ae60;");
            }
            
            // Documents actifs
            List<Document> documents = documentService.getAllDocuments();
            long documentsActifs = documents.stream()
                .filter(d -> d.getStatut().isActif())
                .count();
            
            if (statDocumentsActifs != null) {
                statDocumentsActifs.setText(String.valueOf(documentsActifs));
            }
            
            if (trendDocuments != null) {
                trendDocuments.setText("↗ +12 cette semaine");
                trendDocuments.setStyle("-fx-text-fill: #27ae60;");
            }
            
            // Messages non lus
            List<Message> messages = messageService.getMessagesForUser(currentUser.getId());
            long nonLus = messages.stream()
                .filter(m -> !m.isLu())
                .count();
            
            if (statMessagesNonLus != null) {
                statMessagesNonLus.setText(String.valueOf(nonLus));
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des statistiques: " + e.getMessage());
        }
    }
    
    // ========================================
    // GRAPHIQUES
    // ========================================
    
    private void initializeCharts() {
        initializeEvolutionChart();
        initializeRepartitionChart();
    }
    
    private void initializeEvolutionChart() {
        if (evolutionChart == null) return;
        
        try {
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Courriers traités");
            
            // Données des 7 derniers jours
            String[] jours = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
            int[] valeurs = {12, 15, 10, 18, 14, 8, 11};
            
            for (int i = 0; i < jours.length; i++) {
                series.getData().add(new XYChart.Data<>(jours[i], valeurs[i]));
            }
            
            evolutionChart.getData().add(series);
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation du graphique d'évolution: " + e.getMessage());
        }
    }
    
    private void initializeRepartitionChart() {
        if (repartitionChart == null) return;
        
        try {
            List<Courrier> courriers = getCourriersVisibles();
            
            Map<String, Long> repartition = courriers.stream()
                .collect(Collectors.groupingBy(
                    c -> c.getStatut().getLibelle(),
                    Collectors.counting()
                ));
            
            ObservableList<PieChart.Data> pieChartData = FXCollections.observableArrayList();
            
            repartition.forEach((statut, count) -> {
                pieChartData.add(new PieChart.Data(statut + " (" + count + ")", count));
            });
            
            repartitionChart.setData(pieChartData);
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation du graphique de répartition: " + e.getMessage());
        }
    }
    
    // ========================================
    // VISUALISATION DU WORKFLOW
    // ========================================
    
    private void initializeWorkflowVisualization() {
        if (workflowContainer == null) return;
        
        try {
            workflowContainer.getChildren().clear();
            
            // Informations sur le service actuel
            if (userService != null && labelServiceActuel != null) {
                labelServiceActuel.setText(userService.getServiceName());
            }
            
            // Récupérer les courriers visibles
            List<Courrier> courriers = getCourriersVisibles();
            courriersEnWorkflow = FXCollections.observableArrayList(
                courriers.stream()
                    .filter(Courrier::estEnWorkflow)
                    .collect(Collectors.toList())
            );
            
            // Statistiques rapides
            long enCours = courriersEnWorkflow.stream()
                .filter(c -> c.getStatut() == StatutCourrier.EN_COURS)
                .count();
            
            if (labelCourriersEnCours != null) {
                labelCourriersEnCours.setText(String.valueOf(enCours));
            }
            
            // Afficher les courriers dans le workflow
            afficherCourriersWorkflow();
            
        } catch (Exception e) {
            System.err.println("Erreur lors de l'initialisation de la visualisation workflow: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void afficherCourriersWorkflow() {
        if (workflowContainer == null) return;
        
        workflowContainer.getChildren().clear();
        
        if (courriersEnWorkflow.isEmpty()) {
            Label emptyLabel = new Label("Aucun courrier en workflow actuellement");
            emptyLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 14px;");
            VBox emptyBox = new VBox(emptyLabel);
            emptyBox.setAlignment(Pos.CENTER);
            emptyBox.setPadding(new Insets(30));
            workflowContainer.getChildren().add(emptyBox);
            return;
        }
        
        // Afficher chaque courrier avec sa chaîne de workflow
        for (Courrier courrier : courriersEnWorkflow) {
            VBox courrierCard = createCourrierWorkflowCard(courrier);
            workflowContainer.getChildren().add(courrierCard);
        }
    }
    
    private VBox createCourrierWorkflowCard(Courrier courrier) {
        VBox card = new VBox(10);
        card.getStyleClass().add("card");
        card.setPadding(new Insets(15));
        card.setStyle("-fx-background-color: white; -fx-background-radius: 8; " +
                     "-fx-effect: dropshadow(gaussian, rgba(0,0,0,0.1), 5, 0, 0, 2);");
        
        // En-tête du courrier
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label numeroLabel = new Label(courrier.getNumeroCourrier());
        numeroLabel.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        
        Label objetLabel = new Label(courrier.getObjet());
        objetLabel.setStyle("-fx-text-fill: #2c3e50;");
        HBox.setHgrow(objetLabel, Priority.ALWAYS);
        
        Label prioriteLabel = new Label(courrier.getPriorite());
        prioriteLabel.setStyle("-fx-font-size: 16px;");
        
        header.getChildren().addAll(numeroLabel, objetLabel, prioriteLabel);
        
        // Récupérer l'historique du workflow
        List<WorkflowStep> steps = workflowService.getWorkflowHistory(courrier.getId());
        
        // Créer la visualisation de la chaîne
        HBox workflowChain = createWorkflowChain(steps, courrier);
        
        // Informations supplémentaires
        HBox info = new HBox(20);
        info.setAlignment(Pos.CENTER_LEFT);
        
        Label etapeLabel = new Label("Étape: " + courrier.getEtapeActuelle() + "/" + steps.size());
        etapeLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        
        Label serviceLabel = new Label("Service actuel: " + courrier.getServiceActuel());
        serviceLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        
        info.getChildren().addAll(etapeLabel, serviceLabel);
        
        // Boutons d'action (si c'est le service de l'utilisateur)
        if (userService != null && courrier.getServiceActuel() != null &&
            courrier.getServiceActuel().equals(userService.getServiceCode())) {
            
            HBox actions = new HBox(10);
            actions.setAlignment(Pos.CENTER_RIGHT);
            
            Button traiterBtn = new Button("Traiter");
            traiterBtn.getStyleClass().add("button-primary");
            traiterBtn.setOnAction(e -> handleTraiterCourrier(courrier));
            
            Button transfererBtn = new Button("Transférer");
            transfererBtn.getStyleClass().add("button-info");
            transfererBtn.setOnAction(e -> handleTransfererCourrier(courrier));
            
            Button voirBtn = new Button("Voir détails");
            voirBtn.getStyleClass().add("button-secondary");
            voirBtn.setOnAction(e -> handleVoirDetailsCourrier(courrier));
            
            actions.getChildren().addAll(voirBtn, traiterBtn, transfererBtn);
            
            card.getChildren().addAll(header, workflowChain, info, new Separator(), actions);
        } else {
            card.getChildren().addAll(header, workflowChain, info);
        }
        
        return card;
    }
    
    private HBox createWorkflowChain(List<WorkflowStep> steps, Courrier courrier) {
        HBox chain = new HBox(5);
        chain.setAlignment(Pos.CENTER_LEFT);
        chain.setPadding(new Insets(10, 0, 10, 0));
        
        for (int i = 0; i < steps.size(); i++) {
            WorkflowStep step = steps.get(i);
            
            // Créer le nœud de l'étape
            VBox stepNode = createStepNode(step, i == courrier.getEtapeActuelle() - 1);
            chain.getChildren().add(stepNode);
            
            // Ajouter une flèche si ce n'est pas la dernière étape
            if (i < steps.size() - 1) {
                Label arrow = new Label("→");
                arrow.setStyle("-fx-font-size: 20px; -fx-text-fill: #bdc3c7;");
                chain.getChildren().add(arrow);
            }
        }
        
        // Si le workflow n'est pas terminé, ajouter un indicateur
        if (!courrier.isWorkflowTermine() && courrier.getEtapeActuelle() < steps.size()) {
            Label pending = new Label("...");
            pending.setStyle("-fx-font-size: 20px; -fx-text-fill: #95a5a6;");
            chain.getChildren().add(pending);
        }
        
        ScrollPane scrollPane = new ScrollPane(chain);
        scrollPane.setFitToHeight(true);
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED);
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.NEVER);
        scrollPane.setStyle("-fx-background-color: transparent;");
        
        HBox wrapper = new HBox(scrollPane);
        HBox.setHgrow(scrollPane, Priority.ALWAYS);
        
        return wrapper;
    }
    
    private VBox createStepNode(WorkflowStep step, boolean isCurrent) {
        VBox node = new VBox(3);
        node.setAlignment(Pos.CENTER);
        node.setPrefWidth(120);
        node.setMaxWidth(120);
        
        // Icône de statut
        Circle circle = new Circle(12);
        
        switch (step.getStatutEtape().name()) {
            case "TERMINE":
                circle.setFill(Color.web("#27ae60"));
                break;
            case "EN_COURS":
                circle.setFill(Color.web("#3498db"));
                break;
            case "EN_ATTENTE":
                circle.setFill(Color.web("#f39c12"));
                break;
            case "TRANSFERE":
                circle.setFill(Color.web("#9b59b6"));
                break;
            case "REJETE":
                circle.setFill(Color.web("#e74c3c"));
                break;
            default:
                circle.setFill(Color.web("#95a5a6"));
        }
        
        if (isCurrent) {
            circle.setStroke(Color.web("#2c3e50"));
            circle.setStrokeWidth(3);
        }
        
        // Nom du service (abrégé)
        Label serviceLabel = new Label(step.getServiceCode());
        serviceLabel.setStyle("-fx-font-size: 10px; -fx-font-weight: bold; " +
                             "-fx-text-fill: #2c3e50;");
        serviceLabel.setWrapText(true);
        serviceLabel.setMaxWidth(115);
        serviceLabel.setAlignment(Pos.CENTER);
        
        // Date/statut
        Label statusLabel = new Label(step.getStatutEtape().getLibelle());
        statusLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #7f8c8d;");
        statusLabel.setMaxWidth(115);
        statusLabel.setAlignment(Pos.CENTER);
        
        // Temps écoulé ou temps restant
        Label timeLabel = new Label(step.getTempsEcoule());
        timeLabel.setStyle("-fx-font-size: 9px; -fx-text-fill: #95a5a6;");
        timeLabel.setMaxWidth(115);
        timeLabel.setAlignment(Pos.CENTER);
        
        node.getChildren().addAll(circle, serviceLabel, statusLabel, timeLabel);
        
        // Style pour l'étape actuelle
        if (isCurrent) {
            node.setStyle("-fx-background-color: #e3f2fd; -fx-padding: 8; -fx-background-radius: 5;");
        }
        
        // Tooltip avec plus d'informations
        Tooltip tooltip = new Tooltip(
            "Service: " + step.getServiceName() + "\n" +
            "Statut: " + step.getStatutEtape().getLibelle() + "\n" +
            "Date: " + step.getDateActionFormatee() + "\n" +
            (step.getUserName() != null ? "Traité par: " + step.getUserName() + "\n" : "") +
            (step.getCommentaire() != null ? "Commentaire: " + step.getCommentaire() : "")
        );
        Tooltip.install(node, tooltip);
        
        return node;
    }
    
    // ========================================
    // ACTIVITÉS RÉCENTES
    // ========================================
    
    private void initializeActivitesRecentes() {
        if (activitesRecentesContainer == null) return;
        
        activitesRecentesContainer.getChildren().clear();
        
        // Récupérer les activités récentes basées sur les courriers
        List<Courrier> courriersRecents = getCourriersVisibles().stream()
            .sorted(Comparator.comparing(Courrier::getDateReception).reversed())
            .limit(5)
            .collect(Collectors.toList());
        
        for (Courrier courrier : courriersRecents) {
            HBox activiteBox = createActiviteBox(courrier);
            activitesRecentesContainer.getChildren().add(activiteBox);
        }
    }
    
    private HBox createActiviteBox(Courrier courrier) {
        HBox box = new HBox(15);
        box.setAlignment(Pos.CENTER_LEFT);
        box.setPadding(new Insets(10));
        box.setStyle("-fx-background-color: #f8f9fa; -fx-background-radius: 5;");
        
        Label icon = new Label("📧");
        icon.setStyle("-fx-font-size: 16px;");
        
        VBox content = new VBox(3);
        HBox.setHgrow(content, Priority.ALWAYS);
        
        Label title = new Label("Courrier: " + courrier.getObjet());
        title.setStyle("-fx-font-weight: bold;");
        
        Label details = new Label("De: " + courrier.getExpediteur() + " • " + 
                                 courrier.getStatut().getLibelle());
        details.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px;");
        
        content.getChildren().addAll(title, details);
        
        Label time = new Label(getTempsEcouleDepuis(courrier.getDateReception()));
        time.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 11px;");
        
        box.getChildren().addAll(icon, content, time);
        
        return box;
    }
    
    // ========================================
    // ACTIONS DES COURRIERS
    // ========================================
    
    private void handleTraiterCourrier(Courrier courrier) {
        boolean confirm = AlertUtils.showConfirmation(
            "Terminer le traitement",
            "Voulez-vous marquer ce courrier comme traité et terminé ?\n\n" +
            "Courrier: " + courrier.getNumeroCourrier() + "\n" +
            "Objet: " + courrier.getObjet()
        );
        
        if (confirm) {
            Optional<String> commentaire = AlertUtils.showTextInput(
                "Commentaire de clôture",
                "Veuillez ajouter un commentaire de clôture:", ""
            );
            
            if (commentaire.isPresent()) {
                boolean success = workflowService.terminerWorkflow(
                    courrier, 
                    currentUser, 
                    commentaire.get()
                );
                
                if (success) {
                    AlertUtils.showInfo("Courrier traité avec succès et archivé");
                    refreshDashboard();
                } else {
                    AlertUtils.showError("Erreur lors du traitement du courrier");
                }
            }
        }
    }
    
    private void handleTransfererCourrier(Courrier courrier) {
        // Récupérer les services vers lesquels on peut transférer
        List<ServiceHierarchy> servicesDisponibles = 
            workflowService.getTransferableServices(currentUser);
        
        if (servicesDisponibles.isEmpty()) {
            AlertUtils.showWarning("Aucun service disponible pour le transfert");
            return;
        }
        
        // Créer une boîte de dialogue pour sélectionner le service
        Dialog<ServiceHierarchy> dialog = new Dialog<>();
        dialog.setTitle("Transférer le courrier");
        dialog.setHeaderText("Sélectionnez le service de destination");
        
        // ComboBox pour les services
        ComboBox<ServiceHierarchy> serviceCombo = new ComboBox<>();
        serviceCombo.setItems(FXCollections.observableArrayList(servicesDisponibles));
        serviceCombo.setPromptText("Choisir un service...");
        serviceCombo.setPrefWidth(300);
        
        // Affichage personnalisé
        serviceCombo.setCellFactory(param -> new ListCell<ServiceHierarchy>() {
            @Override
            protected void updateItem(ServiceHierarchy service, boolean empty) {
                super.updateItem(service, empty);
                if (empty || service == null) {
                    setText(null);
                } else {
                    setText(service.getIcone() + " " + service.getServiceName() + 
                           " (" + service.getServiceCode() + ")");
                }
            }
        });
        serviceCombo.setButtonCell(serviceCombo.getCellFactory().call(null));
        
        // Champ commentaire
        TextArea commentaireArea = new TextArea();
        commentaireArea.setPromptText("Commentaire (optionnel)");
        commentaireArea.setPrefRowCount(3);
        
        // Champ délai
        TextField delaiField = new TextField();
        delaiField.setPromptText("Délai en heures (optionnel)");
        
        VBox content = new VBox(10);
        content.getChildren().addAll(
            new Label("Service destination:"), serviceCombo,
            new Label("Commentaire:"), commentaireArea,
            new Label("Délai de traitement:"), delaiField
        );
        
        dialog.getDialogPane().setContent(content);
        dialog.getDialogPane().getButtonTypes().addAll(ButtonType.OK, ButtonType.CANCEL);
        
        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == ButtonType.OK) {
                return serviceCombo.getValue();
            }
            return null;
        });
        
        Optional<ServiceHierarchy> result = dialog.showAndWait();
        
        result.ifPresent(service -> {
            String commentaire = commentaireArea.getText();
            Integer delai = null;
            
            try {
                if (!delaiField.getText().isEmpty()) {
                    delai = Integer.parseInt(delaiField.getText());
                }
            } catch (NumberFormatException e) {
                // Ignorer si le délai n'est pas valide
            }
            
            boolean success = workflowService.transferCourrier(
                courrier,
                currentUser,
                service.getServiceCode(),
                commentaire,
                delai
            );
            
            if (success) {
                AlertUtils.showInfo("Courrier transféré avec succès vers " + service.getServiceName());
                refreshDashboard();
            } else {
                AlertUtils.showError("Erreur lors du transfert du courrier");
            }
        });
    }
    
    private void handleVoirDetailsCourrier(Courrier courrier) {
        // Créer une fenêtre de dialogue pour afficher les détails
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Détails du courrier");
        alert.setHeaderText(courrier.getNumeroCourrier() + " - " + courrier.getObjet());
        
        VBox content = new VBox(10);
        content.setPadding(new Insets(10));
        
        // Informations du courrier
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8);
        
        grid.add(new Label("Type:"), 0, 0);
        grid.add(new Label(courrier.getTypeCourrier().getLibelle()), 1, 0);
        
        grid.add(new Label("Expéditeur:"), 0, 1);
        grid.add(new Label(courrier.getExpediteur()), 1, 1);
        
        grid.add(new Label("Date:"), 0, 2);
        grid.add(new Label(courrier.getDateReceptionFormatee()), 1, 2);
        
        grid.add(new Label("Priorité:"), 0, 3);
        grid.add(new Label(courrier.getPriorite()), 1, 3);
        
        grid.add(new Label("Statut:"), 0, 4);
        grid.add(new Label(courrier.getStatut().getLibelle()), 1, 4);
        
        if (courrier.getNotes() != null && !courrier.getNotes().isEmpty()) {
            Label notesLabel = new Label("Notes:");
            TextArea notesArea = new TextArea(courrier.getNotes());
            notesArea.setEditable(false);
            notesArea.setPrefRowCount(4);
            
            grid.add(notesLabel, 0, 5);
            grid.add(notesArea, 1, 5);
        }
        
        content.getChildren().add(grid);
        
        // Historique du workflow
        List<WorkflowStep> steps = workflowService.getWorkflowHistory(courrier.getId());
        if (!steps.isEmpty()) {
            content.getChildren().add(new Separator());
            Label histoLabel = new Label("Historique du workflow:");
            histoLabel.setStyle("-fx-font-weight: bold;");
            content.getChildren().add(histoLabel);
            
            VBox histoBox = new VBox(5);
            for (WorkflowStep step : steps) {
                Label stepLabel = new Label(
                    step.getIconeStatut() + " " + step.getServiceName() + 
                    " - " + step.getStatutEtape().getLibelle() + 
                    " - " + step.getDateActionFormatee()
                );
                stepLabel.setStyle("-fx-font-size: 11px;");
                histoBox.getChildren().add(stepLabel);
                
                if (step.getCommentaire() != null && !step.getCommentaire().isEmpty()) {
                    Label commentLabel = new Label("  → " + step.getCommentaire());
                    commentLabel.setStyle("-fx-font-size: 10px; -fx-text-fill: #7f8c8d;");
                    histoBox.getChildren().add(commentLabel);
                }
            }
            
            ScrollPane histoScroll = new ScrollPane(histoBox);
            histoScroll.setFitToWidth(true);
            histoScroll.setPrefHeight(150);
            content.getChildren().add(histoScroll);
        }
        
        alert.getDialogPane().setContent(content);
        alert.showAndWait();
    }
    
    // ========================================
    // MÉTHODES UTILITAIRES
    // ========================================
    
    private List<Courrier> getCourriersVisibles() {
        try {
            return workflowService.getCourriersVisiblesPourUtilisateur(currentUser);
        } catch (Exception e) {
            System.err.println("Erreur lors de la récupération des courriers: " + e.getMessage());
            return new ArrayList<>();
        }
    }
    
    private String getTempsEcouleDepuis(LocalDateTime date) {
        if (date == null) return "";
        
        LocalDateTime now = LocalDateTime.now();
        java.time.Duration duration = java.time.Duration.between(date, now);
        
        long minutes = duration.toMinutes();
        if (minutes < 60) {
            return "Il y a " + minutes + " min";
        }
        
        long heures = duration.toHours();
        if (heures < 24) {
            return "Il y a " + heures + "h";
        }
        
        long jours = duration.toDays();
        return "Il y a " + jours + "j";
    }
    
    @FXML
    private void handleActualiser() {
        refreshDashboard();
    }
    
    private void refreshDashboard() {
        initializeStatistics();
        initializeCharts();
        initializeWorkflowVisualization();
        initializeActivitesRecentes();
    }
    
    private void startAutoRefresh() {
        Thread refreshThread = new Thread(() -> {
            while (true) {
                try {
                    Thread.sleep(30000); // 30 secondes
                    Platform.runLater(this::refreshDashboard);
                } catch (InterruptedException e) {
                    break;
                }
            }
        });
        refreshThread.setDaemon(true);
        refreshThread.start();
    }
}