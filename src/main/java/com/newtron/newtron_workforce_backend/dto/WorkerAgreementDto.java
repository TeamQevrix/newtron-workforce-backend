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
    private String engagementType;
    private BigDecimal dailyWage;
    private BigDecimal monthlySalary;
    private BigDecimal commissionRate;
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
}
