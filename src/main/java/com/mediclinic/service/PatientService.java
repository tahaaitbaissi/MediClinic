package com.mediclinic.service;

import com.mediclinic.dao.PatientDAO;
import com.mediclinic.dao.DossierMedicalDAO;
import com.mediclinic.dao.RendezVousDAO;
import com.mediclinic.model.Patient;
import com.mediclinic.model.DossierMedical;
import com.mediclinic.model.Medecin;
import com.mediclinic.model.Role;
import com.mediclinic.model.User;
import com.mediclinic.util.UserSession;
import java.time.LocalDate;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;
import java.util.regex.Pattern;

public class PatientService {

    private final PatientDAO patientDAO;
    private final DossierMedicalDAO dossierMedicalDAO; // Nécessaire pour créer le dossier
    private final RendezVousDAO rendezVousDAO; // Pour vérifier les rendez-vous

    // CONSTRUCTEUR : Injection de dépendances (simple)
    // Le service dépend des DAOs
    public PatientService() {
        this.patientDAO = new PatientDAO();
        this.dossierMedicalDAO = new DossierMedicalDAO();
        this.rendezVousDAO = new RendezVousDAO();
    }

    // Patterns de validation
    private static final Pattern EMAIL_PATTERN = Pattern.compile(
            "^[A-Za-z0-9+_.-]+@[A-Za-z0-9.-]+\\.[A-Za-z]{2,}$"
    );
    private static final Pattern PHONE_PATTERN = Pattern.compile(
            "^[+]?[(]?[0-9]{1,4}[)]?[-\\s.]?[(]?[0-9]{1,4}[)]?[-\\s.]?[0-9]{1,9}$"
    );

    // --- Logique Métier ---

    /**
     * Vérifie la validité des coordonnées et sauvegarde le patient,
     * puis crée son dossier médical.
     */
    public Patient createPatient(Patient patient) throws IllegalArgumentException, SecurityException {
        // Check authentication and permission
        if (!UserSession.isAuthenticated()) {
            throw new SecurityException("Utilisateur non authentifié.");
        }
        
        User user = UserSession.getInstance().getUser();
        Role role = user.getRole();
        
        // Only SEC and ADMIN can create patients
        if (role != Role.SEC && role != Role.ADMIN) {
            throw new SecurityException("Vous n'avez pas la permission de créer un patient.");
        }
        
        // Règle de gestion 1 : Coordonnées valides (email, téléphone)
        if (!validateContactInfo(patient)) {
            throw new IllegalArgumentException("Informations de contact (email/téléphone) invalides ou incomplètes.");
        }

        // Validation du format email
        if (!isValidEmail(patient.getEmail())) {
            throw new IllegalArgumentException("Format d'email invalide.");
        }

        // Validation du format téléphone
        if (!isValidPhone(patient.getTelephone())) {
            throw new IllegalArgumentException("Format de téléphone invalide.");
        }

        // Validation de la date de naissance
        if (patient.getDateNaissance() != null && patient.getDateNaissance().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La date de naissance ne peut pas être dans le futur.");
        }

        // Règle de gestion 2 : Vérifier l'unicité de l'email
        if (patientDAO.findByEmail(patient.getEmail()) != null) {
            throw new IllegalArgumentException("L'email est déjà utilisé par un autre patient.");
        }

        // 1. Sauvegarder le patient d'abord pour obtenir l'ID généré
        Patient savedPatient = patientDAO.save(patient);

        // 2. Création automatique du Dossier Médical avec le patient managé
        DossierMedical dossier = new DossierMedical();
        dossier.setPatient(savedPatient);
        
        // 3. Sauvegarder le dossier médical
        DossierMedical savedDossier = dossierMedicalDAO.save(dossier);
        
        // 4. Établir la relation bidirectionnelle sur les entités managées
        savedPatient.setDossierMedical(savedDossier);

        return savedPatient;
    }

