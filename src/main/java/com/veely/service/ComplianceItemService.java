package com.veely.service;


import com.veely.entity.ComplianceItem;
import com.veely.repository.ComplianceItemRepository;
import com.veely.repository.ComplianceCategoryRepository;
import com.veely.repository.ProjectRepository;
import com.veely.repository.EmployeeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ComplianceItemService {
    private final ComplianceItemRepository repository;
    private final ComplianceCategoryRepository categoryRepo;
    private final ProjectRepository projectRepo;
    private final EmployeeRepository employeeRepo;

    public ComplianceItem create(ComplianceItem item) {
    	if (item.getCategory() != null && item.getCategory().getId() != null) {
            categoryRepo.findById(item.getCategory().getId()).ifPresent(item::setCategory);
        } else {
            item.setCategory(null);
        }
        if (item.getEmployee() != null && item.getEmployee().getId() != null) {
            employeeRepo.findById(item.getEmployee().getId()).ifPresent(item::setEmployee);
        } else {
            item.setEmployee(null);
        }
        if (item.getProject() != null && item.getProject().getId() != null) {
            projectRepo.findById(item.getProject().getId()).ifPresent(item::setProject);
        } else {
            item.setProject(null);
        }
        applyPeriodicity(item);
        return repository.save(item);
    }

    public ComplianceItem update(Long id, ComplianceItem payload) {
        ComplianceItem existing = findByIdOrThrow(id);
        if (payload.getCategory() != null && payload.getCategory().getId() != null) {
            categoryRepo.findById(payload.getCategory().getId()).ifPresent(existing::setCategory);
        } else {
            existing.setCategory(null);
        }
        if (payload.getEmployee() != null && payload.getEmployee().getId() != null) {
            employeeRepo.findById(payload.getEmployee().getId()).ifPresent(existing::setEmployee);
        } else {
            existing.setEmployee(null);
        }
        if (payload.getProject() != null && payload.getProject().getId() != null) {
            projectRepo.findById(payload.getProject().getId()).ifPresent(existing::setProject);
        } else {
            existing.setProject(null);
        }
        existing.setDescription(payload.getDescription());
        existing.setVisitDate(payload.getVisitDate());
        existing.setPeriodicity(payload.getPeriodicity());
        existing.setDueDate(payload.getDueDate());
        applyPeriodicity(existing);
        return repository.save(existing);
    }

    @Transactional(readOnly = true)
    public ComplianceItem findByIdOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new com.veely.exception.ResourceNotFoundException("Adempimento non trovato: " + id));
    }

    @Transactional(readOnly = true)
    public List<ComplianceItem> search(Long categoryId, Long projectId, Long employeeId,
    		LocalDate from, LocalDate to,
            Boolean expired) {
    		return repository.search(categoryId, projectId, employeeId, from, to, expired);	
    }

    @Transactional(readOnly = true)
    public List<ComplianceItem> upcoming(int days) {
        LocalDate threshold = LocalDate.now().plusDays(days);
        return repository.findByDueDateBefore(threshold);
    }

    public void delete(Long id) {
        ComplianceItem item = findByIdOrThrow(id);
        repository.delete(item);
    }
    /**
     * Aggiorna la data di scadenza in base a data visita/corso e periodicità
     * se entrambi presenti.
     */
    private void applyPeriodicity(ComplianceItem item) {
        if (item.getVisitDate() != null && item.getPeriodicity() != null) {
            item.setDueDate(item.getVisitDate().plusYears(item.getPeriodicity()));
        }
    }
}