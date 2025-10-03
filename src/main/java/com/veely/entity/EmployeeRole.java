package com.veely.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "employee_roles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class EmployeeRole {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;
}