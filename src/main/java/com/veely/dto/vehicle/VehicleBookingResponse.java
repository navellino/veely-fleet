package com.veely.dto.vehicle;

import com.veely.model.VehicleBookingStatus;
import lombok.Builder;
import lombok.Value;

import java.time.LocalDateTime;

@Value
@Builder
public class VehicleBookingResponse {
    Long id;
    Long vehicleId;
    String vehicleLabel;
    String title;
    String requesterName;
    String requesterContact;
    VehicleBookingStatus status;
    LocalDateTime startDateTime;
    LocalDateTime endDateTime;
    String notes;
}
