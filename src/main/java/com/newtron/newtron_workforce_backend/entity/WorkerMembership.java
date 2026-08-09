package com.newtron.newtron_workforce_backend.entity;

import com.newtron.newtron_workforce_backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDateTime;

@Entity
@Table(name = "worker_memberships")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerMembership extends BaseEntity {

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_profile_id", nullable = false)
    private WorkerProfile workerProfile;

    @Column(name = "plan", nullable = false)
    private String plan;

    @Column(name = "amount", nullable = false)
    private Long amount;

    @Column(name = "currency", nullable = false)
    private String currency;

    @Column(name = "status", nullable = false)
    private String status;

    @Column(name = "activated_at")
    private LocalDateTime activatedAt;

    @Column(name = "expires_at")
    private LocalDateTime expiresAt;

    @Column(name = "payment_provider")
    private String paymentProvider;

    @Column(name = "payment_order_id", nullable = false, unique = true)
    private String paymentOrderId;

    @Column(name = "payment_payment_id")
    private String paymentPaymentId;

    @Column(name = "payment_signature")
    private String paymentSignature;
}
