package com.veely.controller;

import com.veely.entity.Refuel;
import com.veely.service.RefuelService;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/api/refuels")
@RequiredArgsConstructor
public class RefuelRestController {

    private final RefuelService refuelService;

    @GetMapping
    public List<Refuel> search(
            @RequestParam(value = "vehicleId", required = false) Long vehicleId,
            @RequestParam(value = "cardId", required = false) Long cardId,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "from", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam(value = "to", required = false)
            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        return refuelService.search(vehicleId, cardId, year, from, to);
    }

    @GetMapping("/{id}")
    public Refuel getById(@PathVariable Long id) {
        return refuelService.findByIdOrThrow(id);
    }

    @PostMapping
    public Refuel create(@RequestBody Refuel refuel) {
        return refuelService.create(refuel);
    }

    @PutMapping("/{id}")
    public Refuel update(@PathVariable Long id, @RequestBody Refuel refuel) {
        return refuelService.update(id, refuel);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        refuelService.delete(id);
    }
}

