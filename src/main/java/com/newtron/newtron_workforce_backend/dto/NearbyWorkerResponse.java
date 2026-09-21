package com.newtron.newtron_workforce_backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class NearbyWorkerResponse {
    private Long workerId;
    private String fullName;
    private String photoStorageKey;
    private String mainSkill;
    private Integer experienceYears;
    private Double distanceKm;
    private Boolean isAvailableOnDemand;
}
