package com.veely.service;

import com.veely.entity.*;
import com.veely.exception.BusinessValidationException;
import com.veely.model.*;
import com.veely.repository.*;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ValidationService {
    
    private final VehicleRepository vehicleRepo;
    private final AssignmentRepository assignmentRepo;
    private final EmploymentRepository employmentRepo;
    private final RefuelRepository refuelRepo;
    
    /**
     * Valida se un veicolo può essere assegnato
     */
    public void validateVehicleCanBeAssigned(Long vehicleId) {
        Vehicle vehicle = vehicleRepo.findById(vehicleId)
            .orElseThrow(() -> new BusinessValidationException("Veicolo non trovato"));
        
        List<String> errors = new ArrayList<>();
        
        // Verifica stato veicolo
        if (vehicle.getStatus() != VehicleStatus.IN_SERVICE) {
            errors.add("Il veicolo non è in servizio");
        }
        
        // Verifica se già assegnato
        boolean hasActiveAssignment = assignmentRepo
            .findByVehicleIdAndStatus(vehicleId, AssignmentStatus.ASSIGNED)
            .stream()
            .anyMatch(a -> a.getEndDate() == null || a.getEndDate().isAfter(LocalDate.now()));
            
        if (hasActiveAssignment) {
            errors.add("Il veicolo è già assegnato");
        }
        
        // Verifica documenti scaduti
        LocalDate now = LocalDate.now();
        if (vehicle.getInsuranceExpiryDate() != null && 
            vehicle.getInsuranceExpiryDate().isBefore(now)) {
            errors.add("Assicurazione scaduta");
        }
        
        if (vehicle.getCarTaxExpiryDate() != null && 
            vehicle.getCarTaxExpiryDate().isBefore(now)) {
            errors.add("Bollo auto scaduto");
        }
        
        if (!errors.isEmpty()) {
            throw new BusinessValidationException("Veicolo non assegnabile", errors);
        }
    }
    
    /**
     * Valida se un dipendente può ricevere un'assegnazione
     */
    public void validateEmploymentCanReceiveAssignment(Long employmentId) {
        Employment employment = employmentRepo.findById(employmentId)
            .orElseThrow(() -> new BusinessValidationException("Rapporto di lavoro non trovato"));
        
        List<String> errors = new ArrayList<>();
        
        // Verifica stato employment
        if (employment.getStatus() != EmploymentStatus.ACTIVE) {
            errors.add("Il rapporto di lavoro non è attivo");
        }
        
     // Verifica se ha già un veicolo assegnato
        boolean hasActiveAssignment = assignmentRepo
            .findByEmploymentIdAndStatus(employmentId, AssignmentStatus.ASSIGNED)
            .stream()
            .anyMatch(a -> a.getEndDate() == null || a.getEndDate().isAfter(LocalDate.now()));
            
        if (hasActiveAssignment) {
            errors.add("Il dipendente ha già un veicolo assegnato");
        }
        
        if (!errors.isEmpty()) {
            throw new BusinessValidationException("Impossibile assegnare veicolo al dipendente", errors);
        }
    }
    
    /**
     * Valida la coerenza delle date in un'assegnazione
     */
    public void validateAssignmentDates(LocalDate startDate, LocalDate endDate) {
        List<String> errors = new ArrayList<>();
        
        if (startDate == null) {
            errors.add("Data inizio obbligatoria");
        }
        
        if (startDate != null && endDate != null && endDate.isBefore(startDate)) {
            errors.add("La data fine deve essere successiva alla data inizio");
        }

                
        if (!errors.isEmpty()) {
            throw new BusinessValidationException("Date non valide", errors);
        }
    }
    
    /**
     * Valida un rifornimento
     */
    public void validateRefuel(Refuel refuel) {
        List<String> errors = new ArrayList<>();
        
        // Verifica chilometraggio progressivo
        Integer lastMileage = refuelRepo.findLastMileageFromAllSources(
            refuel.getVehicle().getId(), refuel.getDate()
        );
        
        Integer mileage = refuel.getMileage();
        if (mileage == null || mileage == 0) {
            if (lastMileage != null) {
                refuel.setMileage(lastMileage);
            }
        } else if (lastMileage != null && mileage <= lastMileage) {
            errors.add("Il chilometraggio deve essere superiore all'ultimo rifornimento (" + lastMileage + " km)");
        }
        
        // Verifica quantità carburante
        if (refuel.getQuantity() != null && refuel.getQuantity().doubleValue() > 200) {
            errors.add("Quantità carburante non plausibile (max 200 litri)");
        }
        
        // Verifica data non futura
        if (refuel.getDate() != null && refuel.getDate().isAfter(LocalDate.now())) {
            errors.add("La data del rifornimento non può essere futura");
        }
        
        if (!errors.isEmpty()) {
            throw new BusinessValidationException("Rifornimento non valido", errors);
        }
    }
    
    /**
     * Valida la chiusura di un rapporto di lavoro
     */
    public void validateEmploymentTermination(Long employmentId, LocalDate endDate) {
        Employment employment = employmentRepo.findById(employmentId)
            .orElseThrow(() -> new BusinessValidationException("Rapporto di lavoro non trovato"));
        
        List<String> errors = new ArrayList<>();
        
        // Verifica assegnazioni attive
        List<Assignment> activeAssignments = assignmentRepo
            .findByEmploymentIdAndStatus(employmentId, AssignmentStatus.ASSIGNED);
            
        if (!activeAssignments.isEmpty()) {
            errors.add("Il dipendente ha " + activeAssignments.size() + 
                      " veicoli assegnati che devono essere restituiti prima");
        }
        
        // Verifica data fine coerente
        if (endDate != null && employment.getStartDate() != null && 
            endDate.isBefore(employment.getStartDate())) {
            errors.add("La data fine non può essere precedente alla data inizio");
        }
        
        if (!errors.isEmpty()) {
            throw new BusinessValidationException("Impossibile terminare il rapporto di lavoro", errors);
        }
    }
}