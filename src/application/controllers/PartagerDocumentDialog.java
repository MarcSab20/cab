package application.controllers;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.stage.Modality;
import application.models.Document;
import application.models.User;
import application.models.PartageInfo;
import application.services.UserService;

import java.util.ArrayList;
import java.util.List;

/**
 * Dialogue pour partager un document avec d'autres utilisateurs
 */
public class PartagerDocumentDialog extends Dialog<PartageInfo> {
    
    private final ListView<User> listeUtilisateurs;
    private final ListView<User> listeUtilisateursSelectionnes;
    private final ComboBox<String> comboNiveauAcces;
    private final DatePicker dateExpiration;
    private final CheckBox chkNotifierEmail;
    private final TextArea textAreaMessage;
    
    private final Document document;
    private final UserService userService;
    
    /**
     * Constructeur
     */
    public PartagerDocumentDialog(Document document) {
        this.document = document;
        this.userService = UserService.getInstance();
        
        // Configuration du dialogue
        setTitle("Partager le document");
        setHeaderText("📤 Partager: " + document.getTitre());
        initModality(Modality.APPLICATION_MODAL);
        setWidth(700);
        setHeight(600);
        
        // Boutons
        ButtonType btnPartager = new ButtonType("Partager", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnAnnuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(btnPartager, btnAnnuler);
        
        // Création du formulaire
        VBox container = new VBox(15);
        container.setPadding(new Insets(20));
        
        // Information du document
        HBox infoBox = new HBox(15);
        infoBox.setStyle("-fx-background-color: #ecf0f1; -fx-padding: 10; -fx-background-radius: 5;");
        
        VBox docInfo = new VBox(5);
        Label lblDoc = new Label("📄 " + document.getTitre());
        lblDoc.setStyle("-fx-font-weight: bold; -fx-font-size: 14px;");
        Label lblCode = new Label("Code: " + document.getCodeDocument());
        lblCode.setStyle("-fx-text-fill: #7f8c8d;");
        Label lblType = new Label("Type: " + document.getTypeDocument());
        lblType.setStyle("-fx-text-fill: #7f8c8d;");
        docInfo.getChildren().addAll(lblDoc, lblCode, lblType);
        
        infoBox.getChildren().add(docInfo);
        container.getChildren().add(infoBox);
        
        Separator sep1 = new Separator();
        container.getChildren().add(sep1);
        
        // Sélection des utilisateurs
        Label lblUtilisateurs = new Label("👥 Sélectionner les utilisateurs:");
        lblUtilisateurs.setStyle("-fx-font-weight: bold; -fx-font-size: 13px;");
        container.getChildren().add(lblUtilisateurs);
        
        HBox selectionBox = new HBox(10);
        
        // Liste des utilisateurs disponibles
        VBox vboxDisponibles = new VBox(5);
        Label lblDisponibles = new Label("Disponibles:");
        lblDisponibles.setStyle("-fx-font-weight: bold;");
        
        listeUtilisateurs = new ListView<>();
        listeUtilisateurs.setPrefHeight(200);
        listeUtilisateurs.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        chargerUtilisateurs();
        
        listeUtilisateurs.setCellFactory(param -> new ListCell<User>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getCode() + " - " + item.getNomComplet() + 
                           " (" + item.getRole().getNom() + ")");
                }
            }
        });
        
        vboxDisponibles.getChildren().addAll(lblDisponibles, listeUtilisateurs);
        
        // Boutons d'ajout/retrait
        VBox vboxBoutons = new VBox(10);
        vboxBoutons.setAlignment(javafx.geometry.Pos.CENTER);
        
        Button btnAjouter = new Button("➡️ Ajouter");
        btnAjouter.setOnAction(e -> ajouterUtilisateurs());
        
        Button btnRetirer = new Button("⬅️ Retirer");
        btnRetirer.setOnAction(e -> retirerUtilisateurs());
        
        Button btnTous = new Button("⏩ Tous");
        btnTous.setOnAction(e -> ajouterTousUtilisateurs());
        
        Button btnAucun = new Button("⏪ Aucun");
        btnAucun.setOnAction(e -> retirerTousUtilisateurs());
        
        vboxBoutons.getChildren().addAll(btnAjouter, btnRetirer, btnTous, btnAucun);
        
        // Liste des utilisateurs sélectionnés
        VBox vboxSelectionnes = new VBox(5);
        Label lblSelectionnes = new Label("Sélectionnés:");
        lblSelectionnes.setStyle("-fx-font-weight: bold;");
        
        listeUtilisateursSelectionnes = new ListView<>();
        listeUtilisateursSelectionnes.setPrefHeight(200);
        listeUtilisateursSelectionnes.getSelectionModel().setSelectionMode(SelectionMode.MULTIPLE);
        
        listeUtilisateursSelectionnes.setCellFactory(param -> new ListCell<User>() {
            @Override
            protected void updateItem(User item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    setText(item.getCode() + " - " + item.getNomComplet());
                }
            }
        });
        
        vboxSelectionnes.getChildren().addAll(lblSelectionnes, listeUtilisateursSelectionnes);
        
        selectionBox.getChildren().addAll(vboxDisponibles, vboxBoutons, vboxSelectionnes);
        HBox.setHgrow(vboxDisponibles, Priority.ALWAYS);
        HBox.setHgrow(vboxSelectionnes, Priority.ALWAYS);
        
        container.getChildren().add(selectionBox);
        
        Separator sep2 = new Separator();
        container.getChildren().add(sep2);
        
        // Options de partage
        GridPane gridOptions = new GridPane();
        gridOptions.setHgap(15);
        gridOptions.setVgap(10);
        
        int row = 0;
        
        // Niveau d'accès
        Label lblAcces = new Label("🔐 Niveau d'accès:");
        lblAcces.setStyle("-fx-font-weight: bold;");
        gridOptions.add(lblAcces, 0, row);
        
        comboNiveauAcces = new ComboBox<>();
        comboNiveauAcces.setItems(FXCollections.observableArrayList(
            "Lecture seule",
            "Lecture et téléchargement",
            "Lecture et commentaires",
            "Édition complète"
        ));
        comboNiveauAcces.setValue("Lecture seule");
        gridOptions.add(comboNiveauAcces, 1, row++);
        
        // Date d'expiration
        Label lblExpiration = new Label("📅 Expiration:");
        gridOptions.add(lblExpiration, 0, row);
        
        HBox hboxExpiration = new HBox(10);
        dateExpiration = new DatePicker();
        dateExpiration.setPromptText("Aucune expiration");
        
        Button btnSemaine = new Button("+7j");
        btnSemaine.setOnAction(e -> dateExpiration.setValue(
            java.time.LocalDate.now().plusDays(7)));
        
        Button btnMois = new Button("+30j");
        btnMois.setOnAction(e -> dateExpiration.setValue(
            java.time.LocalDate.now().plusDays(30)));
        
        hboxExpiration.getChildren().addAll(dateExpiration, btnSemaine, btnMois);
        gridOptions.add(hboxExpiration, 1, row++);
        
        // Notification email
        chkNotifierEmail = new CheckBox("📧 Envoyer une notification par email");
        chkNotifierEmail.setSelected(true);
        gridOptions.add(chkNotifierEmail, 0, row++, 2, 1);
        
        container.getChildren().add(gridOptions);
        
        // Message personnalisé
        Label lblMessage = new Label("💬 Message (optionnel):");
        lblMessage.setStyle("-fx-font-weight: bold;");
        container.getChildren().add(lblMessage);
        
        textAreaMessage = new TextArea();
        textAreaMessage.setPromptText("Ajoutez un message pour les destinataires...");
        textAreaMessage.setPrefRowCount(3);
        textAreaMessage.setWrapText(true);
        container.getChildren().add(textAreaMessage);
        
        getDialogPane().setContent(container);
        
        // Validation
        Button btnValidate = (Button) getDialogPane().lookupButton(btnPartager);
        btnValidate.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!validateForm()) {
                event.consume();
            }
        });
        
        // Conversion du résultat
        setResultConverter(dialogButton -> {
            if (dialogButton == btnPartager) {
                return createPartageInfo();
            }
            return null;
        });
    }
    
    /**
     * Charge la liste des utilisateurs
     */
    private void chargerUtilisateurs() {
        try {
            // Récupérer tous les utilisateurs actifs sauf l'utilisateur actuel
            // TODO: Implémenter dans UserService
            List<User> utilisateurs = new ArrayList<>(); // userService.getAllActiveUsers();
            
            listeUtilisateurs.setItems(FXCollections.observableArrayList(utilisateurs));
            
        } catch (Exception e) {
            System.err.println("Erreur chargement utilisateurs: " + e.getMessage());
        }
    }
    
    /**
     * Ajoute les utilisateurs sélectionnés
     */
    private void ajouterUtilisateurs() {
        List<User> selection = new ArrayList<>(listeUtilisateurs.getSelectionModel().getSelectedItems());
        
        for (User user : selection) {
            if (!listeUtilisateursSelectionnes.getItems().contains(user)) {
                listeUtilisateursSelectionnes.getItems().add(user);
            }
            listeUtilisateurs.getItems().remove(user);
        }
    }
    
    /**
     * Retire les utilisateurs sélectionnés
     */
    private void retirerUtilisateurs() {
        List<User> selection = new ArrayList<>(listeUtilisateursSelectionnes.getSelectionModel().getSelectedItems());
        
        for (User user : selection) {
            listeUtilisateursSelectionnes.getItems().remove(user);
            if (!listeUtilisateurs.getItems().contains(user)) {
                listeUtilisateurs.getItems().add(user);
            }
        }
    }
    
    /**
     * Ajoute tous les utilisateurs
     */
    private void ajouterTousUtilisateurs() {
        listeUtilisateursSelectionnes.getItems().addAll(listeUtilisateurs.getItems());
        listeUtilisateurs.getItems().clear();
    }
    
    /**
     * Retire tous les utilisateurs
     */
    private void retirerTousUtilisateurs() {
        listeUtilisateurs.getItems().addAll(listeUtilisateursSelectionnes.getItems());
        listeUtilisateursSelectionnes.getItems().clear();
    }
    
    /**
     * Valide le formulaire
     */
    private boolean validateForm() {
        if (listeUtilisateursSelectionnes.getItems().isEmpty()) {
            showError("Veuillez sélectionner au moins un utilisateur");
            return false;
        }
        
        return true;
    }
    
    /**
     * Crée les informations de partage
     */
    private PartageInfo createPartageInfo() {
        PartageInfo info = new PartageInfo();
        
        info.setUtilisateurs(new ArrayList<>(listeUtilisateursSelectionnes.getItems()));
        info.setNiveauAcces(comboNiveauAcces.getValue());
        info.setDateExpiration(dateExpiration.getValue());
        info.setNotifierEmail(chkNotifierEmail.isSelected());
        info.setMessage(textAreaMessage.getText().trim());
        
        return info;
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
}