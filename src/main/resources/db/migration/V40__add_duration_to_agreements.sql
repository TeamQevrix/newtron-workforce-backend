ALTER TABLE agreements
ADD COLUMN engagement_duration_type VARCHAR(30),
ADD COLUMN duration_value INT,
ADD COLUMN duration VARCHAR(255);
