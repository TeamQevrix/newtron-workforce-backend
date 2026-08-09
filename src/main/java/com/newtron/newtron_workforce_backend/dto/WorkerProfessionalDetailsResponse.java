package com.newtron.newtron_workforce_backend.dto;

import com.newtron.newtron_workforce_backend.enums.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerProfessionalDetailsResponse {

    private List<WorkerSkillDto> skills;
    private Long highestQualificationId;
    private String highestQualificationName;
    private EmploymentStatus currentEmploymentStatus;
    private SalaryType salaryType;
    private BigDecimal expectedSalary;
    private PreferredWorkType preferredWorkType;
    private PreferredShift preferredShift;
    private Boolean immediateJoining;
    private String currentCompany;
    private String currentDesignation;
    private Integer noticePeriodDays;

    // Onboarding progress fields
    private OnboardingStep currentStep;
    private OnboardingStep nextStep;
    private Double completionPercentage;

    private java.time.Instant updatedAt;
}
