package com.mediclinic.service;

import com.mediclinic.dao.MedecinDAO;
import com.mediclinic.dao.PatientDAO;
import com.mediclinic.dao.RendezVousDAO;
import com.mediclinic.model.Medecin;
import com.mediclinic.model.Patient;
import com.mediclinic.model.RendezVous;
import com.mediclinic.model.RendezVousStatus;
import com.mediclinic.model.Role;
import com.mediclinic.model.User;
import com.mediclinic.util.UserSession;
import java.time.Duration;
import java.time.LocalDateTime;
import java.util.List;

public class RendezVousService {
    /**
     * Retourne la liste d'attente (rendez-vous à venir, triés par heure, statut PLANIFIE ou CONFIRME)
     */
    public List<RendezVous> getWaitingRoomReservations() {
        List<RendezVous> all = rdvDAO.findAllWithDetails();
        return all.stream()
            .filter(rdv -> rdv.getStatus() == RendezVousStatus.PLANIFIE || rdv.getStatus() == RendezVousStatus.CONFIRME)
            .filter(rdv -> rdv.getDateHeureDebut() != null && rdv.getDateHeureDebut().isAfter(LocalDateTime.now().minusMinutes(10)))
            .sorted((a, b) -> a.getDateHeureDebut().compareTo(b.getDateHeureDebut()))
            .toList();
    }

    private final RendezVousDAO rdvDAO;
    private final MedecinDAO medecinDAO;
    private final PatientDAO patientDAO;
    private final ConsultationService consultationService;

    // Durée minimale d'un rendez-vous (15 minutes)
    private static final Duration MIN_APPOINTMENT_DURATION = Duration.ofMinutes(
        15
    );

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
     * @throws SecurityException Si l'utilisateur n'a pas la permission de créer/modifier.
     */
    public RendezVous planifierRendezVous(RendezVous rdv)
        throws IllegalStateException, IllegalArgumentException, SecurityException {
        // Check authentication and permission
        if (!UserSession.isAuthenticated()) {
            throw new SecurityException("Utilisateur non authentifié.");
        }

        User user = UserSession.getInstance().getUser();
        Role role = user.getRole();

        // SEC and ADMIN can create appointments
        if (role != Role.SEC && role != Role.ADMIN && role != Role.MEDECIN) {
            throw new SecurityException(
                "Vous n'avez pas la permission de créer un rendez-vous."
            );
        }

        // Check if SEC is trying to create appointment for their associated doctor
        if (role == Role.SEC) {
            Medecin userMedecin = user.getMedecin();
            if (userMedecin == null) {
                throw new SecurityException(
                    "Secrétaire non associée à un médecin."
                );
            }

            // SEC can only create appointments for their associated doctor
            if (
                rdv.getMedecin() == null ||
                !rdv.getMedecin().getId().equals(userMedecin.getId())
            ) {
                throw new SecurityException(
                    "Vous ne pouvez créer des rendez-vous que pour le médecin associé."
                );
            }
        }

        // 1. Validation des dates
        validateAppointmentDates(rdv);

        // 2. Vérification des Entités
        if (rdv.getPatient() == null || rdv.getMedecin() == null) {
            throw new IllegalArgumentException(
                "Le Rendez-vous doit être lié à un Patient et un Médecin valides."
            );
        }

        // Charger les entités pour s'assurer qu'elles sont attachées à la session si besoin
        Patient patient = patientDAO.findById(rdv.getPatient().getId());
        Medecin medecin = medecinDAO.findById(rdv.getMedecin().getId());

        if (patient == null || medecin == null) {
            throw new IllegalArgumentException(
                "Patient ou Médecin non trouvé dans la base de données."
            );
        }

        // 3. Règle de Gestion : Prévention des Collisions
        Long collisionCount = rdvDAO.countConflictingAppointments(
            medecin,
            rdv.getDateHeureDebut(),
            rdv.getDateHeureFin(),
            rdv.getId() // Passe l'ID pour exclure le RDV lui-même lors de la modification
        );

        if (collisionCount > 0) {
            throw new IllegalStateException(
                "Collision détectée : Le Médecin " +
                    medecin.getNomComplet() +
                    " a déjà un autre rendez-vous à ce créneau."
            );
        }

        // 4. Mise à jour des relations et statut par défaut
        rdv.setPatient(patient);
        rdv.setMedecin(medecin);
        if (rdv.getStatus() == null) {
            rdv.setStatus(RendezVousStatus.PLANIFIE);
        }

        // 5. Sauvegarde
        RendezVous savedRdv = rdvDAO.save(rdv);
        return savedRdv;
    }

