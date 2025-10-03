package com.veely.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "task_types")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class TaskType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true, length = 50)
    private String code;

    @Column(nullable = false)
    private String description;

    private boolean byDate;
    private boolean byMileage;
    
    /** Numero di mesi da aggiungere dopo la manutenzione per calcolare la nuova scadenza. */
    private Integer monthsInterval;

    /** Chilometri da aggiungere dopo la manutenzione per calcolare la prossima scadenza. */
    private Integer kmInterval;
    
    /** Indica se il task deve essere creato automaticamente per ogni veicolo. */
    private boolean auto;
}
