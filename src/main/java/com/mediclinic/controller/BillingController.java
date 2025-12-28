package com.mediclinic.controller;

import com.mediclinic.model.*;
import com.mediclinic.service.*;
import com.mediclinic.util.UserSession;
import java.io.File;
import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.control.*;
import javafx.scene.control.ButtonBar;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;

public class BillingController implements Initializable {

    @FXML
    private TableView<Facture> invoiceTable;

    @FXML
    private TableColumn<Facture, Long> colId;

    @FXML
    private TableColumn<Facture, String> colDate;

    @FXML
    private TableColumn<Facture, String> colPatient;

    @FXML
    private TableColumn<Facture, String> colAmount;

    @FXML
    private TableColumn<Facture, String> colStatus;

    @FXML
    private TableColumn<Facture, String> colPaymentType;

    @FXML
    private DatePicker startDatePicker;

    @FXML
    private DatePicker endDatePicker;

    @FXML
    private ComboBox<String> statusCombo;

    @FXML
    private TextField searchField;

    @FXML
    private Label totalRevenueLabel;

    @FXML
    private Label paidInvoicesLabel;

    @FXML
    private Label unpaidInvoicesLabel;

    @FXML
    private Label averageInvoiceLabel;

    private FacturationService facturationService;
    private PatientService patientService;
    private PdfService pdfService;
    private EmailService emailService;
    private CsvService csvService;
    private ObservableList<Facture> invoiceList;

