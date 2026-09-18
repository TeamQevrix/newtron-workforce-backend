package com.newtron.newtron_workforce_backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobRecommendationDto {
    private String id;
    private String company;
    private String jobTitle;
    private String location;
    private Double dailyWage;
    private String shiftHours;
    private Double rating;
    private String distance;
    private Boolean isApplied;
}
