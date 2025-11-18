# 🚀 MediClinic - Quick Start Guide

## Prerequisites Checklist

- [ ] Java 17 installed (`java -version`)
- [ ] Maven 3.6+ installed (`mvn -version`)
- [ ] MySQL 8.0+ installed and running
- [ ] IntelliJ IDEA (or any Java IDE)

---

## ⚡ Quick Setup (5 Minutes)

### Step 1: Configure Database

1. Open `src/main/resources/hibernate.cfg.xml`
2. Update MySQL password:
   ```xml
   <property name="connection.password">YOUR_MYSQL_PASSWORD</property>
   ```
3. Ensure MySQL is running:
   ```bash
   # Windows (Command Prompt as Admin)
   net start MySQL80
   
   # Or check in Services
   services.msc → Find MySQL80 → Start
   ```

### Step 2: Open in IntelliJ

1. **File** → **Open** → Select `MediClinic` folder
2. Wait for Maven auto-import (bottom right notification)
3. If not auto-imported: Right-click `pom.xml` → **Maven** → **Reload Project**

### Step 3: Run Application

**Option A: Run Main Class**
- Right-click `MainApp.java`
- **Run 'MainApp.main()'**
- Or press **Shift + F10**

**Option B: Maven Command**
- Open terminal in IntelliJ (Alt + F12)
- Run: `mvn javafx:run`

**Option C: Maven Run Configuration**
1. **Run** → **Edit Configurations**
2. Click **+** → **Maven**
3. Name: `Run MediClinic`
4. Command: `javafx:run`
5. Click **OK** → Run

---

## 🎯 First Run

1. **Application starts** → Main window appears
2. **Database is created automatically** (if `createDatabaseIfNotExist=true`)
3. **Tables are created automatically** (Hibernate `hbm2ddl.auto=update`)

---

## 🧪 Test the Application

### Test Patient Management:

1. Click **"👥 Patients"** in the menu
2. Click **"+ Nouveau Patient"**
3. Fill in the form:
   - Nom: `Dupont`
   - Prénom: `Jean`
   - Email: `jean.dupont@email.com`
   - Téléphone: `+33123456789`
   - Date de naissance: `1990-01-15`
4. Click **"Enregistrer"**
5. Patient should appear in the table

### Test Appointment Scheduling:

1. Click **"📅 Agenda"** in the menu
2. (Functionality needs to be fully implemented)

---

## 🔧 Troubleshooting

### ❌ "Cannot resolve symbol" errors
**Fix:**
- Right-click project → **Maven** → **Reload Project**
- **File** → **Invalidate Caches / Restart**

### ❌ "Hibernate SessionFactory initialization failed"
**Fix:**
- Check MySQL is running
- Verify credentials in `hibernate.cfg.xml`
- Test connection: `mysql -u root -p`

### ❌ "Port 3306 already in use"
**Fix:**
- Another MySQL instance is running
- Stop it or change port in `hibernate.cfg.xml`

### ❌ "Access denied for user 'root'"
**Fix:**
- Wrong password in `hibernate.cfg.xml`
- Update password or create new MySQL user

### ❌ JavaFX runtime errors
**Fix:**
- Ensure JavaFX dependencies are downloaded (check Maven)
- Use `mvn javafx:run` instead of running MainApp directly

---

## 📋 Application Features

### ✅ Fully Working:
- Patient CRUD operations
- Medical record management
- Database persistence
- Modern UI navigation

### ⚠️ Partially Implemented:
- Agenda/Appointment scheduling (UI ready)
- Doctor management (UI ready)
- Billing system (UI ready)
- Dashboard statistics (UI ready, needs data)

---

## 🎨 Application Structure

```
Main Window
├── 📊 Tableau de Bord (Dashboard)
├── 👥 Patients (Fully Functional)
├── 📅 Agenda (UI Ready)
├── 👨‍⚕️ Médecins (UI Ready)
└── 💰 Facturation (UI Ready)
```

---

## 💡 Tips

1. **First Time Setup:**
   - Database will be created automatically
   - Tables will be created automatically
   - No manual SQL scripts needed

2. **Development:**
   - Use `hbm2ddl.auto=update` (already set)
   - Changes to models will update database schema
   - Be careful in production!

3. **Debugging:**
   - Check IntelliJ console for errors
   - Hibernate shows SQL queries (if `show_sql=true`)
   - Check MySQL logs if connection fails

---

## 📞 Need Help?

1. Check `PROJECT_ANALYSIS.md` for detailed analysis
2. Review error messages in console
3. Verify MySQL is running and accessible
4. Check database credentials

---

## ✅ Success Indicators

You'll know it's working when:
- ✅ Application window opens
- ✅ No errors in console
- ✅ Can navigate between sections
- ✅ Can add a patient successfully
- ✅ Patient appears in the table

---

**Happy Coding! 🎉**

