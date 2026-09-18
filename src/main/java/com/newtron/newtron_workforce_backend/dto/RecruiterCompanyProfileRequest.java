package com.newtron.newtron_workforce_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RecruiterCompanyProfileRequest {

    @NotBlank(message = "Company name is required")
    @Size(min = 2, max = 150, message = "Company name must be between 2 and 150 characters")
    private String companyName;

    @NotBlank(message = "Contact person name is required")
    private String contactPersonName;

    @NotBlank(message = "Contact mobile number is required")
    private String contactMobile;

    @NotBlank(message = "City is required")
    private String city;

    private String email;

    @Size(max = 500, message = "Address cannot exceed 500 characters")
    private String address;

    private String pincode;

    @Size(max = 1000, message = "Description cannot exceed 1000 characters")
    private String description;
}
