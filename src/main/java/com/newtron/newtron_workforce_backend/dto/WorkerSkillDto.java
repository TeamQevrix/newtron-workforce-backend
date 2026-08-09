package com.newtron.newtron_workforce_backend.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerSkillDto {

    @NotNull(message = "Skill ID is required")
    private Long skillId;

    private String skillName; // Output only

    @NotNull(message = "Primary flag is required")
    private Boolean isPrimary;

    @NotNull(message = "Experience years is required")
    @Min(value = 0, message = "Experience years cannot be negative")
    private Integer experienceYears;

    @NotNull(message = "Experience months is required")
    @Min(value = 0, message = "Experience months cannot be negative")
    @Max(value = 11, message = "Experience months must be between 0 and 11")
    private Integer experienceMonths;
}
