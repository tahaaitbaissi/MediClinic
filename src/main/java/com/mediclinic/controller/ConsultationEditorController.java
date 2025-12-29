package com.mediclinic.controller;

import com.mediclinic.model.Consultation;
import com.mediclinic.model.RendezVous;
import com.mediclinic.service.ConsultationService;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Button;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.Modality;
import javafx.stage.Stage;

import java.io.IOException;
import java.time.format.DateTimeFormatter;

public class ConsultationEditorController {

    @FXML
    private Label titleLabel;
    @FXML
    private Label patientLabel;
    @FXML
    private Label dateLabel;
    @FXML
    private Label motifLabel;
    @FXML
    private TextArea observationsArea;
    @FXML
    private TextArea diagnosticArea;
    @FXML
    private TextArea prescriptionsArea;
    @FXML
    private Button saveBtn;

    private final ConsultationService consultationService = new ConsultationService();
    private Consultation consultation;

    public void initData(Consultation consultation) {
        this.consultation = consultation;
        if (consultation != null) {
            RendezVous rdv = consultation.getRendezVous();
            titleLabel.setText("Consultation #" + consultation.getId());
            if (rdv != null) {
                if (rdv.getPatient() != null) {
                    patientLabel.setText(rdv.getPatient().getNomComplet());
                }
                if (rdv.getDateHeureDebut() != null) {
                    dateLabel.setText(rdv.getDateHeureDebut().format(DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")));
                }
                motifLabel.setText(rdv.getMotif() != null ? rdv.getMotif() : "");
            }
            observationsArea.setText(consultation.getObservations());
            diagnosticArea.setText(consultation.getDiagnostic());
            prescriptionsArea.setText(consultation.getPrescriptions());
        }
    }

    @FXML
    private void handleSave() {
        if (consultation != null) {
            consultationService.updateConsultationNotes(
                    consultation.getId(),
                    observationsArea.getText(),
                    diagnosticArea.getText(),
                    prescriptionsArea.getText()
            );
            closeWindow();
        }
    }

    @FXML
    private void handleClose() {
        closeWindow();
    }

    private void closeWindow() {
        Stage stage = (Stage) saveBtn.getScene().getWindow();
        stage.close();
    }

    public static void openEditor(Consultation consultation) throws IOException {
        FXMLLoader loader = new FXMLLoader(ConsultationEditorController.class.getResource("/fxml/consultation_editor.fxml"));
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Éditer la consultation");
        dialog.setScene(new Scene(loader.load()));
        ConsultationEditorController controller = loader.getController();
        controller.initData(consultation);
        dialog.showAndWait();
    }
}
