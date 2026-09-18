-- Create worker reviews table Flyway Migration
CREATE TABLE worker_reviews (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    worker_id BIGINT NOT NULL,
    recruiter_id BIGINT NOT NULL,
    application_id BIGINT NOT NULL UNIQUE,
    job_id BIGINT NOT NULL,
    rating INT NOT NULL,
    comment TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100),
    updated_by VARCHAR(100),
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_worker_reviews_worker FOREIGN KEY (worker_id) REFERENCES users(id),
    CONSTRAINT fk_worker_reviews_recruiter FOREIGN KEY (recruiter_id) REFERENCES users(id),
    CONSTRAINT fk_worker_reviews_application FOREIGN KEY (application_id) REFERENCES applications(id),
    CONSTRAINT fk_worker_reviews_job FOREIGN KEY (job_id) REFERENCES jobs(id),
    CONSTRAINT uq_worker_reviews_application UNIQUE (application_id)
);

CREATE INDEX idx_worker_reviews_worker ON worker_reviews (worker_id, deleted);
