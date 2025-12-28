package com.mediclinic.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "dossier_medical")
public class DossierMedical implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // --- Relation One-to-One ---

    // Un Dossier est lié à un seul Patient.
    // @JoinColumn indique que cette table (dossier_medical) contient la clé étrangère (patient_id).
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", unique = true, nullable = false)
    private Patient patient;

    // --- Champs du Dossier ---

    @Column(name = "date_creation", nullable = false)
    private LocalDate dateCreation = LocalDate.now(); // Initialisé à la date de création

    @Lob // Annotation pour les grands objets de type texte (CLOB)
    @Column(name = "notes_generales", columnDefinition = "TEXT")
    private String notesGenerales;

    // --- Relation One-to-Many (Historique des consultations) ---

    // Un Dossier contient plusieurs Consultations
    // MappedBy pointe vers le champ 'dossierMedical' dans l'entité Consultation
    @OneToMany(mappedBy = "dossierMedical", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<Consultation> historiqueConsultations = new ArrayList<>();


    // --- Constructeurs ---

    public DossierMedical() {
        this.dateCreation = LocalDate.now();
        this.historiqueConsultations = new ArrayList<>();
    }

    public DossierMedical(Patient patient, String notesGenerales) {
        this.patient = patient;
        this.notesGenerales = notesGenerales;
        this.dateCreation = LocalDate.now();
        this.historiqueConsultations = new ArrayList<>();
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

    public LocalDate getDateCreation() {
        return dateCreation;
    }

    public void setDateCreation(LocalDate dateCreation) {
        this.dateCreation = dateCreation;
    }

    public String getNotesGenerales() {
        return notesGenerales;
    }

    public void setNotesGenerales(String notesGenerales) {
        this.notesGenerales = notesGenerales;
    }

    public List<Consultation> getHistoriqueConsultations() {
        return historiqueConsultations;
    }

    public void setHistoriqueConsultations(List<Consultation> historiqueConsultations) {
        this.historiqueConsultations = historiqueConsultations;
    }

    // --- Méthode Utile ---

    public void addConsultation(Consultation consultation) {
        this.historiqueConsultations.add(consultation);
        consultation.setDossierMedical(this);
    }
}