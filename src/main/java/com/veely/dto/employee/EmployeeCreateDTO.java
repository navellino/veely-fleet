package com.veely.dto.employee;

import com.veely.model.*;
import jakarta.validation.constraints.*;
import lombok.Data;

import java.time.LocalDate;
import java.util.Set;

@Data
public class EmployeeCreateDTO {
    
    @NotBlank(message = "Nome obbligatorio")
    @Size(max = 100)
    private String firstName;
    
    @NotBlank(message = "Cognome obbligatorio")
    @Size(max = 100)
    private String lastName;
    
    @NotNull(message = "Data di nascita obbligatoria")
    @Past(message = "Data di nascita deve essere nel passato")
    private LocalDate birthDate;
    
    @Size(max = 100)
    private String birthPlace;
    
    @NotNull(message = "Genere obbligatorio")
    private Gender gender;
    
    @NotBlank(message = "Codice fiscale obbligatorio")
    private String fiscalCode;
    
    @Pattern(regexp = "^[IT]{2}[0-9]{2}[A-Z][0-9]{22}$", 
             message = "Formato IBAN non valido")
    private String iban;
    
    @NotBlank(message = "Email obbligatoria")
    @Email(message = "Email non valida")
    private String email;
    
    @Pattern(regexp = "^\\+?[0-9\\s\\-\\.\\(\\)]+$", 
             message = "Formato telefono non valido")
    private String phone;
    
    @Pattern(regexp = "^\\+?[0-9\\s\\-\\.\\(\\)]+$", 
             message = "Formato cellulare non valido")
    private String mobile;
    
    @Email(message = "PEC non valida")
    private String pec;
    
    private MaritalStatus maritalStatus;
    private EducationLevel educationLevel;
    private Set<Long> roleIds;
    
    // Password per primo accesso
    @Size(min = 8, message = "Password deve essere almeno 8 caratteri")
    @Pattern(regexp = "^(?=.*[0-9])(?=.*[a-z])(?=.*[A-Z])(?=.*[@#$%^&+=]).*$",
             message = "Password deve contenere maiuscole, minuscole, numeri e caratteri speciali")
    private String password;
    
    // Indirizzo embedded
    private FullAddress residenceAddress;
}
