package com.newtron.newtron_workforce_backend.controller;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.common.exception.ResourceNotFoundException;
import com.newtron.newtron_workforce_backend.dto.JobHistoryResponseDto;
import com.newtron.newtron_workforce_backend.entity.Application;
import com.newtron.newtron_workforce_backend.entity.Job;
import com.newtron.newtron_workforce_backend.entity.WorkerEarning;
import com.newtron.newtron_workforce_backend.entity.WorkerReview;
import com.newtron.newtron_workforce_backend.repository.ApplicationRepository;
import com.newtron.newtron_workforce_backend.repository.WorkerEarningRepository;
import com.newtron.newtron_workforce_backend.repository.WorkerReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/v1/worker/job-history")
@RequiredArgsConstructor
public class WorkerJobHistoryController {

    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final WorkerEarningRepository workerEarningRepository;
    private final WorkerReviewRepository workerReviewRepository;

    @GetMapping
    public List<JobHistoryResponseDto> getJobHistory(@AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new ResourceNotFoundException("NOT_AUTHENTICATED", "Not authenticated");
        }
        User currentUser = userRepository.findByMobile(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));

        List<Application> apps = applicationRepository.findWorkerJobHistory(currentUser.getId());
        if (apps.isEmpty()) {
            return Collections.emptyList();
        }

        List<Long> applicationIds = apps.stream().map(Application::getId).collect(Collectors.toList());

        // Batch fetch earnings and reviews to avoid N+1 queries
        List<WorkerEarning> earnings = workerEarningRepository.findByApplicationIdInAndDeletedFalse(applicationIds);
        Map<Long, WorkerEarning> earningsMap = earnings.stream()
                .collect(Collectors.toMap(e -> e.getApplication().getId(), e -> e, (e1, e2) -> e1));

        List<WorkerReview> reviews = workerReviewRepository.findByApplicationIdInAndDeletedFalse(applicationIds);
        Map<Long, WorkerReview> reviewsMap = reviews.stream()
                .collect(Collectors.toMap(r -> r.getApplication().getId(), r -> r, (r1, r2) -> r1));

        List<JobHistoryResponseDto> dtos = new ArrayList<>();
        for (Application app : apps) {
            Job job = app.getJob();
            String companyName = "Newtron Client";
            String location = "";
            String salary = "";
            String duration = "";
            String jobTitle = "";
            Long jobId = null;

            if (job != null) {
                jobId = job.getId();
                jobTitle = job.getTitle();
                location = job.getCity();
                duration = job.getDuration() != null ? job.getDuration() : "";
                salary = job.getSalary() != null ? job.getSalary() : "";
                if (job.getRecruiter() != null && job.getRecruiter().getFullName() != null) {
                    companyName = job.getRecruiter().getFullName();
                }
            }

            WorkerEarning earning = earningsMap.get(app.getId());
            WorkerReview review = reviewsMap.get(app.getId());

            dtos.add(JobHistoryResponseDto.builder()
                    .applicationId(app.getId())
                    .jobId(jobId)
                    .jobTitle(jobTitle)
                    .company(companyName)
                    .location(location)
                    .salary(salary)
                    .hiredDate(app.getHiredAt())
                    .completionDate(app.getCompletedAt())
                    .status(app.getStatus())
                    .duration(duration)
                    .earnings(earning != null ? earning.getAmount() : null)
                    .rating(review != null ? review.getRating() : null)
                    .comment(review != null ? review.getComment() : null)
                    .build());
        }

        return dtos;
    }
}
