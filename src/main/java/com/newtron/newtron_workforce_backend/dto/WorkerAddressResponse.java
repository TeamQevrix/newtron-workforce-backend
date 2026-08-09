package com.newtron.newtron_workforce_backend.dto;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerAddressResponse {
    private Long id;
    private Long stateId;
    private Long districtId;
    private Long cityId;
    private String areaVillage;
    private String pincode;
    private String currentAddress;
    private String landmark;
    private Double latitude;
    private Double longitude;
}
