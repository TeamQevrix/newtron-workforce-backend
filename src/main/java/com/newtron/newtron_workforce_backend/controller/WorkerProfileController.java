package com.newtron.newtron_workforce_backend.controller;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.common.exception.ResourceNotFoundException;
import com.newtron.newtron_workforce_backend.common.logging.RequestContext;
import com.newtron.newtron_workforce_backend.common.response.ApiResponse;
import com.newtron.newtron_workforce_backend.common.response.ApiResponseFactory;
import com.newtron.newtron_workforce_backend.dto.WorkerBasicProfileRequest;
import com.newtron.newtron_workforce_backend.dto.WorkerBasicProfileResponse;
import com.newtron.newtron_workforce_backend.service.WorkerProfileService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/worker/profile")
@RequiredArgsConstructor
public class WorkerProfileController {

    private final WorkerProfileService workerProfileService;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<WorkerBasicProfileResponse> getProfile(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);
        
        WorkerBasicProfileResponse response = workerProfileService.getProfile(currentUser);
        
        return ApiResponseFactory.success(response, "Worker profile retrieved successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @PostMapping
    public ApiResponse<WorkerBasicProfileResponse> saveProfile(
            @Valid @RequestBody WorkerBasicProfileRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);
        
        WorkerBasicProfileResponse response = workerProfileService.saveProfile(request, currentUser);
        
        return ApiResponseFactory.success(response, "Worker profile created successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @PutMapping
    public ApiResponse<WorkerBasicProfileResponse> updateProfile(
            @Valid @RequestBody WorkerBasicProfileRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);
        
        WorkerBasicProfileResponse response = workerProfileService.updateProfile(request, currentUser);
        
        return ApiResponseFactory.success(response, "Worker profile updated successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @PostMapping("/photo")
    public ApiResponse<WorkerBasicProfileResponse> uploadPhoto(
            @RequestParam("file") MultipartFile file,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);
        
        WorkerBasicProfileResponse response = workerProfileService.uploadProfilePhoto(file, currentUser);
        
        return ApiResponseFactory.success(response, "Profile photo uploaded successfully",
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
