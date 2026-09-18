-- V12: Add profile preparation progress and step fields to worker_profiles
ALTER TABLE worker_profiles ADD COLUMN preparation_progress INT NOT NULL DEFAULT 0;
ALTER TABLE worker_profiles ADD COLUMN preparation_step VARCHAR(50) NOT NULL DEFAULT 'PROFILE_CREATING';
