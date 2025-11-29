package com.mediclinic.service;

import com.mediclinic.dao.MedecinDAO;
import com.mediclinic.dao.RendezVousDAO;
import com.mediclinic.model.Medecin;
import com.mediclinic.model.SpecialiteMedecin;
import java.util.List;

public class MedecinService {

    private final MedecinDAO medecinDAO;
    private final RendezVousDAO rendezVousDAO;

    // CONSTRUCTEUR : Initialisation du DAO
    public MedecinService() {
        this.medecinDAO = new MedecinDAO();
        this.rendezVousDAO = new RendezVousDAO();
    }

    // --- Méthodes de Gestion (CRUD) ---

    /**
     * Sauvegarde ou met à jour un Médecin.
     */
    public Medecin saveMedecin(Medecin medecin) {
        // En théorie, on pourrait valider l'unicité de l'email ici
        // Mais nous laissons le DAO/DB gérer la contrainte UNIQUE.
        return medecinDAO.save(medecin);
    }

    /**
     * Met à jour un médecin existant par son ID.
     * Utilise l'ID pour éviter les problèmes d'entités détachées.
     */
    public Medecin updateMedecin(Long medecinId, String nom, String prenom, 
                                  SpecialiteMedecin specialite, String email, String telephone) {
        if (medecinId == null) {
            throw new IllegalArgumentException("L'ID du médecin est requis.");
        }
        
        Medecin existingMedecin = medecinDAO.findById(medecinId);
        if (existingMedecin == null) {
            throw new IllegalArgumentException("Médecin non trouvé avec l'ID: " + medecinId);
        }
        
        // Mettre à jour les champs
        existingMedecin.setNom(nom);
        existingMedecin.setPrenom(prenom);
        existingMedecin.setSpecialite(specialite);
        existingMedecin.setEmail(email);
        existingMedecin.setTelephone(telephone);
        
        return medecinDAO.save(existingMedecin);
    }

    /**
     * Suppression contrôlée pour éviter la perte d'historique.
     * Un médecin ne peut être supprimé s'il a encore des rendez-vous.
     */
    public void deleteMedecin(Long medecinId) throws IllegalStateException {
        Medecin medecin = medecinDAO.findById(medecinId);

        if (medecin == null) {
            throw new IllegalArgumentException("Médecin non trouvé.");
        }

        // Vérification de l'historique de planification en utilisant le DAO
        Long rdvCount = rendezVousDAO.countByMedecin(medecin);

        if (rdvCount > 0) {
            throw new IllegalStateException("Impossible de supprimer le médecin : il est lié à " +
                    rdvCount + " rendez-vous. Archivez-le plutôt.");
        }

        medecinDAO.delete(medecin);
    }

    // --- Méthodes de Recherche ---

    public Medecin findById(Long id) {
        return medecinDAO.findById(id);
    }

    public List<Medecin> findAll() {
        return medecinDAO.findAll();
    }

    /**
     * Recherche les médecins par spécialité.
     * C'est une fonction clé pour le Front-End lors de la prise de RDV.
     */
    public List<Medecin> findBySpecialite(SpecialiteMedecin specialite) {
        return medecinDAO.findBySpecialite(specialite);
    }

    /**
     * Recherche un médecin par nom ou prénom.
     */
    public List<Medecin> searchByName(String nom, String prenom) {
        return medecinDAO.searchByName(nom, prenom);
    }
}