package com.newtron.newtron_workforce_backend.auth.otp.service;

import com.newtron.newtron_workforce_backend.auth.otp.entity.OtpPurpose;
import com.newtron.newtron_workforce_backend.auth.otp.entity.OtpVerificationResult;

public interface OtpService {
    void sendOtp(String countryCode, String mobileNumber, OtpPurpose purpose, com.newtron.newtron_workforce_backend.auth.enums.Role role);
    OtpVerificationResult verifyOtp(String countryCode, String mobileNumber, OtpPurpose purpose, String code);
}
