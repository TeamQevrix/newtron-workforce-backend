package com.newtron.newtron_workforce_backend.interceptor;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.common.response.ApiError;
import com.newtron.newtron_workforce_backend.common.response.ApiResponse;
import com.newtron.newtron_workforce_backend.common.response.ApiResponseFactory;
import com.newtron.newtron_workforce_backend.entity.WorkerMembership;
import com.newtron.newtron_workforce_backend.entity.WorkerProfile;
import com.newtron.newtron_workforce_backend.repository.WorkerMembershipRepository;
import com.newtron.newtron_workforce_backend.repository.WorkerProfileRepository;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

@Component
@Slf4j
@RequiredArgsConstructor
public class WorkerMembershipInterceptor implements HandlerInterceptor {

    private final UserRepository userRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final WorkerMembershipRepository workerMembershipRepository;
    private final ObjectMapper objectMapper;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) throws Exception {
        Authentication authentication = SecurityContextHolder.getContext().getAuthentication();

        if (authentication == null || !authentication.isAuthenticated() || "anonymousUser".equals(authentication.getPrincipal())) {
            return true;
        }

        boolean isWorker = authentication.getAuthorities().stream()
                .anyMatch(a -> "ROLE_WORKER".equals(a.getAuthority()) || "WORKER".equals(a.getAuthority()));

        if (!isWorker) {
            return true;
        }

        Object principal = authentication.getPrincipal();
        String mobile;
        if (principal instanceof UserDetails) {
            mobile = ((UserDetails) principal).getUsername();
        } else {
            return true;
        }

        Optional<User> userOpt = userRepository.findByMobile(mobile);
        if (userOpt.isEmpty()) {
            return true;
        }
        User user = userOpt.get();

        Optional<WorkerProfile> profileOpt = workerProfileRepository.findByUserId(user.getId());
        if (profileOpt.isEmpty()) {
            return true;
        }
        WorkerProfile profile = profileOpt.get();

        Optional<WorkerMembership> membershipOpt = workerMembershipRepository.findByWorkerProfileId(profile.getId());
        String uri = request.getRequestURI();
        boolean isTeamApi = uri != null && uri.startsWith("/api/v1/worker/teams");
        boolean isTeamRegistration = isTeamApi && "POST".equalsIgnoreCase(request.getMethod()) && uri.equals("/api/v1/worker/teams");

        // TEAM PAYMENT ACCESS GATE FIX — 24-09-2026 15:00:00
        // Reason:
        // 1. Backend previously allowed Team API access when WorkerMembership was missing.
        // 2. TEAM requires positive confirmation of ACTIVE TEAM_MONTHLY membership.
        // 3. Team Registration remains allowed before payment.
        // 4. Individual membership must never authorize Team Management.
        if (isTeamApi && !isTeamRegistration) {
            if (membershipOpt.isEmpty() || !"TEAM_MONTHLY".equals(membershipOpt.get().getPlan())) {
                sendForbiddenError(request, response, "TEAM_MEMBERSHIP_REQUIRED", "Active Team membership required.");
                return false;
            }
            WorkerMembership membership = membershipOpt.get();
            if (membership.getExpiresAt() != null && !membership.getExpiresAt().isAfter(LocalDateTime.now())) {
                sendForbiddenError(request, response, "MEMBERSHIP_EXPIRED", "Team membership expired. Renewal required.");
                return false;
            }
            return true;
        }

        if (membershipOpt.isEmpty()) {
            return true;
        }

        WorkerMembership membership = membershipOpt.get();
        if (membership.getExpiresAt() != null) {
            if (!membership.getExpiresAt().isAfter(LocalDateTime.now())) {
                sendForbiddenError(request, response, "MEMBERSHIP_EXPIRED", "Membership expired. Renewal required.");
                return false;
            }
        }

        return true;
    }

    private void sendForbiddenError(HttpServletRequest request, HttpServletResponse response, String code, String message) throws Exception {
        response.setStatus(HttpStatus.FORBIDDEN.value());
        response.setContentType("application/json");
        response.setCharacterEncoding("UTF-8");
        
        long startTime = getStartTime(request);
        
        ApiError apiError = ApiError.builder()
                .code(code)
                .message(message)
                .build();

        ApiResponse<?> apiResponse = ApiResponseFactory.failure(
                List.of(apiError),
                message,
                (String) request.getAttribute("requestId"),
                request.getRequestURI(),
                startTime
        );

        response.getWriter().write(objectMapper.writeValueAsString(apiResponse));
    }

    private long getStartTime(HttpServletRequest request) {
        Object attr = request.getAttribute("startTimeMs");
        if (attr instanceof Long) {
            return (Long) attr;
        }
        return System.currentTimeMillis();
    }
}
