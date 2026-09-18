package com.newtron.newtron_workforce_backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerSummaryDto {
    private Long userId;
    private Long workerId;
    private String displayName;
    private String profilePhotoUrl;
    private String verificationStatus;
    private Boolean available;
    private String role;
    private String currentOnboardingStep;
    private Boolean profileCompleted;
    private Integer profileCompletionPercentage;
}
