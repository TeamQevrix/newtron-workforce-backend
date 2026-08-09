package com.newtron.newtron_workforce_backend.auth.otp.provider;

import com.newtron.newtron_workforce_backend.auth.otp.entity.OtpPurpose;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.annotation.Profile;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@Profile({"dev", "default"})
public class ConsoleOtpProvider implements OtpProvider {

    @Override
    public void send(String countryCode, String mobileNumber, String otp, OtpPurpose purpose) {
        log.info("[DEVELOPMENT OTP] To: {}{} | Purpose: {} | Code: {}", 
                countryCode, mobileNumber, purpose, otp);
        System.out.printf("[DEVELOPMENT OTP] To: %s%s | Purpose: %s | Code: %s%n", 
                countryCode, mobileNumber, purpose, otp);
    }
}
