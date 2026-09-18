CREATE TABLE saved_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    worker_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_saved_jobs_worker FOREIGN KEY (worker_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_saved_jobs_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE,
    CONSTRAINT uq_saved_jobs_worker_job UNIQUE (worker_id, job_id)
);

CREATE INDEX idx_saved_jobs_worker ON saved_jobs(worker_id);

CREATE TABLE not_interested_jobs (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    worker_id BIGINT NOT NULL,
    job_id BIGINT NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT fk_not_interested_jobs_worker FOREIGN KEY (worker_id) REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_not_interested_jobs_job FOREIGN KEY (job_id) REFERENCES jobs(id) ON DELETE CASCADE,
    CONSTRAINT uq_not_interested_jobs_worker_job UNIQUE (worker_id, job_id)
);

CREATE INDEX idx_not_interested_jobs_worker ON not_interested_jobs(worker_id);
