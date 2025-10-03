package com.veely.dto.vehicle;

import com.veely.model.VehicleBookingStatus;
import lombok.Data;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDateTime;

@Data
public class VehicleBookingRequest {

    private Long id;
    private Long vehicleId;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime startDateTime;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE_TIME)
    private LocalDateTime endDateTime;

    private String title;
    private String requesterName;
    private String requesterContact;
    private String notes;
    private VehicleBookingStatus status;
}
