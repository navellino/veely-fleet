package com.veely.service;

import com.veely.entity.DocumentCategory;
import com.veely.entity.DocumentTypeEntity;
import com.veely.repository.DocumentTypeEntityRepository;
import com.veely.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class DocumentTypeService {
    private final DocumentTypeEntityRepository repository;

    @Transactional(readOnly = true)
    public List<DocumentTypeEntity> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<DocumentTypeEntity> findByCategory(DocumentCategory category) {
        return repository.findByCategory(category);
    }

    @Transactional(readOnly = true)
    public DocumentTypeEntity findByCode(String code) {
        return repository.findByCode(code);
    }
    
    @Transactional(readOnly = true)
    public DocumentTypeEntity findByIdOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Tipo documento non trovato: " + id));
    }
    
    public void delete(Long id) {
        DocumentTypeEntity existing = findByIdOrThrow(id);
        repository.delete(existing);
    }
    
    public DocumentTypeEntity save(DocumentTypeEntity type) {
        return repository.save(type);
    }
}
