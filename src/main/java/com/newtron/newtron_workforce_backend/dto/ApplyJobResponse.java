package com.newtron.newtron_workforce_backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ApplyJobResponse {
    private boolean success;
    private String message;
    private String applicationId;
}
