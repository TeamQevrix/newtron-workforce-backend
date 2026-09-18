package com.newtron.newtron_workforce_backend.repository;

import com.newtron.newtron_workforce_backend.entity.WorkerEarning;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Optional;

@Repository
public interface WorkerEarningRepository extends JpaRepository<WorkerEarning, Long> {

    Optional<WorkerEarning> findByApplicationId(Long applicationId);

    java.util.List<WorkerEarning> findByApplicationIdInAndDeletedFalse(java.util.List<Long> applicationIds);

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
        FROM WorkerEarning e
        WHERE e.worker.id = :workerId
          AND e.deleted = false
    """)
    BigDecimal sumTotalEarningsByWorkerId(@Param("workerId") Long workerId);

    @Query("""
        SELECT COALESCE(SUM(e.amount), 0)
        FROM WorkerEarning e
        WHERE e.worker.id = :workerId
          AND e.earningDate >= :start
          AND e.earningDate < :end
          AND e.deleted = false
    """)
    BigDecimal sumEarningsByWorkerIdAndDateRange(
        @Param("workerId") Long workerId,
        @Param("start") Instant start,
        @Param("end") Instant end
    );
}
