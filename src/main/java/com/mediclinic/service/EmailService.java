package com.mediclinic.service;

import com.mediclinic.model.RendezVous;
import com.mediclinic.util.ConfigurationManager;
import jakarta.mail.*;
import jakarta.mail.internet.*;
import java.io.File;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.format.DateTimeFormatter;
import java.util.Properties;

public class EmailService {

    private final ConfigurationManager config;
    private final String username;
    private final String password;
    private final PdfService pdfService;
    private static final DateTimeFormatter DATETIME_FORMATTER =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public EmailService() {
        config = ConfigurationManager.getInstance();
        username = config.getEmailUsername();
        password = config.getEmailPassword();
        pdfService = new PdfService();

        if (username == null || username.equals("votre.email@gmail.com")) {
            System.err.println(
                "WARNING: Email username not configured properly!"
            );
            System.err.println(
                "Please create 'application-local.properties' with your email credentials."
            );
        }

        if (password == null || password.contains("abcd efgh")) {
            System.err.println(
                "WARNING: Email password not configured properly!"
            );
            System.err.println(
                "Please set your app password in 'application-local.properties'."
            );
        }
    }

    public void sendEmailWithAttachment(
        String toEmail,
        String subject,
        String body,
        String filePath
    ) {
        sendEmailWithAttachment(toEmail, subject, body, filePath, null);
    }

    private void sendEmailWithAttachment(
        String toEmail,
        String subject,
        String body,
        String filePath,
        Path tempFileToDelete
    ) {
        if (!isConfigured()) {
            System.err.println(
                "Email service not configured. Email will not be sent."
            );
            System.err.println(
                "Please configure email credentials in application-local.properties"
            );
            return;
        }

        new Thread(() -> {
            try {
                Properties prop = new Properties();
                prop.put("mail.smtp.host", config.getSmtpHost());
                prop.put(
                    "mail.smtp.port",
                    String.valueOf(config.getSmtpPort())
                );
                prop.put(
                    "mail.smtp.auth",
                    String.valueOf(config.isSmtpAuthEnabled())
                );
                prop.put(
                    "mail.smtp.starttls.enable",
                    String.valueOf(config.isStartTlsEnabled())
                );
                prop.put("mail.smtp.ssl.trust", config.getSmtpSslTrust());

                Session session = Session.getInstance(
                    prop,
                    new Authenticator() {
                        @Override
                        protected PasswordAuthentication getPasswordAuthentication() {
                            return new PasswordAuthentication(
                                username,
                                password
                            );
                        }
                    }
                );

                Message message = new MimeMessage(session);
                message.setFrom(new InternetAddress(username));
                message.setRecipients(
                    Message.RecipientType.TO,
                    InternetAddress.parse(toEmail)
                );
                message.setSubject(subject);

                MimeBodyPart mimeBodyPart = new MimeBodyPart();
                mimeBodyPart.setContent(body, "text/html; charset=utf-8");

                Multipart multipart = new MimeMultipart();
                multipart.addBodyPart(mimeBodyPart);

                if (filePath != null && !filePath.isEmpty()) {
                    MimeBodyPart attachmentBodyPart = new MimeBodyPart();
                    attachmentBodyPart.attachFile(new File(filePath));
                    multipart.addBodyPart(attachmentBodyPart);
                }

                message.setContent(multipart);

                Transport.send(message);
                System.out.println("Email envoye avec succes a " + toEmail);
            } catch (Exception e) {
                System.err.println(
                    "Erreur lors de l'envoi de l'email : " + e.getMessage()
                );
                e.printStackTrace();
            } finally {
                if (tempFileToDelete != null) {
                    try {
                        Files.deleteIfExists(tempFileToDelete);
                    } catch (Exception e) {
                        System.err.println(
                            "Could not delete temporary PDF file: " +
                                e.getMessage()
                        );
                    }
                }
            }
        })
            .start();
    }

    public boolean isConfigured() {
        return (
            username != null &&
            !username.equals("votre.email@gmail.com") &&
            password != null &&
            !password.contains("abcd efgh")
        );
    }

    public String getConfiguredEmail() {
        return username;
    }

