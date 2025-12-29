package com.mediclinic.controller;

import com.mediclinic.model.Patient;
import com.mediclinic.model.Role;
import com.mediclinic.service.EmailService;
import com.mediclinic.service.PatientService;
import com.mediclinic.util.PermissionChecker;
import com.mediclinic.util.UserSession;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
 

public class PatientController implements Initializable {

    @FXML
    private TableView<Patient> patientTable;

    @FXML
    private TableColumn<Patient, Long> colId;

    @FXML
    private TableColumn<Patient, String> colNomComplet;

    @FXML
    private TableColumn<Patient, String> colEmail;

    @FXML
    private TableColumn<Patient, String> colTelephone;

    @FXML
    private TableColumn<Patient, String> colDateNaissance;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> filterCombo;

    @FXML
    private Button addPatientBtn;

    @FXML
    private Label totalPatientsLabel;

    @FXML
    private Label pageInfoLabel;

    @FXML
    private Button firstPageBtn;

    @FXML
    private Button prevPageBtn;

    @FXML
    private Button nextPageBtn;

    @FXML
    private Button lastPageBtn;

    @FXML
    private HBox statsBox;

    @FXML
    private Label totalPatientsStatLabel;

    @FXML
    private Label newPatientsMonthLabel;

    @FXML
    private Label activePatientsLabel;

    @FXML
    private Label patientsWithConsultLabel;

    private PatientService patientService;
    private EmailService emailService;
    private ObservableList<Patient> patientList;
    private ObservableList<Patient> allPatients;
    private int currentPage = 1;
    private int itemsPerPage = 20;

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

