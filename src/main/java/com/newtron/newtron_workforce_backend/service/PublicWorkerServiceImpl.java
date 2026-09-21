package com.newtron.newtron_workforce_backend.service;

import com.newtron.newtron_workforce_backend.dto.NearbyWorkerProjection;
import com.newtron.newtron_workforce_backend.dto.NearbyWorkerResponse;
import com.newtron.newtron_workforce_backend.repository.WorkerProfileRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
public class PublicWorkerServiceImpl implements PublicWorkerService {

    private final WorkerProfileRepository workerProfileRepository;

    @Override
    public Page<NearbyWorkerResponse> findNearbyAvailableIndividualWorkers(Double latitude, Double longitude, Double radiusKm, Long skillId, Pageable pageable) {
        Double activeRadiusKm = (radiusKm != null && radiusKm > 0) ? Math.min(radiusKm, 10.0) : 10.0;
        
        Page<NearbyWorkerProjection> projections = workerProfileRepository.findNearbyAvailableIndividualWorkers(
                latitude, longitude, activeRadiusKm, skillId, pageable);
        
        return projections.map(projection -> NearbyWorkerResponse.builder()
                .workerId(projection.getWorkerId())
                .fullName(projection.getFullName())
                .photoStorageKey(projection.getPhotoStorageKey())
                .mainSkill(projection.getMainSkill())
                .experienceYears(projection.getExperienceYears())
                .distanceKm(projection.getDistanceKm())
                .isAvailableOnDemand(projection.getIsAvailableOnDemand())
                .build());
    }

    @Override
    @org.springframework.transaction.annotation.Transactional(readOnly = true)
    public com.newtron.newtron_workforce_backend.dto.PublicWorkerProfileDto getPublicWorkerProfile(Long workerId) {
        com.newtron.newtron_workforce_backend.entity.WorkerProfile profile = workerProfileRepository.findEligiblePublicWorkerProfileById(workerId)
                .orElseThrow(() -> new com.newtron.newtron_workforce_backend.common.exception.ResourceNotFoundException("WORKER_NOT_FOUND", "Worker profile not found or ineligible"));

        String mainSkill = null;
        String experienceYears = null;
        java.util.List<String> skillNames = new java.util.ArrayList<>();
        
        if (profile.getSkills() != null) {
            for (com.newtron.newtron_workforce_backend.entity.WorkerSkill ws : profile.getSkills()) {
                if (ws.getSkill() != null) {
                    skillNames.add(ws.getSkill().getName());
                }
                if (Boolean.TRUE.equals(ws.getIsPrimary())) {
                    if (ws.getSkill() != null) {
                        mainSkill = ws.getSkill().getName();
                    }
                    if (ws.getExperienceYears() != null) {
                        experienceYears = ws.getExperienceYears() + " Yr";
                    }
                }
            }
            if (mainSkill == null && !profile.getSkills().isEmpty()) {
                com.newtron.newtron_workforce_backend.entity.WorkerSkill ws = profile.getSkills().iterator().next();
                if (ws.getSkill() != null) {
                    mainSkill = ws.getSkill().getName();
                }
                if (ws.getExperienceYears() != null) {
                    experienceYears = ws.getExperienceYears() + " Yr";
                }
            }
        }

        String generalLocation = null;
        if (profile.getAddress() != null) {
            StringBuilder locBuilder = new StringBuilder();
            if (profile.getAddress().getAreaVillage() != null) {
                locBuilder.append(profile.getAddress().getAreaVillage());
            }
            // Add city/district if required, but area_village is already broad enough.
            // Using areaVillage as generalLocation.
            generalLocation = locBuilder.toString().isEmpty() ? null : locBuilder.toString();
        }

        return com.newtron.newtron_workforce_backend.dto.PublicWorkerProfileDto.builder()
                .workerId(profile.getId())
                .fullName(profile.getFullName())
                .photoStorageKey(profile.getPhotoStorageKey())
                .mainSkill(mainSkill)
                .skills(skillNames)
                .experienceYears(experienceYears)
                .isAvailableOnDemand(profile.getIsAvailableOnDemand())
                .generalLocation(generalLocation)
                .build();
    }
}
