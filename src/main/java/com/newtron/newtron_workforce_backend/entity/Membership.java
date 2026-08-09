package com.newtron.newtron_workforce_backend.entity;

import com.newtron.newtron_workforce_backend.auth.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Entity
@Table(name = "memberships")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Membership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    //-------------------------------------------------
    // Worker
    //-------------------------------------------------

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(
            name = "user_id",
            nullable = false,
            unique = true
    )
    private User user;

    //-------------------------------------------------
    // Plan
    //-------------------------------------------------

    @Column(nullable = false)
    private String planName;

    @Column(nullable = false)
    private BigDecimal amount;

    @Column(nullable = false)
    private Integer validityDays;

    //-------------------------------------------------
    // Payment
    //-------------------------------------------------

    @Column(unique = true)
    private String orderId;

    @Column(unique = true)
    private String paymentId;

    private String paymentMethod;

    //-------------------------------------------------
    // Status
    //-------------------------------------------------

    @Enumerated(EnumType.STRING)
    private MembershipStatus status;

    //-------------------------------------------------
    // Validity
    //-------------------------------------------------

    private LocalDateTime startDate;

    private LocalDateTime expiryDate;

    //-------------------------------------------------
    // Audit
    //-------------------------------------------------

    private LocalDateTime createdAt;

    private LocalDateTime updatedAt;

}