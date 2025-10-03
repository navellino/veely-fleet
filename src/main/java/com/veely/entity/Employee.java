package com.veely.entity;


import com.veely.model.DocumentType;
import com.veely.model.EducationLevel;
import com.veely.model.FullAddress;
import com.veely.model.Gender;
import com.veely.model.MaritalStatus;
import com.veely.validation.ValidAge;
import jakarta.persistence.*;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Past;
import jakarta.validation.constraints.Pattern;
import jakarta.validation.constraints.Size;
import lombok.*;

import java.time.LocalDate;
import java.util.HashSet;
import java.util.Set;

import org.springframework.format.annotation.DateTimeFormat;

/**
 * Entity Employee: dati anagrafici, contatti, login, stato civile, 
 * titolo di studio, PEC e indirizzo di residenza completo.
 */
@Entity
@Table(name = "employees")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Employee {

    @Id 
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // --- Anagrafica ---
    @NotBlank(message = "{employee.firstName.required}")
    @Size(min = 2, max = 50, message = "{employee.firstName.size}")
    @Column(nullable = false)
    private String firstName;

    @NotBlank(message = "{employee.lastName.required}")
    @Size(min = 2, max = 50, message = "{employee.lastName.size}")
    @Column(nullable = false)
    private String lastName;

    @NotNull(message = "{employee.birthDate.required}")
    @Past(message = "{employee.birthDate.past}")
    @ValidAge(min = 16, message = "Il dipendente deve avere almeno 16 anni")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate birthDate;
    
    private String birthPlace;
    
    @Enumerated(EnumType.STRING)
    private Gender gender;

    @NotBlank(message = "{employee.fiscalCode.required}")
    @Column(nullable = false, unique = true, length = 16)
    private String fiscalCode;
    
    // --- Contatti ---
    @Pattern(regexp = "^$|^[\\s\\+\\-\\(\\)0-9]{8,20}$", 
	         message = "{employee.phone.pattern}")
    private String phone;
    
    @Pattern(regexp = "^$|^[\\s\\+\\-\\(\\)0-9]{8,20}$", 
	         message = "{employee.phone.pattern}") 
    private String mobile;
    
    @Pattern(regexp = "^$|^IT[0-9]{2}[A-Z][0-9]{10}[0-9A-Z]{12}$", 
            message = "{employee.iban.pattern}")
    @Column(nullable = true)
    private String iban;

    @NotBlank(message = "{employee.email.required}")
    @Email(message = "{employee.email.valid}")
    @Column(nullable = false, unique = true)
    private String email;

    /** Password cifrata (BCrypt). */
    @Pattern(regexp = "^$|.{6,}",
            message = "La password deve essere di almeno 6 caratteri")
    @Column(nullable = false)
    private String password;

    /** Ruoli applicativi assegnati all'utente. */
    @ManyToMany(fetch = FetchType.EAGER)
    @JoinTable(name = "employee_roles_link",
        joinColumns = @JoinColumn(name = "employee_id"),
        inverseJoinColumns = @JoinColumn(name = "role_id"))
    private Set<EmployeeRole> roles = new HashSet<>();


    /** Stato civile ← enum con displayName */
    @Enumerated(EnumType.STRING)
    @Column(name = "marital_status")
    private MaritalStatus maritalStatus;

    /** Titolo di studio ← enum con displayName */
    @Enumerated(EnumType.STRING)
    @Column(name = "education_level")
    private EducationLevel educationLevel;

    /** Indirizzo di residenza completo */
    @Valid
    @Embedded
    private FullAddress residenceAddress;

    /** PEC (facoltativa) */
    private String pec;

    // --- Relazioni ---
    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Employment> employments;

    @OneToMany(mappedBy = "employee", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Document> personalDocuments;
    
    @Transient
    public Document getProfilePhoto() {
        return personalDocuments.stream()
            .filter(doc -> doc.getType() == DocumentType.IDENTITY_PHOTO)
            .findFirst()
            .orElse(null);
    }
    
    @Transient
    public Integer getAge() {
        return birthDate == null ? null
                : java.time.Period.between(birthDate, java.time.LocalDate.now()).getYears();
    }
    
    @Transient
    public String getFullName() {
        if (firstName == null && lastName == null) {
            return "";
        }
        if (firstName == null) {
            return lastName;
        }
        if (lastName == null) {
            return firstName;
        }
        return firstName + " " + lastName;
    }
    
}
