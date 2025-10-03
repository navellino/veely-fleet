package com.veely.controller;

import com.veely.entity.AdminDocument;
import com.veely.service.AdminDocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/admin/documents")
@RequiredArgsConstructor
public class AdminDocumentController {
    private final AdminDocumentService service;

    @GetMapping
    public List<AdminDocument> list() {
        return service.findAll();
    }

    @GetMapping("/{id}")
    public AdminDocument get(@PathVariable Long id) {
        return service.findByIdOrThrow(id);
    }

    @PostMapping
    public AdminDocument create(@RequestBody AdminDocument document) {
        return service.save(document);
    }

    @PutMapping("/{id}")
    public AdminDocument update(@PathVariable Long id, @RequestBody AdminDocument document) {
        document.setId(id);
        return service.save(document);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        service.delete(id);
    }
}