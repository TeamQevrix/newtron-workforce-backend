package com.newtron.newtron_workforce_backend.common.validation.annotation;

import com.newtron.newtron_workforce_backend.common.validation.constants.ValidationMessages;
import com.newtron.newtron_workforce_backend.common.validation.validator.PasswordValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PasswordValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidPassword {
    String message() default ValidationMessages.INVALID_PASSWORD;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
