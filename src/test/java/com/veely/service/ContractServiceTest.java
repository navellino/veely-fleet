package com.veely.service;

import com.veely.entity.Contract;
import com.veely.model.SupplierContractType;
import com.veely.repository.ContractRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.math.BigDecimal;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
class ContractServiceTest {

	 @Mock
	 private ContractRepository contractRepository;

	@InjectMocks
    private ContractService contractService;

    @Test
    void createShouldDefaultAmountNetToZeroWhenNull() {
        Contract contract = Contract.builder()
                .type(SupplierContractType.SERVIZI)
                .subject("Test")
                .build();
        
        when(contractRepository.save(any(Contract.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));


        Contract saved = contractService.create(contract);

        assertThat(saved.getAmountNet())
                .isEqualByComparingTo(BigDecimal.ZERO);
        assertThat(saved.getVatRate())
        .isEqualByComparingTo(BigDecimal.ZERO);

        verify(contractRepository).save(contract);
    }
}
