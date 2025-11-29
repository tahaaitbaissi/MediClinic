package com.mediclinic.controller;

import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.layout.StackPane;
import javafx.scene.layout.VBox;

import java.io.IOException;

public class MainController {

    @FXML private StackPane contentArea;
    @FXML private Button dashboardBtn, patientsBtn, agendaBtn, doctorsBtn, billingBtn;

    @FXML
    public void initialize() {
        showDashboard();
    }

    @FXML
    private void showDashboard() {
        setActiveButton(dashboardBtn);
        loadView("/fxml/dashboard_view.fxml");
    }

    @FXML
    private void showPatients() {
        setActiveButton(patientsBtn);
        loadView("/fxml/patient_view.fxml");
    }

    @FXML
    private void showAgenda() {
        setActiveButton(agendaBtn);
        loadView("/fxml/agenda_view.fxml");
    }

    @FXML
    private void showDoctors() {
        setActiveButton(doctorsBtn);
        loadView("/fxml/doctor_view.fxml");
    }

    @FXML
    private void showBilling() {
        setActiveButton(billingBtn);
        loadView("/fxml/billing_view.fxml");
    }

    private void setActiveButton(Button activeButton) {
        Button[] buttons = {dashboardBtn, patientsBtn, agendaBtn, doctorsBtn, billingBtn};
        for (Button button : buttons) {
            button.getStyleClass().remove("active");
        }
        activeButton.getStyleClass().add("active");
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