package application.controllers;

import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import application.models.Role;
import application.models.User;
import application.utils.PasswordUtils;


/**
 * Dialogue pour créer ou modifier un utilisateur
 */
public class UserFormDialog extends Dialog<User> {
    
    private final TextField champCode;
    private final PasswordField champMotDePasse;
    private final PasswordField champConfirmationMotDePasse;
    private final TextField champNom;
    private final TextField champPrenom;
    private final TextField champEmail;
    private final TextField champTelephone;
    private final ComboBox<Role> comboRole;
    private final CheckBox checkActif;
    private final Label labelForceMotDePasse;
    
    private final User utilisateur;
    private final boolean isNewUser;
    
    /**
     * Constructeur
     * @param user Utilisateur à modifier (null pour créer un nouvel utilisateur)
     * @param roles Liste des rôles disponibles
     */
    public UserFormDialog(User user, ObservableList<Role> roles) {
        this.utilisateur = user;
        this.isNewUser = (user == null);
        
        // Configuration du dialogue
        setTitle(isNewUser ? "Nouvel utilisateur" : "Modifier l'utilisateur");
        setHeaderText(isNewUser ? "Créer un nouvel utilisateur" : "Modifier les informations de l'utilisateur");
        initModality(Modality.APPLICATION_MODAL);
        
        // Boutons
        ButtonType btnValider = new ButtonType(isNewUser ? "Créer" : "Modifier", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnAnnuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(btnValider, btnAnnuler);
        
        // Création du formulaire
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // Champs
        champCode = new TextField();
        champCode.setPromptText("Code utilisateur unique");
        
        champMotDePasse = new PasswordField();
        champMotDePasse.setPromptText(isNewUser ? "Mot de passe" : "Nouveau mot de passe (laisser vide pour ne pas changer)");
        
        champConfirmationMotDePasse = new PasswordField();
        champConfirmationMotDePasse.setPromptText("Confirmer le mot de passe");
        
        labelForceMotDePasse = new Label();
        labelForceMotDePasse.setStyle("-fx-text-fill: gray;");
        
        champNom = new TextField();
        champNom.setPromptText("Nom de famille");
        
        champPrenom = new TextField();
        champPrenom.setPromptText("Prénom");
        
        champEmail = new TextField();
        champEmail.setPromptText("adresse@exemple.com");
        
        champTelephone = new TextField();
        champTelephone.setPromptText("Numéro de téléphone (optionnel)");
        
        comboRole = new ComboBox<>(roles);
        comboRole.setPromptText("Sélectionner un rôle");
        comboRole.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Role role, boolean empty) {
                super.updateItem(role, empty);
                setText(empty || role == null ? null : role.getNom());
            }
        });
        comboRole.setButtonCell(comboRole.getCellFactory().call(null));
        
        checkActif = new CheckBox("Compte actif");
        checkActif.setSelected(true);
        
        // Ajout des champs au grid
        int row = 0;
        grid.add(new Label("Code utilisateur:"), 0, row);
        grid.add(champCode, 1, row++);
        
        grid.add(new Label("Mot de passe:"), 0, row);
        grid.add(champMotDePasse, 1, row++);
        
        grid.add(new Label("Confirmer:"), 0, row);
        grid.add(champConfirmationMotDePasse, 1, row++);
        
        grid.add(new Label(""), 0, row);
        grid.add(labelForceMotDePasse, 1, row++);
        
        grid.add(new Separator(), 0, row);
        grid.add(new Separator(), 1, row++);
        
        grid.add(new Label("Nom:"), 0, row);
        grid.add(champNom, 1, row++);
        
        grid.add(new Label("Prénom:"), 0, row);
        grid.add(champPrenom, 1, row++);
        
        grid.add(new Label("Email:"), 0, row);
        grid.add(champEmail, 1, row++);
        
        grid.add(new Label("Téléphone:"), 0, row);
        grid.add(champTelephone, 1, row++);
        
        grid.add(new Label("Rôle:"), 0, row);
        grid.add(comboRole, 1, row++);
        
        grid.add(new Label(""), 0, row);
        grid.add(checkActif, 1, row++);
        
        getDialogPane().setContent(grid);
        
        // Si modification, remplir les champs
        if (!isNewUser) {
            champCode.setText(utilisateur.getCode());
            champCode.setDisable(true); // Le code ne peut pas être modifié
            champNom.setText(utilisateur.getNom());
            champPrenom.setText(utilisateur.getPrenom());
            champEmail.setText(utilisateur.getEmail());
            comboRole.setValue(utilisateur.getRole());
            checkActif.setSelected(utilisateur.isActif());
        }
        
        // Listener pour la force du mot de passe
        champMotDePasse.textProperty().addListener((obs, oldVal, newVal) -> {
            updatePasswordStrength(newVal);
        });
        
        // Validation
        Button btnValidate = (Button) getDialogPane().lookupButton(btnValider);
        btnValidate.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!validateForm()) {
                event.consume();
            }
        });
        
        // Conversion du résultat
        setResultConverter(dialogButton -> {
            if (dialogButton == btnValider) {
                return createUserFromForm();
            }
            return null;
        });
    }
    
    private void updatePasswordStrength(String password) {
        if (password == null || password.isEmpty()) {
            labelForceMotDePasse.setText("");
            return;
        }
        
        int strength = PasswordUtils.getPasswordStrength(password);
        String description = PasswordUtils.getPasswordStrengthDescription(strength);
        
        String color;
        switch (strength) {
            case 0:
            case 1:
                color = "#d32f2f"; // Rouge
                break;
            case 2:
                color = "#f57c00"; // Orange
                break;
            case 3:
                color = "#fbc02d"; // Jaune
                break;
            case 4:
                color = "#388e3c"; // Vert
                break;
            default:
                color = "gray";
        }
        
        labelForceMotDePasse.setText("Force: " + description);
        labelForceMotDePasse.setStyle("-fx-text-fill: " + color + ";");
    }
    
    private boolean validateForm() {
        // Validation du code
        if (champCode.getText().trim().isEmpty()) {
            showError("Le code utilisateur est obligatoire");
            return false;
        }
        
        // Validation du mot de passe pour nouvel utilisateur
        if (isNewUser) {
            if (champMotDePasse.getText().isEmpty()) {
                showError("Le mot de passe est obligatoire");
                return false;
            }
            
            if (!PasswordUtils.isValidPassword(champMotDePasse.getText())) {
                showError("Le mot de passe ne respecte pas les critères de sécurité:\n" + 
                         PasswordUtils.getPasswordCriteria());
                return false;
            }
        } else {
            // Si modification et mot de passe fourni, le valider
            if (!champMotDePasse.getText().isEmpty() && 
                !PasswordUtils.isValidPassword(champMotDePasse.getText())) {
                showError("Le nouveau mot de passe ne respecte pas les critères de sécurité:\n" + 
                         PasswordUtils.getPasswordCriteria());
                return false;
            }
        }
        
        // Vérification de la confirmation du mot de passe
        if (!champMotDePasse.getText().equals(champConfirmationMotDePasse.getText())) {
            showError("Les mots de passe ne correspondent pas");
            return false;
        }
        
        // Validation du nom
        if (champNom.getText().trim().isEmpty()) {
            showError("Le nom est obligatoire");
            return false;
        }
        
        // Validation du prénom
        if (champPrenom.getText().trim().isEmpty()) {
            showError("Le prénom est obligatoire");
            return false;
        }
        
        // Validation de l'email
        if (champEmail.getText().trim().isEmpty()) {
            showError("L'email est obligatoire");
            return false;
        }
        
        if (!isValidEmail(champEmail.getText().trim())) {
            showError("L'adresse email n'est pas valide");
            return false;
        }
        
        // Validation du rôle
        if (comboRole.getValue() == null) {
            showError("Veuillez sélectionner un rôle");
            return false;
        }
        
        return true;
    }
    
    private boolean isValidEmail(String email) {
        String emailRegex = "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$";
        return email.matches(emailRegex);
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setTitle("Erreur de validation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private User createUserFromForm() {
        User user;
        
        if (isNewUser) {
            user = new User();
            user.setCode(champCode.getText().trim());
            
            // Hachage du mot de passe
            String hashedPassword = PasswordUtils.hashPassword(champMotDePasse.getText());
            user.setPassword(hashedPassword);
        } else {
            user = utilisateur;
            
            // Si un nouveau mot de passe est fourni, le hacher
            if (!champMotDePasse.getText().isEmpty()) {
                String hashedPassword = PasswordUtils.hashPassword(champMotDePasse.getText());
                user.setPassword(hashedPassword);
            }
        }
        
        user.setNom(champNom.getText().trim());
        user.setPrenom(champPrenom.getText().trim());
        user.setEmail(champEmail.getText().trim());
        user.setRole(comboRole.getValue());
        user.setActif(checkActif.isSelected());
        
        return user;
    }
}