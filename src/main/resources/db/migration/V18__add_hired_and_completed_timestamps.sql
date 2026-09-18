-- Migration to add hired and completed timestamps to applications table
ALTER TABLE applications
    ADD COLUMN hired_at TIMESTAMP NULL,
    ADD COLUMN completed_at TIMESTAMP NULL;
