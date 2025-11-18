package com.mediclinic.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class DoctorController implements Initializable {

    @FXML private TableView<?> doctorTable;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> specialtyCombo;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupSpecialties();
    }

    private void setupSpecialties() {
        specialtyCombo.setItems(javafx.collections.FXCollections.observableArrayList(
                "Toutes spécialités", "Généraliste", "Cardiologie", "Dermatologie",
                "Pédiatrie", "Neurologie", "Radiologie"
        ));
        specialtyCombo.setValue("Toutes spécialités");
    }

    @FXML
    private void handleAddDoctor() {
        showAlert("Médecins", "Ajout d'un nouveau médecin", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleSearch() {
        showAlert("Recherche", "Recherche de médecins", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleExportDoctors() {
        showAlert("Export", "Export de la liste des médecins", Alert.AlertType.INFORMATION);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}