package com.mediclinic.controller;

import com.mediclinic.model.*;
import com.mediclinic.service.*;
import java.io.File;
import java.net.URL;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.stage.FileChooser;

public class DoctorController implements Initializable {

    @FXML
    private TableView<Medecin> doctorTable;

    @FXML
    private TableColumn<Medecin, Long> colId;

    @FXML
    private TableColumn<Medecin, String> colNomComplet;

    @FXML
    private TableColumn<Medecin, String> colSpecialite;

    @FXML
    private TableColumn<Medecin, String> colEmail;

    @FXML
    private TableColumn<Medecin, String> colTelephone;

    @FXML
    private TextField searchField;

    @FXML
    private ComboBox<String> specialtyCombo;

    private MedecinService medecinService;
    private CsvService csvService;
    private ObservableList<Medecin> doctorList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Check permission - only ADMIN can access
        try {
            com.mediclinic.util.PermissionChecker.requireRole(
                com.mediclinic.model.Role.ADMIN
            );
        } catch (IllegalStateException e) {
            showAlert(
                "Accès refusé",
                e.getMessage(),
                javafx.scene.control.Alert.AlertType.ERROR
            );
            return;
        }

        medecinService = new MedecinService();
        csvService = new CsvService();

        setupTableColumns();
        setupSpecialties();
        loadDoctors();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        colNomComplet.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getNomComplet()
            )
        );

        colSpecialite.setCellValueFactory(cellData ->
            new javafx.beans.property.SimpleStringProperty(
                cellData.getValue().getSpecialite() != null
                    ? cellData.getValue().getSpecialite().name()
                    : "N/A"
            )
        );

        colEmail.setCellValueFactory(new PropertyValueFactory<>("email"));
        colTelephone.setCellValueFactory(
            new PropertyValueFactory<>("telephone")
        );

        // Add actions column
        TableColumn<Medecin, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(150);
        colActions.setCellFactory(param ->
            new TableCell<Medecin, Void>() {
                private final Button viewBtn = new Button("Détails");
                private final Button editBtn = new Button("Modifier");
                private final Button deleteBtn = new Button("Supprimer");

                {
                    viewBtn.getStyleClass().add("btn-primary");
                    editBtn.getStyleClass().add("btn-warning");
                    deleteBtn.getStyleClass().add("btn-danger");

                    viewBtn.setOnAction(event -> {
                        Medecin medecin = getTableView()
                            .getItems()
                            .get(getIndex());
                        showDoctorDetails(medecin);
                    });

                    editBtn.setOnAction(event -> {
                        Medecin medecin = getTableView()
                            .getItems()
                            .get(getIndex());
                        editDoctor(medecin);
                    });

                    deleteBtn.setOnAction(event -> {
                        Medecin medecin = getTableView()
                            .getItems()
                            .get(getIndex());
                        deleteDoctor(medecin);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        HBox buttons = new HBox(5, viewBtn, editBtn, deleteBtn);
                        setGraphic(buttons);
                    }
                }
            }
        );

        doctorTable.getColumns().add(colActions);
    }

    private void setupSpecialties() {
        ObservableList<String> specialties =
            FXCollections.observableArrayList();
        specialties.add("Toutes spécialités");

        for (SpecialiteMedecin spec : SpecialiteMedecin.values()) {
            specialties.add(spec.name());
        }

        specialtyCombo.setItems(specialties);
        specialtyCombo.setValue("Toutes spécialités");

        specialtyCombo.setOnAction(event -> filterDoctors());
    }

    private void loadDoctors() {
        try {
            doctorList = FXCollections.observableArrayList(
                medecinService.findAll()
            );
            doctorTable.setItems(doctorList);
        } catch (Exception e) {
            showAlert(
                "Erreur",
                "Erreur lors du chargement des médecins: " + e.getMessage(),
                Alert.AlertType.ERROR
            );
        }
    }

    private void filterDoctors() {
        String selectedSpecialty = specialtyCombo.getValue();
        String searchText = searchField.getText();

        ObservableList<Medecin> filtered = doctorList;

        // Filter by specialty
        if (
            selectedSpecialty != null &&
            !selectedSpecialty.equals("Toutes spécialités")
        ) {
            filtered = filtered
                .stream()
                .filter(
                    med ->
                        med.getSpecialite() != null &&
                        med.getSpecialite().name().equals(selectedSpecialty)
                )
                .collect(
                    Collectors.toCollection(FXCollections::observableArrayList)
                );
        }

        // Filter by search text
        if (searchText != null && !searchText.trim().isEmpty()) {
            String searchLower = searchText.toLowerCase();
            filtered = filtered
                .stream()
                .filter(
                    med ->
                        med
                            .getNomComplet()
                            .toLowerCase()
                            .contains(searchLower) ||
                        (med.getEmail() != null &&
                            med
                                .getEmail()
                                .toLowerCase()
                                .contains(searchLower)) ||
                        (med.getTelephone() != null &&
                            med.getTelephone().contains(searchText))
                )
                .collect(
                    Collectors.toCollection(FXCollections::observableArrayList)
                );
        }

        doctorTable.setItems(filtered);
    }

    @FXML
    private void handleAddDoctor() {
        try {
            Dialog<Medecin> dialog = new Dialog<>();
            dialog.setTitle("Nouveau Médecin");
            dialog.setHeaderText("Ajouter un nouveau médecin");

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

            TextField nomField = new TextField();
            nomField.setPromptText("Nom");
            TextField prenomField = new TextField();
            prenomField.setPromptText("Prénom");

            ComboBox<SpecialiteMedecin> specialiteCombo = new ComboBox<>();
            specialiteCombo.setItems(
                FXCollections.observableArrayList(SpecialiteMedecin.values())
            );
            specialiteCombo.setPromptText("Spécialité");

            TextField emailField = new TextField();
            emailField.setPromptText("Email");
            TextField telephoneField = new TextField();
            telephoneField.setPromptText("Téléphone");

            grid.add(new Label("Nom:"), 0, 0);
            grid.add(nomField, 1, 0);
            grid.add(new Label("Prénom:"), 0, 1);
            grid.add(prenomField, 1, 1);
            grid.add(new Label("Spécialité:"), 0, 2);
            grid.add(specialiteCombo, 1, 2);
            grid.add(new Label("Email:"), 0, 3);
            grid.add(emailField, 1, 3);
            grid.add(new Label("Téléphone:"), 0, 4);
            grid.add(telephoneField, 1, 4);

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
                        specialiteCombo.getValue() == null ||
                        emailField.getText().trim().isEmpty() ||
                        telephoneField.getText().trim().isEmpty()
                );
            };

            nomField.textProperty().addListener(changeListener);
            prenomField.textProperty().addListener(changeListener);
            emailField.textProperty().addListener(changeListener);
            telephoneField.textProperty().addListener(changeListener);
            specialiteCombo.setOnAction(event ->
                changeListener.changed(null, "", "")
            );

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    Medecin medecin = new Medecin();
                    medecin.setNom(nomField.getText());
                    medecin.setPrenom(prenomField.getText());
                    medecin.setSpecialite(specialiteCombo.getValue());
                    medecin.setEmail(emailField.getText());
                    medecin.setTelephone(telephoneField.getText());
                    return medecin;
                }
                return null;
            });

            dialog
                .showAndWait()
                .ifPresent(medecin -> {
                    try {
                        medecinService.saveMedecin(medecin);
                        loadDoctors();
                        showAlert(
                            "Succès",
                            "Médecin ajouté avec succès!",
                            Alert.AlertType.INFORMATION
                        );
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

    private void editDoctor(Medecin medecin) {
        if (medecin == null) return;

        try {
            Dialog<Medecin> dialog = new Dialog<>();
            dialog.setTitle("Modifier Médecin");
            dialog.setHeaderText("Modifier les informations du médecin");

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

            TextField nomField = new TextField(medecin.getNom());
            TextField prenomField = new TextField(medecin.getPrenom());

            ComboBox<SpecialiteMedecin> specialiteCombo = new ComboBox<>();
            specialiteCombo.setItems(
                FXCollections.observableArrayList(SpecialiteMedecin.values())
            );
            specialiteCombo.setValue(medecin.getSpecialite());

            TextField emailField = new TextField(medecin.getEmail());
            TextField telephoneField = new TextField(medecin.getTelephone());

            grid.add(new Label("Nom:"), 0, 0);
            grid.add(nomField, 1, 0);
            grid.add(new Label("Prénom:"), 0, 1);
            grid.add(prenomField, 1, 1);
            grid.add(new Label("Spécialité:"), 0, 2);
            grid.add(specialiteCombo, 1, 2);
            grid.add(new Label("Email:"), 0, 3);
            grid.add(emailField, 1, 3);
            grid.add(new Label("Téléphone:"), 0, 4);
            grid.add(telephoneField, 1, 4);

            dialog.getDialogPane().setContent(grid);

            ButtonType saveButtonType = new ButtonType(
                "Enregistrer",
                ButtonBar.ButtonData.OK_DONE
            );
            dialog
                .getDialogPane()
                .getButtonTypes()
                .addAll(saveButtonType, ButtonType.CANCEL);

            ButtonType saveBtn = saveButtonType;
            Long medecinId = medecin.getId();

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveBtn) {
                    // Retourner un marqueur pour indiquer que l'utilisateur veut sauvegarder
                    return medecin;
                }
                return null;
            });

            dialog
                .showAndWait()
                .ifPresent(result -> {
                    try {
                        // Utiliser la méthode ID-based pour éviter les entités détachées
                        medecinService.updateMedecin(
                            medecinId,
                            nomField.getText(),
                            prenomField.getText(),
                            specialiteCombo.getValue(),
                            emailField.getText(),
                            telephoneField.getText()
                        );
                        loadDoctors();
                        showAlert(
                            "Succès",
                            "Médecin modifié avec succès!",
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
                "Erreur: " + e.getMessage(),
                Alert.AlertType.ERROR
            );
        }
    }

    private void deleteDoctor(Medecin medecin) {
        if (medecin == null) return;

        Alert alert = new Alert(Alert.AlertType.CONFIRMATION);
        alert.setTitle("Confirmation de suppression");
        alert.setHeaderText("Supprimer le médecin");
        alert.setContentText(
            "Êtes-vous sûr de vouloir supprimer " +
                medecin.getNomComplet() +
                " ?\n" +
                "Cette action est irréversible si le médecin n'a pas de rendez-vous."
        );

        alert
            .showAndWait()
            .ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        medecinService.deleteMedecin(medecin.getId());
                        loadDoctors();
                        showAlert(
                            "Succès",
                            "Médecin supprimé avec succès!",
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
            });
    }

    private void showDoctorDetails(Medecin medecin) {
        if (medecin == null) return;

        showAlert(
            "Détails Médecin",
            "Nom: " +
                medecin.getNomComplet() +
                "\n" +
                "Spécialité: " +
                (medecin.getSpecialite() != null
                    ? medecin.getSpecialite().name()
                    : "N/A") +
                "\n" +
                "Email: " +
                (medecin.getEmail() != null ? medecin.getEmail() : "N/A") +
                "\n" +
                "Téléphone: " +
                (medecin.getTelephone() != null
                    ? medecin.getTelephone()
                    : "N/A"),
            Alert.AlertType.INFORMATION
        );
    }

    @FXML
    private void handleSearch() {
        filterDoctors();
    }

    @FXML
    private void handleExportDoctors() {
        try {
            List<Medecin> doctorsToExport = doctorTable
                .getItems()
                .stream()
                .collect(Collectors.toList());

            if (doctorsToExport.isEmpty()) {
                showAlert(
                    "Export",
                    "Aucun medecin a exporter",
                    Alert.AlertType.WARNING
                );
                return;
            }

            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Sauvegarder Export Medecins");
            fileChooser
                .getExtensionFilters()
                .add(new FileChooser.ExtensionFilter("CSV Files", "*.csv"));
            fileChooser.setInitialFileName("medecins_export.csv");

            File file = fileChooser.showSaveDialog(
                doctorTable.getScene().getWindow()
            );
            if (file != null) {
                csvService.exportDoctors(
                    doctorsToExport,
                    file.getAbsolutePath()
                );
                showAlert(
                    "Succes",
                    "Export CSV genere avec succes!\nFichier: " +
                        file.getAbsolutePath(),
                    Alert.AlertType.INFORMATION
                );
            }
        } catch (Exception e) {
            showAlert(
                "Erreur",
                "Erreur lors de l'export: " + e.getMessage(),
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
