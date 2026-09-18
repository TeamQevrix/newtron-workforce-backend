# STEP B2.5.3 — TEAM & TEAM MEMBER JPA REPOSITORIES AUDIT REPORT

## 1. Executive Summary
This document records the implementation of Spring Data JPA repositories for the Team Registration module, aligning with the project's architecture and soft-delete conventions.

## 2. Files Inspected
- [WorkerProfileRepository.java](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/repository/WorkerProfileRepository.java)
- [CompanyRepository.java](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/repository/CompanyRepository.java)

## 3. Files Created
1. [TeamRepository.java](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/repository/TeamRepository.java) [NEW]
2. [TeamMemberRepository.java](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/repository/TeamMemberRepository.java) [NEW]

## 4. TeamRepository Methods
Extends `JpaRepository<Team, Long>` and provides active team lookups (`deleted = false`):
- `Optional<Team> findByUuid(String uuid)`: Resolves an active team using its global UUID.
- `Optional<Team> findByOwnerWorkerProfileId(Long profileId)`: Resolves the active team owned by a given `WorkerProfile`.
- `boolean existsByOwnerWorkerProfileIdAndDeletedFalse(Long profileId)`: Performs an active ownership check. Mapped as `existsByOwnerWorkerProfileId(profileId)` as a default facade method for convenience.

## 5. TeamMemberRepository Methods
Extends `JpaRepository<TeamMember, Long>` and provides active team member listing and checks:
- `List<TeamMember> findAllByTeamId(Long teamId)`: Resolves all active members within a parent team.
- `Optional<TeamMember> findByTeamIdAndId(Long teamId, Long memberId)`: Resolves an active member details lookup within a team.
- `boolean existsByTeamIdAndMobileNumberAndDeletedFalse(Long teamId, String mobileNumber)`: Validates that a mobile number is not registered as an active member within the same team.
- `long countByTeamId(Long teamId)`: Counts all active members currently mapped under a team.

## 6. Soft-Delete Strategy
To align with the project's repository layer conventions:
- Custom JQL `@Query` filters are declared explicitly in the repository methods to enforce `deleted = false` checks.
- No physical SQL DELETE method endpoints or custom `deleteBy...` patterns are introduced, keeping soft-deletions managed cleanly in the service layer.

## 7. Active Team & Member Lookup Strategy
- All search and checking routines explicitly include `deleted = false` clauses. Soft-deleted teams and members are omitted from query results.

## 8. Duplicate Mobile Scope
- The mobile uniqueness lookup index constraint checks are restricted to the parent team scope (`team_id`). A mobile number can join different teams as long as it is not duplicated within the same parent team.

## 9. Files Explicitly Not Modified
No other production entities, repository classes, or database schemas were modified.

## 10. Compile Result
- **Result**: Passed (`mvn compile` exited with code 0).

## 11. Ready for B2.5.4?
- **YES** (The repository layer is compiled and ready for DTO mapping setups).
