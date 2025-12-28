package com.mediclinic.service;

import com.mediclinic.model.*;
import java.io.FileWriter;
import java.io.IOException;
import java.io.PrintWriter;
import java.math.BigDecimal;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import java.util.List;

public class CsvService {

    private static final DateTimeFormatter DATE_FORMATTER =
        DateTimeFormatter.ofPattern("dd/MM/yyyy");
    private static final DateTimeFormatter DATETIME_FORMATTER =
        DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

    public void exportPatients(List<Patient> patients, String destPath)
        throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(destPath))) {
            writer.println(
                "ID,Nom,Prenom,Date Naissance,Telephone,Email,Adresse"
            );

            for (Patient patient : patients) {
                writer.println(
                    String.join(
                        ",",
                        escapeCsv(String.valueOf(patient.getId())),
                        escapeCsv(patient.getNom()),
                        escapeCsv(patient.getPrenom()),
                        escapeCsv(
                            patient.getDateNaissance() != null
                                ? patient
                                      .getDateNaissance()
                                      .format(DATE_FORMATTER)
                                : ""
                        ),
                        escapeCsv(patient.getTelephone()),
                        escapeCsv(
                            patient.getEmail() != null ? patient.getEmail() : ""
                        ),
                        escapeCsv(
                            patient.getAdresse() != null
                                ? patient.getAdresse()
                                : ""
                        )
                    )
                );
            }
        }
    }

    public void exportDoctors(List<Medecin> doctors, String destPath)
        throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(destPath))) {
            writer.println("ID,Nom,Prenom,Specialite,Email,Telephone");

            for (Medecin doctor : doctors) {
                writer.println(
                    String.join(
                        ",",
                        escapeCsv(String.valueOf(doctor.getId())),
                        escapeCsv(doctor.getNom()),
                        escapeCsv(doctor.getPrenom()),
                        escapeCsv(
                            doctor.getSpecialite() != null
                                ? doctor.getSpecialite().name()
                                : ""
                        ),
                        escapeCsv(
                            doctor.getEmail() != null ? doctor.getEmail() : ""
                        ),
                        escapeCsv(
                            doctor.getTelephone() != null
                                ? doctor.getTelephone()
                                : ""
                        )
                    )
                );
            }
        }
    }

    public void exportAppointments(
        List<RendezVous> appointments,
        String destPath
    ) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(destPath))) {
            writer.println(
                "ID,Date Heure Debut,Date Heure Fin,Patient,Medecin,Motif,Statut,Duree (min)"
            );

            for (RendezVous rdv : appointments) {
                long dureeMinutes = 0;
                if (rdv.getDuree() != null) {
                    dureeMinutes = rdv.getDuree().toMinutes();
                }

                writer.println(
                    String.join(
                        ",",
                        escapeCsv(String.valueOf(rdv.getId())),
                        escapeCsv(
                            rdv.getDateHeureDebut() != null
                                ? rdv
                                      .getDateHeureDebut()
                                      .format(DATETIME_FORMATTER)
                                : ""
                        ),
                        escapeCsv(
                            rdv.getDateHeureFin() != null
                                ? rdv
                                      .getDateHeureFin()
                                      .format(DATETIME_FORMATTER)
                                : ""
                        ),
                        escapeCsv(
                            rdv.getPatient() != null
                                ? rdv.getPatient().getNomComplet()
                                : ""
                        ),
                        escapeCsv(
                            rdv.getMedecin() != null
                                ? rdv.getMedecin().getNomComplet()
                                : ""
                        ),
                        escapeCsv(rdv.getMotif() != null ? rdv.getMotif() : ""),
                        escapeCsv(
                            rdv.getStatus() != null
                                ? rdv.getStatus().name()
                                : ""
                        ),
                        escapeCsv(String.valueOf(dureeMinutes))
                    )
                );
            }
        }
    }

    public void exportInvoices(List<Facture> invoices, String destPath)
        throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(destPath))) {
            writer.println("ID,Date,Patient,Montant Total,Type Paiement");

            for (Facture facture : invoices) {
                writer.println(
                    String.join(
                        ",",
                        escapeCsv(String.valueOf(facture.getId())),
                        escapeCsv(
                            facture.getDateFacturation() != null
                                ? facture
                                      .getDateFacturation()
                                      .format(DATE_FORMATTER)
                                : ""
                        ),
                        escapeCsv(
                            facture.getPatient() != null
                                ? facture.getPatient().getNomComplet()
                                : ""
                        ),
                        escapeCsv(String.valueOf(facture.getMontantTotal())),
                        escapeCsv(
                            facture.getTypePaiement() != null
                                ? facture.getTypePaiement().name()
                                : ""
                        )
                    )
                );
            }
        }
    }

    public void exportInvoicesDetailed(List<Facture> invoices, String destPath)
        throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(destPath))) {
            writer.println(
                "Facture ID,Date,Patient,Description,Quantite,Prix Unitaire,Montant Ligne,Montant Total Facture"
            );

            for (Facture facture : invoices) {
                if (
                    facture.getLignes() != null &&
                    !facture.getLignes().isEmpty()
                ) {
                    for (LigneFacture ligne : facture.getLignes()) {
                        BigDecimal montantLigne = ligne
                            .getPrixUnitaire()
                            .multiply(BigDecimal.valueOf(ligne.getQuantite()));

                        writer.println(
                            String.join(
                                ",",
                                escapeCsv(String.valueOf(facture.getId())),
                                escapeCsv(
                                    facture.getDateFacturation() != null
                                        ? facture
                                              .getDateFacturation()
                                              .format(DATE_FORMATTER)
                                        : ""
                                ),
                                escapeCsv(
                                    facture.getPatient() != null
                                        ? facture.getPatient().getNomComplet()
                                        : ""
                                ),
                                escapeCsv(
                                    ligne.getDescription() != null
                                        ? ligne.getDescription()
                                        : ""
                                ),
                                escapeCsv(String.valueOf(ligne.getQuantite())),
                                escapeCsv(
                                    String.valueOf(ligne.getPrixUnitaire())
                                ),
                                escapeCsv(String.valueOf(montantLigne)),
                                escapeCsv(
                                    String.valueOf(facture.getMontantTotal())
                                )
                            )
                        );
                    }
                } else {
                    writer.println(
                        String.join(
                            ",",
                            escapeCsv(String.valueOf(facture.getId())),
                            escapeCsv(
                                facture.getDateFacturation() != null
                                    ? facture
                                          .getDateFacturation()
                                          .format(DATE_FORMATTER)
                                    : ""
                            ),
                            escapeCsv(
                                facture.getPatient() != null
                                    ? facture.getPatient().getNomComplet()
                                    : ""
                            ),
                            "",
                            "0",
                            "0",
                            "0",
                            escapeCsv(String.valueOf(facture.getMontantTotal()))
                        )
                    );
                }
            }
        }
    }

    public void exportConsultations(
        List<Consultation> consultations,
        String destPath
    ) throws IOException {
        try (PrintWriter writer = new PrintWriter(new FileWriter(destPath))) {
            writer.println("ID,Date,Patient,Medecin,Diagnostic,Observations");

            for (Consultation consultation : consultations) {
                String patientName = "N/A";
                String medecinName = "N/A";

                if (consultation.getRendezVous() != null) {
                    if (consultation.getRendezVous().getPatient() != null) {
                        patientName = consultation
                            .getRendezVous()
                            .getPatient()
                            .getNomComplet();
                    }
                    if (consultation.getRendezVous().getMedecin() != null) {
                        medecinName = consultation
                            .getRendezVous()
                            .getMedecin()
                            .getNomComplet();
                    }
                }

                writer.println(
                    String.join(
                        ",",
                        escapeCsv(String.valueOf(consultation.getId())),
                        escapeCsv(
                            consultation.getDateConsultation() != null
                                ? consultation
                                      .getDateConsultation()
                                      .format(DATETIME_FORMATTER)
                                : ""
                        ),
                        escapeCsv(patientName),
                        escapeCsv(medecinName),
                        escapeCsv(
                            consultation.getDiagnostic() != null
                                ? consultation.getDiagnostic()
                                : ""
                        ),
                        escapeCsv(
                            consultation.getObservations() != null
                                ? consultation.getObservations()
                                : ""
                        )
                    )
                );
            }
        }
    }

    private String escapeCsv(String value) {
        if (value == null) {
            return "";
        }

        if (
            value.contains(",") ||
            value.contains("\"") ||
            value.contains("\n") ||
            value.contains("\r")
        ) {
            value = value.replace("\"", "\"\"");
            return "\"" + value + "\"";
        }

        return value;
    }
}
