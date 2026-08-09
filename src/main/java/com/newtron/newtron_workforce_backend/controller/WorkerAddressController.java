package com.newtron.newtron_workforce_backend.controller;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.common.exception.ResourceNotFoundException;
import com.newtron.newtron_workforce_backend.common.logging.RequestContext;
import com.newtron.newtron_workforce_backend.common.response.ApiResponse;
import com.newtron.newtron_workforce_backend.common.response.ApiResponseFactory;
import com.newtron.newtron_workforce_backend.dto.WorkerAddressRequest;
import com.newtron.newtron_workforce_backend.dto.WorkerAddressResponse;
import com.newtron.newtron_workforce_backend.service.WorkerAddressService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/worker/profile/address")
@RequiredArgsConstructor
public class WorkerAddressController {

    private final WorkerAddressService service;
    private final UserRepository userRepository;

    @GetMapping
    public ApiResponse<WorkerAddressResponse> getAddress(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);
        
        WorkerAddressResponse response = service.getAddress(currentUser);
        
        return ApiResponseFactory.success(response, "Address details retrieved successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @PostMapping
    public ApiResponse<WorkerAddressResponse> createAddress(
            @Valid @RequestBody WorkerAddressRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);
        
        WorkerAddressResponse response = service.createAddress(request, currentUser);
        
        return ApiResponseFactory.success(response, "Address details created successfully",
                RequestContext.getRequestId(), httpServletRequest.getRequestURI(), startTime);
    }

    @PutMapping
    public ApiResponse<WorkerAddressResponse> updateAddress(
            @Valid @RequestBody WorkerAddressRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);
        
        WorkerAddressResponse response = service.updateAddress(request, currentUser);
        
        return ApiResponseFactory.success(response, "Address details updated successfully",
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
