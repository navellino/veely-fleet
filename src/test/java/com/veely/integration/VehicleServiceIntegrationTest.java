package com.veely.integration;

import com.veely.entity.Vehicle;
import com.veely.model.FuelType;
import com.veely.model.VehicleStatus;
import com.veely.model.VehicleType;
import com.veely.repository.VehicleRepository;
import com.veely.service.VehicleService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
@Transactional
class VehicleServiceIntegrationTest {
    
    @Autowired
    private VehicleService vehicleService;
    
    @Autowired
    private VehicleRepository vehicleRepository;
    
    @BeforeEach
    void setUp() {
    	List<Vehicle> existing = vehicleService.findAll();
        existing.stream()
                .map(Vehicle::getId)
                .forEach(vehicleService::delete);
    }
    
    @Test
    void shouldCreateVehicleWithAllFields() {
        // Given
        Vehicle vehicle = Vehicle.builder()
        	.plate("AA123BB")
            .brand("Toyota")
            .model("Corolla")
            .year(2023)
            .type(VehicleType.CAR)
            .fuelType(FuelType.HYBRID)
            .build();
        
        // When
        Vehicle saved = vehicleService.create(vehicle);
        
        // Then
        assertThat(saved.getId()).isNotNull();
        assertThat(saved.getStatus()).isEqualTo(VehicleStatus.IN_SERVICE);
        assertThat(saved.getPlate()).isEqualTo("AA123BB");
        
        // Verify in database
        Vehicle fromDb = vehicleRepository.findById(saved.getId()).orElse(null);
        assertThat(fromDb).isNotNull();
        assertThat(fromDb.getPlate()).isEqualTo("AA123BB");
    }
    
    @Test
    void shouldUpdateVehicleMileage() {
        // Given
        Vehicle vehicle = Vehicle.builder()
        	.plate("CC234DD")
            .brand("Ford")
            .model("Focus")
            .year(2021)
            .currentMileage(10000)
            .build();
        
        Vehicle saved = vehicleService.create(vehicle);
        
        // When
        Vehicle updated = vehicleService.updateMileage(saved.getId(), 15000);
        
        // Then
        assertThat(updated.getCurrentMileage()).isEqualTo(15000);
    }
}
