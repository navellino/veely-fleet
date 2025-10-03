package com.veely.service;

import com.veely.entity.Maintenance;
import com.veely.entity.TaskType;
import com.veely.entity.Vehicle;
import com.veely.entity.VehicleTask;
import com.veely.model.TaskStatus;
import com.veely.repository.VehicleRepository;
import com.veely.repository.VehicleTaskRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDate;
import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class VehicleTaskService {

    private final VehicleTaskRepository taskRepo;
    private final VehicleRepository vehicleRepo;
    private final TaskTypeService taskTypeService;
    
    /**
     * Create the default tasks for the given vehicle if they are missing.
     * This method is idempotent and can be called whenever a vehicle is saved
     * or updated to ensure all standard tasks are present.
     */
    public void ensureTasksExist(Vehicle vehicle) {
        
        for (TaskType tt : taskTypeService.findAll()) {
            if (!tt.isAuto()) continue;

            DueComputation due = computeDueData(vehicle, tt);
            createIfMissing(vehicle, tt, due.dueDate(), due.dueMileage());
            }
    }

    private void createIfMissing(Vehicle vehicle, TaskType type,
                                 LocalDate dueDate, Integer dueMileage) {
    	if (taskRepo.findByVehicleIdAndTypeAndStatus(vehicle.getId(), type, TaskStatus.OPEN) == null) {
            taskRepo.save(VehicleTask.builder()
                    .vehicle(vehicle)
                    .type(type)
                    .dueDate(dueDate)
                    .dueMileage(dueMileage)
                    .status(TaskStatus.OPEN)
                    .build());
        }
    }
    
    private VehicleTask buildTask(Vehicle vehicle, TaskType type) {
    	DueComputation due = computeDueData(vehicle, type);
        return VehicleTask.builder()
                .vehicle(vehicle)
                .type(type)
                .dueDate(due.dueDate())
                .dueMileage(due.dueMileage())
                .status(TaskStatus.OPEN)
                .build();
    }
    
    public void createInitialTasks(Vehicle vehicle) {
        for (TaskType tt : taskTypeService.findAll()) {
            if (!tt.isAuto()) continue;
            DueComputation due = computeDueData(vehicle, tt);
            taskRepo.save(VehicleTask.builder()
                    .vehicle(vehicle)
                    .type(tt)
                    .dueDate(due.dueDate())
                    .dueMileage(due.dueMileage())
                    .status(TaskStatus.OPEN)
                    .build());
        }
    }

    private DueComputation computeDueData(Vehicle vehicle, TaskType type) {
        LocalDate referenceDate = resolveReferenceDate(vehicle);
        Integer baseMileage = vehicle.getCurrentMileage();

        LocalDate dueDate = null;
        Integer dueMileage = null;

        switch (type.getCode()) {
        case "REVISION" -> dueDate = referenceDate.plusYears(4);
            case "ORDINARY_SERVICE" -> {
                int months = type.getMonthsInterval() != null ? type.getMonthsInterval() : 12;
                dueDate = referenceDate.plusMonths(months);
                int interval = type.getKmInterval() != null ? type.getKmInterval() : 20000;
                dueMileage = computeDueMileage(baseMileage, interval);
            }
            case "TYRE_CHANGE_SUMMER" -> {
            	LocalDate summer = LocalDate.of(referenceDate.getYear(), 4, 15);
                if (!summer.isAfter(referenceDate)) summer = summer.plusYears(1);
                dueDate = summer;
            }
            case "TYRE_CHANGE_WINTER" -> {
            	 LocalDate winter = LocalDate.of(referenceDate.getYear(), 11, 15);
                 if (!winter.isAfter(referenceDate)) winter = winter.plusYears(1);
                dueDate = winter;
            }
            default -> {
            	if (type.getMonthsInterval() != null) {
                    dueDate = referenceDate.plusMonths(type.getMonthsInterval());
                }
                dueMileage = computeDueMileage(baseMileage, type.getKmInterval());
            }
        }

        return new DueComputation(dueDate, dueMileage);
    }


    private LocalDate resolveReferenceDate(Vehicle vehicle) {
        if (vehicle.getContractStartDate() != null) {
            return vehicle.getContractStartDate();
        }
        if (vehicle.getRegistrationDate() != null) {
            return vehicle.getRegistrationDate();       
         }
        return LocalDate.now();
    }
    
    private Integer computeDueMileage(Integer baseMileage, Integer interval) {
        if (interval == null) return null;
        return baseMileage != null ? baseMileage + interval : interval;
    }

    private record DueComputation(LocalDate dueDate, Integer dueMileage) {}

    public List<VehicleTask> findByVehicle(Long vehicleId) {
    	List<VehicleTask> list = taskRepo.findByVehicleIdAndStatus(vehicleId, TaskStatus.OPEN);
        if (list.isEmpty()) {
            Vehicle v = vehicleRepo.findById(vehicleId).orElse(null);
            if (v != null) {
                ensureTasksExist(v);
                list = taskRepo.findByVehicleIdAndStatus(vehicleId, TaskStatus.OPEN);
            }
        }
        return list;
    }

    public VehicleTask create(Long vehicleId, Long typeId, LocalDate dueDate, Integer dueMileage) {
        Vehicle v = vehicleRepo.findById(vehicleId).orElseThrow();
        TaskType type = taskTypeService.findById(typeId);
        VehicleTask task = VehicleTask.builder()
                .vehicle(v)
                .type(type)
                .dueDate(dueDate)
                .dueMileage(dueMileage)
                .status(TaskStatus.OPEN)
                .build();
        return taskRepo.save(task);
    }

    public VehicleTask update(Long id, Long typeId, LocalDate dueDate, Integer dueMileage) {
        VehicleTask task = taskRepo.findById(id).orElseThrow();
        TaskType type = taskTypeService.findById(typeId);
        task.setType(type);
        task.setDueDate(dueDate);
        task.setDueMileage(dueMileage);
        return task;
    }

    public void delete(Long id) {
        taskRepo.deleteById(id);
    }

    @Transactional(readOnly = true)
    public VehicleTask findById(Long id) {
        return taskRepo.findById(id).orElse(null);
    }
    
    public void updateAutoTasks(Long vehicleId, java.util.List<Long> typeIds) {
        Vehicle vehicle = vehicleRepo.findById(vehicleId).orElseThrow();
        java.util.Set<Long> enabled = new java.util.HashSet<>(typeIds);
        for (TaskType tt : taskTypeService.findAuto()) {
            VehicleTask existing = taskRepo.findByVehicleIdAndTypeAndStatus(vehicleId, tt, TaskStatus.OPEN);
            if (enabled.contains(tt.getId())) {
                if (existing == null) {
                    taskRepo.save(buildTask(vehicle, tt));
                }
            } else {
                if (existing != null) {
                    taskRepo.delete(existing);
                }
            }
        }
    }

    public void updateAfterMaintenance(Maintenance m) {
    	
    	if (m.getType() == null) return;
        VehicleTask task = taskRepo.findByVehicleIdAndTypeAndStatus(
                m.getVehicle().getId(), m.getType(), TaskStatus.OPEN);
        if (task == null || !task.getType().isAuto()) return;
        
        LocalDate nextDate = null;
        Integer nextKm = null;
        
        switch (m.getType().getCode()) {
        case "ORDINARY_SERVICE" -> {
            Integer months = task.getType().getMonthsInterval();
            nextDate = m.getDate() != null && months != null
                    ? m.getDate().plusMonths(months) : null;
            Integer kmInt = task.getType().getKmInterval();
            nextKm = m.getMileage() != null && kmInt != null
                    ? m.getMileage() + kmInt : null;
        }
        case "TYRE_CHANGE_SUMMER" -> {
        	LocalDate d = m.getDate() != null ? m.getDate() : LocalDate.now();
            LocalDate next = LocalDate.of(d.plusYears(1).getYear(), 4, 15);
            nextDate = next;
        }
        case "TYRE_CHANGE_WINTER" -> {
        	 LocalDate d = m.getDate() != null ? m.getDate() : LocalDate.now();
             LocalDate next = LocalDate.of(d.plusYears(1).getYear(), 11, 15);
            nextDate = next;
        }
        case "REVISION" -> {
        	LocalDate d = m.getDate() != null ? m.getDate() : LocalDate.now();
        	 Integer months = task.getType().getMonthsInterval();
             nextDate = months != null ? d.plusMonths(months) : null;
		     }
		     default -> {
            Integer months = task.getType().getMonthsInterval();
            nextDate = m.getDate() != null && months != null
                    ? m.getDate().plusMonths(months) : null;
            Integer kmInt = task.getType().getKmInterval();
            nextKm = m.getMileage() != null && kmInt != null
                    ? m.getMileage() + kmInt : null;
        }
        }
        task.setStatus(TaskStatus.CLOSED);
        task.setExecuted(true);
        taskRepo.save(VehicleTask.builder()
                .vehicle(task.getVehicle())
                .type(task.getType())
                .dueDate(nextDate)
                .dueMileage(nextKm)
                .status(TaskStatus.OPEN)
                .executed(false)
                .build());
    }
}