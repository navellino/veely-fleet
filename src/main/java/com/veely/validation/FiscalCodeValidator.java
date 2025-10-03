package com.veely.validation;


import jakarta.validation.ConstraintValidator;
import jakarta.validation.ConstraintValidatorContext;

public class FiscalCodeValidator implements ConstraintValidator<ValidFiscalCode, String> {
    
    @Override
    public boolean isValid(String fiscalCode, ConstraintValidatorContext context) {
        if (fiscalCode == null || fiscalCode.trim().isEmpty()) {
            return true; // @NotBlank si occupa di questo
        }
        
        return isValidFiscalCode(fiscalCode.trim().toUpperCase());
    }
    
    private boolean isValidFiscalCode(String fiscalCode) {
        // Verifica lunghezza
        if (fiscalCode.length() != 16) {
            return false;
        }
        
        // Verifica pattern base
        if (!fiscalCode.matches("^[A-Z]{6}[0-9]{2}[A-Z][0-9]{2}[A-Z][0-9]{3}[A-Z]$")) {
            return false;
        }
        
        // Verifica carattere di controllo
        return verifyControlCharacter(fiscalCode);
    }
    
    private boolean verifyControlCharacter(String fiscalCode) {
        String oddPositionChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        int[] oddPositionValues = {1, 0, 5, 7, 9, 13, 15, 17, 19, 21, 2, 4, 18, 20, 11, 3, 6, 8, 12, 14, 16, 10, 22, 25, 24, 23};
        
        String evenPositionChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        int[] evenPositionValues = {0, 1, 2, 3, 4, 5, 6, 7, 8, 9, 10, 11, 12, 13, 14, 15, 16, 17, 18, 19, 20, 21, 22, 23, 24, 25};
        
        String controlChars = "ABCDEFGHIJKLMNOPQRSTUVWXYZ";
        
        int sum = 0;
        
        for (int i = 0; i < 15; i++) {
            char c = fiscalCode.charAt(i);
            int value;
            
            if (Character.isDigit(c)) {
                value = Character.getNumericValue(c);
                if (i % 2 == 0) { // posizione dispari (0-based diventa dispari 1-based)
                    // Per le cifre nelle posizioni dispari
                    int[] digitOddValues = {1, 0, 5, 7, 9, 13, 15, 17, 19, 21};
                    value = digitOddValues[value];
                } else {
                    // Per le cifre nelle posizioni pari, il valore rimane uguale
                }
            } else {
                // È una lettera
                int charIndex = c - 'A';
                if (i % 2 == 0) { // posizione dispari
                    value = oddPositionValues[charIndex];
                } else { // posizione pari
                    value = evenPositionValues[charIndex];
                }
            }
            
            sum += value;
        }
        
        char expectedControl = controlChars.charAt(sum % 26);
        return fiscalCode.charAt(15) == expectedControl;
    }
}
