package com.veely.validators;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import com.veely.validation.validators.PlateValidator;

import static org.assertj.core.api.Assertions.assertThat;

class PlateValidatorTest {
    
    private PlateValidator validator;
    
    @BeforeEach
    void setUp() {
        validator = new PlateValidator();
        validator.initialize(null);
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"AB123CD", "XY999ZZ", "AA000AA"})
    void shouldAcceptValidPlates(String plate) {
        assertThat(validator.isValid(plate, null)).isTrue();
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"ab123cd", "Ab123Cd", " AB123CD ", "AB 123 CD"})
    void shouldNormalizePlates(String plate) {
        assertThat(validator.isValid(plate, null)).isTrue();
    }
    
    @ParameterizedTest
    @ValueSource(strings = {"ABC123", "12345AB", "ABCDEFG", "AB1234567", ""})
    void shouldRejectInvalidPlates(String plate) {
        assertThat(validator.isValid(plate, null)).isFalse();
    }
    
    @Test
    void shouldAcceptNullValue() {
        assertThat(validator.isValid(null, null)).isTrue();
    }
}
