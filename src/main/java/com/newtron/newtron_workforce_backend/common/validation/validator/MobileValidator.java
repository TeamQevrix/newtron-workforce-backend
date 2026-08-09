package com.newtron.newtron_workforce_backend.common.validation.validator;

import com.newtron.newtron_workforce_backend.common.validation.annotation.ValidMobile;
import com.newtron.newtron_workforce_backend.common.validation.constants.ValidationPatterns;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class MobileValidator implements ConstraintValidator<ValidMobile, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        return ValidationPatterns.MOBILE_PATTERN.matcher(value).matches();
    }
}
