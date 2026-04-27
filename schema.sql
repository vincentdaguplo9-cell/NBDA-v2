-- NBDA v2.1 - Fresh Database Schema
-- Run this to create NEW system (not migration)

DROP DATABASE IF EXISTS blood_archive;
CREATE DATABASE blood_archive;
USE blood_archive;

-- 1. Users Table
CREATE TABLE users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'STAFF',
    is_admin BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Donors Table (v2.1 - with External ID Mapping)
CREATE TABLE donors (
    donor_id INT PRIMARY KEY AUTO_INCREMENT,
    external_card_id VARCHAR(50) UNIQUE DEFAULT NULL,
    external_source VARCHAR(20) DEFAULT 'NONE',
    first_name VARCHAR(50) NOT NULL,
    middle_name VARCHAR(50) NOT NULL DEFAULT '',
    last_name VARCHAR(50) NOT NULL,
    sex ENUM('MALE', 'FEMALE') NOT NULL,
    birth_date DATE NOT NULL,
    blood_type ENUM('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-') NOT NULL,
    barangay VARCHAR(100) NOT NULL,
    contact_no VARCHAR(20) NOT NULL,
    last_successful_donation DATE NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    UNIQUE KEY uq_donor_identity (first_name, middle_name, last_name, birth_date, contact_no)
);

-- 3. Donor Screening Table
CREATE TABLE donor_screening (
    screening_id INT PRIMARY KEY AUTO_INCREMENT,
    donor_id INT NOT NULL,
    screened_by INT NULL,
    screening_date DATE NOT NULL,
    intended_collection_date DATE NOT NULL,
    weight_kg DECIMAL(5,2) NOT NULL,
    blood_pressure VARCHAR(15) NOT NULL,
    systolic_bp SMALLINT NOT NULL DEFAULT 0,
    diastolic_bp SMALLINT NOT NULL DEFAULT 0,
    pulse_bpm SMALLINT NOT NULL DEFAULT 0,
    temperature_c DECIMAL(4,1) NOT NULL DEFAULT 0,
    hemoglobin_g_dl DECIMAL(4,1) NOT NULL DEFAULT 0,
    slept_hours DECIMAL(3,1) NOT NULL DEFAULT 0,
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
    CONSTRAINT fk_screening_donor FOREIGN KEY (donor_id) REFERENCES donors(donor_id) ON DELETE RESTRICT
);

-- 4. Blood Inventory Table
CREATE TABLE blood_inventory (
    bag_id VARCHAR(32) PRIMARY KEY,
    donor_id INT NOT NULL,
    screening_id INT NOT NULL,
    blood_type ENUM('A+', 'A-', 'B+', 'B-', 'AB+', 'AB-', 'O+', 'O-') NOT NULL,
    collection_date DATE NOT NULL,
    expiry_date DATE NOT NULL,
    inventory_status ENUM('QUARANTINE', 'AVAILABLE', 'ISSUED', 'DISCARDED', 'EXPIRED') NOT NULL DEFAULT 'QUARANTINE',
    -- Collection details
    collection_volume_ml INT NULL,
    component VARCHAR(20) NULL,
    -- TTI Screening results
    tti_test_method VARCHAR(100) NULL,
    tti_hiv ENUM('NON_REACTIVE', 'REACTIVE') NULL,
    tti_hbv ENUM('NON_REACTIVE', 'REACTIVE') NULL,
    tti_hcv ENUM('NON_REACTIVE', 'REACTIVE') NULL,
    tti_syphilis ENUM('NON_REACTIVE', 'REACTIVE') NULL,
    tti_malaria ENUM('NON_REACTIVE', 'REACTIVE') NULL,
    tti_overall_status ENUM('CLEARED', 'REACTIVE') NULL,
    tti_remarks TEXT NULL,
    -- Issue fields
    issued_at DATETIME NULL,
    issue_patient_name VARCHAR(100) NULL,
    patient_hospital_no VARCHAR(50) NULL,
    request_hospital VARCHAR(100) NULL,
    requesting_physician VARCHAR(100) NULL,
    blood_request_no VARCHAR(50) NULL,
    crossmatch_status ENUM('COMPATIBLE', 'NOT_REQUIRED', 'INCOMPATIBLE', 'PENDING') NULL,
    issued_by INT NULL,
    FOREIGN KEY (donor_id) REFERENCES donors(donor_id),
    FOREIGN KEY (screening_id) REFERENCES donor_screening(screening_id)
);

-- 5. Audit Log
CREATE TABLE audit_log (
    audit_id INT PRIMARY KEY AUTO_INCREMENT,
    user_id INT NULL,
    event_time TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    action_type VARCHAR(50) NOT NULL,
    entity_type VARCHAR(50) NOT NULL,
    entity_id VARCHAR(50) NULL,
    details TEXT NULL,
    FOREIGN KEY (user_id) REFERENCES users(user_id)
);

-- Indexes
CREATE INDEX idx_donors_blood_type ON donors(blood_type);
CREATE INDEX idx_inventory_status ON blood_inventory(inventory_status);
CREATE INDEX idx_inventory_tti_status ON blood_inventory(tti_overall_status);
CREATE INDEX idx_inventory_expiry ON blood_inventory(expiry_date);
CREATE INDEX idx_inventory_issue_status ON blood_inventory(issued_at);
CREATE INDEX idx_screening_collection_date ON donor_screening(intended_collection_date);
CREATE INDEX idx_audit_event_time ON audit_log(event_time);

-- Insert default admin user (username: admin, password: admin123)
INSERT INTO users (username, password_hash, first_name, last_name, role, is_admin) 
VALUES ('admin', '$2a$10$r7O0Cv0m7k7k7k7k7k7k7O7O0Cv0m7k7k7k7k7k7k7O0Cv0m7k7', 'System', 'Administrator', 'ADMIN', TRUE);