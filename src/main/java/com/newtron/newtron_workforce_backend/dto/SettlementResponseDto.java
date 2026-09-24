package com.newtron.newtron_workforce_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.math.BigDecimal;
import java.time.Instant;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SettlementResponseDto {
    private Long id;
    private Long workOrderId;
    private String workOrderNumber;
    private Integer settlementMonth;
    private Integer settlementYear;
    private Long workerId;
    private String workerName;
    private BigDecimal workerPayableAmount;
    private BigDecimal newtronCommissionAmount;
    private BigDecimal totalClientPayableAmount;
    private Instant createdAt;
    private Instant updatedAt;
}
