package com.veely.dto.assignment;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Future;
import lombok.Data;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
public class AssignmentCreateDTO {
    
    @NotNull(message = "ID veicolo obbligatorio")
    private Long vehicleId;
    
    @NotNull(message = "ID rapporto di lavoro obbligatorio")
    private Long employmentId;
    
    @NotNull(message = "Data inizio obbligatoria")
    private LocalDate startDate;
    
    private LocalTime startTime;
    
    @Future(message = "Data fine deve essere futura")
    private LocalDate endDate;
    
    private LocalTime endTime;
    
    private String note;
    
    private Long accolloProjectId;
}