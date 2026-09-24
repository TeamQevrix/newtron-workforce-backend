package com.newtron.newtron_workforce_backend.controller;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.dto.JobDetailDto;
import com.newtron.newtron_workforce_backend.entity.WorkerAddress;
import com.newtron.newtron_workforce_backend.entity.WorkerProfessional;
import com.newtron.newtron_workforce_backend.entity.WorkerProfile;
import com.newtron.newtron_workforce_backend.repository.ApplicationRepository;
import com.newtron.newtron_workforce_backend.repository.JobRepository;
import com.newtron.newtron_workforce_backend.repository.WorkerAddressRepository;
import com.newtron.newtron_workforce_backend.repository.WorkerProfessionalRepository;
import com.newtron.newtron_workforce_backend.repository.WorkerProfileRepository;
import com.newtron.newtron_workforce_backend.repository.SavedJobRepository;
import com.newtron.newtron_workforce_backend.repository.TeamRepository;
import com.newtron.newtron_workforce_backend.repository.NotInterestedJobRepository;
import com.newtron.newtron_workforce_backend.dto.JobDetailsDto;
import com.newtron.newtron_workforce_backend.dto.ApplyJobResponse;
import com.newtron.newtron_workforce_backend.entity.Application;
import com.newtron.newtron_workforce_backend.common.exception.ResourceNotFoundException;
import com.newtron.newtron_workforce_backend.common.exception.ValidationException;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;
import org.springframework.transaction.annotation.Transactional;

import com.newtron.newtron_workforce_backend.common.exception.BusinessException;
import java.util.ArrayList;
import java.util.List;

@RestController
@RequestMapping("/api/v1/jobs")
@RequiredArgsConstructor
public class JobController {

    private final JobRepository jobRepository;
    private final ApplicationRepository applicationRepository;
    private final UserRepository userRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final WorkerProfessionalRepository workerProfessionalRepository;
    private final WorkerAddressRepository workerAddressRepository;
    private final JdbcTemplate jdbcTemplate;
    private final SavedJobRepository savedJobRepository;
    private final NotInterestedJobRepository notInterestedJobRepository;
    private final TeamRepository teamRepository;
    private final com.newtron.newtron_workforce_backend.repository.JobPhotoRepository jobPhotoRepository;
    private final com.newtron.newtron_workforce_backend.service.StorageService storageService;

