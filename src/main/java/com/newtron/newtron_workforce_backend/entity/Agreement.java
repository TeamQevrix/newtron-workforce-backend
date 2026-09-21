package com.newtron.newtron_workforce_backend.entity;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.common.entity.BaseEntity;
import com.newtron.newtron_workforce_backend.enums.AgreementStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "agreements")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Agreement extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "application_id", nullable = false, unique = true)
    private Application application;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private User worker;

    @Column(name = "engagement_type", nullable = false, length = 50)
    private String engagementType;

    @Column(name = "engagement_duration_type", length = 30)
    private String engagementDurationType;

    @Column(name = "duration_value")
    private Integer durationValue;

    @Column(name = "duration")
    private String duration;

    @Column(name = "daily_wage", precision = 12, scale = 2)
    private BigDecimal dailyWage;

    @Column(name = "monthly_salary", precision = 12, scale = 2)
    private BigDecimal monthlySalary;

    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 4)
    @Builder.Default
    private BigDecimal commissionRate = new BigDecimal("0.0500");

    @Column(name = "payment_responsibility", length = 100)
    private String paymentResponsibility;

    @Column(name = "payment_due_terms", length = 255)
    private String paymentDueTerms;

    @Column(name = "notice_days")
    private Integer noticeDays;

    @Column(name = "cancellation_terms", length = 1000)
    private String cancellationTerms;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    @Builder.Default
    private AgreementStatus status = AgreementStatus.DRAFT;

    @Column(name = "client_accepted_at")
    private Instant clientAcceptedAt;

    @Column(name = "worker_accepted_at")
    private Instant workerAcceptedAt;

    @Column(name = "effective_at")
    private Instant effectiveAt;

    @Column(name = "completed_at")
    private Instant completedAt;
    // PHASE 4B IMMUTABLE SNAPSHOT FIELDS
    @Column(name = "worker_name_snapshot", length = 255)
    private String workerNameSnapshot;

    @Column(name = "company_name_snapshot", length = 255)
    private String companyNameSnapshot;

    @Column(name = "job_title_snapshot", length = 255)
    private String jobTitleSnapshot;

    @Column(name = "job_description_snapshot", columnDefinition = "TEXT")
    private String jobDescriptionSnapshot;

    @Column(name = "work_location_snapshot", length = 255)
    private String workLocationSnapshot;

    @Column(name = "primary_skill_snapshot", length = 255)
    private String primarySkillSnapshot;

    // PHASE 4B COMMERCIAL SNAPSHOT
    @Column(name = "commission_payer", length = 50)
    private String commissionPayer;

    @Column(name = "commission_amount", precision = 12, scale = 2)
    private BigDecimal commissionAmount;

    // PHASE 4B CLIENT CUSTOM TERMS
    @Column(name = "client_custom_terms", columnDefinition = "TEXT")
    private String clientCustomTerms;

    // PHASE 4B AGREEMENT VERSION / LOCKING FOUNDATION
    @Column(name = "agreement_version", nullable = false)
    @Builder.Default
    private Integer agreementVersion = 1;

    @Column(name = "is_locked", nullable = false)
    @Builder.Default
    private Boolean isLocked = false;

    // PHASE 4B STANDARD TERMS / DOCUMENT / EXPIRY FOUNDATION
    @Column(name = "standard_terms_version", length = 50)
    private String standardTermsVersion;

    @Column(name = "document_url", length = 1000)
    private String documentUrl;

    @Column(name = "expires_at")
    private Instant expiresAt;
}
