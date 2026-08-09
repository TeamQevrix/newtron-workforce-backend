package com.newtron.newtron_workforce_backend.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank(message = "Full name is required")
    private String fullName;

    @NotBlank(message = "Mobile number is required")
    @Pattern(
            regexp = "^[6-9]\\d{9}$",
            message = "Invalid mobile number"
    )
    private String mobile;

    @NotBlank(message = "Password is required")
    @Size(
            min = 6,
            max = 20,
            message = "Password must be between 6 and 20 characters"
    )
    private String password;

    @NotBlank(message = "Role is required")
    private String role;
}