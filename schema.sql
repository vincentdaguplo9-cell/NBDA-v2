-- NBDA v2.1 - Database Schema
-- Run this once to create the database

CREATE DATABASE IF NOT EXISTS blood_archive;
USE blood_archive;

-- 1. Users Table
CREATE TABLE IF NOT EXISTS users (
    user_id INT PRIMARY KEY AUTO_INCREMENT,
    staff_id VARCHAR(20) UNIQUE NULL,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(255) NOT NULL,
    first_name VARCHAR(50) NOT NULL,
    last_name VARCHAR(50) NOT NULL,
    role VARCHAR(20) NOT NULL DEFAULT 'STAFF',
    active BOOLEAN NOT NULL DEFAULT TRUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

-- 2. Donors Table (v2.2 - Transient Authentication Model)
CREATE TABLE IF NOT EXISTS donors (
    donor_id VARCHAR(20) PRIMARY KEY, -- Format: NBDA-YYYY-00001
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

-- Donor ID Counter Table for generating sequential IDs
CREATE TABLE IF NOT EXISTS donor_id_counter (
    id_prefix VARCHAR(10) PRIMARY KEY, -- e.g., 'NBDA-2026'
    last_number INT NOT NULL DEFAULT 0
);

-- Bag ID Counter Table for generating sequential IDs  
CREATE TABLE IF NOT EXISTS bag_id_counter (
    id_prefix VARCHAR(15) PRIMARY KEY, -- e.g., 'BAG-20260429-'
    last_number INT NOT NULL DEFAULT 0
);

-- 3. Donor Screening Table
CREATE TABLE IF NOT EXISTS donor_screening (
    screening_id INT PRIMARY KEY AUTO_INCREMENT,
    donor_id VARCHAR(20) NOT NULL,
    auth_id_type ENUM('NATIONAL_ID', 'STUDENT_ID', 'BARANGAY_ID', 'PRC_CARD', 'DRIVERS_LICENSE',
        'PASSPORT', 'UMID', 'VOTERS_ID', 'SSS_GSIS', 'SENIOR_PWD_ID', 'EMPLOYEE_ID', 'OTHER') NOT NULL,
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
    decision_reason TEXT NOT NULL,
    next_eligible_date DATE NULL,
    CONSTRAINT fk_screening_donor FOREIGN KEY (donor_id) REFERENCES donors(donor_id) ON UPDATE CASCADE ON DELETE RESTRICT
);

-- 4. Blood Inventory Table
CREATE TABLE IF NOT EXISTS blood_inventory (
    bag_id VARCHAR(32) PRIMARY KEY,
    donor_id VARCHAR(20) NOT NULL,
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
    tti_tested_at DATETIME NULL,
    tti_tested_by INT NULL,
    tti_test_kit VARCHAR(120) NULL,
    -- Issue fields
    issued_at DATETIME NULL,
    issue_patient_name VARCHAR(100) NULL,
    patient_hospital_no VARCHAR(50) NULL,
    request_hospital VARCHAR(100) NULL,
    requesting_physician VARCHAR(100) NULL,
    blood_request_no VARCHAR(50) NULL,
    crossmatch_status ENUM('COMPATIBLE', 'NOT_REQUIRED', 'INCOMPATIBLE', 'PENDING') NULL,
    issued_by INT NULL,
    issue_notes VARCHAR(255) NULL,
    CONSTRAINT fk_inventory_donor FOREIGN KEY (donor_id) REFERENCES donors(donor_id) ON UPDATE CASCADE,
    CONSTRAINT fk_inventory_screening FOREIGN KEY (screening_id) REFERENCES donor_screening(screening_id)
);

-- 5. Audit Log
CREATE TABLE IF NOT EXISTS audit_log (
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