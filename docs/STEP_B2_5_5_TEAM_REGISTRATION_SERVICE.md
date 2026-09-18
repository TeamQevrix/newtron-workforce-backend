# STEP B2.5.5 — TEAM REGISTRATION SERVICE LAYER ONLY AUDIT REPORT

## 1. Executive Summary
This document records the business validation logic, transactional rollback behaviors, and exception handlers implemented within the new Worker Team registration service.

## 2. Files Inspected
- [WorkerAddressServiceImpl.java](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/service/WorkerAddressServiceImpl.java)
- [WorkerProfileServiceImpl.java](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/service/WorkerProfileServiceImpl.java)

## 3. Files Created
1. [MasterStateRepository.java](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/repository/MasterStateRepository.java) [NEW]
2. [MasterDistrictRepository.java](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/repository/MasterDistrictRepository.java) [NEW]
3. [MasterCityRepository.java](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/repository/MasterCityRepository.java) [NEW]
4. [WorkerTeamService.java](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/service/WorkerTeamService.java) [NEW]
5. [WorkerTeamServiceImpl.java](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/service/WorkerTeamServiceImpl.java) [NEW]

## 4. Files Explicitly Not Modified
No REST controllers, route definitions, or frontend screen scripts were modified (Service layer implementation only).

## 5. Exact Service Method
- `TeamRegistrationResponse registerTeam(TeamRegistrationRequest request, User currentUser)` declared on interface `WorkerTeamService` and implemented on `WorkerTeamServiceImpl`.

## 6. Business Validation and Rules
- **Authenticated User & Role Protection**: The API caller's identity maps to the `User` principal retrieved from the endpoint security context. If the caller's role is not `Role.WORKER`, it throws a `ForbiddenException`.
- **WorkerProfile Resolution**: Loads the profile via `workerProfileRepository.findByUserId(currentUser.getId())`. If missing, throws a `ResourceNotFoundException`.
- **One Active Team Rule**: Enforces the one active team constraint by checking `teamRepository.existsByOwnerWorkerProfileId(ownerProfile.getId())` before continuing. If a record exists, throws a `ConflictException`.
- **Skill Validation**: Enforces ID checks against the master skill table `master_skills` (using `skillRepository.findById(...)`). If invalid, throws `ValidationException`.
- **Location Hierarchy Checks**:
  - Resolves `MasterState`, `MasterDistrict`, and `MasterCity` using JPA repository finders.
  - Verifies hierarchy mapping matching parent nodes: checks that the selected `district.getState().getId()` matches `state.getId()`, and `city.getDistrict().getId()` matches `district.getId()`. If a mismatch is found, throws a `ValidationException(LOCATION_MISMATCH)`.
- **Duplicate Request Member Mobiles**:
  - Scans member list entries. If a duplicate phone number is detected within the request payload, throws `ConflictException(DUPLICATE_TEAM_MEMBER)`.
  - Verifies that no member mobile number matches the authenticated owner's registered phone number. If a match is found, throws `ValidationException(INVALID_MEMBER_MOBILE)`.

## 7. Transaction Safety
- Mapped using `@Transactional(rollbackFor = Exception.class)`. Any JPA validation errors, database constraint checks, or custom business runtime exceptions will cause the database transaction to rollback, ensuring no partial registrations are persisted.

## 8. Persisting Team / Members Relations
- Mapped bidirectional relation mappings. `TeamMember` holds reference to `Team`, and `Team` holds a collection of members configured with `CascadeType.ALL` and `orphanRemoval = true` to save members automatically with the parent team.

## 9. Compile Result
- **Result**: Passed (`mvn compile` exited with code 0).

## 10. Ready for B2.5.6?
- **YES** (The service layer compiles cleanly, and the controller mapping setup can begin).
