package com.newtron.newtron_workforce_backend.auth.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank(message = "Identifier is required")
    private String identifier;

    @NotBlank(message = "Password is required")
    private String password;

    @NotNull(message = "Device information is required")
    @Valid
    private DeviceInfoDto deviceInformation;
}
