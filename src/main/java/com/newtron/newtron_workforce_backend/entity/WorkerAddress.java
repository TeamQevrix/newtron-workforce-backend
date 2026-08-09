package com.newtron.newtron_workforce_backend.entity;

import com.newtron.newtron_workforce_backend.common.entity.BaseEntity;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "worker_addresses")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerAddress extends BaseEntity {

    @OneToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "worker_profile_id", nullable = false, unique = true)
    private WorkerProfile workerProfile;

    @Column(name = "state_id", nullable = false)
    private Long stateId;

    @Column(name = "district_id", nullable = false)
    private Long districtId;

    @Column(name = "city_id", nullable = false)
    private Long cityId;

    @Column(name = "area_village", nullable = false)
    private String areaVillage;

    @Column(name = "pincode", nullable = false, length = 6)
    private String pincode;

    @Column(name = "current_address", nullable = false)
    private String currentAddress;

    @Column(name = "landmark")
    private String landmark;

    @Column(name = "latitude")
    private Double latitude;

    @Column(name = "longitude")
    private Double longitude;
}