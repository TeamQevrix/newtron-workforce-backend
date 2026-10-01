package com.newtron.newtron_workforce_backend.controller;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.security.MessageDigest;

import com.newtron.newtron_workforce_backend.entity.RazorpayWebhookLog;
import com.newtron.newtron_workforce_backend.entity.WorkerMembership;
import com.newtron.newtron_workforce_backend.repository.RazorpayWebhookLogRepository;
import com.newtron.newtron_workforce_backend.repository.WorkerMembershipRepository;
import com.newtron.newtron_workforce_backend.service.WorkerPaymentHistoryService;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1/webhooks/razorpay")
public class RazorpayWebhookController {

    private static final Logger logger = LoggerFactory.getLogger(RazorpayWebhookController.class);
    
    @Value("${razorpay.webhook-secret}")
    private String webhookSecret;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    private final RazorpayWebhookLogRepository webhookLogRepository;
    private final WorkerMembershipRepository workerMembershipRepository;
    private final WorkerPaymentHistoryService workerPaymentHistoryService;

    public RazorpayWebhookController(RazorpayWebhookLogRepository webhookLogRepository,
                                     WorkerMembershipRepository workerMembershipRepository,
                                     WorkerPaymentHistoryService workerPaymentHistoryService) {
        this.webhookLogRepository = webhookLogRepository;
        this.workerMembershipRepository = workerMembershipRepository;
        this.workerPaymentHistoryService = workerPaymentHistoryService;
    }

    @PostMapping
    @Transactional
    public ResponseEntity<String> handleWebhook(
            @RequestHeader("X-Razorpay-Signature") String signature,
            @RequestBody String rawPayload) {
        
        try {
            // 1. Verify Signature using timing-safe comparison
            if (!verifySignature(rawPayload, signature, webhookSecret)) {
                logger.warn("Invalid Razorpay webhook signature");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Invalid signature");
            }
            
            // 2. Parse JSON after successful verification
            JsonNode rootNode = objectMapper.readTree(rawPayload);
            String eventId = rootNode.path("id").asText();
            String eventType = rootNode.path("event").asText();
            
            if (eventId == null || eventId.isEmpty()) {
                logger.warn("Razorpay webhook missing event ID");
                return ResponseEntity.status(HttpStatus.BAD_REQUEST).body("Missing event ID");
            }
            
            if (webhookLogRepository.existsByEventId(eventId)) {
                logger.info("Duplicate webhook event received: {}", eventId);
                return ResponseEntity.ok("OK");
            }

            String paymentId = null;
            String orderId = null;
            
            JsonNode paymentEntity = rootNode.path("payload").path("payment").path("entity");
            if (!paymentEntity.isMissingNode()) {
                paymentId = paymentEntity.path("id").asText(null);
                orderId = paymentEntity.path("order_id").asText(null);
            }

            try {
                RazorpayWebhookLog log = RazorpayWebhookLog.builder()
                        .eventId(eventId)
                        .eventType(eventType)
                        .orderId(orderId)
                        .paymentId(paymentId)
                        .status("RECEIVED")
                        .build();
                webhookLogRepository.save(log);
            } catch (DataIntegrityViolationException ex) {
                logger.info("Duplicate webhook event detected during insert: {}", eventId);
                return ResponseEntity.ok("OK");
            }
            
            // 3. Process events
            if ("payment.captured".equals(eventType)) {
                logger.info("Received payment.captured event");
                
                if (orderId != null && paymentId != null) {
                    String rStatus = paymentEntity.path("status").asText(null);
                    Long rAmount = paymentEntity.path("amount").isMissingNode() ? null : paymentEntity.path("amount").asLong();
                    String rCurrency = paymentEntity.path("currency").asText(null);
                    String rMethod = paymentEntity.path("method").asText(null);
                    String rSignature = paymentEntity.path("signature").asText(null);
                    
                    if ("captured".equalsIgnoreCase(rStatus)) {
                        Optional<WorkerMembership> membershipOpt = workerMembershipRepository.findByPaymentOrderIdWithLock(orderId);
                        
                        if (membershipOpt.isPresent()) {
                            WorkerMembership membership = membershipOpt.get();
                            
                            if (!paymentId.equals(membership.getPaymentPaymentId())) {
                                if (membership.getAmount().equals(rAmount) && membership.getCurrency().equalsIgnoreCase(rCurrency)) {
                                    
                                    membership.setStatus("ACTIVE");
                                    membership.setPaymentPaymentId(paymentId);
                                    if (rSignature != null && !rSignature.isEmpty()) {
                                        membership.setPaymentSignature(rSignature);
                                    }
                                    
                                    LocalDateTime now = LocalDateTime.now();
                                    if (membership.getActivatedAt() == null) {
                                        membership.setActivatedAt(now);
                                    }
                                    
                                    LocalDateTime currentExpiresAt = membership.getExpiresAt();
                                    if (currentExpiresAt != null && currentExpiresAt.isAfter(now)) {
                                        membership.setExpiresAt(currentExpiresAt.plusMonths(1));
                                    } else {
                                        membership.setExpiresAt(now.plusMonths(1));
                                    }
                                    
                                    workerMembershipRepository.save(membership);
                                    
                                    workerPaymentHistoryService.recordPaymentSuccess(
                                            membership, paymentId, orderId, membership.getAmount(), membership.getCurrency(), rMethod);
                                            
                                    logger.info("Successfully activated membership for orderId: {}", orderId);
                                } else {
                                    logger.warn("Amount or currency mismatch for orderId: {}", orderId);
                                }
                            } else {
                                logger.info("Payment {} already processed for orderId: {}", paymentId, orderId);
                            }
                        } else {
                            logger.warn("Membership not found for orderId: {}", orderId);
                        }
                    } else {
                        logger.warn("Payment status is not captured: {}", rStatus);
                    }
                } else {
                    logger.warn("Missing orderId or paymentId in payment.captured event");
                }
                
            } else if ("payment.failed".equals(eventType)) {
                logger.info("Received payment.failed event");
                // TODO: Implement failure handling
            } else {
                logger.info("Received unsupported webhook event");
            }
            
            // Always return 200 OK for verified webhook requests
            return ResponseEntity.ok("OK");
            
        } catch (Exception e) {
            logger.error("Error processing Razorpay webhook", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body("Internal Server Error");
        }
    }

    private boolean verifySignature(String payload, String expectedSignature, String secret) {
        try {
            Mac mac = Mac.getInstance("HmacSHA256");
            SecretKeySpec secretKeySpec = new SecretKeySpec(secret.getBytes(), "HmacSHA256");
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
            
            String generatedSignature = hexString.toString();
            
            // Timing-safe comparison to prevent timing attacks
            return MessageDigest.isEqual(generatedSignature.getBytes(), expectedSignature.getBytes());
        } catch (Exception e) {
            logger.error("Error verifying webhook signature");
            return false;
        }
    }
}
