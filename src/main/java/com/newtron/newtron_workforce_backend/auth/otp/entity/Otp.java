package com.newtron.newtron_workforce_backend.auth.otp.entity;

import com.newtron.newtron_workforce_backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "otps", indexes = {
    @Index(name = "idx_otps_mobile_purpose_status", columnList = "mobile_country_code, mobile_number, purpose, status")
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Otp extends BaseEntity {

    @Column(name = "mobile_country_code", nullable = false, length = 5)
    private String mobileCountryCode;

    @Column(name = "mobile_number", nullable = false, length = 15)
    private String mobileNumber;

    @Column(name = "otp_hash", nullable = false, length = 64)
    private String otpHash;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 30)
    private OtpPurpose purpose;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false, length = 20)
    private OtpStatus status;

    @Enumerated(EnumType.STRING)
    @Column(name = "role", length = 20)
    private com.newtron.newtron_workforce_backend.auth.enums.Role role;


    @Column(name = "attempt_count", nullable = false)
    @Builder.Default
    private Integer attemptCount = 0;

    @Column(name = "max_attempts", nullable = false)
    @Builder.Default
    private Integer maxAttempts = 5;

    @Column(name = "expires_at", nullable = false)
    private Instant expiresAt;

    @Column(name = "verified_at")
    private Instant verifiedAt;

    @Column(name = "blocked_until")
    private Instant blockedUntil;

    @Column(name = "last_sent_at", nullable = false)
    private Instant lastSentAt;
}
