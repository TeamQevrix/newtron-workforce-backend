CREATE TABLE agreements (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    application_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    worker_id BIGINT NOT NULL,
    engagement_type VARCHAR(50) NOT NULL,
    daily_wage DECIMAL(12, 2) NULL,
    monthly_salary DECIMAL(12, 2) NULL,
    commission_rate DECIMAL(5, 4) NOT NULL DEFAULT 0.0500,
    payment_responsibility VARCHAR(100) NULL,
    payment_due_terms VARCHAR(255) NULL,
    notice_days INT NULL,
    cancellation_terms VARCHAR(1000) NULL,
    status VARCHAR(50) NOT NULL DEFAULT 'DRAFT',
    client_accepted_at TIMESTAMP NULL,
    worker_accepted_at TIMESTAMP NULL,
    effective_at TIMESTAMP NULL,
    completed_at TIMESTAMP NULL,

    -- BaseEntity audits
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NULL,
    updated_by VARCHAR(255) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_agreements_application UNIQUE (application_id),
    CONSTRAINT fk_agreements_application FOREIGN KEY (application_id) REFERENCES applications (id),
    CONSTRAINT fk_agreements_job FOREIGN KEY (job_id) REFERENCES jobs (id),
    CONSTRAINT fk_agreements_company FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT fk_agreements_worker FOREIGN KEY (worker_id) REFERENCES users (id)
);

CREATE INDEX idx_agreements_status ON agreements (status);
CREATE INDEX idx_agreements_worker ON agreements (worker_id);
CREATE INDEX idx_agreements_job ON agreements (job_id);