        // Check permission to access patients page
        try {
            if (
                !PermissionChecker.canAccessPage(
                    UserSession.getInstance().getUser().getRole(),
                    "patients"
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

        patientService = new PatientService();
        emailService = new EmailService();
        setupTableColumns();
        setupRoleBasedUI();
        loadPatients();
        setupSearchFilter();
        updatePagination();
        updateStatistics();
    }

    private void setupRoleBasedUI() {
        try {
            // Hide/show "Add Patient" button based on role
            if (addPatientBtn != null) {
                boolean canCreate = patientService.canCreatePatient();
                addPatientBtn.setVisible(canCreate);
                addPatientBtn.setManaged(canCreate);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNomComplet.setCellValueFactory(
            new PropertyValueFactory<>("nomComplet")
        );
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colTelephone.setCellValueFactory(
            new PropertyValueFactory<>("telephone")
        );
        colDateNaissance.setCellValueFactory(
            new PropertyValueFactory<>("dateNaissance")
        );

        TableColumn<Patient, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(280);
        colActions.setCellFactory(param ->
            new TableCell<Patient, Void>() {
                private final Button editBtn = new Button("Modifier");
                private final Button deleteBtn = new Button("Supprimer");
                private final Button detailsBtn = new Button("Détails");
                private final Button dossierBtn = new Button("Dossier");

                {
                    editBtn.getStyleClass().addAll("btn-warning", "btn-actions");
                    deleteBtn.getStyleClass().addAll("btn-danger", "btn-actions");
                    detailsBtn.getStyleClass().addAll("btn-primary", "btn-actions");
                    dossierBtn.getStyleClass().addAll("btn-info", "btn-actions");

                    editBtn.setOnAction(event -> {
                        Patient patient = getTableView()
                            .getItems()
                            .get(getIndex());
                        editPatient(patient);
                    });

                    deleteBtn.setOnAction(event -> {
                        Patient patient = getTableView()
                            .getItems()
                            .get(getIndex());
                        deletePatient(patient);
                    });

                    detailsBtn.setOnAction(event -> {
                        Patient patient = getTableView()
                            .getItems()
                            .get(getIndex());
                        showPatientDetails(patient);
                    });
                    dossierBtn.setOnAction(event -> {
                        Patient patient = getTableView().getItems().get(getIndex());
                        openDossierForPatient(patient);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        try {
                            UserSession session = UserSession.getInstance();
                            Role role = session.getUser().getRole();
                            HBox buttons = new HBox(5);
                            buttons.getChildren().add(detailsBtn); // View button always visible
                            // Doctors can access dossier
                            if (role == Role.MEDECIN || role == Role.ADMIN) {
                                buttons.getChildren().add(dossierBtn);
                            }

                            // Hide edit and delete buttons for MEDECIN (read-only)
                            if (role == Role.ADMIN || role == Role.SEC) {
                                buttons.getChildren().add(editBtn);
                            }

                            // Only ADMIN can delete
                            if (role == Role.ADMIN) {
                                buttons.getChildren().add(deleteBtn);
                            }

                            setGraphic(buttons);
                        } catch (Exception e) {
                            // If error, show only view button
                            setGraphic(new HBox(5, detailsBtn));
                        }
                    }
                }
            }
        );

        patientTable.getColumns().add(colActions);
    }

    private void loadPatients() {
        try {
            // Use findAllForCurrentUser() to apply role-based filtering
            allPatients = FXCollections.observableArrayList(
                patientService.findAllForCurrentUser()
            );
            patientList = allPatients;
            updateTableWithPagination();
        } catch (Exception e) {
            showAlert(
                "Erreur",
                "Erreur lors du chargement des patients: " + e.getMessage(),
                Alert.AlertType.ERROR
            );
        }
    }

    private void updateTableWithPagination() {
        int totalItems = patientList.size();
        int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);

        // Ensure current page is valid
        if (currentPage > totalPages && totalPages > 0) {
            currentPage = totalPages;
        }
        if (currentPage < 1) {
            currentPage = 1;
        }

        // Calculate start and end indices
        int startIndex = (currentPage - 1) * itemsPerPage;
        int endIndex = Math.min(startIndex + itemsPerPage, totalItems);

        // Get page items
        if (startIndex < totalItems) {
            List<Patient> pageItems = patientList.subList(startIndex, endIndex);
            patientTable.setItems(FXCollections.observableArrayList(pageItems));
        } else {
            patientTable.setItems(FXCollections.observableArrayList());
        }
    }

    private void updatePagination() {
        int totalItems = patientList != null ? patientList.size() : 0;
        int totalPages = (int) Math.ceil((double) totalItems / itemsPerPage);

        if (totalPages == 0) totalPages = 1;

        if (pageInfoLabel != null) {
            pageInfoLabel.setText("Page " + currentPage + " / " + totalPages);
        }
        if (totalPatientsLabel != null) {
            totalPatientsLabel.setText(totalItems + " patient(s)");
        }

        // Enable/disable navigation buttons
        if (firstPageBtn != null) firstPageBtn.setDisable(currentPage <= 1);
        if (prevPageBtn != null) prevPageBtn.setDisable(currentPage <= 1);
        if (nextPageBtn != null) nextPageBtn.setDisable(
            currentPage >= totalPages
        );
        if (lastPageBtn != null) lastPageBtn.setDisable(
            currentPage >= totalPages
        );
    }

    private void updateStatistics() {
        try {
            Role role = UserSession.getInstance().getUser().getRole();

            // Only show statistics for ADMIN and SEC
            if (statsBox != null) {
                boolean canSeeStats = (role == Role.ADMIN || role == Role.SEC);
                statsBox.setVisible(canSeeStats);
                statsBox.setManaged(canSeeStats);

                if (canSeeStats && allPatients != null) {
                    if (totalPatientsStatLabel != null) {
                        totalPatientsStatLabel.setText(
                            String.valueOf(allPatients.size())
                        );
                    }

                    // Count new patients this month
                    long newThisMonth = allPatients
                        .stream()
                        .filter(p -> p.getDateNaissance() != null) // Placeholder logic
                        .count();
                    if (newPatientsMonthLabel != null) {
                        newPatientsMonthLabel.setText(
                            String.valueOf(
                                Math.min(newThisMonth, allPatients.size())
                            )
                        );
                    }

                    // Active patients (all for now)
                    if (activePatientsLabel != null) {
                        activePatientsLabel.setText(
                            String.valueOf(allPatients.size())
                        );
                    }

                    // Patients with consultations (placeholder)
                    if (patientsWithConsultLabel != null) {
                        patientsWithConsultLabel.setText("N/A");
                    }
                }
            }
        } catch (Exception e) {
            System.err.println("Error updating statistics: " + e.getMessage());
        }
    }

    @FXML
    private void handleFirstPage() {
        currentPage = 1;
        updateTableWithPagination();
        updatePagination();
    }

    @FXML
    private void handlePrevPage() {
        if (currentPage > 1) {
            currentPage--;
            updateTableWithPagination();
            updatePagination();
        }
    }

    @FXML
    private void handleNextPage() {
        int totalPages = (int) Math.ceil(
            (double) patientList.size() / itemsPerPage
        );
        if (currentPage < totalPages) {
            currentPage++;
            updateTableWithPagination();
            updatePagination();
        }
    }

    @FXML
    private void handleLastPage() {
        int totalPages = (int) Math.ceil(
            (double) patientList.size() / itemsPerPage
        );
        currentPage = Math.max(1, totalPages);
        updateTableWithPagination();
        updatePagination();
    }

    private void setupSearchFilter() {
        filterCombo.setItems(
            FXCollections.observableArrayList(
                "Tous les patients",
                "Actifs seulement",
                "Archivés"
            )
        );
        filterCombo.setValue("Tous les patients");

        searchField
            .textProperty()
            .addListener((observable, oldValue, newValue) -> {
                filterPatients(newValue);
            });
    }

    @FXML
    private void filterPatients(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            patientList = allPatients;
        } else {
            ObservableList<Patient> filteredList =
                FXCollections.observableArrayList();
            for (Patient patient : allPatients) {
                if (
                    patient
                        .getNomComplet()
                        .toLowerCase()
                        .contains(searchText.toLowerCase()) ||
                    patient
                        .getEmail()
                        .toLowerCase()
                        .contains(searchText.toLowerCase()) ||
                    patient.getTelephone().contains(searchText)
                ) {
                    filteredList.add(patient);
                }
            }
            patientList = filteredList;
        }
        currentPage = 1;
        updateTableWithPagination();
        updatePagination();
    }

    @FXML
    private void showAddPatientForm() {
        // Check permission
        if (!patientService.canCreatePatient()) {
            showAlert(
                "Accès refusé",
                "Vous n'avez pas la permission de créer un patient.",
                Alert.AlertType.WARNING
            );
            return;
        }

        try {
            Dialog<Patient> dialog = new Dialog<>();
            dialog.setTitle("Nouveau Patient");
            dialog.setHeaderText("Ajouter un nouveau patient");

            // Créer le formulaire
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

            TextField nomField = new TextField();
            nomField.setPromptText("Nom");
            TextField prenomField = new TextField();
            prenomField.setPromptText("Prénom");
            TextField emailField = new TextField();
            emailField.setPromptText("Email");
            TextField telephoneField = new TextField();
            telephoneField.setPromptText("Téléphone");
            DatePicker dateNaissancePicker = new DatePicker();
            dateNaissancePicker.setPromptText("Date de naissance");

            grid.add(new Label("Nom:"), 0, 0);
            grid.add(nomField, 1, 0);
            grid.add(new Label("Prénom:"), 0, 1);
            grid.add(prenomField, 1, 1);
            grid.add(new Label("Email:"), 0, 2);
            grid.add(emailField, 1, 2);
            grid.add(new Label("Téléphone:"), 0, 3);
            grid.add(telephoneField, 1, 3);
            grid.add(new Label("Date naissance:"), 0, 4);
            grid.add(dateNaissancePicker, 1, 4);

            dialog.getDialogPane().setContent(grid);

            ButtonType saveButtonType = new ButtonType(
                "Enregistrer",
                ButtonBar.ButtonData.OK_DONE
            );
            dialog
                .getDialogPane()
                .getButtonTypes()
                .addAll(saveButtonType, ButtonType.CANCEL);

            Node saveButton = dialog
                .getDialogPane()
                .lookupButton(saveButtonType);
            saveButton.setDisable(true);

            javafx.beans.value.ChangeListener<String> changeListener = (
                observable,
                oldValue,
                newValue
            ) -> {
                saveButton.setDisable(
                    nomField.getText().trim().isEmpty() ||
                        prenomField.getText().trim().isEmpty() ||
                        emailField.getText().trim().isEmpty() ||
                        telephoneField.getText().trim().isEmpty()
                );
            };

            nomField.textProperty().addListener(changeListener);
            prenomField.textProperty().addListener(changeListener);
            emailField.textProperty().addListener(changeListener);
            telephoneField.textProperty().addListener(changeListener);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    Patient patient = new Patient();
                    patient.setNom(nomField.getText());
                    patient.setPrenom(prenomField.getText());
                    patient.setEmail(emailField.getText());
                    patient.setTelephone(telephoneField.getText());
                    if (dateNaissancePicker.getValue() != null) {
                        patient.setDateNaissance(
                            dateNaissancePicker.getValue()
                        );
                    }
                    return patient;
                }
                return null;
            });

            java.util.Optional<Patient> result = dialog.showAndWait();
            result.ifPresent(patient -> {
                try {
                    Patient savedPatient = patientService.createPatient(
                        patient
                    );
                    loadPatients();
                    updatePagination();
                    updateStatistics();

                    // Send welcome email if patient has email
                    if (
                        savedPatient.getEmail() != null &&
                        !savedPatient.getEmail().trim().isEmpty()
                    ) {
                        try {
                            emailService.sendWelcomeEmail(
                                savedPatient.getEmail(),
                                savedPatient.getNomComplet(),
                                String.valueOf(savedPatient.getId())
                            );
                            showAlert(
                                "Succès",
                                "Patient ajouté avec succès!\n\nUn email de bienvenue a été envoyé à " +
                                    savedPatient.getEmail(),
                                Alert.AlertType.INFORMATION
                            );
                        } catch (Exception emailEx) {
                            showAlert(
                                "Succès",
                                "Patient ajouté avec succès!\n\nNote: L'email de bienvenue n'a pas pu être envoyé.",
                                Alert.AlertType.INFORMATION
                            );
                            System.err.println(
                                "Failed to send welcome email: " +
                                    emailEx.getMessage()
                            );
                        }
                    } else {
                        showAlert(
                            "Succès",
                            "Patient ajouté avec succès!",
                            Alert.AlertType.INFORMATION
                        );
                    }
                } catch (Exception e) {
                    showAlert(
                        "Erreur",
                        "Erreur lors de l'ajout: " + e.getMessage(),
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

    private void editPatient(Patient patient) {
        // Check permission
        if (!patientService.canModifyPatient()) {
            showAlert(
                "Accès refusé",
                "Vous n'avez pas la permission de modifier un patient.",
                Alert.AlertType.WARNING
            );
            return;
        }

        try {
            Dialog<Patient> dialog = new Dialog<>();
            dialog.setTitle("Modifier Patient");
            dialog.setHeaderText("Modifier les informations du patient");

            // Créer le formulaire pré-rempli
            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

            TextField nomField = new TextField(patient.getNom());
            nomField.setPromptText("Nom");
            TextField prenomField = new TextField(patient.getPrenom());
            prenomField.setPromptText("Prénom");
            TextField emailField = new TextField(patient.getEmail());
            emailField.setPromptText("Email");
            TextField telephoneField = new TextField(patient.getTelephone());
            telephoneField.setPromptText("Téléphone");
            DatePicker dateNaissancePicker = new DatePicker(
                patient.getDateNaissance()
            );
            dateNaissancePicker.setPromptText("Date de naissance");
            TextArea adresseArea = new TextArea(
                patient.getAdresse() != null ? patient.getAdresse() : ""
            );
            adresseArea.setPromptText("Adresse");
            adresseArea.setPrefRowCount(3);

            grid.add(new Label("Nom:"), 0, 0);
            grid.add(nomField, 1, 0);
            grid.add(new Label("Prénom:"), 0, 1);
            grid.add(prenomField, 1, 1);
            grid.add(new Label("Email:"), 0, 2);
            grid.add(emailField, 1, 2);
            grid.add(new Label("Téléphone:"), 0, 3);
            grid.add(telephoneField, 1, 3);
            grid.add(new Label("Date naissance:"), 0, 4);
            grid.add(dateNaissancePicker, 1, 4);
            grid.add(new Label("Adresse:"), 0, 5);
            grid.add(adresseArea, 1, 5);

            dialog.getDialogPane().setContent(grid);

            ButtonType saveButtonType = new ButtonType(
                "Enregistrer",
                ButtonBar.ButtonData.OK_DONE
            );
            dialog
                .getDialogPane()
                .getButtonTypes()
                .addAll(saveButtonType, ButtonType.CANCEL);

            Node saveButton = dialog
                .getDialogPane()
                .lookupButton(saveButtonType);
            saveButton.setDisable(false);

            javafx.beans.value.ChangeListener<String> changeListener = (
                observable,
                oldValue,
                newValue
            ) -> {
                saveButton.setDisable(
                    nomField.getText().trim().isEmpty() ||
                        prenomField.getText().trim().isEmpty() ||
                        emailField.getText().trim().isEmpty() ||
                        telephoneField.getText().trim().isEmpty()
                );
            };

            nomField.textProperty().addListener(changeListener);
            prenomField.textProperty().addListener(changeListener);
            emailField.textProperty().addListener(changeListener);
            telephoneField.textProperty().addListener(changeListener);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    patient.setNom(nomField.getText());
                    patient.setPrenom(prenomField.getText());
                    patient.setEmail(emailField.getText());
                    patient.setTelephone(telephoneField.getText());
                    patient.setAdresse(adresseArea.getText());
                    if (dateNaissancePicker.getValue() != null) {
                        patient.setDateNaissance(
                            dateNaissancePicker.getValue()
                        );
                    }
                    return patient;
                }
                return null;
            });

            java.util.Optional<Patient> result = dialog.showAndWait();
            result.ifPresent(updatedPatient -> {
                try {
                    patientService.updatePatient(updatedPatient);
                    loadPatients();
                    updatePagination();
                    updateStatistics();
                    showAlert(
                        "Succès",
                        "Patient modifié avec succès!",
                        Alert.AlertType.INFORMATION
                    );
                } catch (Exception e) {
                    showAlert(
                        "Erreur",
                        "Erreur lors de la modification: " + e.getMessage(),
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

    private void deletePatient(Patient patient) {
        // Check permission - only ADMIN can delete
        if (!patientService.canDeletePatient()) {
            showAlert(
                "Accès refusé",
                "Seul l'administrateur peut supprimer un patient.",
                Alert.AlertType.WARNING
            );
            return;
        }

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer le patient");
        alert.setContentText(
            "Êtes-vous sûr de vouloir supprimer " +
                patient.getNomComplet() +
                " ?"
        );

        java.util.Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                patientService.deletePatient(patient.getId());
                loadPatients();
                updatePagination();
                updateStatistics();
                showAlert(
                    "Succès",
                    "Patient supprimé avec succès!",
                    Alert.AlertType.INFORMATION
                );
            } catch (Exception e) {
                showAlert(
                    "Erreur",
                    "Erreur lors de la suppression: " + e.getMessage(),
                    Alert.AlertType.ERROR
                );
            }
        }
    }

    private void showPatientDetails(Patient patient) {
        showAlert(
            "Détails Patient",
            "Nom: " +
                patient.getNomComplet() +
                "\n" +
                "Email: " +
                patient.getEmail() +
                "\n" +
                "Téléphone: " +
                patient.getTelephone() +
                "\n" +
                "Date naissance: " +
                patient.getDateNaissance(),
            Alert.AlertType.INFORMATION
        );
    }

    private void openDossierForPatient(Patient patient) {
        try {
            com.mediclinic.dao.DossierMedicalDAO dossierDAO = new com.mediclinic.dao.DossierMedicalDAO();
            com.mediclinic.model.DossierMedical dossier = dossierDAO.findByPatientId(patient.getId());
            if (dossier != null) {
                // Le dossier retourné par findByPatientId fetch déjà le patient
                com.mediclinic.controller.DossierController.openForDossier(dossier);
            } else {
                showAlert("Info", "Aucun dossier médical pour ce patient.", Alert.AlertType.INFORMATION);
            }
        } catch (Exception e) {
            showAlert("Erreur", "Impossible d'ouvrir le dossier: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    @FXML
    private void handleSearch() {
        filterPatients(searchField.getText());
    }

    @FXML
    private void handleExport() {
        showAlert(
            "Export",
            "Fonction d'export à implémenter",
            Alert.AlertType.INFORMATION
        );
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
