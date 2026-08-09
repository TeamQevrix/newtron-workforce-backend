package com.newtron.newtron_workforce_backend.auth.dto;



import com.newtron.newtron_workforce_backend.auth.enums.Role;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Pattern;
import lombok.Data;

@Data
public class RegisterRequest {

    @NotBlank
    private String fullName;

    @NotBlank
    @Pattern(regexp = "^[6-9]\\d{9}$")
    private String mobile;

    @Email
    private String email;

    @NotBlank
    private String password;

    private String city;

    private Role role;
}
