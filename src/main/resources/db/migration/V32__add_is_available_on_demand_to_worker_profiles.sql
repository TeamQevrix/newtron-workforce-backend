ALTER TABLE worker_profiles
ADD COLUMN is_available_on_demand BOOLEAN NOT NULL DEFAULT FALSE;
