SET FOREIGN_KEY_CHECKS = 0;

-- Drop the unique constraint that restricts one active team per owner
ALTER TABLE teams DROP INDEX uq_active_team_owner;

-- Create a non-unique index to maintain query performance for owner lookups
CREATE INDEX idx_teams_owner_active ON teams(owner_worker_profile_id, active_status);

SET FOREIGN_KEY_CHECKS = 1;
