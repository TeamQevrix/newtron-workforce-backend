package com.newtron.newtron_workforce_backend.repository;

import com.newtron.newtron_workforce_backend.entity.NotInterestedJob;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface NotInterestedJobRepository extends JpaRepository<NotInterestedJob, Long> {

    Optional<NotInterestedJob> findByWorkerIdAndJobId(Long workerId, Long jobId);

    boolean existsByWorkerIdAndJobId(Long workerId, Long jobId);

    @Query("SELECT n.job.id FROM NotInterestedJob n WHERE n.worker.id = :workerId")
    List<Long> findHiddenJobIdsByWorkerId(@Param("workerId") Long workerId);
}
