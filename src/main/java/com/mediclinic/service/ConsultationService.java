package com.mediclinic.service;

import com.mediclinic.dao.ConsultationDAO;
import com.mediclinic.dao.DossierMedicalDAO;
import com.mediclinic.model.Consultation;
import com.mediclinic.model.DossierMedical;
import com.mediclinic.model.RendezVous;
import com.mediclinic.model.RendezVousStatus;
import java.time.LocalDateTime;
import java.util.List;

public class ConsultationService {

    private final ConsultationDAO consultationDAO;
    private final DossierMedicalDAO dossierMedicalDAO;

    // CONSTRUCTEUR : Injection de dépendances
    public ConsultationService() {
        this.consultationDAO = new ConsultationDAO();
        this.dossierMedicalDAO = new DossierMedicalDAO();
    }

    // --- Méthodes de Création et Mise à Jour ---

    /**
     * Crée l'objet Consultation initiale dès que le RendezVous est terminé.
     * Cette méthode est appelée par le RendezVousService.
     */
    public Consultation createConsultationFromRendezVous(RendezVous rdv) throws IllegalStateException {
        if (rdv.getStatus() != RendezVousStatus.TERMINE) {
            throw new IllegalStateException("Le Rendez-vous doit être 'TERMINÉ' pour générer une Consultation.");
        }

        // 1. Charger le patient avec son dossier médical pour éviter LazyInitializationException
        if (rdv.getPatient() == null || rdv.getPatient().getId() == null) {
            throw new IllegalArgumentException("Le rendez-vous doit être associé à un patient valide.");
        }

        // Charger le patient depuis la base pour avoir accès au dossier
        com.mediclinic.dao.PatientDAO patientDAO = new com.mediclinic.dao.PatientDAO();
        com.mediclinic.model.Patient patient = patientDAO.findById(rdv.getPatient().getId());
        
        if (patient == null) {
            throw new IllegalArgumentException("Patient introuvable.");
        }

        // 2. Trouver le Dossier Médical correspondant au Patient
        // Charger le dossier séparément pour éviter les problèmes de lazy loading
        DossierMedical dossier = dossierMedicalDAO.findByPatientId(patient.getId());

        if (dossier == null) {
            // Dans un scénario réel, le Dossier est créé avec le Patient. Si non, il faut le créer ici.
            throw new IllegalStateException("Dossier médical introuvable pour le patient: " + patient.getNomComplet());
        }

        // 3. Création de la Consultation
        Consultation consultation = new Consultation();
        consultation.setDateConsultation(LocalDateTime.now());
        consultation.setRendezVous(rdv);
        consultation.setDossierMedical(dossier);
        
        // Note: Pas besoin d'appeler dossier.addConsultation() car cela accéderait
        // à la collection lazy-loaded. La relation est établie via setDossierMedical().

        // 4. Sauvegarde de la Consultation
        return consultationDAO.save(consultation);
    }

    /**
     * Met à jour les notes médicales de la consultation après l'examen.
     */
    public Consultation updateConsultationNotes(Long consultationId, String observations, String diagnostic, String prescriptions) {
        Consultation consultation = consultationDAO.findById(consultationId);

        if (consultation == null) {
            throw new IllegalArgumentException("Consultation introuvable.");
        }

        consultation.setObservations(observations);
        consultation.setDiagnostic(diagnostic);
        consultation.setPrescriptions(prescriptions);

        consultationDAO.save(consultation);
        return consultation;
    }

    // --- Méthodes de Recherche et Historique ---

    /**
     * Récupère toutes les consultations liées à un Dossier Médical.
     */
    public List<Consultation> getConsultationsByDossier(Long dossierId) {
        DossierMedical dossier = dossierMedicalDAO.findById(dossierId);

        if (dossier == null) {
            return List.of(); // Retourne une liste vide si le dossier n'existe pas
        }

        // Accéder à la liste dans la même transaction que le chargement du dossier
        // La liste est chargée car elle est dans la même session transactionnelle
        List<Consultation> consultations = dossier.getHistoriqueConsultations();
        
        // S'assurer que la liste n'est pas null
        return consultations != null ? consultations : List.of();
    }

    public Consultation findById(Long id) {
        return consultationDAO.findById(id);
    }
}