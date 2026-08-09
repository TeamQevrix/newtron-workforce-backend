package com.newtron.newtron_workforce_backend.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(name = "worker_professionals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerProfessional {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //-------------------------------------------------
    // Relation
    //-------------------------------------------------

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_profile_id",
            nullable = false,
            unique = true)
    private WorkerProfile workerProfile;

    //-------------------------------------------------
    // Skill
    //-------------------------------------------------

    @Column(nullable = false)
    private String primarySkill;

    private String secondarySkill;

    //-------------------------------------------------
    // Experience
    //-------------------------------------------------

    @Column(nullable = false)
    private Integer experienceYears;

    //-------------------------------------------------
    // Salary
    //-------------------------------------------------

    @Column(precision = 10, scale = 2)
    private BigDecimal expectedDailyWage;

    //-------------------------------------------------
    // Work Preference
    //-------------------------------------------------

    private String employmentType;

    private Boolean available;

    private Integer workRadiusKm;

    //-------------------------------------------------
    // Language
    //-------------------------------------------------

    private String languages;

    //-------------------------------------------------
    // About
    //-------------------------------------------------

    @Column(length = 1000)
    private String bio;

}