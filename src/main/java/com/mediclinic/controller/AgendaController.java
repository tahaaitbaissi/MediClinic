package com.mediclinic.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.mediclinic.model.*;
import com.mediclinic.service.*;

import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class AgendaController implements Initializable {

    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> doctorCombo;
    @FXML private TextField searchField;
    @FXML private TableView<RendezVous> appointmentTable;
    @FXML private TableColumn<RendezVous, Long> colId;
    @FXML private TableColumn<RendezVous, String> colTime;
    @FXML private TableColumn<RendezVous, String> colPatient;
    @FXML private TableColumn<RendezVous, String> colDoctor;
    @FXML private TableColumn<RendezVous, String> colMotif;
    @FXML private TableColumn<RendezVous, String> colStatus;

    private RendezVousService rendezVousService;
    private PatientService patientService;
    private MedecinService medecinService;
    private ObservableList<RendezVous> appointmentList;
    private List<Medecin> doctors;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        rendezVousService = new RendezVousService();
        patientService = new PatientService();
        medecinService = new MedecinService();
        
        setupTableColumns();
        setupFilters();
        loadAppointments();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        
        colTime.setCellValueFactory(cellData -> {
            LocalDateTime debut = cellData.getValue().getDateHeureDebut();
            LocalDateTime fin = cellData.getValue().getDateHeureFin();
            if (debut != null && fin != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM HH:mm");
                return new javafx.beans.property.SimpleStringProperty(
                    debut.format(formatter) + " - " + fin.format(DateTimeFormatter.ofPattern("HH:mm"))
                );
            }
            return new javafx.beans.property.SimpleStringProperty("");
        });
        
        colPatient.setCellValueFactory(cellData -> {
            Patient patient = cellData.getValue().getPatient();
            return new javafx.beans.property.SimpleStringProperty(
                patient != null ? patient.getNomComplet() : "N/A"
            );
        });
        
        colDoctor.setCellValueFactory(cellData -> {
            Medecin medecin = cellData.getValue().getMedecin();
            return new javafx.beans.property.SimpleStringProperty(
                medecin != null ? medecin.getNomComplet() : "N/A"
            );
        });
        
        colMotif.setCellValueFactory(new PropertyValueFactory<>("motif"));
        
        colStatus.setCellValueFactory(cellData -> {
            RendezVousStatus status = cellData.getValue().getStatus();
            return new javafx.beans.property.SimpleStringProperty(
                status != null ? status.name() : "N/A"
            );
        });

        // Add actions column
        TableColumn<RendezVous, Void> colActions = new TableColumn<>("Actions");
        colActions.setCellFactory(param -> new TableCell<RendezVous, Void>() {
            private final Button viewBtn = new Button("👁️");
            private final Button confirmBtn = new Button("✓");
            private final Button completeBtn = new Button("✅");
            private final Button cancelBtn = new Button("❌");

            {
                viewBtn.getStyleClass().add("btn-primary");
                confirmBtn.getStyleClass().add("btn-success");
                completeBtn.getStyleClass().add("btn-warning");
                cancelBtn.getStyleClass().add("btn-danger");

                viewBtn.setOnAction(event -> {
                    RendezVous rdv = getTableView().getItems().get(getIndex());
                    showAppointmentDetails(rdv);
                });

                confirmBtn.setOnAction(event -> {
                    RendezVous rdv = getTableView().getItems().get(getIndex());
                    changeStatus(rdv, RendezVousStatus.CONFIRME);
                });

                completeBtn.setOnAction(event -> {
                    RendezVous rdv = getTableView().getItems().get(getIndex());
                    completeAppointment(rdv);
                });

                cancelBtn.setOnAction(event -> {
                    RendezVous rdv = getTableView().getItems().get(getIndex());
                    changeStatus(rdv, RendezVousStatus.ANNULE);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5, viewBtn, confirmBtn, completeBtn, cancelBtn);
                    setGraphic(buttons);
                }
            }
        });

        appointmentTable.getColumns().add(colActions);
    }

    private void setupFilters() {
        try {
            doctors = medecinService.findAll();
            ObservableList<String> doctorNames = FXCollections.observableArrayList();
            doctorNames.add("Tous les médecins");
            doctorNames.addAll(doctors.stream()
                .map(Medecin::getNomComplet)
                .collect(Collectors.toList()));
            
            doctorCombo.setItems(doctorNames);
            doctorCombo.setValue("Tous les médecins");
        } catch (Exception e) {
            doctorCombo.setItems(FXCollections.observableArrayList("Tous les médecins"));
            doctorCombo.setValue("Tous les médecins");
        }

        startDatePicker.setValue(LocalDate.now());
        endDatePicker.setValue(LocalDate.now().plusDays(7));

        // Add listeners for filtering
        doctorCombo.setOnAction(event -> filterAppointments());
        startDatePicker.setOnAction(event -> filterAppointments());
        endDatePicker.setOnAction(event -> filterAppointments());
    }

    private void loadAppointments() {
        try {
            // For now, load all appointments
            // In a real scenario, we'd filter by date range
            appointmentList = FXCollections.observableArrayList();
            appointmentTable.setItems(appointmentList);
        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors du chargement des rendez-vous: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void filterAppointments() {
        // TODO: Implement filtering based on selected doctor and date range
        loadAppointments();
    }

    @FXML
    private void handleNewAppointment() {
        try {
            Dialog<RendezVous> dialog = new Dialog<>();
            dialog.setTitle("Nouveau Rendez-vous");
            dialog.setHeaderText("Planifier un nouveau rendez-vous");

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

            // Patient selection
            ComboBox<Patient> patientCombo = new ComboBox<>();
            patientCombo.setItems(FXCollections.observableArrayList(patientService.findAll()));
            patientCombo.setPromptText("Sélectionner un patient");
            patientCombo.setConverter(new javafx.util.StringConverter<Patient>() {
                @Override
                public String toString(Patient patient) {
                    return patient != null ? patient.getNomComplet() : "";
                }
                @Override
                public Patient fromString(String string) {
                    return null;
                }
            });

            // Doctor selection
            ComboBox<Medecin> doctorComboBox = new ComboBox<>();
            doctorComboBox.setItems(FXCollections.observableArrayList(medecinService.findAll()));
            doctorComboBox.setPromptText("Sélectionner un médecin");
            doctorComboBox.setConverter(new javafx.util.StringConverter<Medecin>() {
                @Override
                public String toString(Medecin medecin) {
                    return medecin != null ? medecin.getNomComplet() + " (" + medecin.getSpecialite() + ")" : "";
                }
                @Override
                public Medecin fromString(String string) {
                    return null;
                }
            });

            DatePicker datePicker = new DatePicker();
            datePicker.setValue(LocalDate.now());
            
            TextField startTimeField = new TextField();
            startTimeField.setPromptText("HH:mm (ex: 09:00)");
            
            TextField endTimeField = new TextField();
            endTimeField.setPromptText("HH:mm (ex: 10:00)");
            
            TextField motifField = new TextField();
            motifField.setPromptText("Motif de la consultation");

            grid.add(new Label("Patient:"), 0, 0);
            grid.add(patientCombo, 1, 0);
            grid.add(new Label("Médecin:"), 0, 1);
            grid.add(doctorComboBox, 1, 1);
            grid.add(new Label("Date:"), 0, 2);
            grid.add(datePicker, 1, 2);
            grid.add(new Label("Heure début:"), 0, 3);
            grid.add(startTimeField, 1, 3);
            grid.add(new Label("Heure fin:"), 0, 4);
            grid.add(endTimeField, 1, 4);
            grid.add(new Label("Motif:"), 0, 5);
            grid.add(motifField, 1, 5);

            dialog.getDialogPane().setContent(grid);

            ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    try {
                        LocalDate date = datePicker.getValue();
                        LocalTime startTime = LocalTime.parse(startTimeField.getText());
                        LocalTime endTime = LocalTime.parse(endTimeField.getText());
                        
                        RendezVous rdv = new RendezVous();
                        rdv.setPatient(patientCombo.getValue());
                        rdv.setMedecin(doctorComboBox.getValue());
                        rdv.setDateHeureDebut(LocalDateTime.of(date, startTime));
                        rdv.setDateHeureFin(LocalDateTime.of(date, endTime));
                        rdv.setMotif(motifField.getText());
                        
                        return rdv;
                    } catch (Exception e) {
                        showAlert("Erreur", "Format de données invalide: " + e.getMessage(), Alert.AlertType.ERROR);
                        return null;
                    }
                }
                return null;
            });

            dialog.showAndWait().ifPresent(rdv -> {
                try {
                    rendezVousService.planifierRendezVous(rdv);
                    loadAppointments();
                    showAlert("Succès", "Rendez-vous créé avec succès!", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Erreur", "Erreur lors de la création: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            });

        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de l'ouverture du formulaire: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showAppointmentDetails(RendezVous rdv) {
        if (rdv == null) return;
        
        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");
        showAlert("Détails du Rendez-vous",
                "Patient: " + (rdv.getPatient() != null ? rdv.getPatient().getNomComplet() : "N/A") + "\n" +
                "Médecin: " + (rdv.getMedecin() != null ? rdv.getMedecin().getNomComplet() : "N/A") + "\n" +
                "Date/Heure: " + (rdv.getDateHeureDebut() != null ? rdv.getDateHeureDebut().format(formatter) : "N/A") + "\n" +
                "Durée: " + rdv.getDuree().toMinutes() + " minutes\n" +
                "Motif: " + (rdv.getMotif() != null ? rdv.getMotif() : "N/A") + "\n" +
                "Statut: " + (rdv.getStatus() != null ? rdv.getStatus().name() : "N/A"),
                Alert.AlertType.INFORMATION);
    }

    private void changeStatus(RendezVous rdv, RendezVousStatus newStatus) {
        if (rdv == null) return;
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Changer le statut du rendez-vous");
        confirm.setContentText("Voulez-vous vraiment changer le statut à " + newStatus.name() + " ?");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    rdv.setStatus(newStatus);
                    // Save through service would be better
                    loadAppointments();
                    showAlert("Succès", "Statut mis à jour avec succès!", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Erreur", "Erreur lors de la mise à jour: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    private void completeAppointment(RendezVous rdv) {
        if (rdv == null) return;
        
        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Terminer le rendez-vous");
        confirm.setHeaderText("Marquer comme terminé");
        confirm.setContentText("Cela créera automatiquement une consultation. Continuer?");
        
        confirm.showAndWait().ifPresent(response -> {
            if (response == ButtonType.OK) {
                try {
                    rendezVousService.terminerRendezVous(rdv.getId());
                    loadAppointments();
                    showAlert("Succès", "Rendez-vous terminé et consultation créée!", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Erreur", "Erreur: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            }
        });
    }

    @FXML
    private void handleSearch() {
        String searchText = searchField.getText();
        if (searchText == null || searchText.trim().isEmpty()) {
            appointmentTable.setItems(appointmentList);
        } else {
            ObservableList<RendezVous> filtered = appointmentList.stream()
                .filter(rdv -> 
                    (rdv.getPatient() != null && rdv.getPatient().getNomComplet().toLowerCase().contains(searchText.toLowerCase())) ||
                    (rdv.getMedecin() != null && rdv.getMedecin().getNomComplet().toLowerCase().contains(searchText.toLowerCase())) ||
                    (rdv.getMotif() != null && rdv.getMotif().toLowerCase().contains(searchText.toLowerCase()))
                )
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
            appointmentTable.setItems(filtered);
        }
    }

    @FXML
    private void handleRefresh() {
        loadAppointments();
        showAlert("Actualisation", "Agenda actualisé", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleWaitingList() {
        showAlert("Liste d'attente", "Affichage de la liste d'attente (à implémenter)", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleStatistics() {
        showAlert("Statistiques", "Affichage des statistiques (à implémenter)", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleDailyReport() {
        showAlert("Rapport", "Génération du rapport quotidien (à implémenter)", Alert.AlertType.INFORMATION);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
