package application.controllers;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.layout.*;
import javafx.geometry.Insets;
import javafx.geometry.Pos;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import application.models.User;
import application.models.UserPresence;
import application.services.MessageSyncService;

import java.net.URL;
import java.util.*;

/**
 * Composant de sélection multiple de destinataires
 */
public class MultipleRecipientsSelector {
    
    private ObservableList<User> availableUsers;
    private ObservableList<User> selectedUsers;
    private MessageSyncService messageSyncService;
    
    // Composants UI
    private VBox mainContainer;
    private TextField searchField;
    private ListView<User> availableListView;
    private FlowPane selectedFlowPane;
    
    public MultipleRecipientsSelector(List<User> users, MessageSyncService syncService) {
        this.availableUsers = FXCollections.observableArrayList(users);
        this.selectedUsers = FXCollections.observableArrayList();
        this.messageSyncService = syncService;
        
        createUI();
    }
    
    private void createUI() {
        mainContainer = new VBox(10);
        mainContainer.setPadding(new Insets(10));
        
        // Champ de recherche
        searchField = new TextField();
        searchField.setPromptText("🔍 Rechercher un destinataire...");
        searchField.textProperty().addListener((obs, oldVal, newVal) -> filterUsers(newVal));
        
        // Zone des destinataires sélectionnés
        Label selectedLabel = new Label("Destinataires sélectionnés:");
        selectedLabel.setStyle("-fx-font-weight: bold;");
        
        selectedFlowPane = new FlowPane(8, 8);
        selectedFlowPane.setPadding(new Insets(10));
        selectedFlowPane.setStyle("-fx-background-color: #f0f0f0; -fx-background-radius: 5;");
        selectedFlowPane.setMinHeight(60);
        
        ScrollPane selectedScrollPane = new ScrollPane(selectedFlowPane);
        selectedScrollPane.setFitToWidth(true);
        selectedScrollPane.setMaxHeight(120);
        selectedScrollPane.setStyle("-fx-background-color: transparent;");
        
        // Liste des utilisateurs disponibles
        Label availableLabel = new Label("Utilisateurs disponibles:");
        availableLabel.setStyle("-fx-font-weight: bold;");
        
        availableListView = new ListView<>();
        availableListView.setItems(availableUsers);
        availableListView.setPrefHeight(300);
        availableListView.setCellFactory(param -> new UserListCell());
        
        // Événement de double-clic pour ajouter
        availableListView.setOnMouseClicked(event -> {
            if (event.getClickCount() == 2) {
                User selected = availableListView.getSelectionModel().getSelectedItem();
                if (selected != null) {
                    addRecipient(selected);
                }
            }
        });
        
        // Boutons d'action
        HBox buttonsBox = new HBox(10);
        buttonsBox.setAlignment(Pos.CENTER);
        
        Button addButton = new Button("➕ Ajouter");
        addButton.setOnAction(e -> {
            User selected = availableListView.getSelectionModel().getSelectedItem();
            if (selected != null) {
                addRecipient(selected);
            }
        });
        
        Button removeAllButton = new Button("🗑️ Tout retirer");
        removeAllButton.setOnAction(e -> clearAllRecipients());
        
        buttonsBox.getChildren().addAll(addButton, removeAllButton);
        
        // Assemblage
        mainContainer.getChildren().addAll(
            searchField,
            new Separator(),
            selectedLabel,
            selectedScrollPane,
            new Separator(),
            availableLabel,
            availableListView,
            buttonsBox
        );
    }
    
    /**
     * Ajoute un destinataire
     */
    private void addRecipient(User user) {
        if (!selectedUsers.contains(user)) {
            selectedUsers.add(user);
            updateSelectedDisplay();
        }
    }
    
    /**
     * Retire un destinataire
     */
    private void removeRecipient(User user) {
        selectedUsers.remove(user);
        updateSelectedDisplay();
    }
    
    /**
     * Retire tous les destinataires
     */
    private void clearAllRecipients() {
        selectedUsers.clear();
        updateSelectedDisplay();
    }
    
    /**
     * Met à jour l'affichage des destinataires sélectionnés
     */
    private void updateSelectedDisplay() {
        selectedFlowPane.getChildren().clear();
        
        if (selectedUsers.isEmpty()) {
            Label emptyLabel = new Label("Aucun destinataire sélectionné");
            emptyLabel.setStyle("-fx-text-fill: #999;");
            selectedFlowPane.getChildren().add(emptyLabel);
        } else {
            for (User user : selectedUsers) {
                HBox chip = createUserChip(user);
                selectedFlowPane.getChildren().add(chip);
            }
        }
    }
    
