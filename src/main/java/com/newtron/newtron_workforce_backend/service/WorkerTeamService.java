package com.newtron.newtron_workforce_backend.service;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.dto.TeamRegistrationRequest;
import com.newtron.newtron_workforce_backend.dto.TeamRegistrationResponse;

public interface WorkerTeamService {

    TeamRegistrationResponse registerTeam(TeamRegistrationRequest request, User currentUser);

    TeamRegistrationResponse getMyTeam(User currentUser); // Kept for legacy compatibility temporarily

    java.util.List<TeamRegistrationResponse> getMyTeams(User currentUser);

    TeamRegistrationResponse updateTeam(Long teamId, com.newtron.newtron_workforce_backend.dto.TeamUpdateRequest request, User currentUser);

    com.newtron.newtron_workforce_backend.dto.TeamMemberResponse addTeamMember(Long teamId, com.newtron.newtron_workforce_backend.dto.TeamMemberRequest request, User currentUser);

    void removeTeamMember(Long teamId, Long memberId, User currentUser);
}
