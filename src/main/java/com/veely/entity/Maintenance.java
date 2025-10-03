package com.veely.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Registro delle manutenzioni per i veicoli aziendali.
 */
@Entity
@Table(name = "maintenance")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Maintenance {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne
    private Supplier supplier;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    private Integer mileage;

    @ManyToOne
    @JoinColumn(name = "type_id")
    private TaskType type;

    private BigDecimal cost;

    private String description;
}
