package com.newtron.newtron_workforce_backend.service;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.enums.Role;
import com.newtron.newtron_workforce_backend.common.exception.ConflictException;
import com.newtron.newtron_workforce_backend.common.exception.ForbiddenException;
import com.newtron.newtron_workforce_backend.common.exception.ResourceNotFoundException;
import com.newtron.newtron_workforce_backend.common.exception.ValidationException;
import com.newtron.newtron_workforce_backend.dto.TeamRegistrationRequest;
import com.newtron.newtron_workforce_backend.dto.TeamRegistrationResponse;
import com.newtron.newtron_workforce_backend.dto.TeamMemberRequest;
import com.newtron.newtron_workforce_backend.dto.TeamMemberResponse;
import com.newtron.newtron_workforce_backend.entity.*;
import com.newtron.newtron_workforce_backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerTeamServiceImpl implements WorkerTeamService {

    private final TeamRepository teamRepository;
    private final TeamMemberRepository teamMemberRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final SkillRepository skillRepository;
    private final MasterStateRepository stateRepository;
    private final MasterDistrictRepository districtRepository;
    private final MasterCityRepository cityRepository;
    private final com.newtron.newtron_workforce_backend.auth.repository.UserRepository userRepository;

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamRegistrationResponse registerTeam(TeamRegistrationRequest request, User currentUser) {
        log.info("Starting team registration validation for user: {}", currentUser.getMobile());

        // 1. Role Check
        if (currentUser.getRole() != Role.WORKER) {
            throw new ForbiddenException("INVALID_ROLE", "Only workers are permitted to register teams.");
        }

        // 2. Resolve WorkerProfile
        WorkerProfile ownerProfile = workerProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("WORKER_PROFILE_NOT_FOUND", "Worker profile setup required first."));

        if (ownerProfile.isDeleted()) {
            throw new ResourceNotFoundException("WORKER_PROFILE_NOT_FOUND", "Worker profile is inactive or deleted.");
        }

        // Active Team check removed to allow multiple teams per owner

        // 4. Resolve & Validate Skill
        Skill primarySkill = skillRepository.findById(request.getPrimarySkillId())
                .orElseThrow(() -> new ValidationException("INVALID_SKILL", "Primary category skill ID not found."));

        // 5. Resolve Geography Master entries
        MasterState state = stateRepository.findById(request.getStateId())
                .orElseThrow(() -> new ValidationException("INVALID_STATE", "State ID not found."));

        MasterDistrict district = districtRepository.findById(request.getDistrictId())
                .orElseThrow(() -> new ValidationException("INVALID_DISTRICT", "District ID not found."));

        MasterCity city = cityRepository.findById(request.getCityId())
                .orElseThrow(() -> new ValidationException("INVALID_CITY", "City ID not found."));

        // 6. Validate Location Hierarchy
        if (!district.getState().getId().equals(state.getId())) {
            throw new ValidationException("LOCATION_MISMATCH", "The selected district does not belong to the selected state.");
        }

        if (!city.getDistrict().getId().equals(district.getId())) {
            throw new ValidationException("LOCATION_MISMATCH", "The selected city does not belong to the selected district.");
        }

        // 7. Validate and map members
        if (request.getMembers() == null || request.getMembers().isEmpty()) {
            throw new ValidationException("MEMBERS_REQUIRED", "At least one team member is required.");
        }

        Set<String> uniqueMobiles = new HashSet<>();
        List<TeamMember> teamMembers = new ArrayList<>();

        Team team = Team.builder()
                .teamName(request.getTeamName().trim())
                .primarySkill(primarySkill)
                .aboutTeam(request.getAboutTeam() != null ? request.getAboutTeam().trim() : null)
                .state(state)
                .district(district)
                .city(city)
                .workAreaAddress(request.getWorkAreaAddress().trim())
                .ownerWorkerProfile(ownerProfile)
                .build();

        for (TeamMemberRequest memReq : request.getMembers()) {
            String mobile = memReq.getMobileNumber().trim();

            // Request level duplicate mobile check
            if (!uniqueMobiles.add(mobile)) {
                throw new ConflictException("DUPLICATE_TEAM_MEMBER", "Duplicate mobile number inside the member list: " + mobile);
            }

            // Check if member mobile is owner's mobile
            if (mobile.equals(currentUser.getMobile())) {
                throw new ValidationException("INVALID_MEMBER_MOBILE", "Team Owner cannot be added as a team member: " + mobile);
            }

            // Skill validation
            Skill memSkill = skillRepository.findById(memReq.getPrimarySkillId())
                    .orElseThrow(() -> new ValidationException("INVALID_SKILL", "Skill ID not found for member: " + memReq.getFullName()));

            TeamMember member = TeamMember.builder()
                    .team(team)
                    .fullName(memReq.getFullName().trim())
                    .mobileNumber(mobile)
                    .primarySkill(memSkill)
                    .experience(memReq.getExperience() != null ? memReq.getExperience().trim() : null)
                    .build();

            teamMembers.add(member);
        }

        team.setMembers(new LinkedHashSet<>(teamMembers));

        // 8. Persist
        Team savedTeam = teamRepository.save(team);
        log.info("Audit Trail: TEAM_REGISTERED - Team registered successfully. ID: {}, UUID: {}", savedTeam.getId(), savedTeam.getUuid());

        // 8b. Update owner WorkerProfile onboarding status to COMPLETED
        ownerProfile.setCurrentStep(com.newtron.newtron_workforce_backend.enums.OnboardingStep.COMPLETED);
        ownerProfile.setIsCompleted(true);
        workerProfileRepository.save(ownerProfile);
        log.info("Audit Trail: WORKER_ONBOARDING_COMPLETED - Worker onboarding completed dynamically after team registration for profile: {}", ownerProfile.getId());

        // 8c. Update User profileCompleted status to true so JWT tokens after login reflect completion
        currentUser.setProfileCompleted(true);
        userRepository.save(currentUser);
        log.info("Audit Trail: USER_PROFILE_COMPLETED - User profile completed dynamically after team registration for user: {}", currentUser.getId());

        // 9. Map and return response DTO
        List<TeamMemberResponse> memberResponses = savedTeam.getMembers().stream()
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
                .id(savedTeam.getId())
                .uuid(savedTeam.getUuid())
                .teamName(savedTeam.getTeamName())
                .ownerWorkerProfileId(ownerProfile.getId())
                .ownerName(ownerProfile.getFullName())
                .primarySkillId(savedTeam.getPrimarySkill().getId())
                .primarySkillName(savedTeam.getPrimarySkill().getName())
                .aboutTeam(savedTeam.getAboutTeam())
                .stateId(savedTeam.getState().getId())
                .stateName(savedTeam.getState().getName())
                .districtId(savedTeam.getDistrict().getId())
                .districtName(savedTeam.getDistrict().getName())
                .cityId(savedTeam.getCity().getId())
                .cityName(savedTeam.getCity().getName())
                .workAreaAddress(savedTeam.getWorkAreaAddress())
                .members(memberResponses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public TeamRegistrationResponse getMyTeam(User currentUser) {
        log.info("Fetching team details for user: {}", currentUser.getMobile());

        if (currentUser.getRole() != Role.WORKER) {
            throw new ForbiddenException("INVALID_ROLE", "Only workers are permitted to view teams.");
        }

        WorkerProfile ownerProfile = workerProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("WORKER_PROFILE_NOT_FOUND", "Worker profile not found."));

        if (ownerProfile.isDeleted()) {
            throw new ResourceNotFoundException("WORKER_PROFILE_NOT_FOUND", "Worker profile is inactive or deleted.");
        }

        List<Team> teams = teamRepository.findAllByOwnerWorkerProfileId(ownerProfile.getId());
        if (teams.isEmpty()) {
            throw new ResourceNotFoundException("TEAM_NOT_FOUND", "No active team found for this worker.");
        }
        Team team = teams.get(0);

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
                .ownerWorkerProfileId(ownerProfile.getId())
                .ownerName(ownerProfile.getFullName())
                .primarySkillId(team.getPrimarySkill().getId())
                .primarySkillName(team.getPrimarySkill().getName())
                .aboutTeam(team.getAboutTeam())
                .stateId(team.getState().getId())
                .stateName(team.getState().getName())
                .districtId(team.getDistrict().getId())
                .districtName(team.getDistrict().getName())
                .cityId(team.getCity().getId())
                .cityName(team.getCity().getName())
                .members(memberResponses)
                .build();
    }

    @Override
    @Transactional(readOnly = true)
    public List<TeamRegistrationResponse> getMyTeams(User currentUser) {
        log.info("Fetching all teams for user: {}", currentUser.getMobile());

        if (currentUser.getRole() != Role.WORKER) {
            throw new ForbiddenException("INVALID_ROLE", "Only workers are permitted to view teams.");
        }

        WorkerProfile ownerProfile = workerProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("WORKER_PROFILE_NOT_FOUND", "Worker profile not found."));

        if (ownerProfile.isDeleted()) {
            throw new ResourceNotFoundException("WORKER_PROFILE_NOT_FOUND", "Worker profile is inactive or deleted.");
        }

        List<Team> teams = teamRepository.findAllByOwnerWorkerProfileId(ownerProfile.getId());

        return teams.stream().map(team -> {
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
                    .ownerWorkerProfileId(ownerProfile.getId())
                    .ownerName(ownerProfile.getFullName())
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
        }).collect(Collectors.toList());
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamRegistrationResponse updateTeam(Long teamId, com.newtron.newtron_workforce_backend.dto.TeamUpdateRequest request, User currentUser) {
        log.info("Updating team details for user: {} teamId: {}", currentUser.getMobile(), teamId);

        if (currentUser.getRole() != Role.WORKER) {
            throw new ForbiddenException("INVALID_ROLE", "Only workers are permitted to update teams.");
        }

        WorkerProfile ownerProfile = workerProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("WORKER_PROFILE_NOT_FOUND", "Worker profile not found."));

        if (ownerProfile.isDeleted()) {
            throw new ResourceNotFoundException("WORKER_PROFILE_NOT_FOUND", "Worker profile is inactive or deleted.");
        }

        Team team = teamRepository.findByIdAndOwnerWorkerProfileIdAndDeletedFalse(teamId, ownerProfile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("TEAM_NOT_FOUND", "Team not found or not owned by the authenticated user."));

        Skill primarySkill = skillRepository.findById(request.getPrimarySkillId())
                .orElseThrow(() -> new ValidationException("INVALID_SKILL", "Primary skill ID not found."));

        team.setTeamName(request.getTeamName().trim());
        team.setPrimarySkill(primarySkill);
        team.setAboutTeam(request.getAboutTeam() != null ? request.getAboutTeam().trim() : null);

        Team savedTeam = teamRepository.save(team);
        log.info("Audit Trail: TEAM_UPDATED - Team updated successfully. ID: {}", savedTeam.getId());

        List<TeamMemberResponse> memberResponses = savedTeam.getMembers().stream()
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
                .id(savedTeam.getId())
                .uuid(savedTeam.getUuid())
                .teamName(savedTeam.getTeamName())
                .ownerWorkerProfileId(ownerProfile.getId())
                .ownerName(ownerProfile.getFullName())
                .primarySkillId(savedTeam.getPrimarySkill().getId())
                .primarySkillName(savedTeam.getPrimarySkill().getName())
                .aboutTeam(savedTeam.getAboutTeam())
                .stateId(savedTeam.getState().getId())
                .stateName(savedTeam.getState().getName())
                .districtId(savedTeam.getDistrict().getId())
                .districtName(savedTeam.getDistrict().getName())
                .cityId(savedTeam.getCity().getId())
                .cityName(savedTeam.getCity().getName())
                .workAreaAddress(savedTeam.getWorkAreaAddress())
                .members(memberResponses)
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public TeamMemberResponse addTeamMember(Long teamId, TeamMemberRequest request, User currentUser) {
        log.info("Adding team member for user: {} teamId: {}", currentUser.getMobile(), teamId);

        if (currentUser.getRole() != Role.WORKER) {
            throw new ForbiddenException("INVALID_ROLE", "Only workers are permitted to update teams.");
        }

        WorkerProfile ownerProfile = workerProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("WORKER_PROFILE_NOT_FOUND", "Worker profile not found."));

        if (ownerProfile.isDeleted()) {
            throw new ResourceNotFoundException("WORKER_PROFILE_NOT_FOUND", "Worker profile is inactive or deleted.");
        }

        Team team = teamRepository.findByIdAndOwnerWorkerProfileIdAndDeletedFalse(teamId, ownerProfile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("TEAM_NOT_FOUND", "Team not found or not owned by the authenticated user."));

        Skill memSkill = skillRepository.findById(request.getPrimarySkillId())
                .orElseThrow(() -> new ValidationException("INVALID_SKILL", "Skill ID not found for member: " + request.getFullName()));

        TeamMember member = TeamMember.builder()
                .team(team)
                .fullName(request.getFullName().trim())
                .mobileNumber(request.getMobileNumber().trim())
                .primarySkill(memSkill)
                .experience(request.getExperience() != null ? request.getExperience().trim() : null)
                .build();

        TeamMember savedMember = teamMemberRepository.save(member);
        
        return TeamMemberResponse.builder()
                .id(savedMember.getId())
                .fullName(savedMember.getFullName())
                .mobileNumber(savedMember.getMobileNumber())
                .primarySkillId(savedMember.getPrimarySkill().getId())
                .primarySkillName(savedMember.getPrimarySkill().getName())
                .experience(savedMember.getExperience())
                .build();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public void removeTeamMember(Long teamId, Long memberId, User currentUser) {
        log.info("Removing team member for user: {} teamId: {} memberId: {}", currentUser.getMobile(), teamId, memberId);

        if (currentUser.getRole() != Role.WORKER) {
            throw new ForbiddenException("INVALID_ROLE", "Only workers are permitted to update teams.");
        }

        WorkerProfile ownerProfile = workerProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("WORKER_PROFILE_NOT_FOUND", "Worker profile not found."));

        if (ownerProfile.isDeleted()) {
            throw new ResourceNotFoundException("WORKER_PROFILE_NOT_FOUND", "Worker profile is inactive or deleted.");
        }

        Team team = teamRepository.findByIdAndOwnerWorkerProfileIdAndDeletedFalse(teamId, ownerProfile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("TEAM_NOT_FOUND", "Team not found or not owned by the authenticated user."));

        TeamMember member = teamMemberRepository.findByTeamIdAndId(teamId, memberId)
                .orElseThrow(() -> new ResourceNotFoundException("MEMBER_NOT_FOUND", "Member not found in this team."));

        member.delete();
        teamMemberRepository.save(member);
    }
}
