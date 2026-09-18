package com.newtron.newtron_workforce_backend.controller;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.common.exception.ResourceNotFoundException;
import com.newtron.newtron_workforce_backend.common.logging.RequestContext;
import com.newtron.newtron_workforce_backend.common.response.ApiResponse;
import com.newtron.newtron_workforce_backend.common.response.ApiResponseFactory;
import com.newtron.newtron_workforce_backend.dto.TeamRegistrationRequest;
import com.newtron.newtron_workforce_backend.dto.TeamRegistrationResponse;
import com.newtron.newtron_workforce_backend.service.WorkerTeamService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import com.newtron.newtron_workforce_backend.dto.AppliedJobResponseDto;
import com.newtron.newtron_workforce_backend.dto.TimelineStepDto;
import com.newtron.newtron_workforce_backend.entity.Application;
import com.newtron.newtron_workforce_backend.entity.Job;
import com.newtron.newtron_workforce_backend.entity.Team;
import com.newtron.newtron_workforce_backend.entity.WorkerProfile;
import com.newtron.newtron_workforce_backend.repository.ApplicationRepository;
import com.newtron.newtron_workforce_backend.repository.TeamRepository;
import com.newtron.newtron_workforce_backend.repository.WorkerProfileRepository;

import java.util.ArrayList;

@RestController
@RequestMapping("/api/v1/worker/teams")
@RequiredArgsConstructor
public class WorkerTeamController {

    private final WorkerTeamService service;
    private final UserRepository userRepository;
    private final TeamRepository teamRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final ApplicationRepository applicationRepository;

    @GetMapping("/my-team")
    public ApiResponse<TeamRegistrationResponse> getMyTeam(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);
        
        TeamRegistrationResponse response = service.getMyTeam(currentUser);
        