    /**
     * Valide les dates d'un rendez-vous.
     */
    private void validateAppointmentDates(RendezVous rdv) {
        if (rdv.getDateHeureDebut() == null || rdv.getDateHeureFin() == null) {
            throw new IllegalArgumentException(
                "Les dates de début et de fin sont obligatoires."
            );
        }

        if (
            rdv.getDateHeureFin().isBefore(rdv.getDateHeureDebut()) ||
            rdv.getDateHeureFin().isEqual(rdv.getDateHeureDebut())
        ) {
            throw new IllegalArgumentException(
                "La date de fin doit être postérieure à la date de début."
            );
        }

        Duration duration = Duration.between(
            rdv.getDateHeureDebut(),
            rdv.getDateHeureFin()
        );
        if (duration.compareTo(MIN_APPOINTMENT_DURATION) < 0) {
            throw new IllegalArgumentException(
                "La durée minimale d'un rendez-vous est de " +
                    MIN_APPOINTMENT_DURATION.toMinutes() +
                    " minutes."
            );
        }

        // Pour les nouveaux rendez-vous (sans ID), ne pas permettre les dates passées
        if (
            rdv.getId() == null &&
            rdv.getDateHeureDebut().isBefore(LocalDateTime.now())
        ) {
            throw new IllegalArgumentException(
                "Impossible de planifier un rendez-vous dans le passé."
            );
        }
    }

    /**
     * Termine un RDV existant et crée une nouvelle Consultation (Orchestration de service).
     * @param rdvId ID du rendez-vous à terminer.
     */
    public void terminerRendezVous(Long rdvId)
        throws IllegalStateException, SecurityException {
        // Check authentication
        if (!UserSession.isAuthenticated()) {
            throw new SecurityException("Utilisateur non authentifié.");
        }

        RendezVous rdv = rdvDAO.findById(rdvId);
        if (rdv == null) {
            throw new IllegalArgumentException("Rendez-vous non trouvé.");
        }

        // Check permission - doctors can only complete their own appointments
        User user = UserSession.getInstance().getUser();
        if (user.getRole() == Role.MEDECIN) {
            Medecin medecin = user.getMedecin();
            if (
                medecin == null ||
                !medecin.getId().equals(rdv.getMedecin().getId())
            ) {
                throw new SecurityException(
                    "Vous ne pouvez terminer que vos propres rendez-vous."
                );
            }
        }

        // Valider la transition de statut
        if (
            !isValidStatusTransition(rdv.getStatus(), RendezVousStatus.TERMINE)
        ) {
            throw new IllegalStateException(
                "Ce rendez-vous ne peut pas être terminé. Statut actuel: " +
                    rdv.getStatus() +
                    ". Seuls les rendez-vous PLANIFIÉ ou CONFIRMÉ peuvent être terminés."
            );
        }

        // 1. Changer le statut et sauvegarder
        rdv.setStatus(RendezVousStatus.TERMINE);
        RendezVous savedRdv = rdvDAO.save(rdv);

        // 2. Recharger le RDV depuis la base pour avoir une entité fraîche
        RendezVous reloadedRdv = rdvDAO.findById(savedRdv.getId());

        // 3. Créer une Consultation vide liée au RDV (l'objet Consultation sera rempli plus tard)
        consultationService.createConsultationFromRendezVous(reloadedRdv);
    }

    // --- Méthodes de Recherche et Mise à Jour ---

