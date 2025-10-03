package com.veely.repository.custom;

import com.veely.entity.Vehicle;
import com.veely.model.VehicleStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;

import java.time.LocalDate;
import java.util.List;

public interface VehicleRepositoryCustom {
    
    /**
     * Trova veicoli con tutti i dati correlati in una singola query
     */
    List<Vehicle> findAllWithRelations();
    
    /**
     * Trova veicoli per stato con fetch ottimizzato
     */
    Page<Vehicle> findByStatusOptimized(VehicleStatus status, Pageable pageable);
    
    /**
     * Ricerca complessa con filtri multipli
     */
    Page<Vehicle> searchVehicles(String plate, String brand, VehicleStatus status, 
                                LocalDate expiryDateFrom, LocalDate expiryDateTo, 
                                Pageable pageable);
    
    /**
     * Report veicoli con statistiche
     */
    List<VehicleStatistics> getVehicleStatistics();
    
    class VehicleStatistics {
        public String brand;
        public Long count;
        public Double avgMileage;
        public Long activeAssignments;
        
        public VehicleStatistics(String brand, Long count, Double avgMileage, Long activeAssignments) {
            this.brand = brand;
            this.count = count;
            this.avgMileage = avgMileage;
            this.activeAssignments = activeAssignments;
        }
    }
}

