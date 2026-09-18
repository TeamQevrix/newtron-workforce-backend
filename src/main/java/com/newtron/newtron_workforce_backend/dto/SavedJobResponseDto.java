package com.newtron.newtron_workforce_backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SavedJobResponseDto {
    private String id;
    private String savedAt;
    private JobDetailDto job;
}
