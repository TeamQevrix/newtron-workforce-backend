package com.newtron.newtron_workforce_backend.auth.otp.entity;

public enum OtpVerificationResult {
    SUCCESS,
    EXPIRED,
    BLOCKED,
    INVALID_CODE,
    MAX_ATTEMPTS_EXCEEDED
}
