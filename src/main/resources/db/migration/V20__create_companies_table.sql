CREATE TABLE companies (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    owner_id BIGINT NOT NULL,
    company_name VARCHAR(255) NOT NULL,
    contact_person_name VARCHAR(255) NOT NULL,
    contact_mobile VARCHAR(20) NOT NULL,
    email VARCHAR(255) NULL,
    city VARCHAR(255) NOT NULL,
    address VARCHAR(500) NULL,
    pincode VARCHAR(20) NULL,
    description VARCHAR(1000) NULL,
    profile_completed BOOLEAN NOT NULL DEFAULT FALSE,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    CONSTRAINT uq_companies_owner UNIQUE (owner_id),
    CONSTRAINT fk_companies_owner FOREIGN KEY (owner_id) REFERENCES users (id) ON DELETE CASCADE
);

CREATE INDEX idx_companies_owner ON companies (owner_id);

ALTER TABLE jobs ADD COLUMN company_id BIGINT NULL;
ALTER TABLE jobs ADD CONSTRAINT fk_jobs_company FOREIGN KEY (company_id) REFERENCES companies (id) ON DELETE SET NULL;
CREATE INDEX idx_jobs_company ON jobs (company_id);
