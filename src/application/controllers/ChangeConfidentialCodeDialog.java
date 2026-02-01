package application.controllers;

import application.services.ConfidentialCodeService;
import application.utils.SessionManager;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.FontWeight;

import java.util.Optional;

/**
 * Dialogue pour changer le code confidentiel (Administrateurs uniquement)
 */
public class ChangeConfidentialCodeDialog extends Dialog<Boolean> {
    
    private PasswordField currentCodeField;
    private PasswordField newCodeField;
    private PasswordField confirmCodeField;
    private TextField descriptionField;
    private Label messageLabel;        // Initialisé dans le constructeur
    private Label strengthLabel;       // Initialisé dans le constructeur
    private ProgressBar strengthBar;
    private final ConfidentialCodeService codeService;
    
    public ChangeConfidentialCodeDialog() {
        this.codeService = ConfidentialCodeService.getInstance();
        
        setTitle("🔐 Changer le code confidentiel");
        setHeaderText(null);
        
        // Vérifier les permissions
        var currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser == null || !currentUser.getRole().isAdmin()) {
            Alert alert = new Alert(Alert.AlertType.ERROR);
            alert.setTitle("Accès refusé");
            alert.setHeaderText("Permission insuffisante");
            alert.setContentText("Seuls les administrateurs peuvent changer le code confidentiel.");
            alert.showAndWait();
            
            setResult(false);
            close();
            return;
        }
        
