package com.newtron.newtron_workforce_backend.service;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.dto.WorkerBasicProfileRequest;
import com.newtron.newtron_workforce_backend.dto.WorkerBasicProfileResponse;
import org.springframework.web.multipart.MultipartFile;

public interface WorkerProfileService {
    WorkerBasicProfileResponse getProfile(User currentUser);
    WorkerBasicProfileResponse saveProfile(WorkerBasicProfileRequest request, User currentUser);
    WorkerBasicProfileResponse updateProfile(WorkerBasicProfileRequest request, User currentUser);
    WorkerBasicProfileResponse uploadProfilePhoto(MultipartFile file, User currentUser);
    com.newtron.newtron_workforce_backend.dto.OnDemandAvailabilityResponse updateOnDemandAvailability(com.newtron.newtron_workforce_backend.dto.OnDemandAvailabilityRequest request, User currentUser);
}
