package com.newtron.newtron_workforce_backend.common.validation.validator;

import com.newtron.newtron_workforce_backend.common.validation.annotation.ValidName;
import com.newtron.newtron_workforce_backend.common.validation.constants.ValidationPatterns;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class NameValidator implements ConstraintValidator<ValidName, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        return ValidationPatterns.NAME_PATTERN.matcher(value.trim()).matches();
    }
}
