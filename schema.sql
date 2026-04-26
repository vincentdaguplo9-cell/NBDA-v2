CREATE DATABASE IF NOT EXISTS blood_archive;
USE blood_archive;

CREATE TABLE IF NOT EXISTS users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(40) NOT NULL UNIQUE,
    password_hash VARCHAR(64) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    role ENUM('ADMIN', 'STAFF') NOT NULL DEFAULT 'STAFF',
    active TINYINT(1) NOT NULL DEFAULT 1,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE IF NOT EXISTS donors (
    donor_id INT PRIMARY KEY AUTO_INCREMENT,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    sex ENUM('MALE', 'FEMALE') NOT NULL,
    birth_date DATE NOT NULL,
    blood_type ENUM('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-') NOT NULL,
    barangay VARCHAR(100) NOT NULL,
    contact_no VARCHAR(20) NOT NULL,
    last_successful_donation DATE NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_donor_identity (first_name, last_name, birth_date, contact_no)
);

CREATE TABLE IF NOT EXISTS donor_screening (
    screening_id INT PRIMARY KEY AUTO_INCREMENT,
    donor_id INT NOT NULL,
    screened_by INT NULL,
    screening_date DATE NOT NULL,
    intended_collection_date DATE NOT NULL,
    weight_kg DECIMAL(5,2) NOT NULL,
    blood_pressure VARCHAR(15) NOT NULL,
    systolic_bp SMALLINT NOT NULL,
    diastolic_bp SMALLINT NOT NULL,
    pulse_bpm SMALLINT NOT NULL,
    temperature_c DECIMAL(4,1) NOT NULL,
    hemoglobin_g_dl DECIMAL(4,1) NOT NULL,
    slept_hours DECIMAL(3,1) NOT NULL,
    guardian_consent_provided TINYINT(1) NOT NULL DEFAULT 0,
    had_meal TINYINT(1) NOT NULL DEFAULT 0,
    alcohol_in_last_24h TINYINT(1) NOT NULL DEFAULT 0,
    has_fever_cough_colds TINYINT(1) NOT NULL DEFAULT 0,
    had_tattoo_or_piercing_last_12m TINYINT(1) NOT NULL DEFAULT 0,
    had_recent_operation TINYINT(1) NOT NULL DEFAULT 0,
    currently_pregnant TINYINT(1) NOT NULL DEFAULT 0,
    screening_status ENUM('ELIGIBLE', 'TEMPORARILY_DEFERRED') NOT NULL,
    decision_reason VARCHAR(255) NOT NULL,
    next_eligible_date DATE NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_screening_donor
        FOREIGN KEY (donor_id)
        REFERENCES donors(donor_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_screening_user
        FOREIGN KEY (screened_by)
        REFERENCES users(user_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS blood_inventory (
    bag_id VARCHAR(32) PRIMARY KEY,
    donor_id INT NOT NULL,
    screening_id INT NOT NULL,
    blood_type ENUM('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-') NOT NULL,
    component ENUM('WHOLE_BLOOD') NOT NULL DEFAULT 'WHOLE_BLOOD',
    collection_volume_ml SMALLINT NOT NULL DEFAULT 450,
    collection_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    tti_hiv ENUM('PENDING', 'NON_REACTIVE', 'REACTIVE') NOT NULL DEFAULT 'PENDING',
    tti_hbv ENUM('PENDING', 'NON_REACTIVE', 'REACTIVE') NOT NULL DEFAULT 'PENDING',
    tti_hcv ENUM('PENDING', 'NON_REACTIVE', 'REACTIVE') NOT NULL DEFAULT 'PENDING',
    tti_syphilis ENUM('PENDING', 'NON_REACTIVE', 'REACTIVE') NOT NULL DEFAULT 'PENDING',
    tti_malaria ENUM('PENDING', 'NON_REACTIVE', 'REACTIVE') NOT NULL DEFAULT 'PENDING',
    tti_overall_status ENUM('PENDING', 'CLEARED', 'REACTIVE') NOT NULL DEFAULT 'PENDING',
    tti_tested_at DATETIME NULL,
    tti_tested_by INT NULL,
    tti_test_kit VARCHAR(120) NULL,
    inventory_status ENUM('QUARANTINE', 'AVAILABLE', 'ISSUED', 'DISCARDED', 'EXPIRED') NOT NULL DEFAULT 'QUARANTINE',
    disposition_notes VARCHAR(255) NULL,
    crossmatch_status ENUM('PENDING', 'COMPATIBLE', 'INCOMPATIBLE', 'NOT_REQUIRED') NOT NULL DEFAULT 'PENDING',
    issue_patient_name VARCHAR(150) NULL,
    patient_hospital_no VARCHAR(60) NULL,
    request_hospital VARCHAR(150) NULL,
    requesting_physician VARCHAR(120) NULL,
    blood_request_no VARCHAR(60) NULL,
    issued_at DATETIME NULL,
    issued_by INT NULL,
    issue_notes VARCHAR(255) NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT fk_inventory_donor
        FOREIGN KEY (donor_id)
        REFERENCES donors(donor_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_inventory_screening
        FOREIGN KEY (screening_id)
        REFERENCES donor_screening(screening_id)
        ON DELETE RESTRICT
        ON UPDATE CASCADE,
    CONSTRAINT fk_inventory_tti_user
        FOREIGN KEY (tti_tested_by)
        REFERENCES users(user_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE,
    CONSTRAINT fk_inventory_issue_user
        FOREIGN KEY (issued_by)
        REFERENCES users(user_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);

CREATE TABLE IF NOT EXISTS audit_log (
    audit_id INT PRIMARY KEY AUTO_INCREMENT,
    event_time DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
    user_id INT NULL,
    action_type VARCHAR(40) NOT NULL,
    entity_type VARCHAR(40) NOT NULL,
    entity_id VARCHAR(60) NULL,
    details VARCHAR(255) NULL,
    CONSTRAINT fk_audit_user
        FOREIGN KEY (user_id)
        REFERENCES users(user_id)
        ON DELETE SET NULL
        ON UPDATE CASCADE
);

CREATE INDEX idx_donors_blood_type ON donors(blood_type);
CREATE INDEX idx_inventory_status ON blood_inventory(inventory_status);
CREATE INDEX idx_inventory_tti_status ON blood_inventory(tti_overall_status);
CREATE INDEX idx_inventory_expiry ON blood_inventory(expiry_date);
CREATE INDEX idx_inventory_issue_status ON blood_inventory(issued_at);
CREATE INDEX idx_screening_collection_date ON donor_screening(intended_collection_date);
CREATE INDEX idx_audit_event_time ON audit_log(event_time);
