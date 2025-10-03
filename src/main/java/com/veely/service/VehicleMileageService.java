package com.veely.service;

import com.veely.entity.Vehicle;
import com.veely.entity.VehicleMileage;
import com.veely.model.MileageSource;
import com.veely.repository.VehicleMileageRepository;
import com.veely.repository.VehicleRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Transactional
public class VehicleMileageService {

    private final VehicleMileageRepository mileageRepo;
    private final VehicleRepository vehicleRepo;

    public void recordMileage(Vehicle vehicle, Integer mileage, MileageSource source,
                              Long sourceId, LocalDate date) {
    	if (mileage == null || mileage == 0) {
            VehicleMileage last = mileageRepo.findTopByVehicleAndDateLessThanOrderByDateDescIdDesc(vehicle, date);
            if (last == null) {
                return;
            }
            mileage = last.getMileage();
        }
    	VehicleMileage prev = mileageRepo.findTopByVehicleAndDateLessThanOrderByDateDescIdDesc(vehicle, date);
        if (prev != null && mileage < prev.getMileage()) {
            throw new IllegalArgumentException("Il kilometraggio non può essere inferiore al precedente");
        }
        VehicleMileage entry = VehicleMileage.builder()
                .vehicle(vehicle)
                .mileage(mileage)
                .source(source)
                .sourceId(sourceId)
                .date(date)
                .build();
        mileageRepo.save(entry);
        updateVehicleCurrentMileage(vehicle);
    }

    public void updateMileage(MileageSource source, Long sourceId, Vehicle vehicle,
                              Integer mileage, LocalDate date) {
        if (mileage == null) {
            removeMileage(source, sourceId);
            return;
        }
        if (mileage == 0) {
        	VehicleMileage last = mileageRepo.findTopByVehicleAndDateLessThanOrderByDateDescIdDesc(vehicle, date);
            if (last == null) {
                removeMileage(source, sourceId);
                return;
            }
            mileage = last.getMileage();
        }
        VehicleMileage prev = mileageRepo.findTopByVehicleAndDateLessThanOrderByDateDescIdDesc(vehicle, date);
        if (prev != null && mileage < prev.getMileage()) {
            throw new IllegalArgumentException("Il kilometraggio non può essere inferiore al precedente");
        }
        VehicleMileage entry = mileageRepo.findBySourceAndSourceId(source, sourceId)
                .orElse(VehicleMileage.builder()
                        .vehicle(vehicle)
                        .source(source)
                        .sourceId(sourceId)
                        .build());
        entry.setVehicle(vehicle);
        entry.setMileage(mileage);
        entry.setDate(date);
        mileageRepo.save(entry);
        updateVehicleCurrentMileage(vehicle);
    }

    public void removeMileage(MileageSource source, Long sourceId) {
        mileageRepo.findBySourceAndSourceId(source, sourceId).ifPresent(entry -> {
            Vehicle vehicle = entry.getVehicle();
            mileageRepo.delete(entry);
            VehicleMileage last = mileageRepo.findTopByVehicleOrderByDateDescIdDesc(vehicle);
            vehicle.setCurrentMileage(last != null ? last.getMileage() : null);
            vehicleRepo.save(vehicle);
        });
    }

    public void deleteByVehicle(Long vehicleId) {
        mileageRepo.deleteByVehicleId(vehicleId);
    }

    private void updateVehicleCurrentMileage(Vehicle vehicle) {
        VehicleMileage last = mileageRepo.findTopByVehicleOrderByDateDescIdDesc(vehicle);
        vehicle.setCurrentMileage(last != null ? last.getMileage() : null);
        vehicleRepo.save(vehicle);
    }
    
    @Transactional(readOnly = true)
    public Integer getLastMileage(Vehicle vehicle) {
        VehicleMileage last = mileageRepo.findTopByVehicleOrderByDateDescIdDesc(vehicle);
        return last != null ? last.getMileage() : null;
    }
}