    public void sendAppointmentConfirmation(
        String patientEmail,
        String patientName,
        String doctorName,
        String appointmentDateTime,
        String motif
    ) {
        if (!isConfigured()) {
            System.err.println(
                "Email service not configured. Appointment confirmation email will not be sent."
            );
            return;
        }

        String subject = "Confirmation de Rendez-vous - MediClinic";

        String body =
            "<html><body style='font-family: Arial, sans-serif;'>" +
            "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0;'>" +
            "<h2 style='color: #2980b9;'>Confirmation de Rendez-vous</h2>" +
            "<p>Bonjour <strong>" +
            patientName +
            "</strong>,</p>" +
            "<p>Votre rendez-vous a été confirmé avec succès.</p>" +
            "<div style='background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
            "<p style='margin: 5px 0;'><strong>Date et Heure:</strong> " +
            appointmentDateTime +
            "</p>" +
            "<p style='margin: 5px 0;'><strong>Médecin:</strong> Dr. " +
            doctorName +
            "</p>" +
            "<p style='margin: 5px 0;'><strong>Motif:</strong> " +
            motif +
            "</p>" +
            "</div>" +
            "<p style='color: #7f8c8d;'><em>Merci de vous présenter 10 minutes avant l'heure du rendez-vous.</em></p>" +
            "<p><strong>Veuillez trouver en pièce jointe votre confirmation de rendez-vous avec le QR code de vérification.</strong></p>" +
            "<p style='color: #e67e22;'><em>Présentez le QR code à la secrétaire le jour de votre rendez-vous pour une vérification rapide.</em></p>" +
            "<p>Si vous devez annuler ou reporter ce rendez-vous, veuillez nous contacter dès que possible.</p>" +
            "<hr style='border: none; border-top: 1px solid #e0e0e0; margin: 20px 0;'>" +
            "<p style='color: #95a5a6; font-size: 12px;'>Cordialement,<br>L'équipe MediClinic</p>" +
            "</div></body></html>";

        sendEmailWithAttachment(patientEmail, subject, body, null);
    }

    public void sendAppointmentConfirmationWithQR(RendezVous rdv) {
        if (!isConfigured()) {
            System.err.println(
                "Email service not configured. Appointment confirmation email will not be sent."
            );
            return;
        }

        if (rdv == null || rdv.getPatient() == null) {
            System.err.println("Invalid RendezVous or Patient data");
            return;
        }

        if (rdv.getId() == null) {
            System.err.println(
                "RendezVous must be saved to database before generating QR code (ID is null)"
            );
            return;
        }

        String patientEmail = rdv.getPatient().getEmail();
        if (patientEmail == null || patientEmail.trim().isEmpty()) {
            System.out.println(
                "Patient email not available. Skipping confirmation email."
            );
            return;
        }

        new Thread(() -> {
            Path tempPdfPath = null;
            try {
                tempPdfPath = Files.createTempFile(
                    "rdv_confirmation_" + rdv.getId() + "_",
                    ".pdf"
                );
                String pdfPath = tempPdfPath.toAbsolutePath().toString();

                pdfService.generateAppointmentConfirmationPdf(rdv, pdfPath);

                String subject = "Confirmation de Rendez-vous - MediClinic";

                String appointmentDateTime = rdv
                    .getDateHeureDebut()
                    .format(DATETIME_FORMATTER);
                String motif = rdv.getMotif() != null &&
                    !rdv.getMotif().isEmpty()
                    ? rdv.getMotif()
                    : "Consultation";

                String body =
                    "<html><body style='font-family: Arial, sans-serif;'>" +
                    "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0;'>" +
                    "<h2 style='color: #2980b9;'>Confirmation de Rendez-vous</h2>" +
                    "<p>Bonjour <strong>" +
                    rdv.getPatient().getNomComplet() +
                    "</strong>,</p>" +
                    "<p>Votre rendez-vous a été confirmé avec succès.</p>" +
                    "<div style='background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
                    "<p style='margin: 5px 0;'><strong>Date et Heure:</strong> " +
                    appointmentDateTime +
                    "</p>" +
                    "<p style='margin: 5px 0;'><strong>Médecin:</strong> Dr. " +
                    rdv.getMedecin().getNomComplet() +
                    "</p>" +
                    "<p style='margin: 5px 0;'><strong>Motif:</strong> " +
                    motif +
                    "</p>" +
                    "</div>" +
                    "<p style='color: #7f8c8d;'><em>Merci de vous présenter 10 minutes avant l'heure du rendez-vous.</em></p>" +
                    "<div style='background-color: #fff3cd; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #e67e22;'>" +
                    "<p style='margin: 0;'><strong>Important:</strong> Veuillez trouver en pièce jointe votre confirmation de rendez-vous avec le QR code de vérification.</p>" +
                    "<p style='margin: 10px 0 0 0;'><strong>Présentez le QR code à la secrétaire le jour de votre rendez-vous</strong> pour une vérification rapide et sans contact.</p>" +
                    "</div>" +
                    "<p>Si vous devez annuler ou reporter ce rendez-vous, veuillez nous contacter dès que possible.</p>" +
                    "<hr style='border: none; border-top: 1px solid #e0e0e0; margin: 20px 0;'>" +
                    "<p style='color: #95a5a6; font-size: 12px;'>Cordialement,<br>L'équipe MediClinic</p>" +
                    "</div></body></html>";

                sendEmailWithAttachment(
                    patientEmail,
                    subject,
                    body,
                    pdfPath,
                    tempPdfPath
                );

                System.out.println(
                    "Appointment confirmation with QR code sent to " +
                        patientEmail
                );
            } catch (Exception e) {
                System.err.println(
                    "Error generating or sending appointment confirmation PDF: " +
                        e.getMessage()
                );
                e.printStackTrace();
                if (tempPdfPath != null) {
                    try {
                        Files.deleteIfExists(tempPdfPath);
                    } catch (Exception ex) {
                        System.err.println(
                            "Could not delete temporary PDF file: " +
                                ex.getMessage()
                        );
                    }
                }
            }
        })
            .start();
    }

