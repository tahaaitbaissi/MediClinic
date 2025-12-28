package com.mediclinic.service;

import com.mediclinic.model.*;
import com.mediclinic.util.UserSession;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.DayOfWeek;
import java.util.List;
import java.util.Map;
import java.util.LinkedHashMap;
import java.util.stream.Collectors;

public class DashboardService {

    private final PatientService patientService;
    private final RendezVousService rendezVousService;
    private final MedecinService medecinService;
    private final FacturationService facturationService;

    public DashboardService() {
        this.patientService = new PatientService();
        this.rendezVousService = new RendezVousService();
        this.medecinService = new MedecinService();
        this.facturationService = new FacturationService();
    }

    /**
     * Get dashboard statistics based on user role
     */
    public DashboardStats getDashboardStats() {
        if (!UserSession.isAuthenticated()) {
            throw new IllegalStateException("User not authenticated");
        }

        User user = UserSession.getInstance().getUser();
        Role role = user.getRole();

        switch (role) {
            case ADMIN:
                return getAdminStats();
            case MEDECIN:
                return getMedecinStats(user);
            case SEC:
                return getSecretaryStats();
            default:
                throw new IllegalStateException("Unknown role: " + role);
        }
    }

    /**
     * Admin sees full statistics
     */
    private DashboardStats getAdminStats() {
        List<Patient> allPatients = patientService.findAll();
        List<Medecin> allDoctors = medecinService.findAll();
        
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        
        List<RendezVous> todayAppointments = getAllAppointmentsForDateRange(startOfDay, endOfDay);
        
        // Monthly revenue (current month)
        LocalDate firstDayOfMonth = today.withDayOfMonth(1);
        LocalDate lastDayOfMonth = today.withDayOfMonth(today.lengthOfMonth());
        BigDecimal monthlyRevenue = calculateMonthlyRevenue(firstDayOfMonth, lastDayOfMonth);
        
        // Unpaid invoices count
        List<Facture> unpaidInvoices = facturationService.getUnpaidFactures();
        
        return new DashboardStats(
            allPatients.size(),
            todayAppointments.size(),
            monthlyRevenue,
            allDoctors.size(),
            unpaidInvoices.size(),
            null, // patient count for doctor
            null  // medecin for doctor
        );
    }

    /**
     * Doctor sees only their appointments and patient info
     */
    private DashboardStats getMedecinStats(User user) {
        Medecin medecin = user.getMedecin();
        if (medecin == null) {
            throw new IllegalStateException("Doctor user has no associated Medecin profile");
        }

        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        
        // Get today's appointments for this doctor
        List<RendezVous> todayAppointments = rendezVousService.findRendezVousByMedecin(medecin, startOfDay, endOfDay);
        
        // Count unique patients from today's appointments
        long uniquePatientsToday = todayAppointments.stream()
            .map(RendezVous::getPatient)
            .distinct()
            .count();
        
        return new DashboardStats(
            null, // total patients (not relevant for doctor)
            todayAppointments.size(),
            null, // monthly revenue (not relevant for doctor)
            null, // active doctors (not relevant for doctor)
            null, // unpaid invoices (not relevant for doctor)
            (int) uniquePatientsToday,
            medecin
        );
    }

    /**
     * Secretary sees pending invoices and today's appointments
     */
    private DashboardStats getSecretaryStats() {
        LocalDate today = LocalDate.now();
        LocalDateTime startOfDay = today.atStartOfDay();
        LocalDateTime endOfDay = today.atTime(LocalTime.MAX);
        
        List<RendezVous> todayAppointments = getAllAppointmentsForDateRange(startOfDay, endOfDay);
        List<Facture> unpaidInvoices = facturationService.getUnpaidFactures();
        
        return new DashboardStats(
            null, // total patients (not shown for secretary)
            todayAppointments.size(),
            null, // monthly revenue (not shown for secretary)
            null, // active doctors (not shown for secretary)
            unpaidInvoices.size(),
            null, // patient count for doctor
            null  // medecin for doctor
        );
    }

