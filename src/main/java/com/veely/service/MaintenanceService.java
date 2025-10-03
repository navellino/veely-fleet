package com.veely.service;

import com.veely.entity.Maintenance;
import com.veely.entity.Supplier;
import com.veely.entity.TaskType;
import com.veely.entity.Vehicle;
import com.veely.service.VehicleMileageService;
import com.veely.model.MileageSource;
import com.veely.exception.ResourceNotFoundException;
import com.veely.repository.MaintenanceRepository;
import com.veely.repository.SupplierRepository;
import com.veely.repository.VehicleRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class MaintenanceService {

    private final MaintenanceRepository maintenanceRepo;
    private final VehicleRepository vehicleRepo;
    private final SupplierRepository supplierRepo;
    private final VehicleTaskService vehicleTaskService;
    private final TaskTypeService taskTypeService;
    private final VehicleMileageService mileageService;

    public Maintenance create(Maintenance maintenance) {
        Vehicle v = vehicleRepo.findById(maintenance.getVehicle().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Veicolo non trovato: " + maintenance.getVehicle().getId()));
        maintenance.setVehicle(v);
        if (maintenance.getSupplier() != null && maintenance.getSupplier().getId() != null) {
            Supplier s = supplierRepo.findById(maintenance.getSupplier().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Fornitore non trovato: " + maintenance.getSupplier().getId()));
            maintenance.setSupplier(s);
        } else {
            maintenance.setSupplier(null);
        }
        if (maintenance.getType() != null && maintenance.getType().getId() != null) {
            TaskType type = taskTypeService.findById(maintenance.getType().getId());
            maintenance.setType(type);
        } else {
            maintenance.setType(null);
        }
        
        Maintenance saved = maintenanceRepo.save(maintenance);
        mileageService.recordMileage(v, saved.getMileage(), MileageSource.MAINTENANCE, saved.getId(), saved.getDate());
        vehicleTaskService.updateAfterMaintenance(saved);
        return saved;
    }

    public Maintenance update(Long id, Maintenance payload) {
        Maintenance existing = findByIdOrThrow(id);
        Vehicle v = vehicleRepo.findById(payload.getVehicle().getId())
                .orElseThrow(() -> new ResourceNotFoundException("Veicolo non trovato: " + payload.getVehicle().getId()));
        existing.setVehicle(v);
        if (payload.getSupplier() != null && payload.getSupplier().getId() != null) {
            Supplier s = supplierRepo.findById(payload.getSupplier().getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Fornitore non trovato: " + payload.getSupplier().getId()));
            existing.setSupplier(s);
        } else {
            existing.setSupplier(null);
        }
        existing.setDate(payload.getDate());
        existing.setMileage(payload.getMileage());
        if (payload.getType() != null && payload.getType().getId() != null) {
            TaskType type = taskTypeService.findById(payload.getType().getId());
            existing.setType(type);
        } else {
            existing.setType(null);
        }
        existing.setCost(payload.getCost());
        existing.setDescription(payload.getDescription());
        mileageService.updateMileage(MileageSource.MAINTENANCE, existing.getId(), v, existing.getMileage(), existing.getDate());
        vehicleTaskService.updateAfterMaintenance(existing);
        
        return existing;
    }

    @Transactional(readOnly = true)
    public Maintenance findByIdOrThrow(Long id) {
        return maintenanceRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Manutenzione non trovata: " + id));
    }

    @Transactional(readOnly = true)
    public List<Maintenance> findAll() {
        return maintenanceRepo.findAll();
    }

    @Transactional(readOnly = true)
    public List<Maintenance> search(String plate, Integer year, Long typeId) {
        String p = (plate != null && !plate.isBlank()) ? plate : null;
        return maintenanceRepo.search(p, year, typeId);
    }
    
    @Transactional(readOnly = true)
    public List<Maintenance> findByVehicle(Long vehicleId) {
        return maintenanceRepo.findByVehicleId(vehicleId);
    }

    public void delete(Long id) {
        Maintenance m = findByIdOrThrow(id);
        mileageService.removeMileage(MileageSource.MAINTENANCE, m.getId());
        maintenanceRepo.delete(m);
    }

    /**
     * Create initial ordinary maintenance schedule for a new vehicle.
     */

    @Transactional(readOnly = true)
    public List<Maintenance> findByYear(int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end   = LocalDate.of(year, 12, 31);
        return maintenanceRepo.findAllByDateBetween(start, end);
    }

    @Transactional(readOnly = true)
    public List<Maintenance> search(String plate, int year, Long typeId) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end   = LocalDate.of(year, 12, 31);
        return maintenanceRepo.searchByFilters(start, end, plate, typeId);
    }

    @Transactional(readOnly = true)
    public long countAll() {
        return maintenanceRepo.count();
    }

    @Transactional(readOnly = true)
    public BigDecimal sumCostYear(int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end   = LocalDate.of(year, 12, 31);
        BigDecimal sum = maintenanceRepo.sumCostBetween(start, end);
        return sum != null ? sum : BigDecimal.ZERO;
    }

    @Transactional(readOnly = true)
    public BigDecimal avgCostYear(int year) {
        LocalDate start = LocalDate.of(year, 1, 1);
        LocalDate end   = LocalDate.of(year, 12, 31);
        Double avg = maintenanceRepo.avgCostBetween(start, end);
        if (avg == null) return BigDecimal.ZERO;
        return BigDecimal.valueOf(avg).setScale(2, RoundingMode.HALF_UP);
    }
}
