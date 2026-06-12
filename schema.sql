-- ============================================================
--  HMS - Hospital Management System
--  Database Schema
--  MySQL 8.0+
-- ============================================================

CREATE DATABASE IF NOT EXISTS hms_db;
USE hms_db;

-- ============================================================
-- 1. USER_ACCOUNT
-- ============================================================
CREATE TABLE user_account (
    user_id       INT AUTO_INCREMENT PRIMARY KEY,
    username      VARCHAR(50)  NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role          ENUM('admin', 'doctor', 'staff', 'receptionist') NOT NULL,
    is_active     BOOLEAN      NOT NULL DEFAULT TRUE,
    last_login    DATETIME,
    created_at    DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_username_len CHECK (CHAR_LENGTH(username) >= 3)
);

-- ============================================================
-- 2. DEPARTMENT
-- ============================================================
CREATE TABLE department (
    dept_id        INT AUTO_INCREMENT PRIMARY KEY,
    dept_name      VARCHAR(100) NOT NULL UNIQUE,
    description    VARCHAR(255),
    head_doctor_id INT,         -- FK added after doctor table
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 3. DOCTOR
-- ============================================================
CREATE TABLE doctor (
    doctor_id      INT AUTO_INCREMENT PRIMARY KEY,
    full_name      VARCHAR(100) NOT NULL,
    specialization VARCHAR(100) NOT NULL,
    phone          VARCHAR(15)  NOT NULL UNIQUE,
    email          VARCHAR(100) UNIQUE,
    dept_id        INT          NOT NULL,
    user_id        INT          UNIQUE,
    is_available   BOOLEAN      NOT NULL DEFAULT TRUE,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_doctor_dept FOREIGN KEY (dept_id)
        REFERENCES department(dept_id) ON DELETE RESTRICT,
    CONSTRAINT fk_doctor_user FOREIGN KEY (user_id)
        REFERENCES user_account(user_id) ON DELETE SET NULL,
    CONSTRAINT chk_doctor_phone CHECK (CHAR_LENGTH(phone) >= 10)
);

-- Now add the FK on department.head_doctor_id
ALTER TABLE department
    ADD CONSTRAINT fk_dept_head FOREIGN KEY (head_doctor_id)
        REFERENCES doctor(doctor_id) ON DELETE SET NULL;

-- ============================================================
-- 4. STAFF
-- ============================================================
CREATE TABLE staff (
    staff_id   INT AUTO_INCREMENT PRIMARY KEY,
    full_name  VARCHAR(100) NOT NULL,
    role       ENUM('nurse','lab_technician','receptionist','pharmacist','cleaner') NOT NULL,
    phone      VARCHAR(15)  NOT NULL UNIQUE,
    email      VARCHAR(100) UNIQUE,
    dept_id    INT          NOT NULL,
    user_id    INT          UNIQUE,
    hire_date  DATE         NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_staff_dept FOREIGN KEY (dept_id)
        REFERENCES department(dept_id) ON DELETE RESTRICT,
    CONSTRAINT fk_staff_user FOREIGN KEY (user_id)
        REFERENCES user_account(user_id) ON DELETE SET NULL
);

-- ============================================================
-- 5. WARD
-- ============================================================
CREATE TABLE ward (
    ward_id    INT AUTO_INCREMENT PRIMARY KEY,
    ward_name  VARCHAR(100) NOT NULL UNIQUE,
    ward_type  ENUM('general','icu','maternity','pediatric','surgical','emergency') NOT NULL,
    dept_id    INT          NOT NULL,
    capacity   INT          NOT NULL,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ward_dept FOREIGN KEY (dept_id)
        REFERENCES department(dept_id) ON DELETE RESTRICT,
    CONSTRAINT chk_ward_capacity CHECK (capacity > 0)
);

-- ============================================================
-- 6. ROOM
-- ============================================================
CREATE TABLE room (
    room_id     INT AUTO_INCREMENT PRIMARY KEY,
    ward_id     INT         NOT NULL,
    room_number VARCHAR(10) NOT NULL UNIQUE,
    room_type   ENUM('single','double','shared','icu') NOT NULL,
    status      ENUM('available','occupied','maintenance') NOT NULL DEFAULT 'available',
    daily_rate  DECIMAL(8,2) NOT NULL,
    created_at  DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_room_ward FOREIGN KEY (ward_id)
        REFERENCES ward(ward_id) ON DELETE RESTRICT,
    CONSTRAINT chk_room_rate CHECK (daily_rate >= 0)
);

-- ============================================================
-- 7. PATIENT
-- ============================================================
CREATE TABLE patient (
    patient_id     INT AUTO_INCREMENT PRIMARY KEY,
    full_name      VARCHAR(100) NOT NULL,
    dob            DATE         NOT NULL,
    gender         ENUM('male','female','other') NOT NULL,
    blood_group    ENUM('A+','A-','B+','B-','AB+','AB-','O+','O-'),
    phone          VARCHAR(15)  NOT NULL UNIQUE,
    email          VARCHAR(100) UNIQUE,
    address        TEXT,
    emergency_contact VARCHAR(15),
    admission_date DATE,
    discharge_date DATE,
    room_id        INT,
    status         ENUM('outpatient','admitted','discharged') NOT NULL DEFAULT 'outpatient',
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_patient_room FOREIGN KEY (room_id)
        REFERENCES room(room_id) ON DELETE SET NULL,
    CONSTRAINT chk_patient_phone CHECK (CHAR_LENGTH(phone) >= 10),
    CONSTRAINT chk_discharge_after_admission
        CHECK (discharge_date IS NULL OR discharge_date >= admission_date)
);

-- ============================================================
-- 8. APPOINTMENT
-- ============================================================
CREATE TABLE appointment (
    appt_id        INT AUTO_INCREMENT PRIMARY KEY,
    patient_id     INT      NOT NULL,
    doctor_id      INT      NOT NULL,
    appt_datetime  DATETIME NOT NULL,
    status         ENUM('scheduled','completed','cancelled','no_show') NOT NULL DEFAULT 'scheduled',
    reason         VARCHAR(255),
    notes          TEXT,
    created_at     DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_appt_patient FOREIGN KEY (patient_id)
        REFERENCES patient(patient_id) ON DELETE CASCADE,
    CONSTRAINT fk_appt_doctor FOREIGN KEY (doctor_id)
        REFERENCES doctor(doctor_id) ON DELETE RESTRICT
);

-- ============================================================
-- 9. PRESCRIPTION
-- ============================================================
CREATE TABLE prescription (
    presc_id     INT AUTO_INCREMENT PRIMARY KEY,
    appt_id      INT  NOT NULL UNIQUE,
    issue_date   DATE NOT NULL,
    diagnosis    TEXT NOT NULL,
    instructions TEXT,
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_presc_appt FOREIGN KEY (appt_id)
        REFERENCES appointment(appt_id) ON DELETE CASCADE
);

-- ============================================================
-- 10. MEDICINE
-- ============================================================
CREATE TABLE medicine (
    medicine_id  INT AUTO_INCREMENT PRIMARY KEY,
    name         VARCHAR(150) NOT NULL UNIQUE,
    type         ENUM('tablet','syrup','injection','capsule','ointment','drops') NOT NULL,
    manufacturer VARCHAR(100),
    unit_price   DECIMAL(8,2) NOT NULL,
    requires_presc BOOLEAN    NOT NULL DEFAULT TRUE,
    created_at   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT chk_medicine_price CHECK (unit_price >= 0)
);

-- ============================================================
-- 11. PRESCRIPTION_MEDICINE
-- ============================================================
CREATE TABLE prescription_medicine (
    id          INT AUTO_INCREMENT PRIMARY KEY,
    presc_id    INT         NOT NULL,
    medicine_id INT         NOT NULL,
    dosage      VARCHAR(50) NOT NULL,
    frequency   VARCHAR(50) NOT NULL,
    duration_days INT       NOT NULL,
    CONSTRAINT fk_pm_presc FOREIGN KEY (presc_id)
        REFERENCES prescription(presc_id) ON DELETE CASCADE,
    CONSTRAINT fk_pm_medicine FOREIGN KEY (medicine_id)
        REFERENCES medicine(medicine_id) ON DELETE RESTRICT,
    CONSTRAINT chk_duration CHECK (duration_days > 0),
    CONSTRAINT uq_presc_medicine UNIQUE (presc_id, medicine_id)
);

-- ============================================================
-- 12. SUPPLIER
-- ============================================================
CREATE TABLE supplier (
    supplier_id    INT AUTO_INCREMENT PRIMARY KEY,
    name           VARCHAR(100) NOT NULL UNIQUE,
    contact_person VARCHAR(100),
    phone          VARCHAR(15)  NOT NULL UNIQUE,
    email          VARCHAR(100) UNIQUE,
    address        TEXT,
    created_at     DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- ============================================================
-- 13. MEDICINE_STOCK
-- ============================================================
CREATE TABLE medicine_stock (
    stock_id      INT AUTO_INCREMENT PRIMARY KEY,
    medicine_id   INT  NOT NULL,
    supplier_id   INT  NOT NULL,
    quantity      INT  NOT NULL,
    expiry_date   DATE NOT NULL,
    received_date DATE NOT NULL DEFAULT (CURRENT_DATE),
    batch_no      VARCHAR(50),
    created_at    DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_stock_medicine FOREIGN KEY (medicine_id)
        REFERENCES medicine(medicine_id) ON DELETE RESTRICT,
    CONSTRAINT fk_stock_supplier FOREIGN KEY (supplier_id)
        REFERENCES supplier(supplier_id) ON DELETE RESTRICT,
    CONSTRAINT chk_stock_qty CHECK (quantity >= 0),
    CONSTRAINT chk_expiry CHECK (expiry_date > received_date)
);

-- ============================================================
-- 14. LAB_TEST
-- ============================================================
CREATE TABLE lab_test (
    test_id    INT AUTO_INCREMENT PRIMARY KEY,
    patient_id INT         NOT NULL,
    doctor_id  INT         NOT NULL,
    test_name  VARCHAR(150) NOT NULL,
    test_date  DATE         NOT NULL,
    status     ENUM('pending','in_progress','completed','cancelled') NOT NULL DEFAULT 'pending',
    test_fee   DECIMAL(8,2) NOT NULL DEFAULT 0,
    created_at DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_test_patient FOREIGN KEY (patient_id)
        REFERENCES patient(patient_id) ON DELETE CASCADE,
    CONSTRAINT fk_test_doctor FOREIGN KEY (doctor_id)
        REFERENCES doctor(doctor_id) ON DELETE RESTRICT,
    CONSTRAINT chk_test_fee CHECK (test_fee >= 0)
);

-- ============================================================
-- 15. LAB_REPORT
-- ============================================================
CREATE TABLE lab_report (
    report_id   INT AUTO_INCREMENT PRIMARY KEY,
    test_id     INT  NOT NULL UNIQUE,
    report_date DATE NOT NULL,
    result      TEXT NOT NULL,
    remarks     TEXT,
    reported_by INT,
    created_at  DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_report_test FOREIGN KEY (test_id)
        REFERENCES lab_test(test_id) ON DELETE CASCADE,
    CONSTRAINT fk_report_staff FOREIGN KEY (reported_by)
        REFERENCES staff(staff_id) ON DELETE SET NULL
);

-- ============================================================
-- 16. INSURANCE
-- ============================================================
CREATE TABLE insurance (
    ins_id          INT AUTO_INCREMENT PRIMARY KEY,
    patient_id      INT          NOT NULL UNIQUE,
    provider        VARCHAR(100) NOT NULL,
    policy_no       VARCHAR(50)  NOT NULL UNIQUE,
    coverage_amount DECIMAL(10,2) NOT NULL,
    expiry_date     DATE          NOT NULL,
    created_at      DATETIME      NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_ins_patient FOREIGN KEY (patient_id)
        REFERENCES patient(patient_id) ON DELETE CASCADE,
    CONSTRAINT chk_coverage CHECK (coverage_amount > 0)
);

-- ============================================================
-- 17. BILL
-- ============================================================
CREATE TABLE bill (
    bill_id      INT AUTO_INCREMENT PRIMARY KEY,
    patient_id   INT     NOT NULL,
    bill_date    DATE    NOT NULL,
    total_amount DECIMAL(10,2) NOT NULL DEFAULT 0,
    paid_amount  DECIMAL(10,2) NOT NULL DEFAULT 0,
    status       ENUM('pending','partial','paid','waived') NOT NULL DEFAULT 'pending',
    created_at   DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_bill_patient FOREIGN KEY (patient_id)
        REFERENCES patient(patient_id) ON DELETE RESTRICT,
    CONSTRAINT chk_paid_lte_total CHECK (paid_amount <= total_amount),
    CONSTRAINT chk_amounts_pos CHECK (total_amount >= 0 AND paid_amount >= 0)
);

-- ============================================================
-- 18. BILL_ITEM
-- ============================================================
CREATE TABLE bill_item (
    item_id     INT AUTO_INCREMENT PRIMARY KEY,
    bill_id     INT          NOT NULL,
    description VARCHAR(200) NOT NULL,
    quantity    INT          NOT NULL DEFAULT 1,
    unit_price  DECIMAL(8,2) NOT NULL,
    amount      DECIMAL(8,2) NOT NULL,
    item_type   ENUM('consultation','medicine','lab_test','room','procedure','other') NOT NULL,
    CONSTRAINT fk_billitem_bill FOREIGN KEY (bill_id)
        REFERENCES bill(bill_id) ON DELETE CASCADE,
    CONSTRAINT chk_item_qty CHECK (quantity > 0),
    CONSTRAINT chk_item_price CHECK (unit_price >= 0)
);

-- ============================================================
-- 19. ERROR_LOG  (for logging requirement)
-- ============================================================
CREATE TABLE error_log (
    log_id     INT AUTO_INCREMENT PRIMARY KEY,
    log_time   DATETIME     NOT NULL DEFAULT CURRENT_TIMESTAMP,
    level      ENUM('INFO','WARN','ERROR','FATAL') NOT NULL DEFAULT 'ERROR',
    module     VARCHAR(100),
    message    TEXT         NOT NULL,
    stack_trace TEXT,
    user_id    INT,
    CONSTRAINT fk_log_user FOREIGN KEY (user_id)
        REFERENCES user_account(user_id) ON DELETE SET NULL
);

-- ============================================================
-- INDEXES for performance
-- ============================================================
CREATE INDEX idx_appt_patient    ON appointment(patient_id);
CREATE INDEX idx_appt_doctor     ON appointment(doctor_id);
CREATE INDEX idx_appt_datetime   ON appointment(appt_datetime);
CREATE INDEX idx_patient_status  ON patient(status);
CREATE INDEX idx_lab_test_status ON lab_test(status);
CREATE INDEX idx_bill_status     ON bill(status);
CREATE INDEX idx_stock_expiry    ON medicine_stock(expiry_date);

-- ============================================================
-- VIEWS (5 required)
-- ============================================================

-- V1: Active admitted patients with room info
CREATE VIEW vw_admitted_patients AS
SELECT
    p.patient_id, p.full_name, p.gender, p.blood_group,
    p.phone, p.admission_date,
    r.room_number, r.room_type,
    w.ward_name, d.dept_name
FROM patient p
JOIN room r ON p.room_id = r.room_id
JOIN ward w ON r.ward_id = w.ward_id
JOIN department d ON w.dept_id = d.dept_id
WHERE p.status = 'admitted';

-- V2: Today's appointment schedule
CREATE VIEW vw_todays_appointments AS
SELECT
    a.appt_id, a.appt_datetime, a.status, a.reason,
    p.full_name AS patient_name, p.phone AS patient_phone,
    doc.full_name AS doctor_name, dep.dept_name
FROM appointment a
JOIN patient p   ON a.patient_id = p.patient_id
JOIN doctor doc  ON a.doctor_id  = doc.doctor_id
JOIN department dep ON doc.dept_id = dep.dept_id
WHERE DATE(a.appt_datetime) = CURRENT_DATE;

-- V3: Medicine stock with low-stock alert
CREATE VIEW vw_medicine_stock AS
SELECT
    m.medicine_id, m.name AS medicine_name, m.type,
    m.unit_price,
    SUM(ms.quantity) AS total_stock,
    MIN(ms.expiry_date) AS nearest_expiry,
    CASE WHEN SUM(ms.quantity) < 20 THEN 'LOW' ELSE 'OK' END AS stock_status
FROM medicine m
LEFT JOIN medicine_stock ms ON m.medicine_id = ms.medicine_id
GROUP BY m.medicine_id, m.name, m.type, m.unit_price;

-- V4: Pending bills summary
CREATE VIEW vw_pending_bills AS
SELECT
    b.bill_id, b.bill_date, b.total_amount, b.paid_amount,
    (b.total_amount - b.paid_amount) AS balance_due,
    b.status,
    p.full_name AS patient_name, p.phone
FROM bill b
JOIN patient p ON b.patient_id = p.patient_id
WHERE b.status IN ('pending','partial');

-- V5: Doctor workload summary
CREATE VIEW vw_doctor_workload AS
SELECT
    doc.doctor_id, doc.full_name, doc.specialization,
    dep.dept_name,
    COUNT(a.appt_id) AS total_appointments,
    SUM(CASE WHEN a.status = 'completed'  THEN 1 ELSE 0 END) AS completed,
    SUM(CASE WHEN a.status = 'scheduled'  THEN 1 ELSE 0 END) AS upcoming,
    SUM(CASE WHEN a.status = 'cancelled'  THEN 1 ELSE 0 END) AS cancelled
FROM doctor doc
JOIN department dep ON doc.dept_id = dep.dept_id
LEFT JOIN appointment a ON doc.doctor_id = a.doctor_id
GROUP BY doc.doctor_id, doc.full_name, doc.specialization, dep.dept_name;

-- ============================================================
-- STORED PROCEDURES (3 required)
-- ============================================================

DELIMITER $$

-- SP1: Admit a patient to a room
CREATE PROCEDURE sp_admit_patient(
    IN p_patient_id INT,
    IN p_room_id    INT,
    IN p_admission_date DATE
)
BEGIN
    DECLARE room_status VARCHAR(20);
    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        INSERT INTO error_log(level, module, message)
            VALUES('ERROR','sp_admit_patient', CONCAT('Failed admitting patient ', p_patient_id));
        RESIGNAL;
    END;

    START TRANSACTION;
        SELECT status INTO room_status FROM room WHERE room_id = p_room_id FOR UPDATE;
        IF room_status != 'available' THEN
            SIGNAL SQLSTATE '45000' SET MESSAGE_TEXT = 'Room is not available';
        END IF;
        UPDATE room    SET status = 'occupied'  WHERE room_id  = p_room_id;
        UPDATE patient SET status = 'admitted', room_id = p_room_id,
                           admission_date = p_admission_date
                       WHERE patient_id = p_patient_id;
    COMMIT;
END$$

-- SP2: Discharge a patient and generate final bill
CREATE PROCEDURE sp_discharge_patient(
    IN p_patient_id    INT,
    IN p_discharge_date DATE
)
BEGIN
    DECLARE v_room_id     INT;
    DECLARE v_admit_date  DATE;
    DECLARE v_days        INT;
    DECLARE v_daily_rate  DECIMAL(8,2);
    DECLARE v_bill_id     INT;

    DECLARE EXIT HANDLER FOR SQLEXCEPTION
    BEGIN
        ROLLBACK;
        INSERT INTO error_log(level, module, message)
            VALUES('ERROR','sp_discharge_patient', CONCAT('Failed discharging patient ', p_patient_id));
        RESIGNAL;
    END;

    START TRANSACTION;
        SELECT room_id, admission_date INTO v_room_id, v_admit_date
        FROM patient WHERE patient_id = p_patient_id FOR UPDATE;

        SELECT daily_rate INTO v_daily_rate FROM room WHERE room_id = v_room_id;
        SET v_days = DATEDIFF(p_discharge_date, v_admit_date);
        IF v_days < 1 THEN SET v_days = 1; END IF;

        -- Create bill
        INSERT INTO bill(patient_id, bill_date, total_amount, status)
            VALUES(p_patient_id, p_discharge_date, v_days * v_daily_rate, 'pending');
        SET v_bill_id = LAST_INSERT_ID();

        -- Add room charges as bill item
        INSERT INTO bill_item(bill_id, description, quantity, unit_price, amount, item_type)
            VALUES(v_bill_id, CONCAT('Room charges (', v_days, ' days)'),
                   v_days, v_daily_rate, v_days * v_daily_rate, 'room');

        -- Free the room, update patient
        UPDATE room    SET status = 'available' WHERE room_id = v_room_id;
        UPDATE patient SET status = 'discharged', room_id = NULL,
                           discharge_date = p_discharge_date
                       WHERE patient_id = p_patient_id;
    COMMIT;
END$$

-- SP3: Get patient full medical history
CREATE PROCEDURE sp_patient_history(IN p_patient_id INT)
BEGIN
    -- Basic info
    SELECT patient_id, full_name, dob, gender, blood_group, phone, status
    FROM patient WHERE patient_id = p_patient_id;

    -- Appointments
    SELECT a.appt_id, a.appt_datetime, a.status, a.reason,
           doc.full_name AS doctor_name
    FROM appointment a
    JOIN doctor doc ON a.doctor_id = doc.doctor_id
    WHERE a.patient_id = p_patient_id
    ORDER BY a.appt_datetime DESC;

    -- Prescriptions
    SELECT p.presc_id, p.issue_date, p.diagnosis,
           m.name AS medicine, pm.dosage, pm.frequency, pm.duration_days
    FROM prescription p
    JOIN appointment a        ON p.appt_id     = a.appt_id
    JOIN prescription_medicine pm ON p.presc_id = pm.presc_id
    JOIN medicine m           ON pm.medicine_id = m.medicine_id
    WHERE a.patient_id = p_patient_id
    ORDER BY p.issue_date DESC;

    -- Lab tests
    SELECT lt.test_name, lt.test_date, lt.status,
           lr.result, lr.remarks
    FROM lab_test lt
    LEFT JOIN lab_report lr ON lt.test_id = lr.test_id
    WHERE lt.patient_id = p_patient_id
    ORDER BY lt.test_date DESC;
END$$

DELIMITER ;

-- ============================================================
-- TRIGGERS (2 required)
-- ============================================================

DELIMITER $$

-- T1: Auto-update bill total when a bill_item is inserted
CREATE TRIGGER trg_update_bill_total
AFTER INSERT ON bill_item
FOR EACH ROW
BEGIN
    UPDATE bill
    SET total_amount = (
        SELECT COALESCE(SUM(amount), 0)
        FROM bill_item
        WHERE bill_id = NEW.bill_id
    )
    WHERE bill_id = NEW.bill_id;
END$$

-- T2: Mark room as occupied / available when patient status changes
CREATE TRIGGER trg_room_status_on_patient_update
AFTER UPDATE ON patient
FOR EACH ROW
BEGIN
    -- Patient just discharged: free the old room
    IF OLD.status = 'admitted' AND NEW.status = 'discharged' AND OLD.room_id IS NOT NULL THEN
        UPDATE room SET status = 'available' WHERE room_id = OLD.room_id;
    END IF;
    -- Patient admitted to new room (direct update path)
    IF NEW.status = 'admitted' AND NEW.room_id IS NOT NULL AND NEW.room_id != COALESCE(OLD.room_id, -1) THEN
        UPDATE room SET status = 'occupied' WHERE room_id = NEW.room_id;
    END IF;
END$$

DELIMITER ;

-- ============================================================
-- CONSTRAINT SUMMARY (10+ across all tables — documented)
-- 1.  chk_username_len       - username >= 3 chars
-- 2.  chk_ward_capacity      - capacity > 0
-- 3.  chk_room_rate          - daily_rate >= 0
-- 4.  chk_patient_phone      - phone >= 10 chars
-- 5.  chk_discharge_after_admission - discharge >= admission
-- 6.  chk_duration           - duration_days > 0
-- 7.  chk_stock_qty          - quantity >= 0
-- 8.  chk_expiry             - expiry > received
-- 9.  chk_test_fee           - test_fee >= 0
-- 10. chk_coverage           - coverage_amount > 0
-- 11. chk_paid_lte_total     - paid <= total
-- 12. chk_amounts_pos        - total >= 0, paid >= 0
-- 13. chk_item_qty           - quantity > 0
-- 14. chk_item_price         - unit_price >= 0
-- 15. chk_medicine_price     - unit_price >= 0
-- ============================================================

-- ============================================================
-- SEED DATA (minimal — for first run / demo / login)
-- ============================================================

-- Default admin login: username = admin, password = admin123
-- NOTE: password is stored in PLAIN TEXT to match LoginForm's current
-- comparison (password_hash = ?). Replace with a hashed value if/when
-- the login flow is updated to use hashing.
INSERT INTO user_account (username, password_hash, role, is_active) VALUES
    ('admin',  'admin123',  'admin',  TRUE),
    ('reception1', 'recep123', 'receptionist', TRUE);

-- A starter department so DoctorForm / StaffForm aren't empty
INSERT INTO department (dept_name, description) VALUES
    ('General Medicine', 'General outpatient and inpatient care'),
    ('Cardiology', 'Heart and cardiovascular care');

-- A starter ward and room so PatientForm admit flow has somewhere to put patients
INSERT INTO ward (ward_name, ward_type, dept_id, capacity) VALUES
    ('General Ward A', 'general', 1, 20);

INSERT INTO room (ward_id, room_number, room_type, status, daily_rate) VALUES
    (1, 'G-101', 'single', 'available', 1500.00),
    (1, 'G-102', 'double', 'available', 1000.00);

-- A starter supplier so PharmacyForm stock entry isn't empty
INSERT INTO supplier (name, contact_person, phone, email, address) VALUES
    ('MediSupply Co.', 'Bilal Ahmed', '03001234567', 'contact@medisupply.example', 'Lahore, Pakistan');
-- ============================================================

