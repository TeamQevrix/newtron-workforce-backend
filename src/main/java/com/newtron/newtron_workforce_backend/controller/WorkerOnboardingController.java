package com.newtron.newtron_workforce_backend.controller;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.common.exception.ResourceNotFoundException;
import com.newtron.newtron_workforce_backend.common.logging.RequestContext;
import com.newtron.newtron_workforce_backend.common.response.ApiResponse;
import com.newtron.newtron_workforce_backend.common.response.ApiResponseFactory;
import com.newtron.newtron_workforce_backend.entity.WorkerProfile;
import com.newtron.newtron_workforce_backend.enums.OnboardingStep;
import com.newtron.newtron_workforce_backend.repository.WorkerProfileRepository;
import com.newtron.newtron_workforce_backend.service.OnboardingProgressService;
import jakarta.servlet.http.HttpServletRequest;
import lombok.Builder;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/worker/onboarding")
@RequiredArgsConstructor
public class WorkerOnboardingController {

    private final UserRepository userRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final OnboardingProgressService onboardingProgressService;

    @GetMapping("/status")
    public ApiResponse<OnboardingStatusResponse> getOnboardingStatus(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);
        
        WorkerProfile profile = workerProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("PROFILE_NOT_FOUND", "Worker profile not found"));

        String status = "PROCESSING";
        String currentStep = profile.getCurrentStep().name();
        int progress = (int) onboardingProgressService.calculateCompletion(profile);
        String nextScreen = "ONBOARDING";

        if (Boolean.TRUE.equals(profile.getIsCompleted()) || profile.getCurrentStep() == OnboardingStep.COMPLETED) {
            status = "COMPLETED";
            currentStep = "COMPLETED";
            progress = 100;
            nextScreen = "DASHBOARD";
        }

        OnboardingStatusResponse response = OnboardingStatusResponse.builder()
                .status(status)
                .currentStep(currentStep)
                .progress(progress)
                .nextScreen(nextScreen)
                .build();

        return ApiResponseFactory.success(response, "Onboarding status retrieved successfully",
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
        return attr instanceof Long ? (Long) attr : System.currentTimeMillis();
    }

    @Getter
    @Builder
    public static class OnboardingStatusResponse {
        private final String status;
        private final String currentStep;
        private final int progress;
        private final String nextScreen;
    }
}
