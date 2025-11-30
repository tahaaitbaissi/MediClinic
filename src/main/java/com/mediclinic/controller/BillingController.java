package com.mediclinic.controller;

import javafx.fxml.FXML;
import javafx.fxml.Initializable;
import javafx.scene.Node;
import javafx.scene.control.*;
import javafx.scene.control.cell.PropertyValueFactory;
import javafx.scene.layout.GridPane;
import javafx.scene.layout.HBox;
import javafx.scene.layout.VBox;
import javafx.collections.FXCollections;
import javafx.collections.ObservableList;
import com.mediclinic.model.*;
import com.mediclinic.service.*;

import java.math.BigDecimal;
import java.net.URL;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.ResourceBundle;
import java.util.stream.Collectors;

public class BillingController implements Initializable {

    @FXML private TableView<Facture> invoiceTable;
    @FXML private TableColumn<Facture, Long> colId;
    @FXML private TableColumn<Facture, String> colDate;
    @FXML private TableColumn<Facture, String> colPatient;
    @FXML private TableColumn<Facture, String> colAmount;
    @FXML private TableColumn<Facture, String> colStatus;
    @FXML private TableColumn<Facture, String> colPaymentType;
    @FXML private DatePicker startDatePicker;
    @FXML private DatePicker endDatePicker;
    @FXML private ComboBox<String> statusCombo;
    @FXML private TextField searchField;
    @FXML private Label totalRevenueLabel;
    @FXML private Label paidInvoicesLabel;
    @FXML private Label unpaidInvoicesLabel;
    @FXML private Label averageInvoiceLabel;

