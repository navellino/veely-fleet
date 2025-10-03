package com.veely.controller;

import com.veely.entity.Contract;
import com.veely.model.*;
import com.veely.service.ContractService;
import com.veely.service.DocumentService;
import com.veely.service.ProjectService;
import com.veely.service.SupplierService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;

@Controller
@RequestMapping("/settings/contracts")
@RequiredArgsConstructor
public class ContractController {

    private final ContractService contractService;
    private final SupplierService supplierService;
    private final ProjectService projectService;
    private final DocumentService documentService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("contracts", contractService.findAll());
        model.addAttribute("contractStats", contractService.getStats());
        return "settings/contracts/index";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("contract", new Contract());
        addOptions(model);
        model.addAttribute("documents", java.util.Collections.emptyList());
        
        return "settings/contracts/form";
    }
/*
    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("contract") Contract contract,
                         BindingResult binding, Model model) {
        if (binding.hasErrors()) {
            addOptions(model);
            model.addAttribute("documents", java.util.Collections.emptyList());
            return "settings/contracts/form";
        }
        Contract saved = contractService.create(contract);
        return "redirect:/settings/contracts/" + saved.getId() + "/edit";
    }
*/
    
    @PostMapping("/create")
    public String create(@Valid @ModelAttribute("contract") Contract contract,
                         BindingResult binding, Model model, HttpServletRequest request) {
        
        System.out.println("=== DEBUG CREATE ===");
        System.out.println("Request URI: " + request.getRequestURI());
        System.out.println("Request Method: " + request.getMethod());
        System.out.println("Contract ID: " + contract.getId());
        System.out.println("Has binding errors: " + binding.hasErrors());
        
        if (binding.hasErrors()) {
            binding.getAllErrors().forEach(error -> 
                System.out.println("Validation error: " + error.getDefaultMessage())
            );
            addOptions(model);
            model.addAttribute("documents", java.util.Collections.emptyList());
            return "settings/contracts/form";
        }
        
        Contract saved = contractService.create(contract);
        System.out.println("Saved contract ID: " + saved.getId());
        String redirectUrl = "redirect:/settings/contracts/" + saved.getId() + "/edit";
        System.out.println("Redirect URL: " + redirectUrl);
        
        return redirectUrl;
    }
    
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Contract contract = contractService.findByIdOrThrow(id);
        model.addAttribute("contract", contract);
        addOptions(model);
        model.addAttribute("documents", documentService.getContractDocuments(id));
        return "settings/contracts/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("contract") Contract contract,
                         BindingResult binding, Model model) {
        if (binding.hasErrors()) {
            addOptions(model);
            model.addAttribute("documents", documentService.getContractDocuments(id));
            return "settings/contracts/form";
        }
        contractService.update(id, contract);
        return "redirect:/settings/contracts/" + id + "/edit";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        contractService.delete(id);
        return "redirect:/settings/contracts";
    }

    @PostMapping("/{id}/docs")
    public String uploadDoc(@PathVariable Long id,
                            @RequestParam("file") MultipartFile file,
                            @RequestParam("documentType") DocumentType documentType,
                            @RequestParam(value = "startDate", required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
                            @RequestParam(value = "endDate", required = false)
                            @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
                            RedirectAttributes ra) throws IOException {
    	documentService.uploadContractDocument(id, file, documentType, startDate, endDate);
        ra.addFlashAttribute("success", "Documento caricato");
        return "redirect:/settings/contracts/" + id + "/edit";
    }

    @GetMapping("/{id}/docs/{filename:.+}")
    public ResponseEntity<Resource> downloadDoc(@PathVariable Long id,
                                                @PathVariable String filename) {
        Resource res = documentService.loadContractDocumentAsResource(id, filename);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{contractId}/docs/{docId}/delete")
    public String deleteDoc(@PathVariable Long contractId, @PathVariable Long docId) throws IOException {
        documentService.deleteDocument(docId);
        return "redirect:/settings/contracts/" + contractId + "/edit";
    }

    private void addOptions(Model model) {
        model.addAttribute("suppliers", supplierService.findAll());
        model.addAttribute("projects", projectService.findAll());
        model.addAttribute("types", SupplierContractType.values());
        model.addAttribute("statuses", SupplierContractStatus.values());
        model.addAttribute("frequencies", RecurringFrequency.values());
        model.addAttribute("currencies", CurrencyCode.values());
        model.addAttribute("documentTypes", java.util.List.of(
                DocumentType.SUPPLIER_CONTRACT,
                DocumentType.INVOICE,
                DocumentType.CERTIFICATE,
                DocumentType.CORRESPONDENCE,
                DocumentType.OTHER));
    }
}