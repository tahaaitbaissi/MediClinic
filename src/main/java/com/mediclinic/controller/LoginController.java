package com.mediclinic.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.fxml.Initializable;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.mediclinic.service.AuthService;
import com.mediclinic.model.User;

import java.net.URL;
import java.util.ResourceBundle;

public class LoginController implements Initializable {

    @FXML private TextField usernameField;
    @FXML private PasswordField passwordField;
    @FXML private PasswordField confirmPasswordField;
    @FXML private VBox confirmPasswordBox;
    @FXML private Button loginBtn;
    @FXML private Button registerBtn;
    @FXML private Label formTitle;
    @FXML private Label errorLabel;
    @FXML private Label toggleLabel;
    @FXML private Hyperlink toggleLink;

    private AuthService authService;
    private boolean isRegisterMode = false;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        authService = new AuthService();
        errorLabel.setText("");
    }

    @FXML
    private void handleLogin() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();

        // Validation basique
        if (username.isEmpty() || password.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        try {
            User user = authService.authenticate(username, password);
            System.out.println("Connexion réussie pour: " + user.getUsername() + " (Role: " + user.getRole() + ")");
            
            // Charger la vue principale
            loadMainView();
            
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur de connexion: " + e.getMessage());
        }
    }

    @FXML
    private void handleRegister() {
        String username = usernameField.getText().trim();
        String password = passwordField.getText();
        String confirmPassword = confirmPasswordField.getText();

        // Validation
        if (username.isEmpty() || password.isEmpty() || confirmPassword.isEmpty()) {
            showError("Veuillez remplir tous les champs.");
            return;
        }

        if (username.length() < 3) {
            showError("Le nom d'utilisateur doit contenir au moins 3 caractères.");
            return;
        }

        if (password.length() < 4) {
            showError("Le mot de passe doit contenir au moins 4 caractères.");
            return;
        }

        if (!password.equals(confirmPassword)) {
            showError("Les mots de passe ne correspondent pas.");
            return;
        }

        try {
            // Créer un compte admin pour les tests
            authService.registerAdmin(username, password);
            System.out.println("Compte créé pour: " + username);
            
            showSuccess("Compte créé avec succès! Vous pouvez maintenant vous connecter.");
            
            // Revenir au mode connexion
            toggleMode();
            
        } catch (IllegalArgumentException e) {
            showError(e.getMessage());
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur lors de la création du compte: " + e.getMessage());
        }
    }

    @FXML
    private void toggleMode() {
        isRegisterMode = !isRegisterMode;
        
        if (isRegisterMode) {
            // Mode inscription
            formTitle.setText("Créer un compte");
            loginBtn.setVisible(false);
            loginBtn.setManaged(false);
            registerBtn.setVisible(true);
            registerBtn.setManaged(true);
            confirmPasswordBox.setVisible(true);
            confirmPasswordBox.setManaged(true);
            toggleLabel.setText("Déjà un compte?");
            toggleLink.setText("Se connecter");
        } else {
            // Mode connexion
            formTitle.setText("Connexion");
            loginBtn.setVisible(true);
            loginBtn.setManaged(true);
            registerBtn.setVisible(false);
            registerBtn.setManaged(false);
            confirmPasswordBox.setVisible(false);
            confirmPasswordBox.setManaged(false);
            toggleLabel.setText("Pas encore de compte?");
            toggleLink.setText("Créer un compte");
        }
        
        // Effacer les champs et erreurs
        clearFields();
        errorLabel.setText("");
    }

    private void loadMainView() {
        try {
            Parent mainView = FXMLLoader.load(getClass().getResource("/fxml/main_view.fxml"));
            Stage stage = (Stage) loginBtn.getScene().getWindow();
            stage.setScene(new Scene(mainView, 1200, 700));
            stage.setTitle("MediClinic - Gestion Médicale");
            stage.setMinWidth(1000);
            stage.setMinHeight(600);
        } catch (Exception e) {
            e.printStackTrace();
            showError("Erreur lors du chargement de l'application.");
        }
    }

    private void showError(String message) {
        errorLabel.setStyle("-fx-text-fill: #e74c3c;");
        errorLabel.setText(message);
    }

    private void showSuccess(String message) {
        errorLabel.setStyle("-fx-text-fill: #27ae60;");
        errorLabel.setText(message);
    }

    private void clearFields() {
        usernameField.clear();
        passwordField.clear();
        confirmPasswordField.clear();
    }
}

