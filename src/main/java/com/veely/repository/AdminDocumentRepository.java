package com.veely.repository;

import com.veely.entity.AdminDocument;

import java.time.LocalDate;
import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

public interface AdminDocumentRepository extends JpaRepository<AdminDocument, Long> {
	
	/**
     * Find documents expiring between two dates
     */
    List<AdminDocument> findByExpiryDateBetween(LocalDate startDate, LocalDate endDate);
    
    /**
     * Find documents that have already expired
     */
    List<AdminDocument> findByExpiryDateBefore(LocalDate date);
    
    /**
     * Find documents without expiry date
     */
    List<AdminDocument> findByExpiryDateIsNull();
    
    /**
     * Count documents by expiry status - for performance optimization
     */
    @Query("SELECT COUNT(d) FROM AdminDocument d WHERE d.expiryDate IS NULL")
    long countDocumentsWithoutExpiry();
    
    @Query("SELECT COUNT(d) FROM AdminDocument d WHERE d.expiryDate < :today")
    long countExpiredDocuments(@Param("today") LocalDate today);
    
    @Query("SELECT COUNT(d) FROM AdminDocument d WHERE d.expiryDate BETWEEN :today AND :warningDate")
    long countExpiringSoonDocuments(@Param("today") LocalDate today, @Param("warningDate") LocalDate warningDate);
    
    @Query("SELECT COUNT(d) FROM AdminDocument d WHERE d.expiryDate > :warningDate")
    long countValidDocuments(@Param("warningDate") LocalDate warningDate);
    
}
