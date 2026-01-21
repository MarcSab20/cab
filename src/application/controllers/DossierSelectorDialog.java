package application.controllers;

import application.models.Dossier;
import application.services.DossierService;
import application.utils.IconeUtils;

import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.geometry.Insets;

import java.util.List;

/**
 * Dialogue pour sélectionner un dossier d'archivage
 */
public class DossierSelectorDialog extends Dialog<Integer> {
    
    private TreeView<Dossier> arborescenceDossiers;
    private DossierService dossierService;
    
    public DossierSelectorDialog(String title) {
        this.dossierService = DossierService.getInstance();
        
        setTitle(title != null ? title : "Sélectionner un dossier");
        setHeaderText("Choisissez le dossier de destination");
        
        // Boutons
        ButtonType btnSelectionner = new ButtonType("Sélectionner", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnAnnuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(btnSelectionner, btnAnnuler);
        
        // Contenu
        VBox content = new VBox(15);
        content.setPadding(new Insets(20));
        content.setPrefSize(500, 400);
        
        Label instruction = new Label("Sélectionnez un dossier dans l'arborescence:");
        instruction.setStyle("-fx-font-weight: bold;");
        
        // Arborescence
        arborescenceDossiers = new TreeView<>();
        VBox.setVgrow(arborescenceDossiers, javafx.scene.layout.Priority.ALWAYS);
        
        chargerArborescence();
        
        content.getChildren().addAll(instruction, arborescenceDossiers);
        getDialogPane().setContent(content);
        
        // Validation
        Button btnSelectionnerNode = (Button) getDialogPane().lookupButton(btnSelectionner);
        btnSelectionnerNode.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!validerSelection()) {
                event.consume();
            }
        });
        
        // Convertisseur résultat
        setResultConverter(dialogButton -> {
            if (dialogButton == btnSelectionner) {
                TreeItem<Dossier> selectedItem = arborescenceDossiers.getSelectionModel().getSelectedItem();
                if (selectedItem != null && selectedItem.getValue() != null) {
                    return selectedItem.getValue().getId();
                }
            }
            return null;
        });
    }
    
    /**
     * Charge l'arborescence des dossiers
     */
    private void chargerArborescence() {
        try {
            List<Dossier> dossiers = dossierService.getAllDossiers();
            
            // Racine
            Dossier racine = new Dossier();
            racine.setId(0);
            racine.setNomDossier("📁 Racine");
            racine.setIcone("📁");
            
            TreeItem<Dossier> rootItem = new TreeItem<>(racine);
            rootItem.setExpanded(true);
            
            construireArborescenceRecursive(rootItem, null, dossiers);
            arborescenceDossiers.setRoot(rootItem);
            
            // Affichage personnalisé
            arborescenceDossiers.setCellFactory(tv -> new TreeCell<Dossier>() {
                @Override
                protected void updateItem(Dossier item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                        setGraphic(null);
                    } else {
                        setText(IconeUtils.formatterNomDossier(item));
                    }
                }
            });
            
        } catch (Exception e) {
            System.err.println("Erreur chargement arborescence: " + e.getMessage());
            showAlert("Erreur lors du chargement de l'arborescence");
        }
    }
    
    /**
     * Construit l'arborescence récursivement
     */
    private void construireArborescenceRecursive(TreeItem<Dossier> parent, Integer parentId, List<Dossier> dossiers) {
        for (Dossier d : dossiers) {
            boolean estEnfant = (parentId == null && d.getDossierParentId() == null) ||
                               (parentId != null && d.getDossierParentId() != null && 
                                d.getDossierParentId().equals(parentId));
            
            if (estEnfant) {
                TreeItem<Dossier> item = new TreeItem<>(d);
                parent.getChildren().add(item);
                construireArborescenceRecursive(item, d.getId(), dossiers);
            }
        }
    }
    
    /**
     * Valide la sélection
     */
    private boolean validerSelection() {
        TreeItem<Dossier> selectedItem = arborescenceDossiers.getSelectionModel().getSelectedItem();
        
        if (selectedItem == null || selectedItem.getValue() == null) {
            showAlert("Veuillez sélectionner un dossier");
            return false;
        }
        
        Dossier dossier = selectedItem.getValue();
        
        if (dossier.getId() == 0) {
            showAlert("Veuillez sélectionner un dossier spécifique (pas la racine)");
            return false;
        }
        
        return true;
    }
    
    /**
     * Affiche une alerte
     */
    private void showAlert(String message) {
        Alert alert = new Alert(Alert.AlertType.WARNING);
        alert.setTitle("Validation");
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}