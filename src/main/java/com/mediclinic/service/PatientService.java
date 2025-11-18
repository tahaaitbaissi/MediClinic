package com.mediclinic.service;

import com.mediclinic.dao.PatientDAO;
import com.mediclinic.dao.DossierMedicalDAO;
import com.mediclinic.dao.RendezVousDAO;
import com.mediclinic.model.Patient;
import com.mediclinic.model.DossierMedical;
import java.time.LocalDate;
import java.util.List;
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
    public Patient createPatient(Patient patient) throws IllegalArgumentException {
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

        // 1. Sauvegarde du Patient
        patientDAO.save(patient);

        // 2. Création automatique du Dossier Médical (One-to-One)
        DossierMedical dossier = new DossierMedical();
        dossier.setPatient(patient);
        
        // Sauvegarde du Dossier
        dossierMedicalDAO.save(dossier);
        
        // 3. Maintenir la relation bidirectionnelle
        patient.setDossierMedical(dossier);

        return patient;
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
    public void deletePatient(Long patientId) throws IllegalStateException {
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

    // --- Méthodes simples (Pass-Through) ---

    public Patient findById(Long id) {
        return patientDAO.findById(id);
    }

    public List<Patient> findAll() {
        return patientDAO.findAll();
    }

    public List<Patient> searchPatients(String term) {
        // Utilise la recherche implémentée dans le DAO
        return patientDAO.searchByName(term);
    }

    public DossierMedical getDossier(Long patientId) {
        Patient patient = patientDAO.findById(patientId);
        return patient != null ? patient.getDossierMedical() : null;
    }
}