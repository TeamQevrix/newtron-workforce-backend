package com.newtron.newtron_workforce_backend.controller;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.common.exception.ResourceNotFoundException;
import com.newtron.newtron_workforce_backend.dto.JobDetailDto;
import com.newtron.newtron_workforce_backend.dto.SavedJobResponseDto;
import com.newtron.newtron_workforce_backend.entity.Job;
import com.newtron.newtron_workforce_backend.entity.SavedJob;
import com.newtron.newtron_workforce_backend.repository.JobRepository;
import com.newtron.newtron_workforce_backend.repository.SavedJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.*;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class WorkerSavedJobsController {

    private final SavedJobRepository savedJobRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    @GetMapping("/worker/saved-jobs")
    public org.springframework.data.domain.Page<SavedJobResponseDto> getSavedJobs(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = fetchCurrentUser(userDetails);
        
        int safeSize = Math.min(size, 50); // Bound the maximum page size
        org.springframework.data.domain.Pageable pageable = org.springframework.data.domain.PageRequest.of(page, safeSize, org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "id"));
        
        org.springframework.data.domain.Page<SavedJob> savedJobs = savedJobRepository.findByWorkerIdWithJobAndRecruiter(currentUser.getId(), pageable);
        return savedJobs.map(this::mapToDto);
    }

    @PostMapping("/jobs/{jobId}/save")
    @Transactional
    public void saveJob(
            @PathVariable("jobId") Long jobId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = fetchCurrentUser(userDetails);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("JOB_NOT_FOUND", "Job not found"));

        Optional<SavedJob> existing = savedJobRepository.findByWorkerIdAndJobId(currentUser.getId(), job.getId());
        if (existing.isPresent()) {
            return; // Idempotent
        }

        SavedJob sj = SavedJob.builder()
                .worker(currentUser)
                .job(job)
                .build();
        savedJobRepository.save(sj);
    }

    @DeleteMapping("/jobs/{jobId}/save")
    @Transactional
    public void unsaveJob(
            @PathVariable("jobId") Long jobId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = fetchCurrentUser(userDetails);
        savedJobRepository.deleteByWorkerIdAndJobId(currentUser.getId(), jobId);
    }

    private User fetchCurrentUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new ResourceNotFoundException("NOT_AUTHENTICATED", "Not authenticated");
        }
        return userRepository.findByMobile(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));
    }

    private SavedJobResponseDto mapToDto(SavedJob savedJob) {
        Job job = savedJob.getJob();
        String companyName = "Newtron Client";
        if (job.getRecruiter() != null && job.getRecruiter().getFullName() != null) {
            companyName = job.getRecruiter().getFullName();
        }
        
        Double wage = null;
        if (job.getSalary() != null) {
            try {
                String clean = job.getSalary().replaceAll("[^0-9.]", "");
                if (!clean.isEmpty()) {
                    wage = Double.parseDouble(clean);
                }
            } catch (Exception e) {
                wage = null;
            }
        }

        JobDetailDto jobDto = JobDetailDto.builder()
                .id(job.getId().toString())
                .company(companyName)
                .companyLogo("logo_default")
                .jobTitle(job.getTitle())
                .location(job.getCity())
                .dailyWage(wage)
                .employmentType(job.getDuration() != null ? job.getDuration() : "Full Time")
                .distance(job.getDistance() != null ? job.getDistance() : "2.5 KM")
                .postedTime("Recent")
                .verified(false)
                .rating(5.0)
                .status(job.getStatus())
                .build();

        return SavedJobResponseDto.builder()
                .id(savedJob.getId().toString())
                .savedAt("Recent")
                .job(jobDto)
                .build();
    }
}
