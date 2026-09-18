package com.newtron.newtron_workforce_backend.repository;

import com.newtron.newtron_workforce_backend.entity.Team;
import com.newtron.newtron_workforce_backend.entity.WorkerProfile;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeamRepository extends JpaRepository<Team, Long>, JpaSpecificationExecutor<Team> {

    @Query("SELECT t FROM Team t WHERE t.uuid = :uuid AND t.deleted = false")
    Optional<Team> findByUuid(@Param("uuid") String uuid);

    @Query("SELECT t FROM Team t WHERE t.ownerWorkerProfile.id = :profileId AND t.deleted = false")
    List<Team> findAllByOwnerWorkerProfileId(@Param("profileId") Long profileId);

    @Query("SELECT t FROM Team t WHERE t.id = :id AND t.ownerWorkerProfile.id = :profileId AND t.deleted = false")
    Optional<Team> findByIdAndOwnerWorkerProfileIdAndDeletedFalse(@Param("id") Long id, @Param("profileId") Long profileId);

    @Query("SELECT COUNT(t) > 0 FROM Team t WHERE t.ownerWorkerProfile.id = :profileId AND t.deleted = false")
    boolean existsByOwnerWorkerProfileIdAndDeletedFalse(@Param("profileId") Long profileId);

    default boolean existsByOwnerWorkerProfileId(Long profileId) {
        return existsByOwnerWorkerProfileIdAndDeletedFalse(profileId);
    }
}
