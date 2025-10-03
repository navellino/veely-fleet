package com.veely.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/** Registro dei rifornimenti di carburante per i veicoli. */
@Entity
@Table(name = "refuels")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Refuel {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    @ManyToOne
    @JoinColumn(name = "fuel_card_id")
    private FuelCard fuelCard;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    /** Chilometraggio del veicolo al momento del rifornimento. */
    private Integer mileage;

    /** Quantità di carburante rifornito (litri). */
    private BigDecimal quantity;

    /** Costo totale del rifornimento. */
    private BigDecimal amount;
}
