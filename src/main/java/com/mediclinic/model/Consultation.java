package com.mediclinic.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;

@Entity
@Table(name = "consultation")
public class Consultation implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // --- Relations ---

    // 1. Lien avec le Dossier Médical (Many-to-One)
    // Plusieurs consultations appartiennent à un Dossier
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "dossier_id", nullable = false)
    private DossierMedical dossierMedical;

    // 2. Lien avec le Rendez-vous (One-to-One)
    // Une Consultation est le résultat d'un unique RendezVous.
    // La colonne rendez_vous_id est à la fois clé étrangère et clé primaire potentielle,
    // garantissant qu'un RDV ne peut avoir qu'une seule Consultation.
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rendez_vous_id", unique = true, nullable = false)
    private RendezVous rendezVous;

    // --- Champs de l'Acte Médical ---

    @Column(name = "date_consultation", nullable = false)
    private LocalDateTime dateConsultation = LocalDateTime.now();

    @Lob
    @Column(name = "observations_examen", columnDefinition = "TEXT")
    private String observations;

    @Lob
    @Column(name = "diagnostic", columnDefinition = "TEXT")
    private String diagnostic;

    @Lob
    @Column(name = "prescriptions", columnDefinition = "TEXT")
    private String prescriptions;


    // --- Constructeurs ---

    public Consultation() {
        this.dateConsultation = LocalDateTime.now();
    }

    public Consultation(DossierMedical dossierMedical, RendezVous rendezVous, String observations, String diagnostic, String prescriptions) {
        this.dossierMedical = dossierMedical;
        this.rendezVous = rendezVous;
        this.observations = observations;
        this.diagnostic = diagnostic;
        this.prescriptions = prescriptions;
        this.dateConsultation = LocalDateTime.now();
    }

    // --- Getters et Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public DossierMedical getDossierMedical() {
        return dossierMedical;
    }

    public void setDossierMedical(DossierMedical dossierMedical) {
        this.dossierMedical = dossierMedical;
    }

    public RendezVous getRendezVous() {
        return rendezVous;
    }

    public void setRendezVous(RendezVous rendezVous) {
        this.rendezVous = rendezVous;
    }

    public LocalDateTime getDateConsultation() {
        return dateConsultation;
    }

    public void setDateConsultation(LocalDateTime dateConsultation) {
        this.dateConsultation = dateConsultation;
    }

    public String getObservations() {
        return observations;
    }

    public void setObservations(String observations) {
        this.observations = observations;
    }

    public String getDiagnostic() {
        return diagnostic;
    }

    public void setDiagnostic(String diagnostic) {
        this.diagnostic = diagnostic;
    }

    public String getPrescriptions() {
        return prescriptions;
    }

    public void setPrescriptions(String prescriptions) {
        this.prescriptions = prescriptions;
    }
}