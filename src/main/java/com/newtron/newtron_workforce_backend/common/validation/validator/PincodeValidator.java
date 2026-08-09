package com.newtron.newtron_workforce_backend.common.validation.validator;

import com.newtron.newtron_workforce_backend.common.validation.annotation.ValidPincode;
import com.newtron.newtron_workforce_backend.common.validation.constants.ValidationPatterns;
import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class PincodeValidator implements ConstraintValidator<ValidPincode, String> {

    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
        if (value == null) {
            return false;
        }
        return ValidationPatterns.PINCODE_PATTERN.matcher(value).matches();
    }
}
