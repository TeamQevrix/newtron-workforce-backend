package com.newtron.newtron_workforce_backend.auth.entity;

import com.newtron.newtron_workforce_backend.common.entity.BaseEntity;
import com.newtron.newtron_workforce_backend.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.time.Instant;

@Entity
@Table(name = "user_sessions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserSession extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY, optional = false)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @Column(name = "refresh_token_hash", nullable = false, length = 64)
    private String refreshTokenHash;

    @Column(name = "device_id", nullable = false, length = 100)
    private String deviceId;

    @Column(name = "device_name", length = 150)
    private String deviceName;

    @Column(name = "device_type", length = 50)
    private String deviceType;

    @Column(name = "os_version", length = 50)
    private String osVersion;

    @Column(name = "app_version", length = 50)
    private String appVersion;

    @Column(name = "fcm_token", length = 255)
    private String fcmToken;

    @Column(name = "active", nullable = false)
    @Builder.Default
    private Boolean active = true;

    @Column(name = "last_activity_at", nullable = false)
    @Builder.Default
    private Instant lastActivityAt = Instant.now();
}
