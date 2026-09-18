package com.newtron.newtron_workforce_backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class JobDetailsDto {
    private String id;
    private String companyId;
    private String companyName;
    private String companyLogo;
    private Boolean companyVerified;
    private Double companyRating;
    private String jobTitle;
    private Double dailyWage;
    private String employmentType;
    private String experienceRequired;
    private Integer openPositions;
    private String joiningDate;
    private String shiftHours;
    private String address;
    private String distance;
    private String description;
    private List<String> requiredSkills;
    private List<String> benefits;
    private String recruiterName;
    private String recruiterContact;
    private String applicationStatus;
    private Boolean isSaved;
    private Boolean isNotInterested;
    private List<String> photos;
}
