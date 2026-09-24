package com.newtron.newtron_workforce_backend.repository;

import com.newtron.newtron_workforce_backend.entity.WorkerPaymentHistory;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface WorkerPaymentHistoryRepository extends JpaRepository<WorkerPaymentHistory, Long> {

    List<WorkerPaymentHistory> findByWorkerProfileIdOrderByPaidAtDesc(Long workerProfileId);

    boolean existsByOrderId(String orderId);
    
    Optional<WorkerPaymentHistory> findByOrderId(String orderId);
}
