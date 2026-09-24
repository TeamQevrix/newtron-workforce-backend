package com.newtron.newtron_workforce_backend.controller;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.common.exception.ResourceNotFoundException;
import com.newtron.newtron_workforce_backend.common.exception.ValidationException;
import com.newtron.newtron_workforce_backend.common.logging.RequestContext;
import com.newtron.newtron_workforce_backend.common.response.ApiResponse;
import com.newtron.newtron_workforce_backend.common.response.ApiResponseFactory;
import com.newtron.newtron_workforce_backend.dto.WorkerAgreementDto;
import com.newtron.newtron_workforce_backend.entity.Agreement;
import com.newtron.newtron_workforce_backend.entity.Application;
import com.newtron.newtron_workforce_backend.enums.AgreementStatus;
import com.newtron.newtron_workforce_backend.repository.AgreementRepository;
import com.newtron.newtron_workforce_backend.repository.ApplicationRepository;
import com.newtron.newtron_workforce_backend.service.NotificationHelper;
import com.newtron.newtron_workforce_backend.entity.WorkOrder;
import com.newtron.newtron_workforce_backend.enums.WorkOrderStatus;
import com.newtron.newtron_workforce_backend.repository.WorkOrderRepository;
import com.newtron.newtron_workforce_backend.enums.NotificationCategory;
import com.newtron.newtron_workforce_backend.enums.NotificationPriority;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.time.Instant;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/worker/agreements")
@RequiredArgsConstructor
public class WorkerAgreementController {

    private final UserRepository userRepository;
    private final ApplicationRepository applicationRepository;
    private final AgreementRepository agreementRepository;
    private final NotificationHelper notificationHelper;
    private final WorkOrderRepository workOrderRepository;

