package com.newtron.newtron_workforce_backend.repository;

import com.newtron.newtron_workforce_backend.entity.WorkerBank;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkerBankRepository extends JpaRepository<WorkerBank, Long> {
    Optional<WorkerBank> findByWorkerProfileId(Long workerProfileId);
}
