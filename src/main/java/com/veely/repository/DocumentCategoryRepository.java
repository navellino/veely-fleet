package com.veely.repository;

import com.veely.entity.DocumentCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface DocumentCategoryRepository extends JpaRepository<DocumentCategory, Long> {
}