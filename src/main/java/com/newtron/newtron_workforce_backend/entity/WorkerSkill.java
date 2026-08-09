package com.newtron.newtron_workforce_backend.entity;

import com.newtron.newtron_workforce_backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "worker_skills")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerSkill extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_profile_id", nullable = false)
    private WorkerProfile workerProfile;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "skill_id", nullable = false)
    private Skill skill;

    @Column(name = "is_primary", nullable = false)
    private Boolean isPrimary;

    @Column(name = "experience_years", nullable = false)
    private Integer experienceYears;

    @Column(name = "experience_months", nullable = false)
    private Integer experienceMonths;
}