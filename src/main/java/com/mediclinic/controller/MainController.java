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
            VBox view = FXMLLoader.load(getClass().getResource(fxmlFile));
            contentArea.getChildren().setAll(view);
        } catch (IOException e) {
            e.printStackTrace();
            showAlert("Erreur", "Impossible de charger la vue: " + fxmlFile, Alert.AlertType.ERROR);
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