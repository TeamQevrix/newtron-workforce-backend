# STEP B2.4.1 — NEWTRON WORKFORCE TEAM BACKEND AUDIT

## 1. Executive Summary
This document records a comprehensive audit of the Spring Boot backend architecture of the **Newtron Workforce** system. The goal of this audit is to identify reusable database models, security constraints, and API contract interfaces to design the database migrations and JPA entity mappings for the future **Team Registration** (B2.4.2) module.

## 2. Current Backend Architecture
The backend is built as a Spring Boot application using Java 17, Spring Data JPA, Hibernate, MySQL, and Flyway.
- **Auditing Framework**: Entities inherit from [BaseEntity](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/common/entity/BaseEntity.java) which includes auditing annotations (`@CreatedDate`, `@LastModifiedDate`, `@CreatedBy`, `@LastModifiedBy`), `@Version` optimistic locking, and a boolean `deleted` soft-delete flag.
- **Request Trace Context**: Employs request ID tracking headers (`X-Request-ID`) mapped via `RequestContext`.

## 3. Flyway & Database Audit
All schema changes are versioned using Flyway migrations located under [db/migration](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/resources/db/migration/).
- **Latest Flyway Version**: `V22__add_password_hash_column.sql`. The next migration version must start at `V23`.
- **Database Tables Audited**:
  - `users`: Managed by [User](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/auth/entity/User.java) entity. Primary Key is `id` (`BIGINT AUTO_INCREMENT`), has a unique `uuid` (`VARCHAR(36)`), unique `mobile` (`VARCHAR(15)`), and unique `email` (`VARCHAR(150)`).
  - `worker_profiles`: One-to-one relationship with `users` (`user_id BIGINT UNIQUE`). Contains fields for `full_name`, `date_of_birth`, `gender`, `blood_group`, and `photo_storage_key`.
  - `worker_professional_details`: One-to-one mapping with `worker_profiles`. References `highest_qualification_id` from master qualifications.
  - `worker_skills`: Maps worker profiles to master skills with experience detail columns.
  - `worker_addresses`: One-to-one address mapping including latitude, longitude, and foreign keys referencing location masters.
  - `master_states`, `master_districts`, `master_cities`: Master tables containing geography indexes seeded in `V11`.

## 4. Existing Team / Group / Organization Findings
A scan of all backend files using pattern queries was performed.
- **Table Results**:
  - **A. Is there any Team-related table?** **NO**. No tables or schemas mapping team members, team categories, or team metadata exist.
  - **B. Is there a reusable Organization/Group entity?** **NO**. The backend has no organization, group, or corporate hierarchy entities.
  - **C. Where should a Team logically fit?** A team must exist as a new independent module with:
    1. A `teams` entity containing metadata, location foreign keys, and a one-to-one link to a `worker_profiles` ID representing the Team Owner.
    2. A `team_members` entity containing member details (Full Name, Mobile Number, Experience details) linked to the primary `teams` table.
- **Naming Conventions**: Auditing fields (`created_by`, `updated_by`) and indexes follow the pattern `idx_[table_name]_[column_name]`. Foreign keys follow `fk_[child_table]_[parent_table]`.

## 5. User & Authentication Model
- **User ID Type**: `Long` (backed by database auto-increment `BIGINT` mapped as `id` in `users`).
- **Authenticated Role Values**: Handled by the `Role` enum with values `WORKER`, `RECRUITER`, and `ADMIN`.
- **Retrieving Current Authenticated User**: Resolves the current user's session details using Spring Security's `@AuthenticationPrincipal UserDetails`. The principal username maps to the user's registered mobile number (`mobile`). The user record is retrieved via `UserRepository.findByMobile(userDetails.getUsername())`.
- **Team Owner representation**: The Team Owner is represented by the existing `WORKER` role. No new role values are required, keeping ownership decoupled from roles.

## 6. Existing Worker Profile Audit
- **Reusing Worker Profile**: Yes. The `worker_profiles` record maps directly to the active worker user. Since the Team Owner is an authenticated worker, their corresponding `worker_profiles` record can be referenced as the foreign key owner (`owner_worker_profile_id`).
- **Team Member Accounts**: No, Team Members do not need to be authenticated Users. Creating separate authenticated accounts for each member is not supported because it would require SMS verification, password configuration, and credentials management. Instead, Team Members should be stored in a flat `team_members` table linked to the Team entity.

## 7. Category & Skill Audit
- **Category Model**: No generic category entity exists. The master skills list (`master_skills`) acts as a flat categories reference (e.g., Electrician, Plumber, Carpenter, Mason).
- **Referencing Category/Skill**: The `primaryWorkCategory` entered for the Team and the `primarySkillOrWorkRole` entered for members can reference entries in the `master_skills` table.

## 8. Location Audit
- **Existing Architecture**: Mapped via `master_states`, `master_districts`, and `master_cities` tables. `WorkerAddress` stores the foreign keys `state_id`, `district_id`, and `city_id` pointing to these master tables.
- **Team Location Reuse**: The team's `state`, `cityDistrict`, and `workAreaAddress` should reuse this model. The team table should store `state_id`, `district_id`, and `city_id` foreign keys, while capturing `workAreaAddress` as a text address field.

