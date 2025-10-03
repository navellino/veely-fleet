package com.veely.validation.validators;

import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

import java.util.regex.Pattern;

import com.veely.validation.annotation.ValidPlate;

public class PlateValidator implements ConstraintValidator<ValidPlate, String> {
    
    // Pattern per targhe italiane (adatta secondo necessità)
    private static final Pattern ITALIAN_PLATE = Pattern.compile("^[A-Z]{2}[0-9]{3}[A-Z]{2}$");
    private static final Pattern OLD_ITALIAN_PLATE = Pattern.compile("^[A-Z]{2}[0-9]{6}$");
    
    @Override
    public void initialize(ValidPlate constraintAnnotation) {
        // Nessuna inizializzazione necessaria
    }
    
    @Override
    public boolean isValid(String value, ConstraintValidatorContext context) {
    	if (value == null) {
            return true; // delega eventuale @NotBlank o @NotNull
        }
    	 String cleanPlate = value.trim();
         if (cleanPlate.isEmpty()) {
             return false;
         }

         cleanPlate = cleanPlate.toUpperCase().replaceAll("\\s+", "");

         return ITALIAN_PLATE.matcher(cleanPlate).matches() ||
               OLD_ITALIAN_PLATE.matcher(cleanPlate).matches();
    }
}
