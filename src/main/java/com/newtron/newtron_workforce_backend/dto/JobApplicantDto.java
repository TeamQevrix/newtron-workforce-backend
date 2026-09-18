package com.newtron.newtron_workforce_backend.dto;

import lombok.*;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class JobApplicantDto {
    private Long applicationId;
    private Long workerId;
    private String workerName;
    private String profilePhoto;
    private String skill;
    private Integer experienceYears;
    private String city;
    private Double rating;
    private Integer jobsCompleted;
    private String status;
    private String appliedDate;
    private Integer currentStep;
}
