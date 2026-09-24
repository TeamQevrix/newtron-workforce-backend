package com.newtron.newtron_workforce_backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CalculateSettlementRequestDto {

    @NotNull(message = "WorkOrderId is required")
    private Long workOrderId;

    @NotNull(message = "Settlement month is required")
    @Min(value = 1, message = "Settlement month must be between 1 and 12")
    @Max(value = 12, message = "Settlement month must be between 1 and 12")
    private Integer settlementMonth;

    @NotNull(message = "Settlement year is required")
    @Min(value = 2000, message = "Settlement year must be valid")
    private Integer settlementYear;
}
