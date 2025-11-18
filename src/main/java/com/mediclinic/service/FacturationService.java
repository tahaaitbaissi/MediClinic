package com.mediclinic.service;

import com.mediclinic.dao.FactureDAO;
import com.mediclinic.dao.LigneFactureDAO;
import com.mediclinic.dao.PatientDAO;
import com.mediclinic.model.Facture;
import com.mediclinic.model.LigneFacture;
import com.mediclinic.model.Patient;
import com.mediclinic.model.TypePaiement;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;

public class FacturationService {

    private final FactureDAO factureDAO;
    private final LigneFactureDAO ligneFactureDAO;
    private final PatientDAO patientDAO;

    // CONSTRUCTEUR : Initialisation des DAOs
    public FacturationService() {
        this.factureDAO = new FactureDAO();
        this.ligneFactureDAO = new LigneFactureDAO();
        this.patientDAO = new PatientDAO();
    }

    // --- Méthodes Critiques (Création et Transaction) ---

    /**
     * Crée une Facture complète avec ses lignes.
     * Applique la règle de gestion du lien avec le patient.
     * @param patientId ID du patient à facturer.
     * @param lignes Lignes de facturation (services, actes).
     */
    public Facture creerFacture(Long patientId, List<LigneFacture> lignes) throws IllegalArgumentException {

        // 1. Validation des lignes
        if (lignes == null || lignes.isEmpty()) {
            throw new IllegalArgumentException("Une facture doit contenir au moins une ligne.");
        }

        // 2. Validation des montants (pas de valeurs négatives)
        for (LigneFacture ligne : lignes) {
            if (ligne.getPrixUnitaire() != null && ligne.getPrixUnitaire().compareTo(BigDecimal.ZERO) < 0) {
                throw new IllegalArgumentException("Le prix unitaire ne peut pas être négatif.");
            }
            if (ligne.getQuantite() < 0) {
                throw new IllegalArgumentException("La quantité ne peut pas être négative.");
            }
        }

        // 3. Règle de Gestion : Validation du Patient
        Patient patient = patientDAO.findById(patientId);
        if (patient == null) {
            throw new IllegalArgumentException("Règle non respectée : Le patient avec l'ID " + patientId + " n'existe pas.");
        }

        // 4. Création de l'En-tête de Facture
        Facture facture = new Facture();
        facture.setPatient(patient);
        facture.setDateFacturation(LocalDate.now());

        // 5. Liaison et Calcul des Lignes en utilisant les méthodes du modèle
        for (LigneFacture ligne : lignes) {
            facture.addLigne(ligne); // Utilise la méthode du modèle qui gère la relation bidirectionnelle
        }

        // 6. Calcul du montant total en utilisant la méthode du modèle
        facture.calculerMontantTotal();

        // 7. Sauvegarde (Hibernate gère les Lignes via Cascade.ALL)
        factureDAO.save(facture);
        return facture;
    }

    /**
     * Marque une facture comme payée et enregistre le type de paiement.
     */
    public Facture marquerCommePayee(Long factureId, TypePaiement typePaiement) {
        Facture facture = factureDAO.findById(factureId);

        if (facture == null) {
            throw new IllegalArgumentException("Facture introuvable.");
        }

        if (facture.isEstPayee()) {
            throw new IllegalStateException("La facture est déjà marquée comme payée.");
        }

        facture.setEstPayee(true);
        facture.setTypePaiement(typePaiement);

        factureDAO.save(facture);
        return facture;
    }

    // --- Méthodes de Recherche ---

    public Facture findById(Long id) {
        return factureDAO.findById(id);
    }

    public List<Facture> getFacturesByPatient(Patient patient) {
        return factureDAO.findByPatient(patient);
    }

    public List<Facture> getUnpaidFactures() {
        return factureDAO.findUnpaid();
    }
}