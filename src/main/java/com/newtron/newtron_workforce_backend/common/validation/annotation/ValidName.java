package com.newtron.newtron_workforce_backend.common.validation.annotation;

import com.newtron.newtron_workforce_backend.common.validation.constants.ValidationMessages;
import com.newtron.newtron_workforce_backend.common.validation.validator.NameValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = NameValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface ValidName {
    String message() default ValidationMessages.INVALID_NAME;
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
