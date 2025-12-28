package com.mediclinic.service;

import com.mediclinic.dao.FactureDAO;
import com.mediclinic.dao.LigneFactureDAO;
import com.mediclinic.dao.PatientDAO;
import com.mediclinic.model.Facture;
import com.mediclinic.model.LigneFacture;
import com.mediclinic.model.Patient;
import com.mediclinic.model.Role;
import com.mediclinic.model.TypePaiement;
import com.mediclinic.util.UserSession;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.List;
import java.io.FileNotFoundException;

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
    public Facture creerFacture(Long patientId, List<LigneFacture> lignes) throws IllegalArgumentException, SecurityException {
        // Check authentication and permission
        if (!UserSession.isAuthenticated()) {
            throw new SecurityException("Utilisateur non authentifié.");
        }
        
        Role role = UserSession.getInstance().getUser().getRole();
        
        // Only SEC and ADMIN can create invoices
        if (role != Role.SEC && role != Role.ADMIN) {
            throw new SecurityException("Vous n'avez pas la permission de créer une facture.");
        }

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

        // 5. Liaison des Lignes à la Facture
        for (LigneFacture ligne : lignes) {
            ligne.setFacture(facture);
            facture.getLignes().add(ligne);
        }

        // 6. Calcul du montant total
        facture.calculerMontantTotal();

        // 7. Sauvegarde (Hibernate gère les Lignes via Cascade.ALL)
        return factureDAO.save(facture);
    }

    /**
     * Marque une facture comme payée et enregistre le type de paiement.
     * Utilise l'ID pour charger et sauvegarder correctement l'entité.
     */
    public Facture marquerCommePayee(Long factureId, TypePaiement typePaiement) throws SecurityException {
        // Check authentication and permission
        if (!UserSession.isAuthenticated()) {
            throw new SecurityException("Utilisateur non authentifié.");
        }
        
        Role role = UserSession.getInstance().getUser().getRole();
        
        // Only SEC and ADMIN can mark invoices as paid
        if (role != Role.SEC && role != Role.ADMIN) {
            throw new SecurityException("Vous n'avez pas la permission de marquer une facture comme payée.");
        }
        
        if (factureId == null) {
            throw new IllegalArgumentException("L'ID de la facture est requis.");
        }
        
        Facture facture = factureDAO.findById(factureId);

        if (facture == null) {
            throw new IllegalArgumentException("Facture introuvable avec l'ID: " + factureId);
        }

        if (facture.isEstPayee()) {
            throw new IllegalStateException("La facture est déjà marquée comme payée.");
        }

        facture.setEstPayee(true);
        facture.setTypePaiement(typePaiement);

        return factureDAO.save(facture);
    }
    
    /**
     * Check if current user can create invoices
     */
    public boolean canCreateInvoice() {
        if (!UserSession.isAuthenticated()) {
            return false;
        }
        Role role = UserSession.getInstance().getUser().getRole();
        return role == Role.SEC || role == Role.ADMIN;
    }

    // --- Méthodes de Recherche ---

    public Facture findById(Long id) {
        return factureDAO.findById(id);
    }

    public List<Facture> getFacturesByPatient(Patient patient) {
        return factureDAO.findByPatient(patient);
    }

    public List<Facture> getUnpaidFactures() {
        // Utiliser la méthode avec eager fetch pour l'UI
        return factureDAO.findUnpaidWithDetails();
    }

    public List<Facture> getAllFactures() {
        return factureDAO.findAllWithDetails();
    }

    public void sendFactureByEmail(Long factureId) {
        // 1. Get Data
        Facture facture = factureDAO.findById(factureId);

        // 2. Generate PDF
        PdfService pdfService = new PdfService();
        String filePath = "facture_" + factureId + ".pdf";
        try {
            pdfService.generateFacturePdf(facture, filePath);

            // 3. Send Email
            EmailService emailService = new EmailService();
            emailService.sendEmailWithAttachment(
                    facture.getPatient().getEmail(),
                    "Votre Facture MediClinic",
                    "Bonjour, veuillez trouver ci-joint votre facture.",
                    filePath
            );
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        }
    }
}