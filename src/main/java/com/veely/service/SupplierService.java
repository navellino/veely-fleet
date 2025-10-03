package com.veely.service;

import com.veely.entity.Supplier;
import com.veely.dto.supplier.SupplierOptionDTO;
import com.veely.exception.ResourceNotFoundException;
import com.veely.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional
public class SupplierService {

    private final SupplierRepository supplierRepo;

    public Supplier create(Supplier supplier) {
    	if (supplier.getReferents() != null) {
            supplier.getReferents().forEach(r -> r.setSupplier(supplier));
        }
        return supplierRepo.save(supplier);
    }

    public Supplier update(Long id, Supplier payload) {
        Supplier existing = findByIdOrThrow(id);
        existing.setName(payload.getName());
        existing.setVatNumber(payload.getVatNumber());
        existing.setCompanyPhone(payload.getCompanyPhone());
        existing.setCompanyEmail(payload.getCompanyEmail());
        existing.setPec(payload.getPec());
        existing.setIban(payload.getIban());
        existing.setSdiCode(payload.getSdiCode());
        existing.setAddress(payload.getAddress());
        existing.getReferents().clear();
        if (payload.getReferents() != null) {
            payload.getReferents().forEach(r -> {
                r.setSupplier(existing);
                existing.getReferents().add(r);
            });
        }
        return supplierRepo.save(existing);
    }

    @Transactional(readOnly = true)
    public Supplier findByIdOrThrow(Long id) {
    	return supplierRepo.findWithReferentsById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Fornitore non trovato: " + id));
    }

    @Transactional(readOnly = true)
    public List<Supplier> findAll() {
        return supplierRepo.findAll();
    }

    @Transactional(readOnly = true)
    public List<SupplierOptionDTO> findAllForSelection() {
        return supplierRepo.findAll().stream()
                .map(SupplierOptionDTO::fromEntity)
                .collect(Collectors.toList());
    }
    
    public void delete(Long id) {
        Supplier s = findByIdOrThrow(id);
        supplierRepo.delete(s);
    }
}

