package com.newtron.newtron_workforce_backend.controller;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.common.exception.ResourceNotFoundException;
import com.newtron.newtron_workforce_backend.common.exception.ValidationException;
import com.newtron.newtron_workforce_backend.common.logging.RequestContext;
import com.newtron.newtron_workforce_backend.common.response.ApiResponse;
import com.newtron.newtron_workforce_backend.common.response.ApiResponseFactory;
import com.newtron.newtron_workforce_backend.entity.Company;
import com.newtron.newtron_workforce_backend.entity.WorkOrder;
import com.newtron.newtron_workforce_backend.entity.WorkerProfile;
import com.newtron.newtron_workforce_backend.repository.CompanyRepository;
import com.newtron.newtron_workforce_backend.repository.WorkOrderRepository;
import com.newtron.newtron_workforce_backend.repository.WorkerProfileRepository;
import com.newtron.newtron_workforce_backend.repository.AgreementRepository;
import com.newtron.newtron_workforce_backend.enums.WorkOrderStatus;
import com.newtron.newtron_workforce_backend.enums.AgreementStatus;
import com.newtron.newtron_workforce_backend.entity.Agreement;
import com.newtron.newtron_workforce_backend.dto.WorkOrderCompleteRequestDto;
import com.newtron.newtron_workforce_backend.dto.WorkOrderCancelRequestDto;
import com.newtron.newtron_workforce_backend.dto.AttendanceRequestDto;
import com.newtron.newtron_workforce_backend.dto.AttendanceResponseDto;
import com.newtron.newtron_workforce_backend.entity.Attendance;
import com.newtron.newtron_workforce_backend.repository.AttendanceRepository;
import org.springframework.transaction.annotation.Transactional;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/recruiter/work-orders")
@RequiredArgsConstructor
public class RecruiterWorkOrderController {

    private final WorkOrderRepository workOrderRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final AgreementRepository agreementRepository;
    private final AttendanceRepository attendanceRepository;

