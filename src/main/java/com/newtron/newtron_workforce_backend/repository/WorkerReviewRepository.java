package com.newtron.newtron_workforce_backend.repository;

import com.newtron.newtron_workforce_backend.entity.WorkerReview;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkerReviewRepository extends JpaRepository<WorkerReview, Long> {

    Optional<WorkerReview> findByApplicationIdAndDeletedFalse(Long applicationId);

    java.util.List<WorkerReview> findByApplicationIdInAndDeletedFalse(java.util.List<Long> applicationIds);

    @Query("""
        SELECT AVG(r.rating)
        FROM WorkerReview r
        WHERE r.worker.id = :workerId
          AND r.deleted = false
    """)
    Double findAverageRatingByWorkerId(@Param("workerId") Long workerId);

    @Query("""
        SELECT r.worker.id, AVG(r.rating)
        FROM WorkerReview r
        WHERE r.worker.id IN :workerIds
          AND r.deleted = false
        GROUP BY r.worker.id
    """)
    java.util.List<Object[]> findAverageRatingsByWorkerIds(@Param("workerIds") java.util.List<Long> workerIds);
}
