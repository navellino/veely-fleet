package com.veely.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

/**
 * Storico dei luoghi di lavoro per un rapporto di lavoro.
 * Collega un Employment a una commessa (Project) con le date di inizio e fine.
 */
@Entity
@Table(name = "employment_workplaces")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmploymentWorkplace {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Rapporto di lavoro associato. */
    @ManyToOne(optional = false)
    private Employment employment;

    /** Commessa/cantiere in cui lavora il dipendente. */
    @ManyToOne(optional = false)
    private Project project;

    /** Data di inizio dell'assegnazione. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    /** Data di fine dell'assegnazione. */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
    
   // @Builder.Default
    //@Column(nullable = true)
    //private boolean active = false;
}

