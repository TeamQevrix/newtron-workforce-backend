package com.newtron.newtron_workforce_backend.service;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.common.exception.ResourceNotFoundException;
import com.newtron.newtron_workforce_backend.dto.WorkerSummaryDto;
import com.newtron.newtron_workforce_backend.entity.WorkerProfessional;
import com.newtron.newtron_workforce_backend.entity.WorkerProfile;
import com.newtron.newtron_workforce_backend.repository.WorkerProfessionalRepository;
import com.newtron.newtron_workforce_backend.repository.WorkerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

import com.newtron.newtron_workforce_backend.dto.WorkerAvailabilityDto;
import com.newtron.newtron_workforce_backend.dto.WorkerDashboardResponse;

@Service
@RequiredArgsConstructor
public class WorkerDashboardServiceImpl implements WorkerDashboardService {

    private final WorkerProfileRepository workerProfileRepository;
    private final WorkerProfessionalRepository workerProfessionalRepository;
    private final OnboardingProgressService onboardingProgressService;
    private final com.newtron.newtron_workforce_backend.repository.JobRepository jobRepository;
    private final com.newtron.newtron_workforce_backend.repository.ApplicationRepository applicationRepository;
    private final com.newtron.newtron_workforce_backend.repository.WorkerMembershipRepository workerMembershipRepository;
    private final com.newtron.newtron_workforce_backend.repository.WorkerEarningRepository workerEarningRepository;
    private final com.newtron.newtron_workforce_backend.repository.WorkerReviewRepository workerReviewRepository;
    private final com.newtron.newtron_workforce_backend.repository.SavedJobRepository savedJobRepository;

    @Transactional(readOnly = true)
    private WorkerSummaryDto buildWorkerSummary(User currentUser, WorkerProfile profile, WorkerProfessional professional) {
        Boolean available = professional != null ? professional.getAvailable() : true;
        String verificationStatus = currentUser.getMobileVerified() ? "VERIFIED" : "UNVERIFIED";
        int completionPercentage = (int) onboardingProgressService.calculateCompletion(profile);

        return WorkerSummaryDto.builder()
                .userId(currentUser.getId())
                .workerId(profile.getId())
                .displayName(profile.getFullName())
                .profilePhotoUrl(profile.getPhotoStorageKey())
                .verificationStatus(verificationStatus)
                .available(available)
                .role(currentUser.getRole().name())
                .currentOnboardingStep(profile.getCurrentStep().name())
                .profileCompleted(profile.getIsCompleted())
                .profileCompletionPercentage(completionPercentage)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public WorkerSummaryDto getWorkerSummary(User currentUser) {
        WorkerProfile profile = workerProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("PROFILE_NOT_FOUND", "Worker profile not found for this user"));
        WorkerProfessional professional = workerProfessionalRepository.findByWorkerProfileId(profile.getId()).orElse(null);
        return buildWorkerSummary(currentUser, profile, professional);
    }

    private WorkerAvailabilityDto buildWorkerAvailability(WorkerProfessional professional) {
        return WorkerAvailabilityDto.builder()
                .available(professional != null ? professional.getAvailable() : true)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public WorkerAvailabilityDto getWorkerAvailability(User currentUser) {
        WorkerProfile profile = workerProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("PROFILE_NOT_FOUND", "Worker profile not found for this user"));
        WorkerProfessional professional = workerProfessionalRepository.findByWorkerProfileId(profile.getId()).orElse(null);
        return buildWorkerAvailability(professional);
    }

    @Override
    @Transactional
    public WorkerAvailabilityDto updateWorkerAvailability(User currentUser, WorkerAvailabilityDto request) {
        WorkerProfile profile = workerProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("PROFILE_NOT_FOUND", "Worker profile not found for this user"));

        WorkerProfessional professional = workerProfessionalRepository.findByWorkerProfileId(profile.getId())
                .orElseGet(() -> {
                    return WorkerProfessional.builder()
                            .workerProfile(profile)
                            .primarySkill(profile.getFullName() != null ? "General" : "Labour")
                            .experienceYears(0)
                            .build();
                });

        professional.setAvailable(request.getAvailable());
        workerProfessionalRepository.save(professional);

