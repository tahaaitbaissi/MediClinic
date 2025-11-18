package com.mediclinic.service;

import com.mediclinic.dao.RendezVousDAO;
import com.mediclinic.model.RendezVous;
import java.util.List;

public class RendezVousService {
    private RendezVousDAO rendezVousDAO = new RendezVousDAO();

    public List<RendezVous> findAll() {
        return rendezVousDAO.findAll();
    }

    public void save(RendezVous rendezVous) {
        rendezVousDAO.save(rendezVous);
    }
}