package com.newtron.newtron_workforce_backend.dto;

import lombok.Data;
import java.time.Instant;

@Data
public class WorkOrderCancelRequestDto {
    private Instant actualEndDate;
    private String cancellationReason;
}
