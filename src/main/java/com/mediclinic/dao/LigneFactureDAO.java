package com.mediclinic.dao;

import com.mediclinic.model.Facture;
import com.mediclinic.model.LigneFacture;

public class LigneFactureDAO extends AbstractDAO<LigneFacture, Long> {

    public LigneFactureDAO() {
        super();
        // Les recherches spécifiques ne sont généralement pas nécessaires ici car
        // les lignes sont toujours récupérées via la relation One-to-Many de l'objet Facture.
    }
}