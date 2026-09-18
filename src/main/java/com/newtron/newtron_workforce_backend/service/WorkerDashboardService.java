package com.newtron.newtron_workforce_backend.service;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.dto.WorkerSummaryDto;

import com.newtron.newtron_workforce_backend.dto.WorkerAvailabilityDto;
import com.newtron.newtron_workforce_backend.dto.WorkerQuickActionsDto;
import com.newtron.newtron_workforce_backend.dto.WorkerStatsDto;
import com.newtron.newtron_workforce_backend.dto.WorkerActivityDto;
import com.newtron.newtron_workforce_backend.dto.JobRecommendationDto;
import com.newtron.newtron_workforce_backend.dto.WorkerDashboardResponse;
import java.util.List;

public interface WorkerDashboardService {
    WorkerSummaryDto getWorkerSummary(User currentUser);
    WorkerAvailabilityDto getWorkerAvailability(User currentUser);
    WorkerAvailabilityDto updateWorkerAvailability(User currentUser, WorkerAvailabilityDto request);
    WorkerQuickActionsDto getQuickActions(User currentUser);
    WorkerStatsDto getWorkerStats(User currentUser);
    List<WorkerActivityDto> getRecentActivities(User currentUser);
    List<JobRecommendationDto> getRecommendedJobs(User currentUser);
    WorkerDashboardResponse getWorkerDashboard(User currentUser);
}
