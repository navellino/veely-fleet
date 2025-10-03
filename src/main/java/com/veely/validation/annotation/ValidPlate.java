package com.veely.validation.annotation;

import com.veely.validation.validators.PlateValidator;
import jakarta.validation.Constraint;
import jakarta.validation.Payload;

import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = PlateValidator.class)
@Documented
public @interface ValidPlate {
    String message() default "Formato targa non valido";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}