        return WorkerAvailabilityDto.builder()
                .available(professional.getAvailable())
                .build();
    }

    private final com.newtron.newtron_workforce_backend.repository.NotificationRepository notificationRepository;

    private com.newtron.newtron_workforce_backend.dto.WorkerQuickActionsDto buildQuickActions(User currentUser) {
        long availableJobs = jobRepository.countActiveJobs();
        long appliedJobs = applicationRepository.countByWorkerId(currentUser.getId());
        long savedJobs = savedJobRepository.countByWorkerId(currentUser.getId());
        long unreadAlerts = notificationRepository.countByUserIdAndIsReadFalseAndDeletedFalse(currentUser.getId());

        return com.newtron.newtron_workforce_backend.dto.WorkerQuickActionsDto.builder()
                .availableJobsCount((int) availableJobs)
                .appliedJobsCount((int) appliedJobs)
                .savedJobsCount((int) savedJobs)
                .unreadAlertsCount((int) unreadAlerts)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public com.newtron.newtron_workforce_backend.dto.WorkerQuickActionsDto getQuickActions(User currentUser) {
        workerProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("PROFILE_NOT_FOUND", "Worker profile not found for this user"));
        return buildQuickActions(currentUser);
    }

    private com.newtron.newtron_workforce_backend.dto.WorkerStatsDto buildWorkerStats(User currentUser) {
        long completedJobs = applicationRepository.countByWorkerIdAndStatus(currentUser.getId(), "Completed");

        java.time.ZoneId zoneId = java.time.ZoneId.of("Asia/Kolkata");
        java.time.ZonedDateTime now = java.time.ZonedDateTime.now(zoneId);
        java.time.Instant start = now.toLocalDate().atStartOfDay(zoneId).toInstant();
        java.time.Instant end = now.toLocalDate().plusDays(1).atStartOfDay(zoneId).toInstant();

        java.math.BigDecimal today = workerEarningRepository.sumEarningsByWorkerIdAndDateRange(currentUser.getId(), start, end);
        java.math.BigDecimal total = workerEarningRepository.sumTotalEarningsByWorkerId(currentUser.getId());

        Double averageRating = workerReviewRepository.findAverageRatingByWorkerId(currentUser.getId());
        if (averageRating != null) {
            averageRating = java.math.BigDecimal.valueOf(averageRating)
                    .setScale(1, java.math.RoundingMode.HALF_UP)
                    .doubleValue();
        }

        return com.newtron.newtron_workforce_backend.dto.WorkerStatsDto.builder()
                .rating(averageRating)
                .completedJobsCount((int) completedJobs)
                .todayEarnings(today.doubleValue())
                .totalEarnings(total.doubleValue())
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public com.newtron.newtron_workforce_backend.dto.WorkerStatsDto getWorkerStats(User currentUser) {
        workerProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("PROFILE_NOT_FOUND", "Worker profile not found for this user"));
        return buildWorkerStats(currentUser);
    }

    private java.util.List<com.newtron.newtron_workforce_backend.dto.WorkerActivityDto> buildRecentActivities(
            User currentUser, 
            WorkerProfile profile, 
            java.util.List<com.newtron.newtron_workforce_backend.entity.Application> applications) {
        java.util.List<com.newtron.newtron_workforce_backend.dto.WorkerActivityDto> activities = new java.util.ArrayList<>();

        // 1. Fetch worker memberships (Membership Activated event)
        Optional<com.newtron.newtron_workforce_backend.entity.WorkerMembership> membershipOpt = workerMembershipRepository.findByWorkerProfileId(profile.getId());
        membershipOpt.ifPresent(m -> {
            if ("ACTIVE".equalsIgnoreCase(m.getStatus()) || "PAID".equalsIgnoreCase(m.getStatus())) {
                activities.add(com.newtron.newtron_workforce_backend.dto.WorkerActivityDto.builder()
                        .id("membership_" + m.getId())
                        .title("Membership Activated")
                        .subtitle("Successfully activated plan: " + m.getPlan())
                        .timestamp(m.getCreatedAt() != null ? m.getCreatedAt().toString() : null)
                        .activityType("PAYMENT")
                        .build());
            }
        });

        // 2. Fetch worker applications (Application Submitted event)
        for (com.newtron.newtron_workforce_backend.entity.Application app : applications) {
            String jobTitle = app.getJob() != null ? app.getJob().getTitle() : "Unknown Job";
            String category = app.getJob() != null ? app.getJob().getCategory() : "General";
            activities.add(com.newtron.newtron_workforce_backend.dto.WorkerActivityDto.builder()
                    .id("app_" + app.getId())
                    .title("Application Submitted")
                    .subtitle("Applied to " + jobTitle + " under " + category)
                    .timestamp(app.getAppliedDate())
                    .activityType("APPLIED")
                    .build());
        }

        // 3. Fetch Profile Created event
        if (profile.getCreatedAt() != null) {
            activities.add(com.newtron.newtron_workforce_backend.dto.WorkerActivityDto.builder()
                    .id("profile_created_" + profile.getId())
                    .title("Profile Created")
                    .subtitle("Your worker profile was created")
                    .timestamp(profile.getCreatedAt().toString())
                    .activityType("PROFILE")
                    .build());
        }

        // Sort newest first
        activities.sort((a, b) -> {
            if (a.getTimestamp() == null && b.getTimestamp() == null) return 0;
            if (a.getTimestamp() == null) return 1;
            if (b.getTimestamp() == null) return -1;
            
            try {
                java.time.Instant tA = java.time.Instant.parse(a.getTimestamp());
                java.time.Instant tB = java.time.Instant.parse(b.getTimestamp());
                return tB.compareTo(tA);
            } catch (Exception e) {
                return b.getTimestamp().compareTo(a.getTimestamp());
            }
        });

        // Limit to 5 latest activities
        if (activities.size() > 5) {
            return activities.subList(0, 5);
        }
        return activities;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<com.newtron.newtron_workforce_backend.dto.WorkerActivityDto> getRecentActivities(User currentUser) {
        WorkerProfile profile = workerProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("PROFILE_NOT_FOUND", "Worker profile not found for this user"));
        java.util.List<com.newtron.newtron_workforce_backend.entity.Application> applications = applicationRepository.findByWorkerId(currentUser.getId());
        return buildRecentActivities(currentUser, profile, applications);
    }

    private java.util.List<com.newtron.newtron_workforce_backend.dto.JobRecommendationDto> buildRecommendedJobs(
            User currentUser, 
            WorkerProfessional professional, 
            java.util.List<com.newtron.newtron_workforce_backend.entity.Application> userApplications) {
        if (professional == null) {
            return java.util.Collections.emptyList();
        }

        String skill1 = professional.getPrimarySkill();
        String skill2 = professional.getSecondarySkill() != null ? professional.getSecondarySkill() : skill1;

        if (skill1 == null || skill1.trim().isEmpty()) {
            return java.util.Collections.emptyList();
        }

        // Fetch already applied jobs
        java.util.Set<Long> appliedJobIds = userApplications.stream()
                .filter(app -> app.getJob() != null)
                .map(app -> app.getJob().getId())
                .collect(java.util.stream.Collectors.toSet());

        // Fetch larger pool of matching active jobs to filter
        java.util.List<com.newtron.newtron_workforce_backend.entity.Job> matchedJobs = jobRepository.findActiveJobsBySkills(
                skill1, skill2, org.springframework.data.domain.PageRequest.of(0, 50)
        );

        java.util.List<com.newtron.newtron_workforce_backend.dto.JobRecommendationDto> recommendations = new java.util.ArrayList<>();
        for (com.newtron.newtron_workforce_backend.entity.Job job : matchedJobs) {
            // Exclude already applied
            if (appliedJobIds.contains(job.getId())) {
                continue;
            }

            // Exclude inactive/paused/closed
            if (job.getStatus() == null || !"Active".equalsIgnoreCase(job.getStatus().trim())) {
                continue;
            }

            // Exclude fully hired/filled positions
            int required = job.getWorkersRequired() != null ? job.getWorkersRequired() : 1;
            long hired = applicationRepository.countByJobIdAndStatus(job.getId(), "Hired");
            if (hired >= required) {
                continue;
            }

            // Safe salary double parsing
            double wageVal = 0.0;
            if (job.getSalary() != null) {
                try {
                    String cleanSalary = job.getSalary().replaceAll("[^0-9.]", "");
                    if (!cleanSalary.isEmpty()) {
                        wageVal = Double.parseDouble(cleanSalary);
                    }
                } catch (Exception e) {
                    wageVal = 0.0;
                }
            }

            String companyName = (job.getRecruiter() != null && job.getRecruiter().getFullName() != null) 
                    ? job.getRecruiter().getFullName() : "Newtron Client";

            recommendations.add(com.newtron.newtron_workforce_backend.dto.JobRecommendationDto.builder()
                    .id(job.getId().toString())
                    .company(companyName)
                    .jobTitle(job.getTitle())
                    .location(job.getCity())
                    .dailyWage(wageVal)
                    .shiftHours(job.getDuration() != null ? job.getDuration() : "8 Hours")
                    .rating(4.8)
                    .distance(job.getDistance() != null ? job.getDistance() : "2.5 km")
                    .isApplied(false)
                    .build());

            // Limit to maximum 5 recommended jobs
            if (recommendations.size() >= 5) {
                break;
            }
        }

        return recommendations;
    }

    @Override
    @Transactional(readOnly = true)
    public java.util.List<com.newtron.newtron_workforce_backend.dto.JobRecommendationDto> getRecommendedJobs(User currentUser) {
        WorkerProfile profile = workerProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("PROFILE_NOT_FOUND", "Worker profile not found for this user"));
        WorkerProfessional professional = workerProfessionalRepository.findByWorkerProfileId(profile.getId()).orElse(null);
        java.util.List<com.newtron.newtron_workforce_backend.entity.Application> userApplications = applicationRepository.findByWorkerId(currentUser.getId());
        return buildRecommendedJobs(currentUser, professional, userApplications);
    }

    @Override
    @Transactional(readOnly = true)
    public WorkerDashboardResponse getWorkerDashboard(User currentUser) {
        WorkerProfile profile = workerProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("PROFILE_NOT_FOUND", "Worker profile not found for this user"));
        WorkerProfessional professional = workerProfessionalRepository.findByWorkerProfileId(profile.getId()).orElse(null);
        java.util.List<com.newtron.newtron_workforce_backend.entity.Application> applications = applicationRepository.findByWorkerId(currentUser.getId());

        return WorkerDashboardResponse.builder()
                .summary(buildWorkerSummary(currentUser, profile, professional))
                .availability(buildWorkerAvailability(professional))
                .quickActions(buildQuickActions(currentUser))
                .stats(buildWorkerStats(currentUser))
                .recentActivities(buildRecentActivities(currentUser, profile, applications))
                .recommendedJobs(buildRecommendedJobs(currentUser, professional, applications))
                .build();
    }
}
