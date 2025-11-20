package application.controllers;

import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.stage.Modality;
import application.models.Role;

/**
 * Dialogue pour créer ou modifier un rôle
 */
public class RoleFormDialog extends Dialog<Role> {
    
    private final TextField champNom;
    private final TextArea textAreaDescription;
    private final CheckBox checkActif;
    
    private final Role role;
    private final boolean isNewRole;
    
    /**
     * Constructeur
     * @param role Rôle à modifier (null pour créer un nouveau rôle)
     */
    public RoleFormDialog(Role role) {
        this.role = role;
        this.isNewRole = (role == null);
        
        // Configuration du dialogue
        setTitle(isNewRole ? "Nouveau rôle" : "Modifier le rôle");
        setHeaderText(isNewRole ? "Créer un nouveau rôle" : "Modifier les informations du rôle");
        initModality(Modality.APPLICATION_MODAL);
        
        // Boutons
        ButtonType btnValider = new ButtonType(isNewRole ? "Créer" : "Modifier", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnAnnuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(btnValider, btnAnnuler);
        
        // Création du formulaire
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);
        grid.setPadding(new Insets(20, 150, 10, 10));
        
        // Champs
        champNom = new TextField();
        champNom.setPromptText("Nom du rôle");
        
        textAreaDescription = new TextArea();
        textAreaDescription.setPromptText("Description du rôle et de ses responsabilités");
        textAreaDescription.setPrefRowCount(3);
        textAreaDescription.setWrapText(true);
        
        checkActif = new CheckBox("Rôle actif");
        checkActif.setSelected(true);
        
        // Ajout des champs au grid
        int row = 0;
        grid.add(new Label("Nom du rôle:"), 0, row);
        grid.add(champNom, 1, row++);
        
        grid.add(new Label("Description:"), 0, row);
        grid.add(textAreaDescription, 1, row++);
        
        grid.add(new Label(""), 0, row);
        grid.add(checkActif, 1, row++);
        
        grid.add(new Label(""), 0, row);
        Label noteLabel = new Label("Note: Les permissions seront configurées après la création du rôle");
        noteLabel.setStyle("-fx-text-fill: gray; -fx-font-size: 11px;");
        grid.add(noteLabel, 1, row++);
        
        getDialogPane().setContent(grid);
        
        // Si modification, remplir les champs
        if (!isNewRole) {
            champNom.setText(role.getNom());
            textAreaDescription.setText(role.getDescription());
            checkActif.setSelected(role.isActif());
        }
        
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
                return createRoleFromForm();
            }
            return null;
        });
    }
    
    private boolean validateForm() {
        // Validation du nom
        if (champNom.getText().trim().isEmpty()) {
            showError("Le nom du rôle est obligatoire");
            return false;
        }
        
        // Validation de la longueur du nom
        if (champNom.getText().trim().length() < 3) {
            showError("Le nom du rôle doit contenir au moins 3 caractères");
            return false;
        }
        
        // Validation de la description
        if (textAreaDescription.getText().trim().isEmpty()) {
            showError("La description du rôle est obligatoire");
            return false;
        }
        
        return true;
    }
    
    private void showError(String message) {
        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.initModality(Modality.APPLICATION_MODAL);
        alert.setTitle("Erreur de validation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
    
    private Role createRoleFromForm() {
        Role newRole;
        
        if (isNewRole) {
            newRole = new Role();
        } else {
            newRole = role;
        }
        
        newRole.setNom(champNom.getText().trim());
        newRole.setDescription(textAreaDescription.getText().trim());
        newRole.setActif(checkActif.isSelected());
        
        return newRole;
    }
}