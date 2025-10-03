package com.veely.dto.vehicle;

import com.veely.model.VehicleStatus;
import lombok.Data;

import java.time.LocalDate;

@Data
public class VehicleListDTO {
    private Long id;
    private String plate;
    private String brand;
    private String model;
    private Integer year;
    private VehicleStatus status;
    private String assignedTo;
    private LocalDate insuranceExpiryDate;
    private LocalDate carTaxExpiryDate;
    private boolean hasExpiredDocuments;
}
