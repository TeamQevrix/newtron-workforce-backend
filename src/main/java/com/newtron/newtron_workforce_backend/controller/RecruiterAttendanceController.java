package com.newtron.newtron_workforce_backend.controller;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.common.exception.ResourceNotFoundException;
import com.newtron.newtron_workforce_backend.common.exception.ValidationException;
import com.newtron.newtron_workforce_backend.common.logging.RequestContext;
import com.newtron.newtron_workforce_backend.common.response.ApiResponse;
import com.newtron.newtron_workforce_backend.common.response.ApiResponseFactory;
import com.newtron.newtron_workforce_backend.common.response.PageResponse;
import com.newtron.newtron_workforce_backend.dto.CentralAttendanceResponseDto;
import com.newtron.newtron_workforce_backend.entity.Company;
import com.newtron.newtron_workforce_backend.repository.AttendanceRepository;
import com.newtron.newtron_workforce_backend.repository.CompanyRepository;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RequestPart;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;
import com.newtron.newtron_workforce_backend.service.AttendanceBulkService;
import com.newtron.newtron_workforce_backend.dto.AttendanceBulkResponseDto;

import java.time.LocalDate;

@RestController
@RequestMapping("/api/v1/recruiter/attendance")
@RequiredArgsConstructor
public class RecruiterAttendanceController {

    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final CompanyRepository companyRepository;
    private final AttendanceBulkService attendanceBulkService;

    @GetMapping
    public ApiResponse<PageResponse<CentralAttendanceResponseDto>> getCentralAttendance(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        Company company = companyRepository.findByOwnerId(currentUser.getId())
                .orElseThrow(() -> new ValidationException("COMPANY_REQUIRED", "Company profile onboarding is not completed."));

        Page<CentralAttendanceResponseDto> attendancePage = attendanceRepository.findCentralAttendanceByCompanyId(
                company.getId(), date, PageRequest.of(page, size));

        PageResponse<CentralAttendanceResponseDto> pagedData = PageResponse.<CentralAttendanceResponseDto>builder()
                .content(attendancePage.getContent())
                .page(attendancePage.getNumber())
                .size(attendancePage.getSize())
                .totalElements(attendancePage.getTotalElements())
                .totalPages(attendancePage.getTotalPages())
                .first(attendancePage.isFirst())
                .last(attendancePage.isLast())
                .empty(attendancePage.isEmpty())
                .build();

        return ApiResponseFactory.paged(pagedData, "Attendance records retrieved successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @PostMapping(value = "/bulk", consumes = {"multipart/form-data"})
    public ApiResponse<AttendanceBulkResponseDto> importBulkAttendance(
            @RequestPart("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        AttendanceBulkResponseDto response = attendanceBulkService.importAttendance(file, currentUser);

        if (response.getErrors() != null && !response.getErrors().isEmpty()) {
            return com.newtron.newtron_workforce_backend.common.response.ApiResponse.<AttendanceBulkResponseDto>builder()
                    .success(false)
                    .message("Attendance import validation failed.")
                    .data(response)
                    .meta(com.newtron.newtron_workforce_backend.common.response.MetaInfo.builder()
                            .apiVersion("v1.0")
                            .timestamp(java.time.Instant.now())
                            .requestId(RequestContext.getRequestId())
                            .path(httpServletRequest.getRequestURI())
                            .processingTimeMs(System.currentTimeMillis() - startTime)
                            .build())
                    .build();
        }

        return ApiResponseFactory.success(response, "Attendance imported successfully.",
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
