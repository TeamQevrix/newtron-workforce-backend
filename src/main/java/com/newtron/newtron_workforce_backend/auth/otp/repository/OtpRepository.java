package com.newtron.newtron_workforce_backend.auth.otp.repository;

import com.newtron.newtron_workforce_backend.auth.otp.entity.Otp;
import com.newtron.newtron_workforce_backend.auth.otp.entity.OtpPurpose;
import com.newtron.newtron_workforce_backend.auth.otp.entity.OtpStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.time.Instant;
import java.util.Optional;

@Repository
public interface OtpRepository extends JpaRepository<Otp, Long> {

    Optional<Otp> findTopByMobileCountryCodeAndMobileNumberAndPurposeAndStatusOrderByCreatedAtDesc(
            String mobileCountryCode, String mobileNumber, OtpPurpose purpose, OtpStatus status);

    int countByMobileCountryCodeAndMobileNumberAndCreatedAtAfter(
            String mobileCountryCode, String mobileNumber, Instant after);

    Optional<Otp> findTopByMobileCountryCodeAndMobileNumberAndPurposeOrderByCreatedAtDesc(
            String mobileCountryCode, String mobileNumber, OtpPurpose purpose);
}
