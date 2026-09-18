package com.newtron.newtron_workforce_backend.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RecruiterJobCreateRequest {

    @NotBlank(message = "Requirement title is required")
    @Size(min = 2, max = 100, message = "Title must be between 2 and 100 characters")
    private String title;

    private String companyName;

    @NotBlank(message = "Worker skill category is required")
    private String category;

    @NotBlank(message = "Work location city is required")
    private String city;

    @NotNull(message = "Number of workers required is required")
    @Min(value = 1, message = "Workers required must be at least 1")
    private Integer workersRequired;

    @NotBlank(message = "Daily wage/salary is required")
    private String salary;

    @NotBlank(message = "Duration is required")
    private String duration;

    @NotBlank(message = "Work description is required")
    @Size(min = 10, max = 2000, message = "Description must be between 10 and 2000 characters")
    private String description;

    private String experienceRequired;

    private String shiftHours;

    private String benefits;

    private String joiningDate;
}
