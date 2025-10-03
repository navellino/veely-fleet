package com.veely.controller;

import com.veely.entity.ComplianceCategory;
import com.veely.service.ComplianceCategoryService;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/safety-categories")
@RequiredArgsConstructor
public class ComplianceCategoryRestController {
    private final ComplianceCategoryService service;

    @GetMapping
    public List<ComplianceCategory> list() {
        return service.findAll();
    }

    @PostMapping
    public ComplianceCategory create(@RequestBody ComplianceCategory category) {
        return service.create(category);
    }
}
