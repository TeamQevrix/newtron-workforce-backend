package com.newtron.newtron_workforce_backend.repository;

import com.newtron.newtron_workforce_backend.entity.SavedJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface SavedJobRepository extends JpaRepository<SavedJob, Long> {
    
    Optional<SavedJob> findByWorkerIdAndJobId(Long workerId, Long jobId);

    @Query(value = "SELECT s FROM SavedJob s JOIN FETCH s.job j LEFT JOIN FETCH j.recruiter r WHERE s.worker.id = :workerId",
           countQuery = "SELECT COUNT(s) FROM SavedJob s WHERE s.worker.id = :workerId")
    org.springframework.data.domain.Page<SavedJob> findByWorkerIdWithJobAndRecruiter(@Param("workerId") Long workerId, org.springframework.data.domain.Pageable pageable);

    void deleteByWorkerIdAndJobId(Long workerId, Long jobId);

    @Query("SELECT COUNT(s) FROM SavedJob s JOIN s.job j WHERE s.worker.id = :workerId")
    long countByWorkerId(@Param("workerId") Long workerId);
}
