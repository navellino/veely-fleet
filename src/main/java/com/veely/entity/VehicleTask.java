package com.veely.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import com.veely.model.TaskStatus;

import java.time.LocalDate;

/** Task associato a un veicolo (revisione, tagliando, cambio gomme, ...). */
@Entity
@Table(name = "vehicle_tasks")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleTask {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne
    @JoinColumn(name = "type_id")
    private TaskType type;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate dueDate;

    private Integer dueMileage;
    
    @Enumerated(EnumType.STRING)
    @Builder.Default
    private TaskStatus status = TaskStatus.OPEN;
    
    @Builder.Default
    private boolean executed = false;
}