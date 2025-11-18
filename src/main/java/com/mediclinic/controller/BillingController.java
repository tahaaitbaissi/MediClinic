package com.mediclinic.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;

import java.net.URL;
import java.util.ResourceBundle;

public class BillingController implements Initializable {

    @FXML private TableView<?> invoiceTable;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> statusCombo;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        setupStatusFilter();
    }

    private void setupStatusFilter() {
        statusCombo.setItems(javafx.collections.FXCollections.observableArrayList(
                "Tous les statuts", "Payée", "En attente", "Annulée"
        ));
        statusCombo.setValue("Tous les statuts");
    }

    @FXML
    private void handleNewInvoice() {
        showAlert("Facturation", "Création d'une nouvelle facture", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleSearchInvoices() {
        showAlert("Recherche", "Recherche de factures", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleGenerateReport() {
        showAlert("Rapport", "Génération du rapport financier", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleExportFinancial() {
        showAlert("Export", "Export des données financières", Alert.AlertType.INFORMATION);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}