    /**
     * Get all appointments for a date range
     */
    private List<RendezVous> getAllAppointmentsForDateRange(LocalDateTime start, LocalDateTime end) {
        List<RendezVous> allAppointments = rendezVousService.findAll();
        return allAppointments.stream()
            .filter(rdv -> {
                LocalDateTime rdvStart = rdv.getDateHeureDebut();
                return rdvStart != null && 
                       !rdvStart.isBefore(start) && 
                       !rdvStart.isAfter(end);
            })
            .collect(Collectors.toList());
    }

    /**
     * Calculate monthly revenue for a date range
     */
    private BigDecimal calculateMonthlyRevenue(LocalDate startDate, LocalDate endDate) {
        List<Facture> allInvoices = facturationService.getAllFactures();
        return allInvoices.stream()
            .filter(facture -> {
                LocalDate invoiceDate = facture.getDateFacturation();
                return invoiceDate != null &&
                       !invoiceDate.isBefore(startDate) &&
                       !invoiceDate.isAfter(endDate);
            })
            .filter(Facture::isEstPayee)
            .map(Facture::getMontantTotal)
            .reduce(BigDecimal.ZERO, BigDecimal::add);
    }

    /**
     * Get appointments grouped by day of the week for the current week
     * Returns a LinkedHashMap with day names and appointment counts
     */
    public Map<String, Integer> getWeeklyAppointments() {
        if (!UserSession.isAuthenticated()) {
            throw new IllegalStateException("User not authenticated");
        }

        LocalDate today = LocalDate.now();
        LocalDate startOfWeek = today.minusDays(today.getDayOfWeek().getValue() - 1); // Monday
        LocalDate endOfWeek = startOfWeek.plusDays(6); // Sunday

        // Get all appointments for the week
        List<RendezVous> weeklyAppointments = rendezVousService.findAll().stream()
            .filter(rdv -> {
                LocalDate rdvDate = rdv.getDateHeureDebut().toLocalDate();
                return rdvDate != null && 
                       !rdvDate.isBefore(startOfWeek) && 
                       !rdvDate.isAfter(endOfWeek) &&
                       (rdv.getStatus() == RendezVousStatus.PLANIFIE || 
                        rdv.getStatus() == RendezVousStatus.CONFIRME);
            })
            .collect(Collectors.toList());

        // Group by day of week
        Map<String, Integer> weeklyMap = new LinkedHashMap<>();
        String[] dayNames = {"Lun", "Mar", "Mer", "Jeu", "Ven", "Sam", "Dim"};
        
        for (int i = 0; i < 7; i++) {
            LocalDate day = startOfWeek.plusDays(i);
            final LocalDate currentDay = day;
            long count = weeklyAppointments.stream()
                .filter(rdv -> rdv.getDateHeureDebut().toLocalDate().equals(currentDay))
                .count();
            weeklyMap.put(dayNames[i], (int) count);
        }

        return weeklyMap;
    }

    /**
     * Get detailed upcoming appointments for the next 7 days
     * Filtered by current user's role
     */
    public List<RendezVous> getUpcomingAppointmentsDetailed(int daysAhead) {
        if (!UserSession.isAuthenticated()) {
            throw new IllegalStateException("User not authenticated");
        }

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime future = now.plusDays(daysAhead);
        User user = UserSession.getInstance().getUser();
        Role role = user.getRole();

        List<RendezVous> allAppointments = rendezVousService.findAll().stream()
            .filter(rdv -> {
                LocalDateTime rdvStart = rdv.getDateHeureDebut();
                return rdvStart != null && 
                       rdvStart.isAfter(now) && 
                       rdvStart.isBefore(future) &&
                       (rdv.getStatus() == RendezVousStatus.PLANIFIE || 
                        rdv.getStatus() == RendezVousStatus.CONFIRME);
            })
            .sorted((r1, r2) -> r1.getDateHeureDebut().compareTo(r2.getDateHeureDebut()))
            .collect(Collectors.toList());

        // Filter by role if needed
        if (role == Role.MEDECIN) {
            Medecin medecin = user.getMedecin();
            if (medecin != null) {
                allAppointments = allAppointments.stream()
                    .filter(rdv -> medecin.getId().equals(rdv.getMedecin().getId()))
                    .collect(Collectors.toList());
            }
        }

        return allAppointments;
    }

