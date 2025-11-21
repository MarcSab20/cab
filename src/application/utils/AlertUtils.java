package application.utils;

import javafx.scene.control.Alert;
import javafx.scene.control.ButtonType;
import javafx.scene.control.TextInputDialog;
import javafx.scene.control.ChoiceDialog;
import javafx.scene.image.Image;
import javafx.stage.Stage;
import javafx.stage.FileChooser;
import javafx.stage.DirectoryChooser;

import java.io.File;
import java.util.List;
import java.util.Optional;

/**
 * Utilitaires pour l'affichage d'alertes et de boîtes de dialogue
 */
public class AlertUtils {
    
    private static final String APP_TITLE = "Gestion Documentaire";
    private static String iconPath = "/images/app-icon.png";
    
    /**
     * Affiche une boîte de dialogue d'information
     */
    public static void showInfo(String message) {
        showInfo("Information", message);
    }
    
    /**
     * Affiche une boîte de dialogue d'information avec titre personnalisé
     */
    public static void showInfo(String title, String message) {
        Alert alert = createAlert(Alert.AlertType.INFORMATION, title, message);
        alert.showAndWait();
    }
    
    /**
     * Affiche une boîte de dialogue d'avertissement
     */
    public static void showWarning(String message) {
        showWarning("Avertissement", message);
    }
    
    /**
     * Affiche une boîte de dialogue d'avertissement avec titre personnalisé
     */
    public static void showWarning(String title, String message) {
        Alert alert = createAlert(Alert.AlertType.WARNING, title, message);
        alert.showAndWait();
    }
    
    /**
     * Affiche une boîte de dialogue d'erreur
     */
    public static void showError(String message) {
        showError("Erreur", message);
    }
    
    /**
     * Affiche une boîte de dialogue d'erreur avec titre personnalisé
     */
    public static void showError(String title, String message) {
        Alert alert = createAlert(Alert.AlertType.ERROR, title, message);
        alert.showAndWait();
    }
    
    /**
     * Affiche une boîte de dialogue de confirmation
     */
    public static boolean showConfirmation(String message) {
        return showConfirmation("Confirmation", message);
    }
    
    /**
     * Affiche une boîte de dialogue de confirmation avec titre personnalisé
     */
    public static boolean showConfirmation(String title, String message) {
        Alert alert = createAlert(Alert.AlertType.CONFIRMATION, title, message);
        
        Optional<ButtonType> result = alert.showAndWait();
        return result.isPresent() && result.get() == ButtonType.OK;
    }
    
    /**
     * Affiche une boîte de dialogue de confirmation avec boutons personnalisés
     */
    public static Optional<ButtonType> showCustomConfirmation(String title, String message, 
                                                            ButtonType... buttonTypes) {
        Alert alert = createAlert(Alert.AlertType.CONFIRMATION, title, message);
        alert.getButtonTypes().setAll(buttonTypes);
        
        return alert.showAndWait();
    }
    
    /**
     * Affiche une boîte de dialogue de saisie de texte
     */
    public static Optional<String> showTextInput(String title, String headerText, String defaultValue) {
        TextInputDialog dialog = new TextInputDialog(defaultValue);
        configureDialog(dialog, title, headerText);
        
        return dialog.showAndWait();
    }
    
    /**
     * Affiche une boîte de dialogue de saisie de texte simple
     */
    public static Optional<String> showTextInput(String message) {
        return showTextInput("Saisie", message, "");
    }
    
    /**
     * Affiche une boîte de dialogue de choix parmi une liste
     */
    public static <T> Optional<T> showChoiceDialog(String title, String headerText, 
                                                   List<T> choices, T defaultChoice) {
        ChoiceDialog<T> dialog = new ChoiceDialog<>(defaultChoice, choices);
        configureDialog(dialog, title, headerText);
        
        return dialog.showAndWait();
    }
    
    /**
     * Affiche une boîte de dialogue pour sélectionner un fichier
     */
    public static Optional<File> showFileChooser(String title, String initialDirectory, 
                                                FileChooser.ExtensionFilter... filters) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        
        if (initialDirectory != null) {
            File dir = new File(initialDirectory);
            if (dir.exists() && dir.isDirectory()) {
                fileChooser.setInitialDirectory(dir);
            }
        }
        
        if (filters != null && filters.length > 0) {
            fileChooser.getExtensionFilters().addAll(filters);
        }
        
        Stage stage = getCurrentStage();
        File selectedFile = fileChooser.showOpenDialog(stage);
        
