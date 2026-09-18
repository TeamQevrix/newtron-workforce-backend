-- V23: Create teams and team_members tables for Worker Team Registration module

SET FOREIGN_KEY_CHECKS = 0;

DROP TABLE IF EXISTS team_members;
DROP TABLE IF EXISTS teams;

-- 1. Create teams table
CREATE TABLE teams (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    uuid VARCHAR(36) NOT NULL UNIQUE,
    owner_worker_profile_id BIGINT NOT NULL,
    team_name VARCHAR(100) NOT NULL,
    primary_skill_id BIGINT NOT NULL,
    about_team TEXT NULL,
    state_id BIGINT NOT NULL,
    district_id BIGINT NOT NULL,
    city_id BIGINT NOT NULL,
    work_area_address VARCHAR(255) NOT NULL,
    
    -- BaseEntity audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,
    
    -- A helper column for MySQL UNIQUE constraint compatibility with soft delete.
    -- Is set to 1 when team is active, and NULL when soft-deleted.
    -- Since MySQL allows multiple NULL values in UNIQUE constraints.
    active_status INT GENERATED ALWAYS AS (IF(deleted = FALSE, 1, NULL)) STORED,
    
    -- Constraints
    CONSTRAINT fk_teams_owner FOREIGN KEY (owner_worker_profile_id) REFERENCES worker_profiles(id),
    CONSTRAINT fk_teams_skill FOREIGN KEY (primary_skill_id) REFERENCES master_skills(id),
    CONSTRAINT fk_teams_state FOREIGN KEY (state_id) REFERENCES master_states(id),
    CONSTRAINT fk_teams_district FOREIGN KEY (district_id) REFERENCES master_districts(id),
    CONSTRAINT fk_teams_city FOREIGN KEY (city_id) REFERENCES master_cities(id),
    
    -- A WorkerProfile can only own one active team
    UNIQUE KEY uq_active_team_owner (owner_worker_profile_id, active_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 2. Create team_members table
CREATE TABLE team_members (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    team_id BIGINT NOT NULL,
    full_name VARCHAR(100) NOT NULL,
    mobile_number VARCHAR(15) NOT NULL,
    primary_skill_id BIGINT NOT NULL,
    experience VARCHAR(50) NULL,
    
    -- BaseEntity audit fields
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    created_by VARCHAR(100) NULL,
    updated_by VARCHAR(100) NULL,
    version BIGINT NOT NULL DEFAULT 0,
    deleted BOOLEAN NOT NULL DEFAULT FALSE,

    -- A helper column for MySQL UNIQUE constraint compatibility with soft delete.
    -- Is set to 1 when member is active, and NULL when soft-deleted.
    active_status INT GENERATED ALWAYS AS (IF(deleted = FALSE, 1, NULL)) STORED,
    
    -- Constraints
    CONSTRAINT fk_members_team FOREIGN KEY (team_id) REFERENCES teams(id),
    CONSTRAINT fk_members_skill FOREIGN KEY (primary_skill_id) REFERENCES master_skills(id),
    
    -- An active mobile number can only join a team once
    UNIQUE KEY uq_active_team_member_mobile (team_id, mobile_number, active_status)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci;

-- 3. Indexes for query optimization
CREATE INDEX idx_teams_uuid ON teams(uuid);
CREATE INDEX idx_teams_owner ON teams(owner_worker_profile_id);
CREATE INDEX idx_team_members_team ON team_members(team_id);

SET FOREIGN_KEY_CHECKS = 1;
