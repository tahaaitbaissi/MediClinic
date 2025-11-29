package com.mediclinic.dao;

import com.mediclinic.model.DossierMedical;
import com.mediclinic.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;

public class DossierMedicalDAO extends AbstractDAO<DossierMedical, Long> {

    public DossierMedicalDAO() {
        super();
    }

    /**
     * Trouve le dossier médical d'un patient par son ID.
     */
    public DossierMedical findByPatientId(Long patientId) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            String hql = "FROM DossierMedical d WHERE d.patient.id = :patientId";
            DossierMedical result = session.createQuery(hql, DossierMedical.class)
                    .setParameter("patientId", patientId)
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