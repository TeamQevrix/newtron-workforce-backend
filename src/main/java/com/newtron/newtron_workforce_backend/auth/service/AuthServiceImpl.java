package com.newtron.newtron_workforce_backend.auth.service;

import com.newtron.newtron_workforce_backend.auth.dto.*;
import com.newtron.newtron_workforce_backend.auth.otp.entity.Otp;
import com.newtron.newtron_workforce_backend.auth.otp.entity.OtpStatus;
import com.newtron.newtron_workforce_backend.auth.otp.entity.OtpVerificationResult;
import com.newtron.newtron_workforce_backend.auth.entity.UserSession;
import com.newtron.newtron_workforce_backend.auth.otp.OtpConstants;
import com.newtron.newtron_workforce_backend.auth.otp.repository.OtpRepository;
import com.newtron.newtron_workforce_backend.auth.otp.service.OtpService;
import com.newtron.newtron_workforce_backend.auth.repository.UserSessionRepository;
import com.newtron.newtron_workforce_backend.auth.util.IpRateLimiter;
import com.newtron.newtron_workforce_backend.common.exception.BusinessException;
import com.newtron.newtron_workforce_backend.common.exception.ConflictException;
import com.newtron.newtron_workforce_backend.common.exception.ResourceNotFoundException;
import com.newtron.newtron_workforce_backend.common.exception.UnauthorizedException;
import com.newtron.newtron_workforce_backend.common.logging.AuditLogger;
import com.newtron.newtron_workforce_backend.auth.enums.Role;
import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.entity.WorkerProfile;
import com.newtron.newtron_workforce_backend.repository.WorkerProfileRepository;
import com.newtron.newtron_workforce_backend.entity.WorkerMembership;
import com.newtron.newtron_workforce_backend.repository.WorkerMembershipRepository;
import com.newtron.newtron_workforce_backend.security.jwt.JwtConstants;
import com.newtron.newtron_workforce_backend.security.jwt.JwtPrincipal;
import com.newtron.newtron_workforce_backend.security.jwt.JwtProperties;
import com.newtron.newtron_workforce_backend.security.jwt.JwtTokenProvider;
import io.jsonwebtoken.Claims;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.Instant;
import java.util.*;

import org.springframework.security.crypto.password.PasswordEncoder;

@Slf4j
@Service
@RequiredArgsConstructor
public class AuthServiceImpl implements AuthService {

    private final OtpService otpService;
    private final OtpRepository otpRepository;
    private final UserRepository userRepository;
    private final UserSessionRepository userSessionRepository;
    private final JwtTokenProvider jwtTokenProvider;
    private final JwtProperties jwtProperties;
    private final IpRateLimiter ipRateLimiter;
    private final WorkerProfileRepository workerProfileRepository;
    private final WorkerMembershipRepository workerMembershipRepository;
    private final com.newtron.newtron_workforce_backend.repository.TeamRepository teamRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void sendOtp(SendOtpRequest request, String ipAddress) {
        // IP Rate limiting: Max 5 Send OTP requests per minute per IP
        if (!ipRateLimiter.isAllowed(ipAddress, 5, 60000)) {
            throw new BusinessException("IP_RATE_LIMIT_EXCEEDED", "Too many requests from this IP. Please try again in a minute.");
        }

        String fullMobile = request.getMobileCountryCode() + request.getMobileNumber();
        java.util.Optional<User> userOpt = userRepository.findByMobile(fullMobile);

        if (request.getPurpose() == com.newtron.newtron_workforce_backend.auth.otp.entity.OtpPurpose.SIGNUP) {
            if (userOpt.isPresent()) {
                throw new ConflictException("USER_ALREADY_EXISTS", "An account already exists with this mobile number. Please login.");
            }
        } else if (request.getPurpose() == com.newtron.newtron_workforce_backend.auth.otp.entity.OtpPurpose.LOGIN) {
            if (userOpt.isEmpty()) {
                throw new ResourceNotFoundException("USER_NOT_FOUND", "No account found. Please create an account.");
            }
        } else if (request.getPurpose() == com.newtron.newtron_workforce_backend.auth.otp.entity.OtpPurpose.FORGOT_PASSWORD) {
            if (userOpt.isEmpty()) {
                // Do not send OTP to avoid enumeration, but simulate success
                return;
            }
        }

        // Delegate to OtpService
        otpService.sendOtp(request.getMobileCountryCode(), request.getMobileNumber(), request.getPurpose(), request.getRole());
    }

