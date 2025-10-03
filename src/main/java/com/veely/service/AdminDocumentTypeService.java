package com.veely.service;

import com.veely.entity.AdminDocumentType;
import com.veely.exception.ResourceNotFoundException;
import com.veely.repository.AdminDocumentTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminDocumentTypeService {
    private final AdminDocumentTypeRepository repository;

    @Transactional(readOnly = true)
    public List<AdminDocumentType> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public AdminDocumentType findByIdOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo documento amministrativo non trovato: " + id));
    }

    public AdminDocumentType save(AdminDocumentType type) {
        return repository.save(type);
    }

    public void delete(Long id) {
        AdminDocumentType existing = findByIdOrThrow(id);
        repository.delete(existing);
    }
}
