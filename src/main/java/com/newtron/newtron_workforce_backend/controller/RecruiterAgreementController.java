package com.newtron.newtron_workforce_backend.controller;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.common.exception.ResourceNotFoundException;
import com.newtron.newtron_workforce_backend.common.exception.ValidationException;
import com.newtron.newtron_workforce_backend.common.logging.RequestContext;
import com.newtron.newtron_workforce_backend.common.response.ApiResponse;
import com.newtron.newtron_workforce_backend.common.response.ApiResponseFactory;
import com.newtron.newtron_workforce_backend.entity.Agreement;
import com.newtron.newtron_workforce_backend.entity.Company;
import com.newtron.newtron_workforce_backend.enums.AgreementStatus;
import com.newtron.newtron_workforce_backend.repository.AgreementRepository;
import com.newtron.newtron_workforce_backend.repository.CompanyRepository;
import com.newtron.newtron_workforce_backend.repository.WorkerProfileRepository;
import com.newtron.newtron_workforce_backend.service.NotificationHelper;
import com.newtron.newtron_workforce_backend.enums.NotificationCategory;
import com.newtron.newtron_workforce_backend.enums.NotificationPriority;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.Map;

@RestController
@RequestMapping("/api/v1/recruiter/agreements")
@RequiredArgsConstructor
public class RecruiterAgreementController {

    private final AgreementRepository agreementRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final NotificationHelper notificationHelper;

    @GetMapping("/{agreementId}")
    public ApiResponse<Map<String, Object>> getAgreement(
            @PathVariable("agreementId") Long agreementId,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        Company company = companyRepository.findByOwnerId(currentUser.getId())
                .orElseThrow(() -> new ValidationException("COMPANY_REQUIRED", "Company profile onboarding is not completed."));

        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("AGREEMENT_NOT_FOUND", "Agreement not found"));

        if (agreement.getCompany() == null || !agreement.getCompany().getId().equals(company.getId())) {
            throw new ValidationException("UNAUTHORIZED_ACCESS", "You do not own this agreement");
        }

        Map<String, Object> response = mapAgreementResponse(agreement);

        return ApiResponseFactory.success(response, "Agreement retrieved successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @PatchMapping("/{agreementId}")
    @Transactional
    public ApiResponse<Map<String, Object>> updateAgreementTerms(
            @PathVariable("agreementId") Long agreementId,
            @RequestBody Map<String, Object> updateRequest,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        Company company = companyRepository.findByOwnerId(currentUser.getId())
                .orElseThrow(() -> new ValidationException("COMPANY_REQUIRED", "Company profile onboarding is not completed."));

        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("AGREEMENT_NOT_FOUND", "Agreement not found"));

        if (agreement.getCompany() == null || !agreement.getCompany().getId().equals(company.getId())) {
            throw new ValidationException("UNAUTHORIZED_ACCESS", "You do not own this agreement");
        }

        if (agreement.getStatus() != AgreementStatus.DRAFT) {
            throw new ValidationException("INVALID_STATUS", "Only DRAFT agreements can be updated");
        }

        if (Boolean.TRUE.equals(agreement.getIsLocked())) {
            throw new ValidationException("AGREEMENT_LOCKED", "Agreement is locked and cannot be updated");
        }

        if (updateRequest.containsKey("clientCustomTerms")) {
            Object termsObj = updateRequest.get("clientCustomTerms");
            agreement.setClientCustomTerms(termsObj != null ? termsObj.toString() : null);
            agreement = agreementRepository.save(agreement);
        }

        Map<String, Object> response = mapAgreementResponse(agreement);

        return ApiResponseFactory.success(response, "Agreement updated successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @PostMapping("/{agreementId}/finalize")
    @Transactional
    public ApiResponse<Map<String, Object>> finalizeAgreement(
            @PathVariable("agreementId") Long agreementId,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        Company company = companyRepository.findByOwnerId(currentUser.getId())
                .orElseThrow(() -> new ValidationException("COMPANY_REQUIRED", "Company profile onboarding is not completed."));

        Agreement agreement = agreementRepository.findById(agreementId)
                .orElseThrow(() -> new ResourceNotFoundException("AGREEMENT_NOT_FOUND", "Agreement not found"));

        if (agreement.getCompany() == null || !agreement.getCompany().getId().equals(company.getId())) {
            throw new ValidationException("UNAUTHORIZED_ACCESS", "You do not own this agreement");
        }

        if (agreement.getStatus() != AgreementStatus.DRAFT) {
            throw new ValidationException("INVALID_STATUS", "Only DRAFT agreements can be finalized");
        }

        if (Boolean.TRUE.equals(agreement.getIsLocked())) {
            throw new ValidationException("AGREEMENT_LOCKED", "Agreement is already locked");
        }

        agreement.setStatus(AgreementStatus.PENDING_WORKER_ACCEPTANCE);
        agreement.setIsLocked(true);
        agreement = agreementRepository.save(agreement);

        notificationHelper.sendNotification(
                agreement.getWorker(),
                "New Job Agreement",
                company.getCompanyName() + " has sent you a new job agreement.",
                NotificationCategory.OFFERS,
                NotificationPriority.HIGH,
                "APPLICATION_DETAILS:" + agreement.getApplication().getId()
        );

        Map<String, Object> response = new HashMap<>();
        response.put("agreementId", agreement.getId());
        response.put("status", agreement.getStatus().name());
        response.put("agreementVersion", agreement.getAgreementVersion());
        response.put("isLocked", agreement.getIsLocked());

        return ApiResponseFactory.success(response, "Agreement finalized successfully",
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

    private Map<String, Object> mapAgreementResponse(Agreement agreement) {
        Map<String, Object> response = new HashMap<>();
        response.put("agreementId", agreement.getId());
        
        Long workerProfileId = null;
        if (agreement.getWorker() != null) {
            workerProfileId = workerProfileRepository.findByUserId(agreement.getWorker().getId())
                    .map(com.newtron.newtron_workforce_backend.entity.WorkerProfile::getId)
                    .orElse(null);
        }
        response.put("workerId", workerProfileId);
        
        response.put("status", agreement.getStatus() != null ? agreement.getStatus().name() : null);
        response.put("agreementVersion", agreement.getAgreementVersion());
        response.put("isLocked", agreement.getIsLocked());
        response.put("workerNameSnapshot", agreement.getWorkerNameSnapshot());
        response.put("companyNameSnapshot", agreement.getCompanyNameSnapshot());
        response.put("jobTitleSnapshot", agreement.getJobTitleSnapshot());
        response.put("jobDescriptionSnapshot", agreement.getJobDescriptionSnapshot());
        response.put("workLocationSnapshot", agreement.getWorkLocationSnapshot());
        response.put("primarySkillSnapshot", agreement.getPrimarySkillSnapshot());
        response.put("engagementType", agreement.getEngagementType());
        response.put("dailyWage", agreement.getDailyWage());
        response.put("monthlySalary", agreement.getMonthlySalary());
        response.put("commissionRate", agreement.getCommissionRate());
        response.put("commissionPayer", agreement.getCommissionPayer());
        response.put("commissionAmount", agreement.getCommissionAmount());
        response.put("duration", agreement.getDuration());
        response.put("engagementDurationType", agreement.getEngagementDurationType());
        response.put("durationValue", agreement.getDurationValue());
        response.put("clientCustomTerms", agreement.getClientCustomTerms());
        response.put("standardTermsVersion", agreement.getStandardTermsVersion());
        response.put("documentUrl", agreement.getDocumentUrl());
        response.put("expiresAt", agreement.getExpiresAt());
        return response;
    }
}
