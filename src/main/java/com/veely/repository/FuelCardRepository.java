package com.veely.repository;


import com.veely.entity.FuelCard;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface FuelCardRepository extends JpaRepository<FuelCard, Long> {
	
	FuelCard findByVehicleId(Long vehicleId);

    FuelCard findByEmployeeId(Long employeeId);

	    @Query("SELECT fc FROM FuelCard fc WHERE fc.vehicle.id = :vehicleId AND fc.active = true AND (fc.expiryDate IS NULL OR fc.expiryDate > CURRENT_DATE)")
	    Optional<FuelCard> findActiveByVehicleId(@Param("vehicleId") Long vehicleId);

	    @Query("SELECT fc FROM FuelCard fc WHERE fc.employee.id = :employeeId AND fc.active = true AND (fc.expiryDate IS NULL OR fc.expiryDate > CURRENT_DATE)")
	    Optional<FuelCard> findActiveByEmployeeId(@Param("employeeId") Long employeeId);
}
