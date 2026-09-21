package com.newtron.newtron_workforce_backend.service;

import com.newtron.newtron_workforce_backend.dto.NearbyWorkerResponse;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import com.newtron.newtron_workforce_backend.dto.PublicWorkerProfileDto;

public interface PublicWorkerService {
    Page<NearbyWorkerResponse> findNearbyAvailableIndividualWorkers(Double latitude, Double longitude, Double radiusKm, Long skillId, Pageable pageable);
    PublicWorkerProfileDto getPublicWorkerProfile(Long workerId);
}
