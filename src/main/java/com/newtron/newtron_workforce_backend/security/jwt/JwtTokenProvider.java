package com.newtron.newtron_workforce_backend.security.jwt;

import com.newtron.newtron_workforce_backend.common.exception.ExpiredTokenException;
import com.newtron.newtron_workforce_backend.common.exception.InvalidTokenException;
import com.newtron.newtron_workforce_backend.common.exception.MalformedTokenException;
import io.jsonwebtoken.*;
import io.jsonwebtoken.security.Keys;
import io.jsonwebtoken.security.SecurityException;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Component;

import javax.crypto.SecretKey;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Component
public class JwtTokenProvider {

    private final JwtProperties jwtProperties;
    private final Clock clock;
    private final SecretKey secretKey;

    public JwtTokenProvider(JwtProperties jwtProperties, Clock clock) {
        this.jwtProperties = jwtProperties;
        this.clock = clock;
        this.secretKey = Keys.hmacShaKeyFor(jwtProperties.getSecret().getBytes(StandardCharsets.UTF_8));
    }

    public String generateAccessToken(JwtPrincipal principal) {
        Date now = Date.from(clock.instant());
        Date expiry = new Date(now.getTime() + jwtProperties.getAccessExpiration());

        String authoritiesString = principal.getAuthorities().stream()
                .map(GrantedAuthority::getAuthority)
                .collect(Collectors.joining(","));

        return Jwts.builder()
                .subject(principal.getMobile())
                .issuer(jwtProperties.getIssuer())
                .audience().add(jwtProperties.getAudience()).and()
                .issuedAt(now)
                .expiration(expiry)
                .claim(JwtConstants.CLAIM_USER_ID, principal.getUserId())
                .claim(JwtConstants.CLAIM_AUTHORITIES, authoritiesString)
                .claim(JwtConstants.CLAIM_TOKEN_TYPE, JwtTokenType.ACCESS.name())
                .claim(JwtConstants.CLAIM_DEVICE_ID, principal.getDeviceId())
                .claim(JwtConstants.CLAIM_SESSION_ID, principal.getSessionId())
                .claim(JwtConstants.CLAIM_TOKEN_VERSION, principal.getTokenVersion())
                .claim(JwtConstants.CLAIM_USER_TYPE, principal.getUserType())
                .claim(JwtConstants.CLAIM_WORKER_ID, principal.getWorkerId())
                .claim(JwtConstants.CLAIM_COMPANY_ID, principal.getCompanyId())
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();
    }

    public String generateRefreshToken(String mobile, String deviceId, String sessionId) {
        Date now = Date.from(clock.instant());
        Date expiry = new Date(now.getTime() + jwtProperties.getRefreshExpiration());

        return Jwts.builder()
                .subject(mobile)
                .issuer(jwtProperties.getIssuer())
                .audience().add(jwtProperties.getAudience()).and()
                .issuedAt(now)
                .expiration(expiry)
                .claim(JwtConstants.CLAIM_TOKEN_TYPE, JwtTokenType.REFRESH.name())
                .claim(JwtConstants.CLAIM_DEVICE_ID, deviceId)
                .claim(JwtConstants.CLAIM_SESSION_ID, sessionId)
                .signWith(secretKey, Jwts.SIG.HS512)
                .compact();
    }

    public boolean validateAccessToken(String token) {
        return validateToken(token, JwtTokenType.ACCESS);
    }

    public boolean validateRefreshToken(String token) {
        return validateToken(token, JwtTokenType.REFRESH);
    }

    private boolean validateToken(String token, JwtTokenType expectedType) {
        try {
            Claims claims = getClaims(token);
            String tokenTypeStr = claims.get(JwtConstants.CLAIM_TOKEN_TYPE, String.class);
            if (tokenTypeStr == null || !tokenTypeStr.equalsIgnoreCase(expectedType.name())) {
                throw new InvalidTokenException("Invalid token type. Expected: " + expectedType.name());
            }
            return true;
        } catch (ExpiredTokenException | MalformedTokenException | InvalidTokenException e) {
            throw e;
        } catch (Exception e) {
            throw new InvalidTokenException("Token validation failed: " + e.getMessage());
        }
    }

    public Claims getClaims(String token) {
        try {
            return Jwts.parser()
                    .verifyWith(secretKey)
                    .clock(() -> Date.from(clock.instant()))
                    .build()
                    .parseSignedClaims(token)
                    .getPayload();
        } catch (ExpiredJwtException ex) {
            throw new ExpiredTokenException("JWT token has expired: " + ex.getMessage());
        } catch (MalformedJwtException ex) {
            throw new MalformedTokenException("JWT token is malformed: " + ex.getMessage());
        } catch (UnsupportedJwtException ex) {
            throw new InvalidTokenException("Unsupported JWT token: " + ex.getMessage());
        } catch (SecurityException ex) {
            throw new MalformedTokenException("JWT signature validation failed: " + ex.getMessage());
        } catch (IllegalArgumentException ex) {
            throw new InvalidTokenException("JWT claims string is empty: " + ex.getMessage());
        }
    }

    public JwtPrincipal getPrincipal(String token) {
        Claims claims = getClaims(token);
        String mobile = claims.getSubject();
        Long userId = claims.get(JwtConstants.CLAIM_USER_ID, Long.class);
        String authoritiesStr = claims.get(JwtConstants.CLAIM_AUTHORITIES, String.class);
        String deviceId = claims.get(JwtConstants.CLAIM_DEVICE_ID, String.class);
        String sessionId = claims.get(JwtConstants.CLAIM_SESSION_ID, String.class);
        Long tokenVersion = claims.get(JwtConstants.CLAIM_TOKEN_VERSION, Long.class);
        String userType = claims.get(JwtConstants.CLAIM_USER_TYPE, String.class);
        Long workerId = claims.get(JwtConstants.CLAIM_WORKER_ID, Long.class);
        Long companyId = claims.get(JwtConstants.CLAIM_COMPANY_ID, Long.class);

        Collection<? extends GrantedAuthority> authorities = List.of();
        if (authoritiesStr != null && !authoritiesStr.trim().isEmpty()) {
            authorities = Arrays.stream(authoritiesStr.split(","))
                    .map(String::trim)
                    .filter(s -> !s.isEmpty())
                    .map(SimpleGrantedAuthority::new)
                    .collect(Collectors.toList());
        }

        return JwtPrincipal.builder()
                .userId(userId)
                .mobile(mobile)
                .authorities(authorities)
                .deviceId(deviceId)
                .sessionId(sessionId)
                .tokenVersion(tokenVersion)
                .userType(userType)
                .workerId(workerId)
                .companyId(companyId)
                .build();
    }
}
