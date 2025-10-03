package com.veely.service;

import com.veely.entity.TaskType;
import com.veely.repository.TaskTypeRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class TaskTypeService {
    private final TaskTypeRepository repository;

    public TaskType findByCode(String code) {
        return repository.findByCode(code);
    }

    public TaskType save(TaskType type) {
        return repository.save(type);
    }

    @Transactional(readOnly = true)
    public List<TaskType> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public List<TaskType> findAuto() {
        return repository.findByAutoTrue();
    }
    
    @Transactional(readOnly = true)
    public TaskType findById(Long id) {
        return repository.findById(id).orElse(null);
    }
    
    public TaskType update(Long id, TaskType payload) {
        TaskType existing = repository.findById(id).orElseThrow();
        existing.setCode(payload.getCode());
        existing.setDescription(payload.getDescription());
        existing.setByDate(payload.isByDate());
        existing.setByMileage(payload.isByMileage());
        existing.setAuto(payload.isAuto());
        existing.setMonthsInterval(payload.getMonthsInterval());
        existing.setKmInterval(payload.getKmInterval());
        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
