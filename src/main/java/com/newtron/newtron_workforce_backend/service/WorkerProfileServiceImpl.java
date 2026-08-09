package com.newtron.newtron_workforce_backend.service;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.common.exception.ConflictException;
import com.newtron.newtron_workforce_backend.common.exception.ResourceNotFoundException;
import com.newtron.newtron_workforce_backend.common.exception.ValidationException;
import com.newtron.newtron_workforce_backend.dto.WorkerBasicProfileRequest;
import com.newtron.newtron_workforce_backend.dto.WorkerBasicProfileResponse;
import com.newtron.newtron_workforce_backend.entity.WorkerProfile;
import com.newtron.newtron_workforce_backend.enums.OnboardingStep;
import com.newtron.newtron_workforce_backend.mapper.WorkerProfileMapper;
import com.newtron.newtron_workforce_backend.repository.WorkerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerProfileServiceImpl implements WorkerProfileService {

    private final WorkerProfileRepository workerProfileRepository;
    private final WorkerProfileMapper workerProfileMapper;
    private final OnboardingProgressService onboardingProgressService;
    private final StorageService storageService;
    private final Clock clock;

    @Override
    @Transactional(readOnly = true)
    public WorkerBasicProfileResponse getProfile(User currentUser) {
        WorkerProfile profile = getProfileEntity(currentUser);
        return mapToResponse(profile);
    }

    @Override
    @Transactional
    public WorkerBasicProfileResponse saveProfile(WorkerBasicProfileRequest request, User currentUser) {
        if (workerProfileRepository.existsByUserId(currentUser.getId())) {
            throw new ConflictException("USER_ALREADY_EXISTS", "Worker profile already exists for this user");
        }

        validateAge(request.getDateOfBirth());

        WorkerProfile profile = WorkerProfile.builder()
                .user(currentUser)
                .fullName(request.getFullName())
                .dateOfBirth(request.getDateOfBirth())
                .gender(request.getGender())
                .bloodGroup(request.getBloodGroup())
                .currentStep(OnboardingStep.PROFESSIONAL_DETAILS) // Advance currentStep to PROFESSIONAL_DETAILS
                .isCompleted(false)
                .build();

        WorkerProfile savedProfile = workerProfileRepository.save(profile);
        log.info("Audit Trail: PROFILE_CREATED - Profile created successfully for user: {}", currentUser.getId());

        return mapToResponse(savedProfile);
    }

    @Override
    @Transactional
    public WorkerBasicProfileResponse updateProfile(WorkerBasicProfileRequest request, User currentUser) {
        WorkerProfile profile = getProfileEntity(currentUser);

        validateAge(request.getDateOfBirth());

        profile.setFullName(request.getFullName());
        profile.setDateOfBirth(request.getDateOfBirth());
        profile.setGender(request.getGender());
        profile.setBloodGroup(request.getBloodGroup());

        // Update step status dynamically
        OnboardingStep nextStep = onboardingProgressService.getNextStep(profile);
        profile.setCurrentStep(nextStep);
        profile.setIsCompleted(nextStep == OnboardingStep.COMPLETED);

        WorkerProfile updatedProfile = workerProfileRepository.save(profile);
        log.info("Audit Trail: PROFILE_UPDATED - Profile updated successfully for user: {}", currentUser.getId());

        return mapToResponse(updatedProfile);
    }

    @Override
    @Transactional
    public WorkerBasicProfileResponse uploadProfilePhoto(MultipartFile file, User currentUser) {
        WorkerProfile profile = getProfileEntity(currentUser);

        String photoKey = storageService.uploadProfilePhoto(file);

        // Delete old photo if present
        if (profile.getPhotoStorageKey() != null) {
            storageService.deletePhoto(profile.getPhotoStorageKey());
        }

        profile.setPhotoStorageKey(photoKey);
        WorkerProfile updatedProfile = workerProfileRepository.save(profile);
        log.info("Audit Trail: PROFILE_PHOTO_UPDATED - Profile photo updated successfully for user: {}", currentUser.getId());

        return mapToResponse(updatedProfile);
    }

    private WorkerProfile getProfileEntity(User currentUser) {
        return workerProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("PROFILE_NOT_FOUND", "Worker profile not found for this user"));
    }

    private void validateAge(LocalDate dob) {
        if (dob == null) {
            throw new ValidationException("INVALID_DATE_OF_BIRTH", "Date of birth is required");
        }
        LocalDate now = LocalDate.now(clock);
        int age = Period.between(dob, now).getYears();
        if (age < 18 || age > 70) {
            throw new ValidationException("INVALID_AGE", "Age must be between 18 and 70 years old");
        }
    }

    private WorkerBasicProfileResponse mapToResponse(WorkerProfile profile) {
        WorkerBasicProfileResponse response = workerProfileMapper.toResponse(profile);
        response.setCompletionPercentage(onboardingProgressService.calculateCompletion(profile));
        response.setNextStep(onboardingProgressService.getNextStep(profile));
        return response;
    }
}
