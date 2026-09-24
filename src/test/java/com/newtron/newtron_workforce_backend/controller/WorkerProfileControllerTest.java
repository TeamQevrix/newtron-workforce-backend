package com.newtron.newtron_workforce_backend.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.common.exception.UnauthorizedException;
import com.newtron.newtron_workforce_backend.common.exception.ValidationException;
import com.newtron.newtron_workforce_backend.dto.request.ChangePasswordRequest;
import com.newtron.newtron_workforce_backend.security.jwt.JwtTokenProvider;
import com.newtron.newtron_workforce_backend.service.CustomUserDetailsService;
import com.newtron.newtron_workforce_backend.service.WorkerProfileService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(WorkerProfileController.class)
@AutoConfigureMockMvc(addFilters = false)
class WorkerProfileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private WorkerProfileService workerProfileService;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private JwtTokenProvider jwtTokenProvider;

    @MockBean
    private CustomUserDetailsService customUserDetailsService;

    @Autowired
    private ObjectMapper objectMapper;

    private User mockUser;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .mobile("1234567890")
                .passwordHash("encodedOldPassword")
                .membershipActive(true)
                .build();
        Mockito.when(userRepository.findByMobile(any(String.class))).thenReturn(Optional.of(mockUser));
    }

    @Test
    @WithMockUser(username = "1234567890", roles = "WORKER")
    void changePassword_Success() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("oldPass", "newPass");

        mockMvc.perform(post("/api/v1/worker/profile/change-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @WithMockUser(username = "1234567890", roles = "WORKER")
    void changePassword_IncorrectCurrentPassword() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("wrongPass", "newPass");

        doThrow(new UnauthorizedException("INVALID_CREDENTIALS", "Incorrect current password"))
                .when(workerProfileService).changePassword(any(ChangePasswordRequest.class), any(User.class));

        mockMvc.perform(post("/api/v1/worker/profile/change-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value("INVALID_CREDENTIALS"));
    }

    @Test
    @WithMockUser(username = "1234567890", roles = "WORKER")
    void changePassword_SameAsCurrentPassword() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("oldPass", "oldPass");

        doThrow(new ValidationException("SAME_PASSWORD", "New password cannot be the same as current password"))
                .when(workerProfileService).changePassword(any(ChangePasswordRequest.class), any(User.class));

        mockMvc.perform(post("/api/v1/worker/profile/change-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value("SAME_PASSWORD"));
    }

    @Test
    void changePassword_Unauthenticated() throws Exception {
        ChangePasswordRequest request = new ChangePasswordRequest("oldPass", "newPass");

        // mockMvc with addFilters = false bypasses spring security but Principal will be null
        mockMvc.perform(post("/api/v1/worker/profile/change-password")
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value("NOT_AUTHENTICATED"));
    }
}
