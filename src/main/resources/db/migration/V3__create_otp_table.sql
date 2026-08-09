CREATE TABLE otps
(
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NULL,
    updated_by VARCHAR(255) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    mobile_country_code VARCHAR(5) NOT NULL,
    mobile_number VARCHAR(15) NOT NULL,
    otp_hash VARCHAR(64) NOT NULL,
    purpose VARCHAR(30) NOT NULL,
    status VARCHAR(20) NOT NULL,
    role VARCHAR(20) NULL,
    attempt_count INT NOT NULL DEFAULT 0,
    max_attempts INT NOT NULL DEFAULT 5,
    expires_at TIMESTAMP NOT NULL,
    verified_at TIMESTAMP NULL,
    blocked_until TIMESTAMP NULL,
    last_sent_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_otps_mobile_purpose_status
ON otps(mobile_country_code, mobile_number, purpose, status);

