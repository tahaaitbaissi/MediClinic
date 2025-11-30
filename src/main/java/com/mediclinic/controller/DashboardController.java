package com.mediclinic.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Label;
import javafx.scene.layout.HBox;
import com.mediclinic.service.*;
import com.mediclinic.model.*;
import com.mediclinic.util.UserSession;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class DashboardController implements Initializable {

    @FXML private Label totalPatientsLabel;
    @FXML private Label todayAppointmentsLabel;
    @FXML private Label monthlyRevenueLabel;
    @FXML private Label activeDoctorsLabel;
    @FXML private BarChart<String, Number> appointmentsChart;

    private DashboardService dashboardService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        dashboardService = new DashboardService();
        
        initializeStats();
        initializeCharts();
    }

    private void initializeStats() {
        try {
            if (!UserSession.isAuthenticated()) {
                return;
            }

            DashboardService.DashboardStats stats = dashboardService.getDashboardStats();
            Role role = UserSession.getInstance().getUser().getRole();

            // Today's appointments - always shown
            if (stats.getTodayAppointments() != null) {
                todayAppointmentsLabel.setText(String.valueOf(stats.getTodayAppointments()));
            } else {
                todayAppointmentsLabel.setText("0");
            }

            // Role-specific stats
            switch (role) {
                case ADMIN:
                    // Admin sees all stats
                    if (stats.getTotalPatients() != null) {
                        totalPatientsLabel.setText(String.valueOf(stats.getTotalPatients()));
                    } else {
                        totalPatientsLabel.setText("0");
                    }
                    
                    if (stats.getActiveDoctors() != null) {
                        activeDoctorsLabel.setText(String.valueOf(stats.getActiveDoctors()));
                    } else {
                        activeDoctorsLabel.setText("0");
                    }
                    
                    if (stats.getMonthlyRevenue() != null) {
                        BigDecimal revenue = stats.getMonthlyRevenue().setScale(2, RoundingMode.HALF_UP);
                        monthlyRevenueLabel.setText(revenue.toString() + " €");
                    } else {
                        monthlyRevenueLabel.setText("0 €");
                    }
                    break;

                case MEDECIN:
                    // Doctor sees only their appointments and patient count
                    if (stats.getPatientsTodayForDoctor() != null) {
                        totalPatientsLabel.setText(String.valueOf(stats.getPatientsTodayForDoctor()));
                    } else {
                        totalPatientsLabel.setText("0");
                    }
                    
                    activeDoctorsLabel.setText("N/A");
                    monthlyRevenueLabel.setText("N/A");
                    break;

                case SEC:
                    // Secretary sees unpaid invoices count instead of total patients
                    if (stats.getUnpaidInvoices() != null) {
                        totalPatientsLabel.setText(String.valueOf(stats.getUnpaidInvoices()));
                    } else {
                        totalPatientsLabel.setText("0");
                    }
                    
                    activeDoctorsLabel.setText("N/A");
                    monthlyRevenueLabel.setText("N/A");
                    break;
            }
            
        } catch (Exception e) {
            System.err.println("Error loading statistics: " + e.getMessage());
            e.printStackTrace();
            // Set default values on error
            totalPatientsLabel.setText("0");
            todayAppointmentsLabel.setText("0");
            monthlyRevenueLabel.setText("0 €");
            activeDoctorsLabel.setText("0");
        }
    }

    private void initializeCharts() {
        try {
            // In a real implementation, we would query appointments by day of the week
            // For now, showing sample data
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Rendez-vous par jour");

            series.getData().add(new XYChart.Data<>("Lun", 8));
            series.getData().add(new XYChart.Data<>("Mar", 12));
            series.getData().add(new XYChart.Data<>("Mer", 10));
            series.getData().add(new XYChart.Data<>("Jeu", 15));
            series.getData().add(new XYChart.Data<>("Ven", 11));
            series.getData().add(new XYChart.Data<>("Sam", 5));

            appointmentsChart.getData().clear();
            appointmentsChart.getData().add(series);
            appointmentsChart.setLegendVisible(false);
        } catch (Exception e) {
            System.err.println("Error loading chart data: " + e.getMessage());
        }
    }

    @FXML
    private void handleRefreshDashboard() {
        try {
            initializeStats();
            initializeCharts();
            showAlert("Dashboard", "Dashboard actualisé avec succès!", Alert.AlertType.INFORMATION);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors de l'actualisation: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleQuickPatient() {
        showAlert("Action rapide", "Ajout rapide de patient - Naviguez vers la section Patients", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleQuickAppointment() {
        showAlert("Action rapide", "Planification rapide de rendez-vous - Naviguez vers l'Agenda", Alert.AlertType.INFORMATION);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
