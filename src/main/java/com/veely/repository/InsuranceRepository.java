package com.veely.repository;

import com.veely.entity.Insurance;

import java.util.List;

import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InsuranceRepository extends JpaRepository<Insurance, Long> {

    @Override
    @EntityGraph(attributePaths = {"project", "supplier", "supplierReferent", "documents"})
    List<Insurance> findAll();

    @Override
    @EntityGraph(attributePaths = {"project", "supplier", "supplierReferent", "documents"})
    List<Insurance> findAll(Sort sort);

    @EntityGraph(attributePaths = {"project", "supplier", "supplierReferent", "documents"})
    List<Insurance> findByProjectId(Long projectId);
}
