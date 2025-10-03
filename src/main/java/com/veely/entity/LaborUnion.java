package com.veely.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Rappresenta una sigla sindacale gestibile da impostazioni.
 */
@Entity
@Table(name = "labor_unions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LaborUnion {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Sigla/descrizione del sindacato. */
    @Column(nullable = false, unique = true)
    private String name;
}