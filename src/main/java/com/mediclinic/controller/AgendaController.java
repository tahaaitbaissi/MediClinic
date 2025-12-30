package com.mediclinic.controller;

import com.mediclinic.model.*;
import com.mediclinic.service.*;
import com.mediclinic.util.PermissionChecker;
import com.mediclinic.util.UserSession;
import java.io.File;
import java.net.URL;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Insets;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

public class AgendaController implements Initializable {

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private ComboBox<String> doctorCombo;

    @FXML
    private TextField searchField;

    @FXML
    private TableView<RendezVous> appointmentTable;

    @FXML
    private TableColumn<RendezVous, Long> colId;

    @FXML
    private TableColumn<RendezVous, String> colTime;

    @FXML
    private TableColumn<RendezVous, String> colPatient;

    @FXML
    private TableColumn<RendezVous, String> colDoctor;

    @FXML
    private TableColumn<RendezVous, String> colMotif;

    @FXML
    private TableColumn<RendezVous, String> colStatus;

    @FXML
    private Button newAppointmentBtn;

    @FXML
    private Label todayAppointmentsStatLabel;

    @FXML
    private Label plannedAppointmentsLabel;

    @FXML
    private Label completedAppointmentsLabel;

    @FXML
    private Label cancelledAppointmentsLabel;

    private RendezVousService rendezVousService;
    private PatientService patientService;
    private MedecinService medecinService;
    private PdfService pdfService;
    private CsvService csvService;
    private EmailService emailService;
    private ObservableList<RendezVous> appointmentList;
    private List<Medecin> doctors;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Check authentication
        if (!UserSession.isAuthenticated()) {
            showAlert(
                "Erreur",
                "Vous devez être connecté pour accéder à cette page.",
                Alert.AlertType.ERROR
            );
            return;
        }

        // Check permission to access agenda page
        try {
            if (
                !PermissionChecker.canAccessPage(
                    UserSession.getInstance().getUser().getRole(),
                    "agenda"
                )
            ) {
                showAlert(
                    "Accès refusé",
                    "Vous n'avez pas la permission d'accéder à cette page.",
                    Alert.AlertType.WARNING
                );
                return;
            }
        } catch (Exception e) {
            showAlert(
                "Erreur",
                "Erreur de vérification des permissions: " + e.getMessage(),
                Alert.AlertType.ERROR
            );
            return;
        }

        rendezVousService = new RendezVousService();
        patientService = new PatientService();
        medecinService = new MedecinService();
        pdfService = new PdfService();
        csvService = new CsvService();
        emailService = new EmailService();

