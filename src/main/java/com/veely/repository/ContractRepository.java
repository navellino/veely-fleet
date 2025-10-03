package com.veely.repository;

import com.veely.entity.Contract;
import com.veely.model.SupplierContractStatus;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ContractRepository extends JpaRepository<Contract, Long> {
	long countByStatus(SupplierContractStatus status);
	
	@Query(value = """
            SELECT COUNT(*) FROM contracts
            WHERE status = 'IN_ESECUZIONE'
              AND end_date IS NOT NULL
              AND DATEDIFF(end_date, CURRENT_DATE) <= termination_notice_days
              AND DATEDIFF(end_date, CURRENT_DATE) >= 0
            """, nativeQuery = true)
    long countExpiring();
}
