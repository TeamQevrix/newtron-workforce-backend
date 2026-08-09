package com.newtron.newtron_workforce_backend.auth.dto;

import com.newtron.newtron_workforce_backend.auth.otp.entity.OtpPurpose;
import com.newtron.newtron_workforce_backend.common.validation.annotation.ValidMobile;
import com.newtron.newtron_workforce_backend.auth.enums.Role;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class SendOtpRequest {

    @NotBlank(message = "Country code is required")
    @Pattern(regexp = "^\\+\\d{1,4}$", message = "Invalid country code format (e.g. +91)")
    private String mobileCountryCode;

    @NotBlank(message = "Mobile number is required")
    @ValidMobile
    private String mobileNumber;

    @NotNull(message = "OTP purpose is required")
    private OtpPurpose purpose;

    private Role role;
}
