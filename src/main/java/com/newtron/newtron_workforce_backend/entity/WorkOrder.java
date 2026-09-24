package com.newtron.newtron_workforce_backend.entity;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.common.entity.BaseEntity;
import com.newtron.newtron_workforce_backend.enums.WorkOrderStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(name = "work_orders")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkOrder extends BaseEntity {

    @Column(name = "work_order_number", nullable = false, unique = true)
    private String workOrderNumber;

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "agreement_id", nullable = false, unique = true)
    private Agreement agreement;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "company_id", nullable = false)
    private Company company;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "job_id", nullable = false)
    private Job job;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_id", nullable = false)
    private User worker;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 50)
    private WorkOrderStatus status;

    @Column(name = "engagement_type", nullable = false, length = 50)
    private String engagementType;

    @Column(name = "daily_wage", precision = 12, scale = 2)
    private BigDecimal dailyWage;

    @Column(name = "monthly_salary", precision = 12, scale = 2)
    private BigDecimal monthlySalary;

    @Column(name = "commission_rate", nullable = false, precision = 5, scale = 4)
    private BigDecimal commissionRate;

    @Column(name = "commission_payer", length = 50)
    private String commissionPayer;

    @Column(name = "commission_amount", precision = 12, scale = 2)
    private BigDecimal commissionAmount;

    @Column(name = "expected_start_date")
    private Instant expectedStartDate;

    @Column(name = "expected_end_date")
    private Instant expectedEndDate;

    @Column(name = "actual_start_date")
    private Instant actualStartDate;

    @Column(name = "actual_end_date")
    private Instant actualEndDate;

    @Column(name = "cancellation_reason", length = 1000)
    private String cancellationReason;
}
