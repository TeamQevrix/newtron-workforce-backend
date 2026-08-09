package com.newtron.newtron_workforce_backend.service;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.common.exception.ConflictException;
import com.newtron.newtron_workforce_backend.common.exception.ResourceNotFoundException;
import com.newtron.newtron_workforce_backend.dto.WorkerAddressRequest;
import com.newtron.newtron_workforce_backend.dto.WorkerAddressResponse;
import com.newtron.newtron_workforce_backend.entity.WorkerAddress;
import com.newtron.newtron_workforce_backend.entity.WorkerProfile;
import com.newtron.newtron_workforce_backend.enums.OnboardingStep;
import com.newtron.newtron_workforce_backend.repository.WorkerAddressRepository;
import com.newtron.newtron_workforce_backend.repository.WorkerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
public class WorkerAddressServiceImpl implements WorkerAddressService {

    private final WorkerAddressRepository workerAddressRepository;
    private final WorkerProfileRepository workerProfileRepository;
    private final OnboardingProgressService onboardingProgressService;

    @Override
    @Transactional(readOnly = true)
    public WorkerAddressResponse getAddress(User currentUser) {
        WorkerProfile profile = getProfile(currentUser);
        WorkerAddress address = workerAddressRepository.findByWorkerProfileId(profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("ADDRESS_NOT_FOUND", "Address details not found"));
        return mapToResponse(address);
    }

    @Override
    @Transactional
    public WorkerAddressResponse createAddress(WorkerAddressRequest request, User currentUser) {
        WorkerProfile profile = getProfile(currentUser);
        if (workerAddressRepository.existsByWorkerProfileId(profile.getId())) {
            throw new ConflictException("ADDRESS_ALREADY_EXISTS", "Address details already exist for this worker");
        }

        WorkerAddress address = WorkerAddress.builder()
                .workerProfile(profile)
                .stateId(request.getStateId())
                .districtId(request.getDistrictId())
                .cityId(request.getCityId())
                .areaVillage(request.getAreaVillage())
                .pincode(request.getPincode())
                .currentAddress(request.getCurrentAddress())
                .landmark(request.getLandmark())
                .latitude(request.getLatitude())
                .longitude(request.getLongitude())
                .build();

        WorkerAddress saved = workerAddressRepository.save(address);
        profile.setAddress(saved);

        // Update step status dynamically
        OnboardingStep nextStep = onboardingProgressService.getNextStep(profile);
        profile.setCurrentStep(nextStep);
        profile.setIsCompleted(nextStep == OnboardingStep.COMPLETED);
        workerProfileRepository.save(profile);

        log.info("Audit Trail: ADDRESS_CREATED - Address created successfully for profile: {}", profile.getId());
        return mapToResponse(saved);
    }

    @Override
    @Transactional
    public WorkerAddressResponse updateAddress(WorkerAddressRequest request, User currentUser) {
        WorkerProfile profile = getProfile(currentUser);
        WorkerAddress address = workerAddressRepository.findByWorkerProfileId(profile.getId())
                .orElseThrow(() -> new ResourceNotFoundException("ADDRESS_NOT_FOUND", "Address details not found"));

        address.setStateId(request.getStateId());
        address.setDistrictId(request.getDistrictId());
        address.setCityId(request.getCityId());
        address.setAreaVillage(request.getAreaVillage());
        address.setPincode(request.getPincode());
        address.setCurrentAddress(request.getCurrentAddress());
        address.setLandmark(request.getLandmark());
        address.setLatitude(request.getLatitude());
        address.setLongitude(request.getLongitude());

        WorkerAddress updated = workerAddressRepository.save(address);

        // Update step status dynamically
        OnboardingStep nextStep = onboardingProgressService.getNextStep(profile);
        profile.setCurrentStep(nextStep);
        profile.setIsCompleted(nextStep == OnboardingStep.COMPLETED);
        workerProfileRepository.save(profile);

        log.info("Audit Trail: ADDRESS_UPDATED - Address updated successfully for profile: {}", profile.getId());
        return mapToResponse(updated);
    }

    private WorkerProfile getProfile(User currentUser) {
        return workerProfileRepository.findByUserId(currentUser.getId())
                .orElseThrow(() -> new ResourceNotFoundException("PROFILE_NOT_FOUND", "Worker profile not found"));
    }

    private WorkerAddressResponse mapToResponse(WorkerAddress address) {
        return WorkerAddressResponse.builder()
                .id(address.getId())
                .stateId(address.getStateId())
                .districtId(address.getDistrictId())
                .cityId(address.getCityId())
                .areaVillage(address.getAreaVillage())
                .pincode(address.getPincode())
                .currentAddress(address.getCurrentAddress())
                .landmark(address.getLandmark())
                .latitude(address.getLatitude())
                .longitude(address.getLongitude())
                .build();
    }
}
