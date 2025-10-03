package com.veely.service;

import com.veely.entity.FuelCard;
import com.veely.entity.Refuel;
import com.veely.entity.Vehicle;
import com.veely.exception.ResourceNotFoundException;
import com.veely.model.MileageSource;
import com.veely.repository.FuelCardRepository;
import com.veely.repository.RefuelRepository;
import com.veely.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class RefuelService {

    private final RefuelRepository refuelRepo;
    private final VehicleRepository vehicleRepo;
    private final FuelCardRepository fuelCardRepo;
    private final VehicleMileageService mileageService;
    private final ValidationService validationService;

    public Refuel create(Refuel refuel) {
        Vehicle v = vehicleRepo.findById(refuel.getVehicle().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Veicolo non trovato: " + refuel.getVehicle().getId()));
        refuel.setVehicle(v);
        if (refuel.getFuelCard() != null && refuel.getFuelCard().getId() != null) {
            FuelCard card = fuelCardRepo.findById(refuel.getFuelCard().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Fuel card non trovata: " + refuel.getFuelCard().getId()));
            refuel.setFuelCard(card);
        } else {
            refuel.setFuelCard(null);
        }
        validationService.validateRefuel(refuel);
        Refuel saved = refuelRepo.save(refuel);
        mileageService.recordMileage(v, saved.getMileage(), MileageSource.REFUEL, saved.getId(), saved.getDate());
        return saved;
    }

    public Refuel update(Long id, Refuel payload) {
        Refuel existing = findByIdOrThrow(id);
        Vehicle v = vehicleRepo.findById(payload.getVehicle().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Veicolo non trovato: " + payload.getVehicle().getId()));
        existing.setVehicle(v);
        if (payload.getFuelCard() != null && payload.getFuelCard().getId() != null) {
            FuelCard card = fuelCardRepo.findById(payload.getFuelCard().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Fuel card non trovata: " + payload.getFuelCard().getId()));
            existing.setFuelCard(card);
        } else {
            existing.setFuelCard(null);
        }
        existing.setDate(payload.getDate());
        existing.setMileage(payload.getMileage());
        existing.setQuantity(payload.getQuantity());
        existing.setAmount(payload.getAmount());
        validationService.validateRefuel(existing);
        Refuel saved = refuelRepo.save(existing);
        mileageService.updateMileage(MileageSource.REFUEL, saved.getId(), v, saved.getMileage(), saved.getDate());
        return saved;
    }

    @Transactional(readOnly = true)
    public Refuel findByIdOrThrow(Long id) {
        return refuelRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Rifornimento non trovato: " + id));
    }

    @Transactional(readOnly = true)
    public List<Refuel> findAll() {
        return refuelRepo.findAll();
    }
    
    @Transactional(readOnly = true)
    public List<Refuel> search(Long vehicleId, Long cardId, Integer year,
                               java.time.LocalDate start, java.time.LocalDate end) {
        return refuelRepo.search(vehicleId, cardId, year, start, end);
    }

    public void delete(Long id) {
        Refuel r = findByIdOrThrow(id);
        mileageService.removeMileage(MileageSource.REFUEL, r.getId());
        refuelRepo.delete(r);
    }
}