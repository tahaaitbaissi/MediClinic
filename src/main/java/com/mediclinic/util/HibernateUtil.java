package com.mediclinic.util;

import org.hibernate.SessionFactory;
import org.hibernate.boot.Metadata;
import org.hibernate.boot.MetadataSources;
import org.hibernate.boot.registry.StandardServiceRegistry;
import org.hibernate.boot.registry.StandardServiceRegistryBuilder;
import org.hibernate.cfg.Environment;
//import com.mediclinic.model.*;

import java.util.HashMap;
import java.util.Map;

public class HibernateUtil {
    private static StandardServiceRegistry registry;
    private static SessionFactory sessionFactory;

    public static SessionFactory getSessionFactory() {
        if (sessionFactory == null) {
            try {
                // Configuration d'Hibernate
                StandardServiceRegistryBuilder registryBuilder =
                        new StandardServiceRegistryBuilder();

                // Paramètres de configuration
                Map<String, Object> settings = new HashMap<>();
                settings.put(Environment.DRIVER, "com.mysql.cj.jdbc.Driver");
                settings.put(Environment.URL, "jdbc:mysql://localhost:3306/mediclinic?useSSL=false");
                settings.put(Environment.USER, "root");
                settings.put(Environment.PASS, "elkarmiamine"); // Remplacez par votre mot de passe
                settings.put(Environment.DIALECT, "org.hibernate.dialect.MySQL8Dialect");
                settings.put(Environment.SHOW_SQL, "true");
                settings.put(Environment.FORMAT_SQL, "true");
                settings.put(Environment.HBM2DDL_AUTO, "update"); // update, create, create-drop
                settings.put(Environment.CURRENT_SESSION_CONTEXT_CLASS, "thread");

                registryBuilder.applySettings(settings);
                registry = registryBuilder.build();

                // Enregistrement des entités
                /*
                MetadataSources sources = new MetadataSources(registry);
                sources.addAnnotatedClass(Patient.class);
                sources.addAnnotatedClass(Medecin.class);
                sources.addAnnotatedClass(RendezVous.class);
                sources.addAnnotatedClass(Consultation.class);
                sources.addAnnotatedClass(DossierMedical.class);
                sources.addAnnotatedClass(Facture.class);
                sources.addAnnotatedClass(LigneFacture.class);

                Metadata metadata = sources.getMetadataBuilder().build();

                sessionFactory = metadata.getSessionFactoryBuilder().build();
 */
            } catch (Exception e) {
                e.printStackTrace();
                if (registry != null) {
                    StandardServiceRegistryBuilder.destroy(registry);
                }
            }
        }
        return sessionFactory;
    }

    public static void shutdown() {
        if (registry != null) {
            StandardServiceRegistryBuilder.destroy(registry);
        }
    }
}