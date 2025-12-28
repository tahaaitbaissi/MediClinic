package com.mediclinic.service;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import com.mediclinic.model.RendezVous;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.time.format.DateTimeFormatter;
import java.util.HashMap;
import java.util.Map;
import javax.imageio.ImageIO;

public class QRCodeService {

    private static final int QR_CODE_SIZE = 300;
    private static final DateTimeFormatter DATETIME_FORMATTER = DateTimeFormatter.ofPattern(
        "dd/MM/yyyy HH:mm"
    );

    public byte[] generateAppointmentQRCode(RendezVous rdv)
        throws WriterException, IOException {
        if (rdv == null || rdv.getId() == null) {
            throw new IllegalArgumentException(
                "RendezVous must not be null and must have an ID"
            );
        }

        String qrData = buildQRData(rdv);

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(
            qrData,
            BarcodeFormat.QR_CODE,
            QR_CODE_SIZE,
            QR_CODE_SIZE,
            hints
        );

        BufferedImage qrImage = MatrixToImageWriter.toBufferedImage(bitMatrix);

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(qrImage, "PNG", baos);
        return baos.toByteArray();
    }

    public BufferedImage generateAppointmentQRCodeImage(RendezVous rdv)
        throws WriterException {
        if (rdv == null || rdv.getId() == null) {
            throw new IllegalArgumentException(
                "RendezVous must not be null and must have an ID"
            );
        }

        String qrData = buildQRData(rdv);

        Map<EncodeHintType, Object> hints = new HashMap<>();
        hints.put(EncodeHintType.ERROR_CORRECTION, ErrorCorrectionLevel.H);
        hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");
        hints.put(EncodeHintType.MARGIN, 1);

        QRCodeWriter qrCodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix = qrCodeWriter.encode(
            qrData,
            BarcodeFormat.QR_CODE,
            QR_CODE_SIZE,
            QR_CODE_SIZE,
            hints
        );

        return MatrixToImageWriter.toBufferedImage(bitMatrix);
    }

    private String buildQRData(RendezVous rdv) {
        StringBuilder data = new StringBuilder();

        data.append("MEDICLINIC_RDV\n");
        data.append("ID:").append(rdv.getId()).append("\n");

        data
            .append("PATIENT:")
            .append(rdv.getPatient().getNomComplet())
            .append("\n");
        data
            .append("PATIENT_ID:")
            .append(rdv.getPatient().getId())
            .append("\n");

        data
            .append("MEDECIN:Dr. ")
            .append(rdv.getMedecin().getNomComplet())
            .append("\n");
        data
            .append("MEDECIN_ID:")
            .append(rdv.getMedecin().getId())
            .append("\n");

        data
            .append("DATE:")
            .append(rdv.getDateHeureDebut().format(DATETIME_FORMATTER))
            .append("\n");

        data
            .append("DATE_FIN:")
            .append(rdv.getDateHeureFin().format(DATETIME_FORMATTER))
            .append("\n");

        data.append("STATUS:").append(rdv.getStatus().name()).append("\n");

        if (rdv.getMotif() != null && !rdv.getMotif().isEmpty()) {
            data.append("MOTIF:").append(rdv.getMotif()).append("\n");
        }

        return data.toString();
    }

    public AppointmentData parseQRCode(String qrData) {
        if (
            qrData == null ||
            !qrData.startsWith("MEDICLINIC_RDV")
        ) {
            throw new IllegalArgumentException(
                "Invalid QR code data format"
            );
        }

        AppointmentData appointmentData = new AppointmentData();
        String[] lines = qrData.split("\n");

        for (String line : lines) {
            if (line.contains(":")) {
                String[] parts = line.split(":", 2);
                String key = parts[0].trim();
                String value = parts.length > 1 ? parts[1].trim() : "";

                switch (key) {
                    case "ID":
                        appointmentData.setRendezVousId(
                            Long.parseLong(value)
                        );
                        break;
                    case "PATIENT":
                        appointmentData.setPatientName(value);
                        break;
                    case "PATIENT_ID":
                        appointmentData.setPatientId(Long.parseLong(value));
                        break;
                    case "MEDECIN":
                        appointmentData.setDoctorName(value);
                        break;
                    case "MEDECIN_ID":
                        appointmentData.setDoctorId(Long.parseLong(value));
                        break;
                    case "DATE":
                        appointmentData.setAppointmentDateTime(value);
                        break;
                    case "DATE_FIN":
                        appointmentData.setAppointmentEndDateTime(value);
                        break;
                    case "STATUS":
                        appointmentData.setStatus(value);
                        break;
                    case "MOTIF":
                        appointmentData.setMotif(value);
                        break;
                }
            }
        }

        return appointmentData;
    }

    public static class AppointmentData {

        private Long rendezVousId;
        private String patientName;
        private Long patientId;
        private String doctorName;
        private Long doctorId;
        private String appointmentDateTime;
        private String appointmentEndDateTime;
        private String status;
        private String motif;

        public Long getRendezVousId() {
            return rendezVousId;
        }

        public void setRendezVousId(Long rendezVousId) {
            this.rendezVousId = rendezVousId;
        }

        public String getPatientName() {
            return patientName;
        }

        public void setPatientName(String patientName) {
            this.patientName = patientName;
        }

        public Long getPatientId() {
            return patientId;
        }

        public void setPatientId(Long patientId) {
            this.patientId = patientId;
        }

        public String getDoctorName() {
            return doctorName;
        }

        public void setDoctorName(String doctorName) {
            this.doctorName = doctorName;
        }

        public Long getDoctorId() {
            return doctorId;
        }

        public void setDoctorId(Long doctorId) {
            this.doctorId = doctorId;
        }

        public String getAppointmentDateTime() {
            return appointmentDateTime;
        }

        public void setAppointmentDateTime(String appointmentDateTime) {
            this.appointmentDateTime = appointmentDateTime;
        }

        public String getAppointmentEndDateTime() {
            return appointmentEndDateTime;
        }

        public void setAppointmentEndDateTime(String appointmentEndDateTime) {
            this.appointmentEndDateTime = appointmentEndDateTime;
        }

        public String getStatus() {
            return status;
        }

        public void setStatus(String status) {
            this.status = status;
        }

        public String getMotif() {
            return motif;
        }

        public void setMotif(String motif) {
            this.motif = motif;
        }

        @Override
        public String toString() {
            return (
                "AppointmentData{" +
                "rendezVousId=" +
                rendezVousId +
                ", patientName='" +
                patientName +
                '\'' +
                ", patientId=" +
                patientId +
                ", doctorName='" +
                doctorName +
                '\'' +
                ", doctorId=" +
                doctorId +
                ", appointmentDateTime='" +
                appointmentDateTime +
                '\'' +
                ", appointmentEndDateTime='" +
                appointmentEndDateTime +
                '\'' +
                ", status='" +
                status +
                '\'' +
                ", motif='" +
                motif +
                '\'' +
                '}'
            );
        }
    }
}
