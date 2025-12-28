package com.mediclinic.controller;

import com.google.zxing.*;
import com.google.zxing.client.j2se.BufferedImageLuminanceSource;
import com.google.zxing.common.HybridBinarizer;
import com.mediclinic.model.RendezVous;
import com.mediclinic.model.RendezVousStatus;
import com.mediclinic.service.QRCodeService;
import com.mediclinic.service.RendezVousService;
import javafx.application.Platform;
import javafx.fxml.FXML;
import javafx.scene.control.*;
import javafx.scene.image.Image;
import javafx.scene.image.ImageView;
import javafx.scene.layout.VBox;
import javafx.stage.FileChooser;
import javafx.stage.Stage;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.File;
import java.io.FileInputStream;
import java.time.format.DateTimeFormatter;

public class QRScannerController {

    @FXML
    private ImageView qrImageView;

    @FXML
    private Button selectImageButton;

    @FXML
    private Button scanButton;

    @FXML
    private Button confirmButton;

    @FXML
    private VBox resultPane;

    @FXML
    private Label statusLabel;

    @FXML
    private Label patientNameLabel;

    @FXML
    private Label patientIdLabel;

    @FXML
    private Label doctorNameLabel;

    @FXML
    private Label appointmentDateLabel;

    @FXML
    private Label appointmentStatusLabel;

    @FXML
    private Label motifLabel;

    @FXML
    private TextArea detailsTextArea;

    private final QRCodeService qrCodeService = new QRCodeService();
    private final RendezVousService rendezVousService = new RendezVousService();
    private File selectedImageFile;
    private QRCodeService.AppointmentData scannedData;
    private RendezVous verifiedAppointment;

    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(
        "dd/MM/yyyy HH:mm"
    );

    @FXML
    public void initialize() {
        resultPane.setVisible(false);
        confirmButton.setVisible(false);
        scanButton.setDisable(true);
    }

    @FXML
    private void handleSelectImage() {
        FileChooser fileChooser = new FileChooser();
        fileChooser.setTitle("Selectionner une image QR Code");
        fileChooser
            .getExtensionFilters()
            .addAll(
                new FileChooser.ExtensionFilter("Images", "*.png", "*.jpg", "*.jpeg", "*.gif", "*.bmp"),
                new FileChooser.ExtensionFilter("Tous les fichiers", "*.*")
            );

        Stage stage = (Stage) selectImageButton.getScene().getWindow();
        selectedImageFile = fileChooser.showOpenDialog(stage);

        if (selectedImageFile != null) {
            try {
                Image image = new Image(new FileInputStream(selectedImageFile));
                qrImageView.setImage(image);
                scanButton.setDisable(false);
                statusLabel.setText("Image selectionnee. Cliquez sur 'Scanner' pour verifier.");
                statusLabel.setStyle("-fx-text-fill: #3498db;");
                resultPane.setVisible(false);
                confirmButton.setVisible(false);
            } catch (Exception e) {
                showError("Erreur lors du chargement de l'image: " + e.getMessage());
            }
        }
    }

