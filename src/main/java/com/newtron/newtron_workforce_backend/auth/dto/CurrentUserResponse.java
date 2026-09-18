package com.newtron.newtron_workforce_backend.auth.dto;

import lombok.Builder;
import lombok.Data;

import java.time.Instant;

@Data
@Builder
public class CurrentUserResponse {
    private Long userId;
    private String role;
    private String mobile;
    private String accountStatus;
    private String membershipStatus;
    private String membershipPlan;
    private String verificationStatus;
    private Double profileCompletion;
    private Boolean profileCompleted;
    private String onboardingStep;
    private Instant lastLogin;
}
