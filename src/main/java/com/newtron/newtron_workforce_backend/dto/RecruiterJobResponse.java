package com.newtron.newtron_workforce_backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecruiterJobResponse {
    private Long id;
    private String companyName;
    private String title;
    private String category;
    private String city;
    private Integer workersRequired;
    private String salary;
    private String duration;
    private String description;
    private String status;
    private java.math.BigDecimal monthlySalaryAmount;
}
