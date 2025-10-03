package com.veely.entity;

import com.veely.model.SupplierContractType;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.math.BigDecimal;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class ContractValidationTest {

    private Validator validator;

    @BeforeEach
    void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
    }

    @Test
    void shouldDetectMissingRequiredFields() {
        Contract contract = Contract.builder().build();

        Set<ConstraintViolation<Contract>> violations = validator.validate(contract);

        assertThat(violations)
            .extracting(v -> v.getPropertyPath().toString())
            .contains("type", "subject");
    }

    @Test
    void shouldRejectNegativePeriodicFee() {
        Contract contract = Contract.builder()
            .type(SupplierContractType.SERVIZI)
            .subject("Test")
            .periodicFee(new BigDecimal("-1"))
            .build();

        Set<ConstraintViolation<Contract>> violations = validator.validate(contract);

        assertThat(violations)
            .extracting(v -> v.getPropertyPath().toString())
            .contains("periodicFee");
    }
}
