package com.mediclinic.dao;

import com.mediclinic.model.Patient;
import com.mediclinic.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import java.util.List;

public class PatientDAO extends AbstractDAO<Patient, Long> {

    public PatientDAO() {
        super();
    }

    /**
     * Recherche les patients par nom ou prénom (partiel).
     */
    public List<Patient> searchByName(String terme) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            String hql = "FROM Patient p WHERE p.nom LIKE :terme OR p.prenom LIKE :terme";
            List<Patient> results = session.createQuery(hql, Patient.class)
                    .setParameter("terme", "%" + terme + "%")
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
     * Recherche un patient par son email (doit être unique).
     */
    public Patient findByEmail(String email) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            String hql = "FROM Patient p WHERE p.email = :email";
            Patient result = session.createQuery(hql, Patient.class)
                    .setParameter("email", email)
                    .uniqueResult();
            tx.commit();
            return result;
        } catch (Exception e) {
            if (tx != null) tx.rollback();
            e.printStackTrace();
            throw e;
        }
    }
}