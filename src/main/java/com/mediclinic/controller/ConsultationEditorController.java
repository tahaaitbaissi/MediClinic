package com.mediclinic.controller;

import com.mediclinic.model.Consultation;
import com.mediclinic.model.RendezVous;
import com.mediclinic.service.ConsultationService;
import com.mediclinic.service.PdfService;
import com.mediclinic.util.UserSession;
import java.io.File;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import javafx.fxml.FXML;
import javafx.fxml.FXMLLoader;
import javafx.scene.Scene;
import javafx.scene.control.Alert;
import javafx.scene.control.Button;
import javafx.scene.control.CheckBox;
import javafx.scene.control.Label;
import javafx.scene.control.TextArea;
import javafx.stage.FileChooser;
import javafx.stage.Modality;
import javafx.stage.Stage;

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

    @FXML
    private Button exportPdfBtn;

    @FXML
    private CheckBox includeSignatureCheckBox;

    private final ConsultationService consultationService =
        new ConsultationService();
    private final PdfService pdfService = new PdfService();
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
                    dateLabel.setText(
                        rdv
                            .getDateHeureDebut()
                            .format(
                                DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm")
                            )
                    );
                }
                motifLabel.setText(
                    rdv.getMotif() != null ? rdv.getMotif() : ""
                );
            }
            observationsArea.setText(consultation.getObservations());
            diagnosticArea.setText(consultation.getDiagnostic());
            prescriptionsArea.setText(consultation.getPrescriptions());
        }

        // Check if current doctor has a signature
        updateSignatureCheckbox();
    }

    private void updateSignatureCheckbox() {
        if (includeSignatureCheckBox != null) {
            Long medecinId = UserSession.getMedecinId();
            if (medecinId != null) {
                boolean hasSignature = pdfService.hasSignature(medecinId);
                includeSignatureCheckBox.setDisable(!hasSignature);
                if (!hasSignature) {
                    includeSignatureCheckBox.setSelected(false);
                    includeSignatureCheckBox.setText(
                        "Inclure signature (non configurée)"
                    );
                } else {
                    includeSignatureCheckBox.setSelected(true);
                    includeSignatureCheckBox.setText(
                        "Inclure signature électronique"
                    );
                }
            }
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
    private void handleExportPdf() {
        if (consultation == null) {
            showAlert(
                "Erreur",
                "Aucune consultation à exporter",
                Alert.AlertType.ERROR
            );
            return;
        }

        // Save changes first
        handleSave();

        // Choose file location
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Exporter la consultation en PDF");
        fileChooser.setInitialFileName(
            "consultation_" + consultation.getId() + ".pdf"
        );
        fileChooser
            .getExtensionFilters()
            .add(new FileChooser.ExtensionFilter("PDF Files", "*.pdf"));

        Stage stage = (Stage) saveBtn.getScene().getWindow();
        File file = fileChooser.showSaveDialog(stage);

        if (file != null) {
            try {
                // Generate PDF with or without signature
                boolean includeSignature =
                    includeSignatureCheckBox != null &&
                    includeSignatureCheckBox.isSelected();

                // Re-fetch consultation with all relationships to avoid LazyInitializationException
                Consultation consultationWithDetails =
                    consultationService.findByIdWithAllDetails(
                        consultation.getId()
                    );

                if (consultationWithDetails == null) {
                    throw new IllegalStateException("Consultation introuvable");
                }

                // Generate the PDF
                pdfService.generateConsultationPdf(
                    consultationWithDetails,
                    file.getAbsolutePath(),
                    includeSignature
                );

                showAlert(
                    "Succès",
                    "PDF exporté avec succès vers:\n" +
                        file.getAbsolutePath() +
                        "\n\n" +
                        (includeSignature
                            ? "✓ Signature électronique incluse."
                            : "Sans signature."),
                    Alert.AlertType.INFORMATION
                );

                System.out.println(
                    "✓ PDF exported to: " + file.getAbsolutePath()
                );
                System.out.println("Include signature: " + includeSignature);
            } catch (Exception e) {
                System.err.println("Error exporting PDF: " + e.getMessage());
                e.printStackTrace();
                showAlert(
                    "Erreur",
                    "Erreur lors de l'export PDF: " + e.getMessage(),
                    Alert.AlertType.ERROR
                );
            }
        }
    }

    @FXML
    private void handleClose() {
        closeWindow();
    }

    private void showAlert(String title, String content, Alert.AlertType type) {
        Alert alert = new Alert(type);
        alert.setTitle(title);
        alert.setHeaderText(null);
        alert.setContentText(content);
        alert.showAndWait();
    }

    private void closeWindow() {
        Stage stage = (Stage) saveBtn.getScene().getWindow();
        stage.close();
    }

    public static void openEditor(Consultation consultation)
        throws IOException {
        FXMLLoader loader = new FXMLLoader(
            ConsultationEditorController.class.getResource(
                "/fxml/consultation_editor.fxml"
            )
        );
        Stage dialog = new Stage();
        dialog.initModality(Modality.APPLICATION_MODAL);
        dialog.setTitle("Éditer la consultation");
        dialog.setScene(new Scene(loader.load()));
        ConsultationEditorController controller = loader.getController();
        controller.initData(consultation);
        dialog.showAndWait();
    }
}