    @GetMapping
    public ApiResponse<List<Map<String, Object>>> getWorkOrders(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        Company company = companyRepository.findByOwnerId(currentUser.getId())
                .orElseThrow(() -> new ValidationException("COMPANY_REQUIRED", "Company profile onboarding is not completed."));

        List<WorkOrder> workOrders = workOrderRepository.findByCompanyIdOrderByIdDesc(company.getId());

        List<Map<String, Object>> response = workOrders.stream().map(this::mapToListResponse).collect(Collectors.toList());

        return ApiResponseFactory.success(response, "Work orders retrieved successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @GetMapping("/{id}")
    public ApiResponse<Map<String, Object>> getWorkOrder(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        Company company = companyRepository.findByOwnerId(currentUser.getId())
                .orElseThrow(() -> new ValidationException("COMPANY_REQUIRED", "Company profile onboarding is not completed."));

        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WORK_ORDER_NOT_FOUND", "Work order not found"));

        if (workOrder.getCompany() == null || !workOrder.getCompany().getId().equals(company.getId())) {
            throw new ValidationException("UNAUTHORIZED_ACCESS", "You do not own this work order");
        }

        Map<String, Object> response = mapToDetailResponse(workOrder);

        return ApiResponseFactory.success(response, "Work order retrieved successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @PostMapping("/{id}/complete")
    @Transactional
    public ApiResponse<Map<String, Object>> completeWorkOrder(
            @PathVariable("id") Long id,
            @RequestBody WorkOrderCompleteRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        if (request.getActualEndDate() == null) {
            throw new ValidationException("VALIDATION_ERROR", "actualEndDate is required");
        }

        Company company = companyRepository.findByOwnerId(currentUser.getId())
                .orElseThrow(() -> new ValidationException("COMPANY_REQUIRED", "Company profile onboarding is not completed."));

        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WORK_ORDER_NOT_FOUND", "Work order not found"));

        if (workOrder.getCompany() == null || !workOrder.getCompany().getId().equals(company.getId())) {
            throw new ValidationException("UNAUTHORIZED_ACCESS", "You do not own this work order");
        }

        if (workOrder.getStatus() != WorkOrderStatus.ACTIVE) {
            throw new ValidationException("INVALID_STATUS", "Work order must be ACTIVE to be completed");
        }

        if (workOrder.getActualStartDate() != null && request.getActualEndDate().isBefore(workOrder.getActualStartDate())) {
            throw new ValidationException("VALIDATION_ERROR", "actualEndDate cannot be before actualStartDate");
        }

        Agreement agreement = workOrder.getAgreement();
        if (agreement == null) {
            throw new ResourceNotFoundException("AGREEMENT_NOT_FOUND", "Associated agreement not found");
        }

        if (agreement.getStatus() != AgreementStatus.ACTIVE) {
            throw new ValidationException("INVALID_STATUS", "Associated agreement must be ACTIVE to be completed");
        }

        workOrder.setStatus(WorkOrderStatus.COMPLETED);
        workOrder.setActualEndDate(request.getActualEndDate());
        workOrder = workOrderRepository.save(workOrder);

        agreement.setStatus(AgreementStatus.COMPLETED);
        agreement = agreementRepository.save(agreement);

        Map<String, Object> response = new HashMap<>();
        response.put("workOrderId", workOrder.getId());
        response.put("workOrderNumber", workOrder.getWorkOrderNumber());
        response.put("workOrderStatus", workOrder.getStatus().name());
        response.put("agreementId", agreement.getId());
        response.put("agreementStatus", agreement.getStatus().name());
        response.put("actualEndDate", workOrder.getActualEndDate());

        return ApiResponseFactory.success(response, "Work order completed successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @PostMapping("/{id}/cancel")
    @Transactional
    public ApiResponse<Map<String, Object>> cancelWorkOrder(
            @PathVariable("id") Long id,
            @RequestBody WorkOrderCancelRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        if (request.getActualEndDate() == null) {
            throw new ValidationException("VALIDATION_ERROR", "actualEndDate is required");
        }

        if (request.getCancellationReason() == null || request.getCancellationReason().trim().isEmpty()) {
            throw new ValidationException("VALIDATION_ERROR", "cancellationReason is required and cannot be blank");
        }

        if (request.getCancellationReason().length() > 1000) {
            throw new ValidationException("VALIDATION_ERROR", "cancellationReason exceeds maximum length of 1000 characters");
        }

        Company company = companyRepository.findByOwnerId(currentUser.getId())
                .orElseThrow(() -> new ValidationException("COMPANY_REQUIRED", "Company profile onboarding is not completed."));

        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WORK_ORDER_NOT_FOUND", "Work order not found"));

        if (workOrder.getCompany() == null || !workOrder.getCompany().getId().equals(company.getId())) {
            throw new ValidationException("UNAUTHORIZED_ACCESS", "You do not own this work order");
        }

        if (workOrder.getStatus() != WorkOrderStatus.ACTIVE) {
            throw new ValidationException("INVALID_STATUS", "Work order must be ACTIVE to be cancelled");
        }

        if (workOrder.getActualStartDate() != null && request.getActualEndDate().isBefore(workOrder.getActualStartDate())) {
            throw new ValidationException("VALIDATION_ERROR", "actualEndDate cannot be before actualStartDate");
        }

        Agreement agreement = workOrder.getAgreement();
        if (agreement == null) {
            throw new ResourceNotFoundException("AGREEMENT_NOT_FOUND", "Associated agreement not found");
        }

        if (agreement.getStatus() != AgreementStatus.ACTIVE) {
            throw new ValidationException("INVALID_STATUS", "Associated agreement must be ACTIVE to be terminated");
        }

        workOrder.setStatus(WorkOrderStatus.CANCELLED);
        workOrder.setActualEndDate(request.getActualEndDate());
        workOrder.setCancellationReason(request.getCancellationReason());
        workOrder = workOrderRepository.save(workOrder);

        agreement.setStatus(AgreementStatus.TERMINATED);
        agreement = agreementRepository.save(agreement);

        Map<String, Object> response = new HashMap<>();
        response.put("workOrderId", workOrder.getId());
        response.put("workOrderNumber", workOrder.getWorkOrderNumber());
        response.put("workOrderStatus", workOrder.getStatus().name());
        response.put("agreementId", agreement.getId());
        response.put("agreementStatus", agreement.getStatus().name());
        response.put("actualEndDate", workOrder.getActualEndDate());
        response.put("cancellationReason", workOrder.getCancellationReason());

        return ApiResponseFactory.success(response, "Work order cancelled successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @PostMapping("/{id}/attendance")
    @Transactional
    public ApiResponse<AttendanceResponseDto> markAttendance(
            @PathVariable("id") Long id,
            @Valid @RequestBody AttendanceRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        Company company = companyRepository.findByOwnerId(currentUser.getId())
                .orElseThrow(() -> new ValidationException("COMPANY_REQUIRED", "Company profile onboarding is not completed."));

        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WORK_ORDER_NOT_FOUND", "Work order not found"));

        if (workOrder.getCompany() == null || !workOrder.getCompany().getId().equals(company.getId())) {
            throw new ValidationException("UNAUTHORIZED_ACCESS", "You do not own this work order");
        }

        if (workOrder.getStatus() != WorkOrderStatus.ACTIVE) {
            throw new ValidationException("INVALID_STATUS", "Work order must be ACTIVE to mark attendance");
        }

        if (attendanceRepository.existsByWorkOrderIdAndAttendanceDate(workOrder.getId(), request.getAttendanceDate())) {
            throw new ValidationException("VALIDATION_ERROR", "Attendance already exists for this date");
        }

        Attendance attendance = Attendance.builder()
                .workOrder(workOrder)
                .attendanceDate(request.getAttendanceDate())
                .status(request.getStatus())
                .remark(request.getRemark())
                .markedBy(currentUser)
                .build();

        attendance = attendanceRepository.save(attendance);

        AttendanceResponseDto response = AttendanceResponseDto.builder()
                .id(attendance.getId())
                .workOrderId(workOrder.getId())
                .attendanceDate(attendance.getAttendanceDate())
                .status(attendance.getStatus())
                .remark(attendance.getRemark())
                .createdAt(attendance.getCreatedAt())
                .updatedAt(attendance.getUpdatedAt())
                .build();

        return ApiResponseFactory.success(response, "Attendance marked successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @GetMapping("/{id}/attendance")
    public ApiResponse<List<AttendanceResponseDto>> getAttendance(
            @PathVariable("id") Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        Company company = companyRepository.findByOwnerId(currentUser.getId())
                .orElseThrow(() -> new ValidationException("COMPANY_REQUIRED", "Company profile onboarding is not completed."));

        WorkOrder workOrder = workOrderRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("WORK_ORDER_NOT_FOUND", "Work order not found"));

        if (workOrder.getCompany() == null || !workOrder.getCompany().getId().equals(company.getId())) {
            throw new ValidationException("UNAUTHORIZED_ACCESS", "You do not own this work order");
        }

        List<Attendance> attendances = attendanceRepository.findByWorkOrderIdOrderByAttendanceDateDesc(workOrder.getId());
        List<AttendanceResponseDto> response = attendances.stream().map(a -> AttendanceResponseDto.builder()
                .id(a.getId())
                .workOrderId(a.getWorkOrder().getId())
                .attendanceDate(a.getAttendanceDate())
                .status(a.getStatus())
                .remark(a.getRemark())
                .createdAt(a.getCreatedAt())
                .updatedAt(a.getUpdatedAt())
                .build()).collect(Collectors.toList());

        return ApiResponseFactory.success(response, "Attendance records retrieved successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }


    private Map<String, Object> mapToListResponse(WorkOrder workOrder) {
        Map<String, Object> response = new HashMap<>();
        response.put("id", workOrder.getId());
        response.put("workOrderNumber", workOrder.getWorkOrderNumber());
        
        if (workOrder.getJob() != null) {
            response.put("jobTitle", workOrder.getJob().getTitle());
        }

        Long workerProfileId = null;
        String workerName = null;
        if (workOrder.getWorker() != null) {
            workerName = workOrder.getWorker().getFullName();
            WorkerProfile workerProfile = workerProfileRepository.findByUserId(workOrder.getWorker().getId()).orElse(null);
            if (workerProfile != null) {
                workerProfileId = workerProfile.getId();
                if (workerProfile.getFullName() != null) {
                    workerName = workerProfile.getFullName();
                }
            }
        }
        
        response.put("workerName", workerName);
        response.put("workerProfileId", workerProfileId);
        
        response.put("engagementType", workOrder.getEngagementType());
        response.put("dailyWage", workOrder.getDailyWage());
        response.put("monthlySalary", workOrder.getMonthlySalary());
        response.put("commissionRate", workOrder.getCommissionRate());
        response.put("commissionAmount", workOrder.getCommissionAmount());
        response.put("commissionPayer", workOrder.getCommissionPayer());
        
        response.put("status", workOrder.getStatus() != null ? workOrder.getStatus().name() : null);
        response.put("expectedStartDate", workOrder.getExpectedStartDate());
        response.put("expectedEndDate", workOrder.getExpectedEndDate());
        
        return response;
    }

    private Map<String, Object> mapToDetailResponse(WorkOrder workOrder) {
        Map<String, Object> response = new HashMap<>();
        
        // Identity
        response.put("id", workOrder.getId());
        response.put("workOrderNumber", workOrder.getWorkOrderNumber());
        response.put("status", workOrder.getStatus() != null ? workOrder.getStatus().name() : null);
        
        // Relationships
        if (workOrder.getAgreement() != null) {
            response.put("agreementId", workOrder.getAgreement().getId());
        }
        
        if (workOrder.getCompany() != null) {
            response.put("companyId", workOrder.getCompany().getId());
            response.put("companyName", workOrder.getCompany().getCompanyName());
        }
        
        if (workOrder.getJob() != null) {
            response.put("jobId", workOrder.getJob().getId());
            response.put("jobTitle", workOrder.getJob().getTitle());
        }
        
        Long workerProfileId = null;
        String workerName = null;
        if (workOrder.getWorker() != null) {
            workerName = workOrder.getWorker().getFullName();
            WorkerProfile workerProfile = workerProfileRepository.findByUserId(workOrder.getWorker().getId()).orElse(null);
            if (workerProfile != null) {
                workerProfileId = workerProfile.getId();
                if (workerProfile.getFullName() != null) {
                    workerName = workerProfile.getFullName();
                }
            }
        }
        response.put("workerProfileId", workerProfileId);
        response.put("workerName", workerName);

        // Commercial
        response.put("engagementType", workOrder.getEngagementType());
        response.put("dailyWage", workOrder.getDailyWage());
        response.put("monthlySalary", workOrder.getMonthlySalary());
        response.put("commissionRate", workOrder.getCommissionRate());
        response.put("commissionAmount", workOrder.getCommissionAmount());
        response.put("commissionPayer", workOrder.getCommissionPayer());

        // Dates
        response.put("expectedStartDate", workOrder.getExpectedStartDate());
        response.put("expectedEndDate", workOrder.getExpectedEndDate());
        response.put("actualStartDate", workOrder.getActualStartDate());
        response.put("actualEndDate", workOrder.getActualEndDate());

        // Other
        response.put("cancellationReason", workOrder.getCancellationReason());

        return response;
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
