package application.controllers;

import application.models.*;
import application.services.ExpediteurDestinataireService;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.scene.layout.Priority;
import javafx.util.StringConverter;
import org.controlsfx.control.CheckComboBox;

import java.time.LocalDate;
import java.util.*;
import java.util.stream.Collectors;

/**
 * Dialogue amélioré de création/édition de courrier
 * VERSION CORRIGÉE avec ScrollPane pour éviter le dépassement d'écran
 */
public class CourrierFormDialog extends Dialog<Courrier> {
    
    // Champs du formulaire
    private TextField txtObjet;
    private ComboBox<Courrier.TypeCourrier> cmbType;
    private DatePicker dpDateCourrier;
    private ComboBox<Courrier.PrioriteCourrier> cmbPriorite;
    private TextArea txtObservations;
    private CheckBox chkConfidentiel;
    private TextField txtReference;
    
    // Nouveaux champs pour expéditeur
    private ComboBox<Object> cmbCategorieExpediteur;
    private ComboBox<SousCategorieExpediteur> cmbSousCategorieExpediteur;
    private TextField txtExpediteurAutre;
    private Label lblExpediteurSelectionne;
    
    // Nouveaux champs pour destinataires (choix multiples)
    private CheckComboBox<DestinataireStandard> checkCmbDestinataires;
    private TextField txtDestinatairesAutres;
    private Label lblDestinatairesSelectionnes;
    
    private Document document;
    private ExpediteurDestinataireService expedDestService;
    
    // Cache des données
    private Map<CategorieExpediteur, List<SousCategorieExpediteur>> categoriesMap;
    private String expediteurFinal = "";
    private List<String> destinatairesFinals = new ArrayList<>();
    
    public CourrierFormDialog(Document document) {
        this.document = document;
        this.expedDestService = ExpediteurDestinataireService.getInstance();
        
        setTitle("Créer un courrier");
        setHeaderText("📄 Document: " + document.getTitre());
        
        // Boutons
        ButtonType btnCreer = new ButtonType("Créer", ButtonBar.ButtonData.OK_DONE);
        ButtonType btnAnnuler = new ButtonType("Annuler", ButtonBar.ButtonData.CANCEL_CLOSE);
        getDialogPane().getButtonTypes().addAll(btnCreer, btnAnnuler);
        
        // Charger les données
        chargerDonnees();
        
        // Construire le formulaire avec ScrollPane
        ScrollPane scrollPane = construireFormulaireAvecScroll();
        getDialogPane().setContent(scrollPane);
        
        // Validation
        Button btnCreerNode = (Button) getDialogPane().lookupButton(btnCreer);
        btnCreerNode.addEventFilter(javafx.event.ActionEvent.ACTION, event -> {
            if (!validerFormulaire()) {
                event.consume();
            }
        });
        
        // Convertisseur résultat
        setResultConverter(dialogButton -> {
            if (dialogButton == btnCreer) {
                return creerCourrierFromForm();
            }
            return null;
        });
        
        // CORRECTION : Taille optimale pour éviter le dépassement d'écran
        getDialogPane().setPrefWidth(750);
        getDialogPane().setPrefHeight(650); // Hauteur fixe raisonnable
        getDialogPane().setMinWidth(700);
        getDialogPane().setMinHeight(600);
        getDialogPane().setMaxHeight(700); // Limite maximale
    }
    
    /**
     * Charge les données depuis la base de données
     */
    private void chargerDonnees() {
        categoriesMap = expedDestService.getCategoriesAvecSousCategories();
    }
    
    /**
     * Construit le formulaire avec ScrollPane
     * CORRECTION : Ajout du ScrollPane pour permettre le défilement
     */
    private ScrollPane construireFormulaireAvecScroll() {
        VBox mainLayout = construireFormulaire();
        
        // Créer le ScrollPane
        ScrollPane scrollPane = new ScrollPane(mainLayout);
        scrollPane.setFitToWidth(true); // Le contenu s'adapte à la largeur
        scrollPane.setFitToHeight(false); // Permet le défilement vertical
        scrollPane.setPannable(true); // Permet le défilement avec la souris
        scrollPane.setVbarPolicy(ScrollPane.ScrollBarPolicy.AS_NEEDED); // Barre verticale si nécessaire
        scrollPane.setHbarPolicy(ScrollPane.ScrollBarPolicy.NEVER); // Pas de barre horizontale
        
        // Style pour améliorer l'apparence
        scrollPane.setStyle("-fx-background-color: transparent;");
        mainLayout.setStyle("-fx-background-color: white;");
        
        return scrollPane;
    }
    
