package com.veely.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD, ElementType.METHOD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = AgeValidator.class)
@Documented
public @interface ValidAge {
    String message() default "Età non valida";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
    int min() default 0;
    int max() default 120;
}

