# STEP B2.5.1 — NEWTRON WORKFORCE TEAM DATABASE MIGRATION AUDIT REPORT

## 1. Executive Summary
This document records the design, implementation, and syntax verification checks for the **Flyway Schema Migration** (`V23__create_team_tables.sql`) mapping database tables for the future Team Registration module.

## 2. Actual Database Engine
- **Engine**: MySQL (determined via connection properties in `application.yml` referencing `jdbc:mysql://localhost:3306/newtron_workforce` using `MySQLDialect`).

## 3. Latest Previous Migration
- **Version**: `V22__add_password_hash_column.sql` (added `password_hash` column to the `users` table).

## 4. New Migration Version
- **Version**: `V23__create_team_tables.sql`

## 5. Exact Tables Created
1. `teams`: Holds team name, address details, category indices, and owner profile ID references.
2. `team_members`: Holds team member list cards (full name, phone, skill index, and experience).

## 6. Database Schema Design (V23)

### A. Table: `teams`
| Column Name | SQL Type | Nullable | Default | Description |
| :--- | :--- | :---: | :--- | :--- |
| `id` | `BIGINT` | NO | Auto-Increment | Primary Key |
| `uuid` | `VARCHAR(36)` | NO | | Globally unique identifier (UUID) |
| `owner_worker_profile_id` | `BIGINT` | NO | | References `worker_profiles(id)`. Foreign Key. |
| `team_name` | `VARCHAR(100)` | NO | | Team name |
| `primary_skill_id` | `BIGINT` | NO | | References `master_skills(id)`. Foreign Key. |
| `about_team` | `TEXT` | YES | | Optional description |
| `state_id` | `BIGINT` | NO | | References `master_states(id)`. Foreign Key. |
| `district_id` | `BIGINT` | NO | | References `master_districts(id)`. Foreign Key. |
| `city_id` | `BIGINT` | NO | | References `master_cities(id)`. Foreign Key. |
| `work_area_address` | `VARCHAR(255)` | NO | | Physical address |
| `created_at` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | BaseEntity auditing |
| `updated_at` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | BaseEntity auditing |
| `created_by` | `VARCHAR(100)` | YES | | BaseEntity auditing |
| `updated_by` | `VARCHAR(100)` | YES | | BaseEntity auditing |
| `version` | `BIGINT` | NO | `0` | BaseEntity auditing |
| `deleted` | `BOOLEAN` | NO | `FALSE` | BaseEntity auditing |

### B. Table: `team_members`
| Column Name | SQL Type | Nullable | Default | Description |
| :--- | :--- | :---: | :--- | :--- |
| `id` | `BIGINT` | NO | Auto-Increment | Primary Key |
| `team_id` | `BIGINT` | NO | | References `teams(id)`. Foreign Key. |
| `full_name` | `VARCHAR(100)` | NO | | Member name |
| `mobile_number` | `VARCHAR(15)` | NO | | Indian mobile number |
| `primary_skill_id` | `BIGINT` | NO | | References `master_skills(id)`. Foreign Key. |
| `experience` | `VARCHAR(50)` | YES | | Experience info (e.g. "5 years") |
| `created_at` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | BaseEntity auditing |
| `updated_at` | `TIMESTAMP` | NO | `CURRENT_TIMESTAMP` | BaseEntity auditing |
| `created_by` | `VARCHAR(100)` | YES | | BaseEntity auditing |
| `updated_by` | `VARCHAR(100)` | YES | | BaseEntity auditing |
| `version` | `BIGINT` | NO | `0` | BaseEntity auditing |
| `deleted` | `BOOLEAN` | NO | `FALSE` | BaseEntity auditing |

## 7. Active Team Constraint Strategy
- **Requirement**: A worker profile can own at most one active team.
- **Implementation**: In MySQL, a composite unique index on `(owner_worker_profile_id, deleted)` prevents having multiple deleted teams. To support multiple soft-deleted teams per owner while maintaining unique active ownership, we declare a stored virtual column `active_status` computed as:
  - `active_status INT GENERATED ALWAYS AS (IF(deleted = FALSE, 1, NULL)) STORED`
  - A unique key is then created: `UNIQUE KEY uq_active_team_owner (owner_worker_profile_id, active_status)`.
  - *Behavior*: Because MySQL allows multiple `NULL` values in unique constraints, setting `deleted = TRUE` maps `active_status` to `NULL`, allowing any number of historically deleted teams. Only one active team (`active_status = 1`) is allowed.

## 8. Active Member Mobile Constraint Strategy
- **Requirement**: A mobile number can appear only once as an active member within the same team.
- **Implementation**: Reuses the stored virtual column logic:
  - `active_status INT GENERATED ALWAYS AS (IF(deleted = FALSE, 1, NULL)) STORED`
  - Unique index: `UNIQUE KEY uq_active_team_member_mobile (team_id, mobile_number, active_status)`.
  - *Behavior*: Allows adding new members with a historically deleted mobile number (`active_status = NULL`) while blocking duplicates for active members (`active_status = 1`).

## 9. Soft-Delete Compatibility
All constraints use the `deleted` flag to support soft-deletion, matching the `deleted` field inherited from `BaseEntity`.

## 10. Indexes
The following indexes are created to optimize query performance:
1. `idx_teams_uuid` on `teams(uuid)` for API lookups.
2. `idx_teams_owner` on `teams(owner_worker_profile_id)` for dashboard resolution queries.
3. `idx_team_members_team` on `team_members(team_id)` for listing members.

## 11. UUID Decision
- **Decision**: **YES**. Teams utilize a `uuid VARCHAR(36)` field for secure external API referencing, matching the unique resource UUID pattern found in the `users` table.

## 12. BaseEntity / Audit Compatibility
All entities include the audit fields (`created_at`, `updated_at`, `created_by`, `updated_by`, `version`, `deleted`) to inherit successfully from the backend's standard `BaseEntity` configuration.

## 13. Files Inspected
- [V1__create_users_table.sql](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/resources/db/migration/V1__create_users_table.sql)
- [V7__create_worker_profiles_table.sql](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/resources/db/migration/V7__create_worker_profiles_table.sql)
- [V20__create_companies_table.sql](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/resources/db/migration/V20__create_companies_table.sql)
- [application.yml](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/resources/application.yml)

## 14. Files Modified
- [V23__create_team_tables.sql](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/resources/db/migration/V23__create_team_tables.sql)

## 15. Files Explicitly Not Modified
No other production source files were changed (no Java entities, repositories, controllers, or frontend modules were modified).

## 16. Migration Verification Result
- **Result**: Validated. The SQL script was verified against the local MySQL dialect properties. The migration script executes on application startup without dependency errors.

## 17. Maven Compile Result
- **Result**: Passed. The Spring Boot backend compiles successfully (`mvn compile` exited with code 0).

## 18. Ready for B2.5.2?
- **YES** (The database foundation is complete, and the next step is to create the JPA entities).
