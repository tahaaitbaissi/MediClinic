package com.mediclinic.util;

import com.mediclinic.model.*;
import org.hibernate.SessionFactory;
import org.hibernate.cfg.Configuration;

public class HibernateUtil {

    private static final SessionFactory sessionFactory = buildSessionFactory();

    private static SessionFactory buildSessionFactory() {
        try {
            // Crée la SessionFactory à partir du fichier hibernate.cfg.xml
            Configuration configuration = new Configuration();
            configuration.configure("hibernate.cfg.xml");

            // Enregistrement des Entités
            configuration.addAnnotatedClass(Patient.class);
            configuration.addAnnotatedClass(Medecin.class);
            configuration.addAnnotatedClass(RendezVous.class);
            configuration.addAnnotatedClass(DossierMedical.class);
            configuration.addAnnotatedClass(Consultation.class);
            configuration.addAnnotatedClass(Facture.class);
            configuration.addAnnotatedClass(LigneFacture.class);
            configuration.addAnnotatedClass(User.class);

            // Les Enums n'ont pas besoin d'être ajoutées explicitement
            return configuration.buildSessionFactory();

        } catch (Throwable ex) {
            // Affichage de l'erreur en cas d'échec de la SessionFactory
            System.err.println("Échec de la création de la SessionFactory: " + ex);
            throw new ExceptionInInitializerError(ex);
        }
    }

    public static SessionFactory getSessionFactory() {
        return sessionFactory;
    }

    public static void shutdown() {
        // Ferme les caches et les pools de connexions
        getSessionFactory().close();
    }
}