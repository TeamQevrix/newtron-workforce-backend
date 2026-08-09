package com.newtron.newtron_workforce_backend.entity;

import com.newtron.newtron_workforce_backend.common.entity.BaseEntity;
import com.newtron.newtron_workforce_backend.enums.*;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "worker_professional_details")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerProfessionalDetails extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_profile_id", nullable = false, unique = true)
    private WorkerProfile workerProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "highest_qualification_id")
    private Qualification highestQualification;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_employment_status", nullable = false, length = 30)
    private EmploymentStatus currentEmploymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "salary_type", nullable = false, length = 30)
    private SalaryType salaryType;

    @Column(name = "expected_salary", nullable = false, precision = 10, scale = 2)
    private BigDecimal expectedSalary;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_work_type", nullable = false, length = 30)
    private PreferredWorkType preferredWorkType;

    @Enumerated(EnumType.STRING)
    @Column(name = "preferred_shift", nullable = false, length = 30)
    private PreferredShift preferredShift;

    @Column(name = "immediate_joining", nullable = false)
    private Boolean immediateJoining;

    @Column(name = "current_company", length = 100)
    private String currentCompany;

    @Column(name = "current_designation", length = 100)
    private String currentDesignation;

    @Column(name = "notice_period_days")
    private Integer noticePeriodDays;
}
