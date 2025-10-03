package com.veely.validation;

import jakarta.validation.Constraint;
import jakarta.validation.Payload;
import java.lang.annotation.*;

@Target({ElementType.FIELD})
@Retention(RetentionPolicy.RUNTIME)
@Constraint(validatedBy = FiscalCodeValidator.class)
@Documented
public @interface ValidFiscalCode {
    String message() default "Codice fiscale non valido";
    Class<?>[] groups() default {};
    Class<? extends Payload>[] payload() default {};
}

