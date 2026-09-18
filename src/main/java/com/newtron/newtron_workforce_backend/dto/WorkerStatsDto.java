package com.newtron.newtron_workforce_backend.dto;

import com.fasterxml.jackson.annotation.JsonInclude;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class WorkerStatsDto {
    private Double rating;
    private Integer completedJobsCount;
    private Double todayEarnings;
    private Double totalEarnings;
}
