package com.veely.repository;

import com.veely.entity.ProjectInvoice;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Repository delle fatture della commessa.
 */
public interface ProjectInvoiceRepository extends JpaRepository<ProjectInvoice, Long> {
}
