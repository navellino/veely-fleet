package com.veely.repository;

import com.veely.entity.TaskType;
import org.springframework.data.jpa.repository.JpaRepository;

public interface TaskTypeRepository extends JpaRepository<TaskType, Long> {
    TaskType findByCode(String code);
    java.util.List<TaskType> findByAutoTrue();
}