    @Override
    @Transactional
    public AuthResponse verifyOtp(VerifyOtpRequest request) {
        String fullMobile = request.getMobileCountryCode() + request.getMobileNumber();

        // 1. Verify OTP
        OtpVerificationResult result = otpService.verifyOtp(
                request.getMobileCountryCode(), request.getMobileNumber(), request.getPurpose(), request.getOtp());

        if (result != OtpVerificationResult.SUCCESS) {
            switch (result) {
                case EXPIRED:
                    throw new BusinessException("OTP_EXPIRED", "OTP has expired.");
                case BLOCKED:
                    throw new BusinessException("OTP_BLOCKED", "OTP verification is blocked. Please try again later.");
                case MAX_ATTEMPTS_EXCEEDED:
                    throw new BusinessException("OTP_MAX_ATTEMPTS_EXCEEDED", "Maximum verification attempts exceeded. Verification blocked.");
                case INVALID_CODE:
                default:
                    throw new BusinessException("INVALID_OTP", "Invalid OTP entered.");
            }
        }

        // Retrieve latest OTP to load the role associated with the session
        Otp otp = otpRepository.findTopByMobileCountryCodeAndMobileNumberAndPurposeOrderByCreatedAtDesc(
                request.getMobileCountryCode(), request.getMobileNumber(), request.getPurpose())
                .orElseThrow(() -> new BusinessException("OTP_RECORD_NOT_FOUND", "No OTP record found."));

        Optional<User> userOpt = userRepository.findByMobile(fullMobile);
        User user;

        if (request.getPurpose() == com.newtron.newtron_workforce_backend.auth.otp.entity.OtpPurpose.LOGIN) {
            if (userOpt.isEmpty()) {
                throw new ResourceNotFoundException("ACCOUNT_NOT_FOUND", "Account not found. Create a new account?");
            }
            user = userOpt.get();
        } else if (request.getPurpose() == com.newtron.newtron_workforce_backend.auth.otp.entity.OtpPurpose.SIGNUP) {
            if (userOpt.isPresent()) {
                throw new ConflictException("ACCOUNT_ALREADY_EXISTS", "Account already exists with this mobile number.");
            }
            Role role = otp.getRole();
            if (role == null) {
                role = Role.WORKER; // Fallback
            }

            user = User.builder()
                    .uuid(UUID.randomUUID().toString())
                    .fullName("")
                    .mobile(fullMobile)
                    .role(role)
                    .status(com.newtron.newtron_workforce_backend.auth.enums.UserStatus.ACTIVE)
                    .profileCompleted(false)
                    .mobileVerified(true)
                    .tokenVersion(0L)
                    .build();

            userRepository.save(user);
            AuditLogger.logAction("USER_REGISTERED", user.getId().toString(), "Role: " + role);
        } else if (request.getPurpose() == com.newtron.newtron_workforce_backend.auth.otp.entity.OtpPurpose.FORGOT_PASSWORD) {
            if (userOpt.isEmpty()) {
                throw new BusinessException("INVALID_OTP", "Invalid OTP entered."); // Prevents enumeration
            }
            user = userOpt.get();
            String rawToken = UUID.randomUUID().toString();
            user.setPasswordResetTokenHash(hashToken(rawToken));
            user.setPasswordResetExpiresAt(java.time.LocalDateTime.now().plusMinutes(15));
            userRepository.save(user);
            return AuthResponse.builder()
                    .verificationCompleted(true)
                    .resetToken(rawToken)
                    .build();
        } else {
            // Other purposes: verify only
            return AuthResponse.builder()
                    .verificationCompleted(true)
                    .build();
        }

        // Establish User Session
        String sessionId = UUID.randomUUID().toString();
        DeviceInfoDto deviceInfo = request.getDeviceInformation();

        JwtPrincipal principal = JwtPrincipal.builder()
                .userId(user.getId())
                .mobile(user.getMobile())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .deviceId(deviceInfo.getDeviceId())
                .sessionId(sessionId)
                .tokenVersion(user.getTokenVersion())
                .userType(user.getRole().name())
                .build();

        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getMobile(), deviceInfo.getDeviceId(), sessionId);
        String tokenHash = hashToken(refreshToken);

