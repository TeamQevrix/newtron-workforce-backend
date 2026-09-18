package com.newtron.newtron_workforce_backend.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "master_cities")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MasterCity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "district_id", nullable = false)
    private MasterDistrict district;

    @Column(nullable = false, length = 100)
    private String name;
}
