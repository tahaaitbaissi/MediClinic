package com.mediclinic.dao;

import com.mediclinic.model.Medecin;
import com.mediclinic.model.SpecialiteMedecin;
import com.mediclinic.util.HibernateUtil;
import org.hibernate.Session;
import org.hibernate.Transaction;
import java.util.List;

public class MedecinDAO extends AbstractDAO<Medecin, Long> {

    public MedecinDAO() {
        super();
    }

    /**
     * Recherche les médecins par spécialité.
     */
    public List<Medecin> findBySpecialite(SpecialiteMedecin specialite) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            String hql = "FROM Medecin m WHERE m.specialite = :spec";
            List<Medecin> results = session.createQuery(hql, Medecin.class)
                    .setParameter("spec", specialite)
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
     * Recherche un médecin par nom et prénom.
     */
    public List<Medecin> searchByName(String nom, String prenom) {
        Transaction tx = null;
        try (Session session = HibernateUtil.getSessionFactory().openSession()) {
            tx = session.beginTransaction();
            String hql = "FROM Medecin m WHERE m.nom LIKE :n AND m.prenom LIKE :p";
            List<Medecin> results = session.createQuery(hql, Medecin.class)
                    .setParameter("n", "%" + nom + "%")
                    .setParameter("p", "%" + prenom + "%")
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