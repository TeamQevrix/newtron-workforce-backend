package com.newtron.newtron_workforce_backend.entity;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "jobs")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Job {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String title;
    private String category;
    private String city;
    private String salary;
    private String duration;
    private String distance;

    @Column(length = 2000)
    private String description;

    private String status; // "Active", "Paused", "Closed"
    private Integer workersRequired;

    @ManyToOne
    @JoinColumn(name = "recruiter_id")
    private User recruiter;
}
