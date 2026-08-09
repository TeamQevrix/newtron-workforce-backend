package com.newtron.newtron_workforce_backend.controller;

import com.newtron.newtron_workforce_backend.dto.UserProfileResponseDTO;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    @GetMapping("/me")
    public UserProfileResponseDTO me(
            Authentication authentication) {

        UserDetails userDetails =
                (UserDetails) authentication.getPrincipal();

        String role =
                userDetails.getAuthorities()
                        .iterator()
                        .next()
                        .getAuthority();

        return new UserProfileResponseDTO (
                userDetails.getUsername(),
                role
        );
    }


    }
