package com.mediclinic.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal; // IMPORTANT : Toujours utiliser BigDecimal pour la monnaie
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "facture")
public class Facture implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // --- Relations Many-to-One ---

    // Une Facture est liée à un seul Patient (obligatoire selon la règle du sujet)
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "patient_id", nullable = false)
    private Patient patient;

    // --- Relation One-to-One (Optionnel, mais utile) ---

    // Une Facture peut être directement liée à un seul RendezVous (si elle n'en facture qu'un)
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "rendez_vous_id", unique = true)
    private RendezVous rendezVous;

    // --- Champs de la Facture ---

    @Column(name = "date_facturation", nullable = false)
    private LocalDate dateFacturation = LocalDate.now();

    @Column(name = "montant_total", precision = 10, scale = 2) // Precision: 10 chiffres au total, 2 après la virgule
    private BigDecimal montantTotal = BigDecimal.ZERO;

    @Column(name = "est_payee")
    private boolean estPayee = false;

    @Enumerated(EnumType.STRING)
    @Column(name = "type_paiement")
    private TypePaiement typePaiement;

    // --- Relation One-to-Many (Détails) ---

    // Une Facture contient plusieurs LignesFacture.
    // Cascade.ALL permet de supprimer les lignes quand la facture est supprimée.
    @OneToMany(mappedBy = "facture", cascade = CascadeType.ALL, fetch = FetchType.LAZY, orphanRemoval = true)
    private List<LigneFacture> lignes = new ArrayList<>();


    // --- Constructeurs ---

    public Facture() {
        this.dateFacturation = LocalDate.now();
        this.montantTotal = BigDecimal.ZERO;
        this.estPayee = false;
        this.lignes = new ArrayList<>();
    }

    public Facture(Patient patient, RendezVous rendezVous) {
        this.patient = patient;
        this.rendezVous = rendezVous;
        this.dateFacturation = LocalDate.now();
        this.montantTotal = BigDecimal.ZERO;
        this.estPayee = false;
        this.lignes = new ArrayList<>();
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

    public RendezVous getRendezVous() {
        return rendezVous;
    }

    public void setRendezVous(RendezVous rendezVous) {
        this.rendezVous = rendezVous;
    }

    public LocalDate getDateFacturation() {
        return dateFacturation;
    }

    public void setDateFacturation(LocalDate dateFacturation) {
        this.dateFacturation = dateFacturation;
    }

    public BigDecimal getMontantTotal() {
        return montantTotal;
    }

    public void setMontantTotal(BigDecimal montantTotal) {
        this.montantTotal = montantTotal;
    }

    public boolean isEstPayee() {
        return estPayee;
    }

    public void setEstPayee(boolean estPayee) {
        this.estPayee = estPayee;
    }

    public TypePaiement getTypePaiement() {
        return typePaiement;
    }

    public void setTypePaiement(TypePaiement typePaiement) {
        this.typePaiement = typePaiement;
    }

    public List<LigneFacture> getLignes() {
        return lignes;
    }

    public void setLignes(List<LigneFacture> lignes) {
        this.lignes = lignes;
    }

    public void addLigne(LigneFacture ligne) {
        this.lignes.add(ligne);
        ligne.setFacture(this);
        calculerMontantTotal();
    }

    public void removeLigne(LigneFacture ligne) {
        this.lignes.remove(ligne);
        ligne.setFacture(null);
        calculerMontantTotal();
    }

    // --- Méthode Utile (Logique d'affaires) ---

    public void calculerMontantTotal() {
        // Logique pour sommer le montant de toutes les LignesFacture
        this.montantTotal = this.lignes.stream()
                .map(LigneFacture::getMontantLigne)
                .reduce(BigDecimal.ZERO, BigDecimal::add);
    }
}