package com.credixa.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Documented
@Constraint(validatedBy = PanNumberValidator.class)
@Target({ElementType.FIELD, ElementType.PARAMETER})
@Retention(RetentionPolicy.RUNTIME)
public @interface PanNumber {
    String message() default "Invalid PAN number format";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