    @GetMapping
    public List<JobDetailDto> getJobs(
            @RequestParam(value = "page", defaultValue = "0") int page,
            @RequestParam(value = "size", defaultValue = "10") int size,
            @RequestParam(value = "search", required = false) String search,
            @RequestParam(value = "skill", required = false) String skill,
            @RequestParam(value = "employmentType", required = false) String employmentType,
            @AuthenticationPrincipal UserDetails userDetails) {

        List<com.newtron.newtron_workforce_backend.entity.Job> jobs;
        if (search != null && !search.trim().isEmpty()) {
            jobs = jobRepository.findActiveJobsBySearchQuery(search.trim());
        } else {
            jobs = jobRepository.findAllActiveJobs();
        }
        
        List<com.newtron.newtron_workforce_backend.entity.Job> jobsList = new ArrayList<>(jobs);

        List<Long> hiddenJobIds = new ArrayList<>();
        String workerCity = null;
        if (userDetails != null) {
            User currentUser = userRepository.findByMobile(userDetails.getUsername()).orElse(null);
            if (currentUser != null) {
                hiddenJobIds = notInterestedJobRepository.findHiddenJobIdsByWorkerId(currentUser.getId());
                WorkerProfile profile = workerProfileRepository.findByUserId(currentUser.getId()).orElse(null);
                if (profile != null) {
                    WorkerAddress address = workerAddressRepository.findByWorkerProfileId(profile.getId()).orElse(null);
                    if (address != null && address.getCityId() != null) {
                        try {
                            workerCity = jdbcTemplate.queryForObject(
                                    "SELECT name FROM master_cities WHERE id = ?",
                                    String.class,
                                    address.getCityId()
                            );
                        } catch (Exception e) {
                            workerCity = null;
                        }
                    }
                }
            }
        }

        final String finalWorkerCity = workerCity;
        // Sort by city priority, then newest first
        jobsList.sort((a, b) -> {
            if (finalWorkerCity != null) {
                boolean aMatch = a.getCity() != null && a.getCity().trim().equalsIgnoreCase(finalWorkerCity.trim());
                boolean bMatch = b.getCity() != null && b.getCity().trim().equalsIgnoreCase(finalWorkerCity.trim());
                if (aMatch && !bMatch) return -1;
                if (!aMatch && bMatch) return 1;
            }
            return b.getId().compareTo(a.getId());
        });

        java.util.Map<Long, Long> filledCountsMap;
        if (jobsList.isEmpty()) {
            filledCountsMap = java.util.Collections.emptyMap();
        } else {
            filledCountsMap = new java.util.HashMap<>();
            List<Long> jobIds = jobsList.stream().map(com.newtron.newtron_workforce_backend.entity.Job::getId).collect(java.util.stream.Collectors.toList());
            List<Object[]> filledResults = applicationRepository.countFilledByJobIds(jobIds);
            for (Object[] row : filledResults) {
                if (row[0] != null && row[1] != null) {
                    filledCountsMap.put(((Number) row[0]).longValue(), ((Number) row[1]).longValue());
                }
            }
        }

        List<JobDetailDto> dtos = new ArrayList<>();
        for (com.newtron.newtron_workforce_backend.entity.Job job : jobsList) {
            if (hiddenJobIds.contains(job.getId())) {
                continue;
            }
            // Filter by Active status
            if (job.getStatus() == null || !"Active".equalsIgnoreCase(job.getStatus().trim())) {
                continue;
            }

            // Filter out fully filled jobs (Hired + Completed counts)
            int required = job.getWorkersRequired() != null ? job.getWorkersRequired() : 1;
            long filledCount = filledCountsMap.getOrDefault(job.getId(), 0L);
            if (filledCount >= required) {
                continue;
            }

            // Filter by skill chip category
            if (skill != null && !skill.trim().isEmpty() && !"All".equalsIgnoreCase(skill.trim())) {
                boolean matchesCategory = job.getCategory() != null && job.getCategory().equalsIgnoreCase(skill.trim());
                boolean matchesTitle = job.getTitle() != null && job.getTitle().toLowerCase().contains(skill.trim().toLowerCase());
                if (!matchesCategory && !matchesTitle) {
                    continue;
                }
            }

            // Filter by employment type
            if (employmentType != null && !employmentType.trim().isEmpty()) {
                if (job.getDuration() == null || !job.getDuration().toLowerCase().contains(employmentType.toLowerCase().trim())) {
                    continue;
                }
            }

            String companyName;
            if (job.getCompany() != null && job.getCompany().getCompanyName() != null) {
                companyName = job.getCompany().getCompanyName();
            } else {
                companyName = (job.getCompanyName() != null && !job.getCompanyName().trim().isEmpty())
                        ? job.getCompanyName().trim()
                        : ((job.getRecruiter() != null && job.getRecruiter().getFullName() != null)
                                ? job.getRecruiter().getFullName() : "Newtron Client");
            }

            // Safe salary double parsing
            Double wageVal = null;
            if (job.getSalary() != null) {
                try {
                    String cleanSalary = job.getSalary().replaceAll("[^0-9.]", "");
                    if (!cleanSalary.isEmpty()) {
                        wageVal = Double.parseDouble(cleanSalary);
                    }
                } catch (Exception e) {
                    wageVal = null;
                }
            }

            dtos.add(JobDetailDto.builder()
                    .id(job.getId().toString())
                    .company(companyName)
                    .companyLogo(null)
                    .jobTitle(job.getTitle())
                    .location(job.getCity())
                    .dailyWage(wageVal)
                    .engagementType(job.getEngagementType() != null ? job.getEngagementType() : "DAILY")
                    .monthlySalaryAmount(job.getMonthlySalaryAmount())
                    .employmentType(job.getDuration() != null ? job.getDuration() : "Full Time")
                    .distance(job.getDistance() != null ? job.getDistance() : "2.5 KM")
                    .postedTime("Posted recently")
                    .verified(false)
                    .rating(null)
                    .build());
        }

        // Apply pagination
        int fromIndex = page * size;
        if (fromIndex >= dtos.size()) {
            return new ArrayList<>();
        }
        int toIndex = Math.min(fromIndex + size, dtos.size());
        return dtos.subList(fromIndex, toIndex);
    }

