package com.newtron.newtron_workforce_backend.auth.dto;

import com.newtron.newtron_workforce_backend.auth.otp.entity.OtpPurpose;
import com.newtron.newtron_workforce_backend.common.validation.annotation.ValidMobile;
import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class VerifyOtpRequest {

    @NotBlank(message = "Country code is required")
    @Pattern(regexp = "^\\+\\d{1,4}$", message = "Invalid country code format")
    private String mobileCountryCode;

    @NotBlank(message = "Mobile number is required")
    @ValidMobile
    private String mobileNumber;

    @NotBlank(message = "OTP is required")
    @Pattern(regexp = "^\\d{6}$", message = "OTP must be exactly 6 digits")
    private String otp;

    @NotNull(message = "Purpose is required")
    private OtpPurpose purpose;

    @NotNull(message = "Device information is required")
    @Valid
    private DeviceInfoDto deviceInformation;
}
