package com.newtron.newtron_workforce_backend.dto;

import com.newtron.newtron_workforce_backend.enums.BloodGroup;
import com.newtron.newtron_workforce_backend.enums.Gender;
import com.newtron.newtron_workforce_backend.enums.OnboardingStep;
import lombok.*;

import java.time.Instant;
import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerBasicProfileResponse {

    private Long profileId;

    private String fullName;

    private LocalDate dateOfBirth;

    private Gender gender;

    private BloodGroup bloodGroup;

    private String photoStorageKey;

    private OnboardingStep currentStep;

    private OnboardingStep nextStep;

    private double completionPercentage;

    private Boolean isCompleted;

    private Instant createdAt;

    private Instant lastUpdatedAt;
}
