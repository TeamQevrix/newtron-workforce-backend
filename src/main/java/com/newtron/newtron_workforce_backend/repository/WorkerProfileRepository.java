package com.newtron.newtron_workforce_backend.repository;

import com.newtron.newtron_workforce_backend.entity.WorkerProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkerProfileRepository extends JpaRepository<WorkerProfile, Long> {

    boolean existsByUserIdAndDeletedFalse(Long userId);

    default boolean existsByUserId(Long userId) {
        return existsByUserIdAndDeletedFalse(userId);
    }

    @Query("SELECT wp FROM WorkerProfile wp WHERE wp.user.id = :userId AND wp.deleted = false")
    Optional<WorkerProfile> findByUserId(@Param("userId") Long userId);

    @Query("SELECT wp FROM WorkerProfile wp WHERE wp.user.uuid = :uuid AND wp.deleted = false")
    Optional<WorkerProfile> findByUserUuid(@Param("uuid") String uuid);

    @Query("SELECT wp FROM WorkerProfile wp WHERE wp.user.mobile = :mobile AND wp.deleted = false")
    Optional<WorkerProfile> findByUserMobile(@Param("mobile") String mobile);

    @Query("SELECT wp FROM WorkerProfile wp WHERE wp.user.id IN :userIds AND wp.deleted = false")
    java.util.List<WorkerProfile> findByUserIds(@Param("userIds") java.util.List<Long> userIds);

    @Query(value = "SELECT wp.id AS workerId, " +
            "wp.full_name AS fullName, " +
            "wp.photo_storage_key AS photoStorageKey, " +
            "wp.is_available_on_demand AS isAvailableOnDemand, " +
            "( 6371 * acos( cos( radians(:latitude) ) * cos( radians( wa.latitude ) ) " +
            "* cos( radians( wa.longitude ) - radians(:longitude) ) + sin( radians(:latitude) ) * sin( radians( wa.latitude ) ) ) ) AS distanceKm, " +
            "(SELECT s.name FROM worker_skills ws JOIN master_skills s ON ws.skill_id = s.id WHERE ws.worker_profile_id = wp.id AND ws.is_primary = true AND ws.deleted = false LIMIT 1) AS mainSkill, " +
            "(SELECT ws.experience_years FROM worker_skills ws WHERE ws.worker_profile_id = wp.id AND ws.is_primary = true AND ws.deleted = false LIMIT 1) AS experienceYears " +
            "FROM worker_profiles wp " +
            "INNER JOIN users u ON wp.user_id = u.id " +
            "INNER JOIN worker_addresses wa ON wp.id = wa.worker_profile_id " +
            "WHERE wp.is_completed = true " +
            "AND wp.deleted = false " +
            "AND u.status = 'ACTIVE' " +
            "AND u.membership_active = true " +
            "AND wp.is_available_on_demand = true " +
            "AND wa.latitude IS NOT NULL " +
            "AND wa.longitude IS NOT NULL " +
            "AND NOT EXISTS (SELECT 1 FROM teams t WHERE t.owner_worker_profile_id = wp.id AND t.deleted = false) " +
            "AND (:skillId IS NULL OR EXISTS (SELECT 1 FROM worker_skills ws WHERE ws.worker_profile_id = wp.id AND ws.skill_id = :skillId AND ws.deleted = false)) " +
            "HAVING distanceKm <= :radiusKm " +
            "ORDER BY distanceKm ASC",
           countQuery = "SELECT count(*) FROM ( " +
            "SELECT wp.id, " +
            "( 6371 * acos( cos( radians(:latitude) ) * cos( radians( wa.latitude ) ) " +
            "* cos( radians( wa.longitude ) - radians(:longitude) ) + sin( radians(:latitude) ) * sin( radians( wa.latitude ) ) ) ) AS distanceKm " +
            "FROM worker_profiles wp " +
            "INNER JOIN users u ON wp.user_id = u.id " +
            "INNER JOIN worker_addresses wa ON wp.id = wa.worker_profile_id " +
            "WHERE wp.is_completed = true " +
            "AND wp.deleted = false " +
            "AND u.status = 'ACTIVE' " +
            "AND u.membership_active = true " +
            "AND wp.is_available_on_demand = true " +
            "AND wa.latitude IS NOT NULL " +
            "AND wa.longitude IS NOT NULL " +
            "AND NOT EXISTS (SELECT 1 FROM teams t WHERE t.owner_worker_profile_id = wp.id AND t.deleted = false) " +
            "AND (:skillId IS NULL OR EXISTS (SELECT 1 FROM worker_skills ws WHERE ws.worker_profile_id = wp.id AND ws.skill_id = :skillId AND ws.deleted = false)) " +
            "HAVING distanceKm <= :radiusKm " +
            ") AS nearby_workers",
           nativeQuery = true)
    org.springframework.data.domain.Page<com.newtron.newtron_workforce_backend.dto.NearbyWorkerProjection> findNearbyAvailableIndividualWorkers(
            @Param("latitude") Double latitude,
            @Param("longitude") Double longitude,
            @Param("radiusKm") Double radiusKm,
            @Param("skillId") Long skillId,
            org.springframework.data.domain.Pageable pageable);

    @Query("SELECT wp FROM WorkerProfile wp " +
           "JOIN FETCH wp.user u " +
           "LEFT JOIN FETCH wp.address wa " +
           "LEFT JOIN FETCH wp.skills ws " +
           "LEFT JOIN FETCH ws.skill s " +
           "WHERE wp.id = :workerId " +
           "AND wp.isCompleted = true " +
           "AND wp.deleted = false " +
           "AND u.status = 'ACTIVE' " +
           "AND u.membershipActive = true " +
           "AND wp.isAvailableOnDemand = true " +
           "AND NOT EXISTS (SELECT 1 FROM Team t WHERE t.ownerWorkerProfile.id = wp.id AND t.deleted = false)")
    Optional<WorkerProfile> findEligiblePublicWorkerProfileById(@Param("workerId") Long workerId);
}
