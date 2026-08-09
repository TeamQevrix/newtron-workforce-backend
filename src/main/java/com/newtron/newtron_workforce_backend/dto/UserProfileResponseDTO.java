package com.newtron.newtron_workforce_backend.dto;

import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class UserProfileResponseDTO {
    private String username;
    private String email;


}