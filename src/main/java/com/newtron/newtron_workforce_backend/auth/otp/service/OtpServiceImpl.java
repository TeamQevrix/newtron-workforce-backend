package com.newtron.newtron_workforce_backend.auth.otp.service;

import com.newtron.newtron_workforce_backend.auth.otp.OtpConstants;
import com.newtron.newtron_workforce_backend.auth.otp.entity.*;
import com.newtron.newtron_workforce_backend.auth.otp.generator.OtpGenerator;
import com.newtron.newtron_workforce_backend.auth.otp.provider.OtpProvider;
import com.newtron.newtron_workforce_backend.auth.otp.repository.OtpRepository;
import com.newtron.newtron_workforce_backend.auth.otp.validator.OtpValidator;
import com.newtron.newtron_workforce_backend.common.exception.BusinessException;
import com.newtron.newtron_workforce_backend.common.logging.AuditLogger;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
public class OtpServiceImpl implements OtpService {

    private final OtpRepository otpRepository;
    private final OtpGenerator otpGenerator;
    private final OtpValidator otpValidator;
    private final OtpProvider otpProvider;

    @Override
    @Transactional
    public void sendOtp(String countryCode, String mobileNumber, OtpPurpose purpose, com.newtron.newtron_workforce_backend.auth.enums.Role role) {
        // Fetch the latest OTP generated for this mobile and purpose
        Optional<Otp> latestOtpOpt = otpRepository.findTopByMobileCountryCodeAndMobileNumberAndPurposeOrderByCreatedAtDesc(
                countryCode, mobileNumber, purpose);

        if (latestOtpOpt.isPresent()) {
            Otp latestOtp = latestOtpOpt.get();

            // Check if verification is currently blocked
            if (latestOtp.getStatus() == OtpStatus.BLOCKED || 
                    (latestOtp.getBlockedUntil() != null && Instant.now().isBefore(latestOtp.getBlockedUntil()))) {
                throw new BusinessException("OTP_BLOCKED", "OTP verification is blocked. Please try again later.");
            }

            // Check minimum resend interval (60 seconds)
            Instant nextAllowedSend = latestOtp.getLastSentAt().plus(Duration.ofSeconds(OtpConstants.RESEND_INTERVAL_SECONDS));
            if (Instant.now().isBefore(nextAllowedSend)) {
                throw new BusinessException("OTP_RESEND_LOCKED", "Please wait before requesting a new OTP.");
            }
        }

        // Rate limit check: Max 3 requests per mobile in the last 15 minutes
        Instant fifteenMinutesAgo = Instant.now().minus(Duration.ofMinutes(OtpConstants.RATE_LIMIT_WINDOW_MINUTES));
        int requestsInWindow = otpRepository.countByMobileCountryCodeAndMobileNumberAndCreatedAtAfter(
                countryCode, mobileNumber, fifteenMinutesAgo);

        if (requestsInWindow >= OtpConstants.MAX_REQUESTS_IN_WINDOW) {
            throw new BusinessException("OTP_RATE_LIMIT_EXCEEDED", "Maximum OTP requests exceeded. Please try again after 15 minutes.");
        }

        // Expire any previously generated active OTPs for this purpose
        latestOtpOpt.ifPresent(otp -> {
            if (otp.getStatus() == OtpStatus.GENERATED) {
                otp.setStatus(OtpStatus.EXPIRED);
                otpRepository.save(otp);
            }
        });

        // Generate and hash OTP
        String rawOtp = otpGenerator.generate();
        String hash = otpValidator.hashOtp(rawOtp);

        // Build new OTP entity
        Otp newOtp = Otp.builder()
                .mobileCountryCode(countryCode)
                .mobileNumber(mobileNumber)
                .otpHash(hash)
                .purpose(purpose)
                .status(OtpStatus.GENERATED)
                .role(role)
                .attemptCount(0)
                .maxAttempts(OtpConstants.MAX_ATTEMPTS)
                .expiresAt(Instant.now().plus(Duration.ofMinutes(OtpConstants.EXPIRY_MINUTES)))
                .lastSentAt(Instant.now())
                .build();

        otpRepository.save(newOtp);

        // Send raw OTP via active provider
        otpProvider.send(countryCode, mobileNumber, rawOtp, purpose);

        // Audit Logging
        AuditLogger.logAction("OTP_GENERATED", null, "Mobile: " + countryCode + mobileNumber + ", Purpose: " + purpose);
    }

    @Override
    @Transactional
    public OtpVerificationResult verifyOtp(String countryCode, String mobileNumber, OtpPurpose purpose, String code) {
        Optional<Otp> otpOpt = otpRepository.findTopByMobileCountryCodeAndMobileNumberAndPurposeOrderByCreatedAtDesc(
                countryCode, mobileNumber, purpose);

        if (otpOpt.isEmpty()) {
            AuditLogger.logAction("OTP_VERIFICATION_FAILED", null, "Mobile: " + countryCode + mobileNumber + ", Purpose: " + purpose + ", Reason: No record found");
            return OtpVerificationResult.INVALID_CODE;
        }

        Otp otp = otpOpt.get();

        // 1. Check if blocked
        if (otp.getStatus() == OtpStatus.BLOCKED || 
                (otp.getBlockedUntil() != null && Instant.now().isBefore(otp.getBlockedUntil()))) {
            return OtpVerificationResult.BLOCKED;
        }

        // 2. Check if already verified or expired
        if (otp.getStatus() == OtpStatus.VERIFIED) {
            return OtpVerificationResult.INVALID_CODE;
        }

        if (otp.getStatus() == OtpStatus.EXPIRED || Instant.now().isAfter(otp.getExpiresAt())) {
            if (otp.getStatus() != OtpStatus.EXPIRED) {
                otp.setStatus(OtpStatus.EXPIRED);
                otpRepository.save(otp);
            }
            return OtpVerificationResult.EXPIRED;
        }

        // 3. Validate code
        boolean isValid = otpValidator.validate(code, otp.getOtpHash());

        if (isValid) {
            otp.setStatus(OtpStatus.VERIFIED);
            otp.setVerifiedAt(Instant.now());
            otpRepository.save(otp);

            AuditLogger.logAction("OTP_VERIFIED", null, "Mobile: " + countryCode + mobileNumber + ", Purpose: " + purpose);
            return OtpVerificationResult.SUCCESS;
        } else {
            otp.setAttemptCount(otp.getAttemptCount() + 1);

            if (otp.getAttemptCount() >= otp.getMaxAttempts()) {
                otp.setStatus(OtpStatus.BLOCKED);
                otp.setBlockedUntil(Instant.now().plus(Duration.ofMinutes(OtpConstants.BLOCK_DURATION_MINUTES)));
                otpRepository.save(otp);

                AuditLogger.logAction("OTP_BLOCKED", null, "Mobile: " + countryCode + mobileNumber + ", Purpose: " + purpose + ", Reason: Max attempts exceeded");
                return OtpVerificationResult.MAX_ATTEMPTS_EXCEEDED;
            } else {
                otpRepository.save(otp);

                AuditLogger.logAction("OTP_VERIFICATION_FAILED", null, "Mobile: " + countryCode + mobileNumber + ", Purpose: " + purpose + ", Attempt: " + otp.getAttemptCount());
                return OtpVerificationResult.INVALID_CODE;
            }
        }
    }
}
