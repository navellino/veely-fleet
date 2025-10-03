package com.veely.repository;


import com.veely.entity.Document;
import com.veely.model.DocumentType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface DocumentRepository extends JpaRepository<Document, Long> {

    List<Document> findByVehicleId(Long vehicleId);

    List<Document> findByEmploymentIdAndType(Long employmentId, DocumentType type);

    List<Document> findByEmployeeIdAndType(Long employeeId, DocumentType type);
    
    List<Document> findByAssignmentId(Long assignmentId);
    
 // Restituisce tutti i documenti anagrafici legati a un employee
    List<Document> findByEmployeeId(Long employeeId);

    // (utile anche per Employment, Vehicle, Assignment)
    List<Document> findByEmploymentId(Long employmentId);
    
    List<Document> findByCorrespondenceId(Long correspondenceId);
    
    List<Document> findByExpenseItemId(Long expenseItemId);
    
    List<Document> findByMaintenanceId(Long maintenanceId);
    
    List<Document> findByComplianceItemId(Long complianceItemId);
    
    List<Document> findByComplianceItemIdAndType(Long complianceItemId, DocumentType type);
    
 // Nuovo metodo per batch loading
    @Query("SELECT d FROM Document d " +
           "WHERE d.employee.id IN :employeeIds " +
           "AND d.type = :type")
    List<Document> findByEmployeeIdInAndType(@Param("employeeIds") List<Long> employeeIds, 
                                            @Param("type") DocumentType type);
    
    // Query ottimizzata per caricare documenti con employee
    @Query("SELECT d FROM Document d " +
           "LEFT JOIN FETCH d.employee " +
           "WHERE d.employee.id = :employeeId")
    List<Document> findByEmployeeIdWithEmployee(@Param("employeeId") Long employeeId);
    
    // Altri metodi ottimizzati se necessario
    @Query("SELECT d FROM Document d " +
           "LEFT JOIN FETCH d.vehicle " +
           "WHERE d.vehicle.id = :vehicleId")
    List<Document> findByVehicleIdWithVehicle(@Param("vehicleId") Long vehicleId);
    
    /**
     * Trova documenti per tipo e contenuto nel path
     */
    List<Document> findByTypeAndPathContaining(DocumentType type, String pathContains);
    
    /**
     * Trova documenti per tipo
     */
    List<Document> findByType(DocumentType type);
    
    /**
     * Trova documenti che contengono una stringa nel path
     */
    List<Document> findByPathContaining(String pathContains);
    
    List<Document> findByProjectId(Long projectId);
    
    List<Document> findByContractId(Long contractId);

    List<Document> findBySupplierId(Long supplierId);
    
    List<Document> findByInsuranceId(Long insuranceId);
    
    List<Document> findByAdminDocumentId(Long adminDocumentId);
}
