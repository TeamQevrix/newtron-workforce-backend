package com.newtron.newtron_workforce_backend.controller;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.enums.Role;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.common.exception.ResourceNotFoundException;
import com.newtron.newtron_workforce_backend.common.exception.ValidationException;
import com.newtron.newtron_workforce_backend.common.logging.RequestContext;
import com.newtron.newtron_workforce_backend.common.response.ApiResponse;
import com.newtron.newtron_workforce_backend.common.response.ApiResponseFactory;
import com.newtron.newtron_workforce_backend.dto.RecruiterJobCreateRequest;
import com.newtron.newtron_workforce_backend.dto.RecruiterJobListResponse;
import com.newtron.newtron_workforce_backend.dto.RecruiterJobResponse;
import com.newtron.newtron_workforce_backend.dto.JobApplicantDto;
import com.newtron.newtron_workforce_backend.entity.Job;
import com.newtron.newtron_workforce_backend.entity.Company;
import com.newtron.newtron_workforce_backend.entity.Application;
import com.newtron.newtron_workforce_backend.entity.WorkerProfile;
import com.newtron.newtron_workforce_backend.entity.WorkerSkill;
import com.newtron.newtron_workforce_backend.entity.WorkerAddress;
import com.newtron.newtron_workforce_backend.repository.JobRepository;
import com.newtron.newtron_workforce_backend.repository.ApplicationRepository;
import com.newtron.newtron_workforce_backend.repository.CompanyRepository;
import com.newtron.newtron_workforce_backend.repository.WorkerProfileRepository;
import com.newtron.newtron_workforce_backend.repository.WorkerSkillRepository;
import com.newtron.newtron_workforce_backend.repository.WorkerReviewRepository;
import com.newtron.newtron_workforce_backend.repository.WorkerAddressRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.jdbc.core.JdbcTemplate;
import com.newtron.newtron_workforce_backend.dto.RecruiterJobDetailResponse;
import com.newtron.newtron_workforce_backend.dto.RecruiterJobUpdateRequest;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.ArrayList;
import java.util.Set;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/recruiter/jobs")
@RequiredArgsConstructor
public class RecruiterJobController {

    private final JobRepository jobRepository;
    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final CompanyRepository companyRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final WorkerSkillRepository workerSkillRepository;
    private final WorkerReviewRepository workerReviewRepository;
    private final WorkerAddressRepository workerAddressRepository;
    private final JdbcTemplate jdbcTemplate;

