package com.veely.validation;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;
import java.time.LocalDate;
import java.time.Period;

public class AgeValidator implements ConstraintValidator<ValidAge, LocalDate> {
    private int min;
    private int max;

    @Override
    public void initialize(ValidAge constraintAnnotation) {
        this.min = constraintAnnotation.min();
        this.max = constraintAnnotation.max();
    }

    @Override
    public boolean isValid(LocalDate birthDate, ConstraintValidatorContext context) {
        if (birthDate == null) {
            return true; // @NotNull si occupa di questo caso
        }
        
        int age = Period.between(birthDate, LocalDate.now()).getYears();
        
        if (age < min || age > max) {
            context.disableDefaultConstraintViolation();
            context.buildConstraintViolationWithTemplate(
                String.format("L'età deve essere compresa tra %d e %d anni (età attuale: %d)", 
                             min, max, age)
            ).addConstraintViolation();
            return false;
        }
        return true;
    }
}