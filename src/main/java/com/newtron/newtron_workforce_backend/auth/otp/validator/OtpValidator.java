package com.newtron.newtron_workforce_backend.auth.otp.validator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;

@Slf4j
@Component
public class OtpValidator {

    private final String pepper;

    public OtpValidator(@Value("${OTP_PEPPER:default_secure_otp_pepper}") String pepper) {
        this.pepper = pepper;
    }

    public String hashOtp(String rawOtp) {
        try {
            String saltedInput = rawOtp + pepper;
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] encodedHash = digest.digest(saltedInput.getBytes(StandardCharsets.UTF_8));
            return bytesToHex(encodedHash);
        } catch (NoSuchAlgorithmException e) {
            log.error("SHA-256 algorithm not found", e);
            throw new IllegalStateException("Failed to initialize SHA-256 algorithm", e);
        }
    }

    public boolean validate(String rawOtp, String otpHash) {
        if (rawOtp == null || otpHash == null) {
            return false;
        }
        return hashOtp(rawOtp).equals(otpHash);
    }

    private static String bytesToHex(byte[] hash) {
        StringBuilder hexString = new StringBuilder(2 * hash.length);
        for (byte b : hash) {
            String hex = Integer.toHexString(0xff & b);
            if (hex.length() == 1) {
                hexString.append('0');
            }
            hexString.append(hex);
        }
        return hexString.toString();
    }
}
