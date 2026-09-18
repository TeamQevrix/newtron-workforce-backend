package com.newtron.newtron_workforce_backend.controller;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.common.exception.ResourceNotFoundException;
import com.newtron.newtron_workforce_backend.common.logging.RequestContext;
import com.newtron.newtron_workforce_backend.common.response.ApiResponse;
import com.newtron.newtron_workforce_backend.common.response.ApiResponseFactory;
import com.newtron.newtron_workforce_backend.dto.WorkerSummaryDto;
import com.newtron.newtron_workforce_backend.dto.WorkerAvailabilityDto;
import com.newtron.newtron_workforce_backend.dto.WorkerQuickActionsDto;
import com.newtron.newtron_workforce_backend.dto.WorkerStatsDto;
import com.newtron.newtron_workforce_backend.dto.WorkerActivityDto;
import com.newtron.newtron_workforce_backend.dto.JobRecommendationDto;
import com.newtron.newtron_workforce_backend.dto.WorkerDashboardResponse;
import java.util.List;
import com.newtron.newtron_workforce_backend.service.WorkerDashboardService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/worker/dashboard")
@RequiredArgsConstructor
public class WorkerDashboardController {

    private final WorkerDashboardService workerDashboardService;
    private final UserRepository userRepository;

    @GetMapping("/summary")
    public ApiResponse<WorkerSummaryDto> getWorkerSummary(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        WorkerSummaryDto response = workerDashboardService.getWorkerSummary(currentUser);

        return ApiResponseFactory.success(response, "Worker summary retrieved successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @GetMapping("/availability")
    public ApiResponse<WorkerAvailabilityDto> getWorkerAvailability(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        WorkerAvailabilityDto response = workerDashboardService.getWorkerAvailability(currentUser);

        return ApiResponseFactory.success(response, "Worker availability retrieved successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @org.springframework.web.bind.annotation.PatchMapping("/availability")
    public ApiResponse<WorkerAvailabilityDto> updateWorkerAvailability(
            @jakarta.validation.Valid @org.springframework.web.bind.annotation.RequestBody WorkerAvailabilityDto request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        WorkerAvailabilityDto response = workerDashboardService.updateWorkerAvailability(currentUser, request);

        return ApiResponseFactory.success(response, "Worker availability updated successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @GetMapping("/quick-actions")
    public ApiResponse<com.newtron.newtron_workforce_backend.dto.WorkerQuickActionsDto> getQuickActions(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        com.newtron.newtron_workforce_backend.dto.WorkerQuickActionsDto response = workerDashboardService.getQuickActions(currentUser);

        return ApiResponseFactory.success(response, "Quick actions retrieved successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @GetMapping("/stats")
    public ApiResponse<com.newtron.newtron_workforce_backend.dto.WorkerStatsDto> getWorkerStats(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        com.newtron.newtron_workforce_backend.dto.WorkerStatsDto response = workerDashboardService.getWorkerStats(currentUser);

        return ApiResponseFactory.success(response, "Worker dashboard stats retrieved successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @GetMapping("/recent-activities")
    public ApiResponse<List<WorkerActivityDto>> getRecentActivities(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        List<WorkerActivityDto> response = workerDashboardService.getRecentActivities(currentUser);

        return ApiResponseFactory.success(response, "Recent activities retrieved successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @GetMapping("/recommended-jobs")
    public ApiResponse<List<JobRecommendationDto>> getRecommendedJobs(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        List<JobRecommendationDto> response = workerDashboardService.getRecommendedJobs(currentUser);

        return ApiResponseFactory.success(response, "Recommended jobs retrieved successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @GetMapping
    public ApiResponse<WorkerDashboardResponse> getWorkerDashboard(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        WorkerDashboardResponse response = workerDashboardService.getWorkerDashboard(currentUser);

        return ApiResponseFactory.success(response, "Worker dashboard retrieved successfully",
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
