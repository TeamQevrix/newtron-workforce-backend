package com.newtron.newtron_workforce_backend.controller;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.common.exception.ResourceNotFoundException;
import com.newtron.newtron_workforce_backend.entity.Job;
import com.newtron.newtron_workforce_backend.entity.NotInterestedJob;
import com.newtron.newtron_workforce_backend.repository.JobRepository;
import com.newtron.newtron_workforce_backend.repository.NotInterestedJobRepository;
import com.newtron.newtron_workforce_backend.repository.SavedJobRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class NotInterestedJobController {

    private final NotInterestedJobRepository notInterestedJobRepository;
    private final SavedJobRepository savedJobRepository;
    private final JobRepository jobRepository;
    private final UserRepository userRepository;

    @PostMapping("/{jobId}/not-interested")
    @Transactional
    public void markNotInterested(
            @PathVariable("jobId") Long jobId,
            @AuthenticationPrincipal UserDetails userDetails) {
        User currentUser = fetchCurrentUser(userDetails);
        Job job = jobRepository.findById(jobId)
                .orElseThrow(() -> new ResourceNotFoundException("JOB_NOT_FOUND", "Job not found"));

        // Conflict Resolution: If saved, remove from saved_jobs
        savedJobRepository.deleteByWorkerIdAndJobId(currentUser.getId(), job.getId());

        Optional<NotInterestedJob> existing = notInterestedJobRepository.findByWorkerIdAndJobId(currentUser.getId(), job.getId());
        if (existing.isPresent()) {
            return; // Idempotent
        }

        NotInterestedJob nij = NotInterestedJob.builder()
                .worker(currentUser)
                .job(job)
                .build();
        notInterestedJobRepository.save(nij);
    }

    private User fetchCurrentUser(UserDetails userDetails) {
        if (userDetails == null) {
            throw new ResourceNotFoundException("NOT_AUTHENTICATED", "Not authenticated");
        }
        return userRepository.findByMobile(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));
    }
}
