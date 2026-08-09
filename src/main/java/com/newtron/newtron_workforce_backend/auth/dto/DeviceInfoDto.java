package com.newtron.newtron_workforce_backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class DeviceInfoDto {
    @NotBlank(message = "Device ID is required")
    private String deviceId;
    
    private String deviceName;
    private String deviceType;
    private String osVersion;
    private String appVersion;
    private String fcmToken;
}
