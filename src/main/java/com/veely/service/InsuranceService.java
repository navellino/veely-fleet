package com.veely.service;

import com.veely.entity.Insurance;
import com.veely.entity.Project;
import com.veely.entity.Supplier;
import com.veely.entity.SupplierReferent;
import com.veely.exception.ResourceNotFoundException;
import com.veely.repository.InsuranceRepository;
import com.veely.repository.ProjectRepository;
import com.veely.repository.SupplierRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Comparator;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class InsuranceService {

    private final InsuranceRepository insuranceRepository;
    private final ProjectRepository projectRepository;
    private final SupplierRepository supplierRepository;

    @Transactional(readOnly = true)
    public List<Insurance> findAll() {
        return insuranceRepository.findAll(Sort.by(
                Sort.Order.asc("project.name"),
                Sort.Order.asc("policyNumber")));
    }

    @Transactional(readOnly = true)
    public Insurance findByIdOrThrow(Long id) {
        return insuranceRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Polizza non trovata: " + id));
    }

    @Transactional(readOnly = true)
    public List<Insurance> findByProjectId(Long projectId) {
        return insuranceRepository.findByProjectId(projectId).stream()
                .sorted(Comparator.comparing(Insurance::getExpiryDate, Comparator.nullsLast(Comparator.naturalOrder())))
                .toList();
    }

    public Insurance create(Insurance insurance) {
        resolveAssociations(insurance);
        return insuranceRepository.save(insurance);
    }

    public Insurance update(Long id, Insurance payload) {
        Insurance existing = findByIdOrThrow(id);
        existing.setPolicyNumber(payload.getPolicyNumber());
        existing.setPolicyType(payload.getPolicyType());
        existing.setStartDate(payload.getStartDate());
        existing.setExpiryDate(payload.getExpiryDate());
        existing.setPaymentDate(payload.getPaymentDate());
        existing.setGuaranteedAmount(payload.getGuaranteedAmount());
        existing.setNotes(payload.getNotes());

        resolveAssociations(existing, payload.getProject(), payload.getSupplier(), payload.getSupplierReferent());
        return existing;
    }

    public void delete(Long id) {
        Insurance insurance = findByIdOrThrow(id);
        insuranceRepository.delete(insurance);
    }

    private void resolveAssociations(Insurance insurance) {
        resolveAssociations(insurance, insurance.getProject(), insurance.getSupplier(), insurance.getSupplierReferent());
    }

    private void resolveAssociations(Insurance target, Project project, Supplier supplier, SupplierReferent referent) {
        if (project != null && project.getId() != null) {
            Project managedProject = projectRepository.findById(project.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Commessa non trovata: " + project.getId()));
            target.setProject(managedProject);
        } else {
            target.setProject(null);
        }

        Supplier managedSupplier = null;
        SupplierReferent managedReferent = null;

        if (supplier != null && supplier.getId() != null) {
            managedSupplier = supplierRepository.findWithReferentsById(supplier.getId())
                    .orElseThrow(() -> new ResourceNotFoundException("Fornitore non trovato: " + supplier.getId()));
            if (referent != null && referent.getId() != null) {
                Long referentId = referent.getId();
                managedReferent = managedSupplier.getReferents().stream()
                        .filter(r -> r.getId().equals(referentId))
                        .findFirst()
                        .orElseThrow(() -> new ResourceNotFoundException("Referente non trovato per il fornitore"));
            }
        }

        target.setSupplier(managedSupplier);
        target.setSupplierReferent(managedReferent);
    }
}
