package com.newtron.newtron_workforce_backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkerQuickActionsDto {
    private Integer availableJobsCount;
    private Integer appliedJobsCount;
    private Integer savedJobsCount;
    private Integer unreadAlertsCount;
}
