package com.newtron.newtron_workforce_backend.auth.otp.provider;

import com.newtron.newtron_workforce_backend.auth.otp.entity.OtpPurpose;

public interface OtpProvider {
    void send(String countryCode, String mobileNumber, String otp, OtpPurpose purpose);
}
