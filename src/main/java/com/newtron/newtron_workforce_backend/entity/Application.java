package com.newtron.newtron_workforce_backend.entity;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.Instant;

@Entity
@Table(name = "applications")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Application {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "job_id")
    private Job job;

    @ManyToOne
    @JoinColumn(name = "worker_id")
    private User worker;

    @ManyToOne
    @JoinColumn(name = "team_id")
    private Team team;

    private String status; // "Applied", "Shortlisted", "Rejected", "Hired", "Completed"
    private String appliedDate;
    private Integer currentStep; // 1-4
    private String declineReason;

    @Column(name = "hired_at")
    private Instant hiredAt;

    @Column(name = "completed_at")
    private Instant completedAt;

    @Column(name = "engagement_type")
    private String engagementType;
}
