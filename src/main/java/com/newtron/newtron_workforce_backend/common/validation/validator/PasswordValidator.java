package com.newtron.newtron_workforce_backend.common.validation.validator;

import com.newtron.newtron_workforce_backend.common.validation.annotation.ValidPassword;
import com.newtron.newtron_workforce_backend.common.validation.constants.ValidationPatterns;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PasswordValidator implements ConstraintValidator<ValidPassword, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        return ValidationPatterns.PASSWORD_PATTERN.matcher(value).matches();
    }
}
