package com.newtron.newtron_workforce_backend.service;

import com.newtron.newtron_workforce_backend.entity.WorkerProfile;
import com.newtron.newtron_workforce_backend.enums.OnboardingStep;
import org.springframework.stereotype.Service;

@Service
public class OnboardingProgressServiceImpl implements OnboardingProgressService {

    @Override
    public double calculateCompletion(WorkerProfile profile) {
        if (profile == null) {
            return 0.0;
        }

        double percentage = 20.0; // Basic profile is completed since profile exists

        if (profile.getProfessionalDetails() != null) {
            percentage += 20.0;
        }
        if (profile.getAddress() != null) {
            percentage += 20.0;
        }
        if (profile.getDocuments() != null && !profile.getDocuments().isEmpty()) {
            percentage += 20.0;
        }
        if (profile.getMembership() != null) {
            percentage += 20.0;
        }

        return percentage;
    }

    @Override
    public OnboardingStep getNextStep(WorkerProfile profile) {
        if (profile == null) {
            return OnboardingStep.BASIC_PROFILE;
        }

        if (profile.getProfessionalDetails() == null) {
            return OnboardingStep.PROFESSIONAL_DETAILS;
        }
        if (profile.getAddress() == null) {
            return OnboardingStep.ADDRESS;
        }
        if (profile.getDocuments() == null || profile.getDocuments().isEmpty()) {
            return OnboardingStep.DOCUMENTS;
        }
        if (profile.getMembership() == null) {
            return OnboardingStep.MEMBERSHIP;
        }

        return OnboardingStep.COMPLETED;
    }
}
