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

    @PatchMapping("/{id}/hire")
    @Transactional
    public ApiResponse<Map<String, Object>> hireApplication(
            @PathVariable("id") Long applicationId,
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

        // Validate monthly salary structure
        BigDecimal salaryBase = job.getMonthlySalaryAmount();
        if (salaryBase == null || salaryBase.compareTo(BigDecimal.ZERO) <= 0) {
            throw new ValidationException("INVALID_SALARY_STRUCTURE", "Salary structure is unavailable for commission calculation.");
        }

        // Validate duplicate commission protection
        if (commissionRepository.existsByApplicationId(applicationId)) {
            throw new ValidationException("COMMISSION_ALREADY_EXISTS", "A commission record already exists for this application.");
        }

        // Complete hiring state transitions
        Instant now = Instant.now();
        application.setHiredAt(now);
        application.setStatus("Hired");
        applicationRepository.save(application);

        BigDecimal commissionRate = new BigDecimal("0.0500");
        BigDecimal commissionAmount = salaryBase.multiply(commissionRate);

        Commission commission = Commission.builder()
                .company(company)
                .job(job)
                .application(application)
                .worker(application.getWorker())
                .salaryBase(salaryBase)
                .commissionRate(commissionRate)
                .commissionAmount(commissionAmount)
                .status(CommissionStatus.PENDING)
                .hiredAt(now)
                .build();
        commissionRepository.save(commission);

        if (application.getWorker() != null) {
            String jobTitle = job.getTitle();
            notificationHelper.sendNotification(
                    application.getWorker(),
                    "Hired!",
                    "Congratulations, you have been hired for " + jobTitle + ".",
                    NotificationCategory.OFFERS,
                    NotificationPriority.HIGH,
                    "APPLICATION_DETAILS:" + application.getId()
            );
        }

        Map<String, Object> result = new HashMap<>();
        result.put("applicationId", applicationId);
        result.put("status", "Hired");
        result.put("hiredAt", now);
        result.put("salaryBase", salaryBase);
        result.put("commissionRate", commissionRate);
        result.put("commissionAmount", commissionAmount);
        result.put("commissionStatus", CommissionStatus.PENDING.name());

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

    private long getStartTime(HttpServletRequest request) {
        Object attr = request.getAttribute("startTimeMs");
        if (attr instanceof Long) {
            return (Long) attr;
        }
        return System.currentTimeMillis();
    }
}
