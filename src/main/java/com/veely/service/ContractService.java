package com.veely.service;

import com.veely.entity.Contract;
import com.veely.exception.ResourceNotFoundException;
import com.veely.model.SupplierContractStatus;
import com.veely.repository.ContractRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ContractService {

    private final ContractRepository contractRepo;

    public Contract create(Contract contract) {
    	if (contract.getAmountNet() == null) {
            contract.setAmountNet(BigDecimal.ZERO);
        }
    	 if (contract.getVatRate() == null) {
             contract.setVatRate(BigDecimal.ZERO);
         }
    	 if (contract.getProject() != null && contract.getProject().getId() == null) {
             contract.setProject(null);
         }
        return contractRepo.save(contract);
    }

    public Contract update(Long id, Contract payload) {
        Contract existing = findByIdOrThrow(id);
        existing.setSupplier(payload.getSupplier());
        existing.setProject(payload.getProject() != null && payload.getProject().getId() != null ? payload.getProject() : null);
        existing.setType(payload.getType());
        existing.setSubject(payload.getSubject());
        existing.setStatus(payload.getStatus());
        existing.setStartDate(payload.getStartDate());
        existing.setEndDate(payload.getEndDate());
        existing.setTerminationNoticeDays(payload.getTerminationNoticeDays());
        existing.setExpiryReminder(payload.getExpiryReminder());
        existing.setAmountNet(payload.getAmountNet() != null ? payload.getAmountNet() : BigDecimal.ZERO);
        existing.setVatRate(payload.getVatRate() != null ? payload.getVatRate() : BigDecimal.ZERO);
        existing.setCurrency(payload.getCurrency());
        existing.setPaymentTerms(payload.getPaymentTerms());
        existing.setPeriodicFee(payload.getPeriodicFee());
        existing.setRecurringFrequency(payload.getRecurringFrequency());
        existing.setNeedsDurc(payload.getNeedsDurc());
        existing.setDurcExpiry(payload.getDurcExpiry());
        existing.setReferencePerson(payload.getReferencePerson());
        return contractRepo.save(existing);
    }

    @Transactional(readOnly = true)
    public Contract findByIdOrThrow(Long id) {
        return contractRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Contratto non trovato: " + id));
    }

    @Transactional(readOnly = true)
    public List<Contract> findAll() {
        return contractRepo.findAll();
    }

    public void delete(Long id) {
        Contract c = findByIdOrThrow(id);
        contractRepo.delete(c);
    }
    
    @Transactional(readOnly = true)
    public ContractStats getStats() {
        long draft = contractRepo.countByStatus(SupplierContractStatus.BOZZA);
        long active = contractRepo.countByStatus(SupplierContractStatus.IN_ESECUZIONE);
        long expiring = contractRepo.countExpiring();
        long expired = contractRepo.countByStatus(SupplierContractStatus.SCADUTO);
        return new ContractStats(draft, active, expiring, expired);
    }

    public record ContractStats(long draftCount, long activeCount, long expiringCount, long expiredCount) {}
}
