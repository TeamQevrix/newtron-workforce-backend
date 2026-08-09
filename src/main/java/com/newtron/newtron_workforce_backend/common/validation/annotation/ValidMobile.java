package com.newtron.newtron_workforce_backend.common.validation.annotation;

import com.newtron.newtron_workforce_backend.common.validation.constants.ValidationMessages;
import com.newtron.newtron_workforce_backend.common.validation.validator.MobileValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = MobileValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidMobile {
    String message() default ValidationMessages.INVALID_MOBILE;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
