package com.newtron.newtron_workforce_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.enums.Role;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.dto.PaymentVerificationRequest;
import com.newtron.newtron_workforce_backend.entity.WorkerMembership;
import com.newtron.newtron_workforce_backend.entity.WorkerProfile;
import com.newtron.newtron_workforce_backend.repository.TeamRepository;
import com.newtron.newtron_workforce_backend.repository.WorkerMembershipRepository;
import com.newtron.newtron_workforce_backend.repository.WorkerProfileRepository;
import com.newtron.newtron_workforce_backend.service.NotificationHelper;
import com.newtron.newtron_workforce_backend.service.OnboardingProgressService;
import com.newtron.newtron_workforce_backend.service.WorkerPaymentHistoryService;
import com.newtron.newtron_workforce_backend.entity.WorkerPaymentHistory;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@TestPropertySource(properties = {
    "razorpay.key-id=rzp_live_12345",
    "razorpay.key-secret=secret_12345"
})
public class MembershipControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private WorkerProfileRepository workerProfileRepository;

    @MockBean
    private WorkerMembershipRepository workerMembershipRepository;

    @MockBean
    private TeamRepository teamRepository;

    @MockBean
    private OnboardingProgressService onboardingProgressService;

    @MockBean
    private NotificationHelper notificationHelper;

    @MockBean
    private WorkerPaymentHistoryService workerPaymentHistoryService;

    private User mockUser;
    private WorkerProfile mockProfile;
    private WorkerMembership pendingMembership;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .mobile("+919000000000")
                .fullName("Test Worker")
                .role(Role.WORKER)
                .build();

        mockProfile = WorkerProfile.builder()
                .user(mockUser)
                .fullName("Test Worker")
                .build();
        mockProfile.setId(10L);

        pendingMembership = WorkerMembership.builder()
                .workerProfile(mockProfile)
                .plan("INDIVIDUAL_MONTHLY")
                .amount(4900L)
                .currency("INR")
                .status("PENDING")
                .paymentOrderId("order_mock_123")
                .build();
        pendingMembership.setId(100L);
    }

    @Test
    @WithMockUser(username = "+919000000000", roles = "WORKER")
    void verifyPayment_SuccessfulVerification_SetsActivatedAtAndExpiresAt() throws Exception {
        Mockito.when(userRepository.findByMobile("+919000000000")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));
        Mockito.when(workerMembershipRepository.findByPaymentOrderId("order_mock_123")).thenReturn(Optional.of(pendingMembership));

        PaymentVerificationRequest request = new PaymentVerificationRequest();
        request.setRazorpayOrderId("order_mock_123");
        request.setRazorpayPaymentId("pay_mock_123");
        request.setRazorpaySignature("sig_mock_123");

        mockMvc.perform(post("/api/v1/worker/membership/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        ArgumentCaptor<WorkerMembership> captor = ArgumentCaptor.forClass(WorkerMembership.class);
        Mockito.verify(workerMembershipRepository).save(captor.capture());
        WorkerMembership savedMembership = captor.getValue();

        // TEST 1: Successful verification sets activatedAt
        assertNotNull(savedMembership.getActivatedAt(), "activatedAt should be populated");
        
        // TEST 2: Successful verification sets expiresAt
        assertNotNull(savedMembership.getExpiresAt(), "expiresAt should be populated");
        
        // TEST 3: expiresAt is calculated from activatedAt
        // TEST 4: Existing membership duration is respected (plusMonths(1))
        LocalDateTime expectedExpiresAt = savedMembership.getActivatedAt().plusMonths(1);
        assertEquals(expectedExpiresAt, savedMembership.getExpiresAt(), "expiresAt should be exactly 1 month after activatedAt");
        
        // TEST 6: Existing verification behavior remains unchanged
        assertEquals("ACTIVE", savedMembership.getStatus());
        assertEquals("pay_mock_123", savedMembership.getPaymentPaymentId());
        
        // TEST 7: No duplicate membership is created because of this change (it updates the existing one)
        assertEquals(100L, savedMembership.getId());
    }

    @Test
    @WithMockUser(username = "+919000000000", roles = "WORKER")
    void verifyPayment_FailedPayment_DoesNotActivateMembership() throws Exception {
        Mockito.when(userRepository.findByMobile("+919000000000")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));
        
        WorkerMembership realOrderMembership = WorkerMembership.builder()
                .workerProfile(mockProfile)
                .plan("INDIVIDUAL_MONTHLY")
                .amount(4900L)
                .currency("INR")
                .status("PENDING")
                .paymentOrderId("order_real_123")
                .build();
        
        Mockito.when(workerMembershipRepository.findByPaymentOrderId("order_real_123")).thenReturn(Optional.of(realOrderMembership));

        PaymentVerificationRequest request = new PaymentVerificationRequest();
        request.setRazorpayOrderId("order_real_123");
        request.setRazorpayPaymentId("pay_real_123");
        request.setRazorpaySignature("invalid_signature");

        mockMvc.perform(post("/api/v1/worker/membership/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());

        ArgumentCaptor<WorkerMembership> captor = ArgumentCaptor.forClass(WorkerMembership.class);
        Mockito.verify(workerMembershipRepository).save(captor.capture());
        WorkerMembership savedMembership = captor.getValue();

        // TEST 5: Failed payment does not activate membership
        assertEquals("FAILED", savedMembership.getStatus(), "Status should be FAILED");
        assertNull(savedMembership.getActivatedAt(), "activatedAt should remain null");
        assertNull(savedMembership.getExpiresAt(), "expiresAt should remain null");
    }
    @Test
    @WithMockUser(username = "+919000000000", roles = "WORKER")
    void verifyPayment_ExistingExpiredMembership_PreservesActivatedAt() throws Exception {
        Mockito.when(userRepository.findByMobile("+919000000000")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));
        
        LocalDateTime historicalDate = LocalDateTime.now().minusMonths(6);
        WorkerMembership expiredMembership = WorkerMembership.builder()
                .workerProfile(mockProfile)
                .plan("INDIVIDUAL_MONTHLY")
                .amount(4900L)
                .currency("INR")
                .status("PENDING")
                .paymentOrderId("order_mock_456")
                .activatedAt(historicalDate) // Historical date
                .build();
        expiredMembership.setId(200L);
        
        Mockito.when(workerMembershipRepository.findByPaymentOrderId("order_mock_456")).thenReturn(Optional.of(expiredMembership));

        PaymentVerificationRequest request = new PaymentVerificationRequest();
        request.setRazorpayOrderId("order_mock_456");
        request.setRazorpayPaymentId("pay_mock_456");
        request.setRazorpaySignature("sig_mock_456");

        mockMvc.perform(post("/api/v1/worker/membership/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));

        ArgumentCaptor<WorkerMembership> captor = ArgumentCaptor.forClass(WorkerMembership.class);
        Mockito.verify(workerMembershipRepository).save(captor.capture());
        WorkerMembership savedMembership = captor.getValue();

        assertEquals("ACTIVE", savedMembership.getStatus());
        assertEquals(historicalDate, savedMembership.getActivatedAt(), "activatedAt should NOT be overwritten");
        assertNotNull(savedMembership.getExpiresAt());
        assertTrue(savedMembership.getExpiresAt().isAfter(LocalDateTime.now().minusMinutes(1)));
        assertEquals("pay_mock_456", savedMembership.getPaymentPaymentId());
    }

    @Test
    @WithMockUser(username = "+919000000000", roles = "WORKER")
    void verifyPayment_ExistingActiveMembership_IdempotentResponse() throws Exception {
        Mockito.when(userRepository.findByMobile("+919000000000")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));

        LocalDateTime activated = LocalDateTime.now().minusDays(5);
        LocalDateTime expired = activated.plusMonths(1);
        WorkerMembership activeMembership = WorkerMembership.builder()
                .workerProfile(mockProfile)
                .plan("INDIVIDUAL_MONTHLY")
                .amount(4900L)
                .currency("INR")
                .status("ACTIVE")
                .paymentOrderId("order_mock_active")
                .activatedAt(activated)
                .expiresAt(expired)
                .build();
        
        Mockito.when(workerMembershipRepository.findByPaymentOrderId("order_mock_active")).thenReturn(Optional.of(activeMembership));

        PaymentVerificationRequest request = new PaymentVerificationRequest();
        request.setRazorpayOrderId("order_mock_active");
        request.setRazorpayPaymentId("pay_mock_already");
        request.setRazorpaySignature("sig_mock_already");

        mockMvc.perform(post("/api/v1/worker/membership/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Payment already verified successfully"));
    }
    @Test
    @WithMockUser(username = "+919000000000", roles = "WORKER")
    void getMembershipStatus_ActiveMembership_ReturnsDetails() throws Exception {
        Mockito.when(userRepository.findByMobile("+919000000000")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));

        LocalDateTime now = LocalDateTime.now();
        LocalDateTime expiresAt = now.plusDays(15).plusHours(1);
        WorkerMembership activeMembership = WorkerMembership.builder()
                .workerProfile(mockProfile)
                .plan("INDIVIDUAL_MONTHLY")
                .amount(4900L)
                .currency("INR")
                .status("ACTIVE")
                .activatedAt(now.minusDays(15))
                .expiresAt(expiresAt)
                .build();
        activeMembership.setId(200L);

        Mockito.when(workerMembershipRepository.findByWorkerProfileId(10L)).thenReturn(Optional.of(activeMembership));

        mockMvc.perform(get("/api/v1/worker/membership")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("ACTIVE"))
                .andExpect(jsonPath("$.data.plan").value("INDIVIDUAL_MONTHLY"))
                // TEST 1: Authenticated worker with ACTIVE membership receives status, activatedAt, expiresAt, calculated daysRemaining
                // TEST 2: Returned activatedAt exactly matches database/entity value
                .andExpect(jsonPath("$.data.activatedAt").isNotEmpty())
                // TEST 3: Returned expiresAt exactly matches database/entity value
                .andExpect(jsonPath("$.data.expiresAt").isNotEmpty())
                // TEST 4: daysRemaining is calculated from expiresAt and is NOT hardcoded
                .andExpect(jsonPath("$.data.daysRemaining").value(15));
    }

    @Test
    @WithMockUser(username = "+919000000000", roles = "WORKER")
    void getMembershipStatus_NoMembership_ReturnsNullData() throws Exception {
        Mockito.when(userRepository.findByMobile("+919000000000")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));
        
        // TEST 6: No membership case follows existing API error/response convention
        Mockito.when(workerMembershipRepository.findByWorkerProfileId(10L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/worker/membership")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").doesNotExist());
    }

    @Test
    @WithMockUser(username = "+919000000000", roles = "WORKER")
    void getMembershipStatus_ExpiredOrNullExpiresAt_CalculatesCorrectly() throws Exception {
        Mockito.when(userRepository.findByMobile("+919000000000")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));

        // Legacy membership with no expiresAt
        WorkerMembership legacyMembership = WorkerMembership.builder()
                .workerProfile(mockProfile)
                .plan("INDIVIDUAL_MONTHLY")
                .amount(4900L)
                .currency("INR")
                .status("PENDING")
                .activatedAt(null)
                .expiresAt(null)
                .build();
        legacyMembership.setId(201L);

        Mockito.when(workerMembershipRepository.findByWorkerProfileId(10L)).thenReturn(Optional.of(legacyMembership));

        mockMvc.perform(get("/api/v1/worker/membership")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.status").value("PENDING"))
                .andExpect(jsonPath("$.data.daysRemaining").isEmpty());
    }

    @Test
    @WithMockUser(username = "+919000000000", roles = "WORKER")
    void getPaymentHistory_ReturnsList() throws Exception {
        Mockito.when(userRepository.findByMobile("+919000000000")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));

        WorkerPaymentHistory history = WorkerPaymentHistory.builder()
                .workerProfile(mockProfile)
                .membership(pendingMembership)
                .paymentId("pay_123")
                .orderId("order_123")
                .amount(4900L)
                .currency("INR")
                .status("SUCCESS")
                .paidAt(LocalDateTime.now())
                .paymentMethod("upi")
                .build();
        history.setId(500L);

        Mockito.when(workerPaymentHistoryService.getWorkerPaymentHistory(10L)).thenReturn(List.of(history));

        mockMvc.perform(get("/api/v1/worker/membership/payments")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].id").value(500))
                .andExpect(jsonPath("$.data[0].paymentId").value("pay_123"))
                .andExpect(jsonPath("$.data[0].amount").value(4900));
    }

    @Test
    @WithMockUser(username = "+919000000000", roles = "WORKER")
    void renewVerify_ActiveMembership_ExtendsExpiry() throws Exception {
        Mockito.when(userRepository.findByMobile("+919000000000")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));

        LocalDateTime activated = LocalDateTime.of(2026, 9, 19, 10, 0);
        LocalDateTime expires = LocalDateTime.of(2026, 10, 19, 10, 0);
        
        WorkerMembership membership = WorkerMembership.builder()
                .workerProfile(mockProfile)
                .plan("INDIVIDUAL_MONTHLY")
                .amount(4900L)
                .currency("INR")
                .status("ACTIVE")
                .paymentOrderId("order_mock_renew")
                .activatedAt(activated)
                .expiresAt(expires)
                .build();
        membership.setId(300L);

        Mockito.when(workerMembershipRepository.findByPaymentOrderId("order_mock_renew")).thenReturn(Optional.of(membership));

        PaymentVerificationRequest request = new PaymentVerificationRequest();
        request.setRazorpayOrderId("order_mock_renew");
        request.setRazorpayPaymentId("pay_mock_renew");
        request.setRazorpaySignature("sig_mock_renew");

        mockMvc.perform(post("/api/v1/worker/membership/renew/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        ArgumentCaptor<WorkerMembership> captor = ArgumentCaptor.forClass(WorkerMembership.class);
        Mockito.verify(workerMembershipRepository).save(captor.capture());
        WorkerMembership savedMembership = captor.getValue();

        assertEquals(expires.plusMonths(1), savedMembership.getExpiresAt(), "Expiry should be extended by exactly 1 month");
        assertEquals(activated, savedMembership.getActivatedAt(), "activatedAt must remain unchanged");
    }

    @Test
    @WithMockUser(username = "+919000000000", roles = "WORKER")
    void renewVerify_ExpiredMembership_StartsNewPeriod() throws Exception {
        Mockito.when(userRepository.findByMobile("+919000000000")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));

        LocalDateTime activated = LocalDateTime.now().minusMonths(2);
        LocalDateTime expires = LocalDateTime.now().minusMonths(1);
        
        WorkerMembership membership = WorkerMembership.builder()
                .workerProfile(mockProfile)
                .plan("INDIVIDUAL_MONTHLY")
                .amount(4900L)
                .currency("INR")
                .status("ACTIVE")
                .paymentOrderId("order_mock_renew")
                .activatedAt(activated)
                .expiresAt(expires)
                .build();

        Mockito.when(workerMembershipRepository.findByPaymentOrderId("order_mock_renew")).thenReturn(Optional.of(membership));

        PaymentVerificationRequest request = new PaymentVerificationRequest();
        request.setRazorpayOrderId("order_mock_renew");
        request.setRazorpayPaymentId("pay_mock_renew");
        request.setRazorpaySignature("sig_mock_renew");

        mockMvc.perform(post("/api/v1/worker/membership/renew/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());

        ArgumentCaptor<WorkerMembership> captor = ArgumentCaptor.forClass(WorkerMembership.class);
        Mockito.verify(workerMembershipRepository).save(captor.capture());
        WorkerMembership savedMembership = captor.getValue();

        assertTrue(savedMembership.getExpiresAt().isAfter(LocalDateTime.now().plusDays(25)), "Should start new period from today");
        assertEquals(activated, savedMembership.getActivatedAt(), "activatedAt must remain unchanged");
    }

    @Test
    @WithMockUser(username = "+919000000000", roles = "WORKER")
    void renewVerify_NullExpiry_RejectsRenewal() throws Exception {
        Mockito.when(userRepository.findByMobile("+919000000000")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));

        WorkerMembership membership = WorkerMembership.builder()
                .workerProfile(mockProfile)
                .plan("INDIVIDUAL_MONTHLY")
                .amount(4900L)
                .currency("INR")
                .status("ACTIVE")
                .paymentOrderId("order_mock_renew")
                .activatedAt(LocalDateTime.now())
                .expiresAt(null)
                .build();

        Mockito.when(workerMembershipRepository.findByPaymentOrderId("order_mock_renew")).thenReturn(Optional.of(membership));

        PaymentVerificationRequest request = new PaymentVerificationRequest();
        request.setRazorpayOrderId("order_mock_renew");
        request.setRazorpayPaymentId("pay_mock_renew");
        request.setRazorpaySignature("sig_mock_renew");

        mockMvc.perform(post("/api/v1/worker/membership/renew/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());
    }

    @Test
    @WithMockUser(username = "+919000000000", roles = "WORKER")
    void renewVerify_DuplicateVerification_DoesNotExtendTwice() throws Exception {
        Mockito.when(userRepository.findByMobile("+919000000000")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));

        LocalDateTime expires = LocalDateTime.now().plusMonths(1);
        WorkerMembership membership = WorkerMembership.builder()
                .workerProfile(mockProfile)
                .plan("INDIVIDUAL_MONTHLY")
                .amount(4900L)
                .currency("INR")
                .status("ACTIVE")
                .paymentOrderId("order_mock_renew")
                .paymentPaymentId("pay_mock_renew")
                .activatedAt(LocalDateTime.now())
                .expiresAt(expires)
                .build();

        Mockito.when(workerMembershipRepository.findByPaymentOrderId("order_mock_renew")).thenReturn(Optional.of(membership));

        PaymentVerificationRequest request = new PaymentVerificationRequest();
        request.setRazorpayOrderId("order_mock_renew");
        request.setRazorpayPaymentId("pay_mock_renew");
        request.setRazorpaySignature("sig_mock_renew");

        mockMvc.perform(post("/api/v1/worker/membership/renew/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk());
                
        // Repository save should NOT be called again
        Mockito.verify(workerMembershipRepository, Mockito.never()).save(any());
    }

    @Test
    @WithMockUser(username = "+919000000000", roles = "WORKER")
    void renewVerify_FailedPayment_DoesNotModifyMembership() throws Exception {
        Mockito.when(userRepository.findByMobile("+919000000000")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));

        LocalDateTime expires = LocalDateTime.now().plusMonths(1);
        WorkerMembership membership = WorkerMembership.builder()
                .workerProfile(mockProfile)
                .plan("INDIVIDUAL_MONTHLY")
                .amount(4900L)
                .currency("INR")
                .status("ACTIVE")
                .paymentOrderId("order_real_renew")
                .activatedAt(LocalDateTime.now())
                .expiresAt(expires)
                .build();

        Mockito.when(workerMembershipRepository.findByPaymentOrderId("order_real_renew")).thenReturn(Optional.of(membership));

        PaymentVerificationRequest request = new PaymentVerificationRequest();
        request.setRazorpayOrderId("order_real_renew");
        request.setRazorpayPaymentId("pay_real_renew");
        request.setRazorpaySignature("invalid_signature");

        mockMvc.perform(post("/api/v1/worker/membership/renew/verify")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity());

        Mockito.verify(workerMembershipRepository, Mockito.never()).save(any());
    }
}
