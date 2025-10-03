package com.veely.service;

import com.veely.entity.Project;
import com.veely.entity.Supplier;
import com.veely.entity.SupplierReferent;
import com.veely.exception.ResourceNotFoundException;
import com.veely.model.ProjectStatus;
import com.veely.repository.ProjectRepository;
import com.veely.repository.SupplierRepository;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class ProjectService {
    private final ProjectRepository projectRepo;
   

    public Project create(Project project) {
    	 if (project.getContacts() != null) {
             project.getContacts().forEach(c -> c.setProject(project));
         }
    	 if (project.getInvoices() != null) {
             project.getInvoices().forEach(i -> i.setProject(project));
         }
        return projectRepo.save(project);
    }

    public Project update(Long id, Project payload) {
        Project existing = findByIdOrThrow(id);
        existing.setCode(payload.getCode());
        existing.setName(payload.getName());
        existing.setCig(payload.getCig());
        existing.setCup(payload.getCup());
        existing.setManager(payload.getManager());
        existing.setStartDate(payload.getStartDate());
        existing.setEndDate(payload.getEndDate());
        existing.setStatus(payload.getStatus());
        existing.setAddress(payload.getAddress());
        existing.setWorkDescription(payload.getWorkDescription());
        existing.setValue(payload.getValue());
        existing.setAdvanceAmount(payload.getAdvanceAmount());

        existing.getContacts().clear();
        if (payload.getContacts() != null) {
            payload.getContacts().forEach(c -> {
                c.setProject(existing);
                existing.getContacts().add(c);
            });
        }
        
        existing.getInvoices().clear();
        if (payload.getInvoices() != null) {
            payload.getInvoices().forEach(i -> {
                i.setProject(existing);
                existing.getInvoices().add(i);
            });
        }
        
        return projectRepo.save(existing);
    }

    @Transactional(readOnly = true)
    public Project findByIdOrThrow(Long id) {
        return projectRepo.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Commessa non trovata: " + id));
    }

    @Transactional(readOnly = true)
    public List<Project> findAll() {
        return projectRepo.findAll();
    }
    
    /**
     * Restituisce tutte le commesse con lo stato specificato.
     */
    @Transactional(readOnly = true)
    public List<Project> findByStatus(ProjectStatus status) {
        return projectRepo.findByStatus(status);
    }

    /**
     * Restituisce solo le commesse attive.
     */
    @Transactional(readOnly = true)
    public List<Project> findActive() {
        return findByStatus(ProjectStatus.ATTIVA);
    }

    public void delete(Long id) {
        Project p = findByIdOrThrow(id);
        projectRepo.delete(p);
    }
}
