CREATE TABLE otp_verifications
(
    id BIGINT PRIMARY KEY AUTO_INCREMENT,

    mobile VARCHAR(15) NOT NULL,

    otp VARCHAR(6) NOT NULL,

    purpose VARCHAR(30) NOT NULL,

    expires_at TIMESTAMP NOT NULL,

    verified BOOLEAN NOT NULL DEFAULT FALSE,

    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_otp_mobile
ON otp_verifications(mobile);

CREATE INDEX idx_otp_verified
ON otp_verifications(verified);