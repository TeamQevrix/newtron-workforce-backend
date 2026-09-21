package com.newtron.newtron_workforce_backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecruiterJobDetailResponse {
    private Long id;
    private String title;
    private String companyName;
    private String category;
    private String city;
    private Integer workersRequired;
    private String engagementType;
    private String salary;
    private java.math.BigDecimal monthlySalaryAmount;
    private String engagementDurationType;
    private Integer durationValue;
    private String duration;
    private String description;
    private String experienceRequired;
    private String shiftHours;
    private String benefits;
    private String joiningDate;
    private String status;
}
