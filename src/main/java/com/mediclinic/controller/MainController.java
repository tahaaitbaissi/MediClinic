package com.mediclinic.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Parent;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;
import javafx.stage.Stage;
import com.mediclinic.model.Role;
import com.mediclinic.util.UserSession;
import com.mediclinic.util.PermissionChecker;

import java.io.IOException;

public class MainController {

    @FXML private StackPane contentArea;
    @FXML private Button dashboardBtn, patientsBtn, agendaBtn, doctorsBtn, billingBtn, usersBtn;
    @FXML private Label userInfoLabel;
    @FXML private Button logoutBtn;

    @FXML
    public void initialize() {
        // Check authentication
        if (!UserSession.isAuthenticated()) {
            redirectToLogin();
            return;
        }

        // Set user info
        updateUserInfo();
        
        // Setup role-based menu visibility
        setupRoleBasedMenu();
        
        // Show dashboard
        showDashboard();
    }

    private void updateUserInfo() {
        try {
            UserSession session = UserSession.getInstance();
            String username = session.getUser().getUsername();
            Role role = session.getUser().getRole();
            
            String roleLabel = "";
            switch (role) {
                case ADMIN:
                    roleLabel = "🔑 Administrateur";
                    break;
                case MEDECIN:
                    roleLabel = "👨‍⚕️ Médecin";
                    if (session.getUser().getMedecin() != null) {
                        username = "Dr. " + session.getUser().getMedecin().getNomComplet();
                    }
                    break;
                case SEC:
                    roleLabel = "📋 Secrétaire";
                    break;
            }
            
            userInfoLabel.setText(roleLabel + " - " + username);
        } catch (Exception e) {
            e.printStackTrace();
            userInfoLabel.setText("Utilisateur");
        }
    }

    private void setupRoleBasedMenu() {
        try {
            UserSession session = UserSession.getInstance();
            Role role = session.getUser().getRole();

            // All roles can see dashboard
            dashboardBtn.setVisible(true);
            dashboardBtn.setManaged(true);

            // Role-based visibility
            switch (role) {
                case ADMIN:
                    // Admin sees everything
                    patientsBtn.setVisible(true);
                    patientsBtn.setManaged(true);
                    agendaBtn.setVisible(true);
                    agendaBtn.setManaged(true);
                    doctorsBtn.setVisible(true);
                    doctorsBtn.setManaged(true);
                    billingBtn.setVisible(true);
                    billingBtn.setManaged(true);
                    usersBtn.setVisible(true);
                    usersBtn.setManaged(true);
                    break;

                case MEDECIN:
                    // Doctor sees only Dashboard and Agenda
                    patientsBtn.setVisible(true);
                    patientsBtn.setManaged(true); // Can view patient info
                    agendaBtn.setVisible(true);
                    agendaBtn.setManaged(true);
                    doctorsBtn.setVisible(false);
                    doctorsBtn.setManaged(false);
                    billingBtn.setVisible(false);
                    billingBtn.setManaged(false);
                    usersBtn.setVisible(false);
                    usersBtn.setManaged(false);
                    break;

                case SEC:
                    // Secretary sees Dashboard, Patients, Agenda, Billing
                    patientsBtn.setVisible(true);
                    patientsBtn.setManaged(true);
                    agendaBtn.setVisible(true);
                    agendaBtn.setManaged(true);
                    doctorsBtn.setVisible(false);
                    doctorsBtn.setManaged(false);
                    billingBtn.setVisible(true);
                    billingBtn.setManaged(true);
                    usersBtn.setVisible(false);
                    usersBtn.setManaged(false);
                    break;
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    @FXML
    private void handleLogout() {
        try {
            UserSession.clean();
            redirectToLogin();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de la déconnexion: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void redirectToLogin() {
        try {
            Parent loginView = FXMLLoader.load(getClass().getResource("/fxml/login_view.fxml"));
            Stage stage = (Stage) (logoutBtn != null ? logoutBtn.getScene().getWindow() : 
                                    (dashboardBtn != null ? dashboardBtn.getScene().getWindow() : null));
            if (stage != null) {
                stage.setScene(new Scene(loginView, 500, 600));
                stage.setTitle("MediClinic - Connexion");
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors du chargement de la page de connexion.", Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void showDashboard() {
        setActiveButton(dashboardBtn);
        loadView("/fxml/dashboard_view.fxml");
    }

    @FXML
    private void showPatients() {
        checkPermission("patients");
        setActiveButton(patientsBtn);
        loadView("/fxml/patient_view.fxml");
    }

    @FXML
    private void showAgenda() {
        checkPermission("agenda");
        setActiveButton(agendaBtn);
        loadView("/fxml/agenda_view.fxml");
    }

    @FXML
    private void showDoctors() {
        checkPermission("doctors");
        setActiveButton(doctorsBtn);
        loadView("/fxml/doctor_view.fxml");
    }

    @FXML
    private void showBilling() {
        checkPermission("billing");
        setActiveButton(billingBtn);
        loadView("/fxml/billing_view.fxml");
    }

    @FXML
    private void showUsers() {
        checkPermission("users");
        setActiveButton(usersBtn);
        loadView("/fxml/user_view.fxml");
    }

    private void checkPermission(String page) {
        try {
            if (!UserSession.isAuthenticated()) {
                redirectToLogin();
                return;
            }

            UserSession session = UserSession.getInstance();
            Role role = session.getUser().getRole();

            if (!PermissionChecker.canAccessPage(role, page)) {
                showAlert("Accès refusé", 
                    "Vous n'avez pas la permission d'accéder à cette page.", 
                    Alert.AlertType.WARNING);
                showDashboard(); // Redirect to dashboard
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur de vérification des permissions: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void setActiveButton(Button activeButton) {
        Button[] buttons = {dashboardBtn, patientsBtn, agendaBtn, doctorsBtn, billingBtn, usersBtn};
        for (Button button : buttons) {
            if (button != null && button.isManaged()) {
                button.getStyleClass().remove("active");
            }
        }
        if (activeButton != null && activeButton.isManaged()) {
            activeButton.getStyleClass().add("active");
        }
    }

    private void loadView(String fxmlFile) {
        try {
            // Vérifier que la ressource existe
            var resourceUrl = getClass().getResource(fxmlFile);
            if (resourceUrl == null) {
                System.err.println("ERREUR: La ressource FXML est introuvable: " + fxmlFile);
                System.err.println("Vérifiez que le fichier existe dans src/main/resources" + fxmlFile);
                showAlert("Erreur de chargement", 
                    "La vue n'a pas pu être chargée.\nFichier introuvable: " + fxmlFile + 
                    "\n\nAssurez-vous que les ressources sont compilées correctement.", 
                    Alert.AlertType.ERROR);
                return;
            }
            
            System.out.println("Chargement de la vue: " + fxmlFile + " depuis " + resourceUrl);
            VBox view = FXMLLoader.load(resourceUrl);
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger la vue: " + fxmlFile + "\n\nDétails: " + e.getMessage(), Alert.AlertType.ERROR);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur inattendue", "Une erreur s'est produite lors du chargement de la vue.\n\nDétails: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}