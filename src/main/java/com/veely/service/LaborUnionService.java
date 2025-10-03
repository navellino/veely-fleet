package com.veely.service;

import com.veely.entity.LaborUnion;
import com.veely.repository.LaborUnionRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class LaborUnionService {

    private final LaborUnionRepository repository;

    public LaborUnion save(LaborUnion union) {
        return repository.save(union);
    }

    @Transactional(readOnly = true)
    public List<LaborUnion> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public LaborUnion findById(Long id) {
        return repository.findById(id).orElse(null);
    }

    public LaborUnion update(Long id, LaborUnion payload) {
        LaborUnion existing = repository.findById(id).orElseThrow();
        existing.setName(payload.getName());
        return repository.save(existing);
    }

    public void delete(Long id) {
        repository.deleteById(id);
    }
}
