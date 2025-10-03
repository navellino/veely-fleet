package com.veely.dto.assignment;

import com.veely.model.AssignmentStatus;
import com.veely.model.AssignmentType;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;
import java.time.LocalTime;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AssignmentDTO {
    private Long id;
    private AssignmentType type;
    private AssignmentStatus status;
    private LocalDate startDate;
    private LocalTime startTime;
    private LocalDate endDate;
    private LocalTime endTime;
    private String note;
    private Long accolloProjectId;
    private String accolloProjectName;
    
    // Relazioni semplificate
    private Long vehicleId;
    private String vehiclePlate;
    private String vehicleBrandModel;
    
    private Long employmentId;
    private String employeeName;
    private String employeeMatricola;
    
    // Campi calcolati
    private boolean isActive;
    private Integer daysRemaining;
}
