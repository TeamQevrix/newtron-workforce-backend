package com.newtron.newtron_workforce_backend.controller;

import com.newtron.newtron_workforce_backend.auth.dto.*;
import com.newtron.newtron_workforce_backend.auth.service.AuthService;
import com.newtron.newtron_workforce_backend.common.exception.ResourceNotFoundException;
import com.newtron.newtron_workforce_backend.common.logging.RequestContext;
import com.newtron.newtron_workforce_backend.common.response.ApiResponse;
import com.newtron.newtron_workforce_backend.common.response.ApiResponseFactory;
import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {

    private final AuthService authService;
    private final UserRepository userRepository;

    @PostMapping("/send-otp")
    public ApiResponse<Void> sendOtp(
            @Valid @RequestBody SendOtpRequest request,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        String ipAddress = getClientIp(httpServletRequest);
        
        authService.sendOtp(request, ipAddress);
        
        return ApiResponseFactory.success(null, "OTP sent successfully", 
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @PostMapping("/register")
    public ApiResponse<UserResponse> register(
            @Valid @RequestBody RegisterRequest request,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        
        UserResponse response = authService.register(request);
        
        return ApiResponseFactory.success(response, "User registered successfully", 
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @PostMapping("/login")
    public ApiResponse<AuthResponse> login(
            @Valid @RequestBody LoginRequest request,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        
        AuthResponse response = authService.login(request);
        
        return ApiResponseFactory.success(response, "User logged in successfully", 
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @PostMapping("/verify-otp")
    public ApiResponse<AuthResponse> verifyOtp(
            @Valid @RequestBody VerifyOtpRequest request,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        
        AuthResponse response = authService.verifyOtp(request);
        
        return ApiResponseFactory.success(response, "OTP verified successfully", 
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @PostMapping("/reset-password")
    public ApiResponse<Void> resetPassword(
            @Valid @RequestBody ResetPasswordRequest request,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        
        authService.resetPassword(request);
        
        return ApiResponseFactory.success(null, "Password reset successfully", 
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @PostMapping("/refresh-token")
    public ApiResponse<AuthResponse> refreshToken(
            @Valid @RequestBody RefreshTokenRequest request,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        
        AuthResponse response = authService.refreshToken(request);
        
        return ApiResponseFactory.success(response, "Token refreshed successfully", 
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @PostMapping("/logout")
    public ApiResponse<Void> logout(
            @Valid @RequestBody LogoutRequest request,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        
        authService.logout(request);
        
        return ApiResponseFactory.success(null, "Logged out successfully", 
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @PostMapping("/logout-all")
    public ApiResponse<Void> logoutAll(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);
        
        authService.logoutAll(currentUser);
        
        return ApiResponseFactory.success(null, "Logged out from all devices", 
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @GetMapping("/me")
    public ApiResponse<CurrentUserResponse> me(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);
        
        CurrentUserResponse response = authService.getMe(currentUser);
        
        return ApiResponseFactory.success(response, "Current user retrieved successfully", 
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    private User fetchCurrentUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new ResourceNotFoundException("NOT_AUTHENTICATED", "Not authenticated");
        }
        return userRepository.findByMobile(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));
    }

    private String getClientIp(HttpServletRequest request) {
        String xff = request.getHeader("X-Forwarded-For");
        if (xff != null && !xff.isBlank()) {
            return xff.split(",")[0].trim();
        }
        return request.getRemoteAddr();
    }

    private long getStartTime(HttpServletRequest request) {
        Object attr = request.getAttribute("startTimeMs");
        if (attr instanceof Long) {
            return (Long) attr;
        }
        return System.currentTimeMillis();
    }
}