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
}
