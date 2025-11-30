package com.mediclinic.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.mediclinic.model.*;
import com.mediclinic.service.*;
import com.mediclinic.util.PermissionChecker;
import com.mediclinic.util.UserSession;

import java.net.URL;
import java.util.ResourceBundle;

public class UserController implements Initializable {

    @FXML private TableView<User> userTable;
    @FXML private TableColumn<User, Long> colId;
    @FXML private TableColumn<User, String> colUsername;
    @FXML private TableColumn<User, String> colRole;
    @FXML private TextField searchField;

    private UserService userService;
    private ObservableList<User> userList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Check permission - only ADMIN can access
        try {
            PermissionChecker.requireRole(Role.ADMIN);
        } catch (IllegalStateException e) {
            showAlert("Accès refusé", e.getMessage(), Alert.AlertType.ERROR);
            return;
        }
        
        userService = new UserService();
        
        setupTableColumns();
        loadUsers();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colUsername.setCellValueFactory(new PropertyValueFactory<>("username"));
        
        colRole.setCellValueFactory(cellData -> 
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getRole() != null ? 
                cellData.getValue().getRole().name() : "N/A"
            )
        );

        // Add actions column
        TableColumn<User, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(150);
        colActions.setCellFactory(param -> new TableCell<User, Void>() {
            private final Button editBtn = new Button("✏️");
            private final Button passwordBtn = new Button("🔑");
            private final Button deleteBtn = new Button("🗑️");

            {
                editBtn.getStyleClass().add("btn-warning");
                passwordBtn.getStyleClass().add("btn-primary");
                deleteBtn.getStyleClass().add("btn-danger");

                editBtn.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    editUser(user);
                });

                passwordBtn.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    changePassword(user);
                });

                deleteBtn.setOnAction(event -> {
                    User user = getTableView().getItems().get(getIndex());
                    deleteUser(user);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5, editBtn, passwordBtn, deleteBtn);
                    setGraphic(buttons);
                }
            }
        });

        userTable.getColumns().add(colActions);
    }

    private void loadUsers() {
        try {
            userList = FXCollections.observableArrayList(userService.findAll());
            userTable.setItems(userList);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors du chargement des utilisateurs: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleNewUser() {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Nouvel Utilisateur");
        dialog.setHeaderText("Créer un nouvel utilisateur");

        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField usernameField = new TextField();
        usernameField.setPromptText("Nom d'utilisateur");
        PasswordField passwordField = new PasswordField();
        passwordField.setPromptText("Mot de passe");
        ComboBox<Role> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll(Role.values());
        roleCombo.setValue(Role.SEC);

        grid.add(new Label("Nom d'utilisateur:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Mot de passe:"), 0, 1);
        grid.add(passwordField, 1, 1);
        grid.add(new Label("Rôle:"), 0, 2);
        grid.add(roleCombo, 1, 2);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType && !usernameField.getText().trim().isEmpty() 
                && !passwordField.getText().isEmpty() && roleCombo.getValue() != null) {
                User newUser = new User();
                newUser.setUsername(usernameField.getText().trim());
                newUser.setPasswordHash(passwordField.getText()); // Will be hashed by service
                newUser.setRole(roleCombo.getValue());
                return newUser;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(user -> {
            try {
                userService.createUserWithPassword(user.getUsername(), user.getPasswordHash(), user.getRole());
                loadUsers();
                showAlert("Succès", "Utilisateur créé avec succès!", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Erreur", "Erreur lors de la création: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void editUser(User user) {
        Dialog<User> dialog = new Dialog<>();
        dialog.setTitle("Modifier Utilisateur");
        dialog.setHeaderText("Modifier les informations de l'utilisateur");

        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        TextField usernameField = new TextField(user.getUsername());
        ComboBox<Role> roleCombo = new ComboBox<>();
        roleCombo.getItems().addAll(Role.values());
        roleCombo.setValue(user.getRole());

        grid.add(new Label("Nom d'utilisateur:"), 0, 0);
        grid.add(usernameField, 1, 0);
        grid.add(new Label("Rôle:"), 0, 1);
        grid.add(roleCombo, 1, 1);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType && !usernameField.getText().trim().isEmpty() 
                && roleCombo.getValue() != null) {
                user.setUsername(usernameField.getText().trim());
                user.setRole(roleCombo.getValue());
                return user;
            }
            return null;
        });

        dialog.showAndWait().ifPresent(updatedUser -> {
            try {
                userService.updateUser(updatedUser);
                loadUsers();
                showAlert("Succès", "Utilisateur modifié avec succès!", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Erreur", "Erreur lors de la modification: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void changePassword(User user) {
        Dialog<String> dialog = new Dialog<>();
        dialog.setTitle("Changer le mot de passe");
        dialog.setHeaderText("Changer le mot de passe pour: " + user.getUsername());

        ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
        dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

        GridPane grid = new GridPane();
        grid.setHgap(10);
        grid.setVgap(10);

        PasswordField newPasswordField = new PasswordField();
        newPasswordField.setPromptText("Nouveau mot de passe");

        grid.add(new Label("Nouveau mot de passe:"), 0, 0);
        grid.add(newPasswordField, 1, 0);

        dialog.getDialogPane().setContent(grid);

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == saveButtonType && !newPasswordField.getText().isEmpty()) {
                return newPasswordField.getText();
            }
            return null;
        });

        dialog.showAndWait().ifPresent(newPassword -> {
            try {
                userService.updatePassword(user.getId(), null, newPassword); // null for old password when admin changes it
                showAlert("Succès", "Mot de passe modifié avec succès!", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Erreur", "Erreur lors de la modification: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void deleteUser(User user) {
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmer la suppression");
        confirm.setHeaderText("Supprimer l'utilisateur: " + user.getUsername());
        confirm.setContentText("Êtes-vous sûr de vouloir supprimer cet utilisateur?");

        confirm.showAndWait().ifPresent(buttonType -> {
            if (buttonType == ButtonType.OK) {
                try {
                    // Prevent deleting yourself
                    if (UserSession.isAuthenticated() && 
                        UserSession.getInstance().getUser().getId().equals(user.getId())) {
                        showAlert("Erreur", "Vous ne pouvez pas supprimer votre propre compte!", Alert.AlertType.ERROR);
                        return;
                    }
                    userService.deleteUser(user.getId());
                    loadUsers();
                    showAlert("Succès", "Utilisateur supprimé avec succès!", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Erreur", "Erreur lors de la suppression: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    @FXML
    private void handleSearch() {
        String searchText = searchField.getText().toLowerCase();
        if (searchText == null || searchText.trim().isEmpty()) {
            userTable.setItems(userList);
        } else {
            ObservableList<User> filtered = userList.filtered(user -> 
                user.getUsername().toLowerCase().contains(searchText) ||
                (user.getRole() != null && user.getRole().name().toLowerCase().contains(searchText))
            );
            userTable.setItems(filtered);
        }
    }

    @FXML
    private void handleRefresh() {
        loadUsers();
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}

