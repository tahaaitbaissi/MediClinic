package com.mediclinic.controller;

import com.mediclinic.model.*;
import com.mediclinic.service.*;
import com.mediclinic.util.UserSession;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.chart.BarChart;
import javafx.scene.chart.CategoryAxis;
import javafx.scene.chart.NumberAxis;
import javafx.scene.chart.XYChart;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TableColumn;
import javafx.scene.control.TableView;
import javafx.scene.layout.HBox;

public class DashboardController implements Initializable {

    @FXML
    private Label totalPatientsLabel;

    @FXML
    private Label todayAppointmentsLabel;

    @FXML
    private Label monthlyRevenueLabel;

    @FXML
    private Label activeDoctorsLabel;

    @FXML
    private Label todayApptCountLabel;

    @FXML
    private BarChart<String, Number> appointmentsChart;

    @FXML
    private TableView<RendezVous> upcomingAppointmentsTable;

    @FXML
    private TableColumn<RendezVous, String> colUpcomingTime;

    @FXML
    private TableColumn<RendezVous, String> colUpcomingPatient;

    @FXML
    private TableColumn<RendezVous, String> colUpcomingDoctor;

    @FXML
    private TableColumn<RendezVous, String> colUpcomingMotif;

    @FXML
    private Button quickPatientBtn;

    @FXML
    private Button quickAppointmentBtn;

    @FXML
    private Button quickInvoiceBtn;

    private DashboardService dashboardService;
    private RendezVousService rendezVousService;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        System.out.println(
            "\n========== DashboardController.initialize() START =========="
        );
        dashboardService = new DashboardService();
        rendezVousService = new RendezVousService();

