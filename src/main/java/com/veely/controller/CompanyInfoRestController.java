package com.veely.controller;

import com.veely.entity.CompanyInfo;
import com.veely.service.CompanyInfoService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/company-info")
@RequiredArgsConstructor
public class CompanyInfoRestController {

    private final CompanyInfoService companyInfoService;

    @GetMapping
    public List<CompanyInfo> list() {
        return companyInfoService.findAll();
    }

    @GetMapping("/primary")
    public ResponseEntity<CompanyInfo> getPrimary() {
        return companyInfoService.getPrimaryCompanyInfoOptional()
                .map(ResponseEntity::ok)
                .orElse(ResponseEntity.notFound().build());
    }

    @GetMapping("/{id}")
    public CompanyInfo getById(@PathVariable Long id) {
        return companyInfoService.findById(id);
    }

    @PostMapping
    public CompanyInfo create(@RequestBody CompanyInfo companyInfo) {
        return companyInfoService.create(companyInfo);
    }

    @PutMapping("/{id}")
    public CompanyInfo update(@PathVariable Long id, @RequestBody CompanyInfo companyInfo) {
        return companyInfoService.update(id, companyInfo);
    }

    @DeleteMapping("/{id}")
    public void delete(@PathVariable Long id) {
        companyInfoService.delete(id);
    }
}

