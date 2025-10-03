package com.veely.controller;

import com.veely.entity.Vehicle;
import com.veely.service.VehicleBookingService;
import com.veely.service.VehicleService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

import java.time.LocalDate;
import java.util.Comparator;
import java.util.List;

@Controller
@RequestMapping("/fleet/vehicles/bookings")
@RequiredArgsConstructor
public class VehicleBookingController {

    private final VehicleService vehicleService;
    private final VehicleBookingService bookingService;

    @GetMapping
    public String planner(Model model) {
        List<Vehicle> availableVehicles = vehicleService.findAvailable();
        availableVehicles.sort(Comparator.comparing(
                Vehicle::getPlate,
                Comparator.nullsLast(String.CASE_INSENSITIVE_ORDER)));

        model.addAttribute("vehicles", availableVehicles);
        model.addAttribute("vehicleCount", availableVehicles.size());
        model.addAttribute("activeBookings", bookingService.countActive());
        model.addAttribute("bookingsToday", bookingService.countForDate(LocalDate.now()));
        model.addAttribute("bookingsNextSevenDays", bookingService.countUpcomingWithinDays(7));
        model.addAttribute("initialVehicleId", availableVehicles.isEmpty() ? null : availableVehicles.get(0).getId());
        model.addAttribute("firstVehicle", availableVehicles.isEmpty() ? null : availableVehicles.get(0));

        return "fleet/vehicles/bookings";
    }
}
