package com.newtron.newtron_workforce_backend.service;

import com.newtron.newtron_workforce_backend.entity.WorkerProfile;
import com.newtron.newtron_workforce_backend.enums.OnboardingStep;

public interface OnboardingProgressService {
    double calculateCompletion(WorkerProfile profile);
    OnboardingStep getNextStep(WorkerProfile profile);
    void startProfilePreparation(WorkerProfile profile);
}
