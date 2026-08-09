package com.newtron.newtron_workforce_backend.auth.repository;


import com.newtron.newtron_workforce_backend.auth.entity.OtpVerification;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface OtpVerificationRepository extends JpaRepository<OtpVerification, Long> {

    Optional<OtpVerification> findTopByMobileOrderByCreatedAtDesc(String mobile);
}