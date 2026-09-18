package com.newtron.newtron_workforce_backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobDetailDto {
    private String id;
    private String company;
    private String companyLogo;
    private String jobTitle;
    private String location;
    private Double dailyWage;
    private String employmentType;
    private String distance;
    private String postedTime;
    private Boolean verified;
    private Double rating;
    private String status;
}