    @GetMapping("/for-you")
    public List<JobDetailDto> getJobsForYou(
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return new ArrayList<>();
        }

        User currentUser = userRepository.findByMobile(userDetails.getUsername()).orElse(null);
        if (currentUser == null) {
            return new ArrayList<>();
        }

        WorkerProfile profile = workerProfileRepository.findByUserId(currentUser.getId()).orElse(null);
        if (profile == null) {
            return new ArrayList<>();
        }

        WorkerProfessional professional = workerProfessionalRepository.findByWorkerProfileId(profile.getId()).orElse(null);
        if (professional == null) {
            return new ArrayList<>();
        }

        WorkerAddress address = workerAddressRepository.findByWorkerProfileId(profile.getId()).orElse(null);
        if (address == null) {
            return new ArrayList<>();
        }

        // Retrieve city name dynamically using JdbcTemplate
        String cityName = null;
        if (address.getCityId() != null) {
            try {
                cityName = jdbcTemplate.queryForObject(
                        "SELECT name FROM master_cities WHERE id = ?",
                        String.class,
                        address.getCityId()
                );
            } catch (Exception e) {
                cityName = null;
            }
        }

        String primarySkill = professional.getPrimarySkill();
        String secondarySkill = professional.getSecondarySkill();

        if ((primarySkill == null || primarySkill.trim().isEmpty()) && cityName == null) {
            return new ArrayList<>();
        }

        // Fetch already applied jobs
        java.util.List<com.newtron.newtron_workforce_backend.entity.Application> userApplications = applicationRepository.findByWorkerId(currentUser.getId());
        java.util.Set<Long> appliedJobIds = userApplications.stream()
                .filter(app -> app.getJob() != null)
                .map(app -> app.getJob().getId())
                .collect(java.util.stream.Collectors.toSet());

        java.util.List<Long> hiddenJobIds = notInterestedJobRepository.findHiddenJobIdsByWorkerId(currentUser.getId());

        List<com.newtron.newtron_workforce_backend.entity.Job> jobs = jobRepository.findAll();

        @Getter
        @RequiredArgsConstructor
        class ScoredJob {
            private final com.newtron.newtron_workforce_backend.entity.Job job;
            private final int score;
            private final boolean applied;
        }

        List<ScoredJob> scoredJobs = new ArrayList<>();
        for (com.newtron.newtron_workforce_backend.entity.Job job : jobs) {
            if (hiddenJobIds.contains(job.getId())) {
                continue;
            }
            // Filter by Active status
            if (job.getStatus() == null || !"Active".equalsIgnoreCase(job.getStatus().trim())) {
                continue;
            }

            // Filter out fully filled jobs (Hired + Completed counts)
            int required = job.getWorkersRequired() != null ? job.getWorkersRequired() : 1;
            long filledCount = applicationRepository.countByJobIdAndStatus(job.getId(), "Hired") +
                               applicationRepository.countByJobIdAndStatus(job.getId(), "Completed");
            if (filledCount >= required) {
                continue;
            }

            int score = 0;
            boolean hasSignal = false;

            // 1. Primary Skill Match
            if (primarySkill != null && !primarySkill.trim().isEmpty()) {
                String ps = primarySkill.trim().toLowerCase();
                if (job.getCategory() != null && job.getCategory().trim().toLowerCase().equals(ps)) {
                    score += 50;
                    hasSignal = true;
                } else if (job.getTitle() != null && job.getTitle().toLowerCase().contains(ps)) {
                    score += 30;
                    hasSignal = true;
                }
            }

            // 2. Secondary Skill Match
            if (secondarySkill != null && !secondarySkill.trim().isEmpty()) {
                String ss = secondarySkill.trim().toLowerCase();
                if (job.getCategory() != null && job.getCategory().trim().toLowerCase().equals(ss)) {
                    score += 40;
                    hasSignal = true;
                } else if (job.getTitle() != null && job.getTitle().toLowerCase().contains(ss)) {
                    score += 20;
                    hasSignal = true;
                }
            }

            // 3. City Match
            if (cityName != null && job.getCity() != null) {
                if (job.getCity().trim().equalsIgnoreCase(cityName.trim())) {
                    score += 30;
                    hasSignal = true;
                }
            }

            // Exclude jobs with score 0 (no signal)
            if (!hasSignal) {
                continue;
            }

            boolean applied = appliedJobIds.contains(job.getId());
            // Prioritize unapplied jobs
            if (!applied) {
                score += 20;
            }

            scoredJobs.add(new ScoredJob(job, score, applied));
        }

