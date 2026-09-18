-- V29: Modify chat_sessions for unified inbox (Team + Company)

-- 1. Drop existing unique constraint and indexes that are no longer accurate
-- Note: uq_active_chat_session was not found in the existing schema, so we do not drop it.
-- Note: idx_chat_sessions_job_team is required by fk_chat_session_job, so we do not drop it.

-- 2. Modify existing columns to be nullable
ALTER TABLE chat_sessions MODIFY COLUMN job_id BIGINT NULL;
ALTER TABLE chat_sessions MODIFY COLUMN application_id BIGINT NULL;

-- 3. Add company_id column
-- Safe to add as NOT NULL because there is no legacy data in chat_sessions
ALTER TABLE chat_sessions ADD COLUMN company_id BIGINT NOT NULL AFTER team_id;

-- 4. Add foreign key for company_id
ALTER TABLE chat_sessions ADD CONSTRAINT fk_chat_session_company FOREIGN KEY (company_id) REFERENCES companies(id);

-- 5. Add new unique constraint for unified inbox (Team + Company)
ALTER TABLE chat_sessions ADD UNIQUE KEY uq_active_chat_session_company (team_id, company_id, active_status);

-- 6. Add new index for query optimization
CREATE INDEX idx_chat_sessions_team_company ON chat_sessions(team_id, company_id);