    /**
     * Crée une "puce" pour un utilisateur sélectionné
     */
    private HBox createUserChip(User user) {
        HBox chip = new HBox(5);
        chip.setAlignment(Pos.CENTER_LEFT);
        chip.setPadding(new Insets(5, 10, 5, 10));
        chip.setStyle("-fx-background-color: #3498db; -fx-background-radius: 15; -fx-cursor: hand;");
        
        // Icône de présence
        UserPresence presence = messageSyncService.getUserPresence(user.getId());
        String statusIcon = presence != null ? presence.getStatut().getIcone() : "⚫";
        
        Label nameLabel = new Label(statusIcon + " " + user.getNomComplet());
        nameLabel.setStyle("-fx-text-fill: white; -fx-font-size: 12px;");
        
        Button removeButton = new Button("✕");
        removeButton.setStyle(
            "-fx-background-color: transparent; " +
            "-fx-text-fill: white; " +
            "-fx-font-weight: bold; " +
            "-fx-font-size: 14px; " +
            "-fx-padding: 0 5 0 5; " +
            "-fx-cursor: hand;"
        );
        removeButton.setOnAction(e -> removeRecipient(user));
        
        chip.getChildren().addAll(nameLabel, removeButton);
        
        return chip;
    }
    
    /**
     * Filtre les utilisateurs selon le texte de recherche
     */
    private void filterUsers(String searchText) {
        if (searchText == null || searchText.trim().isEmpty()) {
            availableListView.setItems(availableUsers);
        } else {
            String search = searchText.toLowerCase();
            ObservableList<User> filtered = FXCollections.observableArrayList();
            
            for (User user : availableUsers) {
                if (user.getNomComplet().toLowerCase().contains(search) ||
                    user.getCode().toLowerCase().contains(search) ||
                    (user.getEmail() != null && user.getEmail().toLowerCase().contains(search))) {
                    filtered.add(user);
                }
            }
            
            availableListView.setItems(filtered);
        }
    }
    
    /**
     * Retourne le conteneur principal
     */
    public VBox getView() {
        return mainContainer;
    }
    
    /**
     * Retourne la liste des utilisateurs sélectionnés
     */
    public List<User> getSelectedUsers() {
        return new ArrayList<>(selectedUsers);
    }
    
    /**
     * Définit les utilisateurs sélectionnés
     */
    public void setSelectedUsers(List<User> users) {
        selectedUsers.clear();
        if (users != null) {
            selectedUsers.addAll(users);
        }
        updateSelectedDisplay();
    }
    
    /**
     * Cellule personnalisée pour la liste des utilisateurs
     */
    private class UserListCell extends ListCell<User> {
        @Override
        protected void updateItem(User user, boolean empty) {
            super.updateItem(user, empty);
            
            if (empty || user == null) {
                setText(null);
                setGraphic(null);
                setStyle("");
            } else {
                HBox container = new HBox(10);
                container.setAlignment(Pos.CENTER_LEFT);
                container.setPadding(new Insets(5));
                
                // Icône de présence
                UserPresence presence = messageSyncService.getUserPresence(user.getId());
                String statusIcon = presence != null ? presence.getStatut().getIcone() : "⚫";
                Label statusLabel = new Label(statusIcon);
                statusLabel.setStyle("-fx-font-size: 12px;");
                
                // Nom
                VBox nameBox = new VBox(2);
                Label nameLabel = new Label(user.getNomComplet());
                nameLabel.setStyle("-fx-font-weight: bold;");
                
                Label codeLabel = new Label(user.getCode() + 
                    (user.getEmail() != null ? " • " + user.getEmail() : ""));
                codeLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");
                
                nameBox.getChildren().addAll(nameLabel, codeLabel);
                
                // Indicateur si déjà sélectionné
                if (selectedUsers.contains(user)) {
                    Label selectedLabel = new Label("✓");
                    selectedLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 16px;");
                    
                    Region spacer = new Region();
                    HBox.setHgrow(spacer, Priority.ALWAYS);
                    
                    container.getChildren().addAll(statusLabel, nameBox, spacer, selectedLabel);
                    setStyle("-fx-background-color: #e8f5e9;");
                } else {
                    container.getChildren().addAll(statusLabel, nameBox);
                    setStyle("");
                }
                
                setGraphic(container);
                setText(null);
            }
        }
    }
}