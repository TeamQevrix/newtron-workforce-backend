package com.newtron.newtron_workforce_backend.entity;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import com.newtron.newtron_workforce_backend.common.entity.BaseEntity;
import com.newtron.newtron_workforce_backend.enums.BloodGroup;
import com.newtron.newtron_workforce_backend.enums.Gender;
import com.newtron.newtron_workforce_backend.enums.OnboardingStep;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

@Entity
@Table(name = "worker_profiles")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerProfile extends BaseEntity {

    // One User = One Worker Profile
    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false, unique = true)
    private User user;

    @Column(name = "full_name", nullable = false, length = 80)
    private String fullName;

    @Column(name = "date_of_birth", nullable = false)
    private LocalDate dateOfBirth;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", nullable = false, length = 20)
    private Gender gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "blood_group", length = 20)
    private BloodGroup bloodGroup;

    @Column(name = "photo_storage_key")
    private String photoStorageKey;

    @Enumerated(EnumType.STRING)
    @Column(name = "current_step", nullable = false, length = 50)
    private OnboardingStep currentStep;

    @Column(name = "is_completed", nullable = false)
    @Builder.Default
    private Boolean isCompleted = false;

    @Column(name = "is_available_on_demand", nullable = false)
    @Builder.Default
    private Boolean isAvailableOnDemand = false;

    @Column(name = "preparation_progress", nullable = false)
    @Builder.Default
    private Integer preparationProgress = 0;

    @Column(name = "preparation_step", nullable = false, length = 50)
    @Builder.Default
    private String preparationStep = "PROFILE_CREATING";

    @Column(name = "emergency_contact", length = 20)
    private String emergencyContact;

    // Aggregate associations
    @OneToOne(mappedBy = "workerProfile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private WorkerProfessionalDetails professionalDetails;

    @OneToOne(mappedBy = "workerProfile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private WorkerAddress address;

    @OneToOne(mappedBy = "workerProfile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    private WorkerMembership membership;

    @OneToMany(mappedBy = "workerProfile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<WorkerDocument> documents = new HashSet<>();

    @OneToMany(mappedBy = "workerProfile", cascade = CascadeType.ALL, orphanRemoval = true, fetch = FetchType.LAZY)
    @Builder.Default
    private Set<WorkerSkill> skills = new java.util.LinkedHashSet<>();
}