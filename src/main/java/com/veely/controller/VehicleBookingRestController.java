package com.veely.controller;

import com.veely.dto.vehicle.VehicleBookingRequest;
import com.veely.dto.vehicle.VehicleBookingResponse;
import com.veely.exception.BusinessValidationException;
import com.veely.service.VehicleBookingService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@RestController
@RequestMapping("/api/vehicle-bookings")
@RequiredArgsConstructor
public class VehicleBookingRestController {

    private final VehicleBookingService bookingService;

    @GetMapping
    public List<VehicleBookingResponse> listByVehicle(
            @RequestParam("vehicleId") Long vehicleId,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return bookingService.findByVehicle(vehicleId, from, to);
    }

    @PostMapping
    public ResponseEntity<?> create(@RequestBody VehicleBookingRequest request) {
        try {
            VehicleBookingResponse response = bookingService.create(request);
            return ResponseEntity.status(HttpStatus.CREATED).body(response);
        } catch (BusinessValidationException ex) {
            return buildValidationResponse(ex);
        }
    }

    @PutMapping("/{id}")
    public ResponseEntity<?> update(@PathVariable Long id, @RequestBody VehicleBookingRequest request) {
        try {
            VehicleBookingResponse response = bookingService.update(id, request);
            return ResponseEntity.ok(response);
        } catch (BusinessValidationException ex) {
            return buildValidationResponse(ex);
        }
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable Long id) {
        bookingService.delete(id);
        return ResponseEntity.noContent().build();
    }

    private ResponseEntity<Map<String, Object>> buildValidationResponse(BusinessValidationException ex) {
        Map<String, Object> body = new HashMap<>();
        body.put("message", ex.getMessage());
        body.put("errors", ex.getErrors());
        return ResponseEntity.badRequest().body(body);
    }
}
