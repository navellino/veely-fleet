package com.veely.service;

import com.veely.entity.PublicAuthority;
import com.veely.exception.ResourceNotFoundException;
import com.veely.repository.PublicAuthorityRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Service
@RequiredArgsConstructor
@Transactional
public class PublicAuthorityService {
    private final PublicAuthorityRepository repository;

    @Transactional(readOnly = true)
    public List<PublicAuthority> findAll() {
        return repository.findAll();
    }

    @Transactional(readOnly = true)
    public PublicAuthority findByIdOrThrow(Long id) {
        return repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Ente pubblico non trovato: " + id));
    }

    public PublicAuthority save(PublicAuthority authority) {
        return repository.save(authority);
    }

    public void delete(Long id) {
        PublicAuthority existing = findByIdOrThrow(id);
        repository.delete(existing);
    }
}
