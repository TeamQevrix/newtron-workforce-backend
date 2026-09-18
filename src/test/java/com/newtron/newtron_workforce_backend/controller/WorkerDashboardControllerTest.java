package com.newtron.newtron_workforce_backend.controller;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.enums.Role;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.common.exception.ResourceNotFoundException;
import com.newtron.newtron_workforce_backend.entity.Application;
import com.newtron.newtron_workforce_backend.entity.Job;
import com.newtron.newtron_workforce_backend.entity.WorkerMembership;
import com.newtron.newtron_workforce_backend.entity.WorkerProfessional;
import com.newtron.newtron_workforce_backend.entity.WorkerProfile;
import com.newtron.newtron_workforce_backend.enums.OnboardingStep;
import com.newtron.newtron_workforce_backend.repository.ApplicationRepository;
import com.newtron.newtron_workforce_backend.repository.JobRepository;
import com.newtron.newtron_workforce_backend.repository.WorkerMembershipRepository;
import com.newtron.newtron_workforce_backend.repository.WorkerProfessionalRepository;
import com.newtron.newtron_workforce_backend.repository.WorkerProfileRepository;
import com.newtron.newtron_workforce_backend.service.OnboardingProgressService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.test.context.support.WithMockUser;
import org.springframework.test.web.servlet.MockMvc;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@SpringBootTest
@AutoConfigureMockMvc
public class WorkerDashboardControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private WorkerProfileRepository workerProfileRepository;

    @MockBean
    private WorkerProfessionalRepository workerProfessionalRepository;

    @MockBean
    private OnboardingProgressService onboardingProgressService;

    @MockBean
    private JobRepository jobRepository;

    @MockBean
    private ApplicationRepository applicationRepository;

    @MockBean
    private WorkerMembershipRepository workerMembershipRepository;

    private User mockUser;
    private WorkerProfile mockProfile;

    @BeforeEach
    void setUp() {
        mockUser = User.builder()
                .id(1L)
                .mobile("+919525301196")
                .fullName("Test Worker")
                .role(Role.WORKER)
                .profileCompleted(false)
                .mobileVerified(true)
                .build();

        mockProfile = WorkerProfile.builder()
                .user(mockUser)
                .fullName("Test Worker")
                .currentStep(OnboardingStep.MEMBERSHIP)
                .isCompleted(false)
                .photoStorageKey("photos/test.png")
                .build();
        mockProfile.setId(10L);
    }

    @Test
    @WithMockUser(username = "+919525301196", roles = "WORKER")
    void getWorkerSummary_Success() throws Exception {
        Mockito.when(userRepository.findByMobile("+919525301196")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));
        Mockito.when(workerProfessionalRepository.findByWorkerProfileId(10L)).thenReturn(
                Optional.of(WorkerProfessional.builder().available(true).build())
        );
        Mockito.when(onboardingProgressService.calculateCompletion(any(WorkerProfile.class))).thenReturn(75.0);

        mockMvc.perform(get("/api/v1/worker/dashboard/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.userId").value(1L))
                .andExpect(jsonPath("$.data.workerId").value(10L))
                .andExpect(jsonPath("$.data.displayName").value("Test Worker"))
                .andExpect(jsonPath("$.data.profilePhotoUrl").value("photos/test.png"))
                .andExpect(jsonPath("$.data.verificationStatus").value("VERIFIED"))
                .andExpect(jsonPath("$.data.available").value(true))
                .andExpect(jsonPath("$.data.role").value("WORKER"))
                .andExpect(jsonPath("$.data.profileCompleted").value(false))
                .andExpect(jsonPath("$.data.profileCompletionPercentage").value(75));
    }

    @Test
    @WithMockUser(username = "+919525301196", roles = "WORKER")
    void getWorkerSummary_ProfileNotFound() throws Exception {
        Mockito.when(userRepository.findByMobile("+919525301196")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/worker/dashboard/summary")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value("PROFILE_NOT_FOUND"));
    }

    @Test
    @WithMockUser(username = "+919525301196", roles = "WORKER")
    void getWorkerAvailability_True() throws Exception {
        Mockito.when(userRepository.findByMobile("+919525301196")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));
        Mockito.when(workerProfessionalRepository.findByWorkerProfileId(10L)).thenReturn(
                Optional.of(WorkerProfessional.builder().available(true).build())
        );

        mockMvc.perform(get("/api/v1/worker/dashboard/availability")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(true));
    }

    @Test
    @WithMockUser(username = "+919525301196", roles = "WORKER")
    void getWorkerAvailability_False() throws Exception {
        Mockito.when(userRepository.findByMobile("+919525301196")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));
        Mockito.when(workerProfessionalRepository.findByWorkerProfileId(10L)).thenReturn(
                Optional.of(WorkerProfessional.builder().available(false).build())
        );

        mockMvc.perform(get("/api/v1/worker/dashboard/availability")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(false));
    }

    @Test
    @WithMockUser(username = "+919525301196", roles = "WORKER")
    void updateWorkerAvailability_True() throws Exception {
        WorkerProfessional professional = WorkerProfessional.builder().available(false).build();
        Mockito.when(userRepository.findByMobile("+919525301196")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));
        Mockito.when(workerProfessionalRepository.findByWorkerProfileId(10L)).thenReturn(Optional.of(professional));

        mockMvc.perform(patch("/api/v1/worker/dashboard/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"available\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(true));

        Mockito.verify(workerProfessionalRepository).save(professional);
    }

    @Test
    @WithMockUser(username = "+919525301196", roles = "WORKER")
    void updateWorkerAvailability_False() throws Exception {
        WorkerProfessional professional = WorkerProfessional.builder().available(true).build();
        Mockito.when(userRepository.findByMobile("+919525301196")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));
        Mockito.when(workerProfessionalRepository.findByWorkerProfileId(10L)).thenReturn(Optional.of(professional));

        mockMvc.perform(patch("/api/v1/worker/dashboard/availability")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"available\":false}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(false));

        Mockito.verify(workerProfessionalRepository).save(professional);
    }

    @Test
    void getWorkerAvailability_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/worker/dashboard/availability")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "+919525301196", roles = "WORKER")
    void getWorkerAvailability_ProfessionalNotFound() throws Exception {
        Mockito.when(userRepository.findByMobile("+919525301196")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));
        Mockito.when(workerProfessionalRepository.findByWorkerProfileId(10L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/worker/dashboard/availability")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.available").value(false));
    }

    @Test
    @WithMockUser(username = "+919525301196", roles = "WORKER")
    void getQuickActions_ZeroRecords() throws Exception {
        Mockito.when(userRepository.findByMobile("+919525301196")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));
        Mockito.when(jobRepository.countActiveJobs()).thenReturn(0L);
        Mockito.when(applicationRepository.countByWorkerId(1L)).thenReturn(0L);

        mockMvc.perform(get("/api/v1/worker/dashboard/quick-actions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.availableJobsCount").value(0))
                .andExpect(jsonPath("$.data.appliedJobsCount").value(0))
                .andExpect(jsonPath("$.data.savedJobsCount").doesNotExist())
                .andExpect(jsonPath("$.data.unreadAlertsCount").doesNotExist());
    }

    @Test
    @WithMockUser(username = "+919525301196", roles = "WORKER")
    void getQuickActions_MultipleRecords() throws Exception {
        Mockito.when(userRepository.findByMobile("+919525301196")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));
        Mockito.when(jobRepository.countActiveJobs()).thenReturn(12L);
        Mockito.when(applicationRepository.countByWorkerId(1L)).thenReturn(5L);

        mockMvc.perform(get("/api/v1/worker/dashboard/quick-actions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.availableJobsCount").value(12))
                .andExpect(jsonPath("$.data.appliedJobsCount").value(5))
                .andExpect(jsonPath("$.data.savedJobsCount").doesNotExist())
                .andExpect(jsonPath("$.data.unreadAlertsCount").doesNotExist());
    }

    @Test
    @WithMockUser(username = "+919525301196", roles = "WORKER")
    void getQuickActions_WorkerIsolation() throws Exception {
        Mockito.when(userRepository.findByMobile("+919525301196")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));
        Mockito.when(jobRepository.countActiveJobs()).thenReturn(12L);
        Mockito.when(applicationRepository.countByWorkerId(1L)).thenReturn(5L);
        Mockito.when(applicationRepository.countByWorkerId(2L)).thenReturn(99L);

        mockMvc.perform(get("/api/v1/worker/dashboard/quick-actions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.appliedJobsCount").value(5));
    }

    @Test
    void getQuickActions_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/worker/dashboard/quick-actions")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "+919525301196", roles = "WORKER")
    void getWorkerStats_Success() throws Exception {
        Mockito.when(userRepository.findByMobile("+919525301196")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));

        mockMvc.perform(get("/api/v1/worker/dashboard/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.rating").doesNotExist())
                .andExpect(jsonPath("$.data.completedJobsCount").doesNotExist())
                .andExpect(jsonPath("$.data.todayEarnings").doesNotExist())
                .andExpect(jsonPath("$.data.totalEarnings").doesNotExist());
    }

    @Test
    @WithMockUser(username = "+919525301196", roles = "WORKER")
    void getWorkerStats_ProfileNotFound() throws Exception {
        Mockito.when(userRepository.findByMobile("+919525301196")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/worker/dashboard/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value("PROFILE_NOT_FOUND"));
    }

    @Test
    void getWorkerStats_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/worker/dashboard/stats")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "+919525301196", roles = "WORKER")
    void getRecentActivities_ZeroRecords() throws Exception {
        Mockito.when(userRepository.findByMobile("+919525301196")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));
        Mockito.when(workerMembershipRepository.findByWorkerProfileId(10L)).thenReturn(Optional.empty());
        Mockito.when(applicationRepository.findByWorkerId(1L)).thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/worker/dashboard/recent-activities")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @WithMockUser(username = "+919525301196", roles = "WORKER")
    void getRecentActivities_MultipleRecords_SortingAndMapping() throws Exception {
        Mockito.when(userRepository.findByMobile("+919525301196")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));

        WorkerMembership membership = WorkerMembership.builder()
                .plan("Premium Plan")
                .status("ACTIVE")
                .build();
        membership.setCreatedAt(Instant.parse("2026-08-12T01:00:00Z"));
        Mockito.when(workerMembershipRepository.findByWorkerProfileId(10L)).thenReturn(Optional.of(membership));

        Job job = Job.builder().title("Electrician").category("Construction").build();
        Application app = Application.builder().job(job).appliedDate("2026-08-12T02:00:00Z").build();
        Mockito.when(applicationRepository.findByWorkerId(1L)).thenReturn(List.of(app));

        mockMvc.perform(get("/api/v1/worker/dashboard/recent-activities")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].activityType").value("APPLIED"))
                .andExpect(jsonPath("$.data[0].title").value("Application Submitted"))
                .andExpect(jsonPath("$.data[0].timestamp").value("2026-08-12T02:00:00Z"))
                .andExpect(jsonPath("$.data[1].activityType").value("PAYMENT"))
                .andExpect(jsonPath("$.data[1].title").value("Membership Activated"))
                .andExpect(jsonPath("$.data[1].timestamp").value("2026-08-12T01:00:00Z"));
    }

    @Test
    @WithMockUser(username = "+919525301196", roles = "WORKER")
    void getRecentActivities_WorkerIsolation() throws Exception {
        Mockito.when(userRepository.findByMobile("+919525301196")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));
        Mockito.when(workerMembershipRepository.findByWorkerProfileId(10L)).thenReturn(Optional.empty());

        Job job = Job.builder().title("Electrician").category("Construction").build();
        Application appA = Application.builder().job(job).appliedDate("2026-08-12T02:00:00Z").build();
        Mockito.when(applicationRepository.findByWorkerId(1L)).thenReturn(List.of(appA));
        Mockito.when(applicationRepository.findByWorkerId(2L)).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/api/v1/worker/dashboard/recent-activities")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(1));
    }

    @Test
    void getRecentActivities_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/worker/dashboard/recent-activities")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "+919525301196", roles = "WORKER")
    void getRecommendedJobs_Success_ExactAndSubstrMatches() throws Exception {
        Mockito.when(userRepository.findByMobile("+919525301196")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));

        WorkerProfessional professional = WorkerProfessional.builder()
                .primarySkill("Electrician")
                .secondarySkill("Plumber")
                .build();
        Mockito.when(workerProfessionalRepository.findByWorkerProfileId(10L)).thenReturn(Optional.of(professional));

        Job job1 = Job.builder().id(101L).title("Chief Electrician").category("Electrical").city("Bengaluru").salary("1500/day").status("Active").build();
        Job job2 = Job.builder().id(102L).title("Assistant Plumber").category("Plumbing").city("Bengaluru").salary("1200/day").status("Active").build();

        Mockito.when(jobRepository.findActiveJobsBySkills(eq("Electrician"), eq("Plumber"), any()))
                .thenReturn(List.of(job1, job2));

        mockMvc.perform(get("/api/v1/worker/dashboard/recommended-jobs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.length()").value(2))
                .andExpect(jsonPath("$.data[0].jobId").value(101))
                .andExpect(jsonPath("$.data[0].title").value("Chief Electrician"))
                .andExpect(jsonPath("$.data[1].jobId").value(102))
                .andExpect(jsonPath("$.data[1].title").value("Assistant Plumber"));
    }

    @Test
    @WithMockUser(username = "+919525301196", roles = "WORKER")
    void getRecommendedJobs_NoFallback_ReturnsEmpty() throws Exception {
        Mockito.when(userRepository.findByMobile("+919525301196")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));

        WorkerProfessional professional = WorkerProfessional.builder()
                .primarySkill("Electrician")
                .secondarySkill("Plumber")
                .build();
        Mockito.when(workerProfessionalRepository.findByWorkerProfileId(10L)).thenReturn(Optional.of(professional));

        Mockito.when(jobRepository.findActiveJobsBySkills(eq("Electrician"), eq("Plumber"), any()))
                .thenReturn(Collections.emptyList());

        mockMvc.perform(get("/api/v1/worker/dashboard/recommended-jobs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data").isEmpty());
    }

    @Test
    @WithMockUser(username = "+919525301196", roles = "WORKER")
    void getRecommendedJobs_CorrectDtoMapping_OmitsFields() throws Exception {
        Mockito.when(userRepository.findByMobile("+919525301196")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));

        WorkerProfessional professional = WorkerProfessional.builder()
                .primarySkill("Electrician")
                .build();
        Mockito.when(workerProfessionalRepository.findByWorkerProfileId(10L)).thenReturn(Optional.of(professional));

        Job job = Job.builder()
                .id(200L)
                .title("Helper Electrician")
                .category("Electrical")
                .city("Mumbai")
                .salary("1800/day")
                .status("Active")
                .build();

        Mockito.when(jobRepository.findActiveJobsBySkills(eq("Electrician"), eq("Electrician"), any()))
                .thenReturn(List.of(job));

        mockMvc.perform(get("/api/v1/worker/dashboard/recommended-jobs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data[0].jobId").value(200))
                .andExpect(jsonPath("$.data[0].title").value("Helper Electrician"))
                .andExpect(jsonPath("$.data[0].company").value("Newtron Client"))
                .andExpect(jsonPath("$.data[0].location").value("Mumbai"))
                .andExpect(jsonPath("$.data[0].salary").value("1800/day"))
                // Omit unsupported fields
                .andExpect(jsonPath("$.data[0].distanceKm").doesNotExist())
                .andExpect(jsonPath("$.data[0].salaryType").doesNotExist())
                .andExpect(jsonPath("$.data[0].postedAt").doesNotExist());
    }

    @Test
    @WithMockUser(username = "+919525301196", roles = "WORKER")
    void getRecommendedJobs_Max10Recommendations() throws Exception {
        Mockito.when(userRepository.findByMobile("+919525301196")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));

        WorkerProfessional professional = WorkerProfessional.builder()
                .primarySkill("Electrician")
                .build();
        Mockito.when(workerProfessionalRepository.findByWorkerProfileId(10L)).thenReturn(Optional.of(professional));

        List<Job> tenJobs = new ArrayList<>();
        for (int i = 0; i < 10; i++) {
            tenJobs.add(Job.builder().id((long) i).title("Job " + i).category("Electrical").status("Active").build());
        }

        org.mockito.ArgumentCaptor<org.springframework.data.domain.Pageable> pageableCaptor =
                org.mockito.ArgumentCaptor.forClass(org.springframework.data.domain.Pageable.class);

        Mockito.when(jobRepository.findActiveJobsBySkills(eq("Electrician"), eq("Electrician"), pageableCaptor.capture()))
                .thenReturn(tenJobs);

        mockMvc.perform(get("/api/v1/worker/dashboard/recommended-jobs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.length()").value(10));

        org.springframework.data.domain.Pageable capturedPageable = pageableCaptor.getValue();
        org.junit.jupiter.api.Assertions.assertEquals(10, capturedPageable.getPageSize());
        org.junit.jupiter.api.Assertions.assertEquals(0, capturedPageable.getPageNumber());
    }

    @Test
    void getRecommendedJobs_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/worker/dashboard/recommended-jobs")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "+919525301196", roles = "WORKER")
    void getWorkerDashboard_Success() throws Exception {
        Mockito.when(userRepository.findByMobile("+919525301196")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));

        // Sub-mocks
        Mockito.when(workerProfessionalRepository.findByWorkerProfileId(10L)).thenReturn(
                Optional.of(WorkerProfessional.builder().primarySkill("Electrician").available(true).build())
        );
        Mockito.when(onboardingProgressService.calculateCompletion(any())).thenReturn(80.0);
        Mockito.when(jobRepository.countActiveJobs()).thenReturn(15L);
        Mockito.when(applicationRepository.countByWorkerId(1L)).thenReturn(3L);
        Mockito.when(workerMembershipRepository.findByWorkerProfileId(10L)).thenReturn(Optional.empty());
        Mockito.when(applicationRepository.findByWorkerId(1L)).thenReturn(new ArrayList<>());
        Mockito.when(jobRepository.findActiveJobsBySkills(eq("Electrician"), eq("Electrician"), any())).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/api/v1/worker/dashboard")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.summary.displayName").value("Test Worker"))
                .andExpect(jsonPath("$.data.availability.available").value(true))
                .andExpect(jsonPath("$.data.quickActions.availableJobsCount").value(15))
                .andExpect(jsonPath("$.data.stats").isMap())
                .andExpect(jsonPath("$.data.recentActivities").isArray())
                .andExpect(jsonPath("$.data.recommendedJobs").isArray());
    }

    @Test
    @WithMockUser(username = "+919525301196", roles = "WORKER")
    void getWorkerDashboard_ProfileNotFound() throws Exception {
        Mockito.when(userRepository.findByMobile("+919525301196")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/api/v1/worker/dashboard")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.errors[0].code").value("PROFILE_NOT_FOUND"));
    }

    @Test
    void getWorkerDashboard_Unauthenticated() throws Exception {
        mockMvc.perform(get("/api/v1/worker/dashboard")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden());
    }

    @Test
    @WithMockUser(username = "+919525301196", roles = "WORKER")
    void getWorkerDashboard_WorkerIsolation() throws Exception {
        Mockito.when(userRepository.findByMobile("+919525301196")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));

        // Sub-mocks
        Mockito.when(workerProfessionalRepository.findByWorkerProfileId(10L)).thenReturn(
                Optional.of(WorkerProfessional.builder().primarySkill("Electrician").available(true).build())
        );
        Mockito.when(onboardingProgressService.calculateCompletion(any())).thenReturn(80.0);
        Mockito.when(jobRepository.countActiveJobs()).thenReturn(15L);
        
        // Assert isolation counts
        Mockito.when(applicationRepository.countByWorkerId(1L)).thenReturn(3L);
        Mockito.when(applicationRepository.countByWorkerId(2L)).thenReturn(99L);
        
        Mockito.when(workerMembershipRepository.findByWorkerProfileId(10L)).thenReturn(Optional.empty());
        Mockito.when(applicationRepository.findByWorkerId(1L)).thenReturn(new ArrayList<>());
        Mockito.when(jobRepository.findActiveJobsBySkills(eq("Electrician"), eq("Electrician"), any())).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/api/v1/worker/dashboard")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.quickActions.appliedJobsCount").value(3));
    }

    @Test
    @WithMockUser(username = "+919525301196", roles = "WORKER")
    void getWorkerDashboard_EmptySections() throws Exception {
        Mockito.when(userRepository.findByMobile("+919525301196")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));

        Mockito.when(workerProfessionalRepository.findByWorkerProfileId(10L)).thenReturn(
                Optional.of(WorkerProfessional.builder().primarySkill("Electrician").available(true).build())
        );
        Mockito.when(onboardingProgressService.calculateCompletion(any())).thenReturn(80.0);
        Mockito.when(jobRepository.countActiveJobs()).thenReturn(0L);
        Mockito.when(applicationRepository.countByWorkerId(1L)).thenReturn(0L);
        Mockito.when(workerMembershipRepository.findByWorkerProfileId(10L)).thenReturn(Optional.empty());
        Mockito.when(applicationRepository.findByWorkerId(1L)).thenReturn(new ArrayList<>());
        Mockito.when(jobRepository.findActiveJobsBySkills(eq("Electrician"), eq("Electrician"), any())).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/api/v1/worker/dashboard")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.recentActivities").isEmpty())
                .andExpect(jsonPath("$.data.recommendedJobs").isEmpty());
    }

    @Test
    @WithMockUser(username = "+919525301196", roles = "WORKER")
    void getWorkerDashboard_ProfessionalNotFound() throws Exception {
        Mockito.when(userRepository.findByMobile("+919525301196")).thenReturn(Optional.of(mockUser));
        Mockito.when(workerProfileRepository.findByUserId(1L)).thenReturn(Optional.of(mockProfile));

        // Sub-mocks returning empty for professional details
        Mockito.when(workerProfessionalRepository.findByWorkerProfileId(10L)).thenReturn(Optional.empty());
        Mockito.when(onboardingProgressService.calculateCompletion(any())).thenReturn(80.0);
        Mockito.when(jobRepository.countActiveJobs()).thenReturn(15L);
        Mockito.when(applicationRepository.countByWorkerId(1L)).thenReturn(3L);
        Mockito.when(workerMembershipRepository.findByWorkerProfileId(10L)).thenReturn(Optional.empty());
        Mockito.when(applicationRepository.findByWorkerId(1L)).thenReturn(new ArrayList<>());

        mockMvc.perform(get("/api/v1/worker/dashboard")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.summary.displayName").value("Test Worker"))
                .andExpect(jsonPath("$.data.availability.available").value(false))
                .andExpect(jsonPath("$.data.quickActions.availableJobsCount").value(15))
                .andExpect(jsonPath("$.data.stats").isMap())
                .andExpect(jsonPath("$.data.recentActivities").isArray())
                .andExpect(jsonPath("$.data.recommendedJobs").isArray())
                .andExpect(jsonPath("$.data.recommendedJobs").isEmpty());
    }
}