    /**
     * Get all appointments for today
     * Filtered by current user's role
     */
    public List<RendezVous> getTodayAppointments() {
        if (!UserSession.isAuthenticated()) {
            System.err.println("User not authenticated in getTodayAppointments");
            return List.of();
        }

        try {
            LocalDate today = LocalDate.now();
            LocalDateTime startOfDay = today.atStartOfDay();
            LocalDateTime endOfDay = today.atTime(23, 59, 59);
            User user = UserSession.getInstance().getUser();
            Role role = user.getRole();
            
            System.out.println("Getting appointments for today: " + today + " (" + startOfDay + " to " + endOfDay + ")");

            // Use the DAO to get all appointments with details loaded
            List<RendezVous> allRDV = rendezVousService.findAll();
            System.out.println("Total appointments in database: " + allRDV.size());
            
            List<RendezVous> allAppointments = allRDV.stream()
                .filter(rdv -> {
                    LocalDateTime rdvStart = rdv.getDateHeureDebut();
                    if (rdvStart == null) {
                        System.out.println("RDV " + rdv.getId() + " has null date");
                        return false;
                    }
                    
                    boolean isToday = !rdvStart.isBefore(startOfDay) && !rdvStart.isAfter(endOfDay);
                    boolean isValidStatus = rdv.getStatus() == RendezVousStatus.PLANIFIE || 
                                           rdv.getStatus() == RendezVousStatus.CONFIRME;
                    
                    if (isToday) {
                        System.out.println("Found appointment today: " + rdv.getId() + " at " + rdvStart + " status=" + rdv.getStatus());
                    }
                    
                    return isToday && isValidStatus;
                })
                .peek(rdv -> {
                    // Eagerly load relationships to avoid lazy loading issues
                    try {
                        if (rdv.getPatient() != null) {
                            rdv.getPatient().getNomComplet();
                        }
                        if (rdv.getMedecin() != null) {
                            rdv.getMedecin().getNomComplet();
                        }
                    } catch (Exception e) {
                        System.err.println("Error loading relationships: " + e.getMessage());
                    }
                })
                .sorted((r1, r2) -> r1.getDateHeureDebut().compareTo(r2.getDateHeureDebut()))
                .collect(Collectors.toList());
            
            System.out.println("Filtered appointments for today: " + allAppointments.size());

            // Filter by role if needed
            if (role == Role.MEDECIN) {
                Medecin medecin = user.getMedecin();
                if (medecin != null) {
                    allAppointments = allAppointments.stream()
                        .filter(rdv -> medecin.getId().equals(rdv.getMedecin().getId()))
                        .collect(Collectors.toList());
                    System.out.println("Filtered for doctor: " + allAppointments.size());
                }
            }

            return allAppointments;
        } catch (Exception e) {
            System.err.println("Error in getTodayAppointments: " + e.getMessage());
            e.printStackTrace();
            return List.of();
        }
    }

    /**
     * Data class for dashboard statistics
     */
    public static class DashboardStats {
        private final Integer totalPatients;
        private final Integer todayAppointments;
        private final BigDecimal monthlyRevenue;
        private final Integer activeDoctors;
        private final Integer unpaidInvoices;
        private final Integer patientsTodayForDoctor;
        private final Medecin medecin;

        public DashboardStats(Integer totalPatients, Integer todayAppointments, BigDecimal monthlyRevenue,
                             Integer activeDoctors, Integer unpaidInvoices, Integer patientsTodayForDoctor,
                             Medecin medecin) {
            this.totalPatients = totalPatients;
            this.todayAppointments = todayAppointments;
            this.monthlyRevenue = monthlyRevenue;
            this.activeDoctors = activeDoctors;
            this.unpaidInvoices = unpaidInvoices;
            this.patientsTodayForDoctor = patientsTodayForDoctor;
            this.medecin = medecin;
        }

        // Getters
        public Integer getTotalPatients() { return totalPatients; }
        public Integer getTodayAppointments() { return todayAppointments; }
        public BigDecimal getMonthlyRevenue() { return monthlyRevenue; }
        public Integer getActiveDoctors() { return activeDoctors; }
        public Integer getUnpaidInvoices() { return unpaidInvoices; }
        public Integer getPatientsTodayForDoctor() { return patientsTodayForDoctor; }
        public Medecin getMedecin() { return medecin; }
    }
}

