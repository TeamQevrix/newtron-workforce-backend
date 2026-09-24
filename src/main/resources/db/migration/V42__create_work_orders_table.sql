CREATE TABLE work_orders (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    work_order_number VARCHAR(255) NOT NULL,
    agreement_id BIGINT NOT NULL,
    company_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    worker_id BIGINT NOT NULL,
    status VARCHAR(50) NOT NULL,
    engagement_type VARCHAR(50) NOT NULL,
    daily_wage DECIMAL(12, 2) NULL,
    monthly_salary DECIMAL(12, 2) NULL,
    commission_rate DECIMAL(5, 4) NOT NULL,
    commission_payer VARCHAR(50) NULL,
    commission_amount DECIMAL(12, 2) NULL,
    expected_start_date TIMESTAMP NULL,
    expected_end_date TIMESTAMP NULL,
    actual_start_date TIMESTAMP NULL,
    actual_end_date TIMESTAMP NULL,
    cancellation_reason VARCHAR(1000) NULL,

    -- BaseEntity audits
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NULL,
    updated_by VARCHAR(255) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    CONSTRAINT uq_work_orders_number UNIQUE (work_order_number),
    CONSTRAINT uq_work_orders_agreement UNIQUE (agreement_id),
    CONSTRAINT fk_work_orders_agreement FOREIGN KEY (agreement_id) REFERENCES agreements (id),
    CONSTRAINT fk_work_orders_company FOREIGN KEY (company_id) REFERENCES companies (id),
    CONSTRAINT fk_work_orders_job FOREIGN KEY (job_id) REFERENCES jobs (id),
    CONSTRAINT fk_work_orders_worker FOREIGN KEY (worker_id) REFERENCES users (id)
);

CREATE INDEX idx_work_orders_status ON work_orders (status);
CREATE INDEX idx_work_orders_worker ON work_orders (worker_id);
CREATE INDEX idx_work_orders_job ON work_orders (job_id);
CREATE INDEX idx_work_orders_company ON work_orders (company_id);
