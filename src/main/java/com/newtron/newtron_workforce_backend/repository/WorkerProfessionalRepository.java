package com.newtron.newtron_workforce_backend.repository;

import com.newtron.newtron_workforce_backend.entity.WorkerProfessional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkerProfessionalRepository extends JpaRepository<WorkerProfessional, Long> {
    Optional<WorkerProfessional> findByWorkerProfileId(Long workerProfileId);
}