    @FXML
    private void handleScan() {
        if (selectedImageFile == null) {
            showError("Aucune image selectionnee");
            return;
        }

        try {
            BufferedImage bufferedImage = ImageIO.read(selectedImageFile);
            LuminanceSource source = new BufferedImageLuminanceSource(bufferedImage);
            BinaryBitmap bitmap = new BinaryBitmap(new HybridBinarizer(source));

            Result result = new MultiFormatReader().decode(bitmap);
            String qrData = result.getText();

            scannedData = qrCodeService.parseQRCode(qrData);

            verifyAppointment(scannedData);
        } catch (NotFoundException e) {
            showError("Aucun QR code trouve dans l'image.");
        } catch (Exception e) {
            showError("Erreur lors du scan: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private void verifyAppointment(QRCodeService.AppointmentData data) {
        if (data.getRendezVousId() == null) {
            showError("QR code invalide: ID de rendez-vous manquant");
            return;
        }

        try {
            verifiedAppointment = rendezVousService.findById(data.getRendezVousId());

            if (verifiedAppointment == null) {
                showError("Rendez-vous non trouve dans le systeme (ID: " + data.getRendezVousId() + ")");
                return;
            }

            boolean isValid = validateAppointmentData(verifiedAppointment, data);

            if (isValid) {
                displayAppointmentInfo(verifiedAppointment, data);
                statusLabel.setText("QR Code VALIDE - Rendez-vous verifie");
                statusLabel.setStyle("-fx-text-fill: #27ae60; -fx-font-weight: bold; -fx-font-size: 14px;");
                confirmButton.setVisible(true);
            } else {
                statusLabel.setText("QR Code INVALIDE - Les donnees ne correspondent pas");
                statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold; -fx-font-size: 14px;");
                resultPane.setVisible(false);
                confirmButton.setVisible(false);
            }
        } catch (Exception e) {
            showError("Erreur lors de la verification: " + e.getMessage());
            e.printStackTrace();
        }
    }

    private boolean validateAppointmentData(
        RendezVous rdv,
        QRCodeService.AppointmentData data
    ) {
        if (!rdv.getId().equals(data.getRendezVousId())) {
            return false;
        }

        if (!rdv.getPatient().getId().equals(data.getPatientId())) {
            return false;
        }

        if (!rdv.getMedecin().getId().equals(data.getDoctorId())) {
            return false;
        }

        String expectedDate = rdv.getDateHeureDebut().format(DATETIME_FORMATTER);
        if (!expectedDate.equals(data.getAppointmentDateTime())) {
            return false;
        }

        return true;
    }

    private void displayAppointmentInfo(
        RendezVous rdv,
        QRCodeService.AppointmentData data
    ) {
        patientNameLabel.setText(rdv.getPatient().getNomComplet());
        patientIdLabel.setText("P-" + rdv.getPatient().getId());
        doctorNameLabel.setText("Dr. " + rdv.getMedecin().getNomComplet());
        appointmentDateLabel.setText(
            rdv.getDateHeureDebut().format(DATETIME_FORMATTER)
        );

        String statusText = "";
        String statusStyle = "";
        switch (rdv.getStatus()) {
            case PLANIFIE:
                statusText = "PLANIFIE";
                statusStyle = "-fx-text-fill: #f39c12; -fx-font-weight: bold;";
                break;
            case CONFIRME:
                statusText = "CONFIRME";
                statusStyle = "-fx-text-fill: #3498db; -fx-font-weight: bold;";
                break;
            case TERMINE:
                statusText = "TERMINE";
                statusStyle = "-fx-text-fill: #95a5a6; -fx-font-weight: bold;";
                break;
            case ANNULE:
                statusText = "ANNULE";
                statusStyle = "-fx-text-fill: #e74c3c; -fx-font-weight: bold;";
                break;
        }
        appointmentStatusLabel.setText(statusText);
        appointmentStatusLabel.setStyle(statusStyle);

        motifLabel.setText(
            rdv.getMotif() != null && !rdv.getMotif().isEmpty()
                ? rdv.getMotif()
                : "Consultation generale"
        );

        StringBuilder details = new StringBuilder();
        details.append("INFORMATIONS DETAILLEES\n");
        details.append("========================\n\n");
        details.append("ID Rendez-vous: ").append(rdv.getId()).append("\n");
        details.append("Patient: ").append(rdv.getPatient().getNomComplet()).append("\n");
        details.append("ID Patient: ").append(rdv.getPatient().getId()).append("\n");

        if (rdv.getPatient().getTelephone() != null) {
            details.append("Telephone: ").append(rdv.getPatient().getTelephone()).append("\n");
        }

        if (rdv.getPatient().getEmail() != null) {
            details.append("Email: ").append(rdv.getPatient().getEmail()).append("\n");
        }

        details.append("\nMedecin: Dr. ").append(rdv.getMedecin().getNomComplet()).append("\n");

        if (rdv.getMedecin().getSpecialite() != null) {
            details.append("Specialite: ").append(rdv.getMedecin().getSpecialite()).append("\n");
        }

        details.append("\nDate debut: ").append(rdv.getDateHeureDebut().format(DATETIME_FORMATTER)).append("\n");
        details.append("Date fin: ").append(rdv.getDateHeureFin().format(DATETIME_FORMATTER)).append("\n");
        details.append("Duree: ").append(rdv.getDuree().toMinutes()).append(" minutes\n");
        details.append("\nStatut: ").append(statusText).append("\n");

        if (rdv.getMotif() != null && !rdv.getMotif().isEmpty()) {
            details.append("Motif: ").append(rdv.getMotif()).append("\n");
        }

        detailsTextArea.setText(details.toString());

        resultPane.setVisible(true);
    }

    @FXML
    private void handleConfirmArrival() {
        if (verifiedAppointment == null) {
            showError("Aucun rendez-vous a confirmer");
            return;
        }

        Alert confirmAlert = new Alert(Alert.AlertType.CONFIRMATION);
        confirmAlert.setTitle("Confirmer l'arrivee");
        confirmAlert.setHeaderText("Confirmer l'arrivee du patient");
        confirmAlert.setContentText(
            "Confirmer l'arrivee de " +
            verifiedAppointment.getPatient().getNomComplet() +
            " pour le rendez-vous avec Dr. " +
            verifiedAppointment.getMedecin().getNomComplet() +
            " ?"
        );

        confirmAlert
            .showAndWait()
            .ifPresent(response -> {
                if (response == ButtonType.OK) {
                    try {
                        if (verifiedAppointment.getStatus() == RendezVousStatus.PLANIFIE) {
                            verifiedAppointment.setStatus(RendezVousStatus.CONFIRME);
                            rendezVousService.updateRendezVous(verifiedAppointment);
                        }

                        Alert successAlert = new Alert(Alert.AlertType.INFORMATION);
                        successAlert.setTitle("Succes");
                        successAlert.setHeaderText("Arrivee confirmee");
                        successAlert.setContentText(
                            "L'arrivee du patient a ete confirmee avec succes.\n" +
                            "Le rendez-vous est maintenant marque comme CONFIRME."
                        );
                        successAlert.showAndWait();

                        appointmentStatusLabel.setText("CONFIRME");
                        appointmentStatusLabel.setStyle("-fx-text-fill: #3498db; -fx-font-weight: bold;");

                        resetScanner();
                    } catch (Exception e) {
                        showError("Erreur lors de la confirmation: " + e.getMessage());
                    }
                }
            });
    }

    @FXML
    private void handleReset() {
        resetScanner();
    }

    private void resetScanner() {
        selectedImageFile = null;
        scannedData = null;
        verifiedAppointment = null;
        qrImageView.setImage(null);
        scanButton.setDisable(true);
        resultPane.setVisible(false);
        confirmButton.setVisible(false);
        statusLabel.setText("Selectionnez une image de QR code pour commencer");
        statusLabel.setStyle("-fx-text-fill: #7f8c8d;");
    }

    private void showError(String message) {
        statusLabel.setText("ERREUR: " + message);
        statusLabel.setStyle("-fx-text-fill: #e74c3c; -fx-font-weight: bold;");
        resultPane.setVisible(false);
        confirmButton.setVisible(false);

        Alert alert = new Alert(Alert.AlertType.ERROR);
        alert.setTitle("Erreur");
        alert.setHeaderText("Erreur de verification");
        alert.setContentText(message);
        alert.showAndWait();
    }
}
