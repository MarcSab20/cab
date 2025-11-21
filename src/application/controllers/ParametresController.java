package application.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.collections.FXCollections;
import application.models.User;
import application.services.UserService;
import application.services.AuthenticationService;
import application.utils.SessionManager;
import application.utils.AlertUtils;
import application.utils.PasswordUtils;

import java.net.URL;
import java.util.ResourceBundle;

public class ParametresController implements Initializable {
    
    // Navigation
    @FXML private VBox menuParametres;
    @FXML private Button btnParametresPersonnels;
    @FXML private Button btnParametresCompte;
    @FXML private Button btnParametresInterface;
    @FXML private Button btnParametresNotifications;
    @FXML private Button btnParametresSysteme;
    @FXML private Button btnParametresReseau;
    @FXML private Button btnParametresUtilisateurs;
    @FXML private Button btnParametresSauvegarde;
    
    // Boutons principaux
    @FXML private Button btnSauvegarderParametres;
    @FXML private Button btnAnnulerParametres;
    
    // Sections
    @FXML private VBox parametresPersonnels;
    @FXML private VBox parametresCompte;
    @FXML private VBox parametresInterface;
    @FXML private VBox parametresNotifications;
    
    // Paramètres personnels
    @FXML private TextField champNom;
    @FXML private TextField champPrenom;
    @FXML private TextField champEmail;
    @FXML private TextField champTelephone;
    @FXML private TextField champPoste;
    @FXML private TextField champService;
    @FXML private ComboBox<String> comboLangue;
    @FXML private ComboBox<String> comboFuseauHoraire;
    @FXML private ComboBox<String> comboFormatDate;
    @FXML private TextArea textAreaSignature;
    
    // Paramètres compte
    @FXML private PasswordField champMotDePasseActuel;
    @FXML private PasswordField champNouveauMotDePasse;
    @FXML private PasswordField champConfirmerMotDePasse;
    @FXML private ProgressBar forceMotDePasse;
    @FXML private Label labelForceMotDePasse;
    @FXML private CheckBox checkA2F;
    
    // Paramètres interface
    @FXML private ToggleGroup toggleTheme;
    @FXML private Slider sliderTaillePolice;
    @FXML private Label labelTaillePolice;
    @FXML private ComboBox<String> comboDensite;
    @FXML private CheckBox checkAnimations;
    @FXML private CheckBox checkSonsInterface;
    @FXML private CheckBox checkAffichageComplet;
    @FXML private CheckBox checkBarreEtat;
    @FXML private CheckBox checkRaccourcisClavier;
    @FXML private ComboBox<String> comboPageDemarrage;
    
    // Paramètres notifications
    @FXML private CheckBox checkNotificationsActives;
    @FXML private CheckBox checkNotificationsSon;
    @FXML private CheckBox checkNotificationsBulle;
    @FXML private CheckBox checkNotificationsBureaux;
    @FXML private CheckBox checkHorairesTravail;
    @FXML private ComboBox<String> comboHeureDebut;
    @FXML private ComboBox<String> comboHeureFin;
    @FXML private CheckBox checkWeekend;
    
    // Notifications par type
    @FXML private CheckBox checkCourrierApp;
    @FXML private CheckBox checkCourrierEmail;
    @FXML private CheckBox checkCourrierMobile;
    @FXML private CheckBox checkMessageApp;
    @FXML private CheckBox checkMessageEmail;
    @FXML private CheckBox checkMessageMobile;
    @FXML private CheckBox checkReunionApp;
    @FXML private CheckBox checkReunionEmail;
    @FXML private CheckBox checkReunionMobile;
    @FXML private CheckBox checkTacheApp;
    @FXML private CheckBox checkTacheEmail;
    @FXML private CheckBox checkTacheMobile;
    @FXML private CheckBox checkSystemeApp;
    @FXML private CheckBox checkSystemeEmail;
    @FXML private CheckBox checkSystemeMobile;
    
    private User currentUser;
    private UserService userService;
    private AuthenticationService authService;
    
    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println("ParametresController.initialize() appelé");
        
