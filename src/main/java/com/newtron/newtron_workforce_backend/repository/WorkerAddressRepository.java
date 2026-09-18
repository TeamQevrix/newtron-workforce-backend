package com.newtron.newtron_workforce_backend.repository;

import com.newtron.newtron_workforce_backend.entity.WorkerAddress;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;
import java.util.List;

@Repository
public interface WorkerAddressRepository extends JpaRepository<WorkerAddress, Long> {

    Optional<WorkerAddress> findByWorkerProfileIdAndDeletedFalse(Long workerProfileId);

    boolean existsByWorkerProfileIdAndDeletedFalse(Long workerProfileId);

    default Optional<WorkerAddress> findByWorkerProfileId(Long workerProfileId) {
        return findByWorkerProfileIdAndDeletedFalse(workerProfileId);
    }

    default boolean existsByWorkerProfileId(Long workerProfileId) {
        return existsByWorkerProfileIdAndDeletedFalse(workerProfileId);
    }

    @org.springframework.data.jpa.repository.Query("SELECT a FROM WorkerAddress a WHERE a.workerProfile.id IN :profileIds AND a.deleted = false")
    List<WorkerAddress> findByWorkerProfileIdIn(@org.springframework.data.repository.query.Param("profileIds") List<Long> profileIds);
}
