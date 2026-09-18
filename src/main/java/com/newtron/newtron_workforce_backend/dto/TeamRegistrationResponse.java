package com.newtron.newtron_workforce_backend.dto;

import lombok.*;

import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TeamRegistrationResponse {

    private Long id;
    private String uuid;
    private String teamName;
    private Long ownerWorkerProfileId;
    private String ownerName;
    private Long primarySkillId;
    private String primarySkillName;
    private String aboutTeam;
    private Long stateId;
    private String stateName;
    private Long districtId;
    private String districtName;
    private Long cityId;
    private String cityName;
    private String workAreaAddress;
    private List<TeamMemberResponse> members;
}
