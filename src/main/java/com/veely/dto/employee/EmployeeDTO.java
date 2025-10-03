package com.veely.dto.employee;

import com.veely.model.Gender;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;
import lombok.Builder;

import java.time.LocalDate;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmployeeDTO {
    private Long id;
    private String firstName;
    private String lastName;
    private String fiscalCode;
    private LocalDate birthDate;
    private Gender gender;
    private String email;
    private String phone;
    private String mobile;
    
    // Indirizzo semplificato
    private String residenceCity;
    private String residenceProvince;
    
    // Relazioni
    private String currentJobTitle;
    private String currentDepartment;
    private boolean hasActiveEmployment;
    private String profilePhotoUrl;
    
    // Campi calcolati
    private Integer age;
    private String fullName;
}
