package com.veely.entity;

import com.veely.model.VehicleBookingStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Entity
@Table(name = "vehicle_bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class VehicleBooking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false, fetch = FetchType.LAZY)
    @JoinColumn(name = "vehicle_id", nullable = false)
    private Vehicle vehicle;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Column(name = "start_datetime", nullable = false)
    private LocalDateTime startDateTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    @Column(name = "end_datetime", nullable = false)
    private LocalDateTime endDateTime;

    /** Titolo sintetico della prenotazione (es. motivo, commessa). */
    @Column(length = 120)
    private String title;

    /** Nome del richiedente/responsabile. */
    @Column(length = 120)
    private String requesterName;

    @Column(length = 255)
    private String requesterContact;

    @Column(length = 1000)
    private String notes;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private VehicleBookingStatus status;

    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @PrePersist
    public void onCreate() {
        if (status == null) {
            status = VehicleBookingStatus.PLANNED;
        }
        if (createdAt == null) {
            createdAt = LocalDateTime.now();
        }
    }
}
