package com.mediclinic.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.mediclinic.model.RendezVous;
import com.mediclinic.service.RendezVousService;

import java.net.URL;
import java.time.LocalDate;
import java.util.ResourceBundle;

public class AgendaController implements Initializable {

    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> doctorCombo;
    @FXML private TextField searchField;
    @FXML private TableView<RendezVous> appointmentTable;

    private RendezVousService rendezVousService;
    private ObservableList<RendezVous> appointmentList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        rendezVousService = new RendezVousService();
        setupFilters();
        loadAppointments();
    }

    private void setupFilters() {
        doctorCombo.setItems(FXCollections.observableArrayList(
                "Tous les médecins", "Dr. Martin", "Dr. Dubois", "Dr. Moreau"
        ));
        doctorCombo.setValue("Tous les médecins");

        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now().plusDays(7));
    }

    private void loadAppointments() {
        try {
            appointmentList = FXCollections.observableArrayList();
            appointmentTable.setItems(appointmentList);
        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors du chargement des rendez-vous: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleSearch() {
        showAlert("Recherche", "Fonction de recherche à implémenter", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleRefresh() {
        loadAppointments();
        showAlert("Actualisation", "Agenda actualisé", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleNewAppointment() {
        showAlert("Nouveau RDV", "Création d'un nouveau rendez-vous", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleWaitingList() {
        showAlert("Liste d'attente", "Affichage de la liste d'attente", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleStatistics() {
        showAlert("Statistiques", "Affichage des statistiques", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleDailyReport() {
        showAlert("Rapport", "Génération du rapport quotidien", Alert.AlertType.INFORMATION);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}