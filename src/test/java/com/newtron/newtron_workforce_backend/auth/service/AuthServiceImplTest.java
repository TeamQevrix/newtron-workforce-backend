package com.newtron.newtron_workforce_backend.auth.service;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.enums.Role;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.auth.dto.CurrentUserResponse;
import com.newtron.newtron_workforce_backend.entity.WorkerMembership;
import com.newtron.newtron_workforce_backend.entity.WorkerProfile;
import com.newtron.newtron_workforce_backend.repository.WorkerMembershipRepository;
import com.newtron.newtron_workforce_backend.repository.WorkerProfileRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;

@ExtendWith(MockitoExtension.class)
public class AuthServiceImplTest {

    @Mock
    private WorkerProfileRepository workerProfileRepository;

    @Mock
    private WorkerMembershipRepository workerMembershipRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private AuthServiceImpl authService;

    private User mockUser;
    private WorkerProfile mockProfile;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .role(Role.WORKER)
                .membershipActive(true)
                .mobileVerified(true)
                .status(com.newtron.newtron_workforce_backend.auth.enums.UserStatus.ACTIVE)
                .build();
        
        mockProfile = WorkerProfile.builder()
                .user(mockUser)
                .currentStep(com.newtron.newtron_workforce_backend.enums.OnboardingStep.BASIC_PROFILE)
                .build();
        mockProfile.setId(10L);
    }

    @Test
    void testMembershipStatus_Active() {
        WorkerMembership membership = WorkerMembership.builder()
                .plan("INDIVIDUAL_MONTHLY")
                .expiresAt(LocalDateTime.now().plusMonths(1))
                .build();

        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));
        Mockito.when(workerMembershipRepository.findByWorkerProfileId(10L)).thenReturn(Optional.of(membership));

        CurrentUserResponse response = authService.getMe(mockUser);
        assertEquals("ACTIVE", response.getMembershipStatus());
    }

    @Test
    void testMembershipStatus_Expired() {
        WorkerMembership membership = WorkerMembership.builder()
                .plan("INDIVIDUAL_MONTHLY")
                .expiresAt(LocalDateTime.now().minusDays(1)) // Expired
                .build();

        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));
        Mockito.when(workerMembershipRepository.findByWorkerProfileId(10L)).thenReturn(Optional.of(membership));

        CurrentUserResponse response = authService.getMe(mockUser);
        assertEquals("EXPIRED", response.getMembershipStatus());
    }

    @Test
    void testMembershipStatus_NoMembership_FallsBackToDbValue() {
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));
        Mockito.when(workerMembershipRepository.findByWorkerProfileId(10L)).thenReturn(Optional.empty());

        CurrentUserResponse response = authService.getMe(mockUser);
        assertEquals("ACTIVE", response.getMembershipStatus());
    }

    @Test
    void testMembershipStatus_NullExpiresAt_RemainsActive() {
        WorkerMembership membership = WorkerMembership.builder()
                .plan("INDIVIDUAL_MONTHLY")
                .expiresAt(null)
                .build();

        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));
        Mockito.when(workerMembershipRepository.findByWorkerProfileId(10L)).thenReturn(Optional.of(membership));

        CurrentUserResponse response = authService.getMe(mockUser);
        assertEquals("ACTIVE", response.getMembershipStatus());
    }
}
