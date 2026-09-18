-- Create worker earnings table Flyway Migration
CREATE TABLE worker_earnings (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    worker_id BIGINT NOT NULL,
    application_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    amount DECIMAL(10,2) NOT NULL,
    earning_date TIMESTAMP NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_worker_earnings_worker FOREIGN KEY (worker_id) REFERENCES users(id),
    CONSTRAINT fk_worker_earnings_application FOREIGN KEY (application_id) REFERENCES applications(id),
    CONSTRAINT fk_worker_earnings_job FOREIGN KEY (job_id) REFERENCES jobs(id),
    CONSTRAINT uq_worker_earnings_application UNIQUE (application_id)
);

CREATE INDEX idx_worker_earnings_worker ON worker_earnings (worker_id, deleted);
