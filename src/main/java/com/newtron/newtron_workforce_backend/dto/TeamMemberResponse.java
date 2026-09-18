package com.newtron.newtron_workforce_backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamMemberResponse {

    private Long id;
    private String fullName;
    private String mobileNumber;
    private Long primarySkillId;
    private String primarySkillName;
    private String experience;
}