        return ApiResponseFactory.success(
                response,
                "Team details fetched successfully",
                RequestContext.getRequestId(),
                httpServletRequest.getRequestURI(),
                startTime
        );
    }

    @GetMapping
    public ApiResponse<java.util.List<TeamRegistrationResponse>> getMyTeams(
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);
        
        java.util.List<TeamRegistrationResponse> response = service.getMyTeams(currentUser);
        
        return ApiResponseFactory.success(
                response,
                "Teams fetched successfully",
                RequestContext.getRequestId(),
                httpServletRequest.getRequestURI(),
                startTime
        );
    }

    @PostMapping
    public ApiResponse<TeamRegistrationResponse> registerTeam(
            @Valid @RequestBody TeamRegistrationRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        TeamRegistrationResponse response = service.registerTeam(request, currentUser);

        return ApiResponseFactory.success(
                response,
                "Team registered successfully",
                RequestContext.getRequestId(),
                httpServletRequest.getRequestURI(),
                startTime
        );
    }

    @PutMapping("/{teamId}")
    public ApiResponse<TeamRegistrationResponse> updateTeam(
            @PathVariable("teamId") Long teamId,
            @Valid @RequestBody com.newtron.newtron_workforce_backend.dto.TeamUpdateRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        TeamRegistrationResponse response = service.updateTeam(teamId, request, currentUser);

        return ApiResponseFactory.success(
                response,
                "Team updated successfully",
                RequestContext.getRequestId(),
                httpServletRequest.getRequestURI(),
                startTime
        );
    }

    @PostMapping("/{teamId}/members")
    public ApiResponse<com.newtron.newtron_workforce_backend.dto.TeamMemberResponse> addTeamMember(
            @PathVariable("teamId") Long teamId,
            @Valid @RequestBody com.newtron.newtron_workforce_backend.dto.TeamMemberRequest request,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        com.newtron.newtron_workforce_backend.dto.TeamMemberResponse response = service.addTeamMember(teamId, request, currentUser);

        return ApiResponseFactory.success(
                response,
                "Team member added successfully",
                RequestContext.getRequestId(),
                httpServletRequest.getRequestURI(),
                startTime
        );
    }

    @DeleteMapping("/{teamId}/members/{memberId}")
    public ApiResponse<Void> removeTeamMember(
            @PathVariable("teamId") Long teamId,
            @PathVariable("memberId") Long memberId,
            @AuthenticationPrincipal UserDetails userDetails,
            HttpServletRequest httpServletRequest) {
        long startTime = getStartTime(httpServletRequest);
        User currentUser = fetchCurrentUser(userDetails);

        service.removeTeamMember(teamId, memberId, currentUser);

        return ApiResponseFactory.success(
                null,
                "Team member removed successfully",
                RequestContext.getRequestId(),
                httpServletRequest.getRequestURI(),
                startTime
        );
    }

    @GetMapping("/{teamId}/applications")
    public java.util.List<AppliedJobResponseDto> getTeamApplications(
            @PathVariable("teamId") Long teamId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = fetchCurrentUser(userDetails);
        
        WorkerProfile ownerProfile = workerProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("WORKER_PROFILE_NOT_FOUND", "Worker profile not found."));
                
        Team team = teamRepository.findByIdAndOwnerWorkerProfileIdAndDeletedFalse(teamId, ownerProfile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("TEAM_NOT_FOUND", "Team not found or not owned by the authenticated user."));
                
        java.util.List<Application> apps = applicationRepository.findByTeamIdWithJobAndRecruiter(team.getId());
        java.util.List<AppliedJobResponseDto> dtos = new ArrayList<>();
        
        for (Application app : apps) {
            dtos.add(mapToDto(app));
        }
        
        return dtos;
    }

    private AppliedJobResponseDto mapToDto(Application app) {
        Job job = app.getJob();
        String companyName = "Newtron Client";
        String location = "";
        Double dailyWage = null;
        String jobTitle = "";
        String jobId = "";

        if (job != null) {
            jobId = job.getId().toString();
            jobTitle = job.getTitle();
            location = job.getCity();
            if (job.getRecruiter() != null && job.getRecruiter().getFullName() != null) {
                companyName = job.getRecruiter().getFullName();
            }

            if (job.getSalary() != null) {
                try {
                    String cleanSalary = job.getSalary().replaceAll("[^0-9.]", "");
                    if (!cleanSalary.isEmpty()) {
                        dailyWage = Double.parseDouble(cleanSalary);
                    }
                } catch (Exception e) {
                    dailyWage = null;
                }
            }
        }

        java.util.List<TimelineStepDto> timeline = generateTimeline(app, companyName);

        return AppliedJobResponseDto.builder()
                .id(app.getId().toString())
                .jobId(jobId)
                .jobTitle(jobTitle)
                .company(companyName)
                .companyLogo("logo_default")
                .location(location)
                .dailyWage(dailyWage)
                .appliedDate(app.getAppliedDate() != null ? app.getAppliedDate() : "")
                .status(app.getStatus())
                .recruiterViewed(!"Applied".equalsIgnoreCase(app.getStatus()))
                .timeline(timeline)
                .build();
    }

    private java.util.List<TimelineStepDto> generateTimeline(Application app, String companyName) {
        java.util.List<TimelineStepDto> timeline = new ArrayList<>();
        String status = app.getStatus() != null ? app.getStatus() : "Applied";
        String appliedDate = app.getAppliedDate() != null ? app.getAppliedDate() : "";

        if ("Rejected".equalsIgnoreCase(status)) {
            timeline.add(TimelineStepDto.builder()
                    .title("Application Submitted")
                    .description("Sent successfully to " + companyName)
                    .timestamp(appliedDate)
                    .completed(true)
                    .build());
            timeline.add(TimelineStepDto.builder()
                    .title("Rejected")
                    .description("Application was not selected by the recruiter.")
                    .timestamp(appliedDate)
                    .completed(true)
                    .build());
        } else if ("Shortlisted".equalsIgnoreCase(status)) {
            timeline.add(TimelineStepDto.builder()
                    .title("Application Submitted")
                    .description("Sent successfully to " + companyName)
                    .timestamp(appliedDate)
                    .completed(true)
                    .build());
            timeline.add(TimelineStepDto.builder()
                    .title("Under Review")
                    .description("Recruiter has reviewed your application.")
                    .timestamp(appliedDate)
                    .completed(true)
                    .build());
        } else if ("Hired".equalsIgnoreCase(status)) {
            timeline.add(TimelineStepDto.builder()
                    .title("Application Submitted")
                    .description("Sent successfully to " + companyName)
                    .timestamp(appliedDate)
                    .completed(true)
                    .build());
            timeline.add(TimelineStepDto.builder()
                    .title("Under Review")
                    .description("Recruiter has reviewed your application.")
                    .timestamp(appliedDate)
                    .completed(true)
                    .build());
            timeline.add(TimelineStepDto.builder()
                    .title("Hired")
                    .description("Congratulations! You have been hired.")
                    .timestamp(appliedDate)
                    .completed(true)
                    .build());
        } else if ("Completed".equalsIgnoreCase(status)) {
            timeline.add(TimelineStepDto.builder()
                    .title("Application Submitted")
                    .description("Sent successfully to " + companyName)
                    .timestamp(appliedDate)
                    .completed(true)
                    .build());
            timeline.add(TimelineStepDto.builder()
                    .title("Under Review")
                    .description("Recruiter has reviewed your application.")
                    .timestamp(appliedDate)
                    .completed(true)
                    .build());
            timeline.add(TimelineStepDto.builder()
                    .title("Hired")
                    .description("Congratulations! You have been hired.")
                    .timestamp(appliedDate)
                    .completed(true)
                    .build());
            timeline.add(TimelineStepDto.builder()
                    .title("Completed")
                    .description("The job has been completed successfully.")
                    .timestamp(app.getCompletedAt() != null ? app.getCompletedAt().toString() : appliedDate)
                    .completed(true)
                    .build());
        } else {
            timeline.add(TimelineStepDto.builder()
                    .title("Application Submitted")
                    .description("Sent successfully to " + companyName)
                    .timestamp(appliedDate)
                    .completed(true)
                    .build());
        }

        return timeline;
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
