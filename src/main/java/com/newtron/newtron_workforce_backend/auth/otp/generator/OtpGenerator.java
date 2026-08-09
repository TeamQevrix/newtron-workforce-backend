package com.newtron.newtron_workforce_backend.auth.otp.generator;

import com.newtron.newtron_workforce_backend.auth.otp.OtpConstants;
import org.springframework.stereotype.Component;

import java.security.SecureRandom;

@Component
public class OtpGenerator {

    private final SecureRandom secureRandom = new SecureRandom();

    public String generate() {
        int length = OtpConstants.OTP_LENGTH;
        StringBuilder otp = new StringBuilder(length);
        for (int i = 0; i < length; i++) {
            otp.append(secureRandom.nextInt(10));
        }
        return otp.toString();
    }
}
