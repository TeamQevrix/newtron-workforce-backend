package com.newtron.newtron_workforce_backend.dto;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
public class WorkerAgreementDto {
    private Long agreementId;
    private Long applicationId;
    private Long jobId;
    private Long companyId;
    private Long workerId;
    
    // Snapshots
    private String workerNameSnapshot;
    private String companyNameSnapshot;
    private String jobTitleSnapshot;
    private String jobDescriptionSnapshot;
    private String workLocationSnapshot;
    private String primarySkillSnapshot;
    
    // Engagement & Compensation
    private String engagementType;
    private BigDecimal dailyWage;
    private BigDecimal monthlySalary;
    private BigDecimal commissionRate;
    private String commissionPayer;
    private BigDecimal commissionAmount;
    
    // Duration
    private String engagementDurationType;
    private Integer durationValue;
    private String duration;
    
    private String status;
    private Instant clientAcceptedAt;
    private Instant workerAcceptedAt;
    private Instant effectiveAt;
    private Instant completedAt;
    private String paymentResponsibility;
    private String paymentDueTerms;
    private Integer noticeDays;
    private String cancellationTerms;
    
    // Terms & Versioning
    private String clientCustomTerms;
    private Integer agreementVersion;
    private Boolean isLocked;
}