        // Sort by score DESC, then job ID DESC
        scoredJobs.sort((a, b) -> {
            int comp = Integer.compare(b.getScore(), a.getScore());
            if (comp != 0) {
                return comp;
            }
            return b.getJob().getId().compareTo(a.getJob().getId());
        });

        List<JobDetailDto> dtos = new ArrayList<>();
        // Return top 20 recommendations
        int limit = Math.min(scoredJobs.size(), 20);
        for (int i = 0; i < limit; i++) {
            com.newtron.newtron_workforce_backend.entity.Job job = scoredJobs.get(i).getJob();

            String companyName;
            if (job.getCompany() != null && job.getCompany().getCompanyName() != null) {
                companyName = job.getCompany().getCompanyName();
            } else {
                companyName = (job.getCompanyName() != null && !job.getCompanyName().trim().isEmpty())
                        ? job.getCompanyName().trim()
                        : ((job.getRecruiter() != null && job.getRecruiter().getFullName() != null)
                                ? job.getRecruiter().getFullName() : "Newtron Client");
            }

            // Safe salary double parsing
            Double wageVal = null;
            if (job.getSalary() != null) {
                try {
                    String cleanSalary = job.getSalary().replaceAll("[^0-9.]", "");
                    if (!cleanSalary.isEmpty()) {
                        wageVal = Double.parseDouble(cleanSalary);
                    }
                } catch (Exception e) {
                    wageVal = null;
                }
            }

            dtos.add(JobDetailDto.builder()
                    .id(job.getId().toString())
                    .company(companyName)
                    .companyLogo(null)
                    .jobTitle(job.getTitle())
                    .location(job.getCity())
                    .dailyWage(wageVal)
                    .employmentType(job.getDuration() != null ? job.getDuration() : "Full Time")
                    .distance(job.getDistance() != null ? job.getDistance() : "2.5 KM")
                    .postedTime("Posted recently")
                    .verified(false)
                    .rating(null)
                    .build());
        }