    /**
     * Valide la présence d'un email et d'un téléphone.
     */
    private boolean validateContactInfo(Patient patient) {
        // Validation simple : vérifie si les champs ne sont pas vides ou null
        return patient.getEmail() != null && !patient.getEmail().trim().isEmpty() &&
                patient.getTelephone() != null && !patient.getTelephone().trim().isEmpty();
    }

    /**
     * Valide le format de l'email.
     */
    private boolean isValidEmail(String email) {
        return email != null && EMAIL_PATTERN.matcher(email).matches();
    }

    /**
     * Valide le format du téléphone.
     */
    private boolean isValidPhone(String phone) {
        return phone != null && PHONE_PATTERN.matcher(phone).matches();
    }

    /**
     * Suppression contrôlée pour éviter la perte d'historique.
     * En mode strict, nous n'autorisons pas la suppression si des RDV existent.
     */
    public void deletePatient(Long patientId) throws IllegalStateException, SecurityException {
        // Check authentication and permission - Only ADMIN can delete
        if (!UserSession.isAuthenticated()) {
            throw new SecurityException("Utilisateur non authentifié.");
        }
        
        User user = UserSession.getInstance().getUser();
        if (user.getRole() != Role.ADMIN) {
            throw new SecurityException("Seul l'administrateur peut supprimer un patient.");
        }
        
        Patient patient = patientDAO.findById(patientId);
        if (patient == null) {
            throw new IllegalArgumentException("Patient non trouvé.");
        }

        // Utiliser le DAO pour éviter LazyInitializationException
        List<com.mediclinic.model.RendezVous> rendezVousList = rendezVousDAO.findByPatient(patient);
        if (rendezVousList != null && !rendezVousList.isEmpty()) {
            // Règle de gestion : Empêcher la perte d'historique (RendezVous et Factures)
            throw new IllegalStateException("Impossible de supprimer le patient : il existe des Rendez-vous ou un historique associé. Utilisez un statut 'Désactivé' à la place (Soft Delete).");
        }

        patientDAO.delete(patient);
    }

    /**
     * Met à jour les informations d'un patient existant.
     */
    public Patient updatePatient(Patient patient) throws IllegalArgumentException, SecurityException {
        // Check authentication and permission
        if (!UserSession.isAuthenticated()) {
            throw new SecurityException("Utilisateur non authentifié.");
        }
        
        User user = UserSession.getInstance().getUser();
        Role role = user.getRole();
        
        // Only SEC and ADMIN can update patients
        if (role != Role.SEC && role != Role.ADMIN) {
            throw new SecurityException("Vous n'avez pas la permission de modifier un patient.");
        }
        
        // Vérifier que le patient existe
        if (patient.getId() == null) {
            throw new IllegalArgumentException("ID du patient manquant.");
        }
        
        Patient existingPatient = patientDAO.findById(patient.getId());
        if (existingPatient == null) {
            throw new IllegalArgumentException("Patient non trouvé.");
        }
        
        // Validation des coordonnées
        if (!validateContactInfo(patient)) {
            throw new IllegalArgumentException("Informations de contact (email/téléphone) invalides ou incomplètes.");
        }

        // Validation du format email
        if (!isValidEmail(patient.getEmail())) {
            throw new IllegalArgumentException("Format d'email invalide.");
        }

        // Validation du format téléphone
        if (!isValidPhone(patient.getTelephone())) {
            throw new IllegalArgumentException("Format de téléphone invalide.");
        }

        // Validation de la date de naissance
        if (patient.getDateNaissance() != null && patient.getDateNaissance().isAfter(LocalDate.now())) {
            throw new IllegalArgumentException("La date de naissance ne peut pas être dans le futur.");
        }

        // Vérifier l'unicité de l'email (sauf pour le patient actuel)
        Patient patientWithSameEmail = patientDAO.findByEmail(patient.getEmail());
        if (patientWithSameEmail != null && !patientWithSameEmail.getId().equals(patient.getId())) {
            throw new IllegalArgumentException("L'email est déjà utilisé par un autre patient.");
        }
        
        // Mettre à jour le patient (save/merge gère à la fois save et update)
        return patientDAO.save(patient);
    }

