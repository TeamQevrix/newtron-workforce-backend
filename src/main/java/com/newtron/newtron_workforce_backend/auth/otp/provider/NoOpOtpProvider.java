package com.newtron.newtron_workforce_backend.auth.otp.provider;

import com.newtron.newtron_workforce_backend.auth.otp.entity.OtpPurpose;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnMissingBean(OtpProvider.class)
public class NoOpOtpProvider implements OtpProvider {

    @Override
    public void send(String countryCode, String mobileNumber, String otp, OtpPurpose purpose) {
        log.warn("No active OtpProvider registered. OTP was generated but not sent. Purpose: {}, To: {}{}",
                purpose, countryCode, mobileNumber);
    }
}
