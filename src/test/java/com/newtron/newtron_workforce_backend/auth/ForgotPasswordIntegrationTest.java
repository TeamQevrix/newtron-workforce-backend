package com.newtron.newtron_workforce_backend.auth;

import com.newtron.newtron_workforce_backend.auth.dto.ResetPasswordRequest;
import com.newtron.newtron_workforce_backend.auth.dto.SendOtpRequest;
import com.newtron.newtron_workforce_backend.auth.dto.VerifyOtpRequest;
import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.enums.Role;
import com.newtron.newtron_workforce_backend.auth.otp.entity.OtpPurpose;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;

@SpringBootTest
@AutoConfigureMockMvc
@ActiveProfiles("test")
@Transactional
public class ForgotPasswordIntegrationTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private PasswordEncoder passwordEncoder;

    @Autowired
    private ObjectMapper objectMapper;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.newtron.newtron_workforce_backend.auth.otp.repository.OtpRepository otpRepository;

    @org.springframework.boot.test.mock.mockito.MockBean
    private com.newtron.newtron_workforce_backend.auth.otp.service.OtpService otpService;

    private User testUser;
    
    @BeforeEach
    void setUp() {
        String testMobile = "+919999999999";
        testUser = userRepository.findByMobile(testMobile).orElseGet(() -> {
            User u = User.builder()
                    .mobile(testMobile)
                    .fullName("Test User")
                    .role(Role.WORKER)
                    .build();
            return userRepository.save(u);
        });
        testUser.setPasswordHash(passwordEncoder.encode("OldPassword123!"));
        testUser.setPasswordResetTokenHash(null);
        testUser.setPasswordResetExpiresAt(null);
        userRepository.save(testUser);
    }

    @Test
    void testUnknownMobileDoesNotRevealUserExistence() throws Exception {
        SendOtpRequest req = new SendOtpRequest();
        req.setMobileCountryCode("+91");
        req.setMobileNumber("9999999998"); // Not registered, must be 10 digits for ValidMobile
        req.setPurpose(OtpPurpose.FORGOT_PASSWORD);
        req.setRole(Role.WORKER);

        mockMvc.perform(post("/api/v1/auth/send-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(req)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("OTP sent successfully"));
    }

    @Test
    void testVerifyOtpAndResetPasswordSuccess() throws Exception {
        // Mock OTP repository for verify-otp step
        com.newtron.newtron_workforce_backend.auth.otp.entity.Otp mockOtp = new com.newtron.newtron_workforce_backend.auth.otp.entity.Otp();
        mockOtp.setRole(Role.WORKER);
        // setOtp does not exist, use mock for verification instead
        
        org.mockito.Mockito.when(otpRepository.findTopByMobileCountryCodeAndMobileNumberAndPurposeOrderByCreatedAtDesc(
                "+91", "9999999999", OtpPurpose.FORGOT_PASSWORD))
                .thenReturn(java.util.Optional.of(mockOtp));
                
        org.mockito.Mockito.when(otpService.verifyOtp(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(com.newtron.newtron_workforce_backend.auth.otp.entity.OtpVerificationResult.SUCCESS);
        
        org.mockito.Mockito.when(otpRepository.countByMobileCountryCodeAndMobileNumberAndCreatedAtAfter(
                org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.anyString(), org.mockito.ArgumentMatchers.any()))
                .thenReturn(1);

        VerifyOtpRequest verifyReq = new VerifyOtpRequest();
        verifyReq.setMobileCountryCode("+91");
        verifyReq.setMobileNumber("9999999999");
        verifyReq.setPurpose(OtpPurpose.FORGOT_PASSWORD);
        verifyReq.setOtp("123456");
        
        com.newtron.newtron_workforce_backend.auth.dto.DeviceInfoDto deviceInfo = new com.newtron.newtron_workforce_backend.auth.dto.DeviceInfoDto();
        deviceInfo.setDeviceId("test-device-id");
        verifyReq.setDeviceInformation(deviceInfo);

        MvcResult result = mockMvc.perform(post("/api/v1/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.verificationCompleted").value(true))
                .andExpect(jsonPath("$.data.resetToken").isNotEmpty())
                .andReturn();

        String responseContent = result.getResponse().getContentAsString();
        String resetToken = objectMapper.readTree(responseContent).get("data").get("resetToken").asText();
        assertNotNull(resetToken);

        // Verify user has hashed token
        User updatedUser = userRepository.findById(testUser.getId()).get();
        assertNotNull(updatedUser.getPasswordResetTokenHash());
        assertNotNull(updatedUser.getPasswordResetExpiresAt());

        // Step 3: Reset password
        ResetPasswordRequest resetReq = new ResetPasswordRequest();
        resetReq.setResetToken(resetToken);
        resetReq.setNewPassword("NewPassword123!");

        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.message").value("Password reset successfully"));

        // Verify token cleared and password changed
        User resetUser = userRepository.findById(testUser.getId()).get();
        assertNull(resetUser.getPasswordResetTokenHash());
        assertNull(resetUser.getPasswordResetExpiresAt());
        assertTrue(passwordEncoder.matches("NewPassword123!", resetUser.getPasswordHash()));

        // Step 4: Same token cannot be reused
        mockMvc.perform(post("/api/v1/auth/reset-password")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(resetReq)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Invalid or expired reset token."));
    }

    @Test
    void testVerifyOtpNormalPurposeDoesNotReturnToken() throws Exception {
        com.newtron.newtron_workforce_backend.auth.otp.entity.Otp mockOtp = new com.newtron.newtron_workforce_backend.auth.otp.entity.Otp();
        mockOtp.setRole(Role.WORKER);
        
        org.mockito.Mockito.when(otpRepository.findTopByMobileCountryCodeAndMobileNumberAndPurposeOrderByCreatedAtDesc(
                "+91", "9999999999", OtpPurpose.LOGIN))
                .thenReturn(java.util.Optional.of(mockOtp));
                
        org.mockito.Mockito.when(otpService.verifyOtp(
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.anyString(),
                org.mockito.ArgumentMatchers.any(),
                org.mockito.ArgumentMatchers.anyString()))
                .thenReturn(com.newtron.newtron_workforce_backend.auth.otp.entity.OtpVerificationResult.SUCCESS);

        VerifyOtpRequest verifyReq = new VerifyOtpRequest();
        verifyReq.setMobileCountryCode("+91");
        verifyReq.setMobileNumber("9999999999");
        verifyReq.setPurpose(OtpPurpose.LOGIN);
        verifyReq.setOtp("123456");
        
        com.newtron.newtron_workforce_backend.auth.dto.DeviceInfoDto deviceInfo2 = new com.newtron.newtron_workforce_backend.auth.dto.DeviceInfoDto();
        deviceInfo2.setDeviceId("test-device-id");
        verifyReq.setDeviceInformation(deviceInfo2);

        mockMvc.perform(post("/api/v1/auth/verify-otp")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(verifyReq)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.resetToken").doesNotExist());
    }
}
