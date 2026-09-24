package com.newtron.newtron_workforce_backend.repository;

import com.newtron.newtron_workforce_backend.entity.Settlement;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@Repository
public interface SettlementRepository extends JpaRepository<Settlement, Long> {

    Optional<Settlement> findByWorkOrderIdAndSettlementMonthAndSettlementYearAndDeletedFalse(
            Long workOrderId, Integer settlementMonth, Integer settlementYear);

    @Query(value = "SELECT s FROM Settlement s JOIN FETCH s.workOrder w JOIN FETCH w.worker WHERE w.company.id = :companyId AND s.deleted = false " +
            "AND (:month IS NULL OR s.settlementMonth = :month) " +
            "AND (:year IS NULL OR s.settlementYear = :year) " +
            "AND (:workOrderId IS NULL OR w.id = :workOrderId) " +
            "ORDER BY s.id DESC",
           countQuery = "SELECT count(s) FROM Settlement s WHERE s.workOrder.company.id = :companyId AND s.deleted = false " +
            "AND (:month IS NULL OR s.settlementMonth = :month) " +
            "AND (:year IS NULL OR s.settlementYear = :year) " +
            "AND (:workOrderId IS NULL OR s.workOrder.id = :workOrderId)")
    Page<Settlement> findByCompanyIdWithFilters(
            @Param("companyId") Long companyId,
            @Param("month") Integer month,
            @Param("year") Integer year,
            @Param("workOrderId") Long workOrderId,
            Pageable pageable);

    @Query("SELECT s FROM Settlement s JOIN FETCH s.workOrder w JOIN FETCH w.worker WHERE s.id = :id AND w.company.id = :companyId AND s.deleted = false")
    Optional<Settlement> findByIdAndCompanyId(@Param("id") Long id, @Param("companyId") Long companyId);
}