    public List<RendezVous> findRendezVousByMedecin(
        Medecin medecin,
        LocalDateTime start,
        LocalDateTime end
    ) {
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

    public List<RendezVous> findAll() {
        // Check authentication
        if (!UserSession.isAuthenticated()) {
            throw new SecurityException("Utilisateur non authentifié.");
        }

        return findAllForCurrentUser();
    }

    /**
     * Get all appointments filtered by current user's role
     */
    public List<RendezVous> findAllForCurrentUser() {
        if (!UserSession.isAuthenticated()) {
            throw new SecurityException("Utilisateur non authentifié.");
        }

        User user = UserSession.getInstance().getUser();
        Role role = user.getRole();

        if (role == Role.ADMIN) {
            // ADMIN sees all appointments
            return rdvDAO.findAllWithDetails();
        } else if (role == Role.SEC) {
            // SEC sees only appointments for their associated doctor
            Medecin medecin = user.getMedecin();
            if (medecin == null) {
                // SEC must be associated with a doctor
                return List.of();
            }
            return rdvDAO.findByMedecin(medecin);
        } else if (role == Role.MEDECIN) {
            // Doctors see only their appointments
            Medecin medecin = user.getMedecin();
            if (medecin == null) {
                return List.of();
            }
            return rdvDAO.findByMedecin(medecin);
        }

        return List.of();
    }

    /**
     * Met à jour le statut d'un rendez-vous par son ID.
     * Utilise l'ID pour éviter les problèmes d'entités détachées.
     * Valide la transition de statut avant de sauvegarder.
     */
    public RendezVous updateStatus(Long rdvId, RendezVousStatus newStatus)
        throws SecurityException {
        // Check authentication
        if (!UserSession.isAuthenticated()) {
            throw new SecurityException("Utilisateur non authentifié.");
        }

        if (rdvId == null) {
            throw new IllegalArgumentException(
                "L'ID du rendez-vous est requis."
            );
        }
        if (newStatus == null) {
            throw new IllegalArgumentException("Le nouveau statut est requis.");
        }

        // Charger le rendez-vous depuis la base de données
        RendezVous rdv = rdvDAO.findById(rdvId);
        if (rdv == null) {
            throw new IllegalArgumentException(
                "Rendez-vous non trouvé avec l'ID: " + rdvId
            );
        }

        // Check permission - doctors can only modify their own appointments
        User user = UserSession.getInstance().getUser();
        if (user.getRole() == Role.MEDECIN) {
            Medecin medecin = user.getMedecin();
            if (
                medecin == null ||
                !medecin.getId().equals(rdv.getMedecin().getId())
            ) {
                throw new SecurityException(
                    "Vous ne pouvez modifier que vos propres rendez-vous."
                );
            }
        }

        // Valider la transition de statut
        if (!isValidStatusTransition(rdv.getStatus(), newStatus)) {
            throw new IllegalStateException(
                "Transition de statut invalide: " +
                    rdv.getStatus() +
                    " → " +
                    newStatus +
                    ". Un rendez-vous " +
                    rdv.getStatus() +
                    " ne peut pas devenir " +
                    newStatus +
                    "."
            );
        }

        // Mettre à jour le statut
        rdv.setStatus(newStatus);
        return rdvDAO.save(rdv);
    }

    /**
     * Check if current user can modify this appointment
     */
    public boolean canModifyAppointment(Long rdvId) {
        if (!UserSession.isAuthenticated()) {
            return false;
        }

        User user = UserSession.getInstance().getUser();
        Role role = user.getRole();

        // ADMIN can modify any appointment
        if (role == Role.ADMIN) {
            return true;
        }

        // SEC can only modify appointments for their associated doctor
        if (role == Role.SEC) {
            RendezVous rdv = rdvDAO.findById(rdvId);
            if (rdv == null) {
                return false;
            }
            Medecin userMedecin = user.getMedecin();
            return (
                userMedecin != null &&
                userMedecin.getId().equals(rdv.getMedecin().getId())
            );
        }

        // Doctors can only modify their own appointments
        if (role == Role.MEDECIN) {
            RendezVous rdv = rdvDAO.findById(rdvId);
            if (rdv == null) {
                return false;
            }
            Medecin medecin = user.getMedecin();
            return (
                medecin != null &&
                medecin.getId().equals(rdv.getMedecin().getId())
            );
        }

        return false;
    }

    /**
     * Check if current user can create appointments
     */
    public boolean canCreateAppointment() {
        if (!UserSession.isAuthenticated()) {
            return false;
        }
        Role role = UserSession.getInstance().getUser().getRole();
        return role == Role.SEC || role == Role.ADMIN;
    }

    /**
     * Find the next available time slot for a doctor.
     * Searches for the next 48 hours in 30-minute intervals.
     * Skips night hours (before 8:00 and after 18:00).
     *
     * @param medecin The doctor to find a slot for
     * @param preferredStart The preferred start time
     * @param duration The duration of the appointment in minutes
     * @return The next available LocalDateTime, or null if none found
     */
    public LocalDateTime findNextAvailableSlot(
        Medecin medecin,
        LocalDateTime preferredStart,
        int duration
    ) {
        if (medecin == null || preferredStart == null) {
            return null;
        }

        LocalDateTime checkTime = preferredStart;
        // Check slots every 30 mins for the next 48 hours (96 half-hour slots)
        for (int i = 0; i < 96; i++) {
            checkTime = checkTime.plusMinutes(30);

            // Skip night hours (before 8:00 AM and after 6:00 PM)
            int hour = checkTime.getHour();
            if (hour < 8 || hour >= 18) {
                // Skip to next day at 8:00 AM
                checkTime = checkTime.toLocalDate().plusDays(1).atTime(8, 0);
                continue;
            }

            // Skip weekends (Saturday = 6, Sunday = 7)
            int dayOfWeek = checkTime.getDayOfWeek().getValue();
            if (dayOfWeek == 6 || dayOfWeek == 7) {
                // Skip to Monday at 8:00 AM
                checkTime = checkTime
                    .toLocalDate()
                    .plusDays(dayOfWeek == 6 ? 2 : 1)
                    .atTime(8, 0);
                continue;
            }

            // Calculate end time based on duration
            LocalDateTime checkEnd = checkTime.plusMinutes(duration);

            // Check if this slot is available
            Long conflictCount = rdvDAO.countConflictingAppointments(
                medecin,
                checkTime,
                checkEnd,
                null // null ID means checking for new appointment
            );

            if (conflictCount == 0) {
                return checkTime; // Found available slot
            }
        }

        return null; // No slot found in the next 48 hours
    }

    /**
     * Met à jour un rendez-vous existant (par exemple pour changer le statut).
     * @deprecated Utiliser updateStatus(Long, RendezVousStatus) à la place
     */
    @Deprecated
    public RendezVous updateRendezVous(RendezVous rdv) {
        if (rdv == null || rdv.getId() == null) {
            throw new IllegalArgumentException(
                "Le rendez-vous doit avoir un ID valide pour être mis à jour."
            );
        }

        // Charger le rendez-vous existant depuis la base de données
        RendezVous existingRdv = rdvDAO.findById(rdv.getId());
        if (existingRdv == null) {
            throw new IllegalArgumentException("Rendez-vous non trouvé.");
        }

        // Mettre à jour uniquement les champs modifiables
        existingRdv.setStatus(rdv.getStatus());
        existingRdv.setDateHeureDebut(rdv.getDateHeureDebut());
        existingRdv.setDateHeureFin(rdv.getDateHeureFin());
        existingRdv.setMotif(rdv.getMotif());

        // Sauvegarder l'entité managée
        return rdvDAO.save(existingRdv);
    }

    /**
     * Vérifie si une transition de statut est valide.
     * Règles:
     * - PLANIFIE → CONFIRME, ANNULE (autorisé)
     * - CONFIRME → TERMINE, ANNULE (autorisé)
     * - TERMINE → rien (bloqué)
     * - ANNULE → rien (bloqué)
     */
    public boolean isValidStatusTransition(
        RendezVousStatus currentStatus,
        RendezVousStatus newStatus
    ) {
        if (currentStatus == null || newStatus == null) {
            return false;
        }

        // Même statut = pas de changement nécessaire
        if (currentStatus == newStatus) {
            return true;
        }

        switch (currentStatus) {
            case PLANIFIE:
                return (
                    newStatus == RendezVousStatus.CONFIRME ||
                    newStatus == RendezVousStatus.ANNULE
                );
            case CONFIRME:
                return (
                    newStatus == RendezVousStatus.TERMINE ||
                    newStatus == RendezVousStatus.ANNULE
                );
            case TERMINE:
            case ANNULE:
                // Ces statuts sont finaux, pas de transition autorisée
                return false;
            default:
                return false;
        }
    }
}