        try {
            setupRoleBasedUI();
            System.out.println("✓ setupRoleBasedUI done");

            setupUpcomingAppointmentsTable();
            System.out.println("✓ setupUpcomingAppointmentsTable done");

            initializeStats();
            System.out.println("✓ initializeStats done");

            initializeCharts();
            System.out.println("✓ initializeCharts done");

            // Load appointments AFTER everything is initialized
            loadUpcomingAppointments();
            System.out.println("✓ loadUpcomingAppointments done");
        } catch (Exception e) {
            System.err.println("ERROR in initialize: " + e.getMessage());
            e.printStackTrace();
        }
        System.out.println(
            "========== DashboardController.initialize() END ==========\n"
        );
    }

    private void setupRoleBasedUI() {
        try {
            if (!UserSession.isAuthenticated()) {
                return;
            }

            Role role = UserSession.getInstance().getUser().getRole();

            // Show/hide action buttons based on role
            boolean canCreatePatient = role == Role.ADMIN || role == Role.SEC;
            boolean canCreateAppointment =
                role == Role.ADMIN || role == Role.SEC;
            boolean canCreateInvoice = role == Role.ADMIN || role == Role.SEC;

            if (quickPatientBtn != null) {
                quickPatientBtn.setVisible(canCreatePatient);
                quickPatientBtn.setManaged(canCreatePatient);
            }
            if (quickAppointmentBtn != null) {
                quickAppointmentBtn.setVisible(canCreateAppointment);
                quickAppointmentBtn.setManaged(canCreateAppointment);
            }
            if (quickInvoiceBtn != null) {
                quickInvoiceBtn.setVisible(canCreateInvoice);
                quickInvoiceBtn.setManaged(canCreateInvoice);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupUpcomingAppointmentsTable() {
        System.out.println("=== setupUpcomingAppointmentsTable() STARTED ===");

        // Check if table exists
        if (upcomingAppointmentsTable == null) {
            System.err.println("CRITICAL: upcomingAppointmentsTable is NULL!");
            return;
        }
        System.out.println("✓ upcomingAppointmentsTable found");

        // Setup Time Column
        if (colUpcomingTime != null) {
            colUpcomingTime.setCellValueFactory(
                new javafx.util.Callback<
                    javafx.scene.control.TableColumn.CellDataFeatures<
                        RendezVous,
                        String
                    >,
                    javafx.beans.value.ObservableValue<String>
                >() {
                    @Override
                    public javafx.beans.value.ObservableValue<String> call(
                        javafx.scene.control.TableColumn.CellDataFeatures<
                            RendezVous,
                            String
                        > param
                    ) {
                        RendezVous rdv = param.getValue();
                        if (rdv == null || rdv.getDateHeureDebut() == null) {
                            return new javafx.beans.property.SimpleStringProperty(
                                ""
                            );
                        }
                        DateTimeFormatter formatter =
                            DateTimeFormatter.ofPattern("dd/MM HH:mm");
                        return new javafx.beans.property.SimpleStringProperty(
                            rdv.getDateHeureDebut().format(formatter)
                        );
                    }
                }
            );
            System.out.println("✓ colUpcomingTime cell factory configured");
        } else {
            System.err.println("✗ colUpcomingTime is NULL");
        }

        // Setup Patient Column
        if (colUpcomingPatient != null) {
            colUpcomingPatient.setCellValueFactory(
                new javafx.util.Callback<
                    javafx.scene.control.TableColumn.CellDataFeatures<
                        RendezVous,
                        String
                    >,
                    javafx.beans.value.ObservableValue<String>
                >() {
                    @Override
                    public javafx.beans.value.ObservableValue<String> call(
                        javafx.scene.control.TableColumn.CellDataFeatures<
                            RendezVous,
                            String
                        > param
                    ) {
                        RendezVous rdv = param.getValue();
                        if (rdv == null || rdv.getPatient() == null) {
                            return new javafx.beans.property.SimpleStringProperty(
                                "N/A"
                            );
                        }
                        return new javafx.beans.property.SimpleStringProperty(
                            rdv.getPatient().getNomComplet()
                        );
                    }
                }
            );
            System.out.println("✓ colUpcomingPatient cell factory configured");
        } else {
            System.err.println("✗ colUpcomingPatient is NULL");
        }

        // Setup Doctor Column
        if (colUpcomingDoctor != null) {
            colUpcomingDoctor.setCellValueFactory(
                new javafx.util.Callback<
                    javafx.scene.control.TableColumn.CellDataFeatures<
                        RendezVous,
                        String
                    >,
                    javafx.beans.value.ObservableValue<String>
                >() {
                    @Override
                    public javafx.beans.value.ObservableValue<String> call(
                        javafx.scene.control.TableColumn.CellDataFeatures<
                            RendezVous,
                            String
                        > param
                    ) {
                        RendezVous rdv = param.getValue();
                        if (rdv == null || rdv.getMedecin() == null) {
                            return new javafx.beans.property.SimpleStringProperty(
                                "N/A"
                            );
                        }
                        return new javafx.beans.property.SimpleStringProperty(
                            rdv.getMedecin().getNomComplet()
                        );
                    }
                }
            );
            System.out.println("✓ colUpcomingDoctor cell factory configured");
        } else {
            System.err.println("✗ colUpcomingDoctor is NULL");
        }

        // Setup Motif Column
        if (colUpcomingMotif != null) {
            colUpcomingMotif.setCellValueFactory(
                new javafx.util.Callback<
                    javafx.scene.control.TableColumn.CellDataFeatures<
                        RendezVous,
                        String
                    >,
                    javafx.beans.value.ObservableValue<String>
                >() {
                    @Override
                    public javafx.beans.value.ObservableValue<String> call(
                        javafx.scene.control.TableColumn.CellDataFeatures<
                            RendezVous,
                            String
                        > param
                    ) {
                        RendezVous rdv = param.getValue();
                        if (rdv == null) {
                            return new javafx.beans.property.SimpleStringProperty(
                                "N/A"
                            );
                        }
                        String motif = rdv.getMotif();
                        return new javafx.beans.property.SimpleStringProperty(
                            motif != null ? motif : "N/A"
                        );
                    }
                }
            );
            System.out.println("✓ colUpcomingMotif cell factory configured");
        } else {
            System.err.println("✗ colUpcomingMotif is NULL");
        }

        System.out.println(
            "=== setupUpcomingAppointmentsTable() COMPLETED ===\n"
        );
    }

    private void loadUpcomingAppointments() {
        System.out.println("\n--- loadUpcomingAppointments() START ---");

        try {
            if (upcomingAppointmentsTable == null) {
                System.err.println("ERROR: upcomingAppointmentsTable is NULL!");
                return;
            }
            System.out.println("✓ Table reference valid");

            if (!UserSession.isAuthenticated()) {
                System.err.println("User not authenticated");
                upcomingAppointmentsTable.setItems(
                    FXCollections.observableArrayList()
                );
                if (todayApptCountLabel != null) todayApptCountLabel.setText(
                    "(0)"
                );
                return;
            }
            System.out.println("✓ User authenticated");

            // Get appointments from service
            List<RendezVous> appointments =
                dashboardService.getTodayAppointments();
            System.out.println(
                "✓ Retrieved " +
                    appointments.size() +
                    " appointments from service"
            );

            // Print each appointment for debugging
            if (appointments.isEmpty()) {
                System.out.println(
                    "  WARNING: No appointments found for today!"
                );
            }
            for (int i = 0; i < appointments.size(); i++) {
                RendezVous rdv = appointments.get(i);
                System.out.println(
                    "  [" +
                        i +
                        "] ID=" +
                        rdv.getId() +
                        ", Time=" +
                        rdv.getDateHeureDebut() +
                        ", Patient=" +
                        (rdv.getPatient() != null
                            ? rdv.getPatient().getNomComplet()
                            : "NULL") +
                        ", Doctor=" +
                        (rdv.getMedecin() != null
                            ? rdv.getMedecin().getNomComplet()
                            : "NULL") +
                        ", Motif=" +
                        rdv.getMotif()
                );
            }

            // Force clear and reset
            upcomingAppointmentsTable.getItems().clear();
            System.out.println(
                "✓ Table cleared, current items: " +
                    upcomingAppointmentsTable.getItems().size()
            );

            // Set items to table with force refresh
            javafx.collections.ObservableList<RendezVous> items =
                FXCollections.observableArrayList(appointments);
            upcomingAppointmentsTable.setItems(items);
            System.out.println(
                "✓ Table updated with " +
                    upcomingAppointmentsTable.getItems().size() +
                    " rows"
            );

            // Update counter label
            if (todayApptCountLabel != null) {
                todayApptCountLabel.setText("(" + appointments.size() + ")");
                System.out.println(
                    "✓ Counter label updated to: (" + appointments.size() + ")"
                );
            }

            // Force layout update
            upcomingAppointmentsTable.refresh();
            System.out.println("✓ Table refresh() called");
        } catch (Exception e) {
            System.err.println(
                "ERROR in loadUpcomingAppointments: " + e.getMessage()
            );
            e.printStackTrace();
        }

        System.out.println("--- loadUpcomingAppointments() END ---\n");
    }

    private void initializeStats() {
        try {
            if (!UserSession.isAuthenticated()) {
                return;
            }

            DashboardService.DashboardStats stats =
                dashboardService.getDashboardStats();
            Role role = UserSession.getInstance().getUser().getRole();

            // Today's appointments - always shown
            if (stats.getTodayAppointments() != null) {
                todayAppointmentsLabel.setText(
                    String.valueOf(stats.getTodayAppointments())
                );
            } else {
                todayAppointmentsLabel.setText("0");
            }

            // Role-specific stats
            switch (role) {
                case ADMIN:
                    // Admin sees all stats
                    if (stats.getTotalPatients() != null) {
                        totalPatientsLabel.setText(
                            String.valueOf(stats.getTotalPatients())
                        );
                    } else {
                        totalPatientsLabel.setText("0");
                    }

                    if (stats.getActiveDoctors() != null) {
                        activeDoctorsLabel.setText(
                            String.valueOf(stats.getActiveDoctors())
                        );
                    } else {
                        activeDoctorsLabel.setText("0");
                    }

                    if (stats.getMonthlyRevenue() != null) {
                        BigDecimal revenue = stats
                            .getMonthlyRevenue()
                            .setScale(2, RoundingMode.HALF_UP);
                        monthlyRevenueLabel.setText(revenue.toString() + " €");
                    } else {
                        monthlyRevenueLabel.setText("0 €");
                    }
                    break;
                case MEDECIN:
                    // Doctor sees only their appointments and patient count
                    if (stats.getPatientsTodayForDoctor() != null) {
                        totalPatientsLabel.setText(
                            String.valueOf(stats.getPatientsTodayForDoctor())
                        );
                    } else {
                        totalPatientsLabel.setText("0");
                    }

                    activeDoctorsLabel.setText("N/A");
                    monthlyRevenueLabel.setText("N/A");
                    break;
                case SEC:
                    // Secretary sees unpaid invoices count instead of total patients
                    if (stats.getUnpaidInvoices() != null) {
                        totalPatientsLabel.setText(
                            String.valueOf(stats.getUnpaidInvoices())
                        );
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
            // Get real appointment data grouped by day of week
            Map<String, Integer> weeklyData =
                dashboardService.getWeeklyAppointments();

            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Rendez-vous par jour");

            // Add data for each day of the week
            for (Map.Entry<String, Integer> entry : weeklyData.entrySet()) {
                series
                    .getData()
                    .add(new XYChart.Data<>(entry.getKey(), entry.getValue()));
            }

            appointmentsChart.getData().clear();
            appointmentsChart.getData().add(series);
            appointmentsChart.setLegendVisible(false);
        } catch (Exception e) {
            System.err.println("Error loading chart data: " + e.getMessage());
            // Fallback with empty data
            XYChart.Series<String, Number> series = new XYChart.Series<>();
            series.setName("Rendez-vous par jour");
            appointmentsChart.getData().add(series);
        }
    }

    @FXML
    private void handleRefreshDashboard() {
        try {
            System.out.println("\n========== REFRESH DASHBOARD ==========");
            initializeStats();
            initializeCharts();
            loadUpcomingAppointments();
            System.out.println("========== REFRESH COMPLETE ==========\n");
            showAlert(
                "Dashboard",
                "Dashboard actualisé avec succès!",
                Alert.AlertType.INFORMATION
            );
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(
                "Erreur",
                "Erreur lors de l'actualisation: " + e.getMessage(),
                Alert.AlertType.ERROR
            );
        }
    }

    @FXML
    private void handleQuickPatient() {
        try {
            // Check if user has permission
            if (!rendezVousService.canCreateAppointment()) {
                showAlert(
                    "Permission Refusée",
                    "Vous n'avez pas la permission de créer un nouveau patient.",
                    Alert.AlertType.WARNING
                );
                return;
            }

            // Navigate to Patient view
            MainController mainController = MainController.getInstance();
            if (mainController != null) {
                mainController.showPatientView();
            } else {
                showAlert(
                    "Navigation",
                    "Veuillez utiliser la section Patients pour ajouter un nouveau patient.",
                    Alert.AlertType.INFORMATION
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(
                "Erreur",
                "Erreur lors de l'accès à la section Patients: " +
                    e.getMessage(),
                Alert.AlertType.ERROR
            );
        }
    }

    @FXML
    private void handleQuickAppointment() {
        try {
            // Check if user has permission
            if (!rendezVousService.canCreateAppointment()) {
                showAlert(
                    "Permission Refusée",
                    "Vous n'avez pas la permission de planifier un rendez-vous.",
                    Alert.AlertType.WARNING
                );
                return;
            }

            // Navigate to Agenda view
            MainController mainController = MainController.getInstance();
            if (mainController != null) {
                mainController.showAgendaView();
            } else {
                showAlert(
                    "Navigation",
                    "Veuillez utiliser la section Agenda pour planifier un rendez-vous.",
                    Alert.AlertType.INFORMATION
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(
                "Erreur",
                "Erreur lors de l'accès à l'Agenda: " + e.getMessage(),
                Alert.AlertType.ERROR
            );
        }
    }

    @FXML
    private void handleQuickInvoice() {
        try {
            // Check if user has permission
            if (!rendezVousService.canCreateAppointment()) {
                showAlert(
                    "Permission Refusée",
                    "Vous n'avez pas la permission de créer une facture.",
                    Alert.AlertType.WARNING
                );
                return;
            }

            // Navigate to Billing view
            MainController mainController = MainController.getInstance();
            if (mainController != null) {
                mainController.showBillingView();
            } else {
                showAlert(
                    "Navigation",
                    "Veuillez utiliser la section Facturation pour créer une facture.",
                    Alert.AlertType.INFORMATION
                );
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(
                "Erreur",
                "Erreur lors de l'accès à la Facturation: " + e.getMessage(),
                Alert.AlertType.ERROR
            );
        }
    }

    @FXML
    private void handleViewReports() {
        try {
            // For now, show a dialog with available reports
            Alert alert = new Alert(Alert.AlertType.INFORMATION);
            alert.setTitle("Rapports");
            alert.setHeaderText("Module de Rapports");
            alert.setContentText(
                "Les rapports suivants seront bientôt disponibles:\n" +
                    "• Rapport des consultations\n" +
                    "• Rapport des revenus mensuels\n" +
                    "• Rapport des patients actifs\n" +
                    "• Rapport d'activité des médecins"
            );
            alert.showAndWait();
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(
                "Erreur",
                "Erreur lors de l'accès aux rapports: " + e.getMessage(),
                Alert.AlertType.ERROR
            );
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
