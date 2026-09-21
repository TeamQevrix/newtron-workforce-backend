package com.newtron.newtron_workforce_backend.dto;

import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnDemandAvailabilityRequest {
    @NotNull(message = "isAvailableOnDemand is required")
    private Boolean isAvailableOnDemand;
}
