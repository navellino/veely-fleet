package com.veely.repository;

import com.veely.entity.ComplianceCategory;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ComplianceCategoryRepository extends JpaRepository<ComplianceCategory, Long> {
}