package com.mediclinic.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;

import java.net.URL;
import java.util.ResourceBundle;

public class DashboardController implements Initializable {

    @FXML private Label totalPatientsLabel;
    @FXML private Label todayAppointmentsLabel;
    @FXML private Label monthlyRevenueLabel;
    @FXML private Label activeDoctorsLabel;
    @FXML private BarChart<String, Number> appointmentsChart;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        initializeStats();
        initializeCharts();
    }

    private void initializeStats() {
        totalPatientsLabel.setText("156");
        todayAppointmentsLabel.setText("12");
        monthlyRevenueLabel.setText("8,450 €");
        activeDoctorsLabel.setText("5");
    }

    private void initializeCharts() {
        CategoryAxis xAxis = new CategoryAxis();
        NumberAxis yAxis = new NumberAxis();

        XYChart.Series<String, Number> series = new XYChart.Series<>();
        series.setName("Rendez-vous par jour");

        series.getData().add(new XYChart.Data<>("Lun", 8));
        series.getData().add(new XYChart.Data<>("Mar", 12));
        series.getData().add(new XYChart.Data<>("Mer", 10));
        series.getData().add(new XYChart.Data<>("Jeu", 15));
        series.getData().add(new XYChart.Data<>("Ven", 11));
        series.getData().add(new XYChart.Data<>("Sam", 5));

        appointmentsChart.getData().add(series);
        appointmentsChart.setLegendVisible(false);
    }

    @FXML
    private void handleRefreshDashboard() {
        initializeStats();
        initializeCharts();
        showAlert("Dashboard", "Dashboard actualisé", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleQuickPatient() {
        showAlert("Action rapide", "Ajout rapide de patient", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleQuickAppointment() {
        showAlert("Action rapide", "Planification rapide de rendez-vous", Alert.AlertType.INFORMATION);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}