package com.newtron.newtron_workforce_backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerActivityDto {
    private String id;
    private String title;
    private String subtitle;
    private String timestamp;
    private String activityType;
}
