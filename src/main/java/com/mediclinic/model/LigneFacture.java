package com.mediclinic.model;

import jakarta.persistence.*;
import java.io.Serializable;
import java.math.BigDecimal;

@Entity
@Table(name = "ligne_facture")
public class LigneFacture implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "id")
    private Long id;

    // --- Relation Many-to-One ---

    // Une Ligne est liée à une seule Facture
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "facture_id", nullable = false)
    private Facture facture;

    // --- Champs de la Ligne ---

    @Column(name = "description", nullable = false)
    private String description;

    @Column(name = "prix_unitaire", precision = 10, scale = 2)
    private BigDecimal prixUnitaire;

    @Column(name = "quantite")
    private int quantite;

    @Transient // Ce champ n'est PAS stocké en base, il est calculé
    private BigDecimal montantLigne;

    // --- Constructeurs ---

    public LigneFacture() {
        this.quantite = 1;
        this.prixUnitaire = BigDecimal.ZERO;
    }

    public LigneFacture(Facture facture, String description, BigDecimal prixUnitaire, int quantite) {
        this.facture = facture;
        this.description = description;
        this.prixUnitaire = prixUnitaire;
        this.quantite = quantite;
        calculerMontant();
    }

    // --- Getters et Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public Facture getFacture() {
        return facture;
    }

    public void setFacture(Facture facture) {
        this.facture = facture;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public BigDecimal getPrixUnitaire() {
        return prixUnitaire;
    }

    public void setPrixUnitaire(BigDecimal prixUnitaire) {
        this.prixUnitaire = prixUnitaire;
        calculerMontant();
    }

    public int getQuantite() {
        return quantite;
    }

    public void setQuantite(int quantite) {
        this.quantite = quantite;
        calculerMontant();
    }

    public void setMontantLigne(BigDecimal montantLigne) {
        this.montantLigne = montantLigne;
    }

    // --- Logique Métier ---

    @PostLoad // Appelé après le chargement de l'entité depuis la DB
    @PrePersist // Appelé avant la sauvegarde de l'entité dans la DB
    @PreUpdate // Appelé avant la mise à jour
    public void calculerMontant() {
        if (prixUnitaire != null) {
            this.montantLigne = prixUnitaire.multiply(BigDecimal.valueOf(quantite));
        } else {
            this.montantLigne = BigDecimal.ZERO;
        }
    }

    public BigDecimal getMontantLigne() {
        // S'assurer que le calcul a toujours lieu avant d'être retourné
        calculerMontant();
        return montantLigne;
    }
}