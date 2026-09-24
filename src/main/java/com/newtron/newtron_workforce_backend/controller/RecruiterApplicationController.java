package com.newtron.newtron_workforce_backend.controller;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.common.exception.ResourceNotFoundException;
import com.newtron.newtron_workforce_backend.common.exception.ValidationException;
import com.newtron.newtron_workforce_backend.common.logging.RequestContext;
import com.newtron.newtron_workforce_backend.common.response.ApiResponse;
import com.newtron.newtron_workforce_backend.common.response.ApiResponseFactory;
import com.newtron.newtron_workforce_backend.dto.WorkerReviewRequest;
import com.newtron.newtron_workforce_backend.entity.Application;
import com.newtron.newtron_workforce_backend.entity.WorkerEarning;
import com.newtron.newtron_workforce_backend.entity.WorkerReview;
import com.newtron.newtron_workforce_backend.repository.ApplicationRepository;
import com.newtron.newtron_workforce_backend.repository.WorkerEarningRepository;
import com.newtron.newtron_workforce_backend.repository.WorkerReviewRepository;
import com.newtron.newtron_workforce_backend.enums.NotificationCategory;
import com.newtron.newtron_workforce_backend.enums.NotificationPriority;
import com.newtron.newtron_workforce_backend.service.NotificationHelper;
import com.newtron.newtron_workforce_backend.entity.Company;
import com.newtron.newtron_workforce_backend.entity.Job;
import com.newtron.newtron_workforce_backend.entity.Commission;
import com.newtron.newtron_workforce_backend.enums.CommissionStatus;
import com.newtron.newtron_workforce_backend.repository.CompanyRepository;
import com.newtron.newtron_workforce_backend.repository.JobRepository;
import com.newtron.newtron_workforce_backend.repository.CommissionRepository;
import com.newtron.newtron_workforce_backend.repository.WorkerProfileRepository;
import com.newtron.newtron_workforce_backend.entity.WorkerProfile;
import com.newtron.newtron_workforce_backend.entity.Agreement;
import com.newtron.newtron_workforce_backend.enums.AgreementStatus;
import com.newtron.newtron_workforce_backend.repository.AgreementRepository;
import com.newtron.newtron_workforce_backend.auth.enums.Role;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.Instant;
import java.util.Map;
import java.util.HashMap;
import com.newtron.newtron_workforce_backend.dto.HireRequestDto;

@RestController
@RequestMapping("/api/v1/recruiter/applications")
@RequiredArgsConstructor
public class RecruiterApplicationController {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final WorkerEarningRepository workerEarningRepository;
    private final WorkerReviewRepository workerReviewRepository;
    private final NotificationHelper notificationHelper;
    private final CompanyRepository companyRepository;
    private final JobRepository jobRepository;
    private final CommissionRepository commissionRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final AgreementRepository agreementRepository;
    private final com.newtron.newtron_workforce_backend.repository.WorkerSkillRepository workerSkillRepository;
    private final com.newtron.newtron_workforce_backend.repository.WorkerAddressRepository workerAddressRepository;
    private final org.springframework.jdbc.core.JdbcTemplate jdbcTemplate;

    @PatchMapping("/{id}/hire")
    @Transactional
    public ApiResponse<Map<String, Object>> hireApplication(
            @PathVariable("id") Long applicationId,
            @RequestBody HireRequestDto hireRequest,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        if (currentUser.getRole() != Role.RECRUITER) {
            throw new ValidationException("UNAUTHORIZED_ACCESS", "Access denied. Only recruiters can hire applicants.");
        }

        Company company = companyRepository.findByOwnerId(currentUser.getId())
                .orElseThrow(() -> new ValidationException("COMPANY_REQUIRED", "Company profile onboarding is not completed."));

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("APPLICATION_NOT_FOUND", "Application not found"));

        // Validate Company Ownership
        if (application.getJob() == null || application.getJob().getCompany() == null ||
                !application.getJob().getCompany().getId().equals(company.getId())) {
            throw new ResourceNotFoundException("APPLICATION_NOT_FOUND", "Application not found");
        }

