package com.veely.repository;

import com.veely.entity.CompanyInfo;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface CompanyInfoRepository extends JpaRepository<CompanyInfo, Long> {

    /**
     * Trova le informazioni aziendali attive
     */
    Optional<CompanyInfo> findByIsActiveTrue();

    /**
     * Trova le informazioni aziendali principali (prima attiva)
     */
    @Query("SELECT c FROM CompanyInfo c WHERE c.isActive = true ORDER BY c.id ASC")
    Optional<CompanyInfo> findPrimaryCompanyInfo();

    /**
     * Verifica se esistono informazioni aziendali
     */
    @Query("SELECT COUNT(c) > 0 FROM CompanyInfo c WHERE c.isActive = true")
    boolean existsActiveCompanyInfo();

    /**
     * Conta le configurazioni attive
     */
    long countByIsActiveTrue();
}

