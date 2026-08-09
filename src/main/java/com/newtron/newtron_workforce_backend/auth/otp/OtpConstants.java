package com.newtron.newtron_workforce_backend.auth.otp;

public final class OtpConstants {

    private OtpConstants() {
        // Prevent instantiation
    }

    public static final int OTP_LENGTH = 6;
    public static final int EXPIRY_MINUTES = 5;
    public static final int RESEND_INTERVAL_SECONDS = 60;
    public static final int MAX_ATTEMPTS = 5;
    public static final int MAX_REQUESTS_IN_WINDOW = 3;
    public static final int RATE_LIMIT_WINDOW_MINUTES = 15;
    public static final int BLOCK_DURATION_MINUTES = 30;
}
