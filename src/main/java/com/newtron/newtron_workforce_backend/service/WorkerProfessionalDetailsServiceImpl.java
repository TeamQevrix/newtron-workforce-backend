package com.newtron.newtron_workforce_backend.service;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.common.exception.*;
import com.newtron.newtron_workforce_backend.dto.*;
import com.newtron.newtron_workforce_backend.entity.*;
import com.newtron.newtron_workforce_backend.enums.*;
import com.newtron.newtron_workforce_backend.mapper.WorkerProfessionalDetailsMapper;
import com.newtron.newtron_workforce_backend.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerProfessionalDetailsServiceImpl implements WorkerProfessionalDetailsService {

    private final WorkerProfileRepository workerProfileRepository;
    private final WorkerProfessionalDetailsRepository workerProfessionalDetailsRepository;
    private final WorkerSkillRepository workerSkillRepository;
    private final SkillRepository skillRepository;
    private final QualificationRepository qualificationRepository;
    private final WorkerProfessionalDetailsMapper mapper;
    private final OnboardingProgressService onboardingProgressService;

    @Override
    @Transactional(readOnly = true)
    public WorkerProfessionalDetailsResponse getProfessionalDetails(User currentUser) {
        WorkerProfile profile = getProfileEntity(currentUser);
        WorkerProfessionalDetails details = workerProfessionalDetailsRepository.findByWorkerProfileId(profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("PROFESSIONAL_DETAILS_NOT_FOUND", "Professional details not found for this user"));

        List<WorkerSkill> skills = workerSkillRepository.findByWorkerProfileId(profile.getId());
        return mapToResponse(details, skills, profile);
    }

    @Override
    @Transactional
    public WorkerProfessionalDetailsResponse createProfessionalDetails(WorkerProfessionalDetailsRequest request, User currentUser) {
        WorkerProfile profile = getProfileEntity(currentUser);

        if (workerProfessionalDetailsRepository.existsByWorkerProfileId(profile.getId())) {
            throw new ConflictException("PROFESSIONAL_DETAILS_ALREADY_EXISTS", "Professional details already exist for this user");
        }

        validateRequest(request);

        Qualification qualification = qualificationRepository.findById(request.getHighestQualificationId())
                .orElseThrow(() -> new ResourceNotFoundException("QUALIFICATION_NOT_FOUND", "Qualification not found"));

        WorkerProfessionalDetails details = WorkerProfessionalDetails.builder()
                .workerProfile(profile)
                .highestQualification(qualification)
                .currentEmploymentStatus(request.getCurrentEmploymentStatus())
                .salaryType(request.getSalaryType())
                .expectedSalary(request.getExpectedSalary())
                .expectedMonthlySalary(request.getExpectedMonthlySalary())
                .preferredWorkType(request.getPreferredWorkType())
                .preferredShift(request.getPreferredShift())
                .immediateJoining(request.getImmediateJoining())
                .currentCompany(request.getCurrentCompany())
                .currentDesignation(request.getCurrentDesignation())
                .noticePeriodDays(request.getNoticePeriodDays() != null ? request.getNoticePeriodDays() : 0)
                .build();

        WorkerProfessionalDetails savedDetails = workerProfessionalDetailsRepository.save(details);
        profile.setProfessionalDetails(savedDetails);

        // Save skills
        saveSkills(request.getSkills(), profile);

        // Update step status dynamically to ADDRESS
        profile.setCurrentStep(OnboardingStep.ADDRESS);
        workerProfileRepository.save(profile);

        log.info("Audit Trail: PROFESSIONAL_DETAILS_CREATED - Professional details created for user: {}", currentUser.getId());

        List<WorkerSkill> savedSkills = workerSkillRepository.findByWorkerProfileId(profile.getId());
        return mapToResponse(savedDetails, savedSkills, profile);
    }

    @Override
    @Transactional
    public WorkerProfessionalDetailsResponse updateProfessionalDetails(WorkerProfessionalDetailsRequest request, User currentUser) {
        WorkerProfile profile = getProfileEntity(currentUser);
        WorkerProfessionalDetails details = workerProfessionalDetailsRepository.findByWorkerProfileId(profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("PROFESSIONAL_DETAILS_NOT_FOUND", "Professional details not found for this user"));

        validateRequest(request);

        Qualification qualification = qualificationRepository.findById(request.getHighestQualificationId())
                .orElseThrow(() -> new ResourceNotFoundException("QUALIFICATION_NOT_FOUND", "Qualification not found"));

        details.setHighestQualification(qualification);
        details.setCurrentEmploymentStatus(request.getCurrentEmploymentStatus());
        details.setSalaryType(request.getSalaryType());
        details.setExpectedSalary(request.getExpectedSalary());
        details.setExpectedMonthlySalary(request.getExpectedMonthlySalary());
        details.setPreferredWorkType(request.getPreferredWorkType());
        details.setPreferredShift(request.getPreferredShift());
        details.setImmediateJoining(request.getImmediateJoining());
        details.setCurrentCompany(request.getCurrentCompany());
        details.setCurrentDesignation(request.getCurrentDesignation());
        details.setNoticePeriodDays(request.getNoticePeriodDays() != null ? request.getNoticePeriodDays() : 0);

        WorkerProfessionalDetails updatedDetails = workerProfessionalDetailsRepository.save(details);

        // Update skills: delete existing and save new
        workerSkillRepository.deleteByWorkerProfileId(profile.getId());
        saveSkills(request.getSkills(), profile);

        // Recalculate progress using onboarding progress service
        OnboardingStep nextStep = onboardingProgressService.getNextStep(profile);
        profile.setCurrentStep(nextStep);
        profile.setIsCompleted(nextStep == OnboardingStep.COMPLETED);
        workerProfileRepository.save(profile);

        log.info("Audit Trail: PROFESSIONAL_DETAILS_UPDATED - Professional details updated for user: {}", currentUser.getId());

        List<WorkerSkill> savedSkills = workerSkillRepository.findByWorkerProfileId(profile.getId());
        return mapToResponse(updatedDetails, savedSkills, profile);
    }

    private WorkerProfile getProfileEntity(User currentUser) {
        return workerProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("PROFILE_NOT_FOUND", "Worker profile not found for this user"));
    }

    private void validateRequest(WorkerProfessionalDetailsRequest request) {
        // Exactly one primary skill
        long primaryCount = request.getSkills().stream().filter(WorkerSkillDto::getIsPrimary).count();
        if (primaryCount != 1) {
            throw new ValidationException("INVALID_SKILLS", "Exactly one primary skill must be specified");
        }

        // Immediate joining -> Notice period must be 0
        if (Boolean.TRUE.equals(request.getImmediateJoining()) && request.getNoticePeriodDays() != null && request.getNoticePeriodDays() != 0) {
            throw new ValidationException("INVALID_NOTICE_PERIOD", "Notice period days must be 0 if immediate joining is enabled");
        }

        // Employed status -> Company and designation are required
        if (request.getCurrentEmploymentStatus() == EmploymentStatus.EMPLOYED) {
            if (request.getCurrentCompany() == null || request.getCurrentCompany().trim().isEmpty()) {
                throw new ValidationException("MISSING_COMPANY", "Current company is required when employment status is EMPLOYED");
            }
            if (request.getCurrentDesignation() == null || request.getCurrentDesignation().trim().isEmpty()) {
                throw new ValidationException("MISSING_DESIGNATION", "Current designation is required when employment status is EMPLOYED");
            }
        }
    }

    private void saveSkills(List<WorkerSkillDto> skillDtos, WorkerProfile profile) {
        for (WorkerSkillDto dto : skillDtos) {
            Skill skill = skillRepository.findById(dto.getSkillId())
                    .orElseThrow(() -> new ResourceNotFoundException("SKILL_NOT_FOUND", "Skill not found with ID: " + dto.getSkillId()));

            WorkerSkill workerSkill = WorkerSkill.builder()
                    .workerProfile(profile)
                    .skill(skill)
                    .isPrimary(dto.getIsPrimary())
                    .experienceYears(dto.getExperienceYears())
                    .experienceMonths(dto.getExperienceMonths())
                    .specialization(dto.getSpecialization())
                    .build();

            workerSkillRepository.save(workerSkill);
        }
    }

    private WorkerProfessionalDetailsResponse mapToResponse(WorkerProfessionalDetails details, List<WorkerSkill> skills, WorkerProfile profile) {
        WorkerProfessionalDetailsResponse response = mapper.toResponse(details);
        response.setSkills(skills.stream().map(mapper::toSkillDto).collect(Collectors.toList()));
        response.setCompletionPercentage(onboardingProgressService.calculateCompletion(profile));
        response.setNextStep(onboardingProgressService.getNextStep(profile));
        response.setCurrentStep(profile.getCurrentStep());
        return response;
    }
}
