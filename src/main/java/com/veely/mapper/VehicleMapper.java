package com.veely.mapper;

import com.veely.dto.vehicle.VehicleCreateDTO;
import com.veely.dto.vehicle.VehicleDTO;
import com.veely.dto.vehicle.VehicleListDTO;
import com.veely.entity.Assignment;
import com.veely.entity.Vehicle;
import com.veely.model.AssignmentStatus;
import com.veely.service.SupplierService;
import com.veely.service.FuelCardService;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.util.List;

@Mapper(componentModel = "spring", 
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public abstract class VehicleMapper {
    
    @Autowired
    protected SupplierService supplierService;
    
    @Autowired
    protected FuelCardService fuelCardService;
    
    // DTO to Entity
    @Mapping(target = "supplier", source = "supplierId", qualifiedByName = "idToSupplier")
    @Mapping(target = "fuelCard", source = "fuelCardId", qualifiedByName = "idToFuelCard")
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "documents", ignore = true)
    @Mapping(target = "assignments", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "totalFee", ignore = true)
    @Mapping(target = "imagePath", ignore = true)
    @Mapping(target = "inServiceCount", ignore = true)
    @Mapping(target = "assignedCount", ignore = true)
    public abstract Vehicle toEntity(VehicleCreateDTO dto);
    
    // Entity to DTO
    @Mapping(target = "supplierName", source = "supplier.name")
    @Mapping(target = "supplierId", source = "supplier.id")
    @Mapping(target = "fuelCardNumber", source = "fuelCard.cardNumber")
    @Mapping(target = "assignedEmployeeName", source = ".", qualifiedByName = "getAssignedEmployeeName")
    @Mapping(target = "assignedEmployeeId", source = ".", qualifiedByName = "getAssignedEmployeeId")
    @Mapping(target = "hasActiveAssignment", source = ".", qualifiedByName = "hasActiveAssignment")
    @Mapping(target = "daysUntilInsuranceExpiry", source = "insuranceExpiryDate", qualifiedByName = "daysUntil")
    @Mapping(target = "daysUntilCarTaxExpiry", source = "carTaxExpiryDate", qualifiedByName = "daysUntil")
    public abstract VehicleDTO toDto(Vehicle entity);
    
    // Entity to List DTO
    @Mapping(target = "assignedTo", source = ".", qualifiedByName = "getAssignedEmployeeName")
    @Mapping(target = "hasExpiredDocuments", source = ".", qualifiedByName = "hasExpiredDocuments")
    public abstract VehicleListDTO toListDto(Vehicle entity);
    
    // Lists
    public List<VehicleDTO> toDtoList(List<Vehicle> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toDto)
                .collect(java.util.stream.Collectors.toList());
    }

    public List<VehicleListDTO> toListDtoList(List<Vehicle> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toListDto)
                .collect(java.util.stream.Collectors.toList());
    }
    
    // Update entity from DTO
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "documents", ignore = true)
    @Mapping(target = "assignments", ignore = true)
    @Mapping(target = "status", ignore = true)
    @Mapping(target = "supplier", source = "supplierId", qualifiedByName = "idToSupplier")
    @Mapping(target = "fuelCard", source = "fuelCardId", qualifiedByName = "idToFuelCard")
    public abstract void updateEntityFromDto(@MappingTarget Vehicle entity, VehicleCreateDTO dto);
    
    // Named mappings
    @Named("idToSupplier")
    protected com.veely.entity.Supplier idToSupplier(Long supplierId) {
        if (supplierId == null) return null;
        try {
            return supplierService.findByIdOrThrow(supplierId);
        } catch (Exception e) {
            return null;
        }
    }
    
    @Named("idToFuelCard")
    protected com.veely.entity.FuelCard idToFuelCard(Long fuelCardId) {
        if (fuelCardId == null) return null;
        try {
            return fuelCardService.findByIdOrThrow(fuelCardId);
        } catch (Exception e) {
            return null;
        }
    }
    
    @Named("getAssignedEmployeeName")
    protected String getAssignedEmployeeName(Vehicle vehicle) {
        if (vehicle.getAssignments() == null) return null;
        
        return vehicle.getAssignments().stream()
            .filter(a -> a.getStatus() == AssignmentStatus.ASSIGNED)
            .filter(a -> a.getEndDate() == null || a.getEndDate().isAfter(LocalDate.now()))
            .findFirst()
            .map(a -> a.getEmployment().getEmployee().getFirstName() + " " + 
                     a.getEmployment().getEmployee().getLastName())
            .orElse(null);
    }
    
    @Named("getAssignedEmployeeId")
    protected Long getAssignedEmployeeId(Vehicle vehicle) {
        if (vehicle.getAssignments() == null) return null;
        
        return vehicle.getAssignments().stream()
            .filter(a -> a.getStatus() == AssignmentStatus.ASSIGNED)
            .filter(a -> a.getEndDate() == null || a.getEndDate().isAfter(LocalDate.now()))
            .findFirst()
            .map(a -> a.getEmployment().getEmployee().getId())
            .orElse(null);
    }
    
    @Named("hasActiveAssignment")
    protected boolean hasActiveAssignment(Vehicle vehicle) {
        if (vehicle.getAssignments() == null) return false;
        
        return vehicle.getAssignments().stream()
            .anyMatch(a -> a.getStatus() == AssignmentStatus.ASSIGNED &&
                          (a.getEndDate() == null || a.getEndDate().isAfter(LocalDate.now())));
    }
    
    @Named("daysUntil")
    protected Integer daysUntil(LocalDate date) {
        if (date == null) return null;
        long days = ChronoUnit.DAYS.between(LocalDate.now(), date);
        return (int) days;
    }
    
    @Named("hasExpiredDocuments")
    protected boolean hasExpiredDocuments(Vehicle vehicle) {
        LocalDate now = LocalDate.now();
        
        if (vehicle.getInsuranceExpiryDate() != null && vehicle.getInsuranceExpiryDate().isBefore(now)) {
            return true;
        }
        
        if (vehicle.getCarTaxExpiryDate() != null && vehicle.getCarTaxExpiryDate().isBefore(now)) {
            return true;
        }
        
        if (vehicle.getDocuments() != null) {
            return vehicle.getDocuments().stream()
                .anyMatch(d -> d.getExpiryDate() != null && d.getExpiryDate().isBefore(now));
        }
        
        return false;
    }
}
