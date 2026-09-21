package com.newtron.newtron_workforce_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class PublicWorkerProfileDto {
    private Long workerId;
    private String fullName;
    private String photoStorageKey;
    private String mainSkill;
    private List<String> skills;
    private String experienceYears;
    private Boolean isAvailableOnDemand;
    private String generalLocation;
}
