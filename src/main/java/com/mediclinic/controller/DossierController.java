package com.mediclinic.controller;

import com.mediclinic.model.Consultation;
import com.mediclinic.model.DossierMedical;
import com.mediclinic.model.RendezVous;
import com.mediclinic.service.ConsultationService;
import javafx.collections.FXCollections;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.*;
import javafx.scene.layout.HBox;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.List;

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
