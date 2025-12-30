package com.mediclinic.service;

import com.google.zxing.WriterException;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.colors.ColorConstants;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.borders.Border;
import com.itextpdf.layout.borders.SolidBorder;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.HorizontalAlignment;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.properties.VerticalAlignment;
import com.mediclinic.model.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import javax.imageio.ImageIO;

public class PdfService {

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    private static final DeviceRgb PRIMARY_COLOR = new DeviceRgb(41, 128, 185);
    private static final DeviceRgb SECONDARY_COLOR = new DeviceRgb(52, 73, 94);
    private static final DeviceRgb ACCENT_COLOR = new DeviceRgb(46, 204, 113);
    private static final DeviceRgb LIGHT_GRAY = new DeviceRgb(236, 240, 241);
    private static final DeviceRgb DARK_GRAY = new DeviceRgb(127, 140, 141);
    private static final DeviceRgb SUCCESS_GREEN = new DeviceRgb(39, 174, 96);
    private static final DeviceRgb WARNING_ORANGE = new DeviceRgb(230, 126, 34);
    private static final DeviceRgb DANGER_RED = new DeviceRgb(231, 76, 60);

    public void generateFacturePdf(Facture facture, String destPath)
        throws FileNotFoundException {
        PdfWriter writer = new PdfWriter(destPath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        addHeader(document, "FACTURE", "N° " + facture.getId());

        document.add(new Paragraph("\n"));

        Table infoTable = new Table(2);
        infoTable.setWidth(UnitValue.createPercentValue(100));

        addInfoCell(
            infoTable,
            "Date:",
            facture.getDateFacturation().format(DATE_FORMATTER)
        );
        addInfoCell(
            infoTable,
            "Patient:",
            facture.getPatient().getNomComplet()
        );
        addInfoCell(
            infoTable,
            "Type de Paiement:",
            facture.getTypePaiement() != null
                ? facture.getTypePaiement().name()
                : "Non defini"
        );

        document.add(infoTable);
        document.add(new Paragraph("\n"));

        Paragraph detailsTitle = new Paragraph("Details de la Facture")
            .setFontSize(14)
            .setBold()
            .setFontColor(SECONDARY_COLOR);
        document.add(detailsTitle);
        document.add(new Paragraph("\n").setMarginTop(-5));

        Table itemsTable = new Table(new float[] { 3, 1, 2, 2 });
        itemsTable.setWidth(UnitValue.createPercentValue(100));

        addTableHeader(itemsTable, "Description");
        addTableHeader(itemsTable, "Quantite");
        addTableHeader(itemsTable, "Prix Unitaire");
        addTableHeader(itemsTable, "Total");

        for (LigneFacture ligne : facture.getLignes()) {
            BigDecimal total = ligne
                .getPrixUnitaire()
                .multiply(BigDecimal.valueOf(ligne.getQuantite()));

            addTableCell(itemsTable, ligne.getDescription(), false);
            addTableCell(
                itemsTable,
                String.valueOf(ligne.getQuantite()),
                false
            );
            addTableCell(itemsTable, ligne.getPrixUnitaire() + " MAD", false);
            addTableCell(itemsTable, total + " MAD", false);
        }

        document.add(itemsTable);
        document.add(new Paragraph("\n"));

        Table totalTable = new Table(2);
        totalTable.setWidth(UnitValue.createPercentValue(50));
        totalTable.setHorizontalAlignment(HorizontalAlignment.RIGHT);

        Cell totalLabelCell = new Cell()
            .add(new Paragraph("TOTAL").setBold().setFontSize(16))
            .setBackgroundColor(SECONDARY_COLOR)
            .setFontColor(ColorConstants.WHITE)
            .setPadding(10)
            .setTextAlignment(TextAlignment.RIGHT)
            .setBorder(Border.NO_BORDER);

        Cell totalValueCell = new Cell()
            .add(
                new Paragraph(facture.getMontantTotal() + " MAD")
                    .setBold()
                    .setFontSize(16)
            )
            .setBackgroundColor(PRIMARY_COLOR)
            .setFontColor(ColorConstants.WHITE)
            .setPadding(10)
            .setTextAlignment(TextAlignment.CENTER)
            .setBorder(Border.NO_BORDER);

        totalTable.addCell(totalLabelCell);
        totalTable.addCell(totalValueCell);

        document.add(totalTable);

        addFooter(document);

        document.close();
        System.out.println("PDF generated at: " + destPath);
    }

    public void generateDailyReport(
        List<RendezVous> appointments,
        LocalDate date,
        String destPath
    ) throws FileNotFoundException {
        PdfWriter writer = new PdfWriter(destPath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        addHeader(document, "RAPPORT QUOTIDIEN", date.format(DATE_FORMATTER));

        document.add(new Paragraph("\n"));

        long totalAppointments = appointments.size();
        long confirmedAppointments = appointments
            .stream()
            .filter(rdv -> rdv.getStatus() == RendezVousStatus.CONFIRME)
            .count();
        long completedAppointments = appointments
            .stream()
            .filter(rdv -> rdv.getStatus() == RendezVousStatus.TERMINE)
            .count();
        long cancelledAppointments = appointments
            .stream()
            .filter(rdv -> rdv.getStatus() == RendezVousStatus.ANNULE)
            .count();

        Table statsTable = new Table(4);
        statsTable.setWidth(UnitValue.createPercentValue(100));

        addStatCard(
            statsTable,
            "Total",
            String.valueOf(totalAppointments),
            PRIMARY_COLOR
        );
        addStatCard(
            statsTable,
            "Confirmes",
            String.valueOf(confirmedAppointments),
            new DeviceRgb(52, 152, 219)
        );
        addStatCard(
            statsTable,
            "Termines",
            String.valueOf(completedAppointments),
            SUCCESS_GREEN
        );
        addStatCard(
            statsTable,
            "Annules",
            String.valueOf(cancelledAppointments),
            DANGER_RED
        );

        document.add(statsTable);
        document.add(new Paragraph("\n"));

        if (!appointments.isEmpty()) {
            Paragraph listTitle = new Paragraph("Liste des Rendez-vous")
                .setFontSize(14)
                .setBold()
                .setFontColor(SECONDARY_COLOR);
            document.add(listTitle);
            document.add(new Paragraph("\n").setMarginTop(-5));

            Table table = new Table(new float[] { 1, 2, 2, 3, 1.5f });
            table.setWidth(UnitValue.createPercentValue(100));

            addTableHeader(table, "Heure");
            addTableHeader(table, "Patient");
            addTableHeader(table, "Medecin");
            addTableHeader(table, "Motif");
            addTableHeader(table, "Statut");

            for (RendezVous rdv : appointments) {
                addTableCell(
                    table,
                    rdv.getDateHeureDebut() != null
                        ? rdv
                              .getDateHeureDebut()
                              .format(DateTimeFormatter.ofPattern("HH:mm"))
                        : "",
                    false
                );
                addTableCell(
                    table,
                    rdv.getPatient() != null
                        ? rdv.getPatient().getNomComplet()
                        : "",
                    false
                );
                addTableCell(
                    table,
                    rdv.getMedecin() != null
                        ? rdv.getMedecin().getNomComplet()
                        : "",
                    false
                );
                addTableCell(
                    table,
                    rdv.getMotif() != null ? rdv.getMotif() : "",
                    false
                );

                String status = rdv.getStatus() != null
                    ? rdv.getStatus().name()
                    : "";
                DeviceRgb statusColor = getStatusColor(rdv.getStatus());
                addStatusCell(table, status, statusColor);
            }

            document.add(table);
        }

        addFooter(document);

        document.close();
        System.out.println("Daily report PDF generated at: " + destPath);
    }

    public void generateStatisticsReport(
        Map<String, Object> stats,
        String destPath
    ) throws FileNotFoundException {
        PdfWriter writer = new PdfWriter(destPath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        addHeader(document, "RAPPORT STATISTIQUES", "Vue d'Ensemble");

        document.add(new Paragraph("\n"));

        Paragraph generalTitle = new Paragraph("Statistiques Generales")
            .setFontSize(14)
            .setBold()
            .setFontColor(SECONDARY_COLOR);
        document.add(generalTitle);
        document.add(new Paragraph("\n").setMarginTop(-5));

        Table generalTable = new Table(2);
        generalTable.setWidth(UnitValue.createPercentValue(70));

        if (stats.containsKey("totalPatients")) {
            addInfoCell(
                generalTable,
                "Total Patients:",
                String.valueOf(stats.get("totalPatients"))
            );
        }
        if (stats.containsKey("totalDoctors")) {
            addInfoCell(
                generalTable,
                "Total Medecins:",
                String.valueOf(stats.get("totalDoctors"))
            );
        }
        if (stats.containsKey("totalAppointments")) {
            addInfoCell(
                generalTable,
                "Total Rendez-vous:",
                String.valueOf(stats.get("totalAppointments"))
            );
        }
        if (stats.containsKey("totalRevenue")) {
            addInfoCell(
                generalTable,
                "Revenu Total:",
                stats.get("totalRevenue") + " MAD"
            );
        }

        document.add(generalTable);
        document.add(new Paragraph("\n"));

        if (stats.containsKey("appointmentsByStatus")) {
            Paragraph statusTitle = new Paragraph("Rendez-vous par Statut")
                .setFontSize(14)
                .setBold()
                .setFontColor(SECONDARY_COLOR);
            document.add(statusTitle);
            document.add(new Paragraph("\n").setMarginTop(-5));

            Table statusTable = new Table(new float[] { 3, 1 });
            statusTable.setWidth(UnitValue.createPercentValue(60));

            addTableHeader(statusTable, "Statut");
            addTableHeader(statusTable, "Nombre");

            Map<String, Long> appointmentsByStatus = (Map<
                String,
                Long
            >) stats.get("appointmentsByStatus");
            for (Map.Entry<
                String,
                Long
            > entry : appointmentsByStatus.entrySet()) {
                addTableCell(statusTable, entry.getKey(), false);
                addTableCell(
                    statusTable,
                    String.valueOf(entry.getValue()),
                    true
                );
            }

            document.add(statusTable);
            document.add(new Paragraph("\n"));
        }

        if (stats.containsKey("appointmentsByDoctor")) {
            Paragraph doctorTitle = new Paragraph("Rendez-vous par Medecin")
                .setFontSize(14)
                .setBold()
                .setFontColor(SECONDARY_COLOR);
            document.add(doctorTitle);
            document.add(new Paragraph("\n").setMarginTop(-5));

            Table doctorTable = new Table(new float[] { 3, 1 });
            doctorTable.setWidth(UnitValue.createPercentValue(60));

            addTableHeader(doctorTable, "Medecin");
            addTableHeader(doctorTable, "Rendez-vous");

            Map<String, Long> appointmentsByDoctor = (Map<
                String,
                Long
            >) stats.get("appointmentsByDoctor");
            for (Map.Entry<
                String,
                Long
            > entry : appointmentsByDoctor.entrySet()) {
                addTableCell(doctorTable, entry.getKey(), false);
                addTableCell(
                    doctorTable,
                    String.valueOf(entry.getValue()),
                    true
                );
            }

            document.add(doctorTable);
        }

        addFooter(document);

        document.close();
        System.out.println("Statistics report PDF generated at: " + destPath);
    }

    public void generateFinancialReport(
        List<Facture> invoices,
        LocalDate startDate,
        LocalDate endDate,
        String destPath
    ) throws FileNotFoundException {
        PdfWriter writer = new PdfWriter(destPath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        addHeader(
            document,
            "RAPPORT FINANCIER",
            startDate.format(DATE_FORMATTER) +
                " - " +
                endDate.format(DATE_FORMATTER)
        );

        document.add(new Paragraph("\n"));

        BigDecimal totalRevenue = invoices
            .stream()
            .map(Facture::getMontantTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);

        long totalInvoices = invoices.size();

        Table summaryTable = new Table(2);
        summaryTable.setWidth(UnitValue.createPercentValue(100));

        addStatCard(
            summaryTable,
            "Nombre de Factures",
            String.valueOf(totalInvoices),
            PRIMARY_COLOR
        );
        addStatCard(
            summaryTable,
            "Revenu Total",
            totalRevenue.toString() + " MAD",
            SUCCESS_GREEN
        );

        document.add(summaryTable);
        document.add(new Paragraph("\n"));

        Map<TypePaiement, Long> paymentsByType = invoices
            .stream()
            .filter(f -> f.getTypePaiement() != null)
            .collect(
                Collectors.groupingBy(
                    Facture::getTypePaiement,
                    Collectors.counting()
                )
            );

        if (!paymentsByType.isEmpty()) {
            Paragraph paymentTitle = new Paragraph(
                "Repartition par Type de Paiement"
            )
                .setFontSize(14)
                .setBold()
                .setFontColor(SECONDARY_COLOR);
            document.add(paymentTitle);
            document.add(new Paragraph("\n").setMarginTop(-5));

            Table paymentTable = new Table(new float[] { 3, 1 });
            paymentTable.setWidth(UnitValue.createPercentValue(60));

            addTableHeader(paymentTable, "Type de Paiement");
            addTableHeader(paymentTable, "Nombre");

            for (Map.Entry<
                TypePaiement,
                Long
            > entry : paymentsByType.entrySet()) {
                addTableCell(paymentTable, entry.getKey().name(), false);
                addTableCell(
                    paymentTable,
                    String.valueOf(entry.getValue()),
                    true
                );
            }

            document.add(paymentTable);
            document.add(new Paragraph("\n"));
        }

        if (!invoices.isEmpty()) {
            Paragraph detailTitle = new Paragraph("Detail des Factures")
                .setFontSize(14)
                .setBold()
                .setFontColor(SECONDARY_COLOR);
            document.add(detailTitle);
            document.add(new Paragraph("\n").setMarginTop(-5));

            Table table = new Table(new float[] { 1, 2, 2, 2, 2 });
            table.setWidth(UnitValue.createPercentValue(100));

            addTableHeader(table, "N°");
            addTableHeader(table, "Date");
            addTableHeader(table, "Patient");
            addTableHeader(table, "Montant");
            addTableHeader(table, "Type Paiement");

            for (Facture facture : invoices) {
                addTableCell(table, String.valueOf(facture.getId()), true);
                addTableCell(
                    table,
                    facture.getDateFacturation() != null
                        ? facture.getDateFacturation().format(DATE_FORMATTER)
                        : "",
                    false
                );
                addTableCell(
                    table,
                    facture.getPatient() != null
                        ? facture.getPatient().getNomComplet()
                        : "",
                    false
                );
                addTableCell(
                    table,
                    facture.getMontantTotal().toString() + " MAD",
                    false
                );
                addTableCell(
                    table,
                    facture.getTypePaiement() != null
                        ? facture.getTypePaiement().name()
                        : "N/A",
                    false
                );
            }

            document.add(table);
        }

        addFooter(document);

        document.close();
        System.out.println("Financial report PDF generated at: " + destPath);
    }

    public void generateDossierMedicalPdf(
        DossierMedical dossier,
        List<Consultation> consultations,
        String destPath
    ) throws FileNotFoundException {
        PdfWriter writer = new PdfWriter(destPath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        String patientName = dossier.getPatient() != null ? dossier.getPatient().getNomComplet() : "";
        addHeader(document, "DOSSIER MEDICAL", patientName);

        document.add(new Paragraph("\n"));

        Table infoTable = new Table(2);
        infoTable.setWidth(UnitValue.createPercentValue(100));

        addInfoCell(infoTable, "ID Dossier:", dossier.getId() != null ? String.valueOf(dossier.getId()) : "");
        addInfoCell(infoTable, "Patient:", patientName);
        addInfoCell(infoTable, "Date de création:", dossier.getDateCreation() != null ? dossier.getDateCreation().format(DATE_FORMATTER) : "");

        document.add(infoTable);
        document.add(new Paragraph("\n"));

        Paragraph notesTitle = new Paragraph("Notes générales")
            .setFontSize(14)
            .setBold()
            .setFontColor(SECONDARY_COLOR);
        document.add(notesTitle);
        document.add(new Paragraph("\n").setMarginTop(-5));

        String notes = dossier.getNotesGenerales() != null ? dossier.getNotesGenerales() : "Aucune note";
        Table notesTable = new Table(1);
        notesTable.setWidth(UnitValue.createPercentValue(100));
        Cell notesCell = new Cell()
            .add(new Paragraph(notes).setFontSize(10))
            .setPadding(10)
            .setBackgroundColor(LIGHT_GRAY)
            .setBorder(new SolidBorder(LIGHT_GRAY, 1));
        notesTable.addCell(notesCell);
        document.add(notesTable);

        document.add(new Paragraph("\n"));

        if (consultations != null && !consultations.isEmpty()) {
            Paragraph histTitle = new Paragraph("Historique des consultations")
                .setFontSize(14)
                .setBold()
                .setFontColor(SECONDARY_COLOR);
            document.add(histTitle);
            document.add(new Paragraph("\n").setMarginTop(-5));

            Table table = new Table(new float[] { 2, 2, 3, 3 });
            table.setWidth(UnitValue.createPercentValue(100));

            addTableHeader(table, "Date");
            addTableHeader(table, "Motif");
            addTableHeader(table, "Observations");
            addTableHeader(table, "Diagnostic");

            for (Consultation c : consultations) {
                RendezVous rdv = c.getRendezVous();
                String date = rdv != null && rdv.getDateHeureDebut() != null ? rdv.getDateHeureDebut().format(DATETIME_FORMATTER) : "";
                String motif = rdv != null && rdv.getMotif() != null ? rdv.getMotif() : "";
                String obs = c.getObservations() != null ? c.getObservations() : "";
                String diag = c.getDiagnostic() != null ? c.getDiagnostic() : "";

                addTableCell(table, date, true);
                addTableCell(table, motif, false);
                addTableCell(table, obs, false);
                addTableCell(table, diag, false);
            }

            document.add(table);
        }

        addFooter(document);
        document.close();
    }

    private void addHeader(Document document, String title, String subtitle) {
        Table headerTable = new Table(1);
        headerTable.setWidth(UnitValue.createPercentValue(100));

        Cell headerCell = new Cell()
            .add(
                new Paragraph("CLINIQUE MEDICLINIC")
                    .setFontSize(24)
                    .setBold()
                    .setFontColor(ColorConstants.WHITE)
            )
            .add(
                new Paragraph(title)
                    .setFontSize(18)
                    .setBold()
                    .setFontColor(ColorConstants.WHITE)
                    .setMarginTop(5)
            )
            .add(
                new Paragraph(subtitle)
                    .setFontSize(12)
                    .setFontColor(LIGHT_GRAY)
                    .setMarginTop(3)
            )
            .setBackgroundColor(PRIMARY_COLOR)
            .setPadding(20)
            .setTextAlignment(TextAlignment.CENTER)
            .setBorder(Border.NO_BORDER);

        headerTable.addCell(headerCell);
        document.add(headerTable);
    }

    private void addFooter(Document document) {
        document.add(new Paragraph("\n\n"));

        Table footerTable = new Table(1);
        footerTable.setWidth(UnitValue.createPercentValue(100));
        footerTable.setFixedPosition(
            document.getLeftMargin(),
            document.getBottomMargin() - 40,
            UnitValue.createPercentValue(100)
        );

        Paragraph footerText = new Paragraph(
            "Genere le " +
                LocalDateTime.now().format(DATETIME_FORMATTER) +
                " | MediClinic - Systeme de Gestion Medicale"
        )
            .setFontSize(8)
            .setFontColor(DARK_GRAY)
            .setTextAlignment(TextAlignment.CENTER);

        Cell footerCell = new Cell()
            .add(footerText)
            .setBorder(new SolidBorder(LIGHT_GRAY, 1))
            .setBorderTop(new SolidBorder(PRIMARY_COLOR, 2))
            .setBackgroundColor(new DeviceRgb(250, 250, 250))
            .setPadding(10);

        footerTable.addCell(footerCell);
        document.add(footerTable);
    }

    private void addTableHeader(Table table, String text) {
        Cell cell = new Cell()
            .add(
                new Paragraph(text)
                    .setBold()
                    .setFontSize(10)
                    .setFontColor(ColorConstants.WHITE)
            )
            .setBackgroundColor(SECONDARY_COLOR)
            .setPadding(8)
            .setTextAlignment(TextAlignment.CENTER)
            .setVerticalAlignment(VerticalAlignment.MIDDLE)
            .setBorder(Border.NO_BORDER);
        table.addHeaderCell(cell);
    }

    private void addTableCell(Table table, String text, boolean centered) {
        Cell cell = new Cell()
            .add(new Paragraph(text).setFontSize(9))
            .setPadding(6)
            .setBackgroundColor(ColorConstants.WHITE)
            .setBorder(new SolidBorder(LIGHT_GRAY, 0.5f));

        if (centered) {
            cell.setTextAlignment(TextAlignment.CENTER);
        }

        table.addCell(cell);
    }

    private void addStatusCell(Table table, String status, DeviceRgb color) {
        Cell cell = new Cell()
            .add(
                new Paragraph(status)
                    .setBold()
                    .setFontSize(9)
                    .setFontColor(ColorConstants.WHITE)
            )
            .setPadding(6)
            .setBackgroundColor(color)
            .setTextAlignment(TextAlignment.CENTER)
            .setBorder(Border.NO_BORDER);
        table.addCell(cell);
    }

    private void addStatCard(
        Table table,
        String label,
        String value,
        DeviceRgb color
    ) {
        Cell cell = new Cell()
            .add(
                new Paragraph(label)
                    .setFontSize(10)
                    .setFontColor(DARK_GRAY)
                    .setBold()
            )
            .add(
                new Paragraph(value)
                    .setFontSize(20)
                    .setBold()
                    .setFontColor(color)
                    .setMarginTop(5)
            )
            .setPadding(15)
            .setBackgroundColor(LIGHT_GRAY)
            .setTextAlignment(TextAlignment.CENTER)
            .setBorder(new SolidBorder(color, 2));
        table.addCell(cell);
    }

    private void addInfoCell(Table table, String label, String value) {
        Cell labelCell = new Cell()
            .add(
                new Paragraph(label)
                    .setBold()
                    .setFontSize(10)
                    .setFontColor(SECONDARY_COLOR)
            )
            .setPadding(8)
            .setBackgroundColor(LIGHT_GRAY)
            .setBorder(new SolidBorder(ColorConstants.WHITE, 2));

        Cell valueCell = new Cell()
            .add(new Paragraph(value).setFontSize(10))
            .setPadding(8)
            .setBorder(new SolidBorder(LIGHT_GRAY, 1));

        table.addCell(labelCell);
        table.addCell(valueCell);
    }

    private DeviceRgb getStatusColor(RendezVousStatus status) {
        if (status == null) return DARK_GRAY;

        switch (status) {
            case TERMINE:
                return SUCCESS_GREEN;
            case CONFIRME:
                return new DeviceRgb(52, 152, 219);
            case PLANIFIE:
                return WARNING_ORANGE;
            case ANNULE:
                return DANGER_RED;
            default:
                return DARK_GRAY;
        }
    }

    public void generateAppointmentConfirmationPdf(
        RendezVous rdv,
        String destPath
    ) throws FileNotFoundException, WriterException, IOException {
        if (rdv == null) {
            throw new IllegalArgumentException("RendezVous cannot be null");
        }

        PdfWriter writer = new PdfWriter(destPath);
        PdfDocument pdf = new PdfDocument(writer);
        Document document = new Document(pdf);

        addHeader(
            document,
            "CONFIRMATION DE RENDEZ-VOUS",
            "Ref. RDV-" + rdv.getId()
        );

        document.add(
            new Paragraph("Confirmation de votre rendez-vous")
                .setFontSize(14)
                .setBold()
                .setMarginTop(20)
                .setMarginBottom(15)
                .setFontColor(SECONDARY_COLOR)
        );

        document.add(
            new Paragraph(
                "Nous confirmons votre rendez-vous avec les details suivants :"
            )
                .setFontSize(10)
                .setMarginBottom(15)
                .setFontColor(DARK_GRAY)
        );

        Table infoTable = new Table(2);
        infoTable.setWidth(UnitValue.createPercentValue(100));
        infoTable.setMarginBottom(20);

        addInfoCell(infoTable, "Patient", rdv.getPatient().getNomComplet());
        addInfoCell(
            infoTable,
            "Numero de dossier",
            "P-" + rdv.getPatient().getId()
        );
        addInfoCell(
            infoTable,
            "Medecin",
            "Dr. " + rdv.getMedecin().getNomComplet()
        );
        addInfoCell(
            infoTable,
            "Specialite",
            rdv.getMedecin().getSpecialite() != null
                ? rdv.getMedecin().getSpecialite().toString()
                : "Medecine Generale"
        );
        addInfoCell(
            infoTable,
            "Date et Heure",
            rdv.getDateHeureDebut().format(DATETIME_FORMATTER)
        );
        addInfoCell(
            infoTable,
            "Duree estimee",
            rdv.getDuree().toMinutes() + " minutes"
        );

        if (rdv.getMotif() != null && !rdv.getMotif().isEmpty()) {
            addInfoCell(infoTable, "Motif", rdv.getMotif());
        }

        String statusLabel = "";
        switch (rdv.getStatus()) {
            case PLANIFIE:
                statusLabel = "Planifie";
                break;
            case CONFIRME:
                statusLabel = "Confirme";
                break;
            case TERMINE:
                statusLabel = "Termine";
                break;
            case ANNULE:
                statusLabel = "Annule";
                break;
        }
        addInfoCell(infoTable, "Statut", statusLabel);

        document.add(infoTable);

        QRCodeService qrCodeService = new QRCodeService();
        BufferedImage qrImage = qrCodeService.generateAppointmentQRCodeImage(
            rdv
        );

        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(qrImage, "PNG", baos);
        byte[] qrBytes = baos.toByteArray();

        Image qrCodeImage = new Image(ImageDataFactory.create(qrBytes));
        qrCodeImage.setWidth(200);
        qrCodeImage.setHeight(200);
        qrCodeImage.setHorizontalAlignment(HorizontalAlignment.CENTER);
        qrCodeImage.setMarginTop(20);
        qrCodeImage.setMarginBottom(10);

        document.add(qrCodeImage);

        document.add(
            new Paragraph("QR Code de verification")
                .setFontSize(12)
                .setBold()
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(10)
                .setFontColor(PRIMARY_COLOR)
        );

        document.add(
            new Paragraph(
                "Presentez ce QR code a la secretaire le jour de votre rendez-vous pour une verification rapide."
            )
                .setFontSize(9)
                .setTextAlignment(TextAlignment.CENTER)
                .setMarginBottom(20)
                .setFontColor(DARK_GRAY)
                .setItalic()
        );

        Table instructionsTable = new Table(1);
        instructionsTable.setWidth(UnitValue.createPercentValue(100));
        instructionsTable.setMarginTop(20);
        instructionsTable.setMarginBottom(20);

        Cell instructionsCell = new Cell()
            .add(
                new Paragraph("Instructions importantes :")
                    .setFontSize(11)
                    .setBold()
                    .setFontColor(PRIMARY_COLOR)
            )
            .add(
                new Paragraph(
                    "Veuillez vous presenter 10 minutes avant l'heure du rendez-vous."
                )
                    .setFontSize(9)
                    .setMarginTop(5)
            )
            .add(
                new Paragraph(
                    "Apportez votre carte d'identite et votre carte vitale si applicable."
                )
                    .setFontSize(9)
                    .setMarginTop(3)
            )
            .add(
                new Paragraph(
                    "En cas d'empechement, merci de nous prevenir au moins 24h a l'avance."
                )
                    .setFontSize(9)
                    .setMarginTop(3)
            )
            .add(
                new Paragraph(
                    "N'oubliez pas vos documents medicaux pertinents (ordonnances, examens, etc.)."
                )
                    .setFontSize(9)
                    .setMarginTop(3)
            )
            .setBackgroundColor(new DeviceRgb(240, 248, 255))
            .setPadding(15)
            .setBorder(new SolidBorder(PRIMARY_COLOR, 1));

        instructionsTable.addCell(instructionsCell);
        document.add(instructionsTable);

        addFooter(document);

        document.close();
    }
}
