package com.veely.repository;

import com.veely.entity.TaskType;
import com.veely.entity.VehicleTask;
import com.veely.model.TaskStatus;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface VehicleTaskRepository extends JpaRepository<VehicleTask, Long> {
    List<VehicleTask> findByVehicleId(Long vehicleId);
    List<VehicleTask> findByVehicleIdAndStatus(Long vehicleId, TaskStatus status);
    VehicleTask findByVehicleIdAndTypeAndStatus(Long vehicleId, TaskType type, TaskStatus status);
    List<VehicleTask> findByStatusOrderByDueDateAsc(TaskStatus status);
}
