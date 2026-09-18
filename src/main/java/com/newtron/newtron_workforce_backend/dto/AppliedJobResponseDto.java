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
    private Double dailyWage;
    private String appliedDate;
    private String status;
    private boolean recruiterViewed;
    private List<TimelineStepDto> timeline;
}
