package com.veely.repository;

import com.veely.entity.Supplier;

import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;

public interface SupplierRepository extends JpaRepository<Supplier, Long> {
	
	@EntityGraph(attributePaths = "referents")
    Optional<Supplier> findWithReferentsById(Long id);

    @Override
    @EntityGraph(attributePaths = "referents")
    List<Supplier> findAll();
    
}