        return dtos;
    }

    @GetMapping("/nearby")
    public List<JobDetailDto> getNearbyJobs(
            @RequestParam(value = "radius", defaultValue = "15.0") Double radius,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (radius == null) {
            radius = 15.0;
        }
        if (radius <= 0 || radius > 50.0) {
            throw new BusinessException("INVALID_RADIUS", "Radius must be greater than 0 and less than or equal to 50 KM");
        }

        if (userDetails == null) {
            return new ArrayList<>();
        }

        User currentUser = userRepository.findByMobile(userDetails.getUsername()).orElse(null);
        if (currentUser == null) {
            return new ArrayList<>();
        }

        WorkerProfile profile = workerProfileRepository.findByUserId(currentUser.getId()).orElse(null);
        if (profile == null) {
            return new ArrayList<>();
        }

        WorkerAddress address = workerAddressRepository.findByWorkerProfileId(profile.getId()).orElse(null);
        if (address == null || address.getLatitude() == null || address.getLongitude() == null) {
            return new ArrayList<>();
        }

        double workerLat = address.getLatitude();
        double workerLon = address.getLongitude();

        // Calculate bounding box boundaries
        double latOffset = radius / 111.0;
        double lonOffset = radius / (111.0 * Math.cos(Math.toRadians(workerLat)));
        double minLat = workerLat - latOffset;
        double maxLat = workerLat + latOffset;
        double minLon = workerLon - lonOffset;
        double maxLon = workerLon + lonOffset;

        java.util.List<Long> hiddenJobIds = notInterestedJobRepository.findHiddenJobIdsByWorkerId(currentUser.getId());

        List<com.newtron.newtron_workforce_backend.entity.Job> candidates =
                jobRepository.findActiveJobsWithinBoundingBox(minLat, maxLat, minLon, maxLon);

        @Getter
        @RequiredArgsConstructor
        class NearbyScoredJob {
            private final JobDetailDto dto;
            private final double distance;
        }

        List<NearbyScoredJob> nearbyScoredJobs = new ArrayList<>();
        for (com.newtron.newtron_workforce_backend.entity.Job job : candidates) {
            if (hiddenJobIds.contains(job.getId())) {
                continue;
            }
            if (job.getLatitude() == null || job.getLongitude() == null) {
                continue;
            }

            // Exclude non-active jobs
            if (job.getStatus() == null || !"Active".equalsIgnoreCase(job.getStatus().trim())) {
                continue;
            }

            // Filter out fully filled jobs (Hired + Completed counts)
            int required = job.getWorkersRequired() != null ? job.getWorkersRequired() : 1;
            long filledCount = applicationRepository.countByJobIdAndStatus(job.getId(), "Hired") +
                               applicationRepository.countByJobIdAndStatus(job.getId(), "Completed");
            if (filledCount >= required) {
                continue;
            }

            // Calculate precise geographic distance using the Haversine formula
            double dLat = Math.toRadians(job.getLatitude() - workerLat);
            double dLon = Math.toRadians(job.getLongitude() - workerLon);
            double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                       Math.cos(Math.toRadians(workerLat)) * Math.cos(Math.toRadians(job.getLatitude())) *
                       Math.sin(dLon / 2) * Math.sin(dLon / 2);
            double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
            double distance = 6371.0 * c; // Earth radius: 6371.0 KM

            if (distance > radius) {
                continue;
            }

            // Safe salary double parsing
            Double wageVal = null;
            if (job.getSalary() != null) {
                try {
                    String cleanSalary = job.getSalary().replaceAll("[^0-9.]", "");
                    if (!cleanSalary.isEmpty()) {
                        wageVal = Double.parseDouble(cleanSalary);
                    }
                } catch (Exception e) {
                    wageVal = null;
                }
            }

            String companyName = (job.getRecruiter() != null && job.getRecruiter().getFullName() != null)
                    ? job.getRecruiter().getFullName() : "Newtron Client";

            JobDetailDto dto = JobDetailDto.builder()
                    .id(job.getId().toString())
                    .company(companyName)
                    .companyLogo(null)
                    .jobTitle(job.getTitle())
                    .location(job.getCity())
                    .dailyWage(wageVal)
                    .engagementType(job.getEngagementType() != null ? job.getEngagementType() : "DAILY")
                    .monthlySalaryAmount(job.getMonthlySalaryAmount())
                    .employmentType(job.getDuration() != null ? job.getDuration() : "Full Time")
                    .distance(String.format(java.util.Locale.US, "%.1f KM", distance))
                    .postedTime("Posted recently")
                    .verified(false)
                    .rating(null)
                    .build();

            nearbyScoredJobs.add(new NearbyScoredJob(dto, distance));
        }

        // Sort by distance ASC
        nearbyScoredJobs.sort((a, b) -> Double.compare(a.getDistance(), b.getDistance()));

        List<JobDetailDto> dtos = new ArrayList<>();
        for (NearbyScoredJob scored : nearbyScoredJobs) {
            dtos.add(scored.getDto());
        }

        return dtos;
    }

    @GetMapping("/{id}")
    public JobDetailsDto getJobDetails(
            @PathVariable("id") Long id,
            @RequestParam(value = "teamId", required = false) Long teamId,
            @AuthenticationPrincipal UserDetails userDetails) {
        com.newtron.newtron_workforce_backend.entity.Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JOB_NOT_FOUND", "Job not found"));

        Double wageVal = null;
        if (job.getSalary() != null) {
            try {
                String cleanSalary = job.getSalary().replaceAll("[^0-9.]", "");
                if (!cleanSalary.isEmpty()) {
                    wageVal = Double.parseDouble(cleanSalary);
                }
            } catch (Exception e) {
                wageVal = null;
            }
        }

        String appStatus = null;
        Double distanceVal = null;
        if (userDetails != null) {
            User worker = userRepository.findByMobile(userDetails.getUsername()).orElse(null);
            if (worker != null) {
                Application app = null;
                if (teamId != null) {
                    app = applicationRepository.findByTeamIdAndJobId(teamId, job.getId()).orElse(null);
                } else {
                    app = applicationRepository.findByWorkerIdAndJobId(worker.getId(), job.getId()).orElse(null);
                }
                
                if (app != null) {
                    appStatus = app.getStatus();
                }

                WorkerProfile profile = workerProfileRepository.findByUserId(worker.getId()).orElse(null);
                if (profile != null) {
                    WorkerAddress address = workerAddressRepository.findByWorkerProfileId(profile.getId()).orElse(null);
                    if (address != null && address.getLatitude() != null && address.getLongitude() != null &&
                            job.getLatitude() != null && job.getLongitude() != null) {
                        double workerLat = address.getLatitude();
                        double workerLon = address.getLongitude();
                        double dLat = Math.toRadians(job.getLatitude() - workerLat);
                        double dLon = Math.toRadians(job.getLongitude() - workerLon);
                        double a = Math.sin(dLat / 2) * Math.sin(dLat / 2) +
                                   Math.cos(Math.toRadians(workerLat)) * Math.cos(Math.toRadians(job.getLatitude())) *
                                   Math.sin(dLon / 2) * Math.sin(dLon / 2);
                        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
                        distanceVal = 6371.0 * c;
                    }
                }
            }
        }

        String distanceStr = (distanceVal != null) ? String.format(java.util.Locale.US, "%.1f KM", distanceVal) : (job.getDistance() != null ? job.getDistance() : "2.5 KM");
        String recruiterName;
        if (job.getCompany() != null && job.getCompany().getCompanyName() != null) {
            recruiterName = job.getCompany().getCompanyName();
        } else {
            recruiterName = (job.getCompanyName() != null && !job.getCompanyName().trim().isEmpty())
                    ? job.getCompanyName().trim()
                    : ((job.getRecruiter() != null && job.getRecruiter().getFullName() != null)
                            ? job.getRecruiter().getFullName() : "Newtron Client");
        }
        String recruiterContact = (job.getRecruiter() != null) ? job.getRecruiter().getMobile() : null;

        int required = job.getWorkersRequired() != null ? job.getWorkersRequired() : 1;
        long filledCount = applicationRepository.countByJobIdAndStatus(job.getId(), "Hired") +
                           applicationRepository.countByJobIdAndStatus(job.getId(), "Completed");
        int openPositions = Math.max(0, required - (int) filledCount);

        boolean isSavedVal = false;
        boolean isNotInterestedVal = false;
        if (userDetails != null) {
            User worker = userRepository.findByMobile(userDetails.getUsername()).orElse(null);
            if (worker != null) {
                isSavedVal = savedJobRepository.findByWorkerIdAndJobId(worker.getId(), job.getId()).isPresent();
                isNotInterestedVal = notInterestedJobRepository.findByWorkerIdAndJobId(worker.getId(), job.getId()).isPresent();
            }
        }

        if (job.getWorkMode() != null && "TEAM".equalsIgnoreCase(job.getWorkMode().trim())) {
            if (userDetails != null) {
                User worker = userRepository.findByMobile(userDetails.getUsername()).orElse(null);
                if (worker != null && worker.getRole() == com.newtron.newtron_workforce_backend.auth.enums.Role.WORKER) {
                    WorkerProfile profile = workerProfileRepository.findByUserId(worker.getId()).orElse(null);
                    if (profile == null || !teamRepository.existsByOwnerWorkerProfileId(profile.getId())) {
                        throw new com.newtron.newtron_workforce_backend.common.exception.ValidationException("UNAUTHORIZED_ACCESS", "Team jobs are restricted to team owners.");
                    }
                }
            } else {
                throw new com.newtron.newtron_workforce_backend.common.exception.ValidationException("UNAUTHORIZED_ACCESS", "Authentication required for team jobs.");
            }
        }

        List<com.newtron.newtron_workforce_backend.entity.JobPhoto> photos = jobPhotoRepository.findByJobIdOrderByDisplayOrderAsc(job.getId());
        List<String> photoUrls = new ArrayList<>();
        if (photos != null && !photos.isEmpty()) {
            for (com.newtron.newtron_workforce_backend.entity.JobPhoto photo : photos) {
                photoUrls.add(storageService.generateDownloadUrl(photo.getStorageKey()));
            }
        }

        return JobDetailsDto.builder()
                .id(job.getId().toString())
                .companyId(job.getRecruiter() != null ? job.getRecruiter().getId().toString() : "comp_101")
                .companyName(recruiterName)
                .companyLogo(null)
                .companyVerified(false)
                .companyRating(null)
                .jobTitle(job.getTitle())
                .dailyWage(wageVal)
                .engagementType(job.getEngagementType() != null ? job.getEngagementType() : "DAILY")
                .monthlySalaryAmount(job.getMonthlySalaryAmount())
                .employmentType(job.getDuration() != null ? job.getDuration() : "Full Time")
                .experienceRequired(job.getExperienceRequired())
                .openPositions(openPositions)
                .joiningDate(job.getJoiningDate())
                .shiftHours(job.getShiftHours())
                .address(job.getCity())
                .distance(distanceStr)
                .description(job.getDescription() != null ? job.getDescription() : "")
                .requiredSkills(java.util.Arrays.asList(job.getCategory() != null ? job.getCategory() : "General"))
                .benefits(job.getBenefits() != null && !job.getBenefits().trim().isEmpty() ? java.util.Arrays.asList(job.getBenefits().split("\\s*,\\s*")) : new java.util.ArrayList<>())
                .recruiterName(recruiterName)
                .recruiterContact(recruiterContact)
                .applicationStatus(appStatus)
                .isSaved(isSavedVal)
                .isNotInterested(isNotInterestedVal)
                .photos(photoUrls)
                .build();
    }

    @PostMapping("/{id}/apply")
    public ApplyJobResponse applyForJob(
            @PathVariable("id") Long id,
            @RequestBody(required = false) com.newtron.newtron_workforce_backend.dto.ApplyJobRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            throw new ResourceNotFoundException("NOT_AUTHENTICATED", "Not authenticated");
        }

        User worker = userRepository.findByMobile(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("USER_NOT_FOUND", "User not found"));

        com.newtron.newtron_workforce_backend.entity.Job job = jobRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("JOB_NOT_FOUND", "Job not found"));

        if (job.getStatus() == null || !"Active".equalsIgnoreCase(job.getStatus().trim())) {
            throw new ValidationException("INACTIVE_JOB", "This job is closed or inactive");
        }

        int required = job.getWorkersRequired() != null ? job.getWorkersRequired() : 1;
        long filledCount = applicationRepository.countByJobIdAndStatus(job.getId(), "Hired") +
                           applicationRepository.countByJobIdAndStatus(job.getId(), "Completed");
        if (filledCount >= required) {
            throw new ValidationException("JOB_FULL", "This job is already fully filled");
        }

        com.newtron.newtron_workforce_backend.entity.Team applyingTeam = null;

        if (job.getWorkMode() != null && "TEAM".equalsIgnoreCase(job.getWorkMode().trim())) {
            if (worker.getRole() != com.newtron.newtron_workforce_backend.auth.enums.Role.WORKER) {
                throw new ValidationException("UNAUTHORIZED_ACCESS", "Only workers can apply to team jobs");
            }
            if (request == null || request.getTeamId() == null) {
                throw new ValidationException("MISSING_TEAM_ID", "Team ID is required to apply for TEAM jobs");
            }

            WorkerProfile profile = workerProfileRepository.findByUserId(worker.getId())
                    .orElseThrow(() -> new ValidationException("UNAUTHORIZED_ACCESS", "Worker profile required"));
            
            applyingTeam = teamRepository.findByIdAndOwnerWorkerProfileIdAndDeletedFalse(request.getTeamId(), profile.getId())
                    .orElseThrow(() -> new ValidationException("UNAUTHORIZED_ACCESS", "You do not own this team or it does not exist."));

            if (applicationRepository.findByTeamIdAndJobId(request.getTeamId(), job.getId()).isPresent()) {
                throw new ValidationException("ALREADY_APPLIED", "This team has already applied for this job");
            }

            String jobSkill = job.getCategory() != null ? job.getCategory().trim().toLowerCase() : "";
            String teamSkill = (applyingTeam.getPrimarySkill() != null && applyingTeam.getPrimarySkill().getName() != null) ? applyingTeam.getPrimarySkill().getName().trim().toLowerCase() : "";

            if (!jobSkill.isEmpty() && !teamSkill.isEmpty() && !jobSkill.equals(teamSkill) && (job.getTitle() == null || !job.getTitle().toLowerCase().contains(teamSkill))) {
                throw new ValidationException("SKILL_MISMATCH", "Your team's primary skill does not match the job requirements");
            }
        } else {
            // INDIVIDUAL jobs
            if (applicationRepository.findByWorkerIdAndJobId(worker.getId(), job.getId()).isPresent()) {
                throw new ValidationException("ALREADY_APPLIED", "You have already applied for this job");
            }
        }

        Application app = Application.builder()
                .job(job)
                .worker(worker)
                .team(applyingTeam)
                .status("Applied")
                .appliedDate(java.time.Instant.now().toString())
                .currentStep(1)
                .build();
        applicationRepository.save(app);

        return ApplyJobResponse.builder()
                .success(true)
                .message("SUCCESS")
                .applicationId(app.getId().toString())
                .build();
    }

    @Transactional(readOnly = true)
    @GetMapping("/team-jobs")
    public List<JobDetailDto> getTeamJobs(
            @RequestParam("teamId") Long teamId,
            @AuthenticationPrincipal UserDetails userDetails) {
        if (userDetails == null) {
            return new ArrayList<>();
        }

        User currentUser = userRepository.findByMobile(userDetails.getUsername()).orElse(null);
        if (currentUser == null || currentUser.getRole() != com.newtron.newtron_workforce_backend.auth.enums.Role.WORKER) {
            throw new com.newtron.newtron_workforce_backend.common.exception.ValidationException("UNAUTHORIZED_ACCESS", "Access denied.");
        }

        WorkerProfile profile = workerProfileRepository.findByUserId(currentUser.getId()).orElse(null);
        if (profile == null) {
            return new ArrayList<>();
        }

        // CRITICAL: Ensure real team ownership exists in DB
        com.newtron.newtron_workforce_backend.entity.Team team = teamRepository.findByIdAndOwnerWorkerProfileIdAndDeletedFalse(teamId, profile.getId())
                .orElseThrow(() -> new com.newtron.newtron_workforce_backend.common.exception.ValidationException("UNAUTHORIZED_ACCESS", "You do not own this team or it does not exist."));

        String skillName = team.getPrimarySkill().getName();
        List<com.newtron.newtron_workforce_backend.entity.Job> jobs = jobRepository.findActiveTeamJobsBySkill(skillName);
        List<Long> hiddenJobIds = notInterestedJobRepository.findHiddenJobIdsByWorkerId(currentUser.getId());

        List<JobDetailDto> dtos = new ArrayList<>();
        for (com.newtron.newtron_workforce_backend.entity.Job job : jobs) {
            if (hiddenJobIds.contains(job.getId())) {
                continue;
            }

            int required = job.getWorkersRequired() != null ? job.getWorkersRequired() : 1;
            long filledCount = applicationRepository.countByJobIdAndStatus(job.getId(), "Hired") +
                               applicationRepository.countByJobIdAndStatus(job.getId(), "Completed");
            if (filledCount >= required) {
                continue;
            }

            String companyName;
            if (job.getCompany() != null && job.getCompany().getCompanyName() != null) {
                companyName = job.getCompany().getCompanyName();
            } else {
                companyName = (job.getCompanyName() != null && !job.getCompanyName().trim().isEmpty())
                        ? job.getCompanyName().trim()
                        : ((job.getRecruiter() != null && job.getRecruiter().getFullName() != null)
                                ? job.getRecruiter().getFullName() : "Newtron Client");
            }

            Double wageVal = null;
            if (job.getSalary() != null) {
                try {
                    String cleanSalary = job.getSalary().replaceAll("[^0-9.]", "");
                    if (!cleanSalary.isEmpty()) {
                        wageVal = Double.parseDouble(cleanSalary);
                    }
                } catch (Exception e) {
                    wageVal = null;
                }
            }

            dtos.add(JobDetailDto.builder()
                    .id(job.getId().toString())
                    .company(companyName)
                    .companyLogo(null)
                    .jobTitle(job.getTitle())
                    .location(job.getCity())
                    .dailyWage(wageVal)
                    .employmentType(job.getDuration() != null ? job.getDuration() : "Full Time")
                    .distance(job.getDistance() != null ? job.getDistance() : "2.5 KM")
                    .postedTime("Posted recently")
                    .verified(false)
                    .rating(null)
                    .build());
        }

        return dtos;
    }
}
