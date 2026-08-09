package com.newtron.newtron_workforce_backend.controller;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.common.exception.ResourceNotFoundException;
import com.newtron.newtron_workforce_backend.common.exception.ValidationException;
import com.newtron.newtron_workforce_backend.common.logging.RequestContext;
import com.newtron.newtron_workforce_backend.common.response.ApiResponse;
import com.newtron.newtron_workforce_backend.common.response.ApiResponseFactory;
import com.newtron.newtron_workforce_backend.dto.RazorpayOrderResponse;
import com.newtron.newtron_workforce_backend.dto.PaymentVerificationRequest;
import com.newtron.newtron_workforce_backend.dto.WorkerMembershipResponse;
import com.newtron.newtron_workforce_backend.entity.WorkerMembership;
import com.newtron.newtron_workforce_backend.entity.WorkerProfile;
import com.newtron.newtron_workforce_backend.enums.OnboardingStep;
import com.newtron.newtron_workforce_backend.repository.WorkerMembershipRepository;
import com.newtron.newtron_workforce_backend.repository.WorkerProfileRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.*;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.client.RestTemplate;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/worker/membership")
@RequiredArgsConstructor
public class MembershipController {

    private final UserRepository userRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final WorkerMembershipRepository workerMembershipRepository;

    @Value("${razorpay.key-id}")
    private String razorpayKeyId;

    @Value("${razorpay.key-secret}")
    private String razorpayKeySecret;

    private static final String PLAN_LIFETIME = "LIFETIME_ACCESS";
    private static final Long AMOUNT_LIFETIME_PAISE = 4900L;
    private static final String CURRENCY_INR = "INR";
    private static final String PROVIDER_RAZORPAY = "RAZORPAY";

    private final RestTemplate restTemplate = new RestTemplate();

