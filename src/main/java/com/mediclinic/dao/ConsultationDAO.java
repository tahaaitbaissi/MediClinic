package com.mediclinic.dao;

import com.mediclinic.model.Consultation;
import com.mediclinic.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class ConsultationDAO extends AbstractDAO<Consultation, Long> {

    public ConsultationDAO() {
        super();
    }

    public Consultation findByRendezVousId(Long rdvId) {
        Transaction tx = null;
        try (
            Session session = HibernateUtil.getSessionFactory().openSession()
        ) {
            tx = session.beginTransaction();
            String hql =
                "FROM Consultation c JOIN FETCH c.rendezVous r JOIN FETCH r.patient WHERE r.id = :rdvId";
            Consultation result = session
                .createQuery(hql, Consultation.class)
                .setParameter("rdvId", rdvId)
                .uniqueResult();
            tx.commit();
            return result;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    public java.util.List<Consultation> findByMedecinId(Long medecinId) {
        Transaction tx = null;
        try (
            Session session = HibernateUtil.getSessionFactory().openSession()
        ) {
            tx = session.beginTransaction();
            String hql =
                "SELECT c FROM Consultation c " +
                "JOIN FETCH c.rendezVous r " +
                "JOIN FETCH r.patient p " +
                "JOIN r.medecin m " +
                "WHERE m.id = :medId";
            java.util.List<Consultation> result = session
                .createQuery(hql, Consultation.class)
                .setParameter("medId", medecinId)
                .getResultList();
            tx.commit();
            return result;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    /**
     * Récupère les consultations d'un dossier avec les dépendances nécessaires
     * pour l'affichage (rendez-vous et patient) afin d'éviter LazyInitializationException.
     */
    public java.util.List<Consultation> findByDossierId(Long dossierId) {
        Transaction tx = null;
        try (
            Session session = HibernateUtil.getSessionFactory().openSession()
        ) {
            tx = session.beginTransaction();
            String hql =
                "SELECT c FROM Consultation c " +
                "JOIN FETCH c.rendezVous r " +
                "JOIN FETCH r.patient p " +
                "JOIN c.dossierMedical d " +
                "WHERE d.id = :dossierId";
            java.util.List<Consultation> result = session
                .createQuery(hql, Consultation.class)
                .setParameter("dossierId", dossierId)
                .getResultList();
            tx.commit();
            return result;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }

    /**
     * Récupère une consultation avec toutes ses dépendances pour l'export PDF
     * (rendez-vous, patient, médecin) afin d'éviter LazyInitializationException.
     */
    public Consultation findByIdWithAllDetails(Long consultationId) {
        Transaction tx = null;
        try (
            Session session = HibernateUtil.getSessionFactory().openSession()
        ) {
            tx = session.beginTransaction();
            String hql =
                "SELECT c FROM Consultation c " +
                "JOIN FETCH c.rendezVous r " +
                "JOIN FETCH r.patient p " +
                "JOIN FETCH r.medecin m " +
                "WHERE c.id = :consultationId";
            Consultation result = session
                .createQuery(hql, Consultation.class)
                .setParameter("consultationId", consultationId)
                .uniqueResult();
            tx.commit();
            return result;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            throw e;
        }
    }
}
