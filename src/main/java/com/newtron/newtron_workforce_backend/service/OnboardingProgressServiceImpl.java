package com.newtron.newtron_workforce_backend.service;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.auth.repository.UserRepository;
import com.newtron.newtron_workforce_backend.entity.WorkerProfile;
import com.newtron.newtron_workforce_backend.enums.OnboardingStep;
import com.newtron.newtron_workforce_backend.repository.WorkerProfileRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CompletableFuture;

@Slf4j
@Service
@RequiredArgsConstructor
public class OnboardingProgressServiceImpl implements OnboardingProgressService {

    private final WorkerProfileRepository workerProfileRepository;
    private final UserRepository userRepository;
    private final Set<Long> activePreparationJobs = ConcurrentHashMap.newKeySet();

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
        if (profile.getMembership() != null && "ACTIVE".equals(profile.getMembership().getStatus())) {
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
        if (profile.getMembership() == null || !"ACTIVE".equals(profile.getMembership().getStatus())) {
            return OnboardingStep.MEMBERSHIP;
        }

        return OnboardingStep.COMPLETED;
    }

    @Override
    public void startProfilePreparation(WorkerProfile profile) {
        if (profile == null || profile.getId() == null) {
            return;
        }

        // Idempotency check: if already completed or already running, skip
        if (profile.getCurrentStep() == OnboardingStep.COMPLETED || Boolean.TRUE.equals(profile.getIsCompleted())) {
            log.info("Profile preparation already completed for worker {}", profile.getId());
            return;
        }

        if (!activePreparationJobs.add(profile.getId())) {
            log.info("Profile preparation job already running for worker {}", profile.getId());
            return;
        }

        CompletableFuture.runAsync(() -> {
            try {
                log.info("Starting background profile preparation for worker: {}", profile.getId());
                
                // Step 1: PROFILE_CREATING (0%)
                updateState(profile.getId(), "PROFILE_CREATING", 0);
                Thread.sleep(1500);

                // Step 2: VERIFYING_DOCUMENTS (20%)
                updateState(profile.getId(), "VERIFYING_DOCUMENTS", 20);
                Thread.sleep(1500);

                // Step 3: ACTIVATING_MEMBERSHIP (40%)
                updateState(profile.getId(), "ACTIVATING_MEMBERSHIP", 40);
                Thread.sleep(1500);

                // Step 4: GENERATING_RECOMMENDATIONS (60%)
                updateState(profile.getId(), "GENERATING_RECOMMENDATIONS", 60);
                Thread.sleep(1500);

                // Step 5: GENERATING_RECOMMENDATIONS (80%)
                updateState(profile.getId(), "GENERATING_RECOMMENDATIONS", 80);
                Thread.sleep(1500);

                // Final Step: COMPLETED (100%)
                completeOnboarding(profile.getId());

            } catch (InterruptedException e) {
                log.error("Profile preparation job interrupted for worker {}", profile.getId(), e);
                Thread.currentThread().interrupt();
            } finally {
                activePreparationJobs.remove(profile.getId());
            }
        });
    }

    private void updateState(Long profileId, String step, int progress) {
        workerProfileRepository.findById(profileId).ifPresent(p -> {
            p.setPreparationStep(step);
            p.setPreparationProgress(progress);
            p.setCurrentStep(OnboardingStep.VERIFICATION);
            workerProfileRepository.saveAndFlush(p);
            log.info("Updated preparation status for worker {}: {} - {}%", profileId, step, progress);
        });
    }

    private void completeOnboarding(Long profileId) {
        workerProfileRepository.findById(profileId).ifPresent(p -> {
            p.setCurrentStep(OnboardingStep.COMPLETED);
            p.setIsCompleted(true);
            p.setPreparationProgress(100);
            p.setPreparationStep("COMPLETED");
            workerProfileRepository.saveAndFlush(p);

            User user = p.getUser();
            if (user != null) {
                user.setProfileCompleted(true);
                userRepository.saveAndFlush(user);
            }
            log.info("Profile preparation completed successfully for worker: {}", profileId);
        });
    }
}
