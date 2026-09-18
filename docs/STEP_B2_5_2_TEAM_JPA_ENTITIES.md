# STEP B2.5.2 — TEAM & TEAM MEMBER JPA ENTITIES AUDIT REPORT

## 1. Executive Summary
This document specifies the implementation details of the Java JPA entities for the Team Registration module, mapping the tables created in the database schema.

## 2. Files Inspected
- [WorkerProfile.java](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/entity/WorkerProfile.java)
- [WorkerAddress.java](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/entity/WorkerAddress.java)
- [BaseEntity.java](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/common/entity/BaseEntity.java)
- [User.java](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/auth/entity/User.java)

## 3. Files Created/Modified
1. [MasterState.java](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/entity/MasterState.java) [NEW]
2. [MasterDistrict.java](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/entity/MasterDistrict.java) [NEW]
3. [MasterCity.java](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/entity/MasterCity.java) [NEW]
4. [Team.java](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/entity/Team.java) [NEW]
5. [TeamMember.java](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/entity/TeamMember.java) [NEW]

## 4. Team Entity Mapping
- **Table**: `teams`
- **Fields**:
  - `uuid`: `VARCHAR(36)`, non-nullable, unique.
  - `ownerWorkerProfile`: `@OneToOne` referencing `worker_profiles`.
  - `teamName`: `VARCHAR(100)`, non-nullable mapping.
  - `primarySkill`: `@ManyToOne` referencing `master_skills` (`Skill` entity).
  - `aboutTeam`: `TEXT`, nullable.
  - `state`: `@ManyToOne` referencing `master_states` (`MasterState` entity).
  - `district`: `@ManyToOne` referencing `master_districts` (`MasterDistrict` entity).
  - `city`: `@ManyToOne` referencing `master_cities` (`MasterCity` entity).
  - `workAreaAddress`: `VARCHAR(255)`, non-nullable.
  - `members`: Bidirectional `@OneToMany` mapping referencing `TeamMember`.

## 5. TeamMember Entity Mapping
- **Table**: `team_members`
- **Fields**:
  - `team`: `@ManyToOne` referencing `teams` (`Team` entity).
  - `fullName`: `VARCHAR(100)`, non-nullable.
  - `mobileNumber`: `VARCHAR(15)`, non-nullable.
  - `primarySkill`: `@ManyToOne` referencing `master_skills` (`Skill` entity).
  - `experience`: `VARCHAR(50)`, nullable.

## 6. Relationship Decisions & Cascade
- **JPA Relationship Cardnalities**:
  - `Team` ↔ `TeamMember` uses `CascadeType.ALL` and `orphanRemoval = true` to tie member lifecycles to their parent team. Deleting a team automatically deletes its member list.
  - Associations to `WorkerProfile`, `Skill`, `MasterState`, `MasterDistrict`, and `MasterCity` use `FetchType.LAZY` and **do not** configure cascade deletes, ensuring master references are never deleted.

## 7. UUID Strategy
- Mapped to pre-generate a standard UUID string on persistence:
```java
@PrePersist
public void prePersist() {
    if (uuid == null) {
        uuid = UUID.randomUUID().toString();
    }
}
```
This is consistent with the UUID creation pattern found in the `User` entity.

## 8. active_status Generated Column Handling
The generated virtual columns `active_status` exist solely for database-level index constraints. They are left unmapped in the Java code, preventing application code from modifying them.

## 9. BaseEntity / Audit / Soft-Delete Compatibility
Both `Team` and `TeamMember` extend [BaseEntity](file:///d:/NewtronProjects/newtron-workforce-backend/newtron-workforce-backend/src/main/java/com/newtron/newtron_workforce_backend/common/entity/BaseEntity.java) to inherit BaseEntity properties:
- `createdAt`, `updatedAt`, `createdBy`, `updatedBy` audit logs.
- `version` optimistic locking.
- `deleted` boolean soft-delete flag.

## 10. Explicitly Not Modified
No other production entities (e.g. `User`, `WorkerProfile`, etc.), authentication modules, controllers, or database schemas were modified.

## 11. Compile Result
- **Result**: Passed (`mvn compile` exited with code 0).

## 12. Ready for B2.5.3?
- **YES** (The entity files are successfully compiled, and the repositories can be created).
