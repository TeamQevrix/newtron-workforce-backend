package com.newtron.newtron_workforce_backend.service;

import com.newtron.newtron_workforce_backend.entity.WorkerMembership;
import com.newtron.newtron_workforce_backend.entity.WorkerPaymentHistory;
import com.newtron.newtron_workforce_backend.repository.WorkerPaymentHistoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Service
@RequiredArgsConstructor
public class WorkerPaymentHistoryService {

    private final WorkerPaymentHistoryRepository workerPaymentHistoryRepository;

    @Transactional
    public void recordPaymentSuccess(WorkerMembership membership, String paymentId, String orderId, Long amount, String currency, String paymentMethod) {
        if (orderId == null) {
            return; // Cannot record without an order ID for idempotency
        }
        
        boolean exists = workerPaymentHistoryRepository.existsByOrderId(orderId);
        if (exists) {
            return; // Idempotency check: already recorded
        }

        WorkerPaymentHistory history = WorkerPaymentHistory.builder()
                .workerProfile(membership.getWorkerProfile())
                .membership(membership)
                .paymentId(paymentId)
                .orderId(orderId)
                .amount(amount)
                .currency(currency)
                .status("SUCCESS")
                .paidAt(LocalDateTime.now())
                .paymentMethod(paymentMethod)
                .build();

        workerPaymentHistoryRepository.save(history);
    }

    @Transactional(readOnly = true)
    public List<WorkerPaymentHistory> getWorkerPaymentHistory(Long workerProfileId) {
        return workerPaymentHistoryRepository.findByWorkerProfileIdOrderByPaidAtDesc(workerProfileId);
    }
}
