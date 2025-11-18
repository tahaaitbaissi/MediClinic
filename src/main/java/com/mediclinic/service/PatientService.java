package com.mediclinic.service;

import com.mediclinic.dao.PatientDAO;
import com.mediclinic.model.Patient;
import java.util.List;

public class PatientService {
    private PatientDAO patientDAO = new PatientDAO();

    public List<Patient> findAll() {
        return patientDAO.findAll();
    }

    public void save(Patient patient) {
        patientDAO.save(patient);
    }

    public void delete(Patient patient) {
        patientDAO.delete(patient);
    }

    public Patient findById(Long id) {
        return patientDAO.findById(id);
    }
}