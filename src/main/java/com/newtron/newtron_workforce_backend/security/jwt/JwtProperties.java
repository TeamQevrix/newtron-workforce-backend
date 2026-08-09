package com.newtron.newtron_workforce_backend.security.jwt;

import jakarta.annotation.PostConstruct;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.context.annotation.Configuration;

import java.nio.charset.StandardCharsets;

@Configuration
@ConfigurationProperties(prefix = "jwt")
@Getter
@Setter
public class JwtProperties {

    private String secret;
    private long accessExpiration;
    private long refreshExpiration;
    private String issuer = "newtron-workforce";
    private String audience = "mobile-app";

    @PostConstruct
    public void validateSecret() {
        if (secret == null || secret.trim().isEmpty()) {
            throw new IllegalStateException("JWT Secret is not configured. Please set the JWT_SECRET environment variable.");
        }
        byte[] bytes = secret.getBytes(StandardCharsets.UTF_8);
        if (bytes.length < 64) {
            throw new IllegalStateException("JWT Secret must be at least 64 bytes (512 bits) to support HS512 algorithm securely. Current length is " + bytes.length + " bytes.");
        }
    }
}
