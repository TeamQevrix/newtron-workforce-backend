package com.newtron.newtron_workforce_backend.service;

import com.newtron.newtron_workforce_backend.dto.TeamRegistrationResponse;
import com.newtron.newtron_workforce_backend.dto.TeamMemberResponse;
import com.newtron.newtron_workforce_backend.entity.Team;
import com.newtron.newtron_workforce_backend.repository.TeamRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RecruiterTeamService {

    private final TeamRepository teamRepository;

    @Transactional(readOnly = true)
    public Page<TeamRegistrationResponse> discoverTeams(
            String name,
            Long skillId,
            Long stateId,
            Long districtId,
            Long cityId,
            Pageable pageable) {

        Specification<Team> spec = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            predicates.add(cb.equal(root.get("deleted"), false));

            if (name != null && !name.trim().isEmpty()) {
                predicates.add(cb.like(cb.lower(root.get("teamName")), "%" + name.trim().toLowerCase() + "%"));
            }
            if (skillId != null) {
                predicates.add(cb.equal(root.get("primarySkill").get("id"), skillId));
            }
            if (stateId != null) {
                predicates.add(cb.equal(root.get("state").get("id"), stateId));
            }
            if (districtId != null) {
                predicates.add(cb.equal(root.get("district").get("id"), districtId));
            }
            if (cityId != null) {
                predicates.add(cb.equal(root.get("city").get("id"), cityId));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        Page<Team> teams = teamRepository.findAll(spec, pageable);

        return teams.map(this::mapToResponse);
    }

    private TeamRegistrationResponse mapToResponse(Team team) {
        List<TeamMemberResponse> memberResponses = team.getMembers().stream()
                .map(m -> TeamMemberResponse.builder()
                        .id(m.getId())
                        .fullName(m.getFullName())
                        .mobileNumber(m.getMobileNumber())
                        .primarySkillId(m.getPrimarySkill().getId())
                        .primarySkillName(m.getPrimarySkill().getName())
                        .experience(m.getExperience())
                        .build())
                .collect(Collectors.toList());

        return TeamRegistrationResponse.builder()
                .id(team.getId())
                .uuid(team.getUuid())
                .teamName(team.getTeamName())
                .ownerWorkerProfileId(team.getOwnerWorkerProfile().getId())
                .ownerName(team.getOwnerWorkerProfile().getFullName())
                .primarySkillId(team.getPrimarySkill().getId())
                .primarySkillName(team.getPrimarySkill().getName())
                .aboutTeam(team.getAboutTeam())
                .stateId(team.getState().getId())
                .stateName(team.getState().getName())
                .districtId(team.getDistrict().getId())
                .districtName(team.getDistrict().getName())
                .cityId(team.getCity().getId())
                .cityName(team.getCity().getName())
                .workAreaAddress(team.getWorkAreaAddress())
                .members(memberResponses)
                .build();
    }
}
