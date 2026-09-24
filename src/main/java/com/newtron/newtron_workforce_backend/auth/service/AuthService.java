package com.newtron.newtron_workforce_backend.auth.service;

import com.newtron.newtron_workforce_backend.auth.dto.*;
import com.newtron.newtron_workforce_backend.auth.entity.User;

public interface AuthService {
    void sendOtp(SendOtpRequest request, String ipAddress);
    AuthResponse verifyOtp(VerifyOtpRequest request);
    AuthResponse refreshToken(RefreshTokenRequest request);
    void logout(LogoutRequest request);
    void logoutAll(User currentUser);
    CurrentUserResponse getMe(User currentUser);
    UserResponse register(RegisterRequest request);
    AuthResponse login(LoginRequest request);
    void resetPassword(ResetPasswordRequest request);
}
