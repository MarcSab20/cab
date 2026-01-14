package application.controllers;

import javafx.collections.FXCollections;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.VBox;
import javafx.stage.Modality;
import application.models.Document;
import application.models.Dossier;
import application.services.DossierService;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Dialogue pour déplacer un document vers un autre dossier
 */
public class DeplacerDocumentDialog extends Dialog<Dossier> {
    
    private final ComboBox<Dossier> comboDossierDestination;
    private final TreeView<Dossier> arborescenceDossiers;
    private final TextField champRecherche;
    private final RadioButton radioCombo;
    private final RadioButton radioArbre;
    
    private final Document document;
    private final DossierService dossierService;
    
    /**
     * Constructeur
     */
    public DeplacerDocumentDialog(Document document) {
        this.document = document;
        this.dossierService = DossierService.getInstance();
        
        // Configuration du dialogue
        setTitle("Déplacer le document");
        setHeaderText("Déplacer: " + document.getTitre());
        initModality(Modality.APPLICATION_MODAL);
        setWidth(500);
        
        // Boutons
        ButtonType btnDeplacer = new ButtonType("Déplacer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnAnnuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(btnDeplacer, btnAnnuler);
        
        // Création du formulaire
        VBox container = new VBox(15);
        container.setPadding(new Insets(20));
        
        // Information actuelle
        Label lblActuel = new Label("📍 Dossier actuel: " + 
            (document.getNomDossier() != null ? document.getIconeDossier() + " " + document.getNomDossier() : "Aucun"));
        lblActuel.setStyle("-fx-font-weight: bold; -fx-text-fill: #7f8c8d;");
        container.getChildren().add(lblActuel);
        
        Separator sep1 = new Separator();
        container.getChildren().add(sep1);
        
        // Mode de sélection
        Label lblMode = new Label("Choisir le mode de sélection:");
        lblMode.setStyle("-fx-font-weight: bold;");
        container.getChildren().add(lblMode);
        
        ToggleGroup modeGroup = new ToggleGroup();
        
        radioCombo = new RadioButton("Liste déroulante simple");
        radioCombo.setToggleGroup(modeGroup);
        radioCombo.setSelected(true);
        
        radioArbre = new RadioButton("Arborescence hiérarchique");
        radioArbre.setToggleGroup(modeGroup);
        
        VBox vboxRadios = new VBox(8);
        vboxRadios.getChildren().addAll(radioCombo, radioArbre);
        container.getChildren().add(vboxRadios);
        
        Separator sep2 = new Separator();
        container.getChildren().add(sep2);
        
        // Recherche rapide
        champRecherche = new TextField();
        champRecherche.setPromptText("🔍 Rechercher un dossier...");
        champRecherche.textProperty().addListener((obs, old, newVal) -> filtrerDossiers(newVal));
        container.getChildren().add(champRecherche);
        
        // ComboBox des dossiers
        comboDossierDestination = new ComboBox<>();
        comboDossierDestination.setPromptText("Sélectionner un dossier de destination");
        comboDossierDestination.setPrefWidth(450);
        chargerDossiers();
        
        comboDossierDestination.setCellFactory(param -> new ListCell<>() {
            @Override
            protected void updateItem(Dossier item, boolean empty) {
                super.updateItem(item, empty);
                if (empty || item == null) {
                    setText(null);
                } else {
                    String icone = item.getIcone();
                    if (icone == null || icone.equals("???") || icone.trim().isEmpty()) {
                        icone = "📁";
                    }
                    setText(icone + " " + item.getNomDossier() + " (" + item.getCodeDossier() + ")");
                }
            }
        });
        comboDossierDestination.setButtonCell(comboDossierDestination.getCellFactory().call(null));
        
        // Arborescence des dossiers
        arborescenceDossiers = new TreeView<>();
        arborescenceDossiers.setPrefHeight(300);
        arborescenceDossiers.setVisible(false);
        arborescenceDossiers.setManaged(false);
        construireArborescence();
        
        // Basculer entre combo et arbre
        radioCombo.setOnAction(e -> {
            comboDossierDestination.setVisible(true);
            comboDossierDestination.setManaged(true);
            arborescenceDossiers.setVisible(false);
            arborescenceDossiers.setManaged(false);
        });
        
        radioArbre.setOnAction(e -> {
            comboDossierDestination.setVisible(false);
            comboDossierDestination.setManaged(false);
            arborescenceDossiers.setVisible(true);
            arborescenceDossiers.setManaged(true);
        });
        
        container.getChildren().addAll(comboDossierDestination, arborescenceDossiers);
        
        // Note
        Label noteLabel = new Label("💡 Le document conservera son code et ses métadonnées");
        noteLabel.setStyle("-fx-text-fill: #3498db; -fx-font-size: 11px; -fx-font-style: italic;");
        container.getChildren().add(noteLabel);
        
        getDialogPane().setContent(container);
        
        // Validation
        Button btnValidate = (Button) getDialogPane().lookupButton(btnDeplacer);
        btnValidate.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!validateForm()) {
                event.consume();
            }
        });
        
        // Conversion du résultat
        setResultConverter(dialogButton -> {
            if (dialogButton == btnDeplacer) {
                return getDossierSelectionne();
            }
            return null;
        });
    }
    
    /**
     * Charge la liste des dossiers
     */
    private void chargerDossiers() {
        try {
            List<Dossier> dossiers = dossierService.getAllDossiers();
            
            // Filtrer les dossiers système non pertinents et le dossier actuel
            dossiers = dossiers.stream()
                .filter(d -> !d.getCodeDossier().equals("ROOT") && 
                           !d.getCodeDossier().equals("CORBEILLE") &&
                           (document.getDossierId() == null || d.getId() != document.getDossierId()))
                .collect(Collectors.toList());
            
            comboDossierDestination.setItems(FXCollections.observableArrayList(dossiers));
            
        } catch (Exception e) {
            System.err.println("Erreur chargement dossiers: " + e.getMessage());
        }
    }
    
    /**
     * Construit l'arborescence des dossiers
     */
    private void construireArborescence() {
        try {
            List<Dossier> dossiers = dossierService.getAllDossiers();
            Dossier racine = new Dossier();
            racine.setId(0);
            racine.setNomDossier("Tous les dossiers");
            racine.setIcone("🏢");
            
            TreeItem<Dossier> rootItem = new TreeItem<>(racine);
            rootItem.setExpanded(true);
            
            construireArborescenceRecursive(rootItem, null, dossiers);
            arborescenceDossiers.setRoot(rootItem);
            
            arborescenceDossiers.setCellFactory(tv -> new TreeCell<Dossier>() {
                @Override
                protected void updateItem(Dossier item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty || item == null) {
                        setText(null);
                    } else {
                        String icone = item.getIcone();
                        if (icone == null || icone.equals("???") || icone.trim().isEmpty()) {
                            icone = "📁";
                        }
                        setText(icone + " " + item.getNomDossier());
                    }
                }
            });
            
        } catch (Exception e) {
            System.err.println("Erreur construction arborescence: " + e.getMessage());
        }
    }
    
    private void construireArborescenceRecursive(TreeItem<Dossier> parent, Integer parentId, List<Dossier> dossiers) {
        for (Dossier d : dossiers) {
            if (d.getCodeDossier().equals("ROOT") || d.getCodeDossier().equals("CORBEILLE")) {
                continue;
            }
            
            if (document.getDossierId() != null && d.getId() == document.getDossierId()) {
                continue;
            }
            
            boolean estEnfant = (parentId == null && d.getDossierParentId() == null) ||
                               (parentId != null && d.getDossierParentId() != null && d.getDossierParentId().equals(parentId));
            if (estEnfant) {
                TreeItem<Dossier> item = new TreeItem<>(d);
                parent.getChildren().add(item);
                construireArborescenceRecursive(item, d.getId(), dossiers);
            }
        }
    }
    
    /**
     * Filtre les dossiers selon la recherche
     */
    private void filtrerDossiers(String recherche) {
        if (recherche == null || recherche.trim().isEmpty()) {
            chargerDossiers();
            return;
        }
        
        try {
            List<Dossier> dossiers = dossierService.rechercherDossiers(recherche);
            
            dossiers = dossiers.stream()
                .filter(d -> !d.getCodeDossier().equals("ROOT") && 
                           !d.getCodeDossier().equals("CORBEILLE") &&
                           (document.getDossierId() == null || d.getId() != document.getDossierId()))
                .collect(Collectors.toList());
            
            comboDossierDestination.setItems(FXCollections.observableArrayList(dossiers));
            
        } catch (Exception e) {
            System.err.println("Erreur recherche: " + e.getMessage());
        }
    }
    
    /**
     * Retourne le dossier sélectionné
     */
    private Dossier getDossierSelectionne() {
        if (radioCombo.isSelected()) {
            return comboDossierDestination.getValue();
        } else {
            TreeItem<Dossier> item = arborescenceDossiers.getSelectionModel().getSelectedItem();
            return item != null && item.getValue().getId() > 0 ? item.getValue() : null;
        }
    }
    
    /**
     * Valide le formulaire
     */
    private boolean validateForm() {
        Dossier dossierSelectionne = getDossierSelectionne();
        
        if (dossierSelectionne == null) {
            showError("Veuillez sélectionner un dossier de destination");
            return false;
        }
        
        if (document.getDossierId() != null && dossierSelectionne.getId() == document.getDossierId()) {
            showError("Le document est déjà dans ce dossier");
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
}