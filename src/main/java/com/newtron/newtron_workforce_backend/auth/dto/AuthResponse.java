package com.newtron.newtron_workforce_backend.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.util.Set;

@Data
@Builder
public class AuthResponse {
    private String accessToken;
    private String refreshToken;
    private String tokenType;
    private Long expiresIn;

    private Long userId;
    private String role;

    private Boolean isNewUser;
    private Boolean profileCompleted;
    private Boolean verificationCompleted;
    private String membershipStatus;

    private Boolean requiresOnboarding;

    private String displayName;
    private String profilePhoto;
    private Long workerId;
    private Long recruiterId;
    private String onboardingStep;
    private Set<String> permissions;
}