    @PostMapping("/order")
    @Transactional
    public ApiResponse<RazorpayOrderResponse> createOrder(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);
        WorkerProfile profile = workerProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ValidationException("WORKER_PROFILE_NOT_FOUND", "Worker profile not found"));

        // Check if already active
        boolean alreadyActive = workerMembershipRepository.existsByWorkerProfileIdAndStatus(profile.getId(), "ACTIVE");
        if (alreadyActive) {
            throw new ValidationException("ALREADY_ACTIVE", "Worker already has an active membership");
        }

        String orderId;
        if ("rzp_test_dummykey".equals(razorpayKeyId) || "dummysecret".equals(razorpayKeySecret)) {
            orderId = "order_mock_" + profile.getId() + "_" + System.currentTimeMillis();
        } else {
            // Call Razorpay API to create an order
            String razorpayUrl = "https://api.razorpay.com/v1/orders";
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBasicAuth(razorpayKeyId, razorpayKeySecret);

            Map<String, Object> orderRequest = new HashMap<>();
            orderRequest.put("amount", AMOUNT_LIFETIME_PAISE);
            orderRequest.put("currency", CURRENCY_INR);
            orderRequest.put("receipt", "receipt_profile_" + profile.getId() + "_" + System.currentTimeMillis());

            HttpEntity<Map<String, Object>> requestEntity = new HttpEntity<>(orderRequest, headers);
            
            try {
                ResponseEntity<Map> response = restTemplate.postForEntity(razorpayUrl, requestEntity, Map.class);
                if (response.getStatusCode() == HttpStatus.CREATED || response.getStatusCode() == HttpStatus.OK) {
                    Map body = response.getBody();
                    orderId = (String) body.get("id");
                } else {
                    throw new ValidationException("RAZORPAY_ERROR", "Failed to create order on Razorpay");
                }
            } catch (Exception e) {
                throw new ValidationException("RAZORPAY_ERROR", "Error communicating with Razorpay: " + e.getMessage());
            }
        }

        // Create or update PENDING membership
        Optional<WorkerMembership> existing = workerMembershipRepository.findByWorkerProfileId(profile.getId());
        WorkerMembership membership;
        if (existing.isPresent()) {
            membership = existing.get();
            membership.setPlan(PLAN_LIFETIME);
            membership.setAmount(AMOUNT_LIFETIME_PAISE);
            membership.setCurrency(CURRENCY_INR);
            membership.setStatus("PENDING");
            membership.setPaymentProvider(PROVIDER_RAZORPAY);
            membership.setPaymentOrderId(orderId);
            membership.setPaymentPaymentId(null);
            membership.setPaymentSignature(null);
        } else {
            membership = WorkerMembership.builder()
                    .workerProfile(profile)
                    .plan(PLAN_LIFETIME)
                    .amount(AMOUNT_LIFETIME_PAISE)
                    .currency(CURRENCY_INR)
                    .status("PENDING")
                    .paymentProvider(PROVIDER_RAZORPAY)
                    .paymentOrderId(orderId)
                    .build();
        }
        workerMembershipRepository.save(membership);

        RazorpayOrderResponse orderResponse = RazorpayOrderResponse.builder()
                .orderId(orderId)
                .amount(AMOUNT_LIFETIME_PAISE)
                .currency(CURRENCY_INR)
                .keyId(razorpayKeyId)
                .build();

        return ApiResponseFactory.success(orderResponse, "Razorpay order created successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @PostMapping("/verify")
    @Transactional
    public ApiResponse<WorkerMembershipResponse> verifyPayment(
            @RequestBody PaymentVerificationRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);
        WorkerProfile profile = fetchWorkerProfile(currentUser);

        // Find the pending membership
        WorkerMembership membership = workerMembershipRepository.findByPaymentOrderId(request.getRazorpayOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("MEMBERSHIP_NOT_FOUND", "No membership order found for the provided order ID"));

        // Guard: Verify ownership
        if (!membership.getWorkerProfile().getId().equals(profile.getId())) {
            throw new ValidationException("UNAUTHORIZED_ACCESS", "Membership order does not belong to the current authenticated worker");
        }

        // Idempotency: If already active, return the existing active details
        if ("ACTIVE".equalsIgnoreCase(membership.getStatus())) {
            WorkerMembershipResponse response = WorkerMembershipResponse.builder()
                    .plan(membership.getPlan())
                    .amount(membership.getAmount())
                    .currency(membership.getCurrency())
                    .status(membership.getStatus())
                    .activatedAt(membership.getActivatedAt())
                    .build();
            return ApiResponseFactory.success(response, "Payment already verified successfully",
                    RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
        }

        // Verify Razorpay signature using SHA256 HMAC
        boolean isValidSignature;
        if ("rzp_test_dummykey".equals(razorpayKeyId) || "dummysecret".equals(razorpayKeySecret)) {
            isValidSignature = true;
        } else {
            isValidSignature = verifyRazorpaySignature(
                    request.getRazorpayOrderId(),
                    request.getRazorpayPaymentId(),
                    request.getRazorpaySignature(),
                    razorpayKeySecret
            );
        }

        if (!isValidSignature) {
            membership.setStatus("FAILED");
            workerMembershipRepository.save(membership);
            throw new ValidationException("INVALID_SIGNATURE", "Payment verification failed: signature is invalid");
        }

        if (!("rzp_test_dummykey".equals(razorpayKeyId) || "dummysecret".equals(razorpayKeySecret))) {
            // Verify with Razorpay API (Reconciliation check)
            String paymentUrl = "https://api.razorpay.com/v1/payments/" + request.getRazorpayPaymentId();
            HttpHeaders headers = new HttpHeaders();
            headers.setBasicAuth(razorpayKeyId, razorpayKeySecret);
            HttpEntity<Void> entity = new HttpEntity<>(headers);

            try {
                ResponseEntity<Map> response = restTemplate.exchange(paymentUrl, HttpMethod.GET, entity, Map.class);
                if (response.getStatusCode() == HttpStatus.OK) {
                    Map body = response.getBody();
                    String rStatus = (String) body.get("status");
                    String rOrderId = (String) body.get("order_id");
                    Object rAmountObj = body.get("amount");
                    Long rAmount = rAmountObj instanceof Number ? ((Number) rAmountObj).longValue() : Long.parseLong(rAmountObj.toString());
                    String rCurrency = (String) body.get("currency");

                    if (!request.getRazorpayOrderId().equals(rOrderId) ||
                            !AMOUNT_LIFETIME_PAISE.equals(rAmount) ||
                            !CURRENCY_INR.equalsIgnoreCase(rCurrency) ||
                            !("captured".equalsIgnoreCase(rStatus) || "authorized".equalsIgnoreCase(rStatus))) {
                        throw new ValidationException("PAYMENT_MISMATCH", "Payment details from Razorpay do not match the expected plan/amount/order");
                    }
                } else {
                    throw new ValidationException("RECONCILIATION_FAILED", "Failed to contact Razorpay to verify payment status");
                }
            } catch (Exception e) {
                if (e instanceof ValidationException) {
                    throw e;
                }
                throw new ValidationException("RECONCILIATION_FAILED", "Error reconciling payment with Razorpay: " + e.getMessage());
            }
        }

        // Atomically update membership and onboarding step
        membership.setStatus("ACTIVE");
        membership.setPaymentPaymentId(request.getRazorpayPaymentId());
        membership.setPaymentSignature(request.getRazorpaySignature());
        membership.setActivatedAt(LocalDateTime.now());
        workerMembershipRepository.save(membership);

        profile.setCurrentStep(OnboardingStep.COMPLETED);
        profile.setIsCompleted(true);
        workerProfileRepository.save(profile);

        // Update User entity flags consistently
        User user = profile.getUser();
        user.setMembershipActive(true);
        user.setProfileCompleted(true);
        userRepository.save(user);

        WorkerMembershipResponse response = WorkerMembershipResponse.builder()
                .plan(membership.getPlan())
                .amount(membership.getAmount())
                .currency(membership.getCurrency())
                .status(membership.getStatus())
                .activatedAt(membership.getActivatedAt())
                .build();

        return ApiResponseFactory.success(response, "Payment verified successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @GetMapping
    @Transactional(readOnly = true)
    public ApiResponse<WorkerMembershipResponse> getMembershipStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);
        WorkerProfile profile = fetchWorkerProfile(currentUser);

        Optional<WorkerMembership> membership = workerMembershipRepository.findByWorkerProfileId(profile.getId());
        
        WorkerMembershipResponse response = membership.map(m -> WorkerMembershipResponse.builder()
                .plan(m.getPlan())
                .amount(m.getAmount())
                .currency(m.getCurrency())
                .status(m.getStatus())
                .activatedAt(m.getActivatedAt())
                .build()).orElse(null);

        return ApiResponseFactory.success(response, "Membership status retrieved successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    private User fetchCurrentUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new ResourceNotFoundException("NOT_AUTHENTICATED", "Not authenticated");
        }
        return userRepository.findByMobile(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));
    }

    private WorkerProfile fetchWorkerProfile(User user) {
        return workerProfileRepository.findByUserId(user.getId())
                .orElseThrow(() -> new ResourceNotFoundException("PROFILE_NOT_FOUND", "Worker profile not found"));
    }

    private long getStartTime(HttpServletRequest request) {
        Object attr = request.getAttribute("startTimeMs");
        if (attr instanceof Long) {
            return (Long) attr;
        }
        return System.currentTimeMillis();
    }

    private boolean verifyRazorpaySignature(String orderId, String paymentId, String signature, String secret) {
        try {
            String payload = orderId + "|" + paymentId;
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
            mac.init(secretKeySpec);
            byte[] hmacBytes = mac.doFinal(payload.getBytes());
            StringBuilder hexString = new StringBuilder();
            for (byte b : hmacBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString().equals(signature);
        } catch (Exception e) {
            return false;
        }
    }
}
