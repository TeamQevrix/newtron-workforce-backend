package com.newtron.newtron_workforce_backend.dto;

import com.newtron.newtron_workforce_backend.common.validation.annotation.Adult;
import com.newtron.newtron_workforce_backend.enums.BloodGroup;
import com.newtron.newtron_workforce_backend.enums.Gender;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Pattern;
import lombok.*;

import java.time.LocalDate;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class WorkerBasicProfileRequest {

    @NotBlank(message = "Full name is required")
    @Pattern(regexp = "^[\\p{L}'’\\s\\-]{3,80}$", message = "Name must be 3-80 characters long and contain only letters and space/symbol punctuation")
    private String fullName;

    @NotNull(message = "Date of birth is required")
    @Adult
    private LocalDate dateOfBirth;

    @NotNull(message = "Gender is required")
    private Gender gender;

    private BloodGroup bloodGroup;
}
