# MediClinic Services API Documentation

This document provides comprehensive documentation for all service layer methods in the MediClinic application. These services handle business logic, validation, and data access with role-based security.

---

## Table of Contents

1. [PatientService](#patientservice)
2. [RendezVousService](#rendezvousservice)
3. [MedecinService](#medecinservice)
4. [FacturationService](#facturationservice)
5. [ConsultationService](#consultationservice)
6. [AuthService](#authservice)
7. [UserService](#userservice)
8. [DashboardService](#dashboardservice)

---

## PatientService

Service for managing patient records and medical files.

### Constructor
```java
public PatientService()
```
Initializes the service with required DAOs.

---

### `createPatient`
Creates a new patient and automatically creates their medical file (DossierMedical).

**Signature:**
```java
public Patient createPatient(Patient patient) throws IllegalArgumentException, SecurityException
```

**Parameters:**
- `patient` (Patient) - The patient object to create. Must have valid email, telephone, and other required fields.

**Returns:**
- `Patient` - The created patient with generated ID and associated medical file.

**Throws:**
- `SecurityException` - If user is not authenticated or doesn't have permission (SEC or ADMIN required).
- `IllegalArgumentException` - If:
  - Contact info (email/telephone) is invalid or incomplete
  - Email format is invalid
  - Phone format is invalid
  - Date of birth is in the future
  - Email already exists for another patient

**Permissions Required:**
- Roles: `SEC`, `ADMIN`

**Example:**
```java
Patient patient = new Patient();
patient.setNom("Dupont");
patient.setPrenom("Jean");
patient.setEmail("jean.dupont@example.com");
patient.setTelephone("+33123456789");
patient.setDateNaissance(LocalDate.of(1980, 1, 1));

Patient created = patientService.createPatient(patient);
```

---

### `deletePatient`
Deletes a patient if they have no associated appointments.

**Signature:**
```java
public void deletePatient(Long patientId) throws IllegalStateException, SecurityException
```

**Parameters:**
- `patientId` (Long) - The ID of the patient to delete.

**Throws:**
- `SecurityException` - If user is not authenticated or is not ADMIN.
- `IllegalArgumentException` - If patient not found.
- `IllegalStateException` - If patient has associated appointments (cannot delete).

**Permissions Required:**
- Roles: `ADMIN` only

**Example:**
```java
patientService.deletePatient(1L);
```

---

### `findById`
Finds a patient by their ID.

**Signature:**
```java
public Patient findById(Long id)
```

**Parameters:**
- `id` (Long) - The patient ID.

**Returns:**
- `Patient` - The patient if found, `null` otherwise.

**Note:** This method does not check permissions. Use `findAllForCurrentUser()` for filtered results.

---

### `findAll`
Gets all patients filtered by the current user's role.

**Signature:**
```java
public List<Patient> findAll()
```

**Returns:**
- `List<Patient>` - List of patients:
  - ADMIN/SEC: All patients
  - MEDECIN: Only patients from their appointments

**Throws:**
- `SecurityException` - If user is not authenticated.

**Example:**
```java
List<Patient> patients = patientService.findAll();
```

---

### `findAllForCurrentUser`
Gets all patients filtered by current user's role (explicit method).

**Signature:**
```java
public List<Patient> findAllForCurrentUser()
```

**Returns:**
- `List<Patient>` - Role-filtered list of patients.

**Throws:**
- `SecurityException` - If user is not authenticated.

**Role-based filtering:**
- `ADMIN` / `SEC`: All patients
- `MEDECIN`: Only patients from their appointments

---

### `searchPatients`
Searches patients by name, applying role-based filtering.

**Signature:**
```java
public List<Patient> searchPatients(String term)
```

**Parameters:**
- `term` (String) - Search term (matches name).

**Returns:**
- `List<Patient>` - Filtered search results based on user role.

**Throws:**
- `SecurityException` - If user is not authenticated.

---

### `getDossier`
Gets the medical file (DossierMedical) for a patient.

**Signature:**
```java
public DossierMedical getDossier(Long patientId)
```

**Parameters:**
- `patientId` (Long) - The patient ID.

**Returns:**
- `DossierMedical` - The medical file if found and accessible, `null` if patient not found.

**Throws:**
- `SecurityException` - If user is not authenticated or doesn't have access to the patient.

---

### `canCreatePatient`
Checks if the current user can create patients.

**Signature:**
```java
public boolean canCreatePatient()
```

**Returns:**
- `boolean` - `true` if user is SEC or ADMIN, `false` otherwise.

---

### `canModifyPatient`
Checks if the current user can modify patients.

**Signature:**
```java
public boolean canModifyPatient()
```

**Returns:**
- `boolean` - `true` if user is SEC or ADMIN, `false` otherwise.

---

### `canDeletePatient`
Checks if the current user can delete patients.

**Signature:**
```java
public boolean canDeletePatient()
```

**Returns:**
- `boolean` - `true` if user is ADMIN, `false` otherwise.

---

## RendezVousService

Service for managing appointments (rendez-vous) with scheduling logic and status management.

### Constructor
```java
public RendezVousService()
```

---

### `planifierRendezVous`
Plans a new appointment or updates an existing one. Validates scheduling conflicts.

**Signature:**
```java
public RendezVous planifierRendezVous(RendezVous rdv) throws IllegalStateException, IllegalArgumentException, SecurityException
```

**Parameters:**
- `rdv` (RendezVous) - The appointment to create/update. Must have:
  - Valid patient (with ID)
  - Valid doctor (with ID)
  - Start and end dates
  - Start date must be in the future (for new appointments)
  - Minimum duration of 15 minutes

**Returns:**
- `RendezVous` - The saved appointment with generated ID if new.

**Throws:**
- `SecurityException` - If user is not authenticated or is not SEC/ADMIN.
- `IllegalArgumentException` - If:
  - Patient or doctor is missing or invalid
  - Dates are invalid
  - Duration is less than 15 minutes
  - Start date is in the past (for new appointments)
- `IllegalStateException` - If there's a scheduling conflict (doctor already has appointment at that time).

**Permissions Required:**
- Roles: `SEC`, `ADMIN`

**Validation Rules:**
- Minimum appointment duration: 15 minutes
- Cannot schedule appointments in the past (for new appointments)
- Checks for conflicts with existing appointments

**Example:**
```java
RendezVous rdv = new RendezVous();
rdv.setPatient(patient);
rdv.setMedecin(medecin);
rdv.setDateHeureDebut(LocalDateTime.of(2024, 1, 15, 10, 0));
rdv.setDateHeureFin(LocalDateTime.of(2024, 1, 15, 10, 30));
rdv.setMotif("Consultation générale");

RendezVous saved = rendezVousService.planifierRendezVous(rdv);
```

---

### `terminerRendezVous`
Completes an appointment and automatically creates a consultation.

**Signature:**
```java
public void terminerRendezVous(Long rdvId) throws IllegalStateException, SecurityException
```

**Parameters:**
- `rdvId` (Long) - The appointment ID to complete.

**Throws:**
- `SecurityException` - If user is not authenticated or (for MEDECIN) doesn't own the appointment.
- `IllegalArgumentException` - If appointment not found.
- `IllegalStateException` - If appointment cannot be completed (invalid status transition).

**Permissions:**
- `ADMIN`, `SEC`: Can complete any appointment
- `MEDECIN`: Can only complete their own appointments

**Status Transition:**
- Valid: `PLANIFIE` → `TERMINE` or `CONFIRME` → `TERMINE`
- Invalid: `TERMINE` or `ANNULE` (final states)

**Side Effects:**
- Automatically creates a `Consultation` linked to the appointment.

---

### `updateStatus`
Updates the status of an appointment.

**Signature:**
```java
public RendezVous updateStatus(Long rdvId, RendezVousStatus newStatus) throws SecurityException
```

**Parameters:**
- `rdvId` (Long) - The appointment ID.
- `newStatus` (RendezVousStatus) - The new status.

**Returns:**
- `RendezVous` - The updated appointment.

**Throws:**
- `SecurityException` - If user is not authenticated or (for MEDECIN) doesn't own the appointment.
- `IllegalArgumentException` - If rdvId or newStatus is null, or appointment not found.
- `IllegalStateException` - If status transition is invalid.

**Permissions:**
- `ADMIN`, `SEC`: Can modify any appointment
- `MEDECIN`: Can only modify their own appointments

**Valid Status Transitions:**
- `PLANIFIE` → `CONFIRME` or `ANNULE`
- `CONFIRME` → `TERMINE` or `ANNULE`
- `TERMINE` / `ANNULE` → No transitions allowed (final states)

**Example:**
```java
RendezVous updated = rendezVousService.updateStatus(1L, RendezVousStatus.CONFIRME);
```

---

### `findAll`
Gets all appointments filtered by current user's role.

**Signature:**
```java
public List<RendezVous> findAll()
```

**Returns:**
- `List<RendezVous>` - List of appointments:
  - ADMIN/SEC: All appointments
  - MEDECIN: Only their appointments

**Throws:**
- `SecurityException` - If user is not authenticated.

---

### `findAllForCurrentUser`
Gets all appointments filtered by current user's role (explicit method).

**Signature:**
```java
public List<RendezVous> findAllForCurrentUser()
```

**Returns:**
- `List<RendezVous>` - Role-filtered list of appointments.

**Throws:**
- `SecurityException` - If user is not authenticated.

---

### `findRendezVousByMedecin`
Finds appointments for a specific doctor within a date range.

**Signature:**
```java
public List<RendezVous> findRendezVousByMedecin(Medecin medecin, LocalDateTime start, LocalDateTime end)
```

**Parameters:**
- `medecin` (Medecin) - The doctor.
- `start` (LocalDateTime) - Start of date range.
- `end` (LocalDateTime) - End of date range.

**Returns:**
- `List<RendezVous>` - Appointments for the doctor in the specified range.

**Note:** No permission check. Use with caution.

---

### `findRendezVousForPatient`
Finds all appointments for a specific patient.

**Signature:**
```java
public List<RendezVous> findRendezVousForPatient(Patient patient)
```

**Parameters:**
- `patient` (Patient) - The patient (must have valid ID).

**Returns:**
- `List<RendezVous>` - List of appointments for the patient, empty list if patient is null or not found.

---

### `findById`
Finds an appointment by ID.

**Signature:**
```java
public RendezVous findById(Long id)
```

**Parameters:**
- `id` (Long) - The appointment ID.

**Returns:**
- `RendezVous` - The appointment if found, `null` otherwise.

---

### `isValidStatusTransition`
Checks if a status transition is valid.

**Signature:**
```java
public boolean isValidStatusTransition(RendezVousStatus currentStatus, RendezVousStatus newStatus)
```

**Parameters:**
- `currentStatus` (RendezVousStatus) - Current status.
- `newStatus` (RendezVousStatus) - Desired new status.

**Returns:**
- `boolean` - `true` if transition is valid, `false` otherwise.

**Valid Transitions:**
- `PLANIFIE` → `CONFIRME` or `ANNULE`
- `CONFIRME` → `TERMINE` or `ANNULE`
- `TERMINE` / `ANNULE` → No transitions (final states)

---

### `canModifyAppointment`
Checks if the current user can modify a specific appointment.

**Signature:**
```java
public boolean canModifyAppointment(Long rdvId)
```

**Parameters:**
- `rdvId` (Long) - The appointment ID.

**Returns:**
- `boolean` - `true` if user can modify:
  - ADMIN/SEC: Always true
  - MEDECIN: True only if appointment belongs to them

---

### `canCreateAppointment`
Checks if the current user can create appointments.

**Signature:**
```java
public boolean canCreateAppointment()
```

**Returns:**
- `boolean` - `true` if user is SEC or ADMIN, `false` otherwise.

---

## MedecinService

Service for managing doctor (médecin) records. Admin-only operations.

### Constructor
```java
public MedecinService()
```

---

### `saveMedecin`
Saves or updates a doctor.

**Signature:**
```java
public Medecin saveMedecin(Medecin medecin) throws SecurityException
```

**Parameters:**
- `medecin` (Medecin) - The doctor to save/update.

**Returns:**
- `Medecin` - The saved doctor with generated ID if new.

**Throws:**
- `SecurityException` - If user is not authenticated or is not ADMIN.

**Permissions Required:**
- Roles: `ADMIN` only

---

### `updateMedecin`
Updates an existing doctor by ID.

**Signature:**
```java
public Medecin updateMedecin(Long medecinId, String nom, String prenom, 
                              SpecialiteMedecin specialite, String email, String telephone) throws SecurityException
```

**Parameters:**
- `medecinId` (Long) - The doctor ID.
- `nom` (String) - Last name.
- `prenom` (String) - First name.
- `specialite` (SpecialiteMedecin) - Specialty.
- `email` (String) - Email address.
- `telephone` (String) - Phone number.

**Returns:**
- `Medecin` - The updated doctor.

**Throws:**
- `SecurityException` - If user is not authenticated or is not ADMIN.
- `IllegalArgumentException` - If medecinId is null or doctor not found.

**Permissions Required:**
- Roles: `ADMIN` only

---

### `deleteMedecin`
Deletes a doctor if they have no associated appointments.

**Signature:**
```java
public void deleteMedecin(Long medecinId) throws IllegalStateException, SecurityException
```

**Parameters:**
- `medecinId` (Long) - The doctor ID.

**Throws:**
- `SecurityException` - If user is not authenticated or is not ADMIN.
- `IllegalArgumentException` - If doctor not found.
- `IllegalStateException` - If doctor has associated appointments.

**Permissions Required:**
- Roles: `ADMIN` only

---

### `findById`
Finds a doctor by ID.

**Signature:**
```java
public Medecin findById(Long id)
```

**Parameters:**
- `id` (Long) - The doctor ID.

**Returns:**
- `Medecin` - The doctor if found, `null` otherwise.

---

### `findAll`
Gets all doctors.

**Signature:**
```java
public List<Medecin> findAll()
```

**Returns:**
- `List<Medecin>` - All doctors.

---

### `findBySpecialite`
Finds doctors by specialty.

**Signature:**
```java
public List<Medecin> findBySpecialite(SpecialiteMedecin specialite)
```

**Parameters:**
- `specialite` (SpecialiteMedecin) - The specialty.

**Returns:**
- `List<Medecin>` - Doctors with the specified specialty.

---

### `searchByName`
Searches doctors by name or first name.

**Signature:**
```java
public List<Medecin> searchByName(String nom, String prenom)
```

**Parameters:**
- `nom` (String) - Last name (can be partial).
- `prenom` (String) - First name (can be partial).

**Returns:**
- `List<Medecin>` - Matching doctors.

---

### `canCreateDoctor`
Checks if the current user can create doctors.

**Signature:**
```java
public boolean canCreateDoctor()
```

**Returns:**
- `boolean` - `true` if user is ADMIN, `false` otherwise.

---

## FacturationService

Service for managing invoices (factures) and payment processing.

### Constructor
```java
public FacturationService()
```

---

### `creerFacture`
Creates a new invoice with invoice lines.

**Signature:**
```java
public Facture creerFacture(Long patientId, List<LigneFacture> lignes) throws IllegalArgumentException, SecurityException
```

**Parameters:**
- `patientId` (Long) - The patient ID to invoice.
- `lignes` (List<LigneFacture>) - List of invoice lines (at least one required).

**Returns:**
- `Facture` - The created invoice with calculated total.

**Throws:**
- `SecurityException` - If user is not authenticated or is not SEC/ADMIN.
- `IllegalArgumentException` - If:
  - Lignes list is null or empty
  - Any line has negative price or quantity
  - Patient not found

**Permissions Required:**
- Roles: `SEC`, `ADMIN`

**Business Rules:**
- Invoice must have at least one line
- Prices and quantities must be non-negative
- Total amount is calculated automatically

**Example:**
```java
LigneFacture ligne = new LigneFacture();
ligne.setDescription("Consultation");
ligne.setPrixUnitaire(new BigDecimal("50.00"));
ligne.setQuantite(1);

List<LigneFacture> lignes = List.of(ligne);
Facture facture = facturationService.creerFacture(patientId, lignes);
```

---

### `marquerCommePayee`
Marks an invoice as paid and records payment type.

**Signature:**
```java
public Facture marquerCommePayee(Long factureId, TypePaiement typePaiement) throws SecurityException
```

**Parameters:**
- `factureId` (Long) - The invoice ID.
- `typePaiement` (TypePaiement) - Payment type (e.g., CASH, CARD, CHECK).

**Returns:**
- `Facture` - The updated invoice.

**Throws:**
- `SecurityException` - If user is not authenticated or is not SEC/ADMIN.
- `IllegalArgumentException` - If factureId is null or invoice not found.
- `IllegalStateException` - If invoice is already marked as paid.

**Permissions Required:**
- Roles: `SEC`, `ADMIN`

---

### `canCreateInvoice`
Checks if the current user can create invoices.

**Signature:**
```java
public boolean canCreateInvoice()
```

**Returns:**
- `boolean` - `true` if user is SEC or ADMIN, `false` otherwise.

---

### `findById`
Finds an invoice by ID.

**Signature:**
```java
public Facture findById(Long id)
```

**Parameters:**
- `id` (Long) - The invoice ID.

**Returns:**
- `Facture` - The invoice if found, `null` otherwise.

---

### `getFacturesByPatient`
Gets all invoices for a specific patient.

**Signature:**
```java
public List<Facture> getFacturesByPatient(Patient patient)
```

**Parameters:**
- `patient` (Patient) - The patient.

**Returns:**
- `List<Facture>` - Invoices for the patient.

---

### `getUnpaidFactures`
Gets all unpaid invoices with details loaded.

**Signature:**
```java
public List<Facture> getUnpaidFactures()
```

**Returns:**
- `List<Facture>` - List of unpaid invoices (eagerly loaded with patient and lines).

---

### `getAllFactures`
Gets all invoices with details loaded.

**Signature:**
```java
public List<Facture> getAllFactures()
```

**Returns:**
- `List<Facture>` - All invoices (eagerly loaded with patient and lines).

---

## ConsultationService

Service for managing medical consultations created from completed appointments.

### Constructor
```java
public ConsultationService()
```

---

### `createConsultationFromRendezVous`
Creates a consultation when an appointment is completed.

**Signature:**
```java
public Consultation createConsultationFromRendezVous(RendezVous rdv) throws IllegalStateException
```

**Parameters:**
- `rdv` (RendezVous) - The completed appointment (must have status TERMINE).

**Returns:**
- `Consultation` - The created consultation linked to the appointment and patient's medical file.

**Throws:**
- `IllegalStateException` - If appointment status is not TERMINE.
- `IllegalArgumentException` - If appointment has no valid patient or medical file not found.

**Note:** This method is typically called automatically by `RendezVousService.terminerRendezVous()`.

**Side Effects:**
- Links consultation to patient's medical file (DossierMedical).
- Links consultation to the appointment (one-to-one relationship).

---

### `updateConsultationNotes`
Updates medical notes for a consultation after examination.

**Signature:**
```java
public Consultation updateConsultationNotes(Long consultationId, String observations, String diagnostic, String prescriptions)
```

**Parameters:**
- `consultationId` (Long) - The consultation ID.
- `observations` (String) - Examination observations.
- `diagnostic` (String) - Diagnosis.
- `prescriptions` (String) - Prescriptions.

**Returns:**
- `Consultation` - The updated consultation.

**Throws:**
- `IllegalArgumentException` - If consultation not found.

---

### `getConsultationsByDossier`
Gets all consultations for a medical file.

**Signature:**
```java
public List<Consultation> getConsultationsByDossier(Long dossierId)
```

**Parameters:**
- `dossierId` (Long) - The medical file ID.

**Returns:**
- `List<Consultation>` - List of consultations for the medical file, empty list if dossier not found.

---

### `findById`
Finds a consultation by ID.

**Signature:**
```java
public Consultation findById(Long id)
```

**Parameters:**
- `id` (Long) - The consultation ID.

**Returns:**
- `Consultation` - The consultation if found, `null` otherwise.

---

## AuthService

Service for user authentication and registration.

### Constructor
```java
public AuthService()
```

---

### `authenticate`
Authenticates a user with username and password.

**Signature:**
```java
public User authenticate(String username, String password)
```

**Parameters:**
- `username` (String) - The username.
- `password` (String) - The plain text password.

**Returns:**
- `User` - The authenticated user object.

**Throws:**
- `IllegalArgumentException` - If username not found or password incorrect.

**Security:**
- Uses BCrypt for password hashing and verification.

**Example:**
```java
try {
    User user = authService.authenticate("john.doe", "password123");
    // User is authenticated, set session
} catch (IllegalArgumentException e) {
    // Authentication failed
}
```

---

### `registerMedecin`
Registers a new doctor user account.

**Signature:**
```java
public void registerMedecin(String username, String password, Medecin medecin)
```

**Parameters:**
- `username` (String) - The username (must be unique).
- `password` (String) - The plain text password (will be hashed).
- `medecin` (Medecin) - The doctor profile to link.

**Throws:**
- `IllegalArgumentException` - If username already exists.

**Security:**
- Password is automatically hashed using BCrypt.

---

### `registerAdmin`
Registers a new admin user account (utility method for initialization).

**Signature:**
```java
public void registerAdmin(String username, String password)
```

**Parameters:**
- `username` (String) - The username (must be unique).
- `password` (String) - The plain text password (will be hashed).

**Note:** If username already exists, method returns silently (idempotent).

---

### `registerSecretary`
Registers a new secretary user account.

**Signature:**
```java
public void registerSecretary(String username, String password)
```

**Parameters:**
- `username` (String) - The username (must be unique).
- `password` (String) - The plain text password (will be hashed).

**Throws:**
- `IllegalArgumentException` - If username already exists.

**Security:**
- Password is automatically hashed using BCrypt.

---

## UserService

Service for managing user accounts and profiles.

### Constructor
```java
public UserService()
```

---

### `updatePassword`
Updates a user's password with old password validation.

**Signature:**
```java
public void updatePassword(Long userId, String oldPassword, String newPassword)
```

**Parameters:**
- `userId` (Long) - The user ID.
- `oldPassword` (String) - Current password (for verification).
- `newPassword` (String) - New password (must be at least 4 characters).

**Throws:**
- `IllegalArgumentException` - If:
  - User not found
  - Old password is incorrect
  - New password is less than 4 characters

**Security:**
- New password is automatically hashed using BCrypt.

---

### `findAll`
Gets all users.

**Signature:**
```java
public List<User> findAll()
```

**Returns:**
- `List<User>` - All users in the system.

---

### `updateUser`
Updates user information (excluding password).

**Signature:**
```java
public User updateUser(User updatedUser)
```

**Parameters:**
- `updatedUser` (User) - User object with updated fields (must have valid ID).

**Returns:**
- `User` - The updated user.

**Throws:**
- `IllegalArgumentException` - If user is null, has no ID, or not found.

**Note:** Use `updatePassword()` to change passwords.

---

### `deleteUser`
Deletes a user account.

**Signature:**
```java
public void deleteUser(Long userId)
```

**Parameters:**
- `userId` (Long) - The user ID.

**Throws:**
- `IllegalArgumentException` - If user not found.

---

### `createUser`
Creates a new user account.

**Signature:**
```java
public User createUser(User user)
```

**Parameters:**
- `user` (User) - The user object to create.

**Returns:**
- `User` - The created user with generated ID.

**Throws:**
- `IllegalArgumentException` - If user is null or username already exists.

**Note:** Password should be plain text (will be hashed if not already hashed).

---

### `findByUsername`
Finds a user by username.

**Signature:**
```java
public User findByUsername(String username)
```

**Parameters:**
- `username` (String) - The username.

**Returns:**
- `User` - The user if found, `null` otherwise.

---

### `findById`
Finds a user by ID.

**Signature:**
```java
public User findById(Long id)
```

**Parameters:**
- `id` (Long) - The user ID.

**Returns:**
- `User` - The user if found, `null` otherwise.

---

### `createUserWithPassword`
Creates a new user with a plain password (will be hashed).

**Signature:**
```java
public User createUserWithPassword(String username, String plainPassword, Role role)
```

**Parameters:**
- `username` (String) - The username (must be unique).
- `plainPassword` (String) - The plain text password (will be hashed).
- `role` (Role) - The user role (ADMIN, MEDECIN, SEC).

**Returns:**
- `User` - The created user.

**Throws:**
- `IllegalArgumentException` - If username already exists.

**Security:**
- Password is automatically hashed using BCrypt.

---

## DashboardService

Service for providing role-specific dashboard statistics.

### Constructor
```java
public DashboardService()
```

---

### `getDashboardStats`
Gets dashboard statistics based on the current user's role.

**Signature:**
```java
public DashboardStats getDashboardStats()
```

**Returns:**
- `DashboardStats` - Statistics object with fields based on user role:
  - **ADMIN**: Total patients, today's appointments, monthly revenue, active doctors, unpaid invoices
  - **MEDECIN**: Today's appointments, unique patients today, associated doctor
  - **SEC**: Today's appointments, unpaid invoices count

**Throws:**
- `IllegalStateException` - If user is not authenticated or has unknown role.

**DashboardStats Fields:**
```java
public class DashboardStats {
    private Integer totalPatients;          // ADMIN only
    private Integer todayAppointments;      // All roles
    private BigDecimal monthlyRevenue;      // ADMIN only
    private Integer activeDoctors;          // ADMIN only
    private Integer unpaidInvoices;         // ADMIN, SEC
    private Integer patientsTodayForDoctor; // MEDECIN only
    private Medecin medecin;                // MEDECIN only
}
```

**Example:**
```java
DashboardService dashboardService = new DashboardService();
DashboardService.DashboardStats stats = dashboardService.getDashboardStats();

if (stats.getTotalPatients() != null) {
    System.out.println("Total patients: " + stats.getTotalPatients());
}
System.out.println("Today's appointments: " + stats.getTodayAppointments());
```

---

## Common Patterns and Notes

### Authentication
All service methods that modify data require the user to be authenticated via `UserSession`. Methods throw `SecurityException` if not authenticated.

### Role-Based Access Control
- **ADMIN**: Full access to all operations
- **SEC (Secretary)**: Can create/modify patients, appointments, and invoices
- **MEDECIN (Doctor)**: Read-only access to their own appointments and patients

### Exception Handling
Services throw specific exceptions:
- `SecurityException`: Permission/authentication issues
- `IllegalArgumentException`: Invalid input parameters
- `IllegalStateException`: Business rule violations (e.g., invalid state transitions)

### Data Filtering
Many `findAll()` methods automatically filter results based on the current user's role. Use `findAllForCurrentUser()` for explicit role-based filtering.

### Entity Relationships
- Patients automatically get a `DossierMedical` when created
- Completing an appointment (`TERMINE`) automatically creates a `Consultation`
- Invoices are linked to patients and can have multiple `LigneFacture`

### Status Transitions
Appointment status transitions are strictly validated:
- `PLANIFIE` → `CONFIRME` or `ANNULE`
- `CONFIRME` → `TERMINE` or `ANNULE`
- `TERMINE` and `ANNULE` are final states

---

## Quick Reference: Permissions Matrix

| Operation | ADMIN | SEC | MEDECIN |
|-----------|-------|-----|---------|
| Create Patient | ✅ | ✅ | ❌ |
| Edit Patient | ✅ | ✅ | ❌ |
| Delete Patient | ✅ | ❌ | ❌ |
| View Patients | All | All | Own only |
| Create Appointment | ✅ | ✅ | ❌ |
| Modify Appointment | All | All | Own only |
| View Appointments | All | All | Own only |
| Create Doctor | ✅ | ❌ | ❌ |
| Modify Doctor | ✅ | ❌ | ❌ |
| Delete Doctor | ✅ | ❌ | ❌ |
| Create Invoice | ✅ | ✅ | ❌ |
| Mark Invoice Paid | ✅ | ✅ | ❌ |
| View Invoices | All | All | ❌ |

---

*Documentation generated for MediClinic Application - Backend Services API*
