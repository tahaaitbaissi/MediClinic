package com.mediclinic.dao;

import java.util.List;

// T est le type de l'entité (Patient, Medecin, etc.)
// ID est le type de la clé primaire (Long)
public interface GenericDAO<T, ID> {

    // Crée ou met à jour l'entité
    T save(T entity);

    // Recherche par clé primaire
    T findById(ID id);

    // Récupère toutes les entités du type T
    List<T> findAll();

    // Supprime l'entité
    void delete(T entity);

    // Supprime l'entité par ID
    void deleteById(ID id);
}