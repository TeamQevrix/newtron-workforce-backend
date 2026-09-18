package com.newtron.newtron_workforce_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "master_districts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MasterDistrict {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "state_id", nullable = false)
    private MasterState state;

    @Column(nullable = false, length = 100)
    private String name;
}