    private FacturationService facturationService;
    private PatientService patientService;
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
            showAlert("Accès refusé", e.getMessage(), javafx.scene.control.Alert.AlertType.ERROR);
            return;
        }
        
        facturationService = new FacturationService();
        patientService = new PatientService();
        
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
                date != null ? date.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")) : "N/A"
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
                payee ? "✅ Payée" : "⏰ En attente"
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
        colActions.setPrefWidth(200);
        colActions.setCellFactory(param -> new TableCell<Facture, Void>() {
            private final Button viewBtn = new Button("👁️");
            private final Button payBtn = new Button("💰");
            private final Button printBtn = new Button("🖨️");

            {
                viewBtn.getStyleClass().add("btn-primary");
                payBtn.getStyleClass().add("btn-success");
                printBtn.getStyleClass().add("btn-warning");

                viewBtn.setOnAction(event -> {
                    Facture facture = getTableView().getItems().get(getIndex());
                    showInvoiceDetails(facture);
                });

                payBtn.setOnAction(event -> {
                    Facture facture = getTableView().getItems().get(getIndex());
                    markAsPaid(facture);
                });

                printBtn.setOnAction(event -> {
                    Facture facture = getTableView().getItems().get(getIndex());
                    printInvoice(facture);
                });
            }

            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    Facture facture = getTableView().getItems().get(getIndex());
                    HBox buttons = new HBox(5);
                    buttons.getChildren().add(viewBtn);
                    
                    // Afficher le bouton payer uniquement si la facture n'est pas payée
                    if (facture != null && !facture.isEstPayee()) {
                        payBtn.setDisable(false);
                        buttons.getChildren().add(payBtn);
                    }
                    
                    buttons.getChildren().add(printBtn);
                    setGraphic(buttons);
                }
            }
        });

        invoiceTable.getColumns().add(colActions);
    }

    private void setupStatusFilter() {
        statusCombo.setItems(FXCollections.observableArrayList(
                "Tous les statuts", "Payée", "En attente"
        ));
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
            showAlert("Erreur", "Erreur lors du chargement des factures: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void filterInvoices() {
        try {
            List<Facture> allInvoices = facturationService.getAllFactures();
            List<Facture> filtered = allInvoices.stream()
                .filter(facture -> {
                    String selectedStatus = statusCombo.getValue();
                    if (selectedStatus != null && !selectedStatus.equals("Tous les statuts")) {
                        if (selectedStatus.equals("Payée") && !facture.isEstPayee()) return false;
                        if (selectedStatus.equals("En attente") && facture.isEstPayee()) return false;
                    }
                    LocalDate startDate = startDatePicker != null ? startDatePicker.getValue() : null;
                    if (startDate != null && facture.getDateFacturation() != null) {
                        if (facture.getDateFacturation().isBefore(startDate)) return false;
                    }
                    LocalDate endDate = endDatePicker != null ? endDatePicker.getValue() : null;
                    if (endDate != null && facture.getDateFacturation() != null) {
                        if (facture.getDateFacturation().isAfter(endDate)) return false;
                    }
                    return true;
                })
                .collect(Collectors.toList());
            invoiceList = FXCollections.observableArrayList(filtered);
            invoiceTable.setItems(invoiceList);
        } catch (Exception e) {
            e.printStackTrace();
            showAlert("Erreur", "Erreur lors du filtrage: " + e.getMessage(), Alert.AlertType.ERROR);
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
                ? totalRevenue.divide(BigDecimal.valueOf(paidCount), 2, java.math.RoundingMode.HALF_UP) 
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
            showAlert("Accès refusé", "Vous n'avez pas la permission de créer une facture.", Alert.AlertType.WARNING);
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

            // Invoice lines
            ListView<String> linesListView = new ListView<>();
            ObservableList<LigneFacture> lignes = FXCollections.observableArrayList();
            
            Button addLineBtn = new Button("+ Ajouter une ligne");
            addLineBtn.setOnAction(e -> {
                TextInputDialog lineDialog = new TextInputDialog();
                lineDialog.setTitle("Nouvelle ligne");
                lineDialog.setHeaderText("Ajouter une ligne de facturation");
                lineDialog.setContentText("Description:");
                
                lineDialog.showAndWait().ifPresent(description -> {
                    TextInputDialog priceDialog = new TextInputDialog();
                    priceDialog.setTitle("Prix");
                    priceDialog.setContentText("Prix unitaire:");
                    
                    priceDialog.showAndWait().ifPresent(priceStr -> {
                        try {
                            BigDecimal price = new BigDecimal(priceStr);
                            LigneFacture ligne = new LigneFacture();
                            ligne.setDescription(description);
                            ligne.setPrixUnitaire(price);
                            ligne.setQuantite(1);
                            lignes.add(ligne);
                            linesListView.getItems().add(description + " - " + price + " €");
                        } catch (NumberFormatException ex) {
                            showAlert("Erreur", "Prix invalide", Alert.AlertType.ERROR);
                        }
                    });
                });
            });

            content.getChildren().addAll(
                new Label("Patient:"), patientCombo,
                new Label("Lignes de facturation:"), linesListView,
                addLineBtn
            );

            dialog.getDialogPane().setContent(content);
            
            ButtonType saveButtonType = new ButtonType("Créer", ButtonBar.ButtonData.OK_DONE);
            dialog.getDialogPane().getButtonTypes().addAll(saveButtonType, ButtonType.CANCEL);

            dialog.setResultConverter(dialogButton -> {
                if (dialogButton == saveButtonType) {
                    // Retourner un marqueur non-null pour indiquer que l'utilisateur veut sauvegarder
                    return new Facture(); // Marqueur
                }
                return null;
            });

            dialog.showAndWait().ifPresent(result -> {
                // Vérifier que le patient est sélectionné et qu'il y a des lignes
                if (patientCombo.getValue() == null) {
                    showAlert("Erreur", "Veuillez sélectionner un patient.", Alert.AlertType.ERROR);
                    return;
                }
                if (lignes.isEmpty()) {
                    showAlert("Erreur", "Veuillez ajouter au moins une ligne de facturation.", Alert.AlertType.ERROR);
                    return;
                }
                
                try {
                    facturationService.creerFacture(patientCombo.getValue().getId(), new ArrayList<>(lignes));
                    loadInvoices();
                    updateStatistics();
                    showAlert("Succès", "Facture créée avec succès!", Alert.AlertType.INFORMATION);
                } catch (Exception ex) {
                    showAlert("Erreur", "Erreur lors de la création: " + ex.getMessage(), Alert.AlertType.ERROR);
                }
            });

        } catch (Exception e) {
            showAlert("Erreur", "Erreur: " + e.getMessage(), Alert.AlertType.ERROR);
        }
    }

    private void showInvoiceDetails(Facture facture) {
        if (facture == null) return;
        
        StringBuilder details = new StringBuilder();
        details.append("Facture N°").append(facture.getId()).append("\n\n");
        details.append("Patient: ").append(facture.getPatient() != null ? facture.getPatient().getNomComplet() : "N/A").append("\n");
        details.append("Date: ").append(facture.getDateFacturation()).append("\n");
        details.append("Montant Total: ").append(facture.getMontantTotal()).append(" €\n");
        details.append("Statut: ").append(facture.isEstPayee() ? "Payée" : "En attente").append("\n");
        
        if (facture.getTypePaiement() != null) {
            details.append("Mode de paiement: ").append(facture.getTypePaiement().name()).append("\n");
        }
        
        details.append("\nLignes:\n");
        if (facture.getLignes() != null) {
            for (LigneFacture ligne : facture.getLignes()) {
                details.append("- ").append(ligne.getDescription())
                       .append(": ").append(ligne.getMontantLigne()).append(" €\n");
            }
        }
        
        showAlert("Détails de la Facture", details.toString(), Alert.AlertType.INFORMATION);
    }

    private void markAsPaid(Facture facture) {
        if (facture == null || facture.isEstPayee()) return;
        
        ChoiceDialog<TypePaiement> dialog = new ChoiceDialog<>(TypePaiement.CARTE_CREDIT, TypePaiement.values());
        dialog.setTitle("Marquer comme payée");
        dialog.setHeaderText("Sélectionner le mode de paiement");
        dialog.setContentText("Mode de paiement:");
        
        dialog.showAndWait().ifPresent(typePaiement -> {
            try {
                facturationService.marquerCommePayee(facture.getId(), typePaiement);
                loadInvoices();
                updateStatistics();
                showAlert("Succès", "Facture marquée comme payée!", Alert.AlertType.INFORMATION);
            } catch (Exception e) {
                showAlert("Erreur", "Erreur: " + e.getMessage(), Alert.AlertType.ERROR);
            }
        });
    }

    private void printInvoice(Facture facture) {
        showAlert("Impression", "Fonction d'impression à implémenter", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleSearchInvoices() {
        String searchText = searchField.getText();
        if (searchText == null || searchText.trim().isEmpty()) {
            invoiceTable.setItems(invoiceList);
        } else {
            ObservableList<Facture> filtered = invoiceList.stream()
                .filter(facture -> 
                    facture.getPatient() != null && 
                    facture.getPatient().getNomComplet().toLowerCase().contains(searchText.toLowerCase())
                )
                .collect(Collectors.toCollection(FXCollections::observableArrayList));
            invoiceTable.setItems(filtered);
        }
    }

    @FXML
    private void handleGenerateReport() {
        showAlert("Rapport Financier", "Génération du rapport financier (à implémenter)", Alert.AlertType.INFORMATION);
    }

    @FXML
    private void handleExportFinancial() {
        showAlert("Export", "Export des données financières (à implémenter)", Alert.AlertType.INFORMATION);
    }

    private void showAlert(String title, String message, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(message);
        alert.showAndWait();
    }
}