    // --- Méthodes simples (Pass-Through) ---

    public Patient findById(Long id) {
        return patientDAO.findById(id);
    }

    public List<Patient> findAll() {
        // Check authentication
        if (!UserSession.isAuthenticated()) {
            throw new SecurityException("Utilisateur non authentifié.");
        }
        
        return findAllForCurrentUser();
    }

    /**
     * Get all patients filtered by current user's role
     */
    public List<Patient> findAllForCurrentUser() {
        if (!UserSession.isAuthenticated()) {
            throw new SecurityException("Utilisateur non authentifié.");
        }
        
        User user = UserSession.getInstance().getUser();
        Role role = user.getRole();
        
        if (role == Role.ADMIN || role == Role.SEC) {
            // ADMIN and SEC see all patients
            return patientDAO.findAll();
        } else if (role == Role.MEDECIN) {
            // Doctors see only patients from their appointments
            Medecin medecin = user.getMedecin();
            if (medecin == null) {
                return List.of();
            }
            
            // Get all appointments for this doctor
            List<com.mediclinic.model.RendezVous> appointments = rendezVousDAO.findByMedecin(medecin);
            
            // Extract unique patients
            Set<Long> patientIds = appointments.stream()
                .map(rdv -> rdv.getPatient().getId())
                .collect(Collectors.toSet());
            
            // Return patients with those IDs
            return patientIds.stream()
                .map(id -> patientDAO.findById(id))
                .filter(p -> p != null)
                .collect(Collectors.toList());
        }
        
        return List.of();
    }

    public List<Patient> searchPatients(String term) {
        // Check authentication
        if (!UserSession.isAuthenticated()) {
            throw new SecurityException("Utilisateur non authentifié.");
        }
        
        // Apply role-based filtering to search results
        List<Patient> allResults = patientDAO.searchByName(term);
        List<Patient> filteredResults = findAllForCurrentUser();
        
        // Return intersection of search results and filtered results
        Set<Long> allowedPatientIds = filteredResults.stream()
            .map(Patient::getId)
            .collect(Collectors.toSet());
        
        return allResults.stream()
            .filter(p -> allowedPatientIds.contains(p.getId()))
            .collect(Collectors.toList());
    }

    public DossierMedical getDossier(Long patientId) {
        // Check authentication
        if (!UserSession.isAuthenticated()) {
            throw new SecurityException("Utilisateur non authentifié.");
        }
        
        // Verify user can access this patient
        Patient patient = patientDAO.findById(patientId);
        if (patient == null) {
            return null;
        }
        
        // Check if current user can access this patient
        List<Patient> accessiblePatients = findAllForCurrentUser();
        boolean canAccess = accessiblePatients.stream()
            .anyMatch(p -> p.getId().equals(patientId));
        
        if (!canAccess) {
            throw new SecurityException("Vous n'avez pas accès à ce patient.");
        }
        
        return patient.getDossierMedical();
    }
    
    /**
     * Check if current user can create patients
     */
    public boolean canCreatePatient() {
        if (!UserSession.isAuthenticated()) {
            return false;
        }
        Role role = UserSession.getInstance().getUser().getRole();
        return role == Role.SEC || role == Role.ADMIN;
    }
    
    /**
     * Check if current user can modify patients
     */
    public boolean canModifyPatient() {
        if (!UserSession.isAuthenticated()) {
            return false;
        }
        Role role = UserSession.getInstance().getUser().getRole();
        return role == Role.SEC || role == Role.ADMIN;
    }
    
    /**
     * Check if current user can delete patients
     */
    public boolean canDeletePatient() {
        if (!UserSession.isAuthenticated()) {
            return false;
        }
        Role role = UserSession.getInstance().getUser().getRole();
        return role == Role.ADMIN;
    }
}