package com.veely.validators.service;

import com.veely.entity.Vehicle;
import com.veely.entity.Assignment;
import com.veely.exception.BusinessValidationException;
import com.veely.model.AssignmentStatus;
import com.veely.model.AssignmentType;
import com.veely.model.VehicleStatus;
import com.veely.repository.AssignmentRepository;
import com.veely.repository.VehicleRepository;
import com.veely.service.ValidationService;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class ValidationServiceTest {
    
    @Mock
    private VehicleRepository vehicleRepo;
    
    @Mock
    private AssignmentRepository assignmentRepo;
    
    @InjectMocks
    private ValidationService validationService;
    
    private Vehicle testVehicle;
    
    @BeforeEach
    void setUp() {
        testVehicle = Vehicle.builder()
            .id(1L)
            .plate("AB123CD")
            .status(VehicleStatus.IN_SERVICE)
            .insuranceExpiryDate(LocalDate.now().plusMonths(6))
            .carTaxExpiryDate(LocalDate.now().plusMonths(3))
            .build();
    }
    
    @Test
    void shouldPassValidationForAssignableVehicle() {
        // Given
        when(vehicleRepo.findById(1L)).thenReturn(Optional.of(testVehicle));
        when(assignmentRepo.findByVehicleIdAndStatus(1L, AssignmentStatus.ASSIGNED))
            .thenReturn(List.of());
        
        // When/Then - No exception should be thrown
        validationService.validateVehicleCanBeAssigned(1L);
        
        verify(vehicleRepo).findById(1L);
        verify(assignmentRepo).findByVehicleIdAndStatus(1L, AssignmentStatus.ASSIGNED);
    }
    
    @Test
    void shouldFailValidationForAssignedVehicle() {
        // Given
        Assignment activeAssignment = Assignment.builder()
            .id(1L)
            .status(AssignmentStatus.ASSIGNED)
            .endDate(LocalDate.now().plusDays(10))
            .build();
            
        when(vehicleRepo.findById(1L)).thenReturn(Optional.of(testVehicle));
        when(assignmentRepo.findByVehicleIdAndStatus(1L, AssignmentStatus.ASSIGNED))
            .thenReturn(List.of(activeAssignment));
        
        // When/Then
        assertThatThrownBy(() -> validationService.validateVehicleCanBeAssigned(1L))
            .isInstanceOf(BusinessValidationException.class)
            .hasMessageContaining("non assegnabile")
            .satisfies(ex -> {
                BusinessValidationException bve = (BusinessValidationException) ex;
                assertThat(bve.getErrors()).contains("Il veicolo è già assegnato");
            });
    }
    
    @Test
    void shouldFailValidationForExpiredInsurance() {
        // Given
        testVehicle.setInsuranceExpiryDate(LocalDate.now().minusDays(1));
        
        when(vehicleRepo.findById(1L)).thenReturn(Optional.of(testVehicle));
        when(assignmentRepo.findByVehicleIdAndStatus(1L, AssignmentStatus.ASSIGNED))
            .thenReturn(List.of());
        
        // When/Then
        assertThatThrownBy(() -> validationService.validateVehicleCanBeAssigned(1L))
            .isInstanceOf(BusinessValidationException.class)
            .satisfies(ex -> {
                BusinessValidationException bve = (BusinessValidationException) ex;
                assertThat(bve.getErrors()).contains("Assicurazione scaduta");
            });
    }
    
    @Test
    void shouldValidateAssignmentDates() {
        // Valid case
        LocalDate start = LocalDate.now();
        LocalDate end = start.plusDays(7);
        validationService.validateAssignmentDates(start, end);

        // Invalid case - end before start
        LocalDate endBefore = start.minusDays(1);

        assertThatThrownBy(() ->
            validationService.validateAssignmentDates(start, endBefore))       
            .isInstanceOf(BusinessValidationException.class)
            .satisfies(ex -> {
                BusinessValidationException bve = (BusinessValidationException) ex;
                assertThat(bve.getErrors())
                .contains("La data fine deve essere successiva alla data inizio");
            });
    }
}
