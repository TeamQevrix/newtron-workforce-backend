package com.newtron.newtron_workforce_backend.entity;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;

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
    private Double latitude;
    private Double longitude;

    @Column(length = 2000)
    private String description;

    @Column(name = "experience_required")
    private String experienceRequired;

    @Column(name = "shift_hours")
    private String shiftHours;

    @Column(length = 1000)
    private String benefits;

    @Column(name = "joining_date")
    private String joiningDate;

    @Column(name = "engagement_type", length = 30)
    private String engagementType;

    @Column(name = "engagement_duration_type", length = 30)
    private String engagementDurationType;

    @Column(name = "duration_value")
    private Integer durationValue;

    private String status; // "Active", "Paused", "Closed"
    private Integer workersRequired;

    @Column(name = "work_mode", length = 20)
    private String workMode; // "INDIVIDUAL" or "TEAM"

    @Column(name = "company_name")
    private String companyName;

    @ManyToOne
    @JoinColumn(name = "company_id")
    private Company company;

    @ManyToOne
    @JoinColumn(name = "recruiter_id")
    private User recruiter;

    @Column(name = "monthly_salary_amount", precision = 12, scale = 2)
    private BigDecimal monthlySalaryAmount;

    @OneToMany(mappedBy = "job", cascade = CascadeType.ALL, orphanRemoval = true)
    private java.util.List<JobPhoto> photos;
}

