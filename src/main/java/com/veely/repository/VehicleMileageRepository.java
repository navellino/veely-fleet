package com.veely.repository;

import com.veely.entity.VehicleMileage;
import com.veely.model.MileageSource;
import org.springframework.data.jpa.repository.JpaRepository;
import java.time.LocalDate;

import java.util.Optional;

public interface VehicleMileageRepository extends JpaRepository<VehicleMileage, Long> {

    Optional<VehicleMileage> findBySourceAndSourceId(MileageSource source, Long sourceId);

    VehicleMileage findTopByVehicleOrderByDateDescIdDesc(com.veely.entity.Vehicle vehicle);
    
    VehicleMileage findTopByVehicleAndDateLessThanOrderByDateDescIdDesc(
            com.veely.entity.Vehicle vehicle,
            LocalDate date);

    void deleteByVehicleId(Long vehicleId);
}
