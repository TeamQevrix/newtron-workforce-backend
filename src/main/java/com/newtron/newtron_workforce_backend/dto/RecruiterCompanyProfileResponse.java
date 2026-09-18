package com.newtron.newtron_workforce_backend.dto;

import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class RecruiterCompanyProfileResponse {
    private Long id;
    private String companyName;
    private String contactPersonName;
    private String contactMobile;
    private String email;
    private String city;
    private String address;
    private String pincode;
    private String description;
    private Boolean profileCompleted;
}
