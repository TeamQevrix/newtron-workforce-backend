package com.newtron.newtron_workforce_backend.repository;

import com.newtron.newtron_workforce_backend.entity.WorkerDocument;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkerDocumentRepository extends JpaRepository<WorkerDocument, Long> {
    Optional<WorkerDocument> findByWorkerProfileIdAndDocumentType(Long workerProfileId, String documentType);
    boolean existsByWorkerProfileIdAndDocumentType(Long workerProfileId, String documentType);
}
