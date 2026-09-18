ALTER TABLE jobs ADD COLUMN monthly_salary_amount DECIMAL(12, 2) NULL;

CREATE TABLE commissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    company_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    application_id BIGINT NOT NULL,
    worker_id BIGINT NOT NULL,
    salary_base DECIMAL(12, 2) NOT NULL,
    commission_rate DECIMAL(5, 4) NOT NULL DEFAULT 0.0500,
    commission_amount DECIMAL(12, 2) NOT NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'PENDING',
    hired_at TIMESTAMP NOT NULL,
    paid_at TIMESTAMP NULL,
    
    -- BaseEntity audits
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NULL,
    updated_by VARCHAR(255) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_commissions_application UNIQUE (application_id),
    CONSTRAINT fk_commissions_company FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT fk_commissions_job FOREIGN KEY (job_id) REFERENCES jobs (id),
    CONSTRAINT fk_commissions_application FOREIGN KEY (application_id) REFERENCES applications (id),
    CONSTRAINT fk_commissions_worker FOREIGN KEY (worker_id) REFERENCES users (id)
);

CREATE INDEX idx_commissions_company ON commissions (company_id);
CREATE INDEX idx_commissions_job ON commissions (job_id);
CREATE INDEX idx_commissions_status ON commissions (status);
