package com.veely.mapper;

import com.veely.dto.employee.EmployeeCreateDTO;
import com.veely.dto.employee.EmployeeDTO;
import com.veely.entity.Employee;
import com.veely.entity.EmployeeRole;
import com.veely.model.DocumentType;
import com.veely.model.EmploymentStatus;
import com.veely.service.EmployeeRoleService;
import org.mapstruct.*;
import org.springframework.beans.factory.annotation.Autowired;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring",
        nullValuePropertyMappingStrategy = NullValuePropertyMappingStrategy.IGNORE)
public abstract class EmployeeMapper {
    
    @Autowired
    protected EmployeeRoleService employeeRoleService;
    
    // DTO to Entity
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employments", ignore = true)
    @Mapping(target = "personalDocuments", ignore = true)
    @Mapping(target = "roles", source = "roleIds", qualifiedByName = "idsToEmployeeRoles")
    @Mapping(target = "password", ignore = true) // Gestita dal service
    public abstract Employee toEntity(EmployeeCreateDTO dto);
    
    // Entity to DTO
    @Mapping(target = "residenceCity", source = "residenceAddress.city")
    @Mapping(target = "residenceProvince", source = "residenceAddress.province")
    @Mapping(target = "currentJobTitle", source = ".", qualifiedByName = "getCurrentJobTitle")
    @Mapping(target = "currentDepartment", source = ".", qualifiedByName = "getCurrentDepartment")
    @Mapping(target = "hasActiveEmployment", source = ".", qualifiedByName = "hasActiveEmployment")
    @Mapping(target = "profilePhotoUrl", source = ".", qualifiedByName = "getProfilePhotoUrl")
    @Mapping(target = "age", source = ".", qualifiedByName = "calculateAge")
    @Mapping(target = "fullName", expression = "java(entity.getFirstName() + \" \" + entity.getLastName())")
    public abstract EmployeeDTO toDto(Employee entity);
    
    // Lists
    public List<EmployeeDTO> toDtoList(List<Employee> entities) {
        if (entities == null) {
            return null;
        }
        return entities.stream()
                .map(this::toDto)
                .collect(java.util.stream.Collectors.toList());
    }
    
    // Update entity from DTO
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "employments", ignore = true)
    @Mapping(target = "personalDocuments", ignore = true)
    @Mapping(target = "password", ignore = true)
    @Mapping(target = "roles", source = "roleIds", qualifiedByName = "idsToEmployeeRoles")
    public abstract void updateEntityFromDto(@MappingTarget Employee entity, EmployeeCreateDTO dto);
    
    // Named mappings
    @Named("idsToEmployeeRoles")
    protected Set<EmployeeRole> idsToEmployeeRoles(Set<Long> ids) {
        if (ids == null) return null;
        return ids.stream()
                .map(id -> {
                    try {
                        return employeeRoleService.findByIdOrThrow(id);
                    } catch (Exception e) {
                        return null;
                    }
                })
                .filter(r -> r != null)
                .collect(java.util.stream.Collectors.toSet());
    }
    
    @Named("getCurrentJobTitle")
    protected String getCurrentJobTitle(Employee employee) {
        if (employee.getEmployments() == null) return null;
        
        return employee.getEmployments().stream()
            .filter(e -> e.getStatus() == EmploymentStatus.ACTIVE)
            .findFirst()
            .map(e -> e.getJobTitle() != null ? e.getJobTitle() : null)
            .orElse(null);
    }
    
    @Named("getCurrentDepartment")
    protected String getCurrentDepartment(Employee employee) {
        if (employee.getEmployments() == null) return null;
        
        return employee.getEmployments().stream()
            .filter(e -> e.getStatus() == EmploymentStatus.ACTIVE)
            .findFirst()
            .map(e -> e.getDepartment())
            .orElse(null);
    }
    
    @Named("hasActiveEmployment")
    protected boolean hasActiveEmployment(Employee employee) {
        if (employee.getEmployments() == null) return false;
        
        return employee.getEmployments().stream()
            .anyMatch(e -> e.getStatus() == EmploymentStatus.ACTIVE);
    }
    
    @Named("getProfilePhotoUrl")
    protected String getProfilePhotoUrl(Employee employee) {
        if (employee.getPersonalDocuments() == null) return null;
        
        return employee.getPersonalDocuments().stream()
            .filter(d -> d.getType() == DocumentType.IDENTITY_PHOTO)
            .findFirst()
            .map(d -> "/fleet/employees/" + employee.getId() + "/photos/" + 
                     d.getPath().substring(d.getPath().lastIndexOf('/') + 1))
            .orElse(null);
    }
    
    @Named("calculateAge")
    protected Integer calculateAge(Employee employee) {
        if (employee.getBirthDate() == null) return null;
        return java.time.Period.between(employee.getBirthDate(), java.time.LocalDate.now()).getYears();
    }
}