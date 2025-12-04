package application.controllers;

import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import application.models.User;
import application.models.Courrier;
import application.models.ServiceHierarchy;
import application.services.WorkflowService;
import application.services.AdminService;
import application.utils.SessionManager;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

/**
 * Dialogue pour coter un courrier à un utilisateur
 */
public class CoterCourrierDialog extends Dialog<CotationInfo> {
    
    private final ComboBox<User> comboUtilisateur;
    private final TextArea textAreaCommentaire;
    private final DatePicker dateEcheance;
    private final Spinner<Integer> spinnerDelaiJours;
    private final CheckBox checkNotifierUtilisateur;
    
    private final Courrier courrier;
    private final User currentUser;
    
    /**
     * Constructeur
     * @param courrier Le courrier à coter
     */
    public CoterCourrierDialog(Courrier courrier) {
        this.courrier = courrier;
        this.currentUser = SessionManager.getInstance().getCurrentUser();
        
        // Configuration du dialogue
        setTitle("Coter un courrier");
        setHeaderText("Coter le courrier N° " + courrier.getNumeroCourrier() + 
                     "\nà un utilisateur pour traitement");
        initModality(Modality.APPLICATION_MODAL);
        
        // Boutons
        ButtonType btnCoter = new ButtonType("Coter", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnAnnuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(btnCoter, btnAnnuler);
        
        // Création du formulaire
        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(12);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // Champs du formulaire
        comboUtilisateur = new ComboBox<>();
        loadUtilisateurs();
        comboUtilisateur.setPromptText("Sélectionner un utilisateur");
        comboUtilisateur.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String serviceInfo = item.getServiceCode() != null ? 
                        " [" + item.getServiceCode() + "]" : "";
                    setText(item.getNomComplet() + " - " + item.getRole().getNom() + serviceInfo);
                }
            }
        });
        comboUtilisateur.setButtonCell(comboUtilisateur.getCellFactory().call(null));
        
        textAreaCommentaire = new TextArea();
        textAreaCommentaire.setPromptText("Instructions ou commentaires pour l'utilisateur...");
        textAreaCommentaire.setPrefRowCount(4);
        textAreaCommentaire.setWrapText(true);
        
        dateEcheance = new DatePicker();
        dateEcheance.setValue(java.time.LocalDate.now().plusDays(3));
        dateEcheance.setPromptText("Date d'échéance");
        
        spinnerDelaiJours = new Spinner<>(1, 30, 3);
        spinnerDelaiJours.setEditable(true);
        
        checkNotifierUtilisateur = new CheckBox("Notifier l'utilisateur par email");
        checkNotifierUtilisateur.setSelected(true);
        
        // Ajout des champs au grid
        int row = 0;
        
        Label labelInfo = new Label("⚠️ La cotation permet d'assigner le courrier à un utilisateur spécifique\n" +
                                   "pour traitement. Le workflow sera mis à jour automatiquement.");
        labelInfo.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 12px; -fx-wrap-text: true;");
        labelInfo.setWrapText(true);
        grid.add(labelInfo, 0, row++, 2, 1);
        
        grid.add(new Separator(), 0, row++, 2, 1);
        
        grid.add(new Label("Utilisateur:*"), 0, row);
        grid.add(comboUtilisateur, 1, row++);
        
        grid.add(new Label("Commentaire:"), 0, row);
        grid.add(textAreaCommentaire, 1, row++);
        
        grid.add(new Label("Échéance:"), 0, row);
        grid.add(dateEcheance, 1, row++);
        
        grid.add(new Label("Délai (jours):"), 0, row);
        grid.add(spinnerDelaiJours, 1, row++);
        
        grid.add(new Label(""), 0, row);
        grid.add(checkNotifierUtilisateur, 1, row++);
        
        grid.add(new Label(""), 0, row);
        Label noteLabel = new Label("* Champs obligatoires");
        noteLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 11px;");
        grid.add(noteLabel, 1, row++);
        
        getDialogPane().setContent(grid);
        
        // Synchronisation du délai avec la date d'échéance
        spinnerDelaiJours.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                dateEcheance.setValue(java.time.LocalDate.now().plusDays(newVal));
            }
        });
        
        dateEcheance.valueProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal != null) {
                long jours = java.time.temporal.ChronoUnit.DAYS.between(
                    java.time.LocalDate.now(), newVal
                );
                if (jours >= 1 && jours <= 30) {
                    spinnerDelaiJours.getValueFactory().setValue((int) jours);
                }
            }
        });
        
        // Validation
        Button btnValidate = (Button) getDialogPane().lookupButton(btnCoter);
        btnValidate.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!validateForm()) {
                event.consume();
            }
        });
        
        // Conversion du résultat
        setResultConverter(dialogButton -> {
            if (dialogButton == btnCoter) {
                return createCotationInfo();
            }
            return null;
        });
    }
    
    /**
     * Charge la liste des utilisateurs disponibles
     */
    private void loadUtilisateurs() {
        try {
            AdminService adminService = AdminService.getInstance();
            List<User> allUsers = adminService.getAllUsers();
            
            // Filtrer les utilisateurs actifs (exclure l'utilisateur courant)
            List<User> availableUsers = allUsers.stream()
                .filter(u -> u.isActif() && u.getId() != currentUser.getId())
                .collect(Collectors.toList());
            
            // Si le courrier a un service actuel, prioriser les utilisateurs de ce service
            if (courrier.getServiceActuel() != null && !courrier.getServiceActuel().isEmpty()) {
                availableUsers.sort((u1, u2) -> {
                    boolean u1Match = courrier.getServiceActuel().equals(u1.getServiceCode());
                    boolean u2Match = courrier.getServiceActuel().equals(u2.getServiceCode());
                    
                    if (u1Match && !u2Match) return -1;
                    if (!u1Match && u2Match) return 1;
                    return u1.getNomComplet().compareTo(u2.getNomComplet());
                });
            }
            
            ObservableList<User> usersList = FXCollections.observableArrayList(availableUsers);
            comboUtilisateur.setItems(usersList);
            
        } catch (Exception e) {
            System.err.println("Erreur lors du chargement des utilisateurs: " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    /**
     * Valide le formulaire
     */
    private boolean validateForm() {
        // Validation de l'utilisateur
        if (comboUtilisateur.getValue() == null) {
            showError("Veuillez sélectionner un utilisateur");
            return false;
        }
        
        // Validation de la date d'échéance
        if (dateEcheance.getValue() == null) {
            showError("Veuillez spécifier une date d'échéance");
            return false;
        }
        
        if (dateEcheance.getValue().isBefore(java.time.LocalDate.now())) {
            showError("La date d'échéance ne peut pas être dans le passé");
            return false;
        }
        
        return true;
    }
    
    /**
     * Affiche un message d'erreur
     */
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setTitle("Erreur de validation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    /**
     * Crée l'objet CotationInfo à partir des valeurs du formulaire
     */
    private CotationInfo createCotationInfo() {
        CotationInfo info = new CotationInfo();
        info.setUtilisateur(comboUtilisateur.getValue());
        info.setCommentaire(textAreaCommentaire.getText().trim());
        info.setDateEcheance(dateEcheance.getValue().atStartOfDay());
        info.setDelaiJours(spinnerDelaiJours.getValue());
        info.setNotifierUtilisateur(checkNotifierUtilisateur.isSelected());
        return info;
    }
    
}