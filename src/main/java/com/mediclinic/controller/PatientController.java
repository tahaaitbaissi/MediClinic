package com.mediclinic.controller;

import com.mediclinic.model.Patient;
import com.mediclinic.model.Role;
import com.mediclinic.service.EmailService;
import com.mediclinic.service.PatientService;
import com.mediclinic.service.PhotoService;
import com.mediclinic.util.DefaultAvatarGenerator;
import com.mediclinic.util.PermissionChecker;
import com.mediclinic.util.UserSession;
import java.awt.image.BufferedImage;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.geometry.Pos;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

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
    private PhotoService photoService;
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
        photoService = new PhotoService();
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
                    editBtn
                        .getStyleClass()
                        .addAll("btn-warning", "btn-actions");
                    deleteBtn
                        .getStyleClass()
                        .addAll("btn-danger", "btn-actions");
                    detailsBtn
                        .getStyleClass()
                        .addAll("btn-primary", "btn-actions");
                    dossierBtn
                        .getStyleClass()
                        .addAll("btn-info", "btn-actions");

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
                        Patient patient = getTableView()
                            .getItems()
                            .get(getIndex());
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

            // Photo section
            ImageView photoPreview = new ImageView();
            photoPreview.setFitWidth(120);
            photoPreview.setFitHeight(120);
            photoPreview.setPreserveRatio(true);
            photoPreview.setStyle(
                "-fx-border-color: #ddd; -fx-border-width: 2;"
            );

            // Set default avatar
            photoPreview.setImage(
                DefaultAvatarGenerator.generateDefaultAvatar(120)
            );

            Button takePhotoBtn = new Button("📷 Prendre Photo");
            takePhotoBtn.setStyle("-fx-font-size: 12px;");
            takePhotoBtn.setMaxWidth(Double.MAX_VALUE);

            Button uploadPhotoBtn = new Button("📁 Charger Fichier");
            uploadPhotoBtn.setStyle("-fx-font-size: 12px;");
            uploadPhotoBtn.setMaxWidth(Double.MAX_VALUE);

            VBox photoBox = new VBox(10);
            photoBox.setAlignment(Pos.CENTER);
            photoBox
                .getChildren()
                .addAll(photoPreview, takePhotoBtn, uploadPhotoBtn);

            final BufferedImage[] capturedPhoto = { null };

            takePhotoBtn.setOnAction(e -> {
                BufferedImage photo = capturePhotoDialog();
                debugLog("=== TAKE PHOTO BUTTON CLICKED ===");
                debugLog(
                    "Photo captured: " +
                        (photo != null
                            ? photo.getWidth() + "x" + photo.getHeight()
                            : "NULL")
                );
                System.out.println(
                    "DEBUG: Take Photo clicked, captured: " +
                        (photo != null
                            ? photo.getWidth() + "x" + photo.getHeight()
                            : "NULL")
                );
                if (photo != null) {
                    capturedPhoto[0] = photo;
                    debugLog(
                        "Stored in capturedPhoto[0]: " +
                            photo.getWidth() +
                            "x" +
                            photo.getHeight()
                    );
                    System.out.println("DEBUG: Stored in capturedPhoto[0]");
                    Image fxImage = photoService.convertToFXImage(photo);
                    photoPreview.setImage(fxImage);
                    debugLog("Updated preview ImageView");
                    System.out.println("DEBUG: Updated preview ImageView");
                } else {
                    debugLog("ERROR: Photo capture returned NULL!");
                    System.err.println("DEBUG: Photo capture returned NULL!");
                }
            });

            uploadPhotoBtn.setOnAction(e -> {
                BufferedImage photo = uploadPhotoDialog();
                debugLog("=== UPLOAD PHOTO BUTTON CLICKED ===");
                debugLog(
                    "Photo uploaded: " +
                        (photo != null
                            ? photo.getWidth() + "x" + photo.getHeight()
                            : "NULL")
                );
                if (photo != null) {
                    capturedPhoto[0] = photo;
                    debugLog(
                        "Stored in capturedPhoto[0]: " +
                            photo.getWidth() +
                            "x" +
                            photo.getHeight()
                    );
                    Image fxImage = photoService.convertToFXImage(photo);
                    photoPreview.setImage(fxImage);
                    debugLog("Updated preview ImageView");
                } else {
                    debugLog("ERROR: Upload returned NULL!");
                }
            });

            grid.add(new Label("Photo:"), 0, 0);
            grid.add(photoBox, 1, 0);
            grid.add(new Label("Nom:"), 0, 1);
            grid.add(nomField, 1, 1);
            grid.add(new Label("Prénom:"), 0, 2);
            grid.add(prenomField, 1, 2);
            grid.add(new Label("Email:"), 0, 3);
            grid.add(emailField, 1, 3);
            grid.add(new Label("Téléphone:"), 0, 4);
            grid.add(telephoneField, 1, 4);
            grid.add(new Label("Date naissance:"), 0, 5);
            grid.add(dateNaissancePicker, 1, 5);

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

                    // Save photo if captured
                    debugLog("=== SAVE PATIENT BUTTON CLICKED ===");
                    debugLog(
                        "Patient created with ID: " + savedPatient.getId()
                    );
                    debugLog("Checking capturedPhoto[0]...");
                    System.out.println(
                        "DEBUG: Saving patient, checking for photo..."
                    );
                    System.out.println(
                        "DEBUG: capturedPhoto[0] = " +
                            (capturedPhoto[0] != null
                                ? capturedPhoto[0].getWidth() +
                                  "x" +
                                  capturedPhoto[0].getHeight()
                                : "NULL")
                    );

                    if (capturedPhoto[0] != null) {
                        debugLog(
                            "capturedPhoto[0] is NOT NULL: " +
                                capturedPhoto[0].getWidth() +
                                "x" +
                                capturedPhoto[0].getHeight()
                        );
                        debugLog(
                            "Calling photoService.savePatientPhoto for patient ID: " +
                                savedPatient.getId()
                        );
                        System.out.println(
                            "DEBUG: Calling savePatientPhoto for patient ID: " +
                                savedPatient.getId()
                        );
                        boolean saved = photoService.savePatientPhoto(
                            savedPatient.getId(),
                            capturedPhoto[0]
                        );
                        debugLog(
                            "Photo save result: " +
                                (saved ? "SUCCESS" : "FAILED")
                        );
                        System.out.println(
                            "DEBUG: Photo save result: " +
                                (saved ? "SUCCESS" : "FAILED")
                        );
                    } else {
                        debugLog(
                            "ERROR: capturedPhoto[0] is NULL - photo will NOT be saved!"
                        );
                        System.out.println(
                            "DEBUG: No photo to save (capturedPhoto[0] is null)"
                        );
                    }

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

            // Photo section
            ImageView photoPreview = new ImageView();
            photoPreview.setFitWidth(120);
            photoPreview.setFitHeight(120);
            photoPreview.setPreserveRatio(true);
            photoPreview.setStyle(
                "-fx-border-color: #ddd; -fx-border-width: 2;"
            );

            // Load existing photo or show default
            Image existingPhoto = photoService.loadPatientPhoto(
                patient.getId()
            );
            if (existingPhoto != null) {
                photoPreview.setImage(existingPhoto);
            } else {
                photoPreview.setImage(
                    DefaultAvatarGenerator.generateDefaultAvatar(120)
                );
            }

            Button takePhotoBtn = new Button("📷 Prendre Photo");
            takePhotoBtn.setStyle("-fx-font-size: 12px;");
            takePhotoBtn.setMaxWidth(Double.MAX_VALUE);

            Button uploadPhotoBtn = new Button("📁 Charger Fichier");
            uploadPhotoBtn.setStyle("-fx-font-size: 12px;");
            uploadPhotoBtn.setMaxWidth(Double.MAX_VALUE);

            Button deletePhotoBtn = new Button("🗑 Supprimer Photo");
            deletePhotoBtn.setStyle("-fx-font-size: 12px;");
            deletePhotoBtn.setMaxWidth(Double.MAX_VALUE);
            deletePhotoBtn.setDisable(
                !photoService.hasPatientPhoto(patient.getId())
            );

            VBox photoBox = new VBox(10);
            photoBox.setAlignment(Pos.CENTER);
            photoBox
                .getChildren()
                .addAll(
                    photoPreview,
                    takePhotoBtn,
                    uploadPhotoBtn,
                    deletePhotoBtn
                );

            final BufferedImage[] capturedPhoto = { null };
            final boolean[] photoDeleted = { false };

            takePhotoBtn.setOnAction(e -> {
                BufferedImage photo = capturePhotoDialog();
                debugLog("=== EDIT PATIENT - TAKE PHOTO CLICKED ===");
                debugLog(
                    "Photo captured: " +
                        (photo != null
                            ? photo.getWidth() + "x" + photo.getHeight()
                            : "NULL")
                );
                if (photo != null) {
                    capturedPhoto[0] = photo;
                    photoDeleted[0] = false;
                    debugLog(
                        "Stored in capturedPhoto[0]: " +
                            photo.getWidth() +
                            "x" +
                            photo.getHeight()
                    );
                    Image fxImage = photoService.convertToFXImage(photo);
                    photoPreview.setImage(fxImage);
                    deletePhotoBtn.setDisable(false);
                    debugLog("Updated preview and enabled delete button");
                } else {
                    debugLog("ERROR: Photo capture returned NULL!");
                }
            });

            uploadPhotoBtn.setOnAction(e -> {
                BufferedImage photo = uploadPhotoDialog();
                debugLog("=== EDIT PATIENT - UPLOAD PHOTO CLICKED ===");
                debugLog(
                    "Photo uploaded: " +
                        (photo != null
                            ? photo.getWidth() + "x" + photo.getHeight()
                            : "NULL")
                );
                if (photo != null) {
                    capturedPhoto[0] = photo;
                    photoDeleted[0] = false;
                    debugLog(
                        "Stored in capturedPhoto[0]: " +
                            photo.getWidth() +
                            "x" +
                            photo.getHeight()
                    );
                    Image fxImage = photoService.convertToFXImage(photo);
                    photoPreview.setImage(fxImage);
                    deletePhotoBtn.setDisable(false);
                    debugLog("Updated preview and enabled delete button");
                } else {
                    debugLog("ERROR: Upload returned NULL!");
                }
            });

            deletePhotoBtn.setOnAction(e -> {
                photoDeleted[0] = true;
                capturedPhoto[0] = null;
                photoPreview.setImage(
                    DefaultAvatarGenerator.generateDefaultAvatar(120)
                );
                deletePhotoBtn.setDisable(true);
            });

            grid.add(new Label("Photo:"), 0, 0);
            grid.add(photoBox, 1, 0);
            grid.add(new Label("Nom:"), 0, 1);
            grid.add(nomField, 1, 1);
            grid.add(new Label("Prénom:"), 0, 2);
            grid.add(prenomField, 1, 2);
            grid.add(new Label("Email:"), 0, 3);
            grid.add(emailField, 1, 3);
            grid.add(new Label("Téléphone:"), 0, 4);
            grid.add(telephoneField, 1, 4);
            grid.add(new Label("Date naissance:"), 0, 5);
            grid.add(dateNaissancePicker, 1, 5);
            grid.add(new Label("Adresse:"), 0, 6);
            grid.add(adresseArea, 1, 6);

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

                    // Handle photo updates
                    debugLog("=== EDIT PATIENT - SAVE BUTTON CLICKED ===");
                    debugLog("Patient ID: " + patient.getId());
                    debugLog("photoDeleted[0] = " + photoDeleted[0]);
                    debugLog(
                        "capturedPhoto[0] = " +
                            (capturedPhoto[0] != null
                                ? capturedPhoto[0].getWidth() +
                                  "x" +
                                  capturedPhoto[0].getHeight()
                                : "NULL")
                    );

                    if (photoDeleted[0]) {
                        debugLog(
                            "Deleting photo for patient " + patient.getId()
                        );
                        photoService.deletePatientPhoto(patient.getId());
                        debugLog("Photo deleted");
                    } else if (capturedPhoto[0] != null) {
                        debugLog(
                            "Saving photo for patient " +
                                patient.getId() +
                                " (" +
                                capturedPhoto[0].getWidth() +
                                "x" +
                                capturedPhoto[0].getHeight() +
                                ")"
                        );
                        debugLog(
                            "Image type before save: " +
                                capturedPhoto[0].getType()
                        );
                        boolean saved = photoService.savePatientPhoto(
                            patient.getId(),
                            capturedPhoto[0]
                        );
                        debugLog(
                            "Photo save result: " +
                                (saved ? "SUCCESS" : "FAILED")
                        );
                    } else {
                        debugLog("No photo changes - keeping existing photo");
                    }

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
        Dialog<Void> dialog = new Dialog<>();
        dialog.setTitle("Détails Patient");
        dialog.setHeaderText("Informations du patient");

        GridPane grid = new GridPane();
        grid.setHgap(15);
        grid.setVgap(10);
        grid.setPadding(new javafx.geometry.Insets(20));

        // Photo section
        ImageView photoView = new ImageView();
        photoView.setFitWidth(150);
        photoView.setFitHeight(150);
        photoView.setPreserveRatio(true);
        photoView.setStyle(
            "-fx-border-color: #ddd; -fx-border-width: 2; -fx-border-radius: 5;"
        );

        // Load patient photo or show default
        Image photo = photoService.loadPatientPhoto(patient.getId());
        if (photo != null) {
            photoView.setImage(photo);
        } else {
            photoView.setImage(
                DefaultAvatarGenerator.generateDefaultAvatar(150)
            );
        }

        VBox photoBox = new VBox(10);
        photoBox.setAlignment(Pos.CENTER);
        photoBox.getChildren().add(photoView);

        // Details section
        VBox detailsBox = new VBox(8);
        detailsBox
            .getChildren()
            .addAll(
                createDetailLabel("ID:", patient.getId().toString()),
                createDetailLabel("Nom complet:", patient.getNomComplet()),
                createDetailLabel("Email:", patient.getEmail()),
                createDetailLabel("Téléphone:", patient.getTelephone()),
                createDetailLabel(
                    "Date de naissance:",
                    patient.getDateNaissance() != null
                        ? patient.getDateNaissance().toString()
                        : "Non renseignée"
                ),
                createDetailLabel(
                    "Adresse:",
                    patient.getAdresse() != null &&
                            !patient.getAdresse().isEmpty()
                        ? patient.getAdresse()
                        : "Non renseignée"
                )
            );

        grid.add(photoBox, 0, 0);
        grid.add(detailsBox, 1, 0);

        dialog.getDialogPane().setContent(grid);
        dialog.getDialogPane().getButtonTypes().add(ButtonType.OK);

        dialog.showAndWait();
    }

    private HBox createDetailLabel(String label, String value) {
        HBox box = new HBox(5);
        Label labelNode = new Label(label);
        labelNode.setStyle("-fx-font-weight: bold; -fx-min-width: 150px;");
        Label valueNode = new Label(value);
        box.getChildren().addAll(labelNode, valueNode);
        return box;
    }

    private void openDossierForPatient(Patient patient) {
        try {
            com.mediclinic.dao.DossierMedicalDAO dossierDAO =
                new com.mediclinic.dao.DossierMedicalDAO();
            com.mediclinic.model.DossierMedical dossier =
                dossierDAO.findByPatientId(patient.getId());
            if (dossier != null) {
                // Le dossier retourné par findByPatientId fetch déjà le patient
                com.mediclinic.controller.DossierController.openForDossier(
                    dossier
                );
            } else {
                showAlert(
                    "Info",
                    "Aucun dossier médical pour ce patient.",
                    Alert.AlertType.INFORMATION
                );
            }
        } catch (Exception e) {
            showAlert(
                "Erreur",
                "Impossible d'ouvrir le dossier: " + e.getMessage(),
                Alert.AlertType.ERROR
            );
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

    /**
     * Opens a dialog with webcam preview and captures a photo
     * @return BufferedImage of captured photo, or null if capture failed/cancelled
     */
    private BufferedImage capturePhotoDialog() {
        Dialog<BufferedImage> dialog = new Dialog<>();
        dialog.setTitle("Capture Photo");
        dialog.setHeaderText("Prenez une photo du patient");

        VBox content = new VBox(15);
        content.setAlignment(Pos.CENTER);
        content.setPadding(new javafx.geometry.Insets(20));

        ImageView previewView = new ImageView();
        previewView.setFitWidth(640);
        previewView.setFitHeight(480);
        previewView.setPreserveRatio(true);
        previewView.setStyle("-fx-border-color: #333; -fx-border-width: 2;");

        Label statusLabel = new Label("Initialisation de la caméra...");
        statusLabel.setStyle("-fx-font-size: 14px;");

        content.getChildren().addAll(previewView, statusLabel);
        dialog.getDialogPane().setContent(content);

        ButtonType captureButtonType = new ButtonType(
            "📷 Capturer",
            ButtonBar.ButtonData.OK_DONE
        );
        dialog
            .getDialogPane()
            .getButtonTypes()
            .addAll(captureButtonType, ButtonType.CANCEL);

        Node captureButton = dialog
            .getDialogPane()
            .lookupButton(captureButtonType);
        captureButton.setDisable(true);

        final BufferedImage[] capturedImage = { null };
        final Thread[] previewThread = { null };
        final boolean[] keepRunning = { true };
        final Process[] gstProcess = { null };

        // Open webcam in a separate thread to avoid blocking UI
        new Thread(() -> {
            try {
                // Try GStreamer-based continuous preview for Intel IPU6
                boolean useGStreamer = tryGStreamerPreview(
                    previewView,
                    statusLabel,
                    captureButton,
                    keepRunning,
                    previewThread,
                    gstProcess
                );

                if (!useGStreamer) {
                    // Fallback to standard webcam library
                    boolean opened = photoService.openWebcam();

                    javafx.application.Platform.runLater(() -> {
                        if (opened) {
                            statusLabel.setText(
                                "Caméra prête - Cliquez sur 'Capturer' pour prendre la photo"
                            );
                            captureButton.setDisable(false);

                            // Start continuous preview in a separate thread
                            previewThread[0] = new Thread(() -> {
                                while (
                                    keepRunning[0] &&
                                    photoService.isWebcamOpen()
                                ) {
                                    try {
                                        BufferedImage frame =
                                            photoService.capturePhoto();
                                        if (frame != null) {
                                            Image fxImage =
                                                photoService.convertToFXImage(
                                                    frame
                                                );
                                            javafx.application.Platform.runLater(
                                                () -> {
                                                    previewView.setImage(
                                                        fxImage
                                                    );
                                                }
                                            );
                                        }
                                        // ~15 FPS for smooth preview
                                        Thread.sleep(66);
                                    } catch (InterruptedException e) {
                                        break;
                                    } catch (Exception e) {
                                        System.err.println(
                                            "Preview frame error: " +
                                                e.getMessage()
                                        );
                                    }
                                }
                            });
                            previewThread[0].setDaemon(true);
                            previewThread[0].start();
                        } else {
                            showCameraUnavailableMessage(content, statusLabel);
                        }
                    });
                }
            } catch (Exception e) {
                javafx.application.Platform.runLater(() -> {
                    statusLabel.setText(
                        "❌ Erreur de caméra\nUtilisez 'Télécharger Photo'"
                    );
                    statusLabel.setStyle(
                        "-fx-font-size: 14px; -fx-text-fill: red; -fx-text-alignment: center;"
                    );
                    statusLabel.setWrapText(true);
                    System.err.println("Webcam error in UI: " + e.getMessage());
                });
            }
        })
            .start();

        dialog.setResultConverter(dialogButton -> {
            if (dialogButton == captureButtonType) {
                // Stop the preview thread
                keepRunning[0] = false;

                // CRITICAL: Stop GStreamer process immediately
                if (gstProcess[0] != null) {
                    try {
                        gstProcess[0].destroy();
                        gstProcess[0].waitFor(
                            1,
                            java.util.concurrent.TimeUnit.SECONDS
                        );
                        if (gstProcess[0].isAlive()) {
                            gstProcess[0].destroyForcibly();
                            gstProcess[0].waitFor(
                                1,
                                java.util.concurrent.TimeUnit.SECONDS
                            );
                        }
                        System.out.println(
                            "GStreamer process stopped on capture"
                        );
                    } catch (Exception e) {
                        System.err.println(
                            "Error stopping GStreamer on capture: " +
                                e.getMessage()
                        );
                    }
                }

                // Extra safety: kill any orphaned processes
                killGStreamerProcesses();

                if (previewThread[0] != null) {
                    try {
                        previewThread[0].join(500);
                    } catch (InterruptedException e) {
                        previewThread[0].interrupt();
                    }
                }

                // Brief delay to ensure last frame is rendered in UI
                try {
                    Thread.sleep(100);
                } catch (InterruptedException e) {
                    // Ignore
                }

                // Capture the final photo from current preview
                Image currentImage = previewView.getImage();
                System.out.println(
                    "DEBUG: Current image from preview: " +
                        (currentImage != null ? "Present" : "NULL")
                );

                if (currentImage != null) {
                    capturedImage[0] =
                        javafx.embed.swing.SwingFXUtils.fromFXImage(
                            currentImage,
                            null
                        );
                    System.out.println(
                        "DEBUG: Converted to BufferedImage: " +
                            (capturedImage[0] != null
                                ? capturedImage[0].getWidth() +
                                  "x" +
                                  capturedImage[0].getHeight()
                                : "NULL")
                    );
                } else {
                    System.out.println(
                        "DEBUG: No preview image, using fallback capture"
                    );
                    // Fallback: capture from service
                    BufferedImage photo = photoService.capturePhoto();
                    if (photo != null) {
                        capturedImage[0] = photo;
                        System.out.println(
                            "DEBUG: Fallback capture successful: " +
                                photo.getWidth() +
                                "x" +
                                photo.getHeight()
                        );
                    } else {
                        System.err.println("DEBUG: Fallback capture FAILED");
                    }
                }
            } else {
                // Stop preview if canceled
                keepRunning[0] = false;

                // Stop GStreamer process on cancel too
                if (gstProcess[0] != null) {
                    try {
                        gstProcess[0].destroy();
                        gstProcess[0].waitFor(
                            1,
                            java.util.concurrent.TimeUnit.SECONDS
                        );
                        if (gstProcess[0].isAlive()) {
                            gstProcess[0].destroyForcibly();
                            gstProcess[0].waitFor(
                                1,
                                java.util.concurrent.TimeUnit.SECONDS
                            );
                        }
                    } catch (Exception e) {
                        // Ignore errors on cancel
                    }
                }

                // Extra safety: kill any orphaned processes
                killGStreamerProcesses();
            }

            // Always close webcam when dialog closes
            photoService.closeWebcam();
            return capturedImage[0];
        });

        // Ensure webcam is closed if dialog is closed
        dialog.setOnCloseRequest(e -> {
            keepRunning[0] = false;

            // Stop GStreamer process if running
            if (gstProcess[0] != null) {
                try {
                    gstProcess[0].destroy();
                    gstProcess[0].waitFor(
                        1,
                        java.util.concurrent.TimeUnit.SECONDS
                    );
                    if (gstProcess[0].isAlive()) {
                        gstProcess[0].destroyForcibly();
                        gstProcess[0].waitFor(
                            1,
                            java.util.concurrent.TimeUnit.SECONDS
                        );
                    }
                } catch (Exception e2) {
                    System.err.println(
                        "Error stopping GStreamer: " + e2.getMessage()
                    );
                }
            }

            // Extra safety: kill any orphaned processes
            killGStreamerProcesses();

            if (previewThread[0] != null) {
                previewThread[0].interrupt();
            }
            photoService.closeWebcam();
        });

        java.util.Optional<BufferedImage> result = dialog.showAndWait();
        BufferedImage finalImage = result.orElse(null);
        System.out.println(
            "DEBUG: capturePhotoDialog returning: " +
                (finalImage != null
                    ? finalImage.getWidth() + "x" + finalImage.getHeight()
                    : "NULL")
        );
        return finalImage;
    }

    /**
     * Try to use GStreamer for continuous video preview (works with Intel IPU6)
     */
    private boolean tryGStreamerPreview(
        ImageView previewView,
        Label statusLabel,
        Node captureButton,
        boolean[] keepRunning,
        Thread[] previewThread,
        Process[] gstProcessRef
    ) {
        try {
            // Check if GStreamer is available
            ProcessBuilder checkPb = new ProcessBuilder(
                "which",
                "gst-launch-1.0"
            );
            Process checkProcess = checkPb.start();
            int exitCode = checkProcess.waitFor();

            if (exitCode != 0) {
                return false; // GStreamer not available
            }

            // Start GStreamer-based preview
            javafx.application.Platform.runLater(() -> {
                statusLabel.setText(
                    "Caméra prête - Cliquez sur 'Capturer' pour prendre la photo"
                );
                captureButton.setDisable(false);
            });

            previewThread[0] = new Thread(() -> {
                try {
                    // Use optimized libcamera preview script (same backend as qcam)
                    ProcessBuilder gstPb = new ProcessBuilder(
                        "python3",
                        "scripts/libcamera_preview.py",
                        "/tmp/webcam_preview.jpg"
                    );
                    gstProcessRef[0] = gstPb.start();

                    // Read output to detect when ready
                    java.io.BufferedReader reader = new java.io.BufferedReader(
                        new java.io.InputStreamReader(
                            gstProcessRef[0].getInputStream()
                        )
                    );

                    // Wait for preview to be ready
                    String line1 = reader.readLine();
                    if (!"PREVIEW_STARTING".equals(line1)) {
                        return;
                    }

                    String line2 = reader.readLine();
                    if (!"PREVIEW_READY".equals(line2)) {
                        return;
                    }

                    Thread.sleep(500); // Brief delay for stability

                    // Continuously update preview
                    while (keepRunning[0]) {
                        try {
                            java.io.File previewFile = new java.io.File(
                                "/tmp/webcam_preview.jpg"
                            );
                            if (
                                previewFile.exists() && previewFile.length() > 0
                            ) {
                                java.awt.image.BufferedImage frame =
                                    javax.imageio.ImageIO.read(previewFile);
                                if (frame != null) {
                                    Image fxImage =
                                        javafx.embed.swing.SwingFXUtils.toFXImage(
                                            frame,
                                            null
                                        );
                                    javafx.application.Platform.runLater(() -> {
                                        previewView.setImage(fxImage);
                                    });
                                }
                            }
                            Thread.sleep(100); // ~10 FPS (smooth enough for preview)
                        } catch (InterruptedException e) {
                            break;
                        } catch (Exception e) {
                            // Ignore individual frame errors
                        }
                    }
                } catch (Exception e) {
                    System.err.println(
                        "GStreamer preview error: " + e.getMessage()
                    );
                } finally {
                    // Cleanup is now handled by the dialog button handlers
                    // to ensure immediate shutdown when Capture is clicked
                    if (
                        gstProcessRef[0] != null && gstProcessRef[0].isAlive()
                    ) {
                        try {
                            gstProcessRef[0].destroy();
                            gstProcessRef[0].waitFor(
                                1,
                                java.util.concurrent.TimeUnit.SECONDS
                            );
                            if (gstProcessRef[0].isAlive()) {
                                gstProcessRef[0].destroyForcibly();
                            }
                        } catch (Exception e) {
                            // Ignore cleanup errors
                        }
                    }
                    // Cleanup temp file
                    new java.io.File("/tmp/webcam_preview.jpg").delete();
                }
            });

            previewThread[0].setDaemon(true);
            previewThread[0].start();

            return true;
        } catch (Exception e) {
            System.err.println(
                "GStreamer preview not available: " + e.getMessage()
            );
            return false;
        }
    }

    /**
     * Aggressively kill any remaining GStreamer/libcamera processes
     */
    private void killGStreamerProcesses() {
        try {
            // Kill any remaining GStreamer processes related to webcam_preview
            ProcessBuilder pb1 = new ProcessBuilder(
                "pkill",
                "-9",
                "-f",
                "gst-launch.*webcam_preview"
            );
            pb1.start().waitFor(1, java.util.concurrent.TimeUnit.SECONDS);

            // Kill any remaining libcamera_preview.py processes
            ProcessBuilder pb2 = new ProcessBuilder(
                "pkill",
                "-9",
                "-f",
                "libcamera_preview.py"
            );
            pb2.start().waitFor(1, java.util.concurrent.TimeUnit.SECONDS);

            System.out.println("Executed aggressive GStreamer cleanup");
        } catch (Exception e) {
            // Ignore errors - cleanup is best-effort
            System.err.println(
                "Aggressive cleanup failed (may be on non-Linux): " +
                    e.getMessage()
            );
        }
    }

    /**
     * Debug logging to file for troubleshooting
     */
    private void debugLog(String message) {
        try {
            java.nio.file.Files.write(
                java.nio.file.Paths.get("photo_debug.log"),
                (java.time.LocalDateTime.now() +
                    " - " +
                    message +
                    "\n").getBytes(),
                java.nio.file.StandardOpenOption.CREATE,
                java.nio.file.StandardOpenOption.APPEND
            );
        } catch (Exception e) {
            // Ignore logging errors
        }
    }

    /**
     * Show camera unavailable message with helpful instructions
     */
    private void showCameraUnavailableMessage(VBox content, Label statusLabel) {
        statusLabel.setText(
            "❌ Caméra non disponible\nUtilisez 'Télécharger Photo' à la place"
        );
        statusLabel.setStyle(
            "-fx-font-size: 14px; -fx-text-fill: red; -fx-text-alignment: center;"
        );
        statusLabel.setWrapText(true);

        Label helpLabel = new Label(
            "💡 Solution:\n\n" +
                "1. Fermez cette fenêtre\n" +
                "2. Cliquez sur 'Télécharger Photo'\n" +
                "3. Prenez une photo avec votre téléphone\n" +
                "4. Transférez et sélectionnez le fichier"
        );
        helpLabel.setStyle(
            "-fx-font-size: 13px; -fx-text-alignment: center; -fx-padding: 20;"
        );
        helpLabel.setWrapText(true);
        helpLabel.setPrefWidth(380);

        VBox helpBox = new VBox(10);
        helpBox.setAlignment(Pos.CENTER);
        helpBox.getChildren().add(helpLabel);

        if (content.getChildren().size() > 1) {
            content.getChildren().set(0, helpBox);
        }
    }

    /**
     * Opens a file chooser dialog to upload a photo from disk
     * @return BufferedImage of uploaded photo, or null if cancelled/failed
     */
    private BufferedImage uploadPhotoDialog() {
        javafx.stage.FileChooser fileChooser = new javafx.stage.FileChooser();
        fileChooser.setTitle("Sélectionner une photo");
        fileChooser
            .getExtensionFilters()
            .addAll(
                new javafx.stage.FileChooser.ExtensionFilter(
                    "Images",
                    "*.jpg",
                    "*.jpeg",
                    "*.png",
                    "*.bmp",
                    "*.gif"
                ),
                new javafx.stage.FileChooser.ExtensionFilter(
                    "JPEG",
                    "*.jpg",
                    "*.jpeg"
                ),
                new javafx.stage.FileChooser.ExtensionFilter("PNG", "*.png"),
                new javafx.stage.FileChooser.ExtensionFilter(
                    "Tous les fichiers",
                    "*.*"
                )
            );

        java.io.File selectedFile = fileChooser.showOpenDialog(null);
        if (selectedFile != null) {
            BufferedImage image = photoService.loadPhotoFromFile(selectedFile);
            if (image != null) {
                System.out.println(
                    "Photo chargée depuis: " + selectedFile.getName()
                );
                return image;
            } else {
                showAlert(
                    "Erreur",
                    "Impossible de charger l'image. Vérifiez que le fichier est une image valide.",
                    Alert.AlertType.ERROR
                );
            }
        }
        return null;
    }
}
