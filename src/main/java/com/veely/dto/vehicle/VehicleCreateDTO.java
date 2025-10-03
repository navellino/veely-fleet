package com.veely.dto.vehicle;

import com.veely.model.FuelType;
import com.veely.model.OwnershipType;
import com.veely.model.VehicleType;
import com.veely.validation.annotation.ValidPlate;

import jakarta.validation.constraints.*;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;

@Data
public class VehicleCreateDTO {
    
    @NotBlank(message = "Targa obbligatoria")
    @ValidPlate
    private String plate;
    
    @Size(max = 50, message = "Numero telaio troppo lungo")
    private String chassisNumber;
    
    @NotBlank(message = "Marca obbligatoria")
    private String brand;
    
    @NotBlank(message = "Modello obbligatorio")
    private String model;
    
    private String series;
    
    @NotNull(message = "Anno obbligatorio")
    @Min(value = 1900, message = "Anno non valido")
    @Max(value = 2100, message = "Anno non valido")
    private Integer year;
    
    @NotNull(message = "Tipo veicolo obbligatorio")
    private VehicleType type;
    
    @NotNull(message = "Tipo carburante obbligatorio")
    private FuelType fuelType;
    
    @NotNull(message = "Tipo proprietà obbligatorio")
    private OwnershipType ownership;
    
    private Long supplierId;
    
    @PastOrPresent(message = "Data immatricolazione non può essere futura")
    private LocalDate registrationDate;
    
    private LocalDate contractStartDate;
    private LocalDate contractEndDate;
    private Integer contractDuration;
    private Integer contractualKm;
    
    @PositiveOrZero(message = "Canone finanziario non può essere negativo")
    private BigDecimal financialFee;
    
    @PositiveOrZero(message = "Canone assistenza non può essere negativo")
    private BigDecimal assistanceFee;
    
    private BigDecimal annualFringeBenefit;
    private BigDecimal monthlyFringeBenefit;
    
    @PositiveOrZero(message = "Chilometraggio non può essere negativo")
    private Integer currentMileage;
    
    private Long fuelCardId;
    private String telepass;
    
    
    private LocalDate insuranceExpiryDate;
    
   
    private LocalDate carTaxExpiryDate;
}
