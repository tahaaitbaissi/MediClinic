package com.mediclinic.dao;

import com.mediclinic.model.Facture;
import com.mediclinic.model.Patient;
import com.mediclinic.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import java.time.LocalDate;
import java.util.List;


public class FactureDAO extends AbstractDAO<Facture, Long> {

    public FactureDAO() {
        super();
    }

    /**
     * Récupère toutes les factures d'un patient.
     */
    public List<Facture> findByPatient(Patient patient) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            String hql = "FROM Facture f WHERE f.patient = :patient ORDER BY f.dateFacturation DESC";
            List<Facture> results = session.createQuery(hql, Facture.class)
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
     * Récupère les factures impayées.
     */
    public List<Facture> findUnpaid() {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            String hql = "FROM Facture f WHERE f.estPayee = false ORDER BY f.dateFacturation ASC";
            List<Facture> results = session.createQuery(hql, Facture.class)
                    .list();
            tx.commit();
            return results;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }
}