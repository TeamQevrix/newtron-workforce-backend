package com.newtron.newtron_workforce_backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerAvailabilityDto {
    
    @NotNull(message = "Availability value is required")
    private Boolean available;
}
