package com.newtron.newtron_workforce_backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class OnDemandAvailabilityResponse {
    private Boolean isAvailableOnDemand;
    private String message;
}
