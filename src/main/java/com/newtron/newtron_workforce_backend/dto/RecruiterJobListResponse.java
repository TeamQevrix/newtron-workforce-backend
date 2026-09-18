package com.newtron.newtron_workforce_backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecruiterJobListResponse {
    private Long id;
    private String companyName;
    private String title;
    private String category;
    private String city;
    private String salary;
    private String duration;
    private String description;
    private String status;
    private Integer workersRequired;
    private Long applicantsCount;
    private Long hiredCount;
    private java.math.BigDecimal monthlySalaryAmount;
}
