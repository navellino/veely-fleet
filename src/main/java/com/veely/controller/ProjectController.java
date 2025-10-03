package com.veely.controller;

import com.veely.entity.Project;
import com.veely.model.DocumentType;
import com.veely.model.ProjectStatus;
import com.veely.service.DocumentService;
import com.veely.service.EmployeeService;
import com.veely.service.ProjectService;
import com.veely.service.SupplierService;
import com.veely.service.InsuranceService;

import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import java.util.Map;

import org.springframework.core.io.Resource;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

@Controller
@RequestMapping("/settings/projects")
@RequiredArgsConstructor
public class ProjectController {

    private final ProjectService projectService;
    private final EmployeeService employeeService;
    private final DocumentService documentService;
    private final SupplierService supplierService;
    private final InsuranceService insuranceService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("projects", projectService.findAll());
        return "settings/projects/index";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("project", new Project());
        model.addAttribute("employees", employeeService.findAll());
        model.addAttribute("statuses", ProjectStatus.values());
        model.addAttribute("documents", java.util.Collections.emptyList());
        model.addAttribute("policies", java.util.Collections.emptyList());
        model.addAttribute("docTypes", DocumentType.values());
        model.addAttribute("suppliers", supplierService.findAllForSelection());
        return "settings/projects/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("project") Project project, BindingResult binding, Model model) {
        if (binding.hasErrors()) {
        	model.addAttribute("employees", employeeService.findAll());
            model.addAttribute("statuses", ProjectStatus.values());
            model.addAttribute("documents", java.util.Collections.emptyList());
            model.addAttribute("policies", java.util.Collections.emptyList());
            model.addAttribute("docTypes", DocumentType.values());
            model.addAttribute("suppliers", supplierService.findAllForSelection());
            return "settings/projects/form";
        }
        Project saved = projectService.create(project);
        return "redirect:/settings/projects/" + saved.getId() + "/edit";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
    	 Project project = projectService.findByIdOrThrow(id);
         model.addAttribute("project", project);
         model.addAttribute("employees", employeeService.findAll());
         model.addAttribute("statuses", ProjectStatus.values());
         model.addAttribute("documents", documentService.getProjectDocuments(id));
         model.addAttribute("policies", insuranceService.findByProjectId(id));
         model.addAttribute("docTypes", DocumentType.values());
         model.addAttribute("suppliers", supplierService.findAllForSelection());
        return "settings/projects/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute("project") Project project, BindingResult binding, Model model) {
        if (binding.hasErrors()) {
        	model.addAttribute("employees", employeeService.findAll());
            model.addAttribute("statuses", ProjectStatus.values());
            model.addAttribute("documents", documentService.getProjectDocuments(id));
            model.addAttribute("policies", insuranceService.findByProjectId(id));
            model.addAttribute("docTypes", DocumentType.values());
            model.addAttribute("suppliers", supplierService.findAllForSelection());
            return "settings/projects/form";
        }
        projectService.update(id, project);
        return "redirect:/settings/projects/" + id + "/edit";
    }
    
    @PostMapping("/{id}/docs")
    public String uploadDoc(@PathVariable Long id,
                            @RequestParam("file") MultipartFile file,
                            @RequestParam("type") DocumentType type,
                            @RequestParam(value = "issueDate", required = false) String issueDate,
                            @RequestParam(value = "expiryDate", required = false) String expiryDate,
                            RedirectAttributes ra) throws java.io.IOException {
        java.time.LocalDate issued = (issueDate == null || issueDate.isBlank()) ? null : java.time.LocalDate.parse(issueDate);
        java.time.LocalDate exp = (expiryDate == null || expiryDate.isBlank()) ? null : java.time.LocalDate.parse(expiryDate);
        documentService.uploadProjectDocument(id, file, type, issued, exp);
        ra.addFlashAttribute("success", "Documento caricato");
        return "redirect:/settings/projects/" + id + "/edit";
    }

    @GetMapping("/{id}/docs/{filename:.+}")
    public ResponseEntity<Resource> downloadDoc(@PathVariable Long id, @PathVariable String filename) {
        Resource res = documentService.loadProjectDocumentAsResource(id, filename);
        return ResponseEntity.ok(res);
    }

    @GetMapping("/{projId}/docs/{docId}/delete")
    public String deleteDoc(@PathVariable Long projId, @PathVariable Long docId) throws java.io.IOException {
        documentService.deleteDocument(docId);
        return "redirect:/settings/projects/" + projId + "/edit";
    }
        
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        projectService.delete(id);
        return "redirect:/settings/projects";
    }
}
