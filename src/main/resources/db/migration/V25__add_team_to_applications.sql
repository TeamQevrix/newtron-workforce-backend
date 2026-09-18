-- V25: Add team_id to applications for Team Job Applications

ALTER TABLE applications
ADD COLUMN team_id BIGINT DEFAULT NULL;

ALTER TABLE applications
ADD CONSTRAINT fk_applications_team
FOREIGN KEY (team_id) REFERENCES teams(id)
ON DELETE SET NULL;

CREATE INDEX idx_applications_team ON applications(team_id);
