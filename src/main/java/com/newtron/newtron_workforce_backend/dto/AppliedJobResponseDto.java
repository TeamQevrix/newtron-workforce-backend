package com.newtron.newtron_workforce_backend.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AppliedJobResponseDto {
    private String id;
    private String jobId;
    private String jobTitle;
    private String company;
    private String companyLogo;
    private String location;
    private Double dailyWage; // Keeping for backwards compatibility
    private Double wageAmount;
    private String wageType;
    private String appliedDate;
    private String status;
    private boolean recruiterViewed;
    private String clientPhone;
    private Double latitude;
    private Double longitude;
    private List<TimelineStepDto> timeline;
}
