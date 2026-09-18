package com.newtron.newtron_workforce_backend.dto;

import lombok.*;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerDashboardResponse {
    private WorkerSummaryDto summary;
    private WorkerAvailabilityDto availability;
    private WorkerQuickActionsDto quickActions;
    private WorkerStatsDto stats;
    private List<WorkerActivityDto> recentActivities;
    private List<JobRecommendationDto> recommendedJobs;
}
