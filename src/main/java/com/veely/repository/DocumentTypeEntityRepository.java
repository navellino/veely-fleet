package com.veely.repository;

import com.veely.entity.DocumentCategory;
import com.veely.entity.DocumentTypeEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DocumentTypeEntityRepository extends JpaRepository<DocumentTypeEntity, Long> {
    List<DocumentTypeEntity> findByCategory(DocumentCategory category);
    DocumentTypeEntity findByCode(String code);
}
