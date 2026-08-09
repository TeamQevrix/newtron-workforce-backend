package com.newtron.newtron_workforce_backend.security.jwt;

public final class JwtConstants {
    private JwtConstants() {}

    public static final String TOKEN_HEADER = "Authorization";
    public static final String TOKEN_PREFIX = "Bearer ";

    // Claim Names
    public static final String CLAIM_USER_ID = "userId";
    public static final String CLAIM_AUTHORITIES = "authorities";
    public static final String CLAIM_TOKEN_TYPE = "tokenType";
    public static final String CLAIM_DEVICE_ID = "deviceId";
    public static final String CLAIM_SESSION_ID = "sessionId";
    public static final String CLAIM_TOKEN_VERSION = "tokenVersion";
    public static final String CLAIM_USER_TYPE = "userType";
    public static final String CLAIM_WORKER_ID = "workerId";
    public static final String CLAIM_COMPANY_ID = "companyId";
}
