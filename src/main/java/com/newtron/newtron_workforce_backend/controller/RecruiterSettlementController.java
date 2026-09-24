package com.newtron.newtron_workforce_backend.controller;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.common.exception.ResourceNotFoundException;
import com.newtron.newtron_workforce_backend.common.exception.ValidationException;
import com.newtron.newtron_workforce_backend.common.logging.RequestContext;
import com.newtron.newtron_workforce_backend.common.response.ApiResponse;
import com.newtron.newtron_workforce_backend.common.response.ApiResponseFactory;
import com.newtron.newtron_workforce_backend.common.response.PageResponse;
import com.newtron.newtron_workforce_backend.dto.CalculateSettlementRequestDto;
import com.newtron.newtron_workforce_backend.dto.SettlementResponseDto;
import com.newtron.newtron_workforce_backend.entity.Company;
import com.newtron.newtron_workforce_backend.entity.Settlement;
import com.newtron.newtron_workforce_backend.entity.WorkOrder;
import com.newtron.newtron_workforce_backend.entity.WorkerProfile;
import com.newtron.newtron_workforce_backend.repository.CompanyRepository;
import com.newtron.newtron_workforce_backend.repository.SettlementRepository;
import com.newtron.newtron_workforce_backend.repository.WorkOrderRepository;
import com.newtron.newtron_workforce_backend.repository.WorkerProfileRepository;
import com.newtron.newtron_workforce_backend.service.SettlementService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/recruiter/settlements")
@RequiredArgsConstructor
public class RecruiterSettlementController {

    private final SettlementService settlementService;
    private final SettlementRepository settlementRepository;
    private final WorkOrderRepository workOrderRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final WorkerProfileRepository workerProfileRepository;

    @PostMapping("/calculate")
    public ApiResponse<SettlementResponseDto> calculateSettlement(
            @Valid @RequestBody CalculateSettlementRequestDto request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {

        long startTime = getStartTime(httpServletRequest);
        Company company = getAuthenticatedCompany(userDetails);

        WorkOrder workOrder = workOrderRepository.findById(request.getWorkOrderId())
                .orElseThrow(() -> new ResourceNotFoundException("WORK_ORDER_NOT_FOUND", "WorkOrder not found"));

        if (!workOrder.getCompany().getId().equals(company.getId())) {
            throw new ResourceNotFoundException("WORK_ORDER_NOT_FOUND", "WorkOrder not found");
        }

        try {
            Settlement settlement = settlementService.calculateAndSaveSettlement(
                    workOrder.getId(), request.getSettlementMonth(), request.getSettlementYear());
            return ApiResponseFactory.success(
                    mapToResponse(settlement),
                    "Settlement calculated successfully",
                    RequestContext.getRequestId(),
                    httpServletRequest.getRequestURI(),
                    startTime
            );
        } catch (IllegalArgumentException | IllegalStateException e) {
            throw new ValidationException("SETTLEMENT_VALIDATION_ERROR", e.getMessage());
        }
    }

    @GetMapping
    public ApiResponse<PageResponse<SettlementResponseDto>> listSettlements(
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) Long workOrderId,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {

        long startTime = getStartTime(httpServletRequest);
        Company company = getAuthenticatedCompany(userDetails);

        Page<Settlement> settlementPage = settlementRepository.findByCompanyIdWithFilters(
                company.getId(), month, year, workOrderId, PageRequest.of(page, size));

        PageResponse<SettlementResponseDto> pagedData = PageResponse.<SettlementResponseDto>builder()
                .content(settlementPage.getContent().stream().map(this::mapToResponse).toList())
                .page(settlementPage.getNumber())
                .size(settlementPage.getSize())
                .totalElements(settlementPage.getTotalElements())
                .totalPages(settlementPage.getTotalPages())
                .first(settlementPage.isFirst())
                .last(settlementPage.isLast())
                .empty(settlementPage.isEmpty())
                .build();

        return ApiResponseFactory.paged(
                pagedData,
                "Settlements retrieved successfully",
                RequestContext.getRequestId(),
                httpServletRequest.getRequestURI(),
                startTime
        );
    }

    @GetMapping("/{id}")
    public ApiResponse<SettlementResponseDto> getSettlementDetail(
            @PathVariable Long id,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {

        long startTime = getStartTime(httpServletRequest);
        Company company = getAuthenticatedCompany(userDetails);

        Settlement settlement = settlementRepository.findByIdAndCompanyId(id, company.getId())
                .orElseThrow(() -> new ResourceNotFoundException("SETTLEMENT_NOT_FOUND", "Settlement not found"));

        return ApiResponseFactory.success(
                mapToResponse(settlement),
                "Settlement retrieved successfully",
                RequestContext.getRequestId(),
                httpServletRequest.getRequestURI(),
                startTime
        );
    }

    private SettlementResponseDto mapToResponse(Settlement settlement) {
        WorkerProfile workerProfile = workerProfileRepository.findByUserId(settlement.getWorkOrder().getWorker().getId())
                .orElse(null);

        return SettlementResponseDto.builder()
                .id(settlement.getId())
                .workOrderId(settlement.getWorkOrder().getId())
                .workOrderNumber(settlement.getWorkOrder().getWorkOrderNumber())
                .settlementMonth(settlement.getSettlementMonth())
                .settlementYear(settlement.getSettlementYear())
                .workerId(workerProfile != null ? workerProfile.getId() : null)
                .workerName(workerProfile != null ? workerProfile.getFullName() : null)
                .workerPayableAmount(settlement.getWorkerPayableAmount())
                .newtronCommissionAmount(settlement.getNewtronCommissionAmount())
                .totalClientPayableAmount(settlement.getTotalClientPayableAmount())
                .createdAt(settlement.getCreatedAt())
                .updatedAt(settlement.getUpdatedAt())
                .build();
    }

    private Company getAuthenticatedCompany(UserDetails userDetails) {
        if (userDetails == null) {
            throw new ResourceNotFoundException("NOT_AUTHENTICATED", "Not authenticated");
        }
        User user = userRepository.findByMobile(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));

        return companyRepository.findByOwnerId(user.getId())
                .orElseThrow(() -> new ValidationException("COMPANY_REQUIRED", "Company profile onboarding is not completed."));
    }

    private long getStartTime(HttpServletRequest request) {
        Object attr = request.getAttribute("startTimeMs");
        if (attr instanceof Long) {
            return (Long) attr;
        }
        return System.currentTimeMillis();
    }
}
