package com.mediclinic.controller;

import com.mediclinic.dao.DossierMedicalDAO;
import com.mediclinic.model.Consultation;
import com.mediclinic.model.DossierMedical;
import com.mediclinic.model.RendezVous;
import com.mediclinic.service.ConsultationService;
import com.mediclinic.service.PdfService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;
import javafx.stage.FileChooser;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.ArrayList;

public class DossierController {

    @FXML
    private Label patientNameLabel;
    @FXML
    private TextArea notesGeneralesArea;
    @FXML
    private TableView<Consultation> consultationTable;
    @FXML
    private TableColumn<Consultation, String> colDate;
    @FXML
    private TableColumn<Consultation, String> colMotif;
    @FXML
    private TableColumn<Consultation, Void> colActions;

    private final ConsultationService consultationService = new ConsultationService();
    private final DossierMedicalDAO dossierDAO = new DossierMedicalDAO();
    private final PdfService pdfService = new PdfService();
    private DossierMedical dossier;

    public void initData(DossierMedical dossier) {
        this.dossier = dossier;
        if (dossier != null) {
            if (dossier.getPatient() != null) {
                patientNameLabel.setText(dossier.getPatient().getNomComplet());
            }
            notesGeneralesArea.setText(dossier.getNotesGenerales());
            setupTable();
            loadConsultations();
        }
    }

    private void setupTable() {
        colDate.setCellValueFactory(cellData -> {
            Consultation c = cellData.getValue();
            RendezVous rdv = c.getRendezVous();
            String date = rdv != null && rdv.getDateHeureDebut() != null
                    ? rdv.getDateHeureDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm"))
                    : "";
            return new javafx.beans.property.SimpleStringProperty(date);
        });
        colMotif.setCellValueFactory(cellData -> {
            RendezVous rdv = cellData.getValue().getRendezVous();
            String motif = rdv != null && rdv.getMotif() != null ? rdv.getMotif() : "";
            return new javafx.beans.property.SimpleStringProperty(motif);
        });
        colActions.setCellFactory(param -> new TableCell<>() {
            private final Button viewBtn = new Button("Voir");
            private final Button editBtn = new Button("Éditer");
            {
                viewBtn.getStyleClass().addAll("btn-primary", "btn-actions");
                editBtn.getStyleClass().addAll("btn-warning", "btn-actions");
                viewBtn.setOnAction(e -> {
                    Consultation c = getTableView().getItems().get(getIndex());
                    openConsultation(c);
                });
                editBtn.setOnAction(e -> {
                    Consultation c = getTableView().getItems().get(getIndex());
                    openConsultation(c);
                });
            }
            @Override
            protected void updateItem(Void item, boolean empty) {
                super.updateItem(item, empty);
                if (empty) {
                    setGraphic(null);
                } else {
                    setGraphic(new HBox(5, viewBtn, editBtn));
                }
            }
        });
    }

    private void loadConsultations() {
        List<Consultation> consultations = consultationService.getConsultationsByDossier(dossier.getId());
        consultationTable.setItems(FXCollections.observableArrayList(consultations));
    }

    private void openConsultation(Consultation consultation) {
        try {
            ConsultationEditorController.openEditor(consultation);
        } catch (IOException e) {
            Alert alert = new Alert(Alert.AlertType.ERROR, "Erreur d'ouverture: " + e.getMessage());
            alert.showAndWait();
        }
    }

    @FXML
    private void handleSaveNotes() {
        if (dossier == null) {
            new Alert(Alert.AlertType.WARNING, "Aucun dossier chargé.").showAndWait();
            return;
        }
        try {
            dossier.setNotesGenerales(notesGeneralesArea.getText());
            dossier = dossierDAO.save(dossier);
            new Alert(Alert.AlertType.INFORMATION, "Notes générales enregistrées.").showAndWait();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Erreur lors de l'enregistrement des notes: " + ex.getMessage()).showAndWait();
        }
    }

    @FXML
    private void handleExportPdf() {
        if (dossier == null) {
            new Alert(Alert.AlertType.WARNING, "Aucun dossier chargé.").showAndWait();
            return;
        }
        try {
            FileChooser fileChooser = new FileChooser();
            fileChooser.setTitle("Exporter Dossier en PDF");
            fileChooser.getExtensionFilters().add(new FileChooser.ExtensionFilter("Fichiers PDF", "*.pdf"));
            String patientName = dossier.getPatient() != null ? dossier.getPatient().getNomComplet().replaceAll("[\\\\/:*?\"<>|]", "_") : "patient";
            fileChooser.setInitialFileName("dossier_" + patientName + "_" + (dossier.getId() != null ? dossier.getId() : "")+ ".pdf");
            Stage stage = (Stage) consultationTable.getScene().getWindow();
            java.io.File file = fileChooser.showSaveDialog(stage);
            if (file == null) return;

            List<Consultation> consultations;
            if (consultationTable.getItems() != null && !consultationTable.getItems().isEmpty()) {
                consultations = new ArrayList<>(consultationTable.getItems());
            } else {
                consultations = consultationService.getConsultationsByDossier(dossier.getId());
            }

            pdfService.generateDossierMedicalPdf(dossier, consultations, file.getAbsolutePath());
            new Alert(Alert.AlertType.INFORMATION, "PDF exporté: " + file.getAbsolutePath()).showAndWait();
        } catch (Exception ex) {
            new Alert(Alert.AlertType.ERROR, "Erreur d'export PDF: " + ex.getMessage()).showAndWait();
        }
    }

    @FXML
    private void handleClose() {
        Stage stage = (Stage) consultationTable.getScene().getWindow();
        stage.close();
    }

    public static void openForDossier(DossierMedical dossier) throws IOException {
        FXMLLoader loader = new FXMLLoader(DossierController.class.getResource("/fxml/dossier_view.fxml"));
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Dossier Médical");
        dialog.setScene(new Scene(loader.load()));
        DossierController controller = loader.getController();
        controller.initData(dossier);
        dialog.showAndWait();
    }
}
