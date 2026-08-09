package com.newtron.newtron_workforce_backend.entity;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

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

    private String status; // "Applied", "Shortlisted", "Rejected", "Hired"
    private String appliedDate;
    private Integer currentStep; // 1-4
    private String declineReason;
}