    @PostMapping
    @Transactional
    public ApiResponse<RecruiterJobResponse> createJob(
            @Valid @RequestBody RecruiterJobCreateRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        if (currentUser.getRole() != Role.RECRUITER) {
            throw new ValidationException("UNAUTHORIZED_ACCESS", "Access denied. Only recruiters can post jobs.");
        }

        Company company = companyRepository.findByOwnerId(currentUser.getId())
                .orElseThrow(() -> new ValidationException("COMPANY_REQUIRED", "Please complete your company profile onboarding first."));

        String engagementType = request.getEngagementType();
        if ("MONTHLY".equalsIgnoreCase(engagementType)) {
            if (request.getMonthlySalaryAmount() == null) {
                throw new ValidationException("VALIDATION_ERROR", "Monthly salary is required for MONTHLY engagement");
            }
            engagementType = "MONTHLY";
        } else {
            if (request.getSalary() == null || request.getSalary().trim().isEmpty()) {
                throw new ValidationException("VALIDATION_ERROR", "Daily wage/salary is required");
            }
            engagementType = "DAILY";
        }

        String engagementDurationType = request.getEngagementDurationType();
        Integer durationValue = request.getDurationValue();
        String legacyDuration = request.getDuration();

        if (engagementDurationType == null) {
            if (legacyDuration == null || legacyDuration.trim().isEmpty()) {
                throw new ValidationException("VALIDATION_ERROR", "Duration is required");
            }
            legacyDuration = legacyDuration.trim();
        } else if ("PERMANENT".equalsIgnoreCase(engagementDurationType)) {
            engagementDurationType = "PERMANENT";
            durationValue = null;
            legacyDuration = "Permanent";
        } else if ("FIXED_TERM".equalsIgnoreCase(engagementDurationType)) {
            engagementDurationType = "FIXED_TERM";
            if (durationValue == null || durationValue <= 0) {
                throw new ValidationException("VALIDATION_ERROR", "Valid duration value is required for FIXED_TERM engagement");
            }
            if ("DAILY".equals(engagementType)) {
                legacyDuration = durationValue + " Days";
            } else if ("MONTHLY".equals(engagementType)) {
                legacyDuration = durationValue + " Months";
            } else {
                legacyDuration = durationValue + "";
            }
        } else {
            throw new ValidationException("VALIDATION_ERROR", "Unsupported engagement duration type");
        }

        Job job = Job.builder()
                .title(request.getTitle().trim())
                .companyName(company.getCompanyName())
                .company(company)
                .category(request.getCategory().trim())
                .city(request.getCity().trim())
                .workersRequired(request.getWorkersRequired())
                .engagementType(engagementType)
                .salary(request.getSalary() != null ? request.getSalary().trim() : null)
                .monthlySalaryAmount(request.getMonthlySalaryAmount())
                .engagementDurationType(engagementDurationType)
                .durationValue(durationValue)
                .duration(legacyDuration)
                .description(request.getDescription().trim())
                .experienceRequired(request.getExperienceRequired() != null ? request.getExperienceRequired().trim() : null)
                .shiftHours(request.getShiftHours() != null ? request.getShiftHours().trim() : null)
                .benefits(request.getBenefits() != null ? request.getBenefits().trim() : null)
                .joiningDate(request.getJoiningDate() != null ? request.getJoiningDate().trim() : null)
                .status("Active")
                .recruiter(currentUser)
                .build();

        Job savedJob = jobRepository.save(job);

        RecruiterJobResponse response = RecruiterJobResponse.builder()
                .id(savedJob.getId())
                .companyName(savedJob.getCompany() != null ? savedJob.getCompany().getCompanyName() : savedJob.getCompanyName())
                .title(savedJob.getTitle())
                .category(savedJob.getCategory())
                .city(savedJob.getCity())
                .workersRequired(savedJob.getWorkersRequired())
                .salary(savedJob.getSalary())
                .duration(savedJob.getDuration())
                .description(savedJob.getDescription())
                .status(savedJob.getStatus())
                .monthlySalaryAmount(savedJob.getMonthlySalaryAmount())
                .build();

        return ApiResponseFactory.success(response, "Job requirement posted successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @GetMapping
    public ApiResponse<List<RecruiterJobListResponse>> getRecruiterJobs(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        if (currentUser.getRole() != Role.RECRUITER) {
            throw new ValidationException("UNAUTHORIZED_ACCESS", "Access denied. Only recruiters can view their jobs.");
        }

        Company company = companyRepository.findByOwnerId(currentUser.getId())
                .orElseThrow(() -> new ValidationException("COMPANY_REQUIRED", "Please complete your company profile onboarding first."));

        List<Job> jobs = jobRepository.findByCompanyIdOrderByIdDesc(company.getId());

        // Precompute counts in exactly two queries
        List<Object[]> applicantCountsResult = applicationRepository.countApplicantsByCompanyId(company.getId());
        List<Object[]> hiredCountsResult = applicationRepository.countHiredByCompanyId(company.getId());

        Map<Long, Long> applicantCountsMap = new HashMap<>();
        for (Object[] row : applicantCountsResult) {
            applicantCountsMap.put((Long) row[0], (Long) row[1]);
        }

        Map<Long, Long> hiredCountsMap = new HashMap<>();
        for (Object[] row : hiredCountsResult) {
            hiredCountsMap.put((Long) row[0], (Long) row[1]);
        }

        List<RecruiterJobListResponse> listResponses = jobs.stream()
                .map(job -> RecruiterJobListResponse.builder()
                        .id(job.getId())
                        .companyName(job.getCompany() != null ? job.getCompany().getCompanyName() : job.getCompanyName())
                        .title(job.getTitle())
                        .category(job.getCategory())
                        .city(job.getCity())
                        .salary(job.getSalary())
                        .duration(job.getDuration())
                        .description(job.getDescription())
                        .status(job.getStatus())
                        .workersRequired(job.getWorkersRequired())
                        .applicantsCount(applicantCountsMap.getOrDefault(job.getId(), 0L))
                        .hiredCount(hiredCountsMap.getOrDefault(job.getId(), 0L))
                        .monthlySalaryAmount(job.getMonthlySalaryAmount())
                        .engagementType(job.getEngagementType())
                        .build())
                .collect(Collectors.toList());

        return ApiResponseFactory.success(listResponses, "Recruiter jobs retrieved successfully",
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

    @GetMapping("/{jobId}/applicants")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ApiResponse<List<JobApplicantDto>> getJobApplicants(
            @PathVariable("jobId") Long jobId,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        if (currentUser.getRole() != Role.RECRUITER) {
            throw new ValidationException("UNAUTHORIZED_ACCESS", "Access denied. Only recruiters can view applicants.");
        }

        Company company = companyRepository.findByOwnerId(currentUser.getId())
                .orElseThrow(() -> new ValidationException("COMPANY_REQUIRED", "Please complete your company profile onboarding first."));

        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("JOB_NOT_FOUND", "Job not found"));

        if (job.getCompany() == null || !job.getCompany().getId().equals(company.getId())) {
            throw new ResourceNotFoundException("JOB_NOT_FOUND", "Job not found");
        }

        List<Application> applications = applicationRepository.findByJobId(jobId);
        List<JobApplicantDto> dtos = new ArrayList<>();

        if (applications.isEmpty()) {
            return ApiResponseFactory.success(dtos, "Applicants retrieved successfully",
                    RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
        }

        List<Long> workerIds = applications.stream()
                .filter(a -> a.getWorker() != null)
                .map(a -> a.getWorker().getId())
                .collect(Collectors.toList());

        // Batch load profiles
        List<WorkerProfile> profiles = workerProfileRepository.findByUserIds(workerIds);
        Map<Long, WorkerProfile> profileMap = profiles.stream()
                .collect(Collectors.toMap(wp -> wp.getUser().getId(), wp -> wp, (a, b) -> a));

        List<Long> profileIds = profiles.stream().map(WorkerProfile::getId).collect(Collectors.toList());

        // Batch load skills
        List<WorkerSkill> skills = workerSkillRepository.findPrimarySkillsByWorkerIds(workerIds);
        Map<Long, WorkerSkill> skillMap = skills.stream()
                .collect(Collectors.toMap(ws -> ws.getWorkerProfile().getUser().getId(), ws -> ws, (a, b) -> a));

        // Batch load reviews and ratings
        List<Object[]> ratingRows = workerReviewRepository.findAverageRatingsByWorkerIds(workerIds);
        Map<Long, Double> ratingMap = new HashMap<>();
        for (Object[] row : ratingRows) {
            ratingMap.put((Long) row[0], ((Number) row[1]).doubleValue());
        }

        // Batch load completed jobs count
        List<Object[]> completedJobsRows = applicationRepository.countCompletedJobsByWorkerIds(workerIds);
        Map<Long, Integer> completedJobsMap = new HashMap<>();
        for (Object[] row : completedJobsRows) {
            completedJobsMap.put((Long) row[0], ((Number) row[1]).intValue());
        }

        // Batch load addresses
        List<WorkerAddress> addresses = profileIds.isEmpty() ? new ArrayList<>() : workerAddressRepository.findByWorkerProfileIdIn(profileIds);
        Map<Long, WorkerAddress> addressMap = addresses.stream()
                .collect(Collectors.toMap(wa -> wa.getWorkerProfile().getUser().getId(), wa -> wa, (a, b) -> a));

        List<Long> cityIds = addresses.stream()
                .map(WorkerAddress::getCityId)
                .filter(java.util.Objects::nonNull)
                .distinct()
                .collect(Collectors.toList());

        Map<Long, String> cityNamesMap = new HashMap<>();
        if (!cityIds.isEmpty()) {
            String sql = "SELECT id, name FROM master_cities WHERE id IN (" +
                    cityIds.stream().map(Object::toString).collect(Collectors.joining(",")) + ")";
            jdbcTemplate.query(sql, (rs) -> {
                cityNamesMap.put(rs.getLong("id"), rs.getString("name"));
            });
        }

        for (Application app : applications) {
            if (app.getWorker() == null) continue;
            Long workerId = app.getWorker().getId();
            WorkerProfile profile = profileMap.get(workerId);
            WorkerSkill skill = skillMap.get(workerId);
            WorkerAddress address = addressMap.get(workerId);

            String workerName = profile != null ? profile.getFullName() : (app.getWorker().getFullName() != null ? app.getWorker().getFullName() : "Newtron Candidate");
            String photo = profile != null ? profile.getPhotoStorageKey() : null;
            String skillName = (skill != null && skill.getSkill() != null) ? skill.getSkill().getName() : "General";
            Integer expYears = skill != null ? skill.getExperienceYears() : 0;
            String cityName = (address != null && address.getCityId() != null) ? cityNamesMap.get(address.getCityId()) : "";
            String pref = profile != null && profile.getProfessionalDetails() != null && profile.getProfessionalDetails().getPreferredWorkType() != null ? profile.getProfessionalDetails().getPreferredWorkType().name() : "BOTH";

            dtos.add(JobApplicantDto.builder()
                    .applicationId(app.getId())
                    .workerId(workerId)
                    .workerName(workerName)
                    .profilePhoto(photo)
                    .skill(skillName)
                    .experienceYears(expYears)
                    .city(cityName != null ? cityName : "")
                    .rating(ratingMap.getOrDefault(workerId, 0.0))
                    .jobsCompleted(completedJobsMap.getOrDefault(workerId, 0))
                    .status(app.getStatus())
                    .appliedDate(app.getAppliedDate() != null ? app.getAppliedDate() : "")
                    .currentStep(app.getCurrentStep())
                    .workPreference(pref)
                    .build());
        }

        return ApiResponseFactory.success(dtos, "Applicants retrieved successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @GetMapping("/{id}")
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public ApiResponse<RecruiterJobDetailResponse> getJobDetails(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        if (currentUser.getRole() != Role.RECRUITER) {
            throw new ValidationException("UNAUTHORIZED_ACCESS", "Access denied. Only recruiters can view jobs.");
        }

        Company company = companyRepository.findByOwnerId(currentUser.getId())
                .orElseThrow(() -> new ValidationException("COMPANY_REQUIRED", "Please complete your company profile onboarding first."));

        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JOB_NOT_FOUND", "Job not found"));

        if (job.getCompany() == null || !job.getCompany().getId().equals(company.getId())) {
            throw new ResourceNotFoundException("JOB_NOT_FOUND", "Job not found");
        }

        RecruiterJobDetailResponse response = RecruiterJobDetailResponse.builder()
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

        return ApiResponseFactory.success(response, "Job retrieved successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @PutMapping("/{id}")
    @Transactional
    public ApiResponse<RecruiterJobResponse> updateJob(
            @PathVariable("id") Long id,
            @Valid @RequestBody RecruiterJobUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        if (currentUser.getRole() != Role.RECRUITER) {
            throw new ValidationException("UNAUTHORIZED_ACCESS", "Access denied. Only recruiters can update jobs.");
        }

        Company company = companyRepository.findByOwnerId(currentUser.getId())
                .orElseThrow(() -> new ValidationException("COMPANY_REQUIRED", "Please complete your company profile onboarding first."));

        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JOB_NOT_FOUND", "Job not found"));

        if (job.getCompany() == null || !job.getCompany().getId().equals(company.getId())) {
            throw new ResourceNotFoundException("JOB_NOT_FOUND", "Job not found");
        }

        String engagementType = request.getEngagementType();
        if ("MONTHLY".equalsIgnoreCase(engagementType)) {
            if (request.getMonthlySalaryAmount() == null) {
                throw new ValidationException("VALIDATION_ERROR", "Monthly salary is required for MONTHLY engagement");
            }
            engagementType = "MONTHLY";
        } else {
            if (request.getSalary() == null || request.getSalary().trim().isEmpty()) {
                throw new ValidationException("VALIDATION_ERROR", "Daily wage/salary is required");
            }
            engagementType = "DAILY";
        }

        String engagementDurationType = request.getEngagementDurationType();
        Integer durationValue = request.getDurationValue();
        String legacyDuration = request.getDuration();

        if (engagementDurationType == null) {
            if (legacyDuration == null || legacyDuration.trim().isEmpty()) {
                throw new ValidationException("VALIDATION_ERROR", "Duration is required");
            }
            legacyDuration = legacyDuration.trim();
        } else if ("PERMANENT".equalsIgnoreCase(engagementDurationType)) {
            engagementDurationType = "PERMANENT";
            durationValue = null;
            legacyDuration = "Permanent";
        } else if ("FIXED_TERM".equalsIgnoreCase(engagementDurationType)) {
            engagementDurationType = "FIXED_TERM";
            if (durationValue == null || durationValue <= 0) {
                throw new ValidationException("VALIDATION_ERROR", "Valid duration value is required for FIXED_TERM engagement");
            }
            if ("DAILY".equals(engagementType)) {
                legacyDuration = durationValue + " Days";
            } else if ("MONTHLY".equals(engagementType)) {
                legacyDuration = durationValue + " Months";
            } else {
                legacyDuration = durationValue + "";
            }
        } else {
            throw new ValidationException("VALIDATION_ERROR", "Unsupported engagement duration type");
        }

        job.setTitle(request.getTitle().trim());
        job.setCategory(request.getCategory().trim());
        job.setCity(request.getCity().trim());
        job.setWorkersRequired(request.getWorkersRequired());
        job.setEngagementType(engagementType);
        job.setSalary(request.getSalary() != null ? request.getSalary().trim() : null);
        job.setMonthlySalaryAmount(request.getMonthlySalaryAmount());
        job.setEngagementDurationType(engagementDurationType);
        job.setDurationValue(durationValue);
        job.setDuration(legacyDuration);
        job.setDescription(request.getDescription().trim());
        job.setExperienceRequired(request.getExperienceRequired() != null ? request.getExperienceRequired().trim() : null);
        job.setShiftHours(request.getShiftHours() != null ? request.getShiftHours().trim() : null);
        job.setBenefits(request.getBenefits() != null ? request.getBenefits().trim() : null);
        job.setJoiningDate(request.getJoiningDate() != null ? request.getJoiningDate().trim() : null);

        Job savedJob = jobRepository.save(job);

        RecruiterJobResponse response = RecruiterJobResponse.builder()
                .id(savedJob.getId())
                .companyName(savedJob.getCompany() != null ? savedJob.getCompany().getCompanyName() : savedJob.getCompanyName())
                .title(savedJob.getTitle())
                .category(savedJob.getCategory())
                .city(savedJob.getCity())
                .workersRequired(savedJob.getWorkersRequired())
                .salary(savedJob.getSalary())
                .duration(savedJob.getDuration())
                .description(savedJob.getDescription())
                .status(savedJob.getStatus())
                .monthlySalaryAmount(savedJob.getMonthlySalaryAmount())
                .build();

        return ApiResponseFactory.success(response, "Job updated successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @PatchMapping("/{id}/status")
    @Transactional
    public ApiResponse<RecruiterJobResponse> closeJob(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        if (currentUser.getRole() != Role.RECRUITER) {
            throw new ValidationException("UNAUTHORIZED_ACCESS", "Access denied. Only recruiters can update jobs.");
        }

        Company company = companyRepository.findByOwnerId(currentUser.getId())
                .orElseThrow(() -> new ValidationException("COMPANY_REQUIRED", "Please complete your company profile onboarding first."));

        Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JOB_NOT_FOUND", "Job not found"));

        if (job.getCompany() == null || !job.getCompany().getId().equals(company.getId())) {
            throw new ResourceNotFoundException("JOB_NOT_FOUND", "Job not found");
        }

        job.setStatus("Closed");
        Job savedJob = jobRepository.save(job);

        RecruiterJobResponse response = RecruiterJobResponse.builder()
                .id(savedJob.getId())
                .companyName(savedJob.getCompany() != null ? savedJob.getCompany().getCompanyName() : savedJob.getCompanyName())
                .title(savedJob.getTitle())
                .category(savedJob.getCategory())
                .city(savedJob.getCity())
                .workersRequired(savedJob.getWorkersRequired())
                .salary(savedJob.getSalary())
                .duration(savedJob.getDuration())
                .description(savedJob.getDescription())
                .status(savedJob.getStatus())
                .monthlySalaryAmount(savedJob.getMonthlySalaryAmount())
                .build();

        return ApiResponseFactory.success(response, "Job closed successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }
}
