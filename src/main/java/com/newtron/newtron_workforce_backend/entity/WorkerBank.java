package com.newtron.newtron_workforce_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "worker_banks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerBank {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //-------------------------------------------------
    // Relation
    //-------------------------------------------------

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "worker_profile_id",
            nullable = false,
            unique = true
    )
    private WorkerProfile workerProfile;

    //-------------------------------------------------
    // Bank Information
    //-------------------------------------------------

    @Column(nullable = false)
    private String accountHolderName;

    @Column(nullable = false, unique = true)
    private String accountNumber;

    @Column(nullable = false)
    private String bankName;

    @Column(nullable = false)
    private String ifscCode;

    @Column(name = "branch_name")
    private String branchName;

    @Column(name = "account_type", nullable = false)
    private String accountType;

    @Column(name = "upi_id", unique = true)
    private String upiId;

    //-------------------------------------------------
    // Verification
    //-------------------------------------------------

    @Builder.Default
    private Boolean bankVerified = false;

    @Builder.Default
    private Boolean primaryAccount = true;

}
