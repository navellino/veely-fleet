package com.veely.repository;

import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.veely.entity.Employee;
import com.veely.model.EmploymentStatus;


public interface EmployeeRepository extends JpaRepository<Employee, Long> {
    
	// Query ottimizzata con fetch join
    @Query(value = "SELECT DISTINCT e FROM Employee e " +
            "LEFT JOIN FETCH e.employments emp " +
            "LEFT JOIN FETCH e.roles",
            countQuery = "SELECT COUNT(DISTINCT e) FROM Employee e")
    Page<Employee> findAllWithEmployments(Pageable pageable);
    
    @Query("SELECT e FROM Employee e " +
           "LEFT JOIN FETCH e.employments " +
           "LEFT JOIN FETCH e.personalDocuments " +
           "LEFT JOIN FETCH e.roles " +
           "WHERE e.id = :id")
    Optional<Employee> findByIdWithAllRelations(@Param("id") Long id);
    
    // Batch loading per documenti
    @Query("SELECT DISTINCT e FROM Employee e " +
           "LEFT JOIN FETCH e.personalDocuments " +
           "WHERE e IN :employees")
    List<Employee> loadDocumentsForEmployees(@Param("employees") List<Employee> employees);
    
    boolean existsByEmail(String email);

	Optional<Employee> findByEmail(String email);
	
	/** Controlla se esiste già un altro utente con lo stesso fiscalCode (CF). */
    boolean existsByFiscalCode(String fiscalCode);
	
    Optional<Employee> findByFiscalCodeIgnoreCase(String fiscalCode);
    
    Page<Employee> findByFirstNameIgnoreCaseContainingOrLastNameIgnoreCaseContaining(
        String firstName, String lastName, Pageable pageable);
    
    @Query("SELECT e FROM Employee e WHERE e.id NOT IN " +
    		"(SELECT emp.employee.id FROM Employment emp WHERE emp.status = :status)")
    List<Employee> findAvailableForEmployment(@Param("status") EmploymentStatus status);
    
    @Query("""
            SELECT e FROM Employee e
            WHERE NOT EXISTS (
                SELECT fc FROM FuelCard fc
                WHERE fc.employee = e
            )
            """)
    List<Employee> findWithoutFuelCard();
    
    boolean existsByRoles_Id(Long roleId);
}
