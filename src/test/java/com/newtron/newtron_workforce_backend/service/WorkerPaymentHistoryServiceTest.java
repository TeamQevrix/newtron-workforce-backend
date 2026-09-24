package com.newtron.newtron_workforce_backend.service;

import com.newtron.newtron_workforce_backend.entity.WorkerMembership;
import com.newtron.newtron_workforce_backend.entity.WorkerPaymentHistory;
import com.newtron.newtron_workforce_backend.entity.WorkerProfile;
import com.newtron.newtron_workforce_backend.repository.WorkerPaymentHistoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkerPaymentHistoryServiceTest {

    @Mock
    private WorkerPaymentHistoryRepository workerPaymentHistoryRepository;

    @InjectMocks
    private WorkerPaymentHistoryService workerPaymentHistoryService;

    private WorkerMembership membership;

    @BeforeEach
    void setUp() {
        WorkerProfile profile = new WorkerProfile();
        profile.setId(1L);

        membership = new WorkerMembership();
        membership.setId(10L);
        membership.setWorkerProfile(profile);
    }

    @Test
    void recordPaymentSuccess_createsRecord_whenNotExists() {
        when(workerPaymentHistoryRepository.existsByOrderId("order_123")).thenReturn(false);

        workerPaymentHistoryService.recordPaymentSuccess(membership, "pay_123", "order_123", 4900L, "INR", "upi");

        ArgumentCaptor<WorkerPaymentHistory> captor = ArgumentCaptor.forClass(WorkerPaymentHistory.class);
        verify(workerPaymentHistoryRepository, times(1)).save(captor.capture());

        WorkerPaymentHistory saved = captor.getValue();
        assertEquals("pay_123", saved.getPaymentId());
        assertEquals("order_123", saved.getOrderId());
        assertEquals(4900L, saved.getAmount());
        assertEquals("INR", saved.getCurrency());
        assertEquals("SUCCESS", saved.getStatus());
        assertEquals("upi", saved.getPaymentMethod());
        assertNotNull(saved.getPaidAt());
        assertEquals(membership.getWorkerProfile(), saved.getWorkerProfile());
        assertEquals(membership, saved.getMembership());
    }

    @Test
    void recordPaymentSuccess_doesNotCreateDuplicate_whenExists() {
        when(workerPaymentHistoryRepository.existsByOrderId("order_dup")).thenReturn(true);

        workerPaymentHistoryService.recordPaymentSuccess(membership, "pay_dup", "order_dup", 4900L, "INR", "upi");

        verify(workerPaymentHistoryRepository, never()).save(any(WorkerPaymentHistory.class));
    }

    @Test
    void getWorkerPaymentHistory_returnsList() {
        WorkerPaymentHistory h1 = new WorkerPaymentHistory();
        h1.setId(100L);
        when(workerPaymentHistoryRepository.findByWorkerProfileIdOrderByPaidAtDesc(1L)).thenReturn(List.of(h1));

        List<WorkerPaymentHistory> result = workerPaymentHistoryService.getWorkerPaymentHistory(1L);

        assertEquals(1, result.size());
        assertEquals(100L, result.get(0).getId());
    }
}
