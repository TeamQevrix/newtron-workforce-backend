-- SPRINT B2 - STEP 1: Worker Basic Profile Schema Migration

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS worker_documents;
DROP TABLE IF EXISTS worker_memberships;
DROP TABLE IF EXISTS worker_addresses;
DROP TABLE IF EXISTS worker_professional_details;
DROP TABLE IF EXISTS worker_profiles;

CREATE TABLE worker_profiles (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL UNIQUE,
    full_name VARCHAR(80) NOT NULL,
    date_of_birth DATE NOT NULL,
    gender VARCHAR(20) NOT NULL,
    blood_group VARCHAR(20),
    photo_storage_key VARCHAR(255),
    current_step VARCHAR(50) NOT NULL,
    is_completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_worker_profiles_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE worker_professional_details (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    worker_profile_id BIGINT NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_prof_details_profile FOREIGN KEY (worker_profile_id) REFERENCES worker_profiles(id)
);

CREATE TABLE worker_addresses (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    worker_profile_id BIGINT NOT NULL UNIQUE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_addresses_profile FOREIGN KEY (worker_profile_id) REFERENCES worker_profiles(id)
);

CREATE TABLE worker_documents (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    worker_profile_id BIGINT NOT NULL,
    document_type VARCHAR(50) NOT NULL,
    file_storage_key VARCHAR(255) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_documents_profile FOREIGN KEY (worker_profile_id) REFERENCES worker_profiles(id)
);

CREATE TABLE worker_memberships (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    worker_profile_id BIGINT NOT NULL UNIQUE,
    plan_name VARCHAR(100),
    amount DECIMAL(10,2),
    validity_days INT,
    order_id VARCHAR(100) UNIQUE,
    payment_id VARCHAR(100) UNIQUE,
    payment_method VARCHAR(50),
    status VARCHAR(50),
    start_date DATETIME,
    expiry_date DATETIME,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_memberships_profile FOREIGN KEY (worker_profile_id) REFERENCES worker_profiles(id)
);

SET FOREIGN_KEY_CHECKS = 1;
