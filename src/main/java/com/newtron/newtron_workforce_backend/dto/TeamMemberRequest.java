package com.newtron.newtron_workforce_backend.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamMemberRequest {

    @NotBlank(message = "Member full name is required")
    @Size(max = 100, message = "Member name must not exceed 100 characters")
    private String fullName;

    @NotBlank(message = "Member mobile number is required")
    @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "Invalid member mobile number"
    )
    private String mobileNumber;

    @NotNull(message = "Member primary skill ID is required")
    @Positive(message = "Invalid primary skill ID")
    private Long primarySkillId;

    @Size(max = 50, message = "Experience info must not exceed 50 characters")
    private String experience;
}
