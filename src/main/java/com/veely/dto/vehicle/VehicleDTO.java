package com.veely.dto.vehicle;

import com.veely.model.FuelType;
import com.veely.model.OwnershipType;
import com.veely.model.VehicleStatus;
import com.veely.model.VehicleType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleDTO {
    private Long id;
    
    private String plate;
    private String chassisNumber;
    private String brand;
    private String model;
    private String series;
    private Integer year;
    private VehicleType type;
    private FuelType fuelType;
    private OwnershipType ownership;
    private VehicleStatus status;
    private Integer currentMileage;
    
    // Dati contratto
    private LocalDate registrationDate;
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;
    private BigDecimal totalFee;
    
    // Scadenze
    private LocalDate insuranceExpiryDate;
    private LocalDate carTaxExpiryDate;
    
    // Relazioni semplificate
    private String supplierName;
    private Long supplierId;
    private String assignedEmployeeName;
    private Long assignedEmployeeId;
    private String fuelCardNumber;
    
    // Campi calcolati
    private boolean hasActiveAssignment;
    private Integer daysUntilInsuranceExpiry;
    private Integer daysUntilCarTaxExpiry;
}
