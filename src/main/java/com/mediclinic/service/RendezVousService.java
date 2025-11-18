package com.mediclinic.service;

import com.mediclinic.dao.MedecinDAO;
import com.mediclinic.dao.PatientDAO;
import com.mediclinic.dao.RendezVousDAO;
import com.mediclinic.model.RendezVous;
import com.mediclinic.model.Medecin;
import com.mediclinic.model.Patient;
import com.mediclinic.model.RendezVousStatus;
import java.time.LocalDateTime;
import java.time.Duration;
import java.util.List;

public class RendezVousService {

    private final RendezVousDAO rdvDAO;
    private final MedecinDAO medecinDAO;
    private final PatientDAO patientDAO;
    private final ConsultationService consultationService;

    // Durée minimale d'un rendez-vous (15 minutes)
    private static final Duration MIN_APPOINTMENT_DURATION = Duration.ofMinutes(15);

    // CONSTRUCTEUR : Initialisation des DAOs
    public RendezVousService() {
        this.rdvDAO = new RendezVousDAO();
        this.medecinDAO = new MedecinDAO(); // Nécessaire pour vérifier le médecin
        this.patientDAO = new PatientDAO(); // Nécessaire pour vérifier le patient
        this.consultationService = new ConsultationService(); // Injection de dépendance
    }

    // --- Méthodes Critiques (Logique de Planification) ---

    /**
     * Planifie un nouveau RendezVous ou met à jour un RendezVous existant.
     * Applique la règle de gestion de collision.
     * @param rdv L'objet RendezVous à enregistrer.
     * @throws IllegalStateException Si un conflit d'horaire est détecté.
     * @throws IllegalArgumentException Si le patient ou le médecin n'est pas trouvé.
     */
    public RendezVous planifierRendezVous(RendezVous rdv) throws IllegalStateException, IllegalArgumentException {

        // 1. Validation des dates
        validateAppointmentDates(rdv);

        // 2. Vérification des Entités
        if (rdv.getPatient() == null || rdv.getMedecin() == null) {
            throw new IllegalArgumentException("Le Rendez-vous doit être lié à un Patient et un Médecin valides.");
        }

        // Charger les entités pour s'assurer qu'elles sont attachées à la session si besoin
        Patient patient = patientDAO.findById(rdv.getPatient().getId());
        Medecin medecin = medecinDAO.findById(rdv.getMedecin().getId());

        if (patient == null || medecin == null) {
            throw new IllegalArgumentException("Patient ou Médecin non trouvé dans la base de données.");
        }

        // 3. Règle de Gestion : Prévention des Collisions
        Long collisionCount = rdvDAO.countConflictingAppointments(
                medecin,
                rdv.getDateHeureDebut(),
                rdv.getDateHeureFin(),
                rdv.getId() // Passe l'ID pour exclure le RDV lui-même lors de la modification
        );

        if (collisionCount > 0) {
            throw new IllegalStateException("Collision détectée : Le Médecin " + medecin.getNomComplet() +
                    " a déjà un autre rendez-vous à ce créneau.");
        }

        // 4. Mise à jour des relations et statut par défaut
        rdv.setPatient(patient);
        rdv.setMedecin(medecin);
        if (rdv.getStatus() == null) {
            rdv.setStatus(RendezVousStatus.PLANIFIE);
        }

        // 5. Sauvegarde
        rdvDAO.save(rdv);
        return rdv;
    }

    /**
     * Valide les dates d'un rendez-vous.
     */
    private void validateAppointmentDates(RendezVous rdv) {
        if (rdv.getDateHeureDebut() == null || rdv.getDateHeureFin() == null) {
            throw new IllegalArgumentException("Les dates de début et de fin sont obligatoires.");
        }

        if (rdv.getDateHeureFin().isBefore(rdv.getDateHeureDebut()) || 
            rdv.getDateHeureFin().isEqual(rdv.getDateHeureDebut())) {
            throw new IllegalArgumentException("La date de fin doit être postérieure à la date de début.");
        }

        Duration duration = Duration.between(rdv.getDateHeureDebut(), rdv.getDateHeureFin());
        if (duration.compareTo(MIN_APPOINTMENT_DURATION) < 0) {
            throw new IllegalArgumentException("La durée minimale d'un rendez-vous est de " + 
                    MIN_APPOINTMENT_DURATION.toMinutes() + " minutes.");
        }

        // Pour les nouveaux rendez-vous (sans ID), ne pas permettre les dates passées
        if (rdv.getId() == null && rdv.getDateHeureDebut().isBefore(LocalDateTime.now())) {
            throw new IllegalArgumentException("Impossible de planifier un rendez-vous dans le passé.");
        }
    }

    /**
     * Termine un RDV existant et crée une nouvelle Consultation (Orchestration de service).
     * @param rdvId ID du rendez-vous à terminer.
     */
    public void terminerRendezVous(Long rdvId) throws IllegalStateException {
        RendezVous rdv = rdvDAO.findById(rdvId);
        if (rdv == null) {
            throw new IllegalArgumentException("Rendez-vous non trouvé.");
        }

        if (rdv.getStatus() != RendezVousStatus.CONFIRME && rdv.getStatus() != RendezVousStatus.PLANIFIE) {
            throw new IllegalStateException("Ce rendez-vous ne peut pas être terminé (Statut: " + rdv.getStatus() + ").");
        }

        // 1. Changer le statut
        rdv.setStatus(RendezVousStatus.TERMINE);
        rdvDAO.save(rdv);

        // 2. Créer une Consultation vide liée au RDV (l'objet Consultation sera rempli plus tard)
        consultationService.createConsultationFromRendezVous(rdv);
    }

    // --- Méthodes de Recherche et Mise à Jour ---

    public List<RendezVous> findRendezVousByMedecin(Medecin medecin, LocalDateTime start, LocalDateTime end) {
        return rdvDAO.findByMedecinAndDateRange(medecin, start, end);
    }

    public List<RendezVous> findRendezVousForPatient(Patient patient) {
        // Utiliser le DAO pour éviter LazyInitializationException
        if (patient == null || patient.getId() == null) {
            return List.of();
        }
        Patient managedPatient = patientDAO.findById(patient.getId());
        if (managedPatient == null) {
            return List.of();
        }
        return rdvDAO.findByPatient(managedPatient);
    }

    public RendezVous findById(Long id) {
        return rdvDAO.findById(id);
    }
}