package com.newtron.newtron_workforce_backend.repository;

import com.newtron.newtron_workforce_backend.entity.WorkerProfessionalDetails;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkerProfessionalDetailsRepository extends JpaRepository<WorkerProfessionalDetails, Long> {
    Optional<WorkerProfessionalDetails> findByWorkerProfileId(Long workerProfileId);
    boolean existsByWorkerProfileId(Long workerProfileId);
}