        // Deactivate previous active sessions for the same device
        userSessionRepository.findByUserIdAndDeviceIdAndActiveTrue(user.getId(), deviceInfo.getDeviceId())
                .ifPresent(prevSession -> {
                    prevSession.setActive(false);
                    userSessionRepository.save(prevSession);
                });

        UserSession session = UserSession.builder()
                .user(user)
                .refreshTokenHash(tokenHash)
                .deviceId(deviceInfo.getDeviceId())
                .deviceName(deviceInfo.getDeviceName())
                .deviceType(deviceInfo.getDeviceType())
                .osVersion(deviceInfo.getOsVersion())
                .appVersion(deviceInfo.getAppVersion())
                .fcmToken(deviceInfo.getFcmToken())
                .active(true)
                .lastActivityAt(Instant.now())
                .build();

        userSessionRepository.save(session);
        AuditLogger.logAction("USER_LOGGED_IN", user.getId().toString(), "Device: " + deviceInfo.getDeviceId());

        String onboardingStep = "BASIC_PROFILE";
        if (user.getRole() == Role.WORKER) {
            Optional<WorkerProfile> profileOpt = workerProfileRepository.findByUserId(user.getId());
            if (profileOpt.isPresent()) {
                onboardingStep = profileOpt.get().getCurrentStep().name();
            }
        } else {
            onboardingStep = user.getProfileCompleted() ? "COMPLETED" : "BASIC_PROFILE";
        }

        boolean isCompleted = isUserProfileCompleted(user);

