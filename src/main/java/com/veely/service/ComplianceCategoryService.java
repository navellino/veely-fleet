package com.veely.service;

import com.veely.entity.ComplianceCategory;
import com.veely.repository.ComplianceCategoryRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ComplianceCategoryService {
    private final ComplianceCategoryRepository repository;
    
    public ComplianceCategory create(ComplianceCategory category) {
        return repository.save(category);
    }

    public ComplianceCategory update(Long id, ComplianceCategory payload) {
        ComplianceCategory existing = findByIdOrThrow(id);
        existing.setName(payload.getName());
        return repository.save(existing);
    }

    public void delete(Long id) {
        ComplianceCategory c = findByIdOrThrow(id);
        repository.delete(c);
    }

    public List<ComplianceCategory> findAll() {
        return repository.findAll();
    }

    public ComplianceCategory findByIdOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new com.veely.exception.ResourceNotFoundException("Categoria non trovata: " + id));
    }
}
