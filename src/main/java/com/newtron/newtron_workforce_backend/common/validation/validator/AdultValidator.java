package com.newtron.newtron_workforce_backend.common.validation.validator;

import com.newtron.newtron_workforce_backend.common.validation.annotation.Adult;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.Clock;
import java.time.LocalDate;
import java.time.Period;

public class AdultValidator implements ConstraintValidator<Adult, LocalDate> {

    private final Clock clock;

    @Autowired
    public AdultValidator(Clock clock) {
        this.clock = clock;
    }

    @Override
    public boolean isValid(LocalDate birthDate, ConstraintValidatorContext context) {
        if (birthDate == null) {
            return false;
        }
        LocalDate now = LocalDate.now(clock);
        int age = Period.between(birthDate, now).getYears();
        return age >= 18 && age <= 70;
    }
}