        setupTableColumns();
        setupFilters();
        setupRoleBasedUI();
        loadAppointments();
        updateStatistics();
    }

    private void updateStatistics() {
        try {
            List<RendezVous> allAppointments =
                rendezVousService.findAllForCurrentUser();
            LocalDate today = LocalDate.now();

            // Count today's appointments
            long todayCount = allAppointments
                .stream()
                .filter(
                    rdv ->
                        rdv.getDateHeureDebut() != null &&
                        rdv.getDateHeureDebut().toLocalDate().equals(today)
                )
                .count();

            // Count by status
            long plannedCount = allAppointments
                .stream()
                .filter(
                    rdv ->
                        rdv.getStatus() == RendezVousStatus.PLANIFIE ||
                        rdv.getStatus() == RendezVousStatus.CONFIRME
                )
                .count();

            long completedCount = allAppointments
                .stream()
                .filter(rdv -> rdv.getStatus() == RendezVousStatus.TERMINE)
                .count();

            long cancelledCount = allAppointments
                .stream()
                .filter(rdv -> rdv.getStatus() == RendezVousStatus.ANNULE)
                .count();

            // Update labels
            if (todayAppointmentsStatLabel != null) {
                todayAppointmentsStatLabel.setText(String.valueOf(todayCount));
            }
            if (plannedAppointmentsLabel != null) {
                plannedAppointmentsLabel.setText(String.valueOf(plannedCount));
            }
            if (completedAppointmentsLabel != null) {
                completedAppointmentsLabel.setText(
                    String.valueOf(completedCount)
                );
            }
            if (cancelledAppointmentsLabel != null) {
                cancelledAppointmentsLabel.setText(
                    String.valueOf(cancelledCount)
                );
            }
        } catch (Exception e) {
            System.err.println("Error updating statistics: " + e.getMessage());
        }
    }

    private void setupRoleBasedUI() {
        try {
            // Hide "New Appointment" button for MEDECIN
            if (newAppointmentBtn != null) {
                boolean canCreate = rendezVousService.canCreateAppointment();
                newAppointmentBtn.setVisible(canCreate);
                newAppointmentBtn.setManaged(canCreate);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        colTime.setCellValueFactory(cellData -> {
            LocalDateTime debut = cellData.getValue().getDateHeureDebut();
            LocalDateTime fin = cellData.getValue().getDateHeureFin();
            if (debut != null && fin != null) {
                DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
                    "dd/MM HH:mm"
                );
                return new javafx.beans.property.SimpleStringProperty(
                    debut.format(formatter) +
                        " - " +
                        fin.format(DateTimeFormatter.ofPattern("HH:mm"))
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
        colActions.setPrefWidth(450);
        colActions.setCellFactory(param ->
            new TableCell<RendezVous, Void>() {
                private final Button viewBtn = new Button("Détails");
                private final Button confirmBtn = new Button("Confirmer");
                private final Button completeBtn = new Button("Terminer");
                private final Button cancelBtn = new Button("Annuler");
                private final Button reminderBtn = new Button("Rappel");
                private final Button consultBtn = new Button("Consultation");

                {
                    viewBtn.getStyleClass().addAll("btn-primary", "btn-actions");
                    confirmBtn.getStyleClass().addAll("btn-success", "btn-actions");
                    completeBtn.getStyleClass().addAll("btn-success", "btn-actions");
                    cancelBtn.getStyleClass().addAll("btn-danger", "btn-actions");
                    reminderBtn.getStyleClass().addAll("btn-warning", "btn-actions");
                    consultBtn.getStyleClass().addAll("btn-info", "btn-actions");

                    viewBtn.setOnAction(event -> {
                        RendezVous rdv = getTableView()
                            .getItems()
                            .get(getIndex());
                        showAppointmentDetails(rdv);
                    });

                    confirmBtn.setOnAction(event -> {
                        RendezVous rdv = getTableView()
                            .getItems()
                            .get(getIndex());
                        changeStatus(rdv, RendezVousStatus.CONFIRME);
                    });

                    completeBtn.setOnAction(event -> {
                        RendezVous rdv = getTableView()
                            .getItems()
                            .get(getIndex());
                        completeAppointment(rdv);
                    });

                    cancelBtn.setOnAction(event -> {
                        RendezVous rdv = getTableView()
                            .getItems()
                            .get(getIndex());
                        changeStatus(rdv, RendezVousStatus.ANNULE);
                    });

                    reminderBtn.setOnAction(event -> {
                        RendezVous rdv = getTableView()
                            .getItems()
                            .get(getIndex());
                        sendManualReminder(rdv);
                    });
                    consultBtn.setOnAction(event -> {
                        RendezVous rdv = getTableView().getItems().get(getIndex());
                        openConsultationEditorForRdv(rdv);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        RendezVous rdv = getTableView()
                            .getItems()
                            .get(getIndex());
                        RendezVousStatus status = rdv.getStatus();

                        // Toujours afficher le bouton voir
                        HBox buttons = new HBox(5);
                        buttons.getChildren().add(viewBtn);

                        // Check if current user can modify this appointment
                        boolean canModify = true;
                        try {
                            if (UserSession.isAuthenticated()) {
                                canModify =
                                    rendezVousService.canModifyAppointment(
                                        rdv.getId()
                                    );
                            } else {
                                canModify = false;
                            }
                        } catch (Exception e) {
                            canModify = false;
                        }

                        // Only show action buttons if user can modify
                        if (canModify) {
                            // Afficher les boutons selon le statut actuel
                            // PLANIFIE → peut confirmer ou annuler
                            // CONFIRME → peut terminer ou annuler
                            // TERMINE, ANNULE → aucune action (statuts finaux)
                            if (status == RendezVousStatus.PLANIFIE) {
                                buttons
                                    .getChildren()
                                    .addAll(confirmBtn, reminderBtn, cancelBtn);
                            } else if (status == RendezVousStatus.CONFIRME) {
                                buttons
                                    .getChildren()
                                    .addAll(
                                        completeBtn,
                                        reminderBtn,
                                        cancelBtn
                                    );
                            } else if (status == RendezVousStatus.TERMINE) {
                                // Do not show the "Consultation" action to secretaries (Role.SEC)
                                try {
                                    if (
                                        !UserSession.isAuthenticated() ||
                                        UserSession.getInstance().getUser().getRole() != Role.SEC
                                    ) {
                                        buttons.getChildren().add(consultBtn);
                                    }
                                } catch (Exception e) {
                                    // If we cannot determine the role, fall back to showing the button
                                    buttons.getChildren().add(consultBtn);
                                }
                            }
                        }
                        // Pour TERMINE et ANNULE, ou si l'utilisateur ne peut pas modifier, seul le bouton voir est affiché

                        setGraphic(buttons);
                    }
                }
            }
        );

        appointmentTable.getColumns().add(colActions);
    }

    private void setupFilters() {
        try {
            // For doctors and secretaries, hide the doctor filter
            // (they only see appointments for their associated doctor)
            if (
                UserSession.isAuthenticated() &&
                (UserSession.getInstance().getUser().getRole() == Role.MEDECIN || UserSession.getInstance().getUser().getRole() == Role.SEC)
            ) {
                doctorCombo.setVisible(false);
                doctorCombo.setManaged(false);
                doctors = List.of();
            } else {
                doctors = medecinService.findAll();
                ObservableList<String> doctorNames =
                    FXCollections.observableArrayList();
                doctorNames.add("Tous les médecins");
                doctorNames.addAll(
                    doctors
                        .stream()
                        .map(Medecin::getNomComplet)
                        .collect(Collectors.toList())
                );

                doctorCombo.setItems(doctorNames);
                doctorCombo.setValue("Tous les médecins");
            }
        } catch (Exception e) {
            doctorCombo.setItems(
                FXCollections.observableArrayList("Tous les médecins")
            );
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
            // Use findAllForCurrentUser() to apply role-based filtering
            List<RendezVous> allAppointments =
                rendezVousService.findAllForCurrentUser();

            appointmentList = FXCollections.observableArrayList(
                allAppointments
            );
            appointmentTable.setItems(appointmentList);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(
                "Erreur",
                "Erreur lors du chargement des rendez-vous: " + e.getMessage(),
                Alert.AlertType.ERROR
            );
        }
    }

    private void filterAppointments() {
        try {
            // Use findAllForCurrentUser() to respect role-based filtering
            List<RendezVous> allAppointments =
                rendezVousService.findAllForCurrentUser();

            // Appliquer les filtres
            List<RendezVous> filtered = allAppointments
                .stream()
                .filter(rdv -> {
                    // Filtre par médecin
                    String selectedDoctor = doctorCombo.getValue();
                    if (
                        selectedDoctor != null &&
                        !selectedDoctor.equals("Tous les médecins")
                    ) {
                        if (
                            rdv.getMedecin() == null ||
                            !rdv
                                .getMedecin()
                                .getNomComplet()
                                .equals(selectedDoctor)
                        ) {
                            return false;
                        }
                    }

                    // Filtre par date de début
                    LocalDate startDate = startDatePicker.getValue();
                    if (startDate != null && rdv.getDateHeureDebut() != null) {
                        if (
                            rdv
                                .getDateHeureDebut()
                                .toLocalDate()
                                .isBefore(startDate)
                        ) {
                            return false;
                        }
                    }

                    // Filtre par date de fin
                    LocalDate endDate = endDatePicker.getValue();
                    if (endDate != null && rdv.getDateHeureDebut() != null) {
                        if (
                            rdv
                                .getDateHeureDebut()
                                .toLocalDate()
                                .isAfter(endDate)
                        ) {
                            return false;
                        }
                    }

                    return true;
                })
                .collect(Collectors.toList());

            appointmentList = FXCollections.observableArrayList(filtered);
            appointmentTable.setItems(appointmentList);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(
                "Erreur",
                "Erreur lors du filtrage des rendez-vous: " + e.getMessage(),
                Alert.AlertType.ERROR
            );
        }
    }

    @FXML
    private void handleNewAppointment() {
        // Check permission
        if (!rendezVousService.canCreateAppointment()) {
            showAlert(
                "Accès refusé",
                "Vous n'avez pas la permission de créer un rendez-vous.",
                Alert.AlertType.WARNING
            );
            return;
        }

        try {
            Dialog<RendezVous> dialog = new Dialog<>();
            dialog.setTitle("Nouveau Rendez-vous");
            dialog.setHeaderText("Planifier un nouveau rendez-vous");

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

            // Patient selection - use filtered list for doctors
            ComboBox<Patient> patientCombo = new ComboBox<>();
            patientCombo.setItems(
                FXCollections.observableArrayList(
                    patientService.findAllForCurrentUser()
                )
            );
            patientCombo.setPromptText("Sélectionner un patient");
            patientCombo.setConverter(
                new javafx.util.StringConverter<Patient>() {
                    @Override
                    public String toString(Patient patient) {
                        return patient != null ? patient.getNomComplet() : "";
                    }

                    @Override
                    public Patient fromString(String string) {
                        return null;
                    }
                }
            );

            // Doctor selection
            ComboBox<Medecin> doctorComboBox = new ComboBox<>();
            doctorComboBox.setItems(
                FXCollections.observableArrayList(medecinService.findAll())
            );
            doctorComboBox.setPromptText("Sélectionner un médecin");
            doctorComboBox.setConverter(
                new javafx.util.StringConverter<Medecin>() {
                    @Override
                    public String toString(Medecin medecin) {
                        return medecin != null
                            ? medecin.getNomComplet() +
                              " (" +
                              medecin.getSpecialite() +
                              ")"
                            : "";
                    }

                    @Override
                    public Medecin fromString(String string) {
                        return null;
                    }
                }
            );

            // Pre-select and disable doctor selection for MEDECIN and SEC users
            UserSession session = UserSession.getInstance();
            Role role = session.getUser().getRole();
            if (role == Role.MEDECIN || role == Role.SEC) {
                Medecin medecin = UserSession.getInstance()
                    .getUser()
                    .getMedecin();
                if (medecin != null) {
                    // Reload the Medecin from database to avoid LazyInitializationException
                    Medecin reloadedMedecin = medecinService.findById(
                        medecin.getId()
                    );
                    if (reloadedMedecin != null) {
                        doctorComboBox.setValue(reloadedMedecin);
                        doctorComboBox.setDisable(true); // Disable doctor selection for doctors and secretaries
                    } else {
                        showAlert(
                            "Erreur",
                            "Médecin non trouvé.",
                            Alert.AlertType.ERROR
                        );
                        return;
                    }
                } else if (role == Role.SEC) {
                    // SEC must have an associated doctor
                    showAlert(
                        "Erreur",
                        "Secrétaire non associée à un médecin.",
                        Alert.AlertType.ERROR
                    );
                    return;
                }
            }

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

            ButtonType saveButtonType = new ButtonType(
                "Enregistrer",
                ButtonBar.ButtonData.OK_DONE
            );
            dialog
                .getDialogPane()
                .getButtonTypes()
                .addAll(saveButtonType, ButtonType.CANCEL);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    try {
                        LocalDate date = datePicker.getValue();
                        LocalTime startTime = LocalTime.parse(
                            startTimeField.getText()
                        );
                        LocalTime endTime = LocalTime.parse(
                            endTimeField.getText()
                        );

                        RendezVous rdv = new RendezVous();
                        rdv.setPatient(patientCombo.getValue());
                        rdv.setMedecin(doctorComboBox.getValue());
                        rdv.setDateHeureDebut(
                            LocalDateTime.of(date, startTime)
                        );
                        rdv.setDateHeureFin(LocalDateTime.of(date, endTime));
                        rdv.setMotif(motifField.getText());

                        return rdv;
                    } catch (Exception e) {
                        showAlert(
                            "Erreur",
                            "Format de données invalide: " + e.getMessage(),
                            Alert.AlertType.ERROR
                        );
                        return null;
                    }
                }
                return null;
            });

            dialog
                .showAndWait()
                .ifPresent(rdv -> {
                    try {
                        RendezVous savedRdv =
                            rendezVousService.planifierRendezVous(rdv);
                        loadAppointments();
                        updateStatistics();

                        // Send confirmation email
                        sendAppointmentConfirmationEmail(savedRdv);

                        showAlert(
                            "Succès",
                            "Rendez-vous créé avec succès!",
                            Alert.AlertType.INFORMATION
                        );
                    } catch (IllegalStateException e) {
                        // Conflict detected - offer smart suggestion
                        if (e.getMessage().contains("Collision détectée")) {
                            handleConflictWithSuggestion(
                                rdv,
                                patientCombo,
                                doctorComboBox,
                                datePicker,
                                startTimeField,
                                endTimeField,
                                motifField
                            );
                        } else {
                            showAlert(
                                "Erreur",
                                e.getMessage(),
                                Alert.AlertType.ERROR
                            );
                        }
                    } catch (Exception e) {
                        showAlert(
                            "Erreur",
                            "Erreur lors de la création: " + e.getMessage(),
                            Alert.AlertType.ERROR
                        );
                    }
                });
        } catch (Exception e) {
            showAlert(
                "Erreur",
                "Erreur lors de l'ouverture du formulaire: " + e.getMessage(),
                Alert.AlertType.ERROR
            );
        }
    }

    /**
     * Handle appointment conflict with smart next slot suggestion
     */
    private void handleConflictWithSuggestion(
        RendezVous rdv,
        ComboBox<Patient> patientCombo,
        ComboBox<Medecin> doctorComboBox,
        DatePicker datePicker,
        TextField startTimeField,
        TextField endTimeField,
        TextField motifField
    ) {
        try {
            // Calculate duration from the original request
            long durationMinutes = java.time.Duration.between(
                rdv.getDateHeureDebut(),
                rdv.getDateHeureFin()
            ).toMinutes();

            // Find next available slot
            LocalDateTime nextSlot = rendezVousService.findNextAvailableSlot(
                rdv.getMedecin(),
                rdv.getDateHeureDebut(),
                (int) durationMinutes
            );

            if (nextSlot != null) {
                // Format the suggestion
                LocalDate suggestedDate = nextSlot.toLocalDate();
                LocalTime suggestedStartTime = nextSlot.toLocalTime();
                LocalTime suggestedEndTime = suggestedStartTime.plusMinutes(
                    durationMinutes
                );

                String dayName = nextSlot.getDayOfWeek().toString();
                String formattedDate = suggestedDate.format(
                    java.time.format.DateTimeFormatter.ofPattern("dd/MM/yyyy")
                );
                String formattedTime = suggestedStartTime.format(
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                );

                // Show confirmation dialog with suggestion
                Alert suggestionAlert = new Alert(Alert.AlertType.CONFIRMATION);
                suggestionAlert.setTitle(
                    "Créneau Occupé - Suggestion Intelligente"
                );
                suggestionAlert.setHeaderText(
                    "Le créneau demandé est déjà réservé pour Dr. " +
                        rdv.getMedecin().getNomComplet()
                );
                suggestionAlert.setContentText(
                    "📅 Créneau suggéré:\n\n" +
                        "Date: " +
                        dayName +
                        " " +
                        formattedDate +
                        "\n" +
                        "Heure: " +
                        formattedTime +
                        " - " +
                        suggestedEndTime.format(
                            java.time.format.DateTimeFormatter.ofPattern(
                                "HH:mm"
                            )
                        ) +
                        "\n" +
                        "Durée: " +
                        durationMinutes +
                        " minutes\n\n" +
                        "Voulez-vous planifier le rendez-vous à ce créneau?"
                );

                ButtonType yesButton = new ButtonType(
                    "Oui, Planifier",
                    ButtonBar.ButtonData.YES
                );
                ButtonType noButton = new ButtonType(
                    "Non, Choisir Manuellement",
                    ButtonBar.ButtonData.NO
                );
                ButtonType cancelButton = new ButtonType(
                    "Annuler",
                    ButtonBar.ButtonData.CANCEL_CLOSE
                );

                suggestionAlert
                    .getButtonTypes()
                    .setAll(yesButton, noButton, cancelButton);

                suggestionAlert
                    .showAndWait()
                    .ifPresent(response -> {
                        if (response == yesButton) {
                            // User accepted the suggestion - create appointment with suggested time
                            try {
                                rdv.setDateHeureDebut(nextSlot);
                                rdv.setDateHeureFin(
                                    nextSlot.plusMinutes(durationMinutes)
                                );
                                RendezVous savedRdv =
                                    rendezVousService.planifierRendezVous(rdv);
                                loadAppointments();
                                updateStatistics();

                                // Send confirmation email
                                sendAppointmentConfirmationEmail(savedRdv);

                                showAlert(
                                    "Succès",
                                    "Rendez-vous créé avec succès au créneau suggéré!",
                                    Alert.AlertType.INFORMATION
                                );
                            } catch (Exception ex) {
                                showAlert(
                                    "Erreur",
                                    "Erreur lors de la création: " +
                                        ex.getMessage(),
                                    Alert.AlertType.ERROR
                                );
                            }
                        } else if (response == noButton) {
                            // User wants to choose manually - reopen dialog with suggested values pre-filled
                            handleNewAppointmentWithPreset(
                                rdv.getPatient(),
                                rdv.getMedecin(),
                                suggestedDate,
                                suggestedStartTime,
                                suggestedEndTime,
                                rdv.getMotif()
                            );
                        }
                        // If cancel, do nothing
                    });
            } else {
                // No available slot found in next 48 hours
                Alert noSlotAlert = new Alert(Alert.AlertType.WARNING);
                noSlotAlert.setTitle("Aucun Créneau Disponible");
                noSlotAlert.setHeaderText("Le créneau demandé est occupé");
                noSlotAlert.setContentText(
                    "Aucun créneau disponible n'a été trouvé pour Dr. " +
                        rdv.getMedecin().getNomComplet() +
                        " dans les prochaines 48 heures.\n\n" +
                        "Veuillez essayer:\n" +
                        "• Un autre médecin\n" +
                        "• Une date ultérieure\n" +
                        "• Contacter le médecin directement"
                );
                noSlotAlert.showAndWait();
            }
        } catch (Exception e) {
            showAlert(
                "Erreur",
                "Erreur lors de la recherche de créneaux: " + e.getMessage(),
                Alert.AlertType.ERROR
            );
        }
    }

    /**
     * Open new appointment dialog with preset values (for manual adjustment after suggestion)
     */
    private void handleNewAppointmentWithPreset(
        Patient presetPatient,
        Medecin presetDoctor,
        LocalDate presetDate,
        LocalTime presetStartTime,
        LocalTime presetEndTime,
        String presetMotif
    ) {
        try {
            Dialog<RendezVous> dialog = new Dialog<>();
            dialog.setTitle("Nouveau Rendez-vous");
            dialog.setHeaderText(
                "Planifier un nouveau rendez-vous (Créneau Suggéré)"
            );

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

            ComboBox<Patient> patientCombo = new ComboBox<>();
            patientCombo.setItems(
                FXCollections.observableArrayList(
                    patientService.findAllForCurrentUser()
                )
            );
            patientCombo.setValue(presetPatient);
            patientCombo.setPromptText("Sélectionner un patient");
            patientCombo.setConverter(
                new javafx.util.StringConverter<Patient>() {
                    @Override
                    public String toString(Patient patient) {
                        return patient != null ? patient.getNomComplet() : "";
                    }

                    @Override
                    public Patient fromString(String string) {
                        return null;
                    }
                }
            );

            ComboBox<Medecin> doctorComboBox = new ComboBox<>();
            doctorComboBox.setItems(
                FXCollections.observableArrayList(medecinService.findAll())
            );
            doctorComboBox.setValue(presetDoctor);
            doctorComboBox.setPromptText("Sélectionner un médecin");
            doctorComboBox.setConverter(
                new javafx.util.StringConverter<Medecin>() {
                    @Override
                    public String toString(Medecin medecin) {
                        return medecin != null
                            ? medecin.getNomComplet() +
                              " (" +
                              medecin.getSpecialite() +
                              ")"
                            : "";
                    }

                    @Override
                    public Medecin fromString(String string) {
                        return null;
                    }
                }
            );

            DatePicker datePicker = new DatePicker();
            datePicker.setValue(presetDate);

            TextField startTimeField = new TextField();
            startTimeField.setText(
                presetStartTime.format(
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                )
            );
            startTimeField.setPromptText("HH:mm (ex: 09:00)");

            TextField endTimeField = new TextField();
            endTimeField.setText(
                presetEndTime.format(
                    java.time.format.DateTimeFormatter.ofPattern("HH:mm")
                )
            );
            endTimeField.setPromptText("HH:mm (ex: 10:00)");

            TextField motifField = new TextField();
            motifField.setText(presetMotif != null ? presetMotif : "");
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

            ButtonType saveButtonType = new ButtonType(
                "Enregistrer",
                ButtonBar.ButtonData.OK_DONE
            );
            dialog
                .getDialogPane()
                .getButtonTypes()
                .addAll(saveButtonType, ButtonType.CANCEL);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    try {
                        LocalDate date = datePicker.getValue();
                        LocalTime startTime = LocalTime.parse(
                            startTimeField.getText()
                        );
                        LocalTime endTime = LocalTime.parse(
                            endTimeField.getText()
                        );

                        RendezVous rdv = new RendezVous();
                        rdv.setPatient(patientCombo.getValue());
                        rdv.setMedecin(doctorComboBox.getValue());
                        rdv.setDateHeureDebut(
                            LocalDateTime.of(date, startTime)
                        );
                        rdv.setDateHeureFin(LocalDateTime.of(date, endTime));
                        rdv.setMotif(motifField.getText());

                        return rdv;
                    } catch (Exception e) {
                        showAlert(
                            "Erreur",
                            "Format de données invalide: " + e.getMessage(),
                            Alert.AlertType.ERROR
                        );
                        return null;
                    }
                }
                return null;
            });

            dialog
                .showAndWait()
                .ifPresent(rdv -> {
                    try {
                        RendezVous savedRdv =
                            rendezVousService.planifierRendezVous(rdv);
                        loadAppointments();
                        updateStatistics();

                        // Send confirmation email
                        sendAppointmentConfirmationEmail(savedRdv);

                        showAlert(
                            "Succès",
                            "Rendez-vous créé avec succès!",
                            Alert.AlertType.INFORMATION
                        );
                    } catch (IllegalStateException e) {
                        if (e.getMessage().contains("Collision détectée")) {
                            handleConflictWithSuggestion(
                                rdv,
                                patientCombo,
                                doctorComboBox,
                                datePicker,
                                startTimeField,
                                endTimeField,
                                motifField
                            );
                        } else {
                            showAlert(
                                "Erreur",
                                e.getMessage(),
                                Alert.AlertType.ERROR
                            );
                        }
                    } catch (Exception e) {
                        showAlert(
                            "Erreur",
                            "Erreur lors de la création: " + e.getMessage(),
                            Alert.AlertType.ERROR
                        );
                    }
                });
        } catch (Exception e) {
            showAlert(
                "Erreur",
                "Erreur lors de l'ouverture du formulaire: " + e.getMessage(),
                Alert.AlertType.ERROR
            );
        }
    }

    private void showAppointmentDetails(RendezVous rdv) {
        if (rdv == null) return;

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern(
            "dd/MM/yyyy HH:mm"
        );
        showAlert(
            "Détails du Rendez-vous",
            "Patient: " +
                (rdv.getPatient() != null
                    ? rdv.getPatient().getNomComplet()
                    : "N/A") +
                "\n" +
                "Médecin: " +
                (rdv.getMedecin() != null
                    ? rdv.getMedecin().getNomComplet()
                    : "N/A") +
                "\n" +
                "Date/Heure: " +
                (rdv.getDateHeureDebut() != null
                    ? rdv.getDateHeureDebut().format(formatter)
                    : "N/A") +
                "\n" +
                "Durée: " +
                rdv.getDuree().toMinutes() +
                " minutes\n" +
                "Motif: " +
                (rdv.getMotif() != null ? rdv.getMotif() : "N/A") +
                "\n" +
                "Statut: " +
                (rdv.getStatus() != null ? rdv.getStatus().name() : "N/A"),
            Alert.AlertType.INFORMATION
        );
    }

    private void changeStatus(RendezVous rdv, RendezVousStatus newStatus) {
        if (rdv == null || rdv.getId() == null) return;

        // Check permission
        try {
            if (!rendezVousService.canModifyAppointment(rdv.getId())) {
                showAlert(
                    "Accès refusé",
                    "Vous ne pouvez modifier que vos propres rendez-vous.",
                    Alert.AlertType.WARNING
                );
                return;
            }
        } catch (Exception e) {
            showAlert(
                "Erreur",
                "Erreur de vérification des permissions: " + e.getMessage(),
                Alert.AlertType.ERROR
            );
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Confirmation");
        confirm.setHeaderText("Changer le statut du rendez-vous");
        confirm.setContentText(
            "Voulez-vous vraiment changer le statut à " +
                newStatus.name() +
                " ?"
        );

        confirm
            .showAndWait()
            .ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        // Utiliser la nouvelle méthode avec ID pour éviter les entités détachées
                        rendezVousService.updateStatus(rdv.getId(), newStatus);
                        loadAppointments();
                        updateStatistics();
                        showAlert(
                            "Succès",
                            "Statut mis à jour avec succès!",
                            Alert.AlertType.INFORMATION
                        );
                    } catch (SecurityException e) {
                        showAlert(
                            "Accès refusé",
                            e.getMessage(),
                            Alert.AlertType.WARNING
                        );
                    } catch (Exception e) {
                        showAlert(
                            "Erreur",
                            "Erreur lors de la mise à jour: " + e.getMessage(),
                            Alert.AlertType.ERROR
                        );
                    }
                }
            });
    }

    private void completeAppointment(RendezVous rdv) {
        if (rdv == null) return;

        // Check permission
        try {
            if (!rendezVousService.canModifyAppointment(rdv.getId())) {
                showAlert(
                    "Accès refusé",
                    "Vous ne pouvez terminer que vos propres rendez-vous.",
                    Alert.AlertType.WARNING
                );
                return;
            }
        } catch (Exception e) {
            showAlert(
                "Erreur",
                "Erreur de vérification des permissions: " + e.getMessage(),
                Alert.AlertType.ERROR
            );
            return;
        }

        Alert confirm = new Alert(Alert.AlertType.CONFIRMATION);
        confirm.setTitle("Terminer le rendez-vous");
        confirm.setHeaderText("Marquer comme terminé");
        confirm.setContentText(
            "Cela créera automatiquement une consultation. Continuer?"
        );

        confirm
            .showAndWait()
            .ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        rendezVousService.terminerRendezVous(rdv.getId());
                        loadAppointments();
                        updateStatistics();
                        showAlert(
                            "Succès",
                            "Rendez-vous terminé et consultation créée!",
                            Alert.AlertType.INFORMATION
                        );
                        // Optionally open the consultation editor directly for doctors
                        try {
                            if (UserSession.getInstance().getUser().getRole() == Role.MEDECIN) {
                                openConsultationEditorForRdv(rdv);
                            }
                        } catch (Exception ignore) {}
                    } catch (SecurityException e) {
                        showAlert(
                            "Accès refusé",
                            e.getMessage(),
                            Alert.AlertType.WARNING
                        );
                    } catch (Exception e) {
                        showAlert(
                            "Erreur",
                            "Erreur: " + e.getMessage(),
                            Alert.AlertType.ERROR
                        );
                    }
                }
            });
    }

    private void openConsultationEditorForRdv(RendezVous rdv) {
        try {
            // Trouver la consultation par RDV avec les dépendances nécessaires
            com.mediclinic.dao.ConsultationDAO consultationDAO = new com.mediclinic.dao.ConsultationDAO();
            com.mediclinic.model.Consultation consultation = consultationDAO.findByRendezVousId(rdv.getId());
            if (consultation != null) {
                // La requête JOIN FETCH charge déjà rendezVous et patient
                com.mediclinic.controller.ConsultationEditorController.openEditor(consultation);
            } else {
                showAlert("Info", "Aucune consultation trouvée pour ce rendez-vous.", Alert.AlertType.INFORMATION);
            }
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ouvrir la consultation: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleSearch() {
        String searchText = searchField.getText();
        if (searchText == null || searchText.trim().isEmpty()) {
            appointmentTable.setItems(appointmentList);
        } else {
            ObservableList<RendezVous> filtered = appointmentList
                .stream()
                .filter(
                    rdv ->
                        (rdv.getPatient() != null &&
                            rdv
                                .getPatient()
                                .getNomComplet()
                                .toLowerCase()
                                .contains(searchText.toLowerCase())) ||
                        (rdv.getMedecin() != null &&
                            rdv
                                .getMedecin()
                                .getNomComplet()
                                .toLowerCase()
                                .contains(searchText.toLowerCase())) ||
                        (rdv.getMotif() != null &&
                            rdv
                                .getMotif()
                                .toLowerCase()
                                .contains(searchText.toLowerCase()))
                )
                .collect(
                    Collectors.toCollection(FXCollections::observableArrayList)
                );
            appointmentTable.setItems(filtered);
        }
    }

    @FXML
    private void handleRefresh() {
        loadAppointments();
        updateStatistics();
        showAlert(
            "Actualisation",
            "Agenda actualisé avec succès!",
            Alert.AlertType.INFORMATION
        );
    }

    @FXML
    private void handleWaitingList() {
        try {
            javafx.fxml.FXMLLoader loader = new javafx.fxml.FXMLLoader(getClass().getResource("/fxml/waiting_room_view.fxml"));
            javafx.scene.Parent view = loader.load();
            javafx.stage.Stage stage = new javafx.stage.Stage();
            stage.setTitle("Liste d'attente");
            stage.setScene(new javafx.scene.Scene(view, 700, 600));
            stage.show();
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'afficher la liste d'attente: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleStatistics() {
        try {
            List<RendezVous> allAppointments = rendezVousService.findAll();

            Map<String, Object> stats = new HashMap<>();
            stats.put("totalAppointments", allAppointments.size());

            Map<String, Long> byStatus = allAppointments
                .stream()
                .collect(
                    Collectors.groupingBy(
                        rdv ->
                            rdv.getStatus() != null
                                ? rdv.getStatus().name()
                                : "INCONNU",
                        Collectors.counting()
                    )
                );
            stats.put("appointmentsByStatus", byStatus);

            Map<String, Long> byDoctor = allAppointments
                .stream()
                .filter(rdv -> rdv.getMedecin() != null)
                .collect(
                    Collectors.groupingBy(
                        rdv -> rdv.getMedecin().getNomComplet(),
                        Collectors.counting()
                    )
                );
            stats.put("appointmentsByDoctor", byDoctor);

            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Statistiques");
            dialog.setHeaderText("Exporter les statistiques");

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20));

            Label infoLabel = new Label(
                "Total rendez-vous: " +
                    stats.get("totalAppointments") +
                    "\n\n" +
                    "Choisissez le format d'export:"
            );
            grid.add(infoLabel, 0, 0, 2, 1);

            ButtonType pdfButton = new ButtonType(
                "PDF",
                ButtonBar.ButtonData.OK_DONE
            );
            ButtonType csvButton = new ButtonType(
                "CSV",
                ButtonBar.ButtonData.APPLY
            );
            dialog
                .getDialogPane()
                .getButtonTypes()
                .addAll(pdfButton, csvButton, ButtonType.CANCEL);

            dialog.getDialogPane().setContent(grid);

            dialog
                .showAndWait()
                .ifPresent(response -> {
                    try {
                        FileChooser fileChooser = new FileChooser();

                        if (response == pdfButton) {
                            fileChooser.setTitle(
                                "Sauvegarder Rapport Statistiques PDF"
                            );
                            fileChooser
                                .getExtensionFilters()
                                .add(
                                    new FileChooser.ExtensionFilter(
                                        "PDF Files",
                                        "*.pdf"
                                    )
                                );
                            fileChooser.setInitialFileName(
                                "statistiques_" + LocalDate.now() + ".pdf"
                            );

                            File file = fileChooser.showSaveDialog(
                                appointmentTable.getScene().getWindow()
                            );
                            if (file != null) {
                                pdfService.generateStatisticsReport(
                                    stats,
                                    file.getAbsolutePath()
                                );
                                showAlert(
                                    "Succes",
                                    "Rapport PDF genere avec succes!",
                                    Alert.AlertType.INFORMATION
                                );
                            }
                        } else if (response == csvButton) {
                            fileChooser.setTitle("Sauvegarder Export CSV");
                            fileChooser
                                .getExtensionFilters()
                                .add(
                                    new FileChooser.ExtensionFilter(
                                        "CSV Files",
                                        "*.csv"
                                    )
                                );
                            fileChooser.setInitialFileName(
                                "rendez_vous_" + LocalDate.now() + ".csv"
                            );

                            File file = fileChooser.showSaveDialog(
                                appointmentTable.getScene().getWindow()
                            );
                            if (file != null) {
                                csvService.exportAppointments(
                                    allAppointments,
                                    file.getAbsolutePath()
                                );
                                showAlert(
                                    "Succes",
                                    "Export CSV genere avec succes!",
                                    Alert.AlertType.INFORMATION
                                );
                            }
                        }
                    } catch (Exception e) {
                        showAlert(
                            "Erreur",
                            "Erreur lors de la generation: " + e.getMessage(),
                            Alert.AlertType.ERROR
                        );
                    }
                });
        } catch (Exception e) {
            showAlert(
                "Erreur",
                "Erreur lors du chargement des statistiques: " + e.getMessage(),
                Alert.AlertType.ERROR
            );
        }
    }

    @FXML
    private void handleDailyReport() {
        try {
            Dialog<LocalDate> dateDialog = new Dialog<>();
            dateDialog.setTitle("Rapport Quotidien");
            dateDialog.setHeaderText("Selectionnez la date pour le rapport");

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new Insets(20));

            DatePicker datePicker = new DatePicker(LocalDate.now());
            grid.add(new Label("Date:"), 0, 0);
            grid.add(datePicker, 1, 0);

            dateDialog.getDialogPane().setContent(grid);
            dateDialog
                .getDialogPane()
                .getButtonTypes()
                .addAll(ButtonType.OK, ButtonType.CANCEL);

            dateDialog.setResultConverter(dialogButton -> {
                if (dialogButton == ButtonType.OK) {
                    return datePicker.getValue();
                }
                return null;
            });

            dateDialog
                .showAndWait()
                .ifPresent(selectedDate -> {
                    try {
                        List<RendezVous> dailyAppointments = rendezVousService
                            .findAll()
                            .stream()
                            .filter(
                                rdv ->
                                    rdv.getDateHeureDebut() != null &&
                                    rdv
                                        .getDateHeureDebut()
                                        .toLocalDate()
                                        .equals(selectedDate)
                            )
                            .collect(Collectors.toList());

                        Dialog<ButtonType> exportDialog = new Dialog<>();
                        exportDialog.setTitle("Export Rapport");
                        exportDialog.setHeaderText(
                            "Rendez-vous trouves: " + dailyAppointments.size()
                        );

                        GridPane exportGrid = new GridPane();
                        exportGrid.setHgap(10);
                        exportGrid.setVgap(10);
                        exportGrid.setPadding(new Insets(20));

                        Label infoLabel = new Label(
                            "Choisissez le format d'export:"
                        );
                        exportGrid.add(infoLabel, 0, 0);

                        ButtonType pdfButton = new ButtonType(
                            "PDF",
                            ButtonBar.ButtonData.OK_DONE
                        );
                        ButtonType csvButton = new ButtonType(
                            "CSV",
                            ButtonBar.ButtonData.APPLY
                        );
                        exportDialog
                            .getDialogPane()
                            .getButtonTypes()
                            .addAll(pdfButton, csvButton, ButtonType.CANCEL);
                        exportDialog.getDialogPane().setContent(exportGrid);

                        exportDialog
                            .showAndWait()
                            .ifPresent(response -> {
                                try {
                                    FileChooser fileChooser = new FileChooser();

                                    if (response == pdfButton) {
                                        fileChooser.setTitle(
                                            "Sauvegarder Rapport Quotidien PDF"
                                        );
                                        fileChooser
                                            .getExtensionFilters()
                                            .add(
                                                new FileChooser.ExtensionFilter(
                                                    "PDF Files",
                                                    "*.pdf"
                                                )
                                            );
                                        fileChooser.setInitialFileName(
                                            "rapport_quotidien_" +
                                                selectedDate +
                                                ".pdf"
                                        );

                                        File file = fileChooser.showSaveDialog(
                                            appointmentTable
                                                .getScene()
                                                .getWindow()
                                        );
                                        if (file != null) {
                                            pdfService.generateDailyReport(
                                                dailyAppointments,
                                                selectedDate,
                                                file.getAbsolutePath()
                                            );
                                            showAlert(
                                                "Succes",
                                                "Rapport PDF genere avec succes!",
                                                Alert.AlertType.INFORMATION
                                            );
                                        }
                                    } else if (response == csvButton) {
                                        fileChooser.setTitle(
                                            "Sauvegarder Export CSV"
                                        );
                                        fileChooser
                                            .getExtensionFilters()
                                            .add(
                                                new FileChooser.ExtensionFilter(
                                                    "CSV Files",
                                                    "*.csv"
                                                )
                                            );
                                        fileChooser.setInitialFileName(
                                            "rendez_vous_" +
                                                selectedDate +
                                                ".csv"
                                        );

                                        File file = fileChooser.showSaveDialog(
                                            appointmentTable
                                                .getScene()
                                                .getWindow()
                                        );
                                        if (file != null) {
                                            csvService.exportAppointments(
                                                dailyAppointments,
                                                file.getAbsolutePath()
                                            );
                                            showAlert(
                                                "Succes",
                                                "Export CSV genere avec succes!",
                                                Alert.AlertType.INFORMATION
                                            );
                                        }
                                    }
                                } catch (Exception e) {
                                    showAlert(
                                        "Erreur",
                                        "Erreur lors de la generation: " +
                                            e.getMessage(),
                                        Alert.AlertType.ERROR
                                    );
                                }
                            });
                    } catch (Exception e) {
                        showAlert(
                            "Erreur",
                            "Erreur lors du chargement des rendez-vous: " +
                                e.getMessage(),
                            Alert.AlertType.ERROR
                        );
                    }
                });
        } catch (Exception e) {
            showAlert(
                "Erreur",
                "Erreur: " + e.getMessage(),
                Alert.AlertType.ERROR
            );
        }
    }

    private void sendAppointmentConfirmationEmail(RendezVous rdv) {
        if (rdv == null || rdv.getId() == null) {
            return;
        }

        try {
            // Reload the appointment from the database to ensure all lazy-loaded relationships are available
            // This prevents LazyInitializationException when accessing patient.getEmail()
            RendezVous reloadedRdv = rendezVousService.findById(rdv.getId());
            
            if (reloadedRdv == null || reloadedRdv.getPatient() == null) {
                System.out.println("Cannot reload appointment or patient");
                return;
            }

            Patient patient = reloadedRdv.getPatient();
            if (patient.getEmail() == null || patient.getEmail().trim().isEmpty()) {
                System.out.println(
                    "Patient has no email - confirmation email not sent"
                );
                return;
            }

            emailService.sendAppointmentConfirmationWithQR(reloadedRdv);

            System.out.println(
                "Appointment confirmation email with QR code sent to: " +
                    patient.getEmail()
            );
        } catch (Exception e) {
            System.err.println(
                "Failed to send appointment confirmation email: " +
                    e.getMessage()
            );
            e.printStackTrace();
        }
    }

    private void sendManualReminder(RendezVous rdv) {
        if (rdv == null || rdv.getPatient() == null) {
            showAlert(
                "Erreur",
                "Rendez-vous ou patient invalide",
                Alert.AlertType.ERROR
            );
            return;
        }

        Patient patient = rdv.getPatient();
        if (patient.getEmail() == null || patient.getEmail().trim().isEmpty()) {
            showAlert(
                "Erreur",
                "Le patient n'a pas d'adresse email enregistrée.",
                Alert.AlertType.ERROR
            );
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Envoyer un Rappel");
        confirmAlert.setHeaderText("Confirmer l'envoi du rappel");
        confirmAlert.setContentText(
            "Envoyer un email de rappel à " +
                patient.getNomComplet() +
                " (" +
                patient.getEmail() +
                ") ?"
        );

        confirmAlert
            .showAndWait()
            .ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        emailService.sendAppointmentReminderWithQR(rdv);

                        showAlert(
                            "Succès",
                            "Email de rappel avec QR code envoyé avec succès à " +
                                patient.getEmail(),
                            Alert.AlertType.INFORMATION
                        );
                    } catch (Exception e) {
                        showAlert(
                            "Erreur",
                            "Erreur lors de l'envoi du rappel: " +
                                e.getMessage(),
                            Alert.AlertType.ERROR
                        );
                    }
                }
            });
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
