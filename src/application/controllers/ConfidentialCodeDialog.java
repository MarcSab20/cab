package application.controllers;

import application.services.ConfidentialCodeService;
import application.services.ConfidentialCodeService.ActionType;
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
 * Dialogue pour saisir le code confidentiel
 * VERSION SÉCURISÉE avec validation et limitation des tentatives
 */
public class ConfidentialCodeDialog extends Dialog<String> {
    
    private final PasswordField codeField;
    private Label messageLabel;      // Initialisé dans le constructeur
    private Label attemptsLabel;     // Initialisé dans le constructeur
    private final ConfidentialCodeService codeService;
    private final ActionType actionType;
    private int attemptCount = 0;
    private static final int MAX_ATTEMPTS = 3;
    
    /**
     * Constructeur
     * @param actionType Type d'action nécessitant le code
     */
    public ConfidentialCodeDialog(ActionType actionType) {
        this.actionType = actionType;
        this.codeService = ConfidentialCodeService.getInstance();
        
        setTitle("🔒 Code confidentiel requis");
        setHeaderText(null);
        
        // Configuration des boutons
        ButtonType btnValider = new ButtonType("Valider", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnAnnuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(btnValider, btnAnnuler);
        
        // Construction de l'interface
        VBox content = new VBox(20);
        content.setPadding(new Insets(20));
        content.setAlignment(Pos.CENTER_LEFT);
        content.setPrefWidth(450);
        
        // Icône et titre
        HBox header = new HBox(15);
        header.setAlignment(Pos.CENTER_LEFT);
        
        Label iconLabel = new Label("🔐");
        iconLabel.setStyle("-fx-font-size: 48px;");
        
        VBox titleBox = new VBox(5);
        Label titleLabel = new Label("Accès sécurisé");
        titleLabel.setFont(Font.font("System", FontWeight.BOLD, 18));
        titleLabel.setTextFill(Color.web("#2c3e50"));
        
        Label subtitleLabel = new Label(actionType.getDescription());
        subtitleLabel.setStyle("-fx-text-fill: #7f8c8d; -fx-font-size: 13px;");
        
        titleBox.getChildren().addAll(titleLabel, subtitleLabel);
        header.getChildren().addAll(iconLabel, titleBox);
        
        // Message d'information
        VBox infoBox = new VBox(8);
        infoBox.setStyle("-fx-background-color: #fff3cd; -fx-padding: 12; " +
                        "-fx-background-radius: 6; -fx-border-color: #ffc107; " +
                        "-fx-border-width: 1; -fx-border-radius: 6;");
        
        Label infoLabel = new Label("⚠️ Cette action nécessite un code confidentiel");
        infoLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #856404;");
        
        Label descLabel = new Label("Veuillez saisir le code à 8 caractères (lettres et chiffres)");
        descLabel.setStyle("-fx-text-fill: #856404; -fx-font-size: 12px;");
        descLabel.setWrapText(true);
        
        infoBox.getChildren().addAll(infoLabel, descLabel);
        
        // Champ de saisie du code
        VBox fieldBox = new VBox(8);
        
        Label fieldLabel = new Label("Code confidentiel *");
        fieldLabel.setStyle("-fx-font-weight: bold; -fx-text-fill: #2c3e50;");
        
        codeField = new PasswordField();
        codeField.setPromptText("Entrez le code (8 caractères)");
        codeField.setStyle("-fx-font-size: 14px; -fx-padding: 10;");
        codeField.setPrefWidth(400);
        
        // Limitation à 8 caractères
        codeField.textProperty().addListener((obs, oldVal, newVal) -> {
            if (newVal.length() > 8) {
                codeField.setText(oldVal);
            }
            // Effacer le message d'erreur quand l'utilisateur tape
            if (messageLabel != null && !newVal.isEmpty()) {
                messageLabel.setVisible(false);
            }
        });
        
        // Indicateur de progression
        HBox progressBox = new HBox(5);
        progressBox.setAlignment(Pos.CENTER_LEFT);
        
        for (int i = 0; i < 8; i++) {
            final int index = i;
            Region indicator = new Region();
            indicator.setPrefSize(30, 5);
            indicator.setStyle("-fx-background-color: #dee2e6; -fx-background-radius: 3;");
            
            codeField.textProperty().addListener((obs, oldVal, newVal) -> {
                if (newVal.length() > index) {
                    indicator.setStyle("-fx-background-color: #28a745; -fx-background-radius: 3;");
                } else {
                    indicator.setStyle("-fx-background-color: #dee2e6; -fx-background-radius: 3;");
                }
            });
            
            progressBox.getChildren().add(indicator);
        }
        
        fieldBox.getChildren().addAll(fieldLabel, codeField, progressBox);
        
        // Label de message d'erreur
        messageLabel = new Label();
        messageLabel.setStyle("-fx-text-fill: #dc3545; -fx-font-weight: bold;");
        messageLabel.setVisible(false);
        messageLabel.setWrapText(true);
        
        // Label du nombre de tentatives
        attemptsLabel = new Label();
        attemptsLabel.setStyle("-fx-text-fill: #6c757d; -fx-font-size: 12px;");
        attemptsLabel.setVisible(false);
        
        // Aide
        VBox helpBox = new VBox(5);
        helpBox.setStyle("-fx-background-color: #e7f3ff; -fx-padding: 10; -fx-background-radius: 6;");
        
        Label helpTitle = new Label("💡 Besoin d'aide ?");
        helpTitle.setStyle("-fx-font-weight: bold; -fx-text-fill: #004085;");
        
        Label helpText = new Label("Contactez votre administrateur système pour obtenir " +
                                  "le code confidentiel ou le réinitialiser.");
        helpText.setStyle("-fx-text-fill: #004085; -fx-font-size: 11px;");
        helpText.setWrapText(true);
        
        helpBox.getChildren().addAll(helpTitle, helpText);
        
        // Assemblage
        content.getChildren().addAll(
            header,
            new Separator(),
            infoBox,
            fieldBox,
            messageLabel,
            attemptsLabel,
            helpBox
        );
        
        getDialogPane().setContent(content);
        
        // Style
        getDialogPane().setStyle("-fx-background-color: white;");
        
        // Focus automatique sur le champ
        codeField.requestFocus();
        
        // Validation du bouton
        Button validateButton = (Button) getDialogPane().lookupButton(btnValider);
        validateButton.setStyle("-fx-background-color: #007bff; -fx-text-fill: white; " +
                               "-fx-font-weight: bold; -fx-padding: 8 20;");
        
        // Désactiver le bouton si le champ est vide
        validateButton.disableProperty().bind(
            codeField.textProperty().isEmpty()
        );
        
        // Gestion de la validation
        validateButton.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!validateCode()) {
                event.consume();
            }
        });
        
        // Convertisseur de résultat
        setResultConverter(dialogButton -> {
            if (dialogButton == btnValider) {
                return codeField.getText();
            }
            return null;
        });
        
        // Vérifier si l'utilisateur est bloqué
        checkIfUserBlocked();
    }
    
    /**
     * Vérifie si l'utilisateur est temporairement bloqué
     */
    private void checkIfUserBlocked() {
        var currentUser = SessionManager.getInstance().getCurrentUser();
        if (currentUser != null && codeService.isUserBlocked(currentUser.getId())) {
            codeField.setDisable(true);
            showError("⚠️ Trop de tentatives échouées. Veuillez réessayer dans 15 minutes.");
            
            Button validateButton = (Button) getDialogPane().lookupButton(
                getDialogPane().getButtonTypes().get(0)
            );
            validateButton.setDisable(true);
        }
    }
    
    /**
     * Valide le code saisi
     */
    private boolean validateCode() {
        String code = codeField.getText();
        
        // Validation du format
        if (!code.matches("^[a-zA-Z0-9]{8}$")) {
            showError("❌ Le code doit contenir exactement 8 caractères alphanumériques");
            attemptCount++;
            updateAttemptsLabel();
            return false;
        }
        
        // Validation du code
        boolean isValid = codeService.validateCode(code);
        
        if (!isValid) {
            attemptCount++;
            
            if (attemptCount >= MAX_ATTEMPTS) {
                showError("🚫 Nombre maximum de tentatives atteint. Accès refusé.");
                
                // Désactiver le champ et le bouton
                codeField.setDisable(true);
                Button validateButton = (Button) getDialogPane().lookupButton(
                    getDialogPane().getButtonTypes().get(0)
                );
                validateButton.setDisable(true);
                
                return false;
            } else {
                int remaining = MAX_ATTEMPTS - attemptCount;
                showError("❌ Code incorrect. Il vous reste " + remaining + 
                         " tentative" + (remaining > 1 ? "s" : ""));
                updateAttemptsLabel();
                
                // Effacer le champ
                codeField.clear();
                codeField.requestFocus();
                
                return false;
            }
        }
        
        return true;
    }
    
    /**
     * Affiche un message d'erreur
     */
    private void showError(String message) {
        messageLabel.setText(message);
        messageLabel.setVisible(true);
        
        // Animation de secousse
        codeField.setStyle("-fx-border-color: #dc3545; -fx-border-width: 2; " +
                          "-fx-font-size: 14px; -fx-padding: 10;");
        
        // Réinitialiser le style après 1 seconde
        javafx.animation.PauseTransition pause = 
            new javafx.animation.PauseTransition(javafx.util.Duration.seconds(1));
        pause.setOnFinished(e -> 
            codeField.setStyle("-fx-font-size: 14px; -fx-padding: 10;")
        );
        pause.play();
    }
    
    /**
     * Met à jour le label du nombre de tentatives
     */
    private void updateAttemptsLabel() {
        if (attemptCount > 0) {
            attemptsLabel.setText("Tentative " + attemptCount + " sur " + MAX_ATTEMPTS);
            attemptsLabel.setVisible(true);
        }
    }
    
    /**
     * Affiche le dialogue et retourne le code validé
     * @return Le code validé ou null si annulé
     */
    public static String showAndValidate(ActionType actionType) {
        ConfidentialCodeDialog dialog = new ConfidentialCodeDialog(actionType);
        Optional<String> result = dialog.showAndWait();
        return result.orElse(null);
    }
}