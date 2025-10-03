package com.veely.repository;

import java.time.LocalDate;
import java.util.Collection;
import java.util.List;
import java.util.Optional;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import com.veely.entity.Employee;
import com.veely.entity.Employment;
import com.veely.model.EmploymentStatus;

public interface EmploymentRepository extends JpaRepository<Employment, Long> {
	/** Rapporto di lavoro attivo per una persona – ce ne può essere al massimo uno. */
    Optional<Employment> findFirstByEmployeeIdAndStatus(Long employeeId, EmploymentStatus status);

    /** Tutti i rapporti attivi in una certa filiale. */
    List<Employment> findByBranchAndStatus(String branch, EmploymentStatus status);
    
    /**
     * Filtra i rapporti per stato (ACTIVE, TERMINATED, SUSPENDED) con paginazione
     */
    Page<Employment> findByStatus(EmploymentStatus status, Pageable pageable);

    /**
     * Cerca per jobTitle contenente la keyword (ignore case) con paginazione
     */
    Page<Employment> findByJobTitleIgnoreCaseContaining(String jobTitle, Pageable pageable);
    
    /** Conta i rapporti di lavoro per stato. */
    long countByStatus(EmploymentStatus status);
    
    /**
     * Recupera un rapporto di lavoro includendo la lista dei luoghi di lavoro
     * per evitare LazyInitializationException quando la sessione è chiusa.
     */
    @EntityGraph(attributePaths = {"employee", "workplaces"})
    @Query("select e from Employment e where e.id = :id")
    Optional<Employment> findByIdWithWorkplaces(Long id);
    
    @Query("""
            select distinct emp from Employment emp
            join emp.employee person
             left join emp.workplaces w
            left join w.project p
            where lower(person.firstName) like lower(concat('%', :kw, '%'))
               or lower(person.lastName) like lower(concat('%', :kw, '%'))
               or lower(emp.matricola) like lower(concat('%', :kw, '%'))
               or lower(emp.jobTitle) like lower(concat('%', :kw, '%'))
               or lower(emp.contractLevel) like lower(concat('%', :kw, '%'))
               or lower(emp.branch) like lower(concat('%', :kw, '%'))
               or lower(coalesce(p.name,'')) like lower(concat('%', :kw, '%'))
        """)
    Page<Employment> searchByKeyword(@Param("kw") String keyword, Pageable pageable);

        @Query("""
            select emp from Employment emp
            join emp.employee person
            where emp.status = :status and (
                lower(person.firstName) like lower(concat('%', :kw, '%'))
                or lower(person.lastName) like lower(concat('%', :kw, '%'))
                or lower(emp.matricola) like lower(concat('%', :kw, '%'))
                or lower(emp.jobTitle) like lower(concat('%', :kw, '%'))
                or lower(emp.contractLevel) like lower(concat('%', :kw, '%'))
                or lower(emp.branch) like lower(concat('%', :kw, '%'))
            )
        """)
        Page<Employment> searchByStatusAndKeyword(@Param("status") EmploymentStatus status, @Param("kw") String keyword, Pageable pageable);
    
    
    List<Employment> findByEmployeeId(Long employeeId);
    
    // Se Employment ha un campo Employee employee, usiamo la property path employee.id
    List<Employment> findByEmployeeIdIn(Collection<Long> employeeIds);
    
    @Query("select e from Employee e left join e.employments emp with emp.status = :status where emp.id is null")
    List<Employee> findAvailableForEmployment(@Param("status") EmploymentStatus status);
    
    @Modifying
    @Query("update Employment e set e.status = :status where e.endDate < :today and e.status <> :status")
    int markExpiredAsTerminated(@Param("today") LocalDate today, @Param("status") EmploymentStatus status);
    
    @Query("""
            select distinct emp from Employment emp
            join emp.employee person
            left join emp.workplaces w
            left join w.project p
            where (:keyword is null or (
                    lower(person.firstName) like lower(concat('%', :keyword, '%'))
                    or lower(person.lastName) like lower(concat('%', :keyword, '%'))
                    or lower(emp.matricola) like lower(concat('%', :keyword, '%'))
                    or lower(emp.jobTitle) like lower(concat('%', :keyword, '%'))
                    or lower(emp.contractLevel) like lower(concat('%', :keyword, '%'))
                    or lower(emp.branch) like lower(concat('%', :keyword, '%'))
            ))
            and (:status is null or emp.status = :status)
            and (:projectId is null or (
                    p.id = :projectId
                    and (w.startDate is null or w.startDate <= :today)
                    and (w.endDate is null or w.endDate >= :today)
            ))
        """)
    Page<Employment> searchByFilters(@Param("keyword") String keyword,
                                     @Param("status") EmploymentStatus status,
                                     @Param("projectId") Long projectId,
                                     @Param("today") LocalDate today,
                                     Pageable pageable);

    
}
