package com.newtron.newtron_workforce_backend.repository;

import com.newtron.newtron_workforce_backend.entity.WorkerMembership;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface WorkerMembershipRepository extends JpaRepository<WorkerMembership, Long> {
    Optional<WorkerMembership> findByWorkerProfileId(Long workerProfileId);
    Optional<WorkerMembership> findByPaymentOrderId(String paymentOrderId);
    boolean existsByWorkerProfileIdAndStatus(Long workerProfileId, String status);
}
