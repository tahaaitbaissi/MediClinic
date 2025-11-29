package com.mediclinic.dao;

import com.mediclinic.model.Medecin;
import com.mediclinic.model.Patient;
import com.mediclinic.model.RendezVous;
import com.mediclinic.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import java.time.LocalDateTime;
import java.util.List;

public class RendezVousDAO extends AbstractDAO<RendezVous, Long> {

    public RendezVousDAO() {
        super();
    }

    /**
     * Récupère tous les RDV d'un médecin pour une période donnée (important pour l'affichage de l'agenda).
     */
    public List<RendezVous> findByMedecinAndDateRange(Medecin medecin, LocalDateTime debut, LocalDateTime fin) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            String hql = "FROM RendezVous r WHERE r.medecin = :medecin AND r.dateHeureDebut BETWEEN :debut AND :fin ORDER BY r.dateHeureDebut";
            List<RendezVous> results = session.createQuery(hql, RendezVous.class)
                    .setParameter("medecin", medecin)
                    .setParameter("debut", debut)
                    .setParameter("fin", fin)
                    .list();
            tx.commit();
            return results;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Vérifie s'il existe des RDV en conflit pour un médecin dans une plage horaire.
     * C'est la fonction la plus importante pour la logique de collision.
     */
    public Long countConflictingAppointments(Medecin medecin, LocalDateTime newStart, LocalDateTime newEnd, Long currentRdvId) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            // Logique de collision (A chevauche B si A.start < B.end ET A.end > B.start)
            String hql = "SELECT COUNT(r) FROM RendezVous r " +
                    "WHERE r.medecin = :medecin " +
                    "AND r.id <> :currentRdvId " + // Exclure le RDV en cours de modification
                    "AND r.dateHeureDebut < :newEnd " +
                    "AND r.dateHeureFin > :newStart";

            Long result = session.createQuery(hql, Long.class)
                    .setParameter("medecin", medecin)
                    .setParameter("newEnd", newEnd)
                    .setParameter("newStart", newStart)
                    .setParameter("currentRdvId", currentRdvId != null ? currentRdvId : -1L) // Utiliser -1 si c'est une création
                    .uniqueResult();
            tx.commit();
            return result;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Récupère tous les RDV d'un patient.
     */
    public List<RendezVous> findByPatient(Patient patient) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            String hql = "FROM RendezVous r WHERE r.patient = :patient ORDER BY r.dateHeureDebut DESC";
            List<RendezVous> results = session.createQuery(hql, RendezVous.class)
                    .setParameter("patient", patient)
                    .list();
            tx.commit();
            return results;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Compte le nombre de rendez-vous d'un médecin.
     */
    public Long countByMedecin(Medecin medecin) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            String hql = "SELECT COUNT(r) FROM RendezVous r WHERE r.medecin = :medecin";
            Long result = session.createQuery(hql, Long.class)
                    .setParameter("medecin", medecin)
                    .uniqueResult();
            tx.commit();
            return result != null ? result : 0L;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }

    /**
     * Récupère tous les rendez-vous avec leurs relations (Patient et Medecin) chargées.
     * Utilisé pour l'affichage dans l'interface utilisateur.
     */
    public List<RendezVous> findAllWithDetails() {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            // JOIN FETCH pour charger Patient et Medecin de manière eager
            String hql = "SELECT DISTINCT r FROM RendezVous r " +
                    "LEFT JOIN FETCH r.patient " +
                    "LEFT JOIN FETCH r.medecin " +
                    "ORDER BY r.dateHeureDebut DESC";
            List<RendezVous> results = session.createQuery(hql, RendezVous.class).list();
            tx.commit();
            return results;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }
}