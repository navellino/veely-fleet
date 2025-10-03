package com.veely.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Contatto utile associato a una commessa.
 */
@Entity
@Table(name = "project_contacts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectContact {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    private String firstName;
    private String lastName;
    private String role;
    private String phone;
    private String email;
    private String pec;
}

