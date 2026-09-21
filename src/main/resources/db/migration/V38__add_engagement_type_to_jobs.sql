-- Add engagement_type to jobs table
ALTER TABLE jobs ADD COLUMN engagement_type VARCHAR(30);

-- Backfill existing jobs to DAILY
UPDATE jobs SET engagement_type = 'DAILY' WHERE engagement_type IS NULL;
