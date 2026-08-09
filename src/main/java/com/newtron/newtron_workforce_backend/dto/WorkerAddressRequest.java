package com.newtron.newtron_workforce_backend.dto;

import jakarta.validation.constraints.*;
import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerAddressRequest {

    @NotNull(message = "State ID is required")
    private Long stateId;

    @NotNull(message = "District ID is required")
    private Long districtId;

    @NotNull(message = "City ID is required")
    private Long cityId;

    @NotBlank(message = "Area/Village is required")
    @Size(max = 255, message = "Area/Village name must not exceed 255 characters")
    private String areaVillage;

    @NotBlank(message = "Pincode is required")
    @Pattern(regexp = "^[0-9]{6}$", message = "Pincode must be exactly 6 digits")
    private String pincode;

    @NotBlank(message = "Current address is required")
    @Size(max = 255, message = "Current address must not exceed 255 characters")
    private String currentAddress;

    @Size(max = 255, message = "Landmark must not exceed 255 characters")
    private String landmark;

    @DecimalMin(value = "-90.0", message = "Latitude must be between -90 and 90")
    @DecimalMax(value = "90.0", message = "Latitude must be between -90 and 90")
    private Double latitude;

    @DecimalMin(value = "-180.0", message = "Longitude must be between -180 and 180")
    @DecimalMax(value = "180.0", message = "Longitude must be between -180 and 180")
    private Double longitude;
}
