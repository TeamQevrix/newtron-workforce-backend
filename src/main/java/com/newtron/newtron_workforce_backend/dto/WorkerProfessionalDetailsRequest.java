package com.newtron.newtron_workforce_backend.dto;

import com.newtron.newtron_workforce_backend.enums.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerProfessionalDetailsRequest {

    @NotEmpty(message = "At least one skill is required")
    @Valid
    private List<WorkerSkillDto> skills;

    @NotNull(message = "Highest qualification ID is required")
    private Long highestQualificationId;

    @NotNull(message = "Current employment status is required")
    private EmploymentStatus currentEmploymentStatus;

    @NotNull(message = "Salary type is required")
    private SalaryType salaryType;

    @DecimalMin(value = "0.01", message = "Expected salary must be greater than 0")
    private BigDecimal expectedSalary;

    @DecimalMin(value = "0.01", message = "Expected monthly salary must be greater than 0")
    private BigDecimal expectedMonthlySalary;

    @NotNull(message = "Preferred work type is required")
    private PreferredWorkType preferredWorkType;

    @NotNull(message = "Preferred shift is required")
    private PreferredShift preferredShift;

    @NotNull(message = "Immediate joining flag is required")
    private Boolean immediateJoining;

    private String currentCompany;

    private String currentDesignation;

    @Min(value = 0, message = "Notice period days cannot be negative")
    private Integer noticePeriodDays;
}
