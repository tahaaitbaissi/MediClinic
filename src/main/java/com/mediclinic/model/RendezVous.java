package com.mediclinic.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDateTime;
import java.time.Duration;

@Entity
@Table(name = "rendez_vous")
public class RendezVous implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // --- Relations Many-to-One ---

    // Chaque RDV est lié à un seul Patient (clé étrangère patient_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    // Chaque RDV est lié à un seul Médecin (clé étrangère medecin_id)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "medecin_id", nullable = false)
    private Medecin medecin;

    // --- Champs de temps et Logique ---

    @Column(name = "date_heure_debut", nullable = false)
    private LocalDateTime dateHeureDebut;

    @Column(name = "date_heure_fin", nullable = false)
    private LocalDateTime dateHeureFin;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false)
    private RendezVousStatus status = RendezVousStatus.PLANIFIE; // Valeur par défaut

    @Column(name = "motif")
    private String motif;

    // --- Relations One-to-One (Optionnel) ---

    // Un RDV peut résulter en une Consultation (mais pas l'inverse)
    @OneToOne(mappedBy = "rendezVous", cascade = CascadeType.ALL, fetch = FetchType.LAZY)
    private Consultation consultation;


    // --- Constructeurs ---

    public RendezVous() {
    }

    public RendezVous(Patient patient, Medecin medecin, LocalDateTime dateHeureDebut, LocalDateTime dateHeureFin, String motif) {
        this.patient = patient;
        this.medecin = medecin;
        this.dateHeureDebut = dateHeureDebut;
        this.dateHeureFin = dateHeureFin;
        this.motif = motif;
        this.status = RendezVousStatus.PLANIFIE;
    }

    // --- Getters et Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Patient getPatient() {
        return patient;
    }

    public void setPatient(Patient patient) {
        this.patient = patient;
    }

    public Medecin getMedecin() {
        return medecin;
    }

    public void setMedecin(Medecin medecin) {
        this.medecin = medecin;
    }

    public LocalDateTime getDateHeureDebut() {
        return dateHeureDebut;
    }

    public void setDateHeureDebut(LocalDateTime dateHeureDebut) {
        this.dateHeureDebut = dateHeureDebut;
    }

    public LocalDateTime getDateHeureFin() {
        return dateHeureFin;
    }

    public void setDateHeureFin(LocalDateTime dateHeureFin) {
        this.dateHeureFin = dateHeureFin;
    }

    public RendezVousStatus getStatus() {
        return status;
    }

    public void setStatus(RendezVousStatus status) {
        this.status = status;
    }

    public String getMotif() {
        return motif;
    }

    public void setMotif(String motif) {
        this.motif = motif;
    }

    public Consultation getConsultation() {
        return consultation;
    }

    public void setConsultation(Consultation consultation) {
        this.consultation = consultation;
    }


    // --- Logique Métier (Essentiel pour le Service Layer) ---

    /**
     * Calcule la durée du rendez-vous.
     */
    public Duration getDuree() {
        if (dateHeureDebut != null && dateHeureFin != null) {
            return Duration.between(dateHeureDebut, dateHeureFin);
        }
        return Duration.ZERO;
    }

    /**
     * Vérifie si ce RDV entre en conflit avec un autre.
     * C'est la méthode logique derrière la règle d'affaires principale.
     */
    public boolean isOverlap(RendezVous other) {
        if (!this.medecin.getId().equals(other.medecin.getId())) {
            // Pas de chevauchement si les médecins sont différents (règle du sujet)
            return false;
        }

        // L'algorithme standard de vérification de chevauchement d'intervalles
        boolean startsDuring = this.dateHeureDebut.isBefore(other.dateHeureFin) && this.dateHeureDebut.isAfter(other.dateHeureDebut);
        boolean endsDuring = this.dateHeureFin.isAfter(other.dateHeureDebut) && this.dateHeureFin.isBefore(other.dateHeureFin);
        boolean containsOther = this.dateHeureDebut.isEqual(other.dateHeureDebut) && this.dateHeureFin.isEqual(other.dateHeureFin);

        return startsDuring || endsDuring || containsOther;
    }
}