        // Boutons
        ButtonType btnChanger = new ButtonType("Changer le code", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnAnnuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(btnChanger, btnAnnuler);
        
        // Construction de l'interface
        VBox content = new VBox(20);
        content.setPadding(new Insets(25));
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPrefWidth(500);
        
        // En-tête
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label iconLabel = new Label("🔐");
        iconLabel.setStyle("-fx-font-size: 48px;");
        
        VBox titleBox = new VBox(5);
        Label titleLabel = new Label("Modifier le code confidentiel");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        titleLabel.setTextFill(Color.web("#2c3e50"));
        
        Label subtitleLabel = new Label("Configuration de sécurité système");
        subtitleLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px;");
        
        titleBox.getChildren().addAll(titleLabel, subtitleLabel);
        header.getChildren().addAll(iconLabel, titleBox);
        
        // Avertissement
        VBox warningBox = new VBox(8);
        warningBox.setStyle("-fx-background-color: #f8d7da; -fx-padding: 15; " +
                           "-fx-background-radius: 6; -fx-border-color: #dc3545; " +
                           "-fx-border-width: 1; -fx-border-radius: 6;");
        
        Label warningLabel = new Label("⚠️ ATTENTION");
        warningLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #721c24; -fx-font-size: 14px;");
        
        Label warningText = new Label(
            "Cette action va changer le code confidentiel pour TOUT le système. " +
            "Tous les utilisateurs devront utiliser le nouveau code pour accéder " +
            "aux ressources confidentielles."
        );
        warningText.setStyle("-fx-text-fill: #721c24; -fx-font-size: 12px;");
        warningText.setWrapText(true);
        
        warningBox.getChildren().addAll(warningLabel, warningText);
        
        // Code actuel
        VBox currentBox = new VBox(8);
        Label currentLabel = new Label("Code actuel *");
        currentLabel.setStyle("-fx-font-weight: bold;");
        
        currentCodeField = new PasswordField();
        currentCodeField.setPromptText("Entrez le code actuel");
        currentCodeField.setStyle("-fx-font-size: 14px; -fx-padding: 10;");
        
        currentBox.getChildren().addAll(currentLabel, currentCodeField);
        
        // Nouveau code
        VBox newBox = new VBox(8);
        Label newLabel = new Label("Nouveau code *");
        newLabel.setStyle("-fx-font-weight: bold;");
        
        newCodeField = new PasswordField();
        newCodeField.setPromptText("8 caractères (lettres et chiffres)");
        newCodeField.setStyle("-fx-font-size: 14px; -fx-padding: 10;");
        
        // Limitation à 8 caractères
        newCodeField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() > 8) {
                newCodeField.setText(oldVal);
            }
            updatePasswordStrength(newVal);
            validateForm();
        });
        
        // Barre de force du mot de passe
        strengthBar = new ProgressBar(0);
        strengthBar.setPrefWidth(400);
        strengthBar.setStyle("-fx-accent: #dc3545;");
        
        strengthLabel = new Label();
        strengthLabel.setStyle("-fx-font-size: 11px; -fx-text-fill: #6c757d;");
        
        newBox.getChildren().addAll(newLabel, newCodeField, strengthBar, strengthLabel);
        
        // Confirmation
        VBox confirmBox = new VBox(8);
        Label confirmLabel = new Label("Confirmer le nouveau code *");
        confirmLabel.setStyle("-fx-font-weight: bold;");
        
        confirmCodeField = new PasswordField();
        confirmCodeField.setPromptText("Retapez le nouveau code");
        confirmCodeField.setStyle("-fx-font-size: 14px; -fx-padding: 10;");
        
        confirmCodeField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() > 8) {
                confirmCodeField.setText(oldVal);
            }
            validateForm();
        });
        
        confirmBox.getChildren().addAll(confirmLabel, confirmCodeField);
        
        // Description
        VBox descBox = new VBox(8);
        Label descLabel = new Label("Description");
        descLabel.setStyle("-fx-font-weight: bold;");
        
        descriptionField = new TextField();
        descriptionField.setPromptText("Ex: Code confidentiel - Janvier 2025");
        descriptionField.setStyle("-fx-font-size: 14px; -fx-padding: 10;");
        
        descBox.getChildren().addAll(descLabel, descriptionField);
        
        // Message d'erreur
        messageLabel = new Label();
        messageLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
        messageLabel.setVisible(false);
        messageLabel.setWrapText(true);
        
        // Conseils
        VBox tipsBox = new VBox(8);
        tipsBox.setStyle("-fx-background-color: #d1ecf1; -fx-padding: 15; -fx-background-radius: 6;");
        
        Label tipsTitle = new Label("💡 Conseils de sécurité");
        tipsTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #0c5460;");
        
        VBox tipsList = new VBox(3);
        tipsList.getChildren().addAll(
            createTipLabel("• Utilisez une combinaison de lettres majuscules et minuscules"),
            createTipLabel("• Incluez des chiffres"),
            createTipLabel("• Évitez les séquences évidentes (12345678, ABCDEFGH)"),
            createTipLabel("• Ne réutilisez pas un ancien code"),
            createTipLabel("• Communiquez le nouveau code de manière sécurisée")
        );
        
        tipsBox.getChildren().addAll(tipsTitle, tipsList);
        
        // Assemblage
        content.getChildren().addAll(
            header,
            new Separator(),
            warningBox,
            currentBox,
            newBox,
            confirmBox,
            descBox,
            messageLabel,
            tipsBox
        );
        
        getDialogPane().setContent(content);
        getDialogPane().setStyle("-fx-background-color: white;");
        
        // Validation du bouton
        Button changeButton = (Button) getDialogPane().lookupButton(btnChanger);
        changeButton.setStyle("-fx-background-color: #dc3545; -fx-text-fill: white; " +
                             "-fx-font-weight: bold; -fx-padding: 8 20;");
        changeButton.setDisable(true);
        
        changeButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!performChange()) {
                event.consume();
            }
        });
        
        // Convertisseur de résultat
        setResultConverter(dialogButton -> {
            return dialogButton == btnChanger;
        });
        
        currentCodeField.requestFocus();
    }
    
    private Label createTipLabel(String text) {
        Label label = new Label(text);
        label.setStyle("-fx-text-fill: #0c5460; -fx-font-size: 11px;");
        label.setWrapText(true);
        return label;
    }
    
    private void updatePasswordStrength(String password) {
        if (password.isEmpty()) {
            strengthBar.setProgress(0);
            strengthLabel.setText("");
            strengthBar.setStyle("-fx-accent: #dc3545;");
            return;
        }
        
        double strength = 0;
        
        // Longueur
        if (password.length() >= 8) strength += 0.25;
        
        // Majuscules
        if (password.matches(".*[A-Z].*")) strength += 0.25;
        
        // Minuscules
        if (password.matches(".*[a-z].*")) strength += 0.25;
        
        // Chiffres
        if (password.matches(".*[0-9].*")) strength += 0.25;
        
        strengthBar.setProgress(strength);
        
        if (strength <= 0.25) {
            strengthLabel.setText("Faible");
            strengthBar.setStyle("-fx-accent: #dc3545;");
        } else if (strength <= 0.5) {
            strengthLabel.setText("Moyen");
            strengthBar.setStyle("-fx-accent: #ffc107;");
        } else if (strength <= 0.75) {
            strengthLabel.setText("Bon");
            strengthBar.setStyle("-fx-accent: #17a2b8;");
        } else {
            strengthLabel.setText("Excellent");
            strengthBar.setStyle("-fx-accent: #28a745;");
        }
    }
    
    private void validateForm() {
        Button changeButton = (Button) getDialogPane().lookupButton(
            getDialogPane().getButtonTypes().get(0)
        );
        
        boolean valid = !currentCodeField.getText().isEmpty() &&
                       newCodeField.getText().length() == 8 &&
                       confirmCodeField.getText().equals(newCodeField.getText()) &&
                       newCodeField.getText().matches("^[a-zA-Z0-9]{8}$");
        
        changeButton.setDisable(!valid);
        
        // Afficher message si les codes ne correspondent pas
        if (!confirmCodeField.getText().isEmpty() && 
            !confirmCodeField.getText().equals(newCodeField.getText())) {
            showError("Les codes ne correspondent pas");
        } else {
            messageLabel.setVisible(false);
        }
    }
    
    private boolean performChange() {
        String currentCode = currentCodeField.getText();
        String newCode = newCodeField.getText();
        String description = descriptionField.getText();
        
        if (description.trim().isEmpty()) {
            description = "Code modifié le " + 
                         java.time.LocalDateTime.now().format(
                             java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")
                         );
        }
        
        // Tenter le changement
        boolean success = codeService.changeCode(currentCode, newCode, description);
        
        if (!success) {
            showError("❌ Échec du changement. Vérifiez le code actuel.");
            currentCodeField.clear();
            currentCodeField.requestFocus();
            return false;
        }
        
        // Succès
        Alert alert = new Alert(Alert.AlertType.INFORMATION);
        alert.setTitle("Succès");
        alert.setHeaderText("Code confidentiel modifié");
        alert.setContentText("✅ Le code confidentiel a été changé avec succès.\n\n" +
                            "Nouveau code: " + newCode + "\n\n" +
                            "⚠️ Communiquez ce code de manière sécurisée aux utilisateurs autorisés.");
        alert.showAndWait();
        
        return true;
    }
    
    private void showError(String message) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
    }
    
    /**
     * Affiche le dialogue
     * @return true si le code a été changé
     */
    public static boolean showDialog() {
        ChangeConfidentialCodeDialog dialog = new ChangeConfidentialCodeDialog();
        Optional<Boolean> result = dialog.showAndWait();
        return result.orElse(false);
    }
}