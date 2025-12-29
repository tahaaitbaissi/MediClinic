package com.mediclinic.model;

import jakarta.persistence.*; // Les annotations JPA
import java.io.Serializable; // Bonne pratique

@Entity
@Table(name = "medecin") // Nom de la table dans la base de données
public class Medecin implements Serializable {

    @Id // Clé primaire
    @GeneratedValue(strategy = GenerationType.IDENTITY) // Auto-incrémenté par la base
    @Column(name = "id")
    private Long id;

    @Column(name = "nom", nullable = false)
    private String nom;

    @Column(name = "prenom")
    private String prenom;

    @Enumerated(EnumType.STRING) // Stocke l'Enum comme une chaîne de caractères
    @Column(name = "specialite", nullable = false)
    private SpecialiteMedecin specialite;

    @Column(name = "email", unique = true)
    private String email;

    @Column(name = "telephone")
    private String telephone;

    @Transient // Not stored in database, calculated on-the-fly
    private boolean hasSignature;

    // --- Constructeurs ---

    public Medecin() {}

    public Medecin(
        String nom,
        String prenom,
        SpecialiteMedecin specialite,
        String email,
        String telephone
    ) {
        this.nom = nom;
        this.prenom = prenom;
        this.specialite = specialite;
        this.email = email;
        this.telephone = telephone;
    }

    // --- Getters et Setters ---

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

    public String getNom() {
        return nom;
    }

    public void setNom(String nom) {
        this.nom = nom;
    }

    public String getPrenom() {
        return prenom;
    }

    public void setPrenom(String prenom) {
        this.prenom = prenom;
    }

    public SpecialiteMedecin getSpecialite() {
        return specialite;
    }

    public void setSpecialite(SpecialiteMedecin specialite) {
        this.specialite = specialite;
    }

    public String getEmail() {
        return email;
    }

    public void setEmail(String email) {
        this.email = email;
    }

    public String getTelephone() {
        return telephone;
    }

    public void setTelephone(String telephone) {
        this.telephone = telephone;
    }

    // --- Méthode Utile ---

    public String getNomComplet() {
        return this.prenom + " " + this.nom.toUpperCase();
    }

    public boolean isHasSignature() {
        return hasSignature;
    }

    public void setHasSignature(boolean hasSignature) {
        this.hasSignature = hasSignature;
    }
}
