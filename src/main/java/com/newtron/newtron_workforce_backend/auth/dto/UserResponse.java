package com.newtron.newtron_workforce_backend.auth.dto;



import com.newtron.newtron_workforce_backend.auth.enums.Role;
import com.newtron.newtron_workforce_backend.auth.enums.UserStatus;
import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UserResponse {

    private String uuid;

    private String fullName;

    private String mobile;

    private String email;

    private String city;

    private Role role;

    private UserStatus status;

    private Boolean profileCompleted;
}
