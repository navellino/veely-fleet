package com.veely.controller;

import com.veely.entity.Insurance;
import com.veely.entity.Project;
import com.veely.entity.Supplier;
import com.veely.entity.SupplierReferent;
import com.veely.exception.ResourceNotFoundException;
import com.veely.model.DocumentType;
import com.veely.service.DocumentService;
import com.veely.service.InsuranceService;
import com.veely.service.ProjectService;
import com.veely.service.SupplierService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import java.io.IOException;
import java.time.LocalDate;
import java.nio.file.Path;
import java.util.Collections;
import java.util.List;

@Controller
@RequestMapping("/settings/policies")
@RequiredArgsConstructor
public class InsuranceController {

    private final InsuranceService insuranceService;
    private final SupplierService supplierService;
    private final ProjectService projectService;
    private final DocumentService documentService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("policies", insuranceService.findAll());
        return "settings/policies/index";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("policy", buildEmptyPolicy());
        addOptions(model);
        model.addAttribute("documents", Collections.emptyList());
        return "settings/policies/form";
    }

    @PostMapping
    public String create(@Valid @ModelAttribute("policy") Insurance policy,
                         BindingResult binding,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            addOptions(model);
            model.addAttribute("documents", Collections.emptyList());
            return "settings/policies/form";
        }
        try {
            Insurance saved = insuranceService.create(policy);
            redirectAttributes.addFlashAttribute("success", "Polizza assicurativa creata correttamente");
            return "redirect:/settings/policies/" + saved.getId() + "/edit";
        } catch (ResourceNotFoundException ex) {
            applyAssociationError(binding, ex);
            addOptions(model);
            model.addAttribute("documents", Collections.emptyList());
            return "settings/policies/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Insurance policy = insuranceService.findByIdOrThrow(id);
        model.addAttribute("policy", policy);
        addOptions(model);
        model.addAttribute("documents", documentService.getInsuranceDocuments(id));
        return "settings/policies/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("policy") Insurance policy,
                         BindingResult binding,
                         Model model,
                         RedirectAttributes redirectAttributes) {
        if (binding.hasErrors()) {
            addOptions(model);
            model.addAttribute("documents", documentService.getInsuranceDocuments(id));
            return "settings/policies/form";
        }
        try {
            insuranceService.update(id, policy);
            redirectAttributes.addFlashAttribute("success", "Polizza aggiornata correttamente");
            return "redirect:/settings/policies/" + id + "/edit";
        } catch (ResourceNotFoundException ex) {
            applyAssociationError(binding, ex);
            addOptions(model);
            model.addAttribute("documents", documentService.getInsuranceDocuments(id));
            return "settings/policies/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        insuranceService.delete(id);
        redirectAttributes.addFlashAttribute("success", "Polizza eliminata correttamente");
        return "redirect:/settings/policies";
    }

    @PostMapping("/{id}/docs")
    public String uploadDoc(@PathVariable Long id,
                            @RequestParam("file") MultipartFile file,
                            @RequestParam("type") DocumentType type,
                            @RequestParam(value = "issueDate", required = false) LocalDate issueDate,
                            @RequestParam(value = "expiryDate", required = false) LocalDate expiryDate,
                            RedirectAttributes redirectAttributes) throws IOException {
        documentService.uploadInsuranceDocument(id, file, type, issueDate, expiryDate);
        redirectAttributes.addFlashAttribute("success", "Documento caricato con successo");
        return "redirect:/settings/policies/" + id + "/edit";
    }

    @GetMapping("/{policyId}/docs/{docId}/download")
    public ResponseEntity<Resource> downloadDoc(@PathVariable Long policyId,
                                                @PathVariable Long docId) {
        Resource resource = documentService.loadDocument(docId);
        String filename = Path.of(resource.getFilename() != null ? resource.getFilename() : "documento").getFileName().toString();
        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + filename + "\"")
                .body(resource);
    }

    @PostMapping("/{policyId}/docs/{docId}/delete")
    public String deleteDoc(@PathVariable Long policyId,
                            @PathVariable Long docId,
                            RedirectAttributes redirectAttributes) throws IOException {
        documentService.deleteDocument(docId);
        redirectAttributes.addFlashAttribute("success", "Documento eliminato");
        return "redirect:/settings/policies/" + policyId + "/edit";
    }

    private Insurance buildEmptyPolicy() {
        Insurance insurance = new Insurance();
        insurance.setProject(new Project());
        insurance.setSupplier(new Supplier());
        insurance.setSupplierReferent(new SupplierReferent());
        return insurance;
    }

    private void addOptions(Model model) {
        model.addAttribute("suppliers", supplierService.findAllForSelection());
        model.addAttribute("projects", projectService.findAll());
        model.addAttribute("documentTypes", insuranceDocumentTypes());
    }

    private void applyAssociationError(BindingResult binding, ResourceNotFoundException ex) {
        String message = ex.getMessage() != null ? ex.getMessage() : "Associazione non valida";
        if (message.contains("Commessa")) {
            binding.rejectValue("project.id", "policy.project.invalid", message);
        } else if (message.contains("Fornitore")) {
            binding.rejectValue("supplier.id", "policy.supplier.invalid", message);
        } else if (message.contains("Referente")) {
            binding.rejectValue("supplierReferent.id", "policy.referent.invalid", message);
        } else {
            binding.reject("policy.association", message);
        }
    }

    private List<DocumentType> insuranceDocumentTypes() {
        return List.of(
                DocumentType.INSURANCE,
                DocumentType.CERTIFICATE,
                DocumentType.CORRESPONDENCE,
                DocumentType.OTHER);
    }
}
