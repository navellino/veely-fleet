package com.veely.entity;

import com.veely.model.MileageSource;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Entity
@Table(name = "vehicle_mileage")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleMileage {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "vehicle_id")
    private Vehicle vehicle;

    private Integer mileage;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    @Enumerated(EnumType.STRING)
    private MileageSource source;

    private Long sourceId;
}
