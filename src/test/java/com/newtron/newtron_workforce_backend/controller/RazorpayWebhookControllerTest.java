package com.newtron.newtron_workforce_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtron.newtron_workforce_backend.entity.WorkerMembership;
import com.newtron.newtron_workforce_backend.repository.RazorpayWebhookLogRepository;
import com.newtron.newtron_workforce_backend.repository.WorkerMembershipRepository;
import com.newtron.newtron_workforce_backend.service.WorkerPaymentHistoryService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "razorpay.webhook-secret=test_secret_123"
})
public class RazorpayWebhookControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private RazorpayWebhookLogRepository webhookLogRepository;

    @MockBean
    private WorkerMembershipRepository workerMembershipRepository;

    @MockBean
    private WorkerPaymentHistoryService workerPaymentHistoryService;

    private static final String SECRET = "test_secret_123";

    private String generateSignature(String payload) throws Exception {
        Mac mac = Mac.getInstance("HmacSHA256");
        SecretKeySpec secretKeySpec = new SecretKeySpec(SECRET.getBytes(), "HmacSHA256");
        mac.init(secretKeySpec);
        byte[] hmacBytes = mac.doFinal(payload.getBytes());
        StringBuilder hexString = new StringBuilder();
        for (byte b : hmacBytes) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }

    private WorkerMembership createMembership(String status, String orderId, String paymentId, Long amount, String currency) {
        WorkerMembership membership = WorkerMembership.builder()
                .amount(amount)
                .currency(currency)
                .status(status)
                .paymentOrderId(orderId)
                .paymentPaymentId(paymentId)
                .build();
        return membership;
    }

    // 1. INVALID SIGNATURE
    @Test
    void testInvalidSignature() throws Exception {
        String payload = "{\"id\":\"evt_1\",\"event\":\"payment.captured\"}";
        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                .header("X-Razorpay-Signature", "invalid_sig")
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isBadRequest())
                .andExpect(content().string("Invalid signature"));

        Mockito.verify(webhookLogRepository, Mockito.never()).save(any());
    }

    // 2. NEW payment.captured
    @Test
    void testPaymentCapturedNewEvent() throws Exception {
        String payload = """
            {
              "id": "evt_new_1",
              "event": "payment.captured",
              "payload": {
                "payment": {
                  "entity": {
                    "id": "pay_123",
                    "order_id": "order_123",
                    "status": "captured",
                    "amount": 4900,
                    "currency": "INR",
                    "method": "upi"
                  }
                }
              }
            }
            """;
        String signature = generateSignature(payload);

        WorkerMembership membership = createMembership("PENDING", "order_123", null, 4900L, "INR");
        Mockito.when(webhookLogRepository.existsByEventId("evt_new_1")).thenReturn(false);
        Mockito.when(workerMembershipRepository.findByPaymentOrderIdWithLock("order_123")).thenReturn(Optional.of(membership));

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                .header("X-Razorpay-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));

        ArgumentCaptor<WorkerMembership> captor = ArgumentCaptor.forClass(WorkerMembership.class);
        Mockito.verify(workerMembershipRepository).save(captor.capture());
        WorkerMembership saved = captor.getValue();
        assertEquals("ACTIVE", saved.getStatus());
        assertEquals("pay_123", saved.getPaymentPaymentId());
        assertNotNull(saved.getActivatedAt());
        assertNotNull(saved.getExpiresAt());

        Mockito.verify(workerPaymentHistoryService).recordPaymentSuccess(
                eq(membership), eq("pay_123"), eq("order_123"), eq(4900L), eq("INR"), eq("upi"));
    }

    // 3. DUPLICATE EVENT ID
    @Test
    void testDuplicateEventId() throws Exception {
        String payload = "{\"id\":\"evt_dup\",\"event\":\"payment.captured\"}";
        String signature = generateSignature(payload);

        Mockito.when(webhookLogRepository.existsByEventId("evt_dup")).thenReturn(true);

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                .header("X-Razorpay-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().string("OK"));

        Mockito.verify(webhookLogRepository, Mockito.never()).save(any());
        Mockito.verify(workerMembershipRepository, Mockito.never()).findByPaymentOrderIdWithLock(any());
    }

    // 4. PAYMENT ID IDEMPOTENCY
    @Test
    void testPaymentIdIdempotency() throws Exception {
        String payload = """
            {
              "id": "evt_new_2",
              "event": "payment.captured",
              "payload": {
                "payment": {
                  "entity": {
                    "id": "pay_123",
                    "order_id": "order_123",
                    "status": "captured",
                    "amount": 4900,
                    "currency": "INR"
                  }
                }
              }
            }
            """;
        String signature = generateSignature(payload);

        WorkerMembership membership = createMembership("ACTIVE", "order_123", "pay_123", 4900L, "INR");
        Mockito.when(webhookLogRepository.existsByEventId("evt_new_2")).thenReturn(false);
        Mockito.when(workerMembershipRepository.findByPaymentOrderIdWithLock("order_123")).thenReturn(Optional.of(membership));

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                .header("X-Razorpay-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk());

        Mockito.verify(workerMembershipRepository, Mockito.never()).save(any());
        Mockito.verify(workerPaymentHistoryService, Mockito.never()).recordPaymentSuccess(any(), any(), any(), any(), any(), any());
    }

    // 5. RENEWAL
    @Test
    void testRenewal() throws Exception {
        String payload = """
            {
              "id": "evt_ren_1",
              "event": "payment.captured",
              "payload": {
                "payment": {
                  "entity": {
                    "id": "pay_456",
                    "order_id": "order_123",
                    "status": "captured",
                    "amount": 4900,
                    "currency": "INR",
                    "method": "card"
                  }
                }
              }
            }
            """;
        String signature = generateSignature(payload);

        WorkerMembership membership = createMembership("ACTIVE", "order_123", "pay_123", 4900L, "INR");
        LocalDateTime oldExpiresAt = LocalDateTime.now().plusDays(10);
        membership.setActivatedAt(LocalDateTime.now().minusDays(20));
        membership.setExpiresAt(oldExpiresAt);

        Mockito.when(webhookLogRepository.existsByEventId("evt_ren_1")).thenReturn(false);
        Mockito.when(workerMembershipRepository.findByPaymentOrderIdWithLock("order_123")).thenReturn(Optional.of(membership));

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                .header("X-Razorpay-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk());

        ArgumentCaptor<WorkerMembership> captor = ArgumentCaptor.forClass(WorkerMembership.class);
        Mockito.verify(workerMembershipRepository).save(captor.capture());
        WorkerMembership saved = captor.getValue();
        
        assertEquals("pay_456", saved.getPaymentPaymentId());
        assertEquals(oldExpiresAt.plusMonths(1), saved.getExpiresAt());
        
        Mockito.verify(workerPaymentHistoryService).recordPaymentSuccess(
                eq(membership), eq("pay_456"), eq("order_123"), eq(4900L), eq("INR"), eq("card"));
    }

    // 6. WRONG AMOUNT
    @Test
    void testWrongAmount() throws Exception {
        String payload = """
            {
              "id": "evt_amt_1",
              "event": "payment.captured",
              "payload": {
                "payment": {
                  "entity": {
                    "id": "pay_123",
                    "order_id": "order_123",
                    "status": "captured",
                    "amount": 9999,
                    "currency": "INR"
                  }
                }
              }
            }
            """;
        String signature = generateSignature(payload);

        WorkerMembership membership = createMembership("PENDING", "order_123", null, 4900L, "INR");
        Mockito.when(webhookLogRepository.existsByEventId("evt_amt_1")).thenReturn(false);
        Mockito.when(workerMembershipRepository.findByPaymentOrderIdWithLock("order_123")).thenReturn(Optional.of(membership));

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                .header("X-Razorpay-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk());

        Mockito.verify(workerMembershipRepository, Mockito.never()).save(any());
    }

    // 7. WRONG CURRENCY
    @Test
    void testWrongCurrency() throws Exception {
        String payload = """
            {
              "id": "evt_cur_1",
              "event": "payment.captured",
              "payload": {
                "payment": {
                  "entity": {
                    "id": "pay_123",
                    "order_id": "order_123",
                    "status": "captured",
                    "amount": 4900,
                    "currency": "USD"
                  }
                }
              }
            }
            """;
        String signature = generateSignature(payload);

        WorkerMembership membership = createMembership("PENDING", "order_123", null, 4900L, "INR");
        Mockito.when(webhookLogRepository.existsByEventId("evt_cur_1")).thenReturn(false);
        Mockito.when(workerMembershipRepository.findByPaymentOrderIdWithLock("order_123")).thenReturn(Optional.of(membership));

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                .header("X-Razorpay-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk());

        Mockito.verify(workerMembershipRepository, Mockito.never()).save(any());
    }

    // 8. WRONG PAYMENT STATUS
    @Test
    void testWrongPaymentStatus() throws Exception {
        String payload = """
            {
              "id": "evt_stat_1",
              "event": "payment.captured",
              "payload": {
                "payment": {
                  "entity": {
                    "id": "pay_123",
                    "order_id": "order_123",
                    "status": "authorized",
                    "amount": 4900,
                    "currency": "INR"
                  }
                }
              }
            }
            """;
        String signature = generateSignature(payload);

        WorkerMembership membership = createMembership("PENDING", "order_123", null, 4900L, "INR");
        Mockito.when(webhookLogRepository.existsByEventId("evt_stat_1")).thenReturn(false);
        
        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                .header("X-Razorpay-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk());

        Mockito.verify(workerMembershipRepository, Mockito.never()).save(any());
    }

    // 9. payment.failed
    @Test
    void testPaymentFailed() throws Exception {
        String payload = """
            {
              "id": "evt_fail_1",
              "event": "payment.failed",
              "payload": {
                "payment": {
                  "entity": {
                    "id": "pay_123",
                    "order_id": "order_123"
                  }
                }
              }
            }
            """;
        String signature = generateSignature(payload);

        Mockito.when(webhookLogRepository.existsByEventId("evt_fail_1")).thenReturn(false);

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                .header("X-Razorpay-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk());

        Mockito.verify(webhookLogRepository).save(any());
        Mockito.verify(workerMembershipRepository, Mockito.never()).save(any());
        Mockito.verify(workerPaymentHistoryService, Mockito.never()).recordPaymentSuccess(any(), any(), any(), any(), any(), any());
    }

    // 10. MISSING ORDER ID / PAYMENT ID
    @Test
    void testMissingOrderIdOrPaymentId() throws Exception {
        String payload = """
            {
              "id": "evt_miss_1",
              "event": "payment.captured",
              "payload": {
                "payment": {
                  "entity": {
                    "status": "captured",
                    "amount": 4900,
                    "currency": "INR"
                  }
                }
              }
            }
            """;
        String signature = generateSignature(payload);

        Mockito.when(webhookLogRepository.existsByEventId("evt_miss_1")).thenReturn(false);

        mockMvc.perform(post("/api/v1/webhooks/razorpay")
                .header("X-Razorpay-Signature", signature)
                .contentType(MediaType.APPLICATION_JSON)
                .content(payload))
                .andExpect(status().isOk());

        Mockito.verify(workerMembershipRepository, Mockito.never()).save(any());
    }
}
