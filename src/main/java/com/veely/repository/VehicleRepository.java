package com.veely.repository;

import com.veely.entity.Vehicle;
import com.veely.model.VehicleStatus;
import com.veely.model.VehicleType;
import com.veely.repository.custom.VehicleRepositoryCustom;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface VehicleRepository extends JpaRepository<Vehicle, Long>, VehicleRepositoryCustom {

    List<Vehicle> findByType(VehicleType type);

    /** Veicoli con stato specifico (es. AVAILABLE). */
    List<Vehicle> findByStatus(VehicleStatus status);
    
    long countByStatus(VehicleStatus status);
    
    /** Veicoli che non hanno lo stato specificato. */
    List<Vehicle> findByStatusNot(VehicleStatus status);
    
 // Query ottimizzata per trovare un veicolo con tutte le relazioni
    @Query("SELECT v FROM Vehicle v " +
           "LEFT JOIN FETCH v.supplier " +
           "LEFT JOIN FETCH v.fuelCard " +
           "LEFT JOIN FETCH v.documents " +
           "WHERE v.id = :id")
    Optional<Vehicle> findByIdWithRelations(@Param("id") Long id);
    
 // Query per batch loading
    @Query("SELECT DISTINCT v FROM Vehicle v " +
           "LEFT JOIN FETCH v.assignments " +
           "WHERE v IN :vehicles")
    List<Vehicle> loadAssignmentsForVehicles(@Param("vehicles") List<Vehicle> vehicles);
    
    @Query("SELECT v FROM Vehicle v WHERE v.id IN " +
    		 "(SELECT DISTINCT a.vehicle.id FROM Assignment a " +
            "WHERE a.status = 'ASSIGNED' AND a.employment.employee.id = :employeeId)")
     List<Vehicle> findByAssignedEmployeeId(@Param("employeeId") Long employeeId);

    @Query("""
            SELECT v FROM Vehicle v
            WHERE NOT EXISTS (
                SELECT fc FROM FuelCard fc
                WHERE fc.vehicle = v
                )
    		""")
    List<Vehicle> findWithoutFuelCard();
}