    /**
     * Construit le formulaire complet
     */
    private VBox construireFormulaire() {
        VBox layout = new VBox(12); // Espacement réduit de 15 à 12
        layout.setPadding(new Insets(15)); // Padding réduit de 20 à 15
        
        // Section 1: Informations générales
        TitledPane sectionGenerale = new TitledPane("Informations générales", 
            construireSectionGenerale());
        sectionGenerale.setCollapsible(false);
        
        // Section 2: Expéditeur
        TitledPane sectionExpediteur = new TitledPane("Expéditeur", 
            construireSectionExpediteur());
        sectionExpediteur.setCollapsible(false);
        
        // Section 3: Destinataires
        TitledPane sectionDestinataires = new TitledPane("Destinataire(s)", 
            construireSectionDestinataires());
        sectionDestinataires.setCollapsible(false);
        
        // Section 4: Détails complémentaires
        TitledPane sectionDetails = new TitledPane("Détails complémentaires", 
            construireSectionDetails());
        sectionDetails.setCollapsible(false);
        
        layout.getChildren().addAll(
            sectionGenerale, 
            sectionExpediteur, 
            sectionDestinataires, 
            sectionDetails
        );
        
        return layout;
    }
    
    /**
     * Construit la section informations générales
     * OPTIMISÉ : Espacement réduit
     */
    private GridPane construireSectionGenerale() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8); // Réduit de 10 à 8
        grid.setPadding(new Insets(8)); // Réduit de 10 à 8
        
        // Objet
        txtObjet = new TextField();
        txtObjet.setPromptText("Objet du courrier *");
        txtObjet.setPrefWidth(500);
        grid.add(new Label("Objet *:"), 0, 0);
        grid.add(txtObjet, 1, 0);
        GridPane.setHgrow(txtObjet, Priority.ALWAYS);
        
        // Type
        cmbType = new ComboBox<>();
        cmbType.getItems().addAll(Courrier.TypeCourrier.values());
        cmbType.setValue(Courrier.TypeCourrier.ENTRANT);
        cmbType.setPrefWidth(200);
        grid.add(new Label("Type *:"), 0, 1);
        grid.add(cmbType, 1, 1);
        
        // Date
        dpDateCourrier = new DatePicker();
        dpDateCourrier.setValue(LocalDate.now());
        dpDateCourrier.setPrefWidth(200);
        grid.add(new Label("Date:"), 0, 2);
        grid.add(dpDateCourrier, 1, 2);
        
        // Priorité
        cmbPriorite = new ComboBox<>();
        cmbPriorite.getItems().addAll(Courrier.PrioriteCourrier.values());
        cmbPriorite.setValue(Courrier.PrioriteCourrier.NORMALE);
        cmbPriorite.setPrefWidth(200);
        grid.add(new Label("Priorité:"), 0, 3);
        grid.add(cmbPriorite, 1, 3);
        
        // Référence
        txtReference = new TextField();
        txtReference.setPromptText("Référence du courrier");
        grid.add(new Label("Référence:"), 0, 4);
        grid.add(txtReference, 1, 4);
        GridPane.setHgrow(txtReference, Priority.ALWAYS);
        
        return grid;
    }
    
    /**
     * Construit la section expéditeur avec hiérarchie
     * OPTIMISÉ : Espacement réduit
     */
    private VBox construireSectionExpediteur() {
        VBox layout = new VBox(8); // Réduit de 10 à 8
        layout.setPadding(new Insets(8)); // Réduit de 10 à 8
        
        // ComboBox catégorie principale
        Label lblCategorie = new Label("Catégorie:");
        cmbCategorieExpediteur = new ComboBox<>();
        cmbCategorieExpediteur.setPromptText("Sélectionner une catégorie...");
        cmbCategorieExpediteur.setPrefWidth(400);
        cmbCategorieExpediteur.setMaxWidth(Double.MAX_VALUE);
        
        // Ajouter les catégories
        List<Object> items = new ArrayList<>();
        items.addAll(categoriesMap.keySet());
        items.add("── Autre (saisie manuelle) ──");
        cmbCategorieExpediteur.getItems().addAll(items);
        
        // ComboBox sous-catégorie (initialement cachée)
        Label lblSousCategorie = new Label("Sous-catégorie:");
        cmbSousCategorieExpediteur = new ComboBox<>();
        cmbSousCategorieExpediteur.setPromptText("Sélectionner...");
        cmbSousCategorieExpediteur.setPrefWidth(400);
        cmbSousCategorieExpediteur.setMaxWidth(Double.MAX_VALUE);
        cmbSousCategorieExpediteur.setVisible(false);
        cmbSousCategorieExpediteur.setManaged(false);
        
        // Champ texte pour "Autre"
        txtExpediteurAutre = new TextField();
        txtExpediteurAutre.setPromptText("Saisir l'expéditeur...");
        txtExpediteurAutre.setPrefWidth(400);
        txtExpediteurAutre.setMaxWidth(Double.MAX_VALUE);
        txtExpediteurAutre.setVisible(false);
        txtExpediteurAutre.setManaged(false);
        
        // Label récapitulatif
        lblExpediteurSelectionne = new Label();
        lblExpediteurSelectionne.setStyle("-fx-font-weight: bold; -fx-text-fill: #27ae60;");
        lblExpediteurSelectionne.setWrapText(true);
        
        // Événement changement de catégorie
        cmbCategorieExpediteur.setOnAction(e -> {
            Object selected = cmbCategorieExpediteur.getValue();
            
            if (selected == null) {
                return;
            }
            
            if (selected instanceof String) {
                // Option "Autre" sélectionnée
                cmbSousCategorieExpediteur.setVisible(false);
                cmbSousCategorieExpediteur.setManaged(false);
                txtExpediteurAutre.setVisible(true);
                txtExpediteurAutre.setManaged(true);
                lblExpediteurSelectionne.setText("");
                
            } else if (selected instanceof CategorieExpediteur) {
                CategorieExpediteur cat = (CategorieExpediteur) selected;
                
                txtExpediteurAutre.setVisible(false);
                txtExpediteurAutre.setManaged(false);
                
                if (cat.isASousCategories()) {
                    // Afficher les sous-catégories
                    List<SousCategorieExpediteur> sousCategories = categoriesMap.get(cat);
                    cmbSousCategorieExpediteur.getItems().clear();
                    cmbSousCategorieExpediteur.getItems().addAll(sousCategories);
                    cmbSousCategorieExpediteur.setVisible(true);
                    cmbSousCategorieExpediteur.setManaged(true);
                    lblExpediteurSelectionne.setText("Catégorie: " + cat.getLibelle() + 
                                                    " → Sélectionner une sous-catégorie");
                } else {
                    // Pas de sous-catégorie, sélection directe
                    cmbSousCategorieExpediteur.setVisible(false);
                    cmbSousCategorieExpediteur.setManaged(false);
                    expediteurFinal = cat.getLibelle();
                    lblExpediteurSelectionne.setText("✓ Expéditeur: " + expediteurFinal);
                }
            }
        });
        
        // Événement changement de sous-catégorie
        cmbSousCategorieExpediteur.setOnAction(e -> {
            SousCategorieExpediteur sousCat = cmbSousCategorieExpediteur.getValue();
            if (sousCat != null) {
                expediteurFinal = sousCat.getLibelle();
                lblExpediteurSelectionne.setText("✓ Expéditeur: " + expediteurFinal);
            }
        });
        
        // Événement saisie manuelle
        txtExpediteurAutre.textProperty().addListener((obs, oldVal, newVal) -> {
            expediteurFinal = newVal != null ? newVal.trim() : "";
            if (!expediteurFinal.isEmpty()) {
                lblExpediteurSelectionne.setText("✓ Expéditeur: " + expediteurFinal);
            } else {
                lblExpediteurSelectionne.setText("");
            }
        });
        
        Separator separator = new Separator();
        separator.setPadding(new Insets(5, 0, 5, 0));
        
        layout.getChildren().addAll(
            lblCategorie,
            cmbCategorieExpediteur,
            lblSousCategorie,
            cmbSousCategorieExpediteur,
            txtExpediteurAutre,
            separator,
            lblExpediteurSelectionne
        );
        
        return layout;
    }
    
    /**
     * Construit la section destinataires avec choix multiples
     * OPTIMISÉ : Espacement réduit
     */
    private VBox construireSectionDestinataires() {
        VBox layout = new VBox(8); // Réduit de 10 à 8
        layout.setPadding(new Insets(8)); // Réduit de 10 à 8
        
        // CheckComboBox pour destinataires standards (choix multiples)
        Label lblDestinataires = new Label("Destinataires standards:");
        
        checkCmbDestinataires = new CheckComboBox<>();
        checkCmbDestinataires.setMaxWidth(Double.MAX_VALUE);
        
        // Charger les destinataires
        List<DestinataireStandard> destinataires = expedDestService.getDestinatairesStandards();
        checkCmbDestinataires.getItems().addAll(destinataires);
        
        // Convertisseur pour affichage
        checkCmbDestinataires.setConverter(new StringConverter<DestinataireStandard>() {
            @Override
            public String toString(DestinataireStandard dest) {
                return dest != null ? dest.getLibelle() : "";
            }
            
            @Override
            public DestinataireStandard fromString(String string) {
                return null;
            }
        });
        
        // Champ pour destinataires personnalisés
        Label lblAutres = new Label("Autres destinataires (séparés par des virgules):");
        txtDestinatairesAutres = new TextField();
        txtDestinatairesAutres.setPromptText("Ex: Service XYZ, Direction ABC...");
        txtDestinatairesAutres.setMaxWidth(Double.MAX_VALUE);
        
        // Label récapitulatif
        lblDestinatairesSelectionnes = new Label();
        lblDestinatairesSelectionnes.setStyle("-fx-font-weight: bold; -fx-text-fill: #27ae60;");
        lblDestinatairesSelectionnes.setWrapText(true);
        
        // Événement changement destinataires
        checkCmbDestinataires.getCheckModel().getCheckedItems().addListener(
            (javafx.collections.ListChangeListener.Change<? extends DestinataireStandard> change) -> {
                mettreAJourRecapDestinataires();
            }
        );
        
        txtDestinatairesAutres.textProperty().addListener((obs, oldVal, newVal) -> {
            mettreAJourRecapDestinataires();
        });
        
        Separator separator = new Separator();
        separator.setPadding(new Insets(5, 0, 5, 0));
        
        layout.getChildren().addAll(
            lblDestinataires,
            checkCmbDestinataires,
            new Separator(),
            lblAutres,
            txtDestinatairesAutres,
            separator,
            lblDestinatairesSelectionnes
        );
        
        return layout;
    }
    
    /**
     * Met à jour le récapitulatif des destinataires sélectionnés
     */
    private void mettreAJourRecapDestinataires() {
        destinatairesFinals.clear();
        
        // Ajouter les destinataires standards cochés
        for (DestinataireStandard dest : checkCmbDestinataires.getCheckModel().getCheckedItems()) {
            destinatairesFinals.add(dest.getLibelle());
        }
        
        // Ajouter les destinataires personnalisés
        String autres = txtDestinatairesAutres.getText();
        if (autres != null && !autres.trim().isEmpty()) {
            String[] autresArray = autres.split(",");
            for (String autre : autresArray) {
                String trimmed = autre.trim();
                if (!trimmed.isEmpty() && !destinatairesFinals.contains(trimmed)) {
                    destinatairesFinals.add(trimmed);
                }
            }
        }
        
        // Afficher le récapitulatif
        if (destinatairesFinals.isEmpty()) {
            lblDestinatairesSelectionnes.setText("");
        } else {
            String recap = "✓ Destinataire(s) sélectionné(s) (" + destinatairesFinals.size() + "): " +
                          String.join(", ", destinatairesFinals);
            lblDestinatairesSelectionnes.setText(recap);
        }
    }
    
    /**
     * Construit la section détails complémentaires
     * OPTIMISÉ : TextArea plus compacte
     */
    private GridPane construireSectionDetails() {
        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(8); // Réduit de 10 à 8
        grid.setPadding(new Insets(8)); // Réduit de 10 à 8
        
        // Observations - TextArea plus compacte
        txtObservations = new TextArea();
        txtObservations.setPromptText("Observations ou instructions...");
        txtObservations.setPrefRowCount(2); // Réduit de 3 à 2
        txtObservations.setWrapText(true);
        txtObservations.setMaxWidth(Double.MAX_VALUE);
        grid.add(new Label("Observations:"), 0, 0);
        grid.add(txtObservations, 1, 0);
        GridPane.setHgrow(txtObservations, Priority.ALWAYS);
        
        // Confidentiel
        chkConfidentiel = new CheckBox("Courrier confidentiel");
        grid.add(chkConfidentiel, 1, 1);
        
        return grid;
    }
    
    /**
     * Valide le formulaire
     */
    private boolean validerFormulaire() {
        // Vérifier l'objet
        if (txtObjet.getText() == null || txtObjet.getText().trim().isEmpty()) {
            showAlert("L'objet du courrier est obligatoire");
            return false;
        }
        
        // Vérifier le type
        if (cmbType.getValue() == null) {
            showAlert("Le type de courrier est obligatoire");
            return false;
        }
        
        // Vérifier l'expéditeur
        if (expediteurFinal == null || expediteurFinal.trim().isEmpty()) {
            showAlert("Veuillez sélectionner un expéditeur");
            return false;
        }
        
        // Vérifier les destinataires
        if (destinatairesFinals.isEmpty()) {
            showAlert("Veuillez sélectionner au moins un destinataire");
            return false;
        }
        
        return true;
    }
    
    /**
     * Crée l'objet Courrier à partir du formulaire
     */
    private Courrier creerCourrierFromForm() {
        Courrier courrier = new Courrier();
        
        courrier.setDocumentId(document.getId());
        courrier.setObjet(txtObjet.getText().trim());
        courrier.setTypeCourrier(cmbType.getValue());
        courrier.setExpediteur(expediteurFinal);
        courrier.setDestinataire(String.join(", ", destinatairesFinals));
        courrier.setReference(txtReference.getText());
        courrier.setDateCourrier(dpDateCourrier.getValue());
        courrier.setPriorite(cmbPriorite.getValue());
        courrier.setObservations(txtObservations.getText());
        courrier.setConfidentiel(chkConfidentiel.isSelected());
        
        // Mettre à jour les statistiques d'utilisation
        mettreAJourStatistiques();
        
        return courrier;
    }
    
    /**
     * Met à jour les statistiques d'utilisation des expéditeurs/destinataires
     */
    private void mettreAJourStatistiques() {
        // Mettre à jour l'expéditeur personnalisé si nécessaire
        Object selectedCat = cmbCategorieExpediteur.getValue();
        if (selectedCat instanceof String) {
            // C'est un expéditeur personnalisé
            expedDestService.ajouterOuMettreAJourExpediteur(expediteurFinal);
        }
        
        // Mettre à jour les destinataires
        for (DestinataireStandard dest : checkCmbDestinataires.getCheckModel().getCheckedItems()) {
            expedDestService.incrementerUtilisationDestinataire(dest.getCode());
        }
        
        // Mettre à jour les destinataires personnalisés
        String autres = txtDestinatairesAutres.getText();
        if (autres != null && !autres.trim().isEmpty()) {
            String[] autresArray = autres.split(",");
            for (String autre : autresArray) {
                String trimmed = autre.trim();
                if (!trimmed.isEmpty()) {
                    expedDestService.ajouterOuMettreAJourDestinataire(trimmed);
                }
            }
        }
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