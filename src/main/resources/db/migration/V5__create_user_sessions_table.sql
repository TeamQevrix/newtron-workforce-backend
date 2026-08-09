CREATE TABLE user_sessions (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(255) NULL,
    updated_by VARCHAR(255) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    user_id BIGINT NOT NULL,
    refresh_token_hash VARCHAR(64) NOT NULL,
    device_id VARCHAR(100) NOT NULL,
    device_name VARCHAR(150) NULL,
    device_type VARCHAR(50) NULL,
    os_version VARCHAR(50) NULL,
    app_version VARCHAR(50) NULL,
    fcm_token VARCHAR(255) NULL,
    active BOOLEAN NOT NULL DEFAULT TRUE,
    last_activity_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,

    CONSTRAINT fk_user_sessions_user_id FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

CREATE INDEX idx_user_sessions_user ON user_sessions(user_id);
CREATE INDEX idx_user_sessions_device_active ON user_sessions(device_id, active);
CREATE INDEX idx_user_sessions_token_hash ON user_sessions(refresh_token_hash);