        java.util.Optional<Agreement> existingAgreementOpt = agreementRepository.findByApplicationId(applicationId);
        if (existingAgreementOpt.isPresent()) {
            Agreement existingAgreement = existingAgreementOpt.get();
            if (existingAgreement.getStatus() == AgreementStatus.DRAFT ||
                existingAgreement.getStatus() == AgreementStatus.PENDING_WORKER_ACCEPTANCE ||
                existingAgreement.getStatus() == AgreementStatus.ACTIVE) {
                
                Map<String, Object> result = new HashMap<>();
                result.put("applicationId", applicationId);
                result.put("agreementId", existingAgreement.getId());
                result.put("status", existingAgreement.getStatus().name());
                return ApiResponseFactory.success(result, "Worker hired successfully",
                        RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
            } else {
                throw new ValidationException("ALREADY_HIRED", "An agreement already exists for this application and cannot be rehired.");
            }
        }

        // PESSIMISTIC LOCKING: Lock Job
        Job job = jobRepository.findByIdForUpdate(application.getJob().getId())
                .orElseThrow(() -> new ResourceNotFoundException("JOB_NOT_FOUND", "Job not found"));

        // Validate application status
        String currentStatus = application.getStatus();
        if ("Hired".equalsIgnoreCase(currentStatus) || "Completed".equalsIgnoreCase(currentStatus)) {
            throw new ValidationException("ALREADY_HIRED", "This job application is already marked as hired or completed");
        }
        if ("Rejected".equalsIgnoreCase(currentStatus)) {
            throw new ValidationException("INVALID_STATUS", "Rejected applications cannot be hired");
        }

        // Validate available capacity
        long hiredCount = applicationRepository.countByJobIdAndStatus(job.getId(), "Hired");
        long completedCount = applicationRepository.countByJobIdAndStatus(job.getId(), "Completed");
        long totalHired = hiredCount + completedCount;
        if (totalHired >= job.getWorkersRequired()) {
            throw new ValidationException("JOB_CAPACITY_REACHED", "Job requirements capacity has already been filled.");
        }

        // Validate worker preference against requested engagementType
        String workerPref = "BOTH";
        if (application.getWorker() != null) {
            WorkerProfile profile = workerProfileRepository.findByUserId(application.getWorker().getId()).orElse(null);
            if (profile != null && profile.getProfessionalDetails() != null && profile.getProfessionalDetails().getPreferredWorkType() != null) {
                workerPref = profile.getProfessionalDetails().getPreferredWorkType().name();
            }
        }
        
        String requestedEngagement = "MONTHLY";
        if (hireRequest != null && hireRequest.getEngagementType() != null) {
            requestedEngagement = hireRequest.getEngagementType().toUpperCase();
            if ("DAILY_WAGE".equals(requestedEngagement)) {
                requestedEngagement = "DAILY";
            }
        }

        String jobEngagement = job.getEngagementType() != null ? job.getEngagementType().toUpperCase() : "DAILY";
        if ("DAILY_WAGE".equals(jobEngagement)) {
            jobEngagement = "DAILY";
        }

        if (!jobEngagement.equals(requestedEngagement)) {
            throw new ValidationException("INVALID_ENGAGEMENT", "Requested engagement type (" + requestedEngagement + ") must match the Job's engagement type (" + jobEngagement + ").");
        }

        boolean isWorkerDaily = "DAILY".equals(workerPref) || "DAILY_WAGE".equals(workerPref);
        boolean isRequestedDaily = "DAILY".equals(requestedEngagement) || "DAILY_WAGE".equals(requestedEngagement);

        if (isWorkerDaily && !isRequestedDaily) {
            throw new ValidationException("INVALID_ENGAGEMENT", "Worker is only available for DAILY engagement.");
        }
        if ("MONTHLY".equals(workerPref) && !"MONTHLY".equals(requestedEngagement)) {
            throw new ValidationException("INVALID_ENGAGEMENT", "Worker is only available for MONTHLY engagement.");
        }

        BigDecimal dailyWage = null;
        BigDecimal monthlySalary = null;
        BigDecimal commissionRate = BigDecimal.ZERO;

        if ("DAILY".equals(requestedEngagement) || "DAILY_WAGE".equals(requestedEngagement)) {
            // DAILY
            commissionRate = BigDecimal.ZERO;
            String rawSalary = job.getSalary() != null ? job.getSalary() : "";
            try {
                String cleanSalary = rawSalary.replaceAll("[^0-9.]", "");
                if (!cleanSalary.isEmpty()) {
                    dailyWage = new BigDecimal(cleanSalary);
                }
            } catch (Exception e) {
                // Ignore parsing errors, let dailyWage be null
            }
        } else {
            // MONTHLY
            monthlySalary = job.getMonthlySalaryAmount();
            commissionRate = new BigDecimal("0.0500");
        }

        Instant now = Instant.now();

        // Complete hiring state transitions
        application.setEngagementType(requestedEngagement);
        applicationRepository.save(application);

        // Create Agreement
        Agreement agreement = Agreement.builder()
                .application(application)
                .job(job)
                .company(company)
                .worker(application.getWorker())
                .engagementType(requestedEngagement)
                .engagementDurationType(job.getEngagementDurationType())
                .durationValue(job.getDurationValue())
                .duration(job.getDuration())
                .dailyWage(dailyWage)
                .monthlySalary(monthlySalary)
                .commissionRate(commissionRate)
                .paymentResponsibility(null)
                .paymentDueTerms(null)
                .noticeDays(null)
                .cancellationTerms(null)
                .status(AgreementStatus.DRAFT)
                .clientAcceptedAt(now)
                // PHASE 4B SNAPSHOT
                .workerNameSnapshot(application.getWorker() != null ? application.getWorker().getFullName() : null)
                .companyNameSnapshot(company != null ? company.getCompanyName() : null)
                .jobTitleSnapshot(job != null ? job.getTitle() : null)
                .jobDescriptionSnapshot(job != null ? job.getDescription() : null)
                .workLocationSnapshot(job != null ? job.getCity() : null)
                .primarySkillSnapshot(job != null ? job.getCategory() : null)
                .commissionPayer("CLIENT")
                .commissionAmount(monthlySalary != null && commissionRate != null ? monthlySalary.multiply(commissionRate) : BigDecimal.ZERO)
                .clientCustomTerms(null)
                .agreementVersion(1)
                .isLocked(false)
                .standardTermsVersion(null)
                .documentUrl(null)
                .expiresAt(null)
                .build();
        
        agreement = agreementRepository.save(agreement);

        if (application.getWorker() != null) {
            String jobTitle = job.getTitle();
            notificationHelper.sendNotification(
                    application.getWorker(),
                    "Job Offer Received!",
                    "Congratulations, you have received a job offer for " + jobTitle + ".",
                    NotificationCategory.OFFERS,
                    NotificationPriority.HIGH,
                    "APPLICATION_DETAILS:" + application.getId()
            );
        }

        Map<String, Object> result = new HashMap<>();
        result.put("applicationId", applicationId);
        result.put("agreementId", agreement.getId());
        result.put("status", AgreementStatus.DRAFT.name());

        return ApiResponseFactory.success(result, "Worker hired successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @PatchMapping("/{id}/complete")
    @Transactional
    public ApiResponse<Void> completeApplication(
            @PathVariable("id") Long applicationId,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("APPLICATION_NOT_FOUND", "Application not found"));

        // Recruiter security check: only the recruiter who created the job can complete it
        if (application.getJob() == null || application.getJob().getRecruiter() == null ||
                !application.getJob().getRecruiter().getId().equals(currentUser.getId())) {
            throw new ValidationException("UNAUTHORIZED_ACCESS", "You do not own this job listing or application");
        }

        // Validate status: Hired -> Completed
        String currentStatus = application.getStatus();
        if ("Completed".equalsIgnoreCase(currentStatus)) {
            throw new ValidationException("ALREADY_COMPLETED", "This job application is already marked as completed");
        }

        if (!"Hired".equalsIgnoreCase(currentStatus)) {
            throw new ValidationException("INVALID_STATUS", "Only HIRED applications can be transitioned to COMPLETED");
        }

        // Validate and parse salary
        if (application.getJob() == null || application.getJob().getSalary() == null) {
            throw new ValidationException("INVALID_SALARY", "Job salary information is missing");
        }

        String rawSalary = application.getJob().getSalary();
        BigDecimal salaryAmount;
        try {
            String cleanSalary = rawSalary.replaceAll("[^0-9.]", "");
            if (cleanSalary.isEmpty()) {
                throw new IllegalArgumentException();
            }
            salaryAmount = new BigDecimal(cleanSalary);
        } catch (Exception e) {
            throw new ValidationException("INVALID_SALARY", "Job salary must be a valid numeric amount, found: " + rawSalary);
        }

        // Complete job
        boolean isFirstTimeCompleted = (application.getCompletedAt() == null);
        if (isFirstTimeCompleted) {
            application.setCompletedAt(Instant.now());
        }
        application.setStatus("Completed");
        applicationRepository.save(application);

        // Create earning record (Atomic operation)
        WorkerEarning earning = WorkerEarning.builder()
                .worker(application.getWorker())
                .application(application)
                .job(application.getJob())
                .amount(salaryAmount)
                .earningDate(Instant.now())
                .build();
        workerEarningRepository.save(earning);

        if (isFirstTimeCompleted && application.getWorker() != null) {
            String jobTitle = application.getJob() != null ? application.getJob().getTitle() : "Job";
            notificationHelper.sendNotification(
                    application.getWorker(),
                    "Job Completed",
                    "Your work at " + jobTitle + " is completed.",
                    NotificationCategory.SYSTEM,
                    NotificationPriority.NORMAL,
                    "APPLICATION_DETAILS:" + application.getId()
            );

            notificationHelper.sendNotification(
                    application.getWorker(),
                    "Earnings Logged",
                    "You earned ₹" + salaryAmount + " for " + jobTitle + ".",
                    NotificationCategory.PAYMENTS,
                    NotificationPriority.HIGH,
                    "APPLICATION_DETAILS:" + application.getId()
            );
        }

        return ApiResponseFactory.success(null, "Job application completed successfully and earnings logged",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @PostMapping("/{id}/review")
    @Transactional
    public ApiResponse<Void> reviewApplication(
            @PathVariable("id") Long applicationId,
            @Valid @RequestBody WorkerReviewRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("APPLICATION_NOT_FOUND", "Application not found"));

        // Recruiter security check: only the recruiter who created the job can submit reviews
        if (application.getJob() == null || application.getJob().getRecruiter() == null ||
                !application.getJob().getRecruiter().getId().equals(currentUser.getId())) {
            throw new ValidationException("UNAUTHORIZED_ACCESS", "You do not own this job listing or application");
        }

        // Validate status: must be exactly COMPLETED
        if (!"Completed".equalsIgnoreCase(application.getStatus())) {
            throw new ValidationException("INVALID_STATUS", "Only COMPLETED applications can be reviewed");
        }

        // Validate uniqueness: duplicate check
        if (workerReviewRepository.findByApplicationIdAndDeletedFalse(applicationId).isPresent()) {
            throw new ValidationException("ALREADY_REVIEWED", "This job application has already been reviewed");
        }

        // Create review record
        WorkerReview review = WorkerReview.builder()
                .worker(application.getWorker())
                .recruiter(currentUser)
                .application(application)
                .job(application.getJob())
                .rating(request.getRating())
                .comment(request.getComment() != null ? request.getComment().trim() : null)
                .build();
        workerReviewRepository.save(review);

        if (application.getWorker() != null) {
            notificationHelper.sendNotification(
                    application.getWorker(),
                    "New Feedback",
                    "Recruiter rated you " + request.getRating() + " stars.",
                    NotificationCategory.SYSTEM,
                    NotificationPriority.NORMAL,
                    "APPLICATION_DETAILS:" + application.getId()
            );
        }

        return ApiResponseFactory.success(null, "Worker review submitted successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    private User fetchCurrentUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new ResourceNotFoundException("NOT_AUTHENTICATED", "Not authenticated");
        }
        return userRepository.findByMobile(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));
    }

    @GetMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ApiResponse<Map<String, Object>> getApplicationDetails(
            @PathVariable("id") Long applicationId,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        if (currentUser.getRole() != Role.RECRUITER) {
            throw new ValidationException("UNAUTHORIZED_ACCESS", "Access denied.");
        }

        Company company = companyRepository.findByOwnerId(currentUser.getId())
                .orElseThrow(() -> new ValidationException("COMPANY_REQUIRED", "Company profile required."));

        Application application = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("APPLICATION_NOT_FOUND", "Application not found"));

        if (application.getJob() == null || application.getJob().getCompany() == null ||
                !application.getJob().getCompany().getId().equals(company.getId())) {
            throw new ResourceNotFoundException("APPLICATION_NOT_FOUND", "Application not found");
        }

        Job job = application.getJob();

        com.newtron.newtron_workforce_backend.dto.RecruiterJobDetailResponse jobDto = com.newtron.newtron_workforce_backend.dto.RecruiterJobDetailResponse.builder()
                .id(job.getId())
                .title(job.getTitle())
                .companyName(job.getCompany() != null ? job.getCompany().getCompanyName() : job.getCompanyName())
                .category(job.getCategory())
                .city(job.getCity())
                .workersRequired(job.getWorkersRequired())
                .engagementType(job.getEngagementType())
                .salary(job.getSalary())
                .monthlySalaryAmount(job.getMonthlySalaryAmount())
                .engagementDurationType(job.getEngagementDurationType())
                .durationValue(job.getDurationValue())
                .duration(job.getDuration())
                .description(job.getDescription())
                .experienceRequired(job.getExperienceRequired())
                .shiftHours(job.getShiftHours())
                .benefits(job.getBenefits())
                .joiningDate(job.getJoiningDate())
                .status(job.getStatus())
                .build();

        com.newtron.newtron_workforce_backend.dto.JobApplicantDto applicantDto = null;
        if (application.getWorker() != null) {
            Long workerId = application.getWorker().getId();
            
            com.newtron.newtron_workforce_backend.entity.WorkerProfile profile = workerProfileRepository.findByUserId(workerId).orElse(null);
            
            java.util.List<com.newtron.newtron_workforce_backend.entity.WorkerSkill> skills = workerSkillRepository.findPrimarySkillsByWorkerIds(java.util.Collections.singletonList(workerId));
            com.newtron.newtron_workforce_backend.entity.WorkerSkill skill = skills.isEmpty() ? null : skills.get(0);
            
            java.util.List<Object[]> ratingRows = workerReviewRepository.findAverageRatingsByWorkerIds(java.util.Collections.singletonList(workerId));
            Double rating = ratingRows.isEmpty() ? 0.0 : ((Number) ratingRows.get(0)[1]).doubleValue();
            
            java.util.List<Object[]> completedRows = applicationRepository.countCompletedJobsByWorkerIds(java.util.Collections.singletonList(workerId));
            Integer completedJobs = completedRows.isEmpty() ? 0 : ((Number) completedRows.get(0)[1]).intValue();
            
            String cityName = "";
            if (profile != null) {
                java.util.List<com.newtron.newtron_workforce_backend.entity.WorkerAddress> addresses = workerAddressRepository.findByWorkerProfileIdIn(java.util.Collections.singletonList(profile.getId()));
                if (!addresses.isEmpty() && addresses.get(0).getCityId() != null) {
                    try {
                        cityName = jdbcTemplate.queryForObject(
                            "SELECT name FROM master_cities WHERE id = ?",
                            new Object[]{addresses.get(0).getCityId()},
                            String.class
                        );
                    } catch (Exception e) {}
                }
            }
            
            String workerName = profile != null ? profile.getFullName() : (application.getWorker().getFullName() != null ? application.getWorker().getFullName() : "Newtron Candidate");
            String photo = profile != null ? profile.getPhotoStorageKey() : null;
            String skillName = (skill != null && skill.getSkill() != null) ? skill.getSkill().getName() : "General";
            Integer expYears = skill != null ? skill.getExperienceYears() : 0;
            String pref = profile != null && profile.getProfessionalDetails() != null && profile.getProfessionalDetails().getPreferredWorkType() != null ? profile.getProfessionalDetails().getPreferredWorkType().name() : "BOTH";

            applicantDto = com.newtron.newtron_workforce_backend.dto.JobApplicantDto.builder()
                    .applicationId(application.getId())
                    .workerId(workerId)
                    .workerName(workerName)
                    .profilePhoto(photo)
                    .skill(skillName)
                    .experienceYears(expYears)
                    .city(cityName != null ? cityName : "")
                    .rating(rating)
                    .jobsCompleted(completedJobs)
                    .status(application.getStatus())
                    .appliedDate(application.getAppliedDate() != null ? application.getAppliedDate() : "")
                    .currentStep(application.getCurrentStep())
                    .workPreference(pref)
                    .build();
        }

        Map<String, Object> result = new HashMap<>();
        result.put("applicant", applicantDto);
        result.put("job", jobDto);

        return ApiResponseFactory.success(result, "Application details retrieved successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    private long getStartTime(HttpServletRequest request) {
        Object attr = request.getAttribute("startTimeMs");
        if (attr instanceof Long) {
            return (Long) attr;
        }
        return System.currentTimeMillis();
    }
}
