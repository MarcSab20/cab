package application.controllers;

import javafx.beans.binding.Bindings;
import javafx.beans.binding.BooleanBinding;
import javafx.beans.property.BooleanProperty;
import javafx.beans.property.SimpleBooleanProperty;
import javafx.application.Platform;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.input.KeyCode;
import javafx.stage.Stage;
import application.models.Role;
import application.models.User;
import application.services.AuthenticationService;
import application.services.UserService;
import application.utils.AlertUtils;
import application.utils.SessionManager;

import java.io.IOException;
import java.net.URL;
import java.util.ResourceBundle;

/**
 * Contrôleur pour la page de connexion
 */
public class LoginController implements Initializable {
    
    @FXML private TextField codeField;
    @FXML private PasswordField passwordField;
    @FXML private ComboBox<Role> roleComboBox;
    @FXML private Button loginButton;
    @FXML private Label statusLabel;
    @FXML private ProgressIndicator loadingIndicator;
    
    private AuthenticationService authService;
    private final BooleanProperty loading = new SimpleBooleanProperty(false);
    private UserService userService;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        authService = AuthenticationService.getInstance();
        userService = UserService.getInstance();
        
        setupUI();
        loadRoles();
    }
    
    private void setupUI() {
        // Configuration initiale de l'interface
        loadingIndicator.setVisible(false);
        statusLabel.setText("");
        
        // Configuration du bouton de connexion
        loginButton.setDefaultButton(true);
        
        // Validation en temps réel
        setupValidation();
        
        // Gestion des événements clavier
        setupKeyboardEvents();
    }
    
    private void setupValidation() {
        // Validation des champs obligatoires
        BooleanBinding isValid = Bindings.createBooleanBinding(() -> 
            !codeField.getText().trim().isEmpty() &&
            !passwordField.getText().trim().isEmpty() &&
            roleComboBox.getValue() != null,
            codeField.textProperty(),
            passwordField.textProperty(),
            roleComboBox.valueProperty()
        );
        
        loginButton.disableProperty().bind(isValid.not().or(loading));
    }
    
    private void setupKeyboardEvents() {
        // Connexion avec la touche Entrée
        codeField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                passwordField.requestFocus();
            }
        });
        
        passwordField.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                if (roleComboBox.getValue() != null) {
                    handleLogin();
                } else {
                    roleComboBox.requestFocus();
                }
            }
        });
        
        roleComboBox.setOnKeyPressed(event -> {
            if (event.getCode() == KeyCode.ENTER) {
                handleLogin();
            }
        });
    }
    
    private void loadRoles() {
        try {
            ObservableList<Role> roles = FXCollections.observableArrayList(
                userService.getRolesActifs()
            );
            roleComboBox.setItems(roles);
            
            // Affichage personnalisé des rôles
            roleComboBox.setCellFactory(listView -> new ListCell<Role>() {
                @Override
                protected void updateItem(Role role, boolean empty) {
                    super.updateItem(role, empty);
                    if (empty || role == null) {
                        setText(null);
                    } else {
                        setText(role.getNom());
                    }
                }
            });
            
            roleComboBox.setButtonCell(roleComboBox.getCellFactory().call(null));
            
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur lors du chargement des rôles: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleLogin() {
        if (!validateInput()) {
            return;
        }
        
        showLoading(true);
        
        String code = codeField.getText().trim();
        String password = passwordField.getText();
        Role selectedRole = roleComboBox.getValue();
        
        // Authentification asynchrone
        Thread authThread = new Thread(() -> {
            try {
                User user = authService.authenticate(code, password, selectedRole);
                
                Platform.runLater(() -> {
                    if (user != null) {
                        handleSuccessfulLogin(user);
                    } else {
                        handleFailedLogin("Code, mot de passe ou rôle incorrect");
                    }
                    showLoading(false);
                });
                
            } catch (Exception e) {
                e.printStackTrace();
                Platform.runLater(() -> {
                    handleFailedLogin("Erreur de connexion: " + e.getMessage());
                    showLoading(false);
                });
            }
        });
        
        authThread.setDaemon(true);
        authThread.start();
    }
    
    private boolean validateInput() {
        if (codeField.getText().trim().isEmpty()) {
            showError("Veuillez saisir votre code utilisateur");
            codeField.requestFocus();
            return false;
        }
        
        if (passwordField.getText().isEmpty()) {
            showError("Veuillez saisir votre mot de passe");
            passwordField.requestFocus();
            return false;
        }
        
        if (roleComboBox.getValue() == null) {
            showError("Veuillez sélectionner un rôle");
            roleComboBox.requestFocus();
            return false;
        }
        
        return true;
    }
    
    private void handleSuccessfulLogin(User user) {
        try {
            System.out.println("Début du chargement de l'interface principale...");
            
            // Enregistrement de la session
            SessionManager.getInstance().setCurrentUser(user);
            System.out.println("Session utilisateur enregistrée");
            
            // Chargement de l'interface principale
            loadMainInterface();
            System.out.println("Interface principale chargée avec succès");
            
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur lors du chargement de l'interface: " + e.getMessage());
            showLoading(false);
        }
    }
    
    private void handleFailedLogin(String message) {
        showError(message);
        passwordField.clear();
        passwordField.requestFocus();
        
        // Log de la tentative de connexion échouée
        authService.logFailedAttempt(codeField.getText().trim());
    }
    
    private void loadMainInterface() throws IOException {
        try {
            System.out.println("Tentative de chargement de main.fxml...");
            
            // Vérifier d'abord si le fichier existe
            URL fxmlUrl = getClass().getResource("/application/views/main.fxml");
            if (fxmlUrl == null) {
                // Essayer un autre chemin
                fxmlUrl = getClass().getResource("/views/main.fxml");
                if (fxmlUrl == null) {
                    throw new IOException("Fichier main.fxml introuvable dans les ressources");
                }
            }
            
            System.out.println("Fichier FXML trouvé: " + fxmlUrl);
            
            FXMLLoader loader = new FXMLLoader(fxmlUrl);
            Parent root = loader.load();
            System.out.println("FXML chargé avec succès");
            
            Scene scene = new Scene(root);
            
            // Chargement du CSS
            try {
                URL cssUrl = getClass().getResource("/application/styles/application.css");
                if (cssUrl == null) {
                    cssUrl = getClass().getResource("/styles/application.css");
                }
                if (cssUrl != null) {
                    scene.getStylesheets().add(cssUrl.toExternalForm());
                    System.out.println("CSS chargé: " + cssUrl);
                } else {
                    System.out.println("Fichier CSS non trouvé, continuons sans");
                }
            } catch (Exception cssError) {
                System.out.println("Erreur CSS (non critique): " + cssError.getMessage());
            }
            
            Stage currentStage = (Stage) loginButton.getScene().getWindow();
            currentStage.setScene(scene);
            currentStage.centerOnScreen();
            
            System.out.println("Interface principale affichée");
            
        } catch (Exception e) {
            System.err.println("Erreur détaillée lors du chargement de l'interface:");
            e.printStackTrace();
            throw new IOException("Erreur lors du chargement de l'interface principale: " + e.getMessage(), e);
        }
    }
    
    private void showLoading(boolean show) {
        loading.set(show);
        loadingIndicator.setVisible(show);
        codeField.setDisable(show);
        passwordField.setDisable(show);
        roleComboBox.setDisable(show);
    }
    
    private void showError(String message) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("success-label");
        statusLabel.getStyleClass().add("error-label");
    }
    
    private void showSuccess(String message) {
        statusLabel.setText(message);
        statusLabel.getStyleClass().removeAll("error-label");
        statusLabel.getStyleClass().add("success-label");
    }
    
    @FXML
    private void handleForgotPassword() {
        AlertUtils.showInfo("Contactez votre administrateur système pour réinitialiser votre mot de passe.");
    }
}