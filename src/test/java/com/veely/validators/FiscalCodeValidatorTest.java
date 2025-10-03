package com.veely.validators;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.veely.validation.FiscalCodeValidator;

import static org.assertj.core.api.Assertions.assertThat;

class FiscalCodeValidatorTest {
    
    private FiscalCodeValidator validator;
    
    @BeforeEach
    void setUp() {
        validator = new FiscalCodeValidator();
    }
    
    @Test
    void shouldAcceptValidFiscalCode() {
        // Codice fiscale di esempio valido
    	String validCF = "RSSMRA85M01H501Q";
        assertThat(validator.isValid(validCF, null)).isTrue();
    }
    
    @ParameterizedTest
    @ValueSource(strings = {
        "ABCDEF12G34H567",  // Lunghezza errata
        "12345678901234567", // Solo numeri
        "ABCDEFGHIJKLMNOP"   // Solo lettere
    })
    void shouldRejectInvalidFiscalCodes(String cf) {
        assertThat(validator.isValid(cf, null)).isFalse();
    }
    
    @Test
    void shouldHandleLowerCase() {
    	String cfLowerCase = "rssmra85m01h501q";
        assertThat(validator.isValid(cfLowerCase, null)).isTrue();
    }
}
