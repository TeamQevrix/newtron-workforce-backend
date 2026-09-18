package com.newtron.newtron_workforce_backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TimelineStepDto {
    private String title;
    private String description;
    private String timestamp;
    private boolean completed;
}
