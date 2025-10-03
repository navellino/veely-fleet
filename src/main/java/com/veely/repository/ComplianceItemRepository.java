package com.veely.repository;

import com.veely.entity.ComplianceItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.util.List;

public interface ComplianceItemRepository extends JpaRepository<ComplianceItem, Long> {

    @Query("SELECT c FROM ComplianceItem c WHERE " +
           "(:catId IS NULL OR c.category.id = :catId) AND " +
           "(:projId IS NULL OR c.project.id = :projId) AND " +
           "(:empId IS NULL OR c.employee.id = :empId) AND " +
           "(:from IS NULL OR c.dueDate >= :from) AND " +
           "(:to IS NULL OR c.dueDate <= :to) AND " +
           "(:expired IS NULL OR (CASE WHEN :expired = true THEN c.dueDate < CURRENT_DATE ELSE c.dueDate >= CURRENT_DATE END)) " +
           "ORDER BY c.dueDate")
    List<ComplianceItem> search(@Param("catId") Long categoryId,
                                @Param("projId") Long projectId,
                                @Param("empId") Long employeeId,
                                @Param("from") LocalDate from,
                                @Param("to") LocalDate to,
                                @Param("expired") Boolean expired);

    List<ComplianceItem> findByDueDateBefore(LocalDate date);
    
    void deleteByEmployeeId(Long employeeId);
}
