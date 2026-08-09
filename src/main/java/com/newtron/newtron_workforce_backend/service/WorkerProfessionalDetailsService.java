package com.newtron.newtron_workforce_backend.service;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.dto.WorkerProfessionalDetailsRequest;
import com.newtron.newtron_workforce_backend.dto.WorkerProfessionalDetailsResponse;

public interface WorkerProfessionalDetailsService {
    WorkerProfessionalDetailsResponse getProfessionalDetails(User currentUser);
    WorkerProfessionalDetailsResponse createProfessionalDetails(WorkerProfessionalDetailsRequest request, User currentUser);
    WorkerProfessionalDetailsResponse updateProfessionalDetails(WorkerProfessionalDetailsRequest request, User currentUser);
}
