package com.newtron.newtron_workforce_backend.repository;

import com.newtron.newtron_workforce_backend.entity.WorkerMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import jakarta.persistence.LockModeType;
import org.springframework.data.jpa.repository.Lock;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

@Repository
public interface WorkerMembershipRepository extends JpaRepository<WorkerMembership, Long> {
    Optional<WorkerMembership> findByWorkerProfileId(Long workerProfileId);
    Optional<WorkerMembership> findByPaymentOrderId(String paymentOrderId);
    boolean existsByWorkerProfileIdAndStatus(Long workerProfileId, String status);

    @Lock(LockModeType.PESSIMISTIC_WRITE)
    @Query("SELECT wm FROM WorkerMembership wm WHERE wm.paymentOrderId = :orderId")
    Optional<WorkerMembership> findByPaymentOrderIdWithLock(@Param("orderId") String orderId);
}
