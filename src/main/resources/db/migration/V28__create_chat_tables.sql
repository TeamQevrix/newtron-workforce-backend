-- V28: Create chat_sessions and chat_messages tables for Team Chat

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS chat_messages;
DROP TABLE IF EXISTS chat_sessions;

-- 1. Create chat_sessions table
CREATE TABLE chat_sessions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    job_id BIGINT NOT NULL,
    team_id BIGINT NOT NULL,
    application_id BIGINT NOT NULL,
    
    -- BaseEntity audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- Active status for UNIQUE constraints
    active_status INT GENERATED ALWAYS AS (IF(deleted = FALSE, 1, NULL)) STORED,

    -- Constraints
    CONSTRAINT fk_chat_session_job FOREIGN KEY (job_id) REFERENCES jobs(id),
    CONSTRAINT fk_chat_session_team FOREIGN KEY (team_id) REFERENCES teams(id),
    CONSTRAINT fk_chat_session_application FOREIGN KEY (application_id) REFERENCES applications(id),

    -- Unique constraint for hiring relationship
    UNIQUE KEY uq_active_chat_session (job_id, team_id, application_id, active_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Create chat_messages table
CREATE TABLE chat_messages (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    session_id BIGINT NOT NULL,
    sender_user_id BIGINT NOT NULL,
    message TEXT NOT NULL,
    
    -- BaseEntity audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    -- Constraints
    CONSTRAINT fk_chat_message_session FOREIGN KEY (session_id) REFERENCES chat_sessions(id),
    CONSTRAINT fk_chat_message_sender FOREIGN KEY (sender_user_id) REFERENCES users(id)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Indexes for query optimization
CREATE INDEX idx_chat_sessions_job_team ON chat_sessions(job_id, team_id);
CREATE INDEX idx_chat_sessions_application ON chat_sessions(application_id);
CREATE INDEX idx_chat_messages_session ON chat_messages(session_id);
CREATE INDEX idx_chat_messages_sender ON chat_messages(sender_user_id);

SET FOREIGN_KEY_CHECKS = 1;
