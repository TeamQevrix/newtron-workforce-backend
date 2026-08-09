package com.newtron.newtron_workforce_backend.controller;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.common.exception.ResourceNotFoundException;
import com.newtron.newtron_workforce_backend.common.logging.RequestContext;
import com.newtron.newtron_workforce_backend.common.response.ApiResponse;
import com.newtron.newtron_workforce_backend.common.response.ApiResponseFactory;
import com.newtron.newtron_workforce_backend.dto.WorkerProfessionalDetailsRequest;
import com.newtron.newtron_workforce_backend.dto.WorkerProfessionalDetailsResponse;
import com.newtron.newtron_workforce_backend.service.WorkerProfessionalDetailsService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/worker/profile/professional")
@RequiredArgsConstructor
public class WorkerProfessionalDetailsController {

    private final WorkerProfessionalDetailsService service;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<WorkerProfessionalDetailsResponse> getProfessionalDetails(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);
        
        WorkerProfessionalDetailsResponse response = service.getProfessionalDetails(currentUser);
        
        return ApiResponseFactory.success(response, "Professional details retrieved successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @PostMapping
    public ApiResponse<WorkerProfessionalDetailsResponse> createProfessionalDetails(
            @Valid @RequestBody WorkerProfessionalDetailsRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);
        
        WorkerProfessionalDetailsResponse response = service.createProfessionalDetails(request, currentUser);
        
        return ApiResponseFactory.success(response, "Professional details created successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @PutMapping
    public ApiResponse<WorkerProfessionalDetailsResponse> updateProfessionalDetails(
            @Valid @RequestBody WorkerProfessionalDetailsRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);
        
        WorkerProfessionalDetailsResponse response = service.updateProfessionalDetails(request, currentUser);
        
        return ApiResponseFactory.success(response, "Professional details updated successfully",
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