    @Override
    public void initialize(URL location, ResourceBundle resources) {
        // Check permission - SEC and ADMIN can access
        try {
            com.mediclinic.util.PermissionChecker.requireRole(
                com.mediclinic.model.Role.SEC,
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

        facturationService = new FacturationService();
        patientService = new PatientService();
        pdfService = new PdfService();
        emailService = new EmailService();
        csvService = new CsvService();

        setupTableColumns();
        setupStatusFilter();
        loadInvoices();
        updateStatistics();
    }

    private void setupTableColumns() {
        colId.setCellValueFactory(new PropertyValueFactory<>("id"));

        colDate.setCellValueFactory(cellData -> {
            LocalDate date = cellData.getValue().getDateFacturation();
            return new javafx.beans.property.SimpleStringProperty(
                date != null
                    ? date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy"))
                    : "N/A"
            );
        });

        colPatient.setCellValueFactory(cellData -> {
            Patient patient = cellData.getValue().getPatient();
            return new javafx.beans.property.SimpleStringProperty(
                patient != null ? patient.getNomComplet() : "N/A"
            );
        });

        colAmount.setCellValueFactory(cellData -> {
            BigDecimal montant = cellData.getValue().getMontantTotal();
            return new javafx.beans.property.SimpleStringProperty(
                montant != null ? montant + " €" : "0 €"
            );
        });

        colStatus.setCellValueFactory(cellData -> {
            boolean payee = cellData.getValue().isEstPayee();
            return new javafx.beans.property.SimpleStringProperty(
                payee ? "Payée" : "En attente"
            );
        });

        colPaymentType.setCellValueFactory(cellData -> {
            TypePaiement type = cellData.getValue().getTypePaiement();
            return new javafx.beans.property.SimpleStringProperty(
                type != null ? type.name() : "N/A"
            );
        });

        // Add actions column
        TableColumn<Facture, Void> colActions = new TableColumn<>("Actions");
        colActions.setPrefWidth(280);
        colActions.setCellFactory(param ->
            new TableCell<Facture, Void>() {
                private final Button viewBtn = new Button("Détails");
                private final Button payBtn = new Button("Marquer payée");
                private final Button printBtn = new Button("📄 PDF");
                private final Button emailBtn = new Button("📧 Email");

                {
                    viewBtn.getStyleClass().add("btn-primary");
                    payBtn.getStyleClass().add("btn-success");
                    printBtn.getStyleClass().add("btn-warning");
                    emailBtn.getStyleClass().add("btn-primary");

                    viewBtn.setOnAction(event -> {
                        Facture facture = getTableView()
                            .getItems()
                            .get(getIndex());
                        showInvoiceDetails(facture);
                    });

                    payBtn.setOnAction(event -> {
                        Facture facture = getTableView()
                            .getItems()
                            .get(getIndex());
                        markAsPaid(facture);
                    });

                    printBtn.setOnAction(event -> {
                        Facture facture = getTableView()
                            .getItems()
                            .get(getIndex());
                        generateAndSavePdf(facture);
                    });

                    emailBtn.setOnAction(event -> {
                        Facture facture = getTableView()
                            .getItems()
                            .get(getIndex());
                        sendInvoiceEmail(facture);
                    });
                }

                @Override
                protected void updateItem(Void item, boolean empty) {
                    super.updateItem(item, empty);
                    if (empty) {
                        setGraphic(null);
                    } else {
                        Facture facture = getTableView()
                            .getItems()
                            .get(getIndex());
                        HBox buttons = new HBox(5);
                        buttons.getChildren().add(viewBtn);

                        // Afficher le bouton payer uniquement si la facture n'est pas payée
                        if (facture != null && !facture.isEstPayee()) {
                            payBtn.setDisable(false);
                            buttons.getChildren().add(payBtn);
                        }

                        buttons.getChildren().addAll(printBtn, emailBtn);
                        setGraphic(buttons);
                    }
                }
            }
        );

        invoiceTable.getColumns().add(colActions);
    }

    private void setupStatusFilter() {
        statusCombo.setItems(
            FXCollections.observableArrayList(
                "Tous les statuts",
                "Payée",
                "En attente"
            )
        );
        statusCombo.setValue("Tous les statuts");

        statusCombo.setOnAction(event -> filterInvoices());

        if (startDatePicker != null) {
            startDatePicker.setValue(LocalDate.now().minusMonths(1));
            startDatePicker.setOnAction(event -> filterInvoices());
        }

        if (endDatePicker != null) {
            endDatePicker.setValue(LocalDate.now());
            endDatePicker.setOnAction(event -> filterInvoices());
        }
    }

    private void loadInvoices() {
        try {
            List<Facture> allInvoices = facturationService.getAllFactures();
            invoiceList = FXCollections.observableArrayList(allInvoices);
            invoiceTable.setItems(invoiceList);
            System.out.println("Factures chargées: " + allInvoices.size());
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(
                "Erreur",
                "Erreur lors du chargement des factures: " + e.getMessage(),
                Alert.AlertType.ERROR
            );
        }
    }

    private void filterInvoices() {
        try {
            List<Facture> allInvoices = facturationService.getAllFactures();
            List<Facture> filtered = allInvoices
                .stream()
                .filter(facture -> {
                    String selectedStatus = statusCombo.getValue();
                    if (
                        selectedStatus != null &&
                        !selectedStatus.equals("Tous les statuts")
                    ) {
                        if (
                            selectedStatus.equals("Payée") &&
                            !facture.isEstPayee()
                        ) return false;
                        if (
                            selectedStatus.equals("En attente") &&
                            facture.isEstPayee()
                        ) return false;
                    }
                    LocalDate startDate = startDatePicker != null
                        ? startDatePicker.getValue()
                        : null;
                    if (
                        startDate != null &&
                        facture.getDateFacturation() != null
                    ) {
                        if (
                            facture.getDateFacturation().isBefore(startDate)
                        ) return false;
                    }
                    LocalDate endDate = endDatePicker != null
                        ? endDatePicker.getValue()
                        : null;
                    if (
                        endDate != null && facture.getDateFacturation() != null
                    ) {
                        if (
                            facture.getDateFacturation().isAfter(endDate)
                        ) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
            invoiceList = FXCollections.observableArrayList(filtered);
            invoiceTable.setItems(invoiceList);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(
                "Erreur",
                "Erreur lors du filtrage: " + e.getMessage(),
                Alert.AlertType.ERROR
            );
        }
    }

    private void updateStatistics() {
        try {
            List<Facture> allInvoices = facturationService.getAllFactures();

            BigDecimal totalRevenue = BigDecimal.ZERO;
            int paidCount = 0;
            int unpaidCount = 0;

            for (Facture f : allInvoices) {
                if (f.isEstPayee()) {
                    paidCount++;
                    if (f.getMontantTotal() != null) {
                        totalRevenue = totalRevenue.add(f.getMontantTotal());
                    }
                } else {
                    unpaidCount++;
                }
            }

            totalRevenueLabel.setText(totalRevenue + " €");
            paidInvoicesLabel.setText(String.valueOf(paidCount));
            unpaidInvoicesLabel.setText(String.valueOf(unpaidCount));

            BigDecimal average = paidCount > 0
                ? totalRevenue.divide(
                      BigDecimal.valueOf(paidCount),
                      2,
                      java.math.RoundingMode.HALF_UP
                  )
                : BigDecimal.ZERO;
            averageInvoiceLabel.setText(average + " €");
        } catch (Exception e) {
            e.printStackTrace();
            System.err.println("Error updating statistics: " + e.getMessage());
        }
    }

    @FXML
    private void handleNewInvoice() {
        // Check permission
        if (!facturationService.canCreateInvoice()) {
            showAlert(
                "Accès refusé",
                "Vous n'avez pas la permission de créer une facture.",
                Alert.AlertType.WARNING
            );
            return;
        }

        try {
            Dialog<Facture> dialog = new Dialog<>();
            dialog.setTitle("Nouvelle Facture");
            dialog.setHeaderText("Créer une nouvelle facture");

            VBox content = new VBox(15);
            content.setPadding(new javafx.geometry.Insets(20));

            // Patient selection
            ComboBox<Patient> patientCombo = new ComboBox<>();
            patientCombo.setItems(
                FXCollections.observableArrayList(patientService.findAll())
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

            // Invoice lines
            ListView<String> linesListView = new ListView<>();
            ObservableList<LigneFacture> lignes =
                FXCollections.observableArrayList();

            Button addLineBtn = new Button("+ Ajouter une ligne");
            addLineBtn.setOnAction(e -> {
                TextInputDialog lineDialog = new TextInputDialog();
                lineDialog.setTitle("Nouvelle ligne");
                lineDialog.setHeaderText("Ajouter une ligne de facturation");
                lineDialog.setContentText("Description:");

                lineDialog
                    .showAndWait()
                    .ifPresent(description -> {
                        TextInputDialog priceDialog = new TextInputDialog();
                        priceDialog.setTitle("Prix");
                        priceDialog.setContentText("Prix unitaire:");

                        priceDialog
                            .showAndWait()
                            .ifPresent(priceStr -> {
                                try {
                                    BigDecimal price = new BigDecimal(priceStr);
                                    LigneFacture ligne = new LigneFacture();
                                    ligne.setDescription(description);
                                    ligne.setPrixUnitaire(price);
                                    ligne.setQuantite(1);
                                    lignes.add(ligne);
                                    linesListView
                                        .getItems()
                                        .add(
                                            description + " - " + price + " €"
                                        );
                                } catch (NumberFormatException ex) {
                                    showAlert(
                                        "Erreur",
                                        "Prix invalide",
                                        Alert.AlertType.ERROR
                                    );
                                }
                            });
                    });
            });

            content
                .getChildren()
                .addAll(
                    new Label("Patient:"),
                    patientCombo,
                    new Label("Lignes de facturation:"),
                    linesListView,
                    addLineBtn
                );

            dialog.getDialogPane().setContent(content);

            ButtonType saveButtonType = new ButtonType(
                "Créer",
                ButtonBar.ButtonData.OK_DONE
            );
            dialog
                .getDialogPane()
                .getButtonTypes()
                .addAll(saveButtonType, ButtonType.CANCEL);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    // Retourner un marqueur non-null pour indiquer que l'utilisateur veut sauvegarder
                    return new Facture(); // Marqueur
                }
                return null;
            });

            dialog
                .showAndWait()
                .ifPresent(result -> {
                    // Vérifier que le patient est sélectionné et qu'il y a des lignes
                    if (patientCombo.getValue() == null) {
                        showAlert(
                            "Erreur",
                            "Veuillez sélectionner un patient.",
                            Alert.AlertType.ERROR
                        );
                        return;
                    }
                    if (lignes.isEmpty()) {
                        showAlert(
                            "Erreur",
                            "Veuillez ajouter au moins une ligne de facturation.",
                            Alert.AlertType.ERROR
                        );
                        return;
                    }

                    try {
                        facturationService.creerFacture(
                            patientCombo.getValue().getId(),
                            new ArrayList<>(lignes)
                        );
                        loadInvoices();
                        updateStatistics();
                        showAlert(
                            "Succès",
                            "Facture créée avec succès!",
                            Alert.AlertType.INFORMATION
                        );
                    } catch (Exception ex) {
                        showAlert(
                            "Erreur",
                            "Erreur lors de la création: " + ex.getMessage(),
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

    private void showInvoiceDetails(Facture facture) {
        if (facture == null) return;

        StringBuilder details = new StringBuilder();
        details.append("Facture N°").append(facture.getId()).append("\n\n");
        details
            .append("Patient: ")
            .append(
                facture.getPatient() != null
                    ? facture.getPatient().getNomComplet()
                    : "N/A"
            )
            .append("\n");
        details
            .append("Date: ")
            .append(facture.getDateFacturation())
            .append("\n");
        details
            .append("Montant Total: ")
            .append(facture.getMontantTotal())
            .append(" €\n");
        details
            .append("Statut: ")
            .append(facture.isEstPayee() ? "Payée" : "En attente")
            .append("\n");

        if (facture.getTypePaiement() != null) {
            details
                .append("Mode de paiement: ")
                .append(facture.getTypePaiement().name())
                .append("\n");
        }

        details.append("\nLignes:\n");
        if (facture.getLignes() != null) {
            for (LigneFacture ligne : facture.getLignes()) {
                details
                    .append("- ")
                    .append(ligne.getDescription())
                    .append(": ")
                    .append(ligne.getMontantLigne())
                    .append(" €\n");
            }
        }

        showAlert(
            "Détails de la Facture",
            details.toString(),
            Alert.AlertType.INFORMATION
        );
    }

    private void markAsPaid(Facture facture) {
        if (facture == null || facture.isEstPayee()) return;

        ChoiceDialog<TypePaiement> dialog = new ChoiceDialog<>(
            TypePaiement.CARTE_CREDIT,
            TypePaiement.values()
        );
        dialog.setTitle("Marquer comme payée");
        dialog.setHeaderText("Sélectionner le mode de paiement");
        dialog.setContentText("Mode de paiement:");

        dialog
            .showAndWait()
            .ifPresent(typePaiement -> {
                try {
                    facturationService.marquerCommePayee(
                        facture.getId(),
                        typePaiement
                    );
                    loadInvoices();
                    updateStatistics();
                    showAlert(
                        "Succès",
                        "Facture marquée comme payée!",
                        Alert.AlertType.INFORMATION
                    );
                } catch (Exception e) {
                    showAlert(
                        "Erreur",
                        "Erreur: " + e.getMessage(),
                        Alert.AlertType.ERROR
                    );
                }
            });
    }

    /**
     * Generate PDF and save to user-selected location
     */
    private void generateAndSavePdf(Facture facture) {
        if (facture == null) {
            showAlert("Erreur", "Facture invalide", Alert.AlertType.ERROR);
            return;
        }

        try {
            // Create file chooser
            javafx.stage.FileChooser fileChooser =
                new javafx.stage.FileChooser();
            fileChooser.setTitle("Enregistrer la facture PDF");
            fileChooser.setInitialFileName(
                "Facture_" + facture.getId() + ".pdf"
            );
            fileChooser
                .getExtensionFilters()
                .add(
                    new javafx.stage.FileChooser.ExtensionFilter(
                        "PDF Files",
                        "*.pdf"
                    )
                );

            // Show save dialog
            javafx.stage.Window window = invoiceTable.getScene().getWindow();
            File file = fileChooser.showSaveDialog(window);

            if (file != null) {
                // Generate PDF
                pdfService.generateFacturePdf(facture, file.getAbsolutePath());

                // Show success with option to open
                Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                successAlert.setTitle("Succès");
                successAlert.setHeaderText("PDF généré avec succès");
                successAlert.setContentText(
                    "La facture a été enregistrée dans:\n" +
                        file.getAbsolutePath() +
                        "\n\nVoulez-vous ouvrir le fichier?"
                );

                ButtonType openButton = new ButtonType("Ouvrir");
                ButtonType closeButton = new ButtonType(
                    "Fermer",
                    ButtonBar.ButtonData.CANCEL_CLOSE
                );
                successAlert.getButtonTypes().setAll(openButton, closeButton);

                successAlert
                    .showAndWait()
                    .ifPresent(response -> {
                        if (response == openButton) {
                            try {
                                // Open PDF with default application
                                java.awt.Desktop.getDesktop().open(file);
                            } catch (Exception e) {
                                showAlert(
                                    "Erreur",
                                    "Impossible d'ouvrir le fichier: " +
                                        e.getMessage(),
                                    Alert.AlertType.ERROR
                                );
                            }
                        }
                    });
            }
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(
                "Erreur",
                "Erreur lors de la génération du PDF: " + e.getMessage(),
                Alert.AlertType.ERROR
            );
        }
    }

    /**
     * Send invoice via email with PDF attachment
     */
    private void sendInvoiceEmail(Facture facture) {
        if (facture == null) {
            showAlert("Erreur", "Facture invalide", Alert.AlertType.ERROR);
            return;
        }

        try {
            // Check if patient has email
            Patient patient = facture.getPatient();
            if (
                patient == null ||
                patient.getEmail() == null ||
                patient.getEmail().trim().isEmpty()
            ) {
                showAlert(
                    "Erreur",
                    "Le patient n'a pas d'adresse email enregistrée.",
                    Alert.AlertType.ERROR
                );
                return;
            }

            // Create email configuration dialog
            Dialog<EmailConfig> dialog = new Dialog<>();
            dialog.setTitle("Envoyer Facture par Email");
            dialog.setHeaderText(
                "Configuration de l'email pour la facture N°" + facture.getId()
            );

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new javafx.geometry.Insets(20, 150, 10, 10));

            // Email fields
            TextField toField = new TextField(patient.getEmail());
            toField.setPromptText("Email du destinataire");

            TextField subjectField = new TextField(
                "Votre Facture MediClinic - N°" + facture.getId()
            );
            subjectField.setPromptText("Sujet de l'email");

            TextArea bodyArea = new TextArea(
                "Bonjour " +
                    patient.getPrenom() +
                    " " +
                    patient.getNom() +
                    ",\n\n" +
                    "Veuillez trouver ci-joint votre facture d'un montant de " +
                    facture.getMontantTotal() +
                    " €.\n\n" +
                    "Date de facturation: " +
                    facture
                        .getDateFacturation()
                        .format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) +
                    "\n" +
                    "Statut: " +
                    (facture.isEstPayee()
                        ? "Payée"
                        : "En attente de paiement") +
                    "\n\n" +
                    "Cordialement,\n" +
                    "L'équipe MediClinic"
            );
            bodyArea.setPrefRowCount(8);
            bodyArea.setWrapText(true);

            CheckBox attachPdfCheckBox = new CheckBox(
                "Joindre le PDF de la facture"
            );
            attachPdfCheckBox.setSelected(true);

            Label infoLabel = new Label(
                "💡 Le PDF sera généré automatiquement et joint à l'email."
            );
            infoLabel.setStyle("-fx-text-fill: #666; -fx-font-size: 11px;");

            grid.add(new Label("Destinataire:"), 0, 0);
            grid.add(toField, 1, 0);
            grid.add(new Label("Sujet:"), 0, 1);
            grid.add(subjectField, 1, 1);
            grid.add(new Label("Message:"), 0, 2);
            grid.add(bodyArea, 1, 2);
            grid.add(attachPdfCheckBox, 1, 3);
            grid.add(infoLabel, 1, 4);

            dialog.getDialogPane().setContent(grid);

            ButtonType sendButton = new ButtonType(
                "Envoyer",
                ButtonBar.ButtonData.OK_DONE
            );
            dialog
                .getDialogPane()
                .getButtonTypes()
                .addAll(sendButton, ButtonType.CANCEL);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == sendButton) {
                    return new EmailConfig(
                        toField.getText(),
                        subjectField.getText(),
                        bodyArea.getText(),
                        attachPdfCheckBox.isSelected()
                    );
                }
                return null;
            });

            dialog
                .showAndWait()
                .ifPresent(config -> {
                    // Validate email
                    if (config.to == null || config.to.trim().isEmpty()) {
                        showAlert(
                            "Erreur",
                            "Veuillez spécifier un destinataire.",
                            Alert.AlertType.ERROR
                        );
                        return;
                    }

                    // Show progress
                    Alert progressAlert = new Alert(
                        Alert.AlertType.INFORMATION
                    );
                    progressAlert.setTitle("Envoi en cours");
                    progressAlert.setHeaderText("Envoi de l'email");
                    progressAlert.setContentText(
                        "Génération du PDF et envoi de l'email en cours...\nVeuillez patienter."
                    );
                    progressAlert.show();

                    // Send email in background thread
                    new Thread(() -> {
                        try {
                            String pdfPath = null;

                            if (config.attachPdf) {
                                // Generate temporary PDF
                                pdfPath =
                                    System.getProperty("java.io.tmpdir") +
                                    "/facture_" +
                                    facture.getId() +
                                    "_" +
                                    System.currentTimeMillis() +
                                    ".pdf";
                                pdfService.generateFacturePdf(facture, pdfPath);
                            }

                            // Send email
                            emailService.sendEmailWithAttachment(
                                config.to,
                                config.subject,
                                config.body,
                                pdfPath
                            );

                            // Update UI on JavaFX thread
                            javafx.application.Platform.runLater(() -> {
                                progressAlert.close();
                                showAlert(
                                    "Succès",
                                    "Email envoyé avec succès à " +
                                        config.to +
                                        "!\n\nNote: L'envoi se fait en arrière-plan.",
                                    Alert.AlertType.INFORMATION
                                );
                            });

                            // Clean up temporary file
                            if (pdfPath != null) {
                                try {
                                    Thread.sleep(5000); // Wait for email to be sent
                                    new File(pdfPath).delete();
                                } catch (Exception e) {
                                    // Ignore cleanup errors
                                }
                            }
                        } catch (Exception e) {
                            javafx.application.Platform.runLater(() -> {
                                progressAlert.close();
                                showAlert(
                                    "Erreur",
                                    "Erreur lors de l'envoi de l'email:\n" +
                                        e.getMessage() +
                                        "\n\nVérifiez la configuration email dans EmailService.java",
                                    Alert.AlertType.ERROR
                                );
                            });
                            e.printStackTrace();
                        }
                    })
                        .start();
                });
        } catch (Exception e) {
            e.printStackTrace();
            showAlert(
                "Erreur",
                "Erreur: " + e.getMessage(),
                Alert.AlertType.ERROR
            );
        }
    }

    /**
     * Helper class for email configuration
     */
    private static class EmailConfig {

        String to;
        String subject;
        String body;
        boolean attachPdf;

        EmailConfig(String to, String subject, String body, boolean attachPdf) {
            this.to = to;
            this.subject = subject;
            this.body = body;
            this.attachPdf = attachPdf;
        }
    }

    @FXML
    private void handleSearchInvoices() {
        String searchText = searchField.getText();
        if (searchText == null || searchText.trim().isEmpty()) {
            invoiceTable.setItems(invoiceList);
        } else {
            ObservableList<Facture> filtered = invoiceList
                .stream()
                .filter(
                    facture ->
                        facture.getPatient() != null &&
                        facture
                            .getPatient()
                            .getNomComplet()
                            .toLowerCase()
                            .contains(searchText.toLowerCase())
                )
                .collect(
                    Collectors.toCollection(FXCollections::observableArrayList)
                );
            invoiceTable.setItems(filtered);
        }
    }

    @FXML
    private void handleGenerateReport() {
        try {
            Dialog<ButtonType> dialog = new Dialog<>();
            dialog.setTitle("Rapport Financier");
            dialog.setHeaderText("Generer un rapport financier");

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new javafx.geometry.Insets(20));

            DatePicker startPicker = new DatePicker(
                LocalDate.now().withDayOfMonth(1)
            );
            DatePicker endPicker = new DatePicker(LocalDate.now());

            grid.add(new Label("Date debut:"), 0, 0);
            grid.add(startPicker, 1, 0);
            grid.add(new Label("Date fin:"), 0, 1);
            grid.add(endPicker, 1, 1);

            ButtonType generateButton = new ButtonType(
                "Generer PDF",
                ButtonBar.ButtonData.OK_DONE
            );
            dialog
                .getDialogPane()
                .getButtonTypes()
                .addAll(generateButton, ButtonType.CANCEL);
            dialog.getDialogPane().setContent(grid);

            dialog
                .showAndWait()
                .ifPresent(response -> {
                    if (response == generateButton) {
                        try {
                            LocalDate startDate = startPicker.getValue();
                            LocalDate endDate = endPicker.getValue();

                            if (startDate == null || endDate == null) {
                                showAlert(
                                    "Erreur",
                                    "Veuillez selectionner les dates",
                                    Alert.AlertType.ERROR
                                );
                                return;
                            }

                            if (startDate.isAfter(endDate)) {
                                showAlert(
                                    "Erreur",
                                    "La date de debut doit etre avant la date de fin",
                                    Alert.AlertType.ERROR
                                );
                                return;
                            }

                            List<Facture> filteredInvoices = facturationService
                                .getAllFactures()
                                .stream()
                                .filter(f -> f.getDateFacturation() != null)
                                .filter(
                                    f ->
                                        !f
                                            .getDateFacturation()
                                            .isBefore(startDate) &&
                                        !f.getDateFacturation().isAfter(endDate)
                                )
                                .collect(Collectors.toList());

                            javafx.stage.FileChooser fileChooser =
                                new javafx.stage.FileChooser();
                            fileChooser.setTitle(
                                "Sauvegarder Rapport Financier"
                            );
                            fileChooser
                                .getExtensionFilters()
                                .add(
                                    new javafx.stage.FileChooser.ExtensionFilter(
                                        "PDF Files",
                                        "*.pdf"
                                    )
                                );
                            fileChooser.setInitialFileName(
                                "rapport_financier_" +
                                    startDate +
                                    "_" +
                                    endDate +
                                    ".pdf"
                            );

                            File file = fileChooser.showSaveDialog(
                                invoiceTable.getScene().getWindow()
                            );
                            if (file != null) {
                                pdfService.generateFinancialReport(
                                    filteredInvoices,
                                    startDate,
                                    endDate,
                                    file.getAbsolutePath()
                                );

                                Alert successAlert = new Alert(
                                    Alert.AlertType.CONFIRMATION
                                );
                                successAlert.setTitle("Succes");
                                successAlert.setHeaderText(
                                    "Rapport genere avec succes!"
                                );
                                successAlert.setContentText(
                                    "Voulez-vous ouvrir le fichier?"
                                );

                                successAlert
                                    .showAndWait()
                                    .ifPresent(btnType -> {
                                        if (btnType == ButtonType.OK) {
                                            try {
                                                java.awt.Desktop.getDesktop().open(
                                                    file
                                                );
                                            } catch (Exception e) {
                                                showAlert(
                                                    "Info",
                                                    "Fichier sauvegarde a: " +
                                                        file.getAbsolutePath(),
                                                    Alert.AlertType.INFORMATION
                                                );
                                            }
                                        }
                                    });
                            }
                        } catch (Exception e) {
                            showAlert(
                                "Erreur",
                                "Erreur lors de la generation du rapport: " +
                                    e.getMessage(),
                                Alert.AlertType.ERROR
                            );
                        }
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

    @FXML
    private void handleExportFinancial() {
        try {
            Dialog<String> dialog = new Dialog<>();
            dialog.setTitle("Export Financier");
            dialog.setHeaderText("Exporter les donnees financieres");

            GridPane grid = new GridPane();
            grid.setHgap(10);
            grid.setVgap(10);
            grid.setPadding(new javafx.geometry.Insets(20));

            ComboBox<String> formatCombo = new ComboBox<>();
            formatCombo.setItems(
                FXCollections.observableArrayList("CSV Simple", "CSV Detail")
            );
            formatCombo.setValue("CSV Simple");

            DatePicker startPicker = new DatePicker(
                LocalDate.now().withDayOfMonth(1)
            );
            DatePicker endPicker = new DatePicker(LocalDate.now());

            grid.add(new Label("Format:"), 0, 0);
            grid.add(formatCombo, 1, 0);
            grid.add(new Label("Date debut:"), 0, 1);
            grid.add(startPicker, 1, 1);
            grid.add(new Label("Date fin:"), 0, 2);
            grid.add(endPicker, 1, 2);

            ButtonType exportButton = new ButtonType(
                "Exporter",
                ButtonBar.ButtonData.OK_DONE
            );
            dialog
                .getDialogPane()
                .getButtonTypes()
                .addAll(exportButton, ButtonType.CANCEL);
            dialog.getDialogPane().setContent(grid);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == exportButton) {
                    return formatCombo.getValue();
                }
                return null;
            });

            dialog
                .showAndWait()
                .ifPresent(format -> {
                    try {
                        LocalDate startDate = startPicker.getValue();
                        LocalDate endDate = endPicker.getValue();

                        if (startDate == null || endDate == null) {
                            showAlert(
                                "Erreur",
                                "Veuillez selectionner les dates",
                                Alert.AlertType.ERROR
                            );
                            return;
                        }

                        if (startDate.isAfter(endDate)) {
                            showAlert(
                                "Erreur",
                                "La date de debut doit etre avant la date de fin",
                                Alert.AlertType.ERROR
                            );
                            return;
                        }

                        List<Facture> filteredInvoices = facturationService
                            .getAllFactures()
                            .stream()
                            .filter(f -> f.getDateFacturation() != null)
                            .filter(
                                f ->
                                    !f
                                        .getDateFacturation()
                                        .isBefore(startDate) &&
                                    !f.getDateFacturation().isAfter(endDate)
                            )
                            .collect(Collectors.toList());

                        javafx.stage.FileChooser fileChooser =
                            new javafx.stage.FileChooser();
                        fileChooser.setTitle("Sauvegarder Export CSV");
                        fileChooser
                            .getExtensionFilters()
                            .add(
                                new javafx.stage.FileChooser.ExtensionFilter(
                                    "CSV Files",
                                    "*.csv"
                                )
                            );
                        fileChooser.setInitialFileName(
                            "export_factures_" +
                                startDate +
                                "_" +
                                endDate +
                                ".csv"
                        );

                        File file = fileChooser.showSaveDialog(
                            invoiceTable.getScene().getWindow()
                        );
                        if (file != null) {
                            if ("CSV Detail".equals(format)) {
                                csvService.exportInvoicesDetailed(
                                    filteredInvoices,
                                    file.getAbsolutePath()
                                );
                            } else {
                                csvService.exportInvoices(
                                    filteredInvoices,
                                    file.getAbsolutePath()
                                );
                            }

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
                });
        } catch (Exception e) {
            showAlert(
                "Erreur",
                "Erreur: " + e.getMessage(),
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