    @GetMapping
    public ApiResponse<List<WorkerAgreementDto>> getAgreements(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        List<Application> applications = applicationRepository.findByWorkerId(currentUser.getId());
        List<WorkerAgreementDto> agreementDtos = new ArrayList<>();

        for (Application app : applications) {
            agreementRepository.findByApplicationId(app.getId()).ifPresent(agreement -> {
                if (agreement.getStatus() != AgreementStatus.DRAFT) {
                    agreementDtos.add(mapToDto(agreement));
                }
            });
        }

        // Sort to prefer pending agreements first, then by latest id
        agreementDtos.sort((a, b) -> {
            boolean aPending = AgreementStatus.PENDING_WORKER_ACCEPTANCE.name().equals(a.getStatus());
            boolean bPending = AgreementStatus.PENDING_WORKER_ACCEPTANCE.name().equals(b.getStatus());
            if (aPending && !bPending) return -1;
            if (!aPending && bPending) return 1;
            return b.getAgreementId().compareTo(a.getAgreementId());
        });

        return ApiResponseFactory.success(agreementDtos, "Worker agreements retrieved successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @GetMapping("/{id}")
    public ApiResponse<WorkerAgreementDto> getAgreement(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        Agreement agreement = agreementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AGREEMENT_NOT_FOUND", "Agreement not found"));

        if (!agreement.getWorker().getId().equals(currentUser.getId())) {
            throw new ValidationException("UNAUTHORIZED_ACCESS", "You do not own this agreement");
        }

        if (agreement.getStatus() == AgreementStatus.DRAFT) {
            throw new ResourceNotFoundException("AGREEMENT_NOT_FOUND", "Agreement not found");
        }

        return ApiResponseFactory.success(mapToDto(agreement), "Agreement retrieved successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @PostMapping("/{id}/accept")
    @Transactional
    public ApiResponse<WorkerAgreementDto> acceptAgreement(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        Agreement agreement = agreementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AGREEMENT_NOT_FOUND", "Agreement not found"));

        if (!agreement.getWorker().getId().equals(currentUser.getId())) {
            throw new ValidationException("UNAUTHORIZED_ACCESS", "You do not own this agreement");
        }

        if (agreement.getStatus() != AgreementStatus.PENDING_WORKER_ACCEPTANCE) {
            throw new ValidationException("INVALID_STATUS", "Agreement can only be accepted when pending worker acceptance");
        }

        agreement.setStatus(AgreementStatus.ACTIVE);
        agreement.setWorkerAcceptedAt(Instant.now());
        agreement = agreementRepository.save(agreement);

        Application application = agreement.getApplication();
        application.setStatus("Offered");
        applicationRepository.save(application);

        if (workOrderRepository.findByAgreementId(agreement.getId()).isEmpty()) {
            WorkOrder workOrder = WorkOrder.builder()
                    .agreement(agreement)
                    .company(agreement.getCompany())
                    .job(agreement.getJob())
                    .worker(agreement.getWorker())
                    .status(WorkOrderStatus.ACTIVE)
                    .engagementType(agreement.getEngagementType())
                    .dailyWage(agreement.getDailyWage())
                    .monthlySalary(agreement.getMonthlySalary())
                    .commissionRate(agreement.getCommissionRate())
                    .commissionPayer(agreement.getCommissionPayer())
                    .commissionAmount(agreement.getCommissionAmount())
                    .workOrderNumber("TEMP-" + agreement.getId())
                    .build();
            
            workOrder = workOrderRepository.save(workOrder);
            workOrder.setWorkOrderNumber("WO-" + workOrder.getId());
            workOrderRepository.save(workOrder);
        }

        if (agreement.getCompany() != null && agreement.getCompany().getOwner() != null) {
            String workerName = agreement.getWorkerNameSnapshot() != null ? agreement.getWorkerNameSnapshot() : "A worker";
            String jobTitle = agreement.getJobTitleSnapshot() != null ? agreement.getJobTitleSnapshot() : "a job";
            notificationHelper.sendNotification(
                    agreement.getCompany().getOwner(),
                    "Agreement Accepted",
                    workerName + " has accepted the agreement for " + jobTitle + ".",
                    NotificationCategory.OFFERS,
                    NotificationPriority.HIGH,
                    "APPLICATION_DETAILS:" + application.getId()
            );
        }

        return ApiResponseFactory.success(mapToDto(agreement), "Agreement accepted successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @PostMapping("/{id}/reject")
    @Transactional
    public ApiResponse<WorkerAgreementDto> rejectAgreement(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        Agreement agreement = agreementRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("AGREEMENT_NOT_FOUND", "Agreement not found"));

        if (!agreement.getWorker().getId().equals(currentUser.getId())) {
            throw new ValidationException("UNAUTHORIZED_ACCESS", "You do not own this agreement");
        }

        if (agreement.getStatus() != AgreementStatus.PENDING_WORKER_ACCEPTANCE) {
            throw new ValidationException("INVALID_STATUS", "Agreement can only be rejected when pending worker acceptance");
        }

        agreement.setStatus(AgreementStatus.REJECTED);
        agreement = agreementRepository.save(agreement);

        return ApiResponseFactory.success(mapToDto(agreement), "Agreement rejected successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    private WorkerAgreementDto mapToDto(Agreement agreement) {
        return WorkerAgreementDto.builder()
                .agreementId(agreement.getId())
                .applicationId(agreement.getApplication().getId())
                .jobId(agreement.getJob().getId())
                .companyId(agreement.getCompany().getId())
                .workerId(agreement.getWorker().getId())
                .workerNameSnapshot(agreement.getWorkerNameSnapshot())
                .companyNameSnapshot(agreement.getCompanyNameSnapshot())
                .jobTitleSnapshot(agreement.getJobTitleSnapshot())
                .jobDescriptionSnapshot(agreement.getJobDescriptionSnapshot())
                .workLocationSnapshot(agreement.getWorkLocationSnapshot())
                .primarySkillSnapshot(agreement.getPrimarySkillSnapshot())
                .engagementType(agreement.getEngagementType())
                .dailyWage(agreement.getDailyWage())
                .monthlySalary(agreement.getMonthlySalary())
                .commissionRate(agreement.getCommissionRate())
                .commissionPayer(agreement.getCommissionPayer())
                .commissionAmount(agreement.getCommissionAmount())
                .engagementDurationType(agreement.getEngagementDurationType())
                .durationValue(agreement.getDurationValue())
                .duration(agreement.getDuration())
                .status(agreement.getStatus() != null ? agreement.getStatus().name() : null)
                .clientAcceptedAt(agreement.getClientAcceptedAt())
                .workerAcceptedAt(agreement.getWorkerAcceptedAt())
                .effectiveAt(agreement.getEffectiveAt())
                .completedAt(agreement.getCompletedAt())
                .paymentResponsibility(agreement.getPaymentResponsibility())
                .paymentDueTerms(agreement.getPaymentDueTerms())
                .noticeDays(agreement.getNoticeDays())
                .cancellationTerms(agreement.getCancellationTerms())
                .clientCustomTerms(agreement.getClientCustomTerms())
                .agreementVersion(agreement.getAgreementVersion())
                .isLocked(agreement.getIsLocked())
                .build();
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
