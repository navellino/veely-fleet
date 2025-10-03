package com.veely.service;


import com.veely.entity.Assignment;
import com.veely.entity.Vehicle;
import com.veely.exception.ResourceNotFoundException;
import com.veely.model.AssignmentStatus;
import com.veely.model.AssignmentType;
import com.veely.model.VehicleStatus;
import com.veely.repository.AssignmentRepository;
import com.veely.repository.ProjectRepository;
import com.veely.repository.VehicleRepository;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class AssignmentService {

    private final AssignmentRepository assignmentRepo;
    private final VehicleRepository vehicleRepo;
    private final ValidationService validationService;
    private final ProjectRepository projectRepo;

    /** Crea una nuova assegnazione (status default ACTIVE) */
    public Assignment create(Assignment assignment) {
    	log.info("Creazione nuova assegnazione per veicolo ID: {} e employment ID: {}", 
                assignment.getVehicle().getId(), assignment.getEmployment().getId());
    	
    	validationService.validateVehicleCanBeAssigned(assignment.getVehicle().getId());
        validationService.validateEmploymentCanReceiveAssignment(assignment.getEmployment().getId());
        validationService.validateAssignmentDates(
        		assignment.getStartDate(),
                assignment.getEndDate()
        );
    	
        if (assignment.getStatus() == null) {
            assignment.setStatus(AssignmentStatus.ASSIGNED);
        }
        // ensure vehicle entity is managed
        if (assignment.getVehicle() != null && assignment.getVehicle().getId() != null) {
            assignment.setVehicle(
                    vehicleRepo.findById(assignment.getVehicle().getId())
                            .orElseThrow(() -> new ResourceNotFoundException("Veicolo non trovato: " + assignment.getVehicle().getId())));
        }
        if (assignment.getAccolloProject() != null && assignment.getAccolloProject().getId() != null) {
            assignment.setAccolloProject(
                    projectRepo.findById(assignment.getAccolloProject().getId())
                            .orElseThrow(() -> new ResourceNotFoundException("Commessa non trovata: " + assignment.getAccolloProject().getId())));
        }
        updateVehicleStatus(assignment);
        Assignment saved = assignmentRepo.save(assignment);
        // persist vehicle status change
        vehicleRepo.save(assignment.getVehicle());
        return saved;
    }

    /** Aggiorna un'assegnazione esistente */
    public Assignment update(Long id, Assignment payload) {
        Assignment existing = findByIdOrThrow(id);
        Vehicle previousVehicle = existing.getVehicle();
        
        existing.setEmployment(payload.getEmployment());
        if (payload.getVehicle() != null && payload.getVehicle().getId() != null) {
            existing.setVehicle(
                    vehicleRepo.findById(payload.getVehicle().getId())
                            .orElseThrow(() -> new ResourceNotFoundException("Veicolo non trovato: " + payload.getVehicle().getId())));
        }
        existing.setStartDate(payload.getStartDate());
        existing.setEndDate(payload.getEndDate());
        if (payload.getAccolloProject() != null && payload.getAccolloProject().getId() != null) {
            existing.setAccolloProject(
                    projectRepo.findById(payload.getAccolloProject().getId())
                            .orElseThrow(() -> new ResourceNotFoundException("Commessa non trovata: " + payload.getAccolloProject().getId())));
        } else {
            existing.setAccolloProject(null);
        }
        if (payload.getStatus() != null) {
            existing.setStatus(payload.getStatus());
        }
        
        if (previousVehicle != null &&
                (payload.getVehicle() == null ||
                 !previousVehicle.getId().equals(payload.getVehicle().getId()))) {
                previousVehicle.setStatus(VehicleStatus.IN_SERVICE);
            }
        
        updateVehicleStatus(existing);
     // persist status updates for involved vehicles
        if (existing.getVehicle() != null) {
            vehicleRepo.save(existing.getVehicle());
        }
        if (previousVehicle != null && !previousVehicle.getId().equals(existing.getVehicle().getId())) {
            vehicleRepo.save(previousVehicle);
        }
        return existing;
    }

    /** Trova per ID o lancia ResourceNotFoundException */
    @Transactional(readOnly = true)
    public Assignment findByIdOrThrow(Long id) {
        return assignmentRepo.findById(id)
            .orElseThrow(() -> new ResourceNotFoundException("Assegnazione non trovata: " + id));
    }

    /** Lista tutte le assegnazioni */
    @Transactional(readOnly = true)
    public List<Assignment> findAll() {
        return assignmentRepo.findAll();
    }

    /** Lista paginata */
    @Transactional(readOnly = true)
    public Page<Assignment> findAll(Pageable pageable) {
        return assignmentRepo.findAll(pageable);
    }

    /** Filtra per stato con paginazione */
    @Transactional(readOnly = true)
    public Page<Assignment> findByStatus(AssignmentStatus status, Pageable pageable) {
        return assignmentRepo.findByStatus(status, pageable);
    }

    /** Elimina un'assegnazione */
    public void delete(Long id) {
        Assignment a = findByIdOrThrow(id);
        assignmentRepo.delete(a);
    }
    
    /**
     * Aggiorna lo stato di tutte le assegnazioni scadute e libera i veicoli.
     */
    public void releaseExpiredAssignments() {
        LocalDate today = LocalDate.now();
        List<Assignment> expired = assignmentRepo.findByStatusAndEndDateBefore(AssignmentStatus.ASSIGNED, today);
        for (Assignment a : expired) {
            a.setStatus(AssignmentStatus.RETURNED);
            updateVehicleStatus(a);
        }
    }
    
    /** Restituisce l'assegnazione attiva per un veicolo, se presente. */
    @Transactional(readOnly = true)
    public Assignment findActiveByVehicle(Long vehicleId) {
        List<Assignment> list = assignmentRepo.findByVehicleIdAndStatus(vehicleId, AssignmentStatus.ASSIGNED);
        LocalDate today = LocalDate.now();
        return list.stream()
                .filter(a -> a.getEndDate() == null || !a.getEndDate().isBefore(today))
                .findFirst()
                .orElse(null);
    }

    /** Aggiorna lo stato del veicolo in base all'assegnazione. */
    private void updateVehicleStatus(Assignment assignment) {
        Vehicle v = assignment.getVehicle();
        if (v == null) return;
        LocalDate end = assignment.getEndDate();
        boolean active = assignment.getStatus() == AssignmentStatus.ASSIGNED &&
                (end == null || !end.isBefore(LocalDate.now()));
        v.setStatus(active ? VehicleStatus.ASSIGNED : VehicleStatus.IN_SERVICE);
    }
    
    /** Storico assegnazioni per un veicolo. */
    @Transactional(readOnly = true)
    public List<Assignment> findHistoryByVehicle(Long vehicleId) {
        return assignmentRepo.findByVehicleIdOrderByStartDateDesc(vehicleId);
    }
}