        String membershipPlan = getMembershipPlan(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessExpiration() / 1000)
                .userId(user.getId())
                .role(user.getRole().name())
                .isNewUser(request.getPurpose() == com.newtron.newtron_workforce_backend.auth.otp.entity.OtpPurpose.SIGNUP)
                .profileCompleted(isCompleted)
                .verificationCompleted(user.getMobileVerified())
                .membershipStatus(getMembershipStatus(user))
                .membershipPlan(membershipPlan)
                .requiresOnboarding(!isCompleted)
                .displayName(user.getFullName())
                .profilePhoto(null)
                .workerId(null)
                .recruiterId(null)
                .onboardingStep(onboardingStep)
                .permissions(Set.of("ROLE_" + user.getRole().name()))
                .build();
    }

    @Override
    @Transactional
    public AuthResponse refreshToken(RefreshTokenRequest request) {
        // Validate Token signature and expiry
        jwtTokenProvider.validateRefreshToken(request.getRefreshToken());
        Claims claims = jwtTokenProvider.getClaims(request.getRefreshToken());

        String mobile = claims.getSubject();
        String sessionId = claims.get(JwtConstants.CLAIM_SESSION_ID, String.class);
        String deviceId = claims.get(JwtConstants.CLAIM_DEVICE_ID, String.class);

        if (!request.getDeviceId().equals(deviceId)) {
            throw new UnauthorizedException("DEVICE_MISMATCH", "Device mismatch for refresh token.");
        }

        String hash = hashToken(request.getRefreshToken());
        UserSession session = userSessionRepository.findByRefreshTokenHashAndActiveTrue(hash)
                .orElseThrow(() -> new UnauthorizedException("SESSION_INVALID", "Session is inactive or invalid."));

        User user = session.getUser();

        // Validate token version for logout-all checks
        JwtPrincipal principal = JwtPrincipal.builder()
                .userId(user.getId())
                .mobile(user.getMobile())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .deviceId(deviceId)
                .sessionId(sessionId)
                .tokenVersion(user.getTokenVersion())
                .userType(user.getRole().name())
                .build();

        // Rotate refresh token
        String newAccessToken = jwtTokenProvider.generateAccessToken(principal);
        String newRefreshToken = jwtTokenProvider.generateRefreshToken(user.getMobile(), deviceId, sessionId);

        // Deactivate old session
        session.setActive(false);
        userSessionRepository.save(session);

        // Save new rotated session
        UserSession newSession = UserSession.builder()
                .user(user)
                .refreshTokenHash(hashToken(newRefreshToken))
                .deviceId(deviceId)
                .deviceName(session.getDeviceName())
                .deviceType(session.getDeviceType())
                .osVersion(session.getOsVersion())
                .appVersion(session.getAppVersion())
                .fcmToken(session.getFcmToken())
                .active(true)
                .lastActivityAt(Instant.now())
                .build();

        userSessionRepository.save(newSession);

        boolean isCompleted = isUserProfileCompleted(user);

        String membershipPlan = getMembershipPlan(user);

        return AuthResponse.builder()
                .accessToken(newAccessToken)
                .refreshToken(newRefreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessExpiration() / 1000)
                .userId(user.getId())
                .role(user.getRole().name())
                .profileCompleted(isCompleted)
                .verificationCompleted(user.getMobileVerified())
                .membershipStatus(getMembershipStatus(user))
                .membershipPlan(membershipPlan)
                .requiresOnboarding(!isCompleted)
                .displayName(user.getFullName())
                .build();
    }

    @Override
    @Transactional
    public void logout(LogoutRequest request) {
        String hash = hashToken(request.getRefreshToken());
        userSessionRepository.findByRefreshTokenHashAndActiveTrue(hash)
                .ifPresent(session -> {
                    if (session.getDeviceId().equals(request.getDeviceId())) {
                        session.setActive(false);
                        userSessionRepository.save(session);
                        AuditLogger.logAction("USER_LOGGED_OUT", session.getUser().getId().toString(), "Device: " + request.getDeviceId());
                    }
                });
    }

    @Override
    @Transactional
    public void logoutAll(User currentUser) {
        // Increment token version to immediately invalidate any issued access tokens
        currentUser.setTokenVersion(currentUser.getTokenVersion() + 1);
        userRepository.save(currentUser);

        // Deactivate all database sessions
        List<UserSession> sessions = userSessionRepository.findAllByUserIdAndActiveTrue(currentUser.getId());
        sessions.forEach(s -> s.setActive(false));
        userSessionRepository.saveAll(sessions);

        AuditLogger.logAction("USER_LOGGED_OUT_ALL", currentUser.getId().toString(), "Invalidated all sessions");
    }

    @Override
    public CurrentUserResponse getMe(User currentUser) {
        String onboardingStep = "BASIC_PROFILE";
        if (currentUser.getRole() == Role.WORKER) {
            Optional<WorkerProfile> profileOpt = workerProfileRepository.findByUserId(currentUser.getId());
            if (profileOpt.isPresent()) {
                onboardingStep = profileOpt.get().getCurrentStep().name();
            }
        } else {
            onboardingStep = currentUser.getProfileCompleted() ? "COMPLETED" : "BASIC_PROFILE";
        }

        boolean isCompleted = isUserProfileCompleted(currentUser);
        String membershipPlan = getMembershipPlan(currentUser);

        return CurrentUserResponse.builder()
                .userId(currentUser.getId())
                .role(currentUser.getRole().name())
                .mobile(currentUser.getMobile())
                .accountStatus(currentUser.getStatus().name())
                .membershipStatus(getMembershipStatus(currentUser))
                .membershipPlan(membershipPlan)
                .verificationStatus(currentUser.getMobileVerified() ? "VERIFIED" : "UNVERIFIED")
                .profileCompletion(isCompleted ? 100.0 : 0.0)
                .profileCompleted(isCompleted)
                .onboardingStep(onboardingStep)
                .lastLogin(Instant.now())
                .build();
    }

    private boolean isUserProfileCompleted(User user) {
        if (user.getRole() == Role.WORKER) {
            return workerProfileRepository.findByUserId(user.getId())
                    .map(WorkerProfile::getIsCompleted)
                    .orElse(false);
        }
        return Boolean.TRUE.equals(user.getProfileCompleted());
    }

    private String hashToken(String token) {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] hash = digest.digest(token.getBytes(StandardCharsets.UTF_8));
            StringBuilder hexString = new StringBuilder(2 * hash.length);
            for (byte b : hash) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) {
                    hexString.append('0');
                }
                hexString.append(hex);
            }
            return hexString.toString();
        } catch (NoSuchAlgorithmException e) {
            throw new IllegalStateException("SHA-256 not supported", e);
        }
    }

    @Override
    @Transactional
    public UserResponse register(RegisterRequest request) {
        // 1. Validate password confirmation
        if (request.getPassword() == null || !request.getPassword().equals(request.getConfirmPassword())) {
            throw new BusinessException("PASSWORD_MISMATCH", "Password and confirm password do not match");
        }

        // 2. Validate role constraint
        if (request.getRole() == Role.ADMIN) {
            throw new BusinessException("INVALID_ROLE", "Registration as ADMIN is not allowed");
        }

        // 3. Format and check duplicate mobile
        String fullMobile = request.getMobileCountryCode() + request.getMobileNumber();
        if (userRepository.existsByMobile(fullMobile)) {
            throw new ConflictException("DUPLICATE_MOBILE", "An account already exists with this mobile number");
        }

        // 4. Handle optional email check
        String email = request.getEmail();
        if (email != null && !email.trim().isEmpty()) {
            email = email.trim().toLowerCase();
            if (userRepository.existsByEmail(email)) {
                throw new ConflictException("DUPLICATE_EMAIL", "An account already exists with this email address");
            }
        } else {
            email = null;
        }

        // 5. Hash password and build entity
        String encodedPassword = passwordEncoder.encode(request.getPassword());
        User user = User.builder()
                .uuid(UUID.randomUUID().toString())
                .fullName(request.getFullName().trim())
                .mobile(fullMobile)
                .email(email)
                .password(null) // Keep legacy password untouched (NULL)
                .passwordHash(encodedPassword)
                .role(request.getRole())
                .status(com.newtron.newtron_workforce_backend.auth.enums.UserStatus.ACTIVE)
                .profileCompleted(false)
                .mobileVerified(true)
                .tokenVersion(0L)
                .build();

        User savedUser = userRepository.save(user);

        // 6. Audit log registration
        AuditLogger.logAction("USER_REGISTERED", savedUser.getId().toString(), "Role: " + savedUser.getRole().name());

        // 7. Map to safe response DTO
        return UserResponse.builder()
                .uuid(savedUser.getUuid())
                .fullName(savedUser.getFullName())
                .mobile(savedUser.getMobile())
                .email(savedUser.getEmail())
                .role(savedUser.getRole())
                .status(savedUser.getStatus())
                .profileCompleted(savedUser.getProfileCompleted())
                .build();
    }

    @Override
    @Transactional
    public AuthResponse login(LoginRequest request) {
        String identifier = request.getIdentifier().trim();
        Optional<User> userOpt;

        // 1. Identify and lookup user
        if (identifier.contains("@")) {
            userOpt = userRepository.findByEmail(identifier.toLowerCase());
        } else {
            String fullMobile;
            if (identifier.startsWith("+")) {
                fullMobile = identifier;
            } else if (identifier.length() == 10 && identifier.matches("^\\d+$")) {
                fullMobile = "+91" + identifier;
            } else {
                fullMobile = identifier;
            }
            userOpt = userRepository.findByMobile(fullMobile);
        }

        if (userOpt.isEmpty()) {
            throw new UnauthorizedException("INVALID_CREDENTIALS", "Invalid credentials");
        }

        User user = userOpt.get();

        // 2. Verify password_hash exists (prevent OTP-only user password login) and matches
        if (user.getPasswordHash() == null || 
                !passwordEncoder.matches(request.getPassword(), user.getPasswordHash())) {
            throw new UnauthorizedException("INVALID_CREDENTIALS", "Invalid credentials");
        }

        // 3. Establish User Session (reusing existing token/session flow)
        String sessionId = UUID.randomUUID().toString();
        DeviceInfoDto deviceInfo = request.getDeviceInformation();

        JwtPrincipal principal = JwtPrincipal.builder()
                .userId(user.getId())
                .mobile(user.getMobile())
                .authorities(List.of(new SimpleGrantedAuthority("ROLE_" + user.getRole().name())))
                .deviceId(deviceInfo.getDeviceId())
                .sessionId(sessionId)
                .tokenVersion(user.getTokenVersion())
                .userType(user.getRole().name())
                .build();

        String accessToken = jwtTokenProvider.generateAccessToken(principal);
        String refreshToken = jwtTokenProvider.generateRefreshToken(user.getMobile(), deviceInfo.getDeviceId(), sessionId);
        String tokenHash = hashToken(refreshToken);

        // Deactivate previous active sessions for the same device
        userSessionRepository.findByUserIdAndDeviceIdAndActiveTrue(user.getId(), deviceInfo.getDeviceId())
                .ifPresent(prevSession -> {
                    prevSession.setActive(false);
                    userSessionRepository.save(prevSession);
                });

        UserSession session = UserSession.builder()
                .user(user)
                .refreshTokenHash(tokenHash)
                .deviceId(deviceInfo.getDeviceId())
                .deviceName(deviceInfo.getDeviceName())
                .deviceType(deviceInfo.getDeviceType())
                .osVersion(deviceInfo.getOsVersion())
                .appVersion(deviceInfo.getAppVersion())
                .fcmToken(deviceInfo.getFcmToken())
                .active(true)
                .lastActivityAt(Instant.now())
                .build();

        userSessionRepository.save(session);
        AuditLogger.logAction("USER_LOGGED_IN", user.getId().toString(), "Device: " + deviceInfo.getDeviceId());

        // 4. Map to matching AuthResponse DTO format
        boolean isCompleted = isUserProfileCompleted(user);
        String membershipPlan = getMembershipPlan(user);

        return AuthResponse.builder()
                .accessToken(accessToken)
                .refreshToken(refreshToken)
                .tokenType("Bearer")
                .expiresIn(jwtProperties.getAccessExpiration() / 1000)
                .userId(user.getId())
                .role(user.getRole().name())
                .isNewUser(false)
                .profileCompleted(isCompleted)
                .verificationCompleted(user.getMobileVerified())
                .membershipStatus(getMembershipStatus(user))
                .membershipPlan(membershipPlan)
                .requiresOnboarding(!isCompleted)
                .displayName(user.getFullName())
                .profilePhoto(null)
                .build();
    }

    @Override
    @Transactional
    public void resetPassword(ResetPasswordRequest request) {
        String hashedToken = hashToken(request.getResetToken());
        User user = userRepository.findByPasswordResetTokenHash(hashedToken)
                .orElseThrow(() -> new BusinessException("INVALID_TOKEN", "Invalid or expired reset token."));

        if (user.getPasswordResetExpiresAt() == null || user.getPasswordResetExpiresAt().isBefore(java.time.LocalDateTime.now())) {
            throw new BusinessException("EXPIRED_TOKEN", "Reset token has expired.");
        }

        user.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        user.setPasswordResetTokenHash(null);
        user.setPasswordResetExpiresAt(null);

        // Optional: invalidate all active sessions for security
        List<UserSession> sessions = userSessionRepository.findAllByUserIdAndActiveTrue(user.getId());
        sessions.forEach(s -> s.setActive(false));
        userSessionRepository.saveAll(sessions);
        user.setTokenVersion(user.getTokenVersion() + 1);

        userRepository.save(user);
        AuditLogger.logAction("PASSWORD_RESET", user.getId().toString(), "Password reset successfully via OTP flow");
    }

    private void syncProfileCompletedFlag(User user) {
        if (Boolean.TRUE.equals(user.getProfileCompleted())) {
            return; // Already synced
        }
        if (user.getRole() == Role.WORKER) {
            workerProfileRepository.findByUserId(user.getId()).ifPresent(wp -> {
                boolean shouldUpdate = false;
                if (Boolean.TRUE.equals(wp.getIsCompleted())) {
                    shouldUpdate = true;
                } else {
                    boolean hasTeam = teamRepository.existsByOwnerWorkerProfileIdAndDeletedFalse(wp.getId());
                    if (hasTeam) {
                        wp.setIsCompleted(true);
                        workerProfileRepository.save(wp);
                        shouldUpdate = true;
                    }
                }
                
                if (shouldUpdate) {
                    user.setProfileCompleted(true);
                    userRepository.save(user);
                }
            });
        }
    }

    private String getMembershipStatus(User user) {
        if (user.getRole() == Role.WORKER && Boolean.TRUE.equals(user.getMembershipActive())) {
            return workerProfileRepository.findByUserId(user.getId())
                    .flatMap(wp -> workerMembershipRepository.findByWorkerProfileId(wp.getId()))
                    .map(m -> {
                        if (m.getExpiresAt() != null && m.getExpiresAt().isBefore(java.time.LocalDateTime.now())) {
                            return "EXPIRED";
                        }
                        return "ACTIVE";
                    })
                    .orElse("ACTIVE");
        }
        return user.getMembershipActive() != null && user.getMembershipActive() ? "ACTIVE" : "INACTIVE";
    }

    private String getMembershipPlan(User user) {
        if (user.getRole() == Role.WORKER && Boolean.TRUE.equals(user.getMembershipActive())) {
            return workerProfileRepository.findByUserId(user.getId())
                    .flatMap(wp -> workerMembershipRepository.findByWorkerProfileId(wp.getId()))
                    .map(WorkerMembership::getPlan)
                    .orElse(null);
        }
        return null;
    }
}
