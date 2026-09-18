package com.newtron.newtron_workforce_backend.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.*;
import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamRegistrationRequest {

    @NotBlank(message = "Team name is required")
    @Size(max = 100, message = "Team name must not exceed 100 characters")
    private String teamName;

    @NotNull(message = "Primary skill ID is required")
    @Positive(message = "Invalid primary skill ID")
    private Long primarySkillId;

    private String aboutTeam;

    @NotNull(message = "State ID is required")
    @Positive(message = "Invalid State ID")
    private Long stateId;

    @NotNull(message = "District ID is required")
    @Positive(message = "Invalid District ID")
    private Long districtId;

    @NotNull(message = "City ID is required")
    @Positive(message = "Invalid City ID")
    private Long cityId;

    @NotBlank(message = "Work area address is required")
    @Size(max = 255, message = "Work area address must not exceed 255 characters")
    private String workAreaAddress;

    @NotEmpty(message = "At least one team member is required")
    @Valid
    private List<TeamMemberRequest> members;
}
