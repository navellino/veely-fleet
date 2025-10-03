package com.veely.service;

import com.veely.entity.FuelCard;
import com.veely.exception.BusinessRuleException;
import com.veely.exception.ResourceNotFoundException;
import com.veely.repository.FuelCardRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class FuelCardService {

    private final FuelCardRepository fuelCardRepo;

    public FuelCard create(FuelCard card) {
    	normalizeAssignments(card);
        validateAssignment(card, null);
        card.updateStatus();
        if (card.getVehicle() != null) {
            card.getVehicle().setFuelCard(card);
        }
        return fuelCardRepo.save(card);
    }

    public FuelCard update(Long id, FuelCard payload) {
        FuelCard existing = findByIdOrThrow(id);
        normalizeAssignments(payload);
        validateAssignment(payload, id);
        existing.setCardNumber(payload.getCardNumber());
        existing.setExpiryDate(payload.getExpiryDate());
        existing.setSupplier(payload.getSupplier());
        existing.setEmployee(payload.getEmployee());
        if (existing.getVehicle() != null && (payload.getVehicle() == null || !existing.getVehicle().getId().equals(payload.getVehicle().getId()))) {
            existing.getVehicle().setFuelCard(null);
        }
        if (payload.getVehicle() != null) {
            payload.getVehicle().setFuelCard(existing);
        }
        existing.setVehicle(payload.getVehicle());
        existing.setPlafond(payload.getPlafond());
        existing.updateStatus();
        return existing;
    }

    @Transactional(readOnly = true)
    public FuelCard findByIdOrThrow(Long id) {
        return fuelCardRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Fuel card non trovata: " + id));
    }

    @Transactional(readOnly = true)
    public List<FuelCard> findAll() {
        return fuelCardRepo.findAll();
    }

    public void delete(Long id) {
        FuelCard card = findByIdOrThrow(id);
     // rimuove le associazioni bidirezionali per evitare che
        // Hibernate tenti di salvare nuovamente entità "FuelCard" non persistenti
        if (card.getVehicle() != null) {
            card.getVehicle().setFuelCard(null);
            card.setVehicle(null);
        }
        if (card.getEmployee() != null) {
            card.setEmployee(null);
        }
        fuelCardRepo.delete(card);
    }
    
    public void flush(Long id) {
    	FuelCard card = findByIdOrThrow(id);
    	if (card.getVehicle() != null) {
            card.getVehicle().setFuelCard(null);
            card.setVehicle(null);
        }
        if (card.getEmployee() != null) {
            card.setEmployee(null);
        }
    }
    
    private void validateAssignment(FuelCard card, Long currentId) {
    	if (card.getVehicle() != null && card.getVehicle().getId() != null) {
            fuelCardRepo.findActiveByVehicleId(card.getVehicle().getId())
                    .filter(fc -> currentId == null || !fc.getId().equals(currentId))
                    .ifPresent(fc -> {
                        throw new BusinessRuleException("Veicolo già associato a una fuel card attiva");
                    });
        }
    	if (card.getEmployee() != null && card.getEmployee().getId() != null) {
            fuelCardRepo.findActiveByEmployeeId(card.getEmployee().getId())
                    .filter(fc -> currentId == null || !fc.getId().equals(currentId))
                    .ifPresent(fc -> {
                        throw new BusinessRuleException("Dipendente già associato a una fuel card attiva");
                    });
        }
    }
    
    private void normalizeAssignments(FuelCard card) {
        if (card.getEmployee() != null && card.getEmployee().getId() == null) {
            card.setEmployee(null);
        }
        if (card.getVehicle() != null && card.getVehicle().getId() == null) {
            card.setVehicle(null);
        }
    }
}
