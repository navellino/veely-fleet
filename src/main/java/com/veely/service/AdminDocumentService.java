package com.veely.service;

import com.veely.dto.document.DocumentStatistics;
import com.veely.entity.AdminDocument;
import com.veely.exception.ResourceNotFoundException;
import com.veely.repository.AdminDocumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class AdminDocumentService {
    private final AdminDocumentRepository repository;

 // Days before expiry to consider a document "expiring soon"
    private static final int EXPIRY_WARNING_DAYS = 30;
    
    @Transactional(readOnly = true)
    public List<AdminDocument> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public AdminDocument findByIdOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Documento amministrativo non trovato: " + id));
    }

    public AdminDocument save(AdminDocument document) {
    	 // Ensure optional relationships are handled correctly
        // When the responsible employee is not selected in the UI,
        // Spring's data binding creates an empty Employee instance
        // with a null id. Persisting this would cause Hibernate to
        // attempt to save the transient Employee, resulting in a
        // TransientObjectException. We explicitly set the relation
        // to null when the id is missing to avoid this.
        if (document.getResponsible() != null && document.getResponsible().getId() == null) {
            document.setResponsible(null);
        }
        return repository.save(document);
    }

    public void delete(Long id) {
        AdminDocument existing = findByIdOrThrow(id);
        repository.delete(existing);
    }
    
    /**
     * Calculate document statistics for dashboard
     */
    @Transactional(readOnly = true)
    public DocumentStatistics calculateStatistics() {
        List<AdminDocument> documents = findAll();
        LocalDate today = LocalDate.now();
        LocalDate warningDate = today.plusDays(EXPIRY_WARNING_DAYS);
        
        long totalCount = documents.size();
        long validCount = 0;
        long expiringSoonCount = 0;
        long expiredCount = 0;
        long noExpiryCount = 0;
        
        for (AdminDocument document : documents) {
            LocalDate expiryDate = document.getExpiryDate();
            
            if (expiryDate == null) {
                noExpiryCount++;
            } else if (expiryDate.isBefore(today)) {
                expiredCount++;
            } else if (expiryDate.isBefore(warningDate) || expiryDate.isEqual(warningDate)) {
                expiringSoonCount++;
            } else {
                validCount++;
            }
        }
        
        return DocumentStatistics.builder()
                .totalCount(totalCount)
                .validCount(validCount)
                .expiringSoonCount(expiringSoonCount)
                .expiredCount(expiredCount)
                .noExpiryCount(noExpiryCount)
                .build();
    }
    
    /**
     * Get documents expiring within specified days
     */
    @Transactional(readOnly = true)
    public List<AdminDocument> findExpiringWithinDays(int days) {
        LocalDate today = LocalDate.now();
        LocalDate futureDate = today.plusDays(days);
        return repository.findByExpiryDateBetween(today, futureDate);
    }
    
    /**
     * Get expired documents
     */
    @Transactional(readOnly = true)
    public List<AdminDocument> findExpired() {
        LocalDate today = LocalDate.now();
        return repository.findByExpiryDateBefore(today);
    }
}