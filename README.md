# 🏥 Hospital Management System (HMS)

A full-stack desktop Hospital Management System built with **Java Swing** and **MySQL**, developed as a Database Systems course project at UET Lahore.

---

## 📌 Overview

HMS is a multi-module desktop application that covers the core operations of a hospital — from patient registration and appointment scheduling to billing, pharmacy, lab management, and PDF reporting. The project focuses on practical application of relational database concepts including transactions, stored procedures, triggers, views, and constraints.

---

## 🛠️ Tech Stack

| Layer | Technology |
|-------|-----------|
| Language | Java (JDK 17+) |
| UI Framework | Java Swing |
| Database | MySQL 8.0+ |
| DB Connectivity | JDBC (MySQL Connector/J 9.x) |
| PDF Reports | iTextPDF 5.5.13 |
| Date Picker | JCalendar 1.4 |
| IDE | IntelliJ IDEA |

---

## ✨ Features

### Modules
- **Patients** — Register, update, search, and manage patient records
- **Doctors** — Doctor profiles with department assignment and availability tracking
- **Appointments** — Schedule and manage appointments with date/time selection
- **Billing** — Create bills, add itemized charges, track payments, and discharge patients
- **Lab** — Order lab tests, enter results, and generate reports
- **Pharmacy** — Manage medicine catalogue, stock entries, and low-stock alerts
- **Staff** — HR management with role-based staff records
- **Reports** — 7 report tabs with CSV export and PDF generation

### Database Highlights
- **19 tables** covering all hospital entities
- **5 views** for admitted patients, today's appointments, medicine stock, pending bills, and doctor workload
- **3 stored procedures** — admit patient (with room locking), discharge patient (auto-generates bill), and full patient medical history
- **2 triggers** — auto-update bill totals on item insert, sync room status on patient update
- **15+ constraints** — CHECK, UNIQUE, FOREIGN KEY with appropriate ON DELETE actions
- **7 indexes** on frequently queried columns for performance
- **Dual logging** — errors written to both `hms_errors.log` file and `error_log` DB table

---

## 🗄️ Database Schema

```
user_account → doctor / staff (role-based login)
department → ward → room → patient
patient → appointment → prescription → prescription_medicine → medicine
patient → lab_test → lab_report
patient → bill → bill_item
medicine → medicine_stock → supplier
patient → insurance
```

Key design decisions:
- `FOR UPDATE` row locking in `sp_admit_patient` to prevent concurrent room double-booking
- Circular FK between `department` and `doctor` resolved via `ALTER TABLE` after both tables exist
- `ON DELETE CASCADE` for patient-owned data, `RESTRICT` for shared references, `SET NULL` for optional links

---

## ⚙️ Setup & Installation

### Prerequisites
- Java JDK 17 or higher
- MySQL 8.0+
- IntelliJ IDEA (recommended)

### Required JARs (place in `/lib` folder)
- `mysql-connector-j-9.x.x.jar` — [Download](https://dev.mysql.com/downloads/connector/j/)
- `itextpdf-5.5.13.jar` — [Download](https://github.com/itext/itextpdf/releases)
- `jcalendar-1.4.jar` — [Download](https://toedter.com/jcalendar/)

### Steps

1. **Clone the repository**
   ```bash
   git clone https://github.com/abdullahthexpert/HospitalManagementSystem.git
   ```

2. **Import schema into MySQL**
   ```bash
   mysql -u root -p < schema.sql
   ```
   Or open `schema.sql` in MySQL Workbench and execute it.

3. **Update DB credentials** in `src/hms/software/DatabaseConnection.java`:
   ```java
   private static final String URL      = "jdbc:mysql://localhost:3306/hms_db";
   private static final String USER     = "root";
   private static final String PASSWORD = "your_password_here";
   ```

4. **Add JARs to project** in IntelliJ: `File → Project Structure → Libraries → + → Java` and select all three JARs.

5. **Run** `src/hms/ui/LoginForm.java`

### Default Login
| Username | Password | Role |
|----------|----------|------|
| admin | admin123 | Admin |

---

## 📁 Project Structure

```
HospitalManagementSystem/
├── src/
│   ├── Main.java
│   ├── hms/
│   │   ├── domain/          # Entity classes (Patient, Doctor, Bill, etc.)
│   │   ├── software/        # Core utilities (DB, Logger, Validator, Reports, Session)
│   │   └── ui/              # Swing UI forms (8 modules + Login + Dashboard)
├── schema.sql               # Full DB schema with seed data
└── README.md
```

---

## ⚠️ Known Limitations

This is a university course project. It is not production-ready:

- Passwords are stored as plain text (no hashing)
- Single shared DB connection (no connection pooling)
- No role-based UI restrictions (all roles see all modules)
- DB credentials hardcoded in source
- No PreparedStatement closing (potential resource leak)

---

## 👥 Contributors

- **Muhammad Ali** (2025-CH-67) — UET Lahore
- **Abdullah** (2025-CH-43) — UET Lahore

---

## 📄 License

This project was developed for academic purposes. No license applied.
