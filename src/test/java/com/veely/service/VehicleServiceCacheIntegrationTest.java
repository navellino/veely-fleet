package com.veely.service;

import com.veely.entity.Vehicle;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

@SpringBootTest
@ActiveProfiles("test")
class VehicleServiceCacheIntegrationTest {

    @Autowired
    private VehicleService vehicleService;

    @BeforeEach
    void setUp() {
    	 List<Vehicle> existing = vehicleService.findAll();
         existing.stream()
                 .map(Vehicle::getId)
                 .forEach(vehicleService::delete);
    }

    @Test
    void shouldEvictCacheWhenUpdatingMileage() {
        // Given
        Vehicle vehicle = Vehicle.builder()
        		.plate("EE345FF")
                .brand("Fiat")
                .model("Punto")
                .year(2020)
                .currentMileage(1000)
                .build();

        Vehicle saved = vehicleService.create(vehicle);

        // First call caches the vehicle
        vehicleService.findByIdOrThrow(saved.getId());

        // When
        vehicleService.updateMileage(saved.getId(), 2000);

        // Then
        Vehicle updated = vehicleService.findByIdOrThrow(saved.getId());
        assertThat(updated.getCurrentMileage()).isEqualTo(2000);
    }
    
    @Test
    void shouldNotAllowDecreasingMileage() {
        Vehicle vehicle = Vehicle.builder()
        	.plate("GG456HH")
            .brand("Fiat")
            .model("Panda")
            .year(2019)
            .currentMileage(10000)
            .build();

        Vehicle saved = vehicleService.create(vehicle);

        assertThatThrownBy(() -> vehicleService.updateMileage(saved.getId(), 9000))
            .isInstanceOf(IllegalArgumentException.class);
    }
}
