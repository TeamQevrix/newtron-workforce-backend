CREATE TABLE jobs (
    id BIGINT NOT NULL AUTO_INCREMENT,
    category VARCHAR(255),
    city VARCHAR(255),
    description VARCHAR(2000),
    distance VARCHAR(255),
    duration VARCHAR(255),
    salary VARCHAR(255),
    status VARCHAR(255),
    title VARCHAR(255),
    workers_required INT,
    recruiter_id BIGINT,
    PRIMARY KEY (id),
    FOREIGN KEY (recruiter_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

CREATE TABLE applications (
    id BIGINT NOT NULL AUTO_INCREMENT,
    applied_date VARCHAR(255),
    current_step INT,
    decline_reason VARCHAR(255),
    status VARCHAR(255),
    job_id BIGINT,
    worker_id BIGINT,
    PRIMARY KEY (id),
    FOREIGN KEY (job_id) REFERENCES jobs(id),
    FOREIGN KEY (worker_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;