        try {
            currentUser = SessionManager.getInstance().getCurrentUser();
            userService = UserService.getInstance();
            authService = AuthenticationService.getInstance();
            
            if (currentUser == null) {
                System.err.println("ERREUR: Aucun utilisateur en session");
                return;
            }
            
            setupNavigation();
            setupComboBoxes();
            setupButtons();
            loadUserSettings();
            setupPasswordStrengthChecker();
            
            // Afficher les paramètres personnels par défaut
            showSection("personnels");
            
        } catch (Exception e) {
            System.err.println("Erreur dans ParametresController.initialize(): " + e.getMessage());
            e.printStackTrace();
        }
    }
    
    private void setupNavigation() {
        if (btnParametresPersonnels != null) {
            btnParametresPersonnels.setOnAction(e -> showSection("personnels"));
        }
        if (btnParametresCompte != null) {
            btnParametresCompte.setOnAction(e -> showSection("compte"));
        }
        if (btnParametresInterface != null) {
            btnParametresInterface.setOnAction(e -> showSection("interface"));
        }
        if (btnParametresNotifications != null) {
            btnParametresNotifications.setOnAction(e -> showSection("notifications"));
        }
        
        // Masquer les sections admin si pas admin
        boolean isAdmin = currentUser.getRole().getNom().toLowerCase().contains("admin");
        if (btnParametresSysteme != null) btnParametresSysteme.setVisible(isAdmin);
        if (btnParametresReseau != null) btnParametresReseau.setVisible(isAdmin);
        if (btnParametresUtilisateurs != null) btnParametresUtilisateurs.setVisible(isAdmin);
        if (btnParametresSauvegarde != null) btnParametresSauvegarde.setVisible(isAdmin);
    }
    
    private void setupComboBoxes() {
        // Langue
        if (comboLangue != null) {
            comboLangue.setItems(FXCollections.observableArrayList(
                "Français", "English", "Español"
            ));
            comboLangue.setValue("Français");
        }
        
        // Fuseau horaire
        if (comboFuseauHoraire != null) {
            comboFuseauHoraire.setItems(FXCollections.observableArrayList(
                "Europe/Paris", "Europe/London", "America/New_York", "Asia/Tokyo"
            ));
            comboFuseauHoraire.setValue("Europe/Paris");
        }
        
        // Format de date
        if (comboFormatDate != null) {
            comboFormatDate.setItems(FXCollections.observableArrayList(
                "DD/MM/YYYY", "MM/DD/YYYY", "YYYY-MM-DD"
            ));
            comboFormatDate.setValue("DD/MM/YYYY");
        }
        
        // Densité
        if (comboDensite != null) {
            comboDensite.setItems(FXCollections.observableArrayList(
                "Compacte", "Normale", "Confortable"
            ));
            comboDensite.setValue("Normale");
        }
        
        // Page de démarrage
        if (comboPageDemarrage != null) {
            comboPageDemarrage.setItems(FXCollections.observableArrayList(
                "Accueil", "Dashboard", "Courrier", "Messages", "Dernière page visitée"
            ));
            comboPageDemarrage.setValue("Accueil");
        }
        
        // Heures de travail
        if (comboHeureDebut != null) {
            comboHeureDebut.setItems(FXCollections.observableArrayList(
                "07:00", "08:00", "09:00"
            ));
            comboHeureDebut.setValue("08:00");
        }
        
        if (comboHeureFin != null) {
            comboHeureFin.setItems(FXCollections.observableArrayList(
                "17:00", "18:00", "19:00", "20:00"
            ));
            comboHeureFin.setValue("18:00");
        }
    }
    
    private void setupButtons() {
        if (btnSauvegarderParametres != null) {
            btnSauvegarderParametres.setOnAction(e -> handleSauvegarder());
        }
        if (btnAnnulerParametres != null) {
            btnAnnulerParametres.setOnAction(e -> handleAnnuler());
        }
    }
    
    private void setupPasswordStrengthChecker() {
        if (champNouveauMotDePasse != null && forceMotDePasse != null && labelForceMotDePasse != null) {
            champNouveauMotDePasse.textProperty().addListener((obs, oldVal, newVal) -> {
                int strength = PasswordUtils.getPasswordStrength(newVal);
                double progress = strength / 4.0;
                forceMotDePasse.setProgress(progress);
                
                String description = PasswordUtils.getPasswordStrengthDescription(strength);
                labelForceMotDePasse.setText("Sécurité: " + description);
                
                // Couleur selon la force
                String color;
                switch (strength) {
                    case 0:
                    case 1:
                        color = "#d32f2f";
                        break;
                    case 2:
                        color = "#f57c00";
                        break;
                    case 3:
                        color = "#fbc02d";
                        break;
                    case 4:
                        color = "#388e3c";
                        break;
                    default:
                        color = "#757575";
                }
                
                forceMotDePasse.setStyle("-fx-accent: " + color + ";");
                labelForceMotDePasse.setStyle("-fx-text-fill: " + color + ";");
            });
        }
    }
    
    private void loadUserSettings() {
        // Charger les informations personnelles
        if (champNom != null) champNom.setText(currentUser.getNom());
        if (champPrenom != null) champPrenom.setText(currentUser.getPrenom());
        if (champEmail != null) champEmail.setText(currentUser.getEmail());
        
        // Charger d'autres paramètres (à implémenter selon vos besoins)
        if (sliderTaillePolice != null && labelTaillePolice != null) {
            sliderTaillePolice.valueProperty().addListener((obs, oldVal, newVal) -> {
                labelTaillePolice.setText(String.format("%.0fpx", newVal.doubleValue()));
            });
            labelTaillePolice.setText("14px");
        }
    }
    
    private void showSection(String section) {
        // Masquer toutes les sections
        if (parametresPersonnels != null) {
            parametresPersonnels.setVisible(false);
            parametresPersonnels.setManaged(false);
        }
        if (parametresCompte != null) {
            parametresCompte.setVisible(false);
            parametresCompte.setManaged(false);
        }
        if (parametresInterface != null) {
            parametresInterface.setVisible(false);
            parametresInterface.setManaged(false);
        }
        if (parametresNotifications != null) {
            parametresNotifications.setVisible(false);
            parametresNotifications.setManaged(false);
        }
        
        // Réinitialiser les styles des boutons
        resetNavigationButtons();
        
        // Afficher la section demandée et activer le bouton
        switch (section) {
            case "personnels":
                if (parametresPersonnels != null) {
                    parametresPersonnels.setVisible(true);
                    parametresPersonnels.setManaged(true);
                }
                if (btnParametresPersonnels != null) {
                    btnParametresPersonnels.getStyleClass().add("sidebar-button-active");
                }
                break;
            case "compte":
                if (parametresCompte != null) {
                    parametresCompte.setVisible(true);
                    parametresCompte.setManaged(true);
                }
                if (btnParametresCompte != null) {
                    btnParametresCompte.getStyleClass().add("sidebar-button-active");
                }
                break;
            case "interface":
                if (parametresInterface != null) {
                    parametresInterface.setVisible(true);
                    parametresInterface.setManaged(true);
                }
                if (btnParametresInterface != null) {
                    btnParametresInterface.getStyleClass().add("sidebar-button-active");
                }
                break;
            case "notifications":
                if (parametresNotifications != null) {
                    parametresNotifications.setVisible(true);
                    parametresNotifications.setManaged(true);
                }
                if (btnParametresNotifications != null) {
                    btnParametresNotifications.getStyleClass().add("sidebar-button-active");
                }
                break;
        }
    }
    
    private void resetNavigationButtons() {
        Button[] buttons = {
            btnParametresPersonnels, btnParametresCompte, 
            btnParametresInterface, btnParametresNotifications
        };
        
        for (Button btn : buttons) {
            if (btn != null) {
                btn.getStyleClass().removeAll("sidebar-button-active");
                btn.getStyleClass().add("sidebar-button");
            }
        }
    }
    
    @FXML
    private void handleSauvegarder() {
        try {
            // Sauvegarder les informations personnelles
            if (champNom != null && !champNom.getText().isEmpty()) {
                currentUser.setNom(champNom.getText());
            }
            if (champPrenom != null && !champPrenom.getText().isEmpty()) {
                currentUser.setPrenom(champPrenom.getText());
            }
            if (champEmail != null && !champEmail.getText().isEmpty()) {
                currentUser.setEmail(champEmail.getText());
            }
            
            // Sauvegarder le changement de mot de passe si demandé
            if (champMotDePasseActuel != null && !champMotDePasseActuel.getText().isEmpty()) {
                handleChangePassword();
            }
            
            // Mettre à jour l'utilisateur
            if (userService.updateUser(currentUser)) {
                AlertUtils.showInfo("Paramètres sauvegardés avec succès");
            } else {
                AlertUtils.showError("Erreur lors de la sauvegarde des paramètres");
            }
            
        } catch (Exception e) {
            System.err.println("Erreur lors de la sauvegarde: " + e.getMessage());
            AlertUtils.showError("Erreur lors de la sauvegarde des paramètres");
        }
    }
    
    private void handleChangePassword() {
        try {
            String oldPassword = champMotDePasseActuel.getText();
            String newPassword = champNouveauMotDePasse.getText();
            String confirmPassword = champConfirmerMotDePasse.getText();
            
            // Validation
            if (oldPassword.isEmpty() || newPassword.isEmpty() || confirmPassword.isEmpty()) {
                AlertUtils.showWarning("Veuillez remplir tous les champs du mot de passe");
                return;
            }
            
            if (!newPassword.equals(confirmPassword)) {
                AlertUtils.showWarning("Les mots de passe ne correspondent pas");
                return;
            }
            
            if (!PasswordUtils.isValidPassword(newPassword)) {
                AlertUtils.showWarning("Le nouveau mot de passe ne respecte pas les critères de sécurité:\n" + 
                    PasswordUtils.getPasswordCriteria());
                return;
            }
            
            // Changer le mot de passe
            authService.changePassword(currentUser, oldPassword, newPassword);
            
            AlertUtils.showInfo("Mot de passe modifié avec succès");
            
            // Réinitialiser les champs
            champMotDePasseActuel.clear();
            champNouveauMotDePasse.clear();
            champConfirmerMotDePasse.clear();
            
        } catch (Exception e) {
            AlertUtils.showError("Erreur lors du changement de mot de passe: " + e.getMessage());
        }
    }
    
    @FXML
    private void handleAnnuler() {
        boolean confirm = AlertUtils.showConfirmation(
            "Confirmation",
            "Voulez-vous annuler les modifications ?"
        );
        
        if (confirm) {
            loadUserSettings();
            AlertUtils.showInfo("Modifications annulées");
        }
    }
}