    public void sendAppointmentReminder(
        String patientEmail,
        String patientName,
        String doctorName,
        String appointmentDateTime,
        String motif
    ) {
        if (!isConfigured()) {
            System.err.println(
                "Email service not configured. Appointment reminder email will not be sent."
            );
            return;
        }

        String subject = "Rappel de Rendez-vous - MediClinic";

        String body =
            "<html><body style='font-family: Arial, sans-serif;'>" +
            "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0;'>" +
            "<h2 style='color: #e67e22;'>⏰ Rappel de Rendez-vous</h2>" +
            "<p>Bonjour <strong>" +
            patientName +
            "</strong>,</p>" +
            "<p>Ceci est un rappel pour votre rendez-vous à venir.</p>" +
            "<div style='background-color: #fff3cd; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #e67e22;'>" +
            "<p style='margin: 5px 0;'><strong>Date et Heure:</strong> " +
            appointmentDateTime +
            "</p>" +
            "<p style='margin: 5px 0;'><strong>Médecin:</strong> Dr. " +
            doctorName +
            "</p>" +
            "<p style='margin: 5px 0;'><strong>Motif:</strong> " +
            motif +
            "</p>" +
            "</div>" +
            "<p style='color: #7f8c8d;'><em>N'oubliez pas d'apporter vos documents médicaux si nécessaire.</em></p>" +
            "<p>Nous vous attendons avec plaisir. En cas d'empêchement, merci de nous prévenir au plus vite.</p>" +
            "<hr style='border: none; border-top: 1px solid #e0e0e0; margin: 20px 0;'>" +
            "<p style='color: #95a5a6; font-size: 12px;'>Cordialement,<br>L'équipe MediClinic</p>" +
            "</div></body></html>";

        sendEmailWithAttachment(patientEmail, subject, body, null);
    }

    public void sendAppointmentReminderWithQR(RendezVous rdv) {
        if (!isConfigured()) {
            System.err.println(
                "Email service not configured. Appointment reminder email will not be sent."
            );
            return;
        }

        if (rdv == null || rdv.getPatient() == null) {
            System.err.println("Invalid RendezVous or Patient data");
            return;
        }

        if (rdv.getId() == null) {
            System.err.println(
                "RendezVous must be saved to database before generating QR code (ID is null)"
            );
            return;
        }

        String patientEmail = rdv.getPatient().getEmail();
        if (patientEmail == null || patientEmail.trim().isEmpty()) {
            System.out.println(
                "Patient email not available. Skipping reminder email."
            );
            return;
        }

        new Thread(() -> {
            Path tempPdfPath = null;
            try {
                tempPdfPath = Files.createTempFile(
                    "rdv_reminder_" + rdv.getId() + "_",
                    ".pdf"
                );
                String pdfPath = tempPdfPath.toAbsolutePath().toString();

                pdfService.generateAppointmentConfirmationPdf(rdv, pdfPath);

                String subject = "Rappel de Rendez-vous - MediClinic";

                String appointmentDateTime = rdv
                    .getDateHeureDebut()
                    .format(DATETIME_FORMATTER);
                String motif = rdv.getMotif() != null &&
                    !rdv.getMotif().isEmpty()
                    ? rdv.getMotif()
                    : "Consultation";

                String body =
                    "<html><body style='font-family: Arial, sans-serif;'>" +
                    "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0;'>" +
                    "<h2 style='color: #e67e22;'>Rappel de Rendez-vous</h2>" +
                    "<p>Bonjour <strong>" +
                    rdv.getPatient().getNomComplet() +
                    "</strong>,</p>" +
                    "<p>Ceci est un rappel pour votre rendez-vous à venir.</p>" +
                    "<div style='background-color: #fff3cd; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #e67e22;'>" +
                    "<p style='margin: 5px 0;'><strong>Date et Heure:</strong> " +
                    appointmentDateTime +
                    "</p>" +
                    "<p style='margin: 5px 0;'><strong>Médecin:</strong> Dr. " +
                    rdv.getMedecin().getNomComplet() +
                    "</p>" +
                    "<p style='margin: 5px 0;'><strong>Motif:</strong> " +
                    motif +
                    "</p>" +
                    "</div>" +
                    "<p style='color: #7f8c8d;'><em>N'oubliez pas d'apporter vos documents médicaux si nécessaire.</em></p>" +
                    "<div style='background-color: #fff3cd; padding: 15px; border-radius: 5px; margin: 20px 0; border-left: 4px solid #e67e22;'>" +
                    "<p style='margin: 0;'><strong>Important:</strong> Veuillez trouver en pièce jointe votre confirmation avec le QR code de vérification.</p>" +
                    "<p style='margin: 10px 0 0 0;'><strong>Présentez le QR code à la secrétaire</strong> pour une vérification rapide et sans contact.</p>" +
                    "</div>" +
                    "<p>Nous vous attendons avec plaisir. En cas d'empêchement, merci de nous prévenir au plus vite.</p>" +
                    "<hr style='border: none; border-top: 1px solid #e0e0e0; margin: 20px 0;'>" +
                    "<p style='color: #95a5a6; font-size: 12px;'>Cordialement,<br>L'équipe MediClinic</p>" +
                    "</div></body></html>";

                sendEmailWithAttachment(
                    patientEmail,
                    subject,
                    body,
                    pdfPath,
                    tempPdfPath
                );

                System.out.println(
                    "Appointment reminder with QR code sent to " + patientEmail
                );
            } catch (Exception e) {
                System.err.println(
                    "Error generating or sending appointment reminder PDF: " +
                        e.getMessage()
                );
                e.printStackTrace();
                if (tempPdfPath != null) {
                    try {
                        Files.deleteIfExists(tempPdfPath);
                    } catch (Exception ex) {
                        System.err.println(
                            "Could not delete temporary PDF file: " +
                                ex.getMessage()
                        );
                    }
                }
            }
        })
            .start();
    }

    public void sendWelcomeEmail(
        String patientEmail,
        String patientName,
        String patientId
    ) {
        if (!isConfigured()) {
            System.err.println(
                "Email service not configured. Welcome email will not be sent."
            );
            return;
        }

        String subject = "Bienvenue à MediClinic !";

        String body =
            "<html><body style='font-family: Arial, sans-serif;'>" +
            "<div style='max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #e0e0e0;'>" +
            "<div style='background-color: #2980b9; padding: 20px; text-align: center; border-radius: 5px 5px 0 0;'>" +
            "<h1 style='color: white; margin: 0;'>Bienvenue à MediClinic !</h1>" +
            "</div>" +
            "<div style='padding: 20px;'>" +
            "<p>Bonjour <strong>" +
            patientName +
            "</strong>,</p>" +
            "<p>Nous sommes ravis de vous accueillir parmi nos patients !</p>" +
            "<p>Votre dossier patient a été créé avec succès.</p>" +
            "<div style='background-color: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0;'>" +
            "<p style='margin: 5px 0;'><strong>Numéro de dossier:</strong> " +
            patientId +
            "</p>" +
            "</div>" +
            "<h3 style='color: #2980b9;'>Nos Services</h3>" +
            "<ul style='line-height: 1.8;'>" +
            "<li>Consultations médicales générales</li>" +
            "<li>Suivi médical personnalisé</li>" +
            "<li>Gestion de vos rendez-vous en ligne</li>" +
            "<li>Accès à votre dossier médical</li>" +
            "</ul>" +
            "<h3 style='color: #2980b9;'>Informations Pratiques</h3>" +
            "<p>Pour prendre rendez-vous ou pour toute question, n'hésitez pas à nous contacter.</p>" +
            "<p style='background-color: #d4edda; padding: 10px; border-radius: 5px; border-left: 4px solid #28a745;'>" +
            "<strong>📞 Conseil:</strong> Pensez à avoir votre numéro de dossier lors de vos contacts avec la clinique." +
            "</p>" +
            "<hr style='border: none; border-top: 1px solid #e0e0e0; margin: 20px 0;'>" +
            "<p style='color: #95a5a6; font-size: 12px;'>Cordialement,<br>L'équipe MediClinic<br>" +
            "<em>Votre santé, notre priorité</em></p>" +
            "</div></div></body></html>";

        sendEmailWithAttachment(patientEmail, subject, body, null);
    }
}
