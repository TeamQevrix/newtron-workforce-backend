package com.newtron.newtron_workforce_backend.controller;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.common.exception.ResourceNotFoundException;
import com.newtron.newtron_workforce_backend.dto.AppliedJobResponseDto;
import com.newtron.newtron_workforce_backend.dto.TimelineStepDto;
import com.newtron.newtron_workforce_backend.entity.Application;
import com.newtron.newtron_workforce_backend.entity.Job;
import com.newtron.newtron_workforce_backend.repository.ApplicationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/worker/applied-jobs")
@RequiredArgsConstructor
public class WorkerAppliedJobsController {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;

    @GetMapping
    public List<AppliedJobResponseDto> getAppliedJobs(
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = fetchCurrentUser(userDetails);

        List<Application> apps = applicationRepository.findByWorkerIdAndTeamIsNullWithJobAndRecruiter(currentUser.getId());
        List<AppliedJobResponseDto> dtos = new ArrayList<>();

        for (Application app : apps) {
            dtos.add(mapToDto(app));
        }

        return dtos;
    }

    @GetMapping("/{applicationId}")
    public AppliedJobResponseDto getApplicationDetails(
            @PathVariable("applicationId") Long applicationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = fetchCurrentUser(userDetails);

        Application app = applicationRepository.findById(applicationId)
                .orElseThrow(() -> new ResourceNotFoundException("APPLICATION_NOT_FOUND", "Application not found"));

        if (app.getWorker() == null || !app.getWorker().getId().equals(currentUser.getId())) {
            throw new ResourceNotFoundException("APPLICATION_NOT_FOUND", "Application not found");
        }

        return mapToDto(app);
    }

    private User fetchCurrentUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new ResourceNotFoundException("NOT_AUTHENTICATED", "Not authenticated");
        }
        return userRepository.findByMobile(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));
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

        List<TimelineStepDto> timeline = generateTimeline(app, companyName);

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

    private List<TimelineStepDto> generateTimeline(Application app, String companyName) {
        List<TimelineStepDto> timeline = new ArrayList<>();
        String status = app.getStatus() != null ? app.getStatus() : "Applied";
        String appliedDate = app.getAppliedDate() != null ? app.getAppliedDate() : "";

        // Status mapping to workflow: Applied -> Shortlisted -> Hired -> Completed
        // (Or Applied -> Rejected)

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
            timeline.add(TimelineStepDto.builder()
                    .title("Shortlisted")
                    .description("You have been shortlisted for this job.")
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
                    .title("Shortlisted")
                    .description("You have been shortlisted for this job.")
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
                    .description("Job has been completed and earnings logged.")
                    .timestamp(appliedDate)
                    .completed(true)
                    .build());
        } else {
            // Default "Applied" status
            timeline.add(TimelineStepDto.builder()
                    .title("Application Submitted")
                    .description("Sent successfully to " + companyName)
                    .timestamp(appliedDate)
                    .completed(true)
                    .build());
            timeline.add(TimelineStepDto.builder()
                    .title("Under Review")
                    .description("Recruiter is reviewing matching requirements.")
                    .timestamp("Pending")
                    .completed(false)
                    .build());
        }

        return timeline;
    }
}
