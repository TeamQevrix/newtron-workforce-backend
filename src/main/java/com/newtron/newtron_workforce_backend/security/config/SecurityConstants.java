package com.newtron.newtron_workforce_backend.security.config;

import java.util.List;

public final class SecurityConstants {
    private SecurityConstants() {}

    public static final List<String> PUBLIC_URLS = List.of(
            "/auth/**",
            "/api/auth/**",
            "/api/v1/auth/send-otp",
            "/api/v1/auth/verify-otp",
            "/api/v1/auth/refresh-token",
            "/api/v1/auth/register",
            "/api/v1/auth/login",
            "/api/v1/master/**",
            "/otp/**",
            "/health",
            "/swagger-ui/**",
            "/v3/api-docs/**",
            "/api/v1/public/**"
    );
}
