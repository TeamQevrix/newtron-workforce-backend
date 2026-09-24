package com.newtron.newtron_workforce_backend.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.entity.WorkerMembership;
import com.newtron.newtron_workforce_backend.entity.WorkerProfile;
import com.newtron.newtron_workforce_backend.repository.WorkerMembershipRepository;
import com.newtron.newtron_workforce_backend.repository.WorkerProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.context.SecurityContext;
import org.springframework.security.core.context.SecurityContextHolder;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class WorkerMembershipInterceptorTest {

    @Mock
    private UserRepository userRepository;

    @Mock
    private WorkerProfileRepository workerProfileRepository;

    @Mock
    private WorkerMembershipRepository workerMembershipRepository;

    @Mock
    private ObjectMapper objectMapper;

    @InjectMocks
    private WorkerMembershipInterceptor interceptor;

    private MockHttpServletRequest request;
    private MockHttpServletResponse response;
    private User workerUser;
    private WorkerProfile workerProfile;
    private WorkerMembership workerMembership;

    @BeforeEach
    void setUp() {
        request = new MockHttpServletRequest();
        response = new MockHttpServletResponse();

        workerUser = User.builder().mobile("1234567890").build();
        workerUser.setId(1L);
        workerProfile = WorkerProfile.builder().user(workerUser).build();
        workerProfile.setId(1L);
        workerMembership = WorkerMembership.builder().workerProfile(workerProfile).build();
        workerMembership.setId(1L);
    }

    private void setupAuthentication(String role) {
        org.springframework.security.core.userdetails.User principal =
                new org.springframework.security.core.userdetails.User("1234567890", "password", List.of(new SimpleGrantedAuthority(role)));
        Authentication auth = new UsernamePasswordAuthenticationToken(principal, "password", principal.getAuthorities());
        SecurityContext securityContext = SecurityContextHolder.createEmptyContext();
        securityContext.setAuthentication(auth);
        SecurityContextHolder.setContext(securityContext);
    }

    @Test
    void preHandle_ActiveMembership_Allowed() throws Exception {
        setupAuthentication("ROLE_WORKER");
        workerMembership.setExpiresAt(LocalDateTime.now().plusDays(1)); // Future expiry

        when(userRepository.findByMobile("1234567890")).thenReturn(Optional.of(workerUser));
        when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(workerProfile));
        when(workerMembershipRepository.findByWorkerProfileId(1L)).thenReturn(Optional.of(workerMembership));

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
    }

    @Test
    void preHandle_ExpiredMembership_Forbidden() throws Exception {
        setupAuthentication("ROLE_WORKER");
        workerMembership.setExpiresAt(LocalDateTime.now().minusDays(1)); // Past expiry

        when(userRepository.findByMobile("1234567890")).thenReturn(Optional.of(workerUser));
        when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(workerProfile));
        when(workerMembershipRepository.findByWorkerProfileId(1L)).thenReturn(Optional.of(workerMembership));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"error\":\"Expired\"}");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertEquals(403, response.getStatus());
    }

    @Test
    void preHandle_ExactExpiry_Forbidden() throws Exception {
        setupAuthentication("ROLE_WORKER");
        workerMembership.setExpiresAt(LocalDateTime.now()); // Exactly now

        when(userRepository.findByMobile("1234567890")).thenReturn(Optional.of(workerUser));
        when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(workerProfile));
        when(workerMembershipRepository.findByWorkerProfileId(1L)).thenReturn(Optional.of(workerMembership));
        when(objectMapper.writeValueAsString(any())).thenReturn("{\"error\":\"Expired\"}");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertFalse(result);
        assertEquals(403, response.getStatus());
    }

    @Test
    void preHandle_NullExpiry_Allowed() throws Exception {
        setupAuthentication("ROLE_WORKER");
        workerMembership.setExpiresAt(null); // Null expiry

        when(userRepository.findByMobile("1234567890")).thenReturn(Optional.of(workerUser));
        when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(workerProfile));
        when(workerMembershipRepository.findByWorkerProfileId(1L)).thenReturn(Optional.of(workerMembership));

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
    }

    @Test
    void preHandle_Unauthenticated_Allowed() throws Exception {
        SecurityContextHolder.clearContext();

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result); // Existing Spring Security behavior remains responsible
    }

    @Test
    void preHandle_NonWorkerRole_Allowed() throws Exception {
        setupAuthentication("ROLE_RECRUITER");

        boolean result = interceptor.preHandle(request, response, new Object());

        assertTrue(result);
    }
}
