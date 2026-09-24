package com.newtron.newtron_workforce_backend.entity;

import com.newtron.newtron_workforce_backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "settlements", uniqueConstraints = {
    @UniqueConstraint(columnNames = {"work_order_id", "settlement_month", "settlement_year"})
})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Settlement extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "work_order_id", nullable = false)
    private WorkOrder workOrder;

    @Column(name = "settlement_month", nullable = false)
    private Integer settlementMonth;

    @Column(name = "settlement_year", nullable = false)
    private Integer settlementYear;

    @Column(name = "worker_payable_amount", precision = 12, scale = 2)
    private BigDecimal workerPayableAmount;

    @Column(name = "newtron_commission_amount", precision = 12, scale = 2)
    private BigDecimal newtronCommissionAmount;

    @Column(name = "total_client_payable_amount", precision = 12, scale = 2)
    private BigDecimal totalClientPayableAmount;
}
