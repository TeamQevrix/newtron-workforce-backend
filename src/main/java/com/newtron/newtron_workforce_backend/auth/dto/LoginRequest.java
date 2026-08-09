package com.newtron.newtron_workforce_backend.auth.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class LoginRequest {

    @NotBlank
    private String mobile;

    @NotBlank
    private String password;
}
