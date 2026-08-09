-- SPRINT B2 - STEP 2: Worker Professional Details Schema Migration

SET FOREIGN_KEY_CHECKS = 0;

-- Drop existing tables to recreate them cleanly
DROP TABLE IF EXISTS worker_skills;
DROP TABLE IF EXISTS worker_professional_details;
DROP TABLE IF EXISTS master_skills;
DROP TABLE IF EXISTS master_qualifications;

-- 1. Create master_skills table
CREATE TABLE master_skills (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- Populate master_skills
INSERT INTO master_skills (name) VALUES 
('Electrician'),
('Plumber'),
('Carpenter'),
('Driver'),
('Welder'),
('Mason'),
('Painter'),
('Helper'),
('Security Guard'),
('Delivery Executive');

-- 2. Create master_qualifications table
CREATE TABLE master_qualifications (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    name VARCHAR(100) NOT NULL UNIQUE
);

-- Populate master_qualifications
INSERT INTO master_qualifications (name) VALUES
('No Formal Education'),
('5th Pass'),
('8th Pass'),
('10th Pass'),
('12th Pass'),
('ITI'),
('Diploma'),
('Graduate'),
('Post Graduate');

-- 3. Create worker_skills table
CREATE TABLE worker_skills (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    worker_profile_id BIGINT NOT NULL,
    skill_id BIGINT NOT NULL,
    is_primary BOOLEAN NOT NULL DEFAULT FALSE,
    experience_years INT NOT NULL DEFAULT 0,
    experience_months INT NOT NULL DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_worker_skills_profile FOREIGN KEY (worker_profile_id) REFERENCES worker_profiles(id),
    CONSTRAINT fk_worker_skills_master FOREIGN KEY (skill_id) REFERENCES master_skills(id)
);

-- 4. Create worker_professional_details table
CREATE TABLE worker_professional_details (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    worker_profile_id BIGINT NOT NULL UNIQUE,
    highest_qualification_id BIGINT,
    current_employment_status VARCHAR(30) NOT NULL,
    salary_type VARCHAR(30) NOT NULL,
    expected_salary DECIMAL(10,2) NOT NULL,
    preferred_work_type VARCHAR(30) NOT NULL,
    preferred_shift VARCHAR(30) NOT NULL,
    immediate_joining BOOLEAN NOT NULL DEFAULT FALSE,
    current_company VARCHAR(100),
    current_designation VARCHAR(100),
    notice_period_days INT DEFAULT 0,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_prof_details_profile FOREIGN KEY (worker_profile_id) REFERENCES worker_profiles(id),
    CONSTRAINT fk_prof_details_qualification FOREIGN KEY (highest_qualification_id) REFERENCES master_qualifications(id)
);

SET FOREIGN_KEY_CHECKS = 1;