## 9. Existing API & Response Patterns
- **Base Route Prefix**: `/api/v1/worker`
- **Response Wrapper**: All controller responses are wrapped in the standard `ApiResponse<T>` using static factory methods in `ApiResponseFactory`.
- **Validation**: Enforced using `@Valid` on request bodies. Fields utilize custom annotations like `@ValidMobile`.
- **Custom Exceptions**: Uses domain-specific runtime exceptions mapped by `GlobalExceptionHandler`. For example:
  - `ResourceNotFoundException` (HttpStatus.NOT_FOUND)
  - `ConflictException` (HttpStatus.CONFLICT)
  - `ValidationException` (HttpStatus.UNPROCESSABLE_ENTITY)
  - `BusinessException` (HttpStatus.BAD_REQUEST)

## 10. Security & Ownership Patterns
- **Endpoint Protection**: Mapped in `SecurityConfig`. Paths starting with `/api/v1/worker/**` require the `ROLE_WORKER` authority.
- **Ownership Verification**: Endpoint methods must resolve the current authenticated `User`'s profile and compare it with the Team's `owner_worker_profile_id`. If they do not match, the controller throws a `ForbiddenException`.

## 11. Duplicate / Data Integrity Patterns
- **Unique Validation**: Uniqueness checks (e.g., mobile verification) query the repository and throw a `ConflictException` if a record exists.
- **Team Duplicate Mobile Check**: The `team_members` table should enforce a unique constraint on the composite key `(team_id, mobile_number)` to prevent duplicate mobile numbers within the same team. However, it should allow different teams to register members with the same mobile numbers.

## 12. Frontend B2 vs Backend Contract Gap Analysis
- **Location Fields**: Frontend captures `State` and `City / District` as text inputs. The backend expects references to location master tables.
  - *Recommendation*: Introduce an endpoint to search or retrieve states and districts, allowing the frontend to send the corresponding `stateId` and `cityId` to the backend.
- **Skill Fields**: Frontend captures skills as text strings (e.g., "Mason"). The backend uses IDs from `master_skills`.
  - *Recommendation*: The frontend should query master skills via `/api/v1/master/skills` to retrieve IDs, or the backend should map string skill names to their corresponding master skill IDs during registration.

## 13. Reusable Existing Components
- **Entities**: `User`, `WorkerProfile`, `Skill` (master_skills), `WorkerAddress` (geography models).
- **Infrastructure**: `BaseEntity`, `ApiResponse`, `ApiResponseFactory`, `GlobalExceptionHandler`, `JwtAuthenticationFilter`, `JwtPrincipal`.

## 14. New Components That Will Be Required
- **Database Tables**:
  - `teams` (holds Team details, address FKs, and the owner profile ID)
  - `team_members` (holds member records with a foreign key to `teams`)
- **Java Code**:
  - `Team` and `TeamMember` entities.
  - `TeamRepository` and `TeamMemberRepository`.
  - `TeamService`, `TeamServiceImpl`, and `TeamController`.
  - DTOs: `TeamRegistrationRequest`, `TeamRegistrationResponse`, `TeamMemberDto`.

## 15. Risks / Unknowns
- **Member Duplicate Mobile Integrity**: If a member registered on Team A later registers as an individual worker or joins Team B, it could create data integrity issues.
  - *Mitigation*: The unique constraint on mobile numbers should only apply within a single team (`team_id, mobile_number`). If a member registers as an individual worker later, their account will be separate from the team member record.

## 16. Recommended B2.4.2 Design Decisions
- **Next Flyway Version**: `V23__create_team_tables.sql`
- **Ownership Model**: One `WorkerProfile` can own only one `Team` (enforced via a unique constraint on the team's owner column).
- **Member Model**: Team members do not receive user accounts. They are stored as flat records in the `team_members` table.

## 17. Exact Files Inspected
- [V1__create_users_table.sql](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/resources/db/migration/V1__create_users_table.sql)
- [V4__alter_users_role_enum.sql](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/resources/db/migration/V4__alter_users_role_enum.sql)
- [V7__create_worker_profiles_table.sql](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/resources/db/migration/V7__create_worker_profiles_table.sql)
- [V8__create_worker_professional_details.sql](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/resources/db/migration/V8__create_worker_professional_details.sql)
- [V9__alter_worker_addresses_add_fields.sql](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/resources/db/migration/V9__alter_worker_addresses_add_fields.sql)
- [V10__create_location_master_tables.sql](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/resources/db/migration/V10__create_location_master_tables.sql)
- [User.java](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/auth/entity/User.java)
- [Role.java](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/auth/enums/Role.java)
- [WorkerProfile.java](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/entity/WorkerProfile.java)
- [Skill.java](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/entity/Skill.java)
- [WorkerSkill.java](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/entity/WorkerSkill.java)
- [WorkerAddress.java](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/entity/WorkerAddress.java)
- [JwtPrincipal.java](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/security/jwt/JwtPrincipal.java)
- [SecurityConfig.java](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/security/config/SecurityConfig.java)
- [SecurityConstants.java](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/security/config/SecurityConstants.java)
- [WorkerProfileController.java](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/controller/WorkerProfileController.java)
- [ApiResponseFactory.java](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/common/response/ApiResponseFactory.java)
- [BaseEntity.java](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/common/entity/BaseEntity.java)
- [GlobalExceptionHandler.java](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/common/exception/GlobalExceptionHandler.java)

## 18. Exact Files Modified
- **NONE** (except this audit documentation file: [STEP_B2_4_1_TEAM_BACKEND_AUDIT.md](file:///C:/Users/HP/.gemini/antigravity-ide/brain/3bbb2a28-3dc8-4e3d-92cd-bc81cbe1e1c0/STEP_B2_4_1_TEAM_BACKEND_AUDIT.md))

---

**Readiness for B2.4.2**: **YES**
