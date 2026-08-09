package com.newtron.newtron_workforce_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class LoginResponse {

    private String token;
    private String mobile;
    private String role;
}