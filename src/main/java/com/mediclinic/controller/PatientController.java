package com.mediclinic.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import com.mediclinic.model.Patient;
import com.mediclinic.service.PatientService;

import java.net.URL;
import java.util.ResourceBundle;

public class PatientController implements Initializable {

    @FXML private TableView<Patient> patientTable;
    @FXML private TableColumn<Patient, Long> colId;
    @FXML private TableColumn<Patient, String> colNomComplet;
    @FXML private TableColumn<Patient, String> colEmail;
    @FXML private TableColumn<Patient, String> colTelephone;
    @FXML private TableColumn<Patient, String> colDateNaissance;
    @FXML private TextField searchField;
    @FXML private ComboBox<String> filterCombo;

    private PatientService patientService;
    private ObservableList<Patient> patientList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        patientService = new PatientService();
        setupTableColumns();
        loadPatients();
        setupSearchFilter();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));
        colNomComplet.setCellValueFactory(new PropertyValueFactory<>("nomComplet"));
        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colTelephone.setCellValueFactory(new PropertyValueFactory<>("telephone"));
        colDateNaissance.setCellValueFactory(new PropertyValueFactory<>("dateNaissance"));

        TableColumn<Patient, Void> colActions = new TableColumn<>("Actions");
        colActions.setCellFactory(param -> new TableCell<Patient, Void>() {
            private final Button editBtn = new Button("✏️");
            private final Button deleteBtn = new Button("🗑️");
            private final Button detailsBtn = new Button("👁️");

            {
                editBtn.getStyleClass().add("btn-warning");
                deleteBtn.getStyleClass().add("btn-danger");
                detailsBtn.getStyleClass().add("btn-primary");

                editBtn.setOnAction(event -> {
                    Patient patient = getTableView().getItems().get(getIndex());
                    editPatient(patient);
                });

                deleteBtn.setOnAction(event -> {
                    Patient patient = getTableView().getItems().get(getIndex());
                    deletePatient(patient);
                });

                detailsBtn.setOnAction(event -> {
                    Patient patient = getTableView().getItems().get(getIndex());
                    showPatientDetails(patient);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    HBox buttons = new HBox(5, detailsBtn, editBtn, deleteBtn);
                    setGraphic(buttons);
                }
            }
        });

        patientTable.getColumns().add(colActions);
    }

    private void loadPatients() {
        try {
            patientList = FXCollections.observableArrayList(patientService.findAll());
            patientTable.setItems(patientList);
        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors du chargement des patients: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void setupSearchFilter() {
        filterCombo.setItems(FXCollections.observableArrayList(
                "Tous les patients", "Actifs seulement", "Archivés"
        ));
        filterCombo.setValue("Tous les patients");

        searchField.textProperty().addListener((observable, oldValue, newValue) -> {
            filterPatients(newValue);
        });
    }

    @FXML
    private void filterPatients(String searchText) {
        if (searchText == null || searchText.isEmpty()) {
            patientTable.setItems(patientList);
        } else {
            ObservableList<Patient> filteredList = FXCollections.observableArrayList();
            for (Patient patient : patientList) {
                if (patient.getNomComplet().toLowerCase().contains(searchText.toLowerCase()) ||
                        patient.getEmail().toLowerCase().contains(searchText.toLowerCase()) ||
                        patient.getTelephone().contains(searchText)) {
                    filteredList.add(patient);
                }
            }
            patientTable.setItems(filteredList);
        }
    }

    @FXML
    private void showAddPatientForm() {
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

            ButtonType saveButtonType = new ButtonType("Enregistrer", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

            Node saveButton = dialog.getDialogPane().lookupButton(saveButtonType);
            saveButton.setDisable(true);

            javafx.beans.value.ChangeListener<String> changeListener = (observable, oldValue, newValue) -> {
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
                        patient.setDateNaissance(dateNaissancePicker.getValue());
                    }
                    return patient;
                }
                return null;
            });

            java.util.Optional<Patient> result = dialog.showAndWait();
            result.ifPresent(patient -> {
                try {
                    patientService.createPatient(patient);
                    loadPatients();
                    showAlert("Succès", "Patient ajouté avec succès!", Alert.AlertType.INFORMATION);
                } catch (Exception e) {
                    showAlert("Erreur", "Erreur lors de l'ajout: " + e.getMessage(), Alert.AlertType.ERROR);
                }
            });

        } catch (Exception e) {
            showAlert("Erreur", "Erreur lors de l'ouverture du formulaire: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void editPatient(Patient patient) {
        showAlert("Information", "Édition du patient: " + patient.getNomComplet(), Alert.AlertType.INFORMATION);
    }

    private void deletePatient(Patient patient) {
        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer le patient");
        alert.setContentText("Êtes-vous sûr de vouloir supprimer " + patient.getNomComplet() + " ?");

        java.util.Optional<ButtonType> result = alert.showAndWait();
        if (result.isPresent() && result.get() == ButtonType.OK) {
            try {
                patientService.deletePatient(patient.getId());
                loadPatients();
                showAlert("Succès", "Patient supprimé avec succès!", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Erreur", "Erreur lors de la suppression: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        }
    }

    private void showPatientDetails(Patient patient) {
        showAlert("Détails Patient",
                "Nom: " + patient.getNomComplet() + "\n" +
                        "Email: " + patient.getEmail() + "\n" +
                        "Téléphone: " + patient.getTelephone() + "\n" +
                        "Date naissance: " + patient.getDateNaissance(),
                Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleSearch() {
        filterPatients(searchField.getText());
    }

    @FXML
    private void handleExport() {
        showAlert("Export", "Fonction d'export à implémenter", Alert.AlertType.INFORMATION);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}