        return Optional.ofNullable(selectedFile);
    }
    
    /**
     * Affiche une boîte de dialogue pour sélectionner plusieurs fichiers
     */
    public static List<File> showMultipleFileChooser(String title, String initialDirectory,
                                                    FileChooser.ExtensionFilter... filters) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        
        if (initialDirectory != null) {
            File dir = new File(initialDirectory);
            if (dir.exists() && dir.isDirectory()) {
                fileChooser.setInitialDirectory(dir);
            }
        }
        
        if (filters != null && filters.length > 0) {
            fileChooser.getExtensionFilters().addAll(filters);
        }
        
        Stage stage = getCurrentStage();
        List<File> selectedFiles = fileChooser.showOpenMultipleDialog(stage);
        
        return selectedFiles != null ? selectedFiles : List.of();
    }
    
    /**
     * Affiche une boîte de dialogue pour sauvegarder un fichier
     */
    public static Optional<File> showSaveFileChooser(String title, String initialDirectory,
                                                    String initialFileName,
                                                    FileChooser.ExtensionFilter... filters) {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle(title);
        
        if (initialDirectory != null) {
            File dir = new File(initialDirectory);
            if (dir.exists() && dir.isDirectory()) {
                fileChooser.setInitialDirectory(dir);
            }
        }
        
        if (initialFileName != null && !initialFileName.trim().isEmpty()) {
            fileChooser.setInitialFileName(initialFileName);
        }
        
        if (filters != null && filters.length > 0) {
            fileChooser.getExtensionFilters().addAll(filters);
        }
        
        Stage stage = getCurrentStage();
        File selectedFile = fileChooser.showSaveDialog(stage);
        
        return Optional.ofNullable(selectedFile);
    }
    
    /**
     * Affiche une boîte de dialogue pour sélectionner un répertoire
     */
    public static Optional<File> showDirectoryChooser(String title, String initialDirectory) {
        DirectoryChooser directoryChooser = new DirectoryChooser();
        directoryChooser.setTitle(title);
        
        if (initialDirectory != null) {
            File dir = new File(initialDirectory);
            if (dir.exists() && dir.isDirectory()) {
                directoryChooser.setInitialDirectory(dir);
            }
        }
        
        Stage stage = getCurrentStage();
        File selectedDirectory = directoryChooser.showDialog(stage);
        
        return Optional.ofNullable(selectedDirectory);
    }
    
    /**
     * Affiche une erreur détaillée avec stacktrace
     */
    public static void showDetailedError(String title, String message, Exception exception) {
        Alert alert = createAlert(Alert.AlertType.ERROR, title, message);
        
        if (exception != null) {
            // Création d'un textarea pour afficher la stacktrace
            java.io.StringWriter sw = new java.io.StringWriter();
            java.io.PrintWriter pw = new java.io.PrintWriter(sw);
            exception.printStackTrace(pw);
            String exceptionText = sw.toString();
            
            javafx.scene.control.TextArea textArea = new javafx.scene.control.TextArea(exceptionText);
            textArea.setEditable(false);
            textArea.setWrapText(true);
            textArea.setMaxWidth(Double.MAX_VALUE);
            textArea.setMaxHeight(Double.MAX_VALUE);
            
            alert.getDialogPane().setExpandableContent(textArea);
        }
        
        alert.showAndWait();
    }
    
    /**
     * Crée une alerte de base avec style cohérent
     */
    private static Alert createAlert(Alert.AlertType type, String title, String message) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        
        // Configuration de l'icône de l'application
        Stage stage = (Stage) alert.getDialogPane().getScene().getWindow();
        configureStageIcon(stage);
        
        // Application du style CSS
        alert.getDialogPane().getStylesheets().add(
            AlertUtils.class.getResource("/application/styles/application.css").toExternalForm()
        );
        
        return alert;
    }
    
    /**
     * Configure une boîte de dialogue avec titre et texte d'en-tête
     */
    private static void configureDialog(javafx.scene.control.Dialog<?> dialog, String title, String headerText) {
        dialog.setTitle(title);
        dialog.setHeaderText(headerText);
        
        // Configuration de l'icône
        Stage stage = (Stage) dialog.getDialogPane().getScene().getWindow();
        configureStageIcon(stage);
        
        // Application du style CSS
        dialog.getDialogPane().getStylesheets().add(
            AlertUtils.class.getResource("/styles/application.css").toExternalForm()
        );
    }
    
    /**
     * Configure l'icône d'une fenêtre
     */
    private static void configureStageIcon(Stage stage) {
        try {
            Image icon = new Image(AlertUtils.class.getResourceAsStream(iconPath));
            stage.getIcons().add(icon);
        } catch (Exception e) {
            // Icône non trouvée, on continue sans
            System.out.println("Icône d'application non trouvée: " + iconPath);
        }
    }
    
    /**
     * Récupère la fenêtre principale courante
     */
    private static Stage getCurrentStage() {
        // Tentative de récupération de la fenêtre principale
        try {
            return (Stage) javafx.stage.Stage.getWindows().stream()
                .filter(window -> window instanceof Stage)
                .findFirst()
                .orElse(null);
        } catch (Exception e) {
            return null;
        }
    }
    
    /**
     * Définit le chemin de l'icône de l'application
     */
    public static void setIconPath(String path) {
        iconPath = path;
    }
    
    /**
     * Filtres de fichiers couramment utilisés
     */
    public static class FileFilters {
        public static final FileChooser.ExtensionFilter ALL_FILES = 
            new FileChooser.ExtensionFilter("Tous les fichiers", "*.*");
        
        public static final FileChooser.ExtensionFilter TEXT_FILES = 
            new FileChooser.ExtensionFilter("Fichiers texte", "*.txt");
        
        public static final FileChooser.ExtensionFilter PDF_FILES = 
            new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf");
        
        public static final FileChooser.ExtensionFilter DOC_FILES = 
            new FileChooser.ExtensionFilter("Documents Word", "*.doc", "*.docx");
        
        public static final FileChooser.ExtensionFilter EXCEL_FILES = 
            new FileChooser.ExtensionFilter("Fichiers Excel", "*.xls", "*.xlsx");
        
        public static final FileChooser.ExtensionFilter IMAGE_FILES = 
            new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp");
        
        public static final FileChooser.ExtensionFilter XML_FILES = 
            new FileChooser.ExtensionFilter("Fichiers XML", "*.xml");
        
        public static final FileChooser.ExtensionFilter JSON_FILES = 
            new FileChooser.ExtensionFilter("Fichiers JSON", "*.json");
        
        public static final FileChooser.ExtensionFilter CSV_FILES = 
            new FileChooser.ExtensionFilter("Fichiers CSV", "*.csv");
    }
}