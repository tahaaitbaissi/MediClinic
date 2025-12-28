package com.mediclinic.service;

import com.mediclinic.model.Patient;
import com.mediclinic.model.RendezVous;
import com.mediclinic.model.RendezVousStatus;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.concurrent.Executors;
import java.util.concurrent.ScheduledExecutorService;
import java.util.concurrent.TimeUnit;

public class AppointmentReminderService {

    private final RendezVousService rendezVousService;
    private final EmailService emailService;
    private ScheduledExecutorService scheduler;
    private boolean isRunning = false;

    public AppointmentReminderService() {
        this.rendezVousService = new RendezVousService();
        this.emailService = new EmailService();
    }

    public void startReminderScheduler() {
        if (isRunning) {
            System.out.println("Reminder scheduler is already running");
            return;
        }

        scheduler = Executors.newScheduledThreadPool(1);

        scheduler.scheduleAtFixedRate(
            this::checkAndSendReminders,
            0,
            1,
            TimeUnit.HOURS
        );

        isRunning = true;
        System.out.println("Appointment reminder scheduler started");
    }

    public void stopReminderScheduler() {
        if (scheduler != null && !scheduler.isShutdown()) {
            scheduler.shutdown();
            try {
                if (!scheduler.awaitTermination(60, TimeUnit.SECONDS)) {
                    scheduler.shutdownNow();
                }
            } catch (InterruptedException e) {
                scheduler.shutdownNow();
                Thread.currentThread().interrupt();
            }
            isRunning = false;
            System.out.println("Appointment reminder scheduler stopped");
        }
    }

    private void checkAndSendReminders() {
        try {
            System.out.println(
                "Checking for appointments needing reminders..."
            );

            LocalDateTime now = LocalDateTime.now();
            LocalDateTime reminderWindow = now.plusHours(24);

            List<RendezVous> allAppointments = rendezVousService.findAll();

            int remindersSent = 0;

            for (RendezVous rdv : allAppointments) {
                if (shouldSendReminder(rdv, now, reminderWindow)) {
                    sendReminder(rdv);
                    remindersSent++;
                }
            }

            if (remindersSent > 0) {
                System.out.println(
                    "Sent " + remindersSent + " appointment reminder(s)"
                );
            } else {
                System.out.println("No appointment reminders to send");
            }
        } catch (Exception e) {
            System.err.println(
                "Error in reminder scheduler: " + e.getMessage()
            );
            e.printStackTrace();
        }
    }

    private boolean shouldSendReminder(
        RendezVous rdv,
        LocalDateTime now,
        LocalDateTime reminderWindow
    ) {
        if (rdv.getDateHeureDebut() == null) {
            return false;
        }

        if (
            rdv.getStatus() != RendezVousStatus.CONFIRME &&
            rdv.getStatus() != RendezVousStatus.PLANIFIE
        ) {
            return false;
        }

        if (rdv.getPatient() == null) {
            return false;
        }

        if (
            rdv.getPatient().getEmail() == null ||
            rdv.getPatient().getEmail().trim().isEmpty()
        ) {
            return false;
        }

        LocalDateTime appointmentTime = rdv.getDateHeureDebut();

        if (appointmentTime.isBefore(now)) {
            return false;
        }

        if (appointmentTime.isAfter(reminderWindow)) {
            return false;
        }

        return true;
    }

    private void sendReminder(RendezVous rdv) {
        try {
            emailService.sendAppointmentReminderWithQR(rdv);

            System.out.println(
                "Reminder with QR code sent to " +
                    rdv.getPatient().getEmail() +
                    " for appointment on " +
                    rdv
                        .getDateHeureDebut()
                        .format(
                            DateTimeFormatter.ofPattern("dd/MM/yyyy à HH:mm")
                        )
            );
        } catch (Exception e) {
            System.err.println(
                "Failed to send reminder for appointment " +
                    rdv.getId() +
                    ": " +
                    e.getMessage()
            );
        }
    }

    public void sendImmediateReminder(Long appointmentId) {
        try {
            RendezVous rdv = rendezVousService.findById(appointmentId);
            if (rdv == null) {
                System.err.println("Appointment not found: " + appointmentId);
                return;
            }

            sendReminder(rdv);
        } catch (Exception e) {
            System.err.println(
                "Failed to send immediate reminder: " + e.getMessage()
            );
        }
    }

    public boolean isRunning() {
        return isRunning;
    }
}
