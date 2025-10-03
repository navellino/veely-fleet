package com.veely.controller;

import com.veely.service.ComplianceItemService;
import com.veely.service.ComplianceCategoryService;
import com.veely.service.ProjectService;
import com.veely.service.EmployeeService;
import com.veely.service.DocumentService;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.http.ResponseEntity;
import org.springframework.http.MediaType;
import org.springframework.http.HttpHeaders;
import org.springframework.core.io.Resource;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDate;
import java.io.IOException;

@Controller
@RequestMapping("/safety")
@RequiredArgsConstructor
public class ComplianceItemController {
    private final ComplianceItemService itemService;
    private final ComplianceCategoryService categoryService;
    private final ProjectService projectService;
    private final EmployeeService employeeService;
    private final DocumentService documentService;

    @GetMapping
    public String list(@RequestParam(required = false) Long categoryId,
                       @RequestParam(required = false) Long projectId,
                       @RequestParam(required = false) Long employeeId,
                       @RequestParam(required = false) String status,
                       @RequestParam(required = false) String from,
                       @RequestParam(required = false) String to,
                       Model model) {
    	 LocalDate fromDate = from != null && !from.isBlank() ? LocalDate.parse(from) : null;
         LocalDate toDate = to != null && !to.isBlank() ? LocalDate.parse(to) : null;
         Boolean expired = null;
         if ("expired".equalsIgnoreCase(status)) expired = true;
         if ("valid".equalsIgnoreCase(status)) expired = false;
         model.addAttribute("items", itemService.search(categoryId, projectId, employeeId, fromDate, toDate, expired));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("projects", projectService.findAll());
        model.addAttribute("employees", employeeService.findAll());
        return "safety/items/index";
    }
    
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("item", new com.veely.entity.ComplianceItem());
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("projects", projectService.findAll());
        model.addAttribute("employees", employeeService.findAll());
        return "safety/items/form";
    }

    @PostMapping("/new")
    public String create(@ModelAttribute("item") com.veely.entity.ComplianceItem item) {
        itemService.create(item);
        return "redirect:/safety";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        model.addAttribute("item", itemService.findByIdOrThrow(id));
        model.addAttribute("categories", categoryService.findAll());
        model.addAttribute("projects", projectService.findAll());
        model.addAttribute("employees", employeeService.findAll());
        model.addAttribute("documents", documentService.getComplianceItemDocuments(id));
        model.addAttribute("docTypes", new com.veely.model.DocumentType[]{
                com.veely.model.DocumentType.CERTIFICATE,
                com.veely.model.DocumentType.MEDICAL,
                com.veely.model.DocumentType.PPE
        });
        return "safety/items/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute("item") com.veely.entity.ComplianceItem item) {
        itemService.update(id, item);
        return "redirect:/safety/" + id + "/edit";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        itemService.delete(id);
        return "redirect:/safety";
    }
    
    @PostMapping("/{id}/docs")
    public String uploadDoc(@PathVariable Long id,
                            @RequestParam("file") MultipartFile file,
                            @RequestParam("type") com.veely.model.DocumentType type,
                            @RequestParam(value = "issueDate", required = false) String issueDate,
                            @RequestParam(value = "expiryDate", required = false) String expiryDate) throws IOException {
        LocalDate issued = (issueDate == null || issueDate.isBlank()) ? null : LocalDate.parse(issueDate);
        LocalDate exp = (expiryDate == null || expiryDate.isBlank()) ? null : LocalDate.parse(expiryDate);
        documentService.uploadComplianceItemDocument(id, file, type, issued, exp);
        return "redirect:/safety/" + id + "/edit";
    }

    @GetMapping("/{id}/docs/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable Long id,
                                              @PathVariable String filename,
                                              HttpServletRequest request) throws IOException {
        Resource resource = documentService.loadComplianceItemDocumentAsResource(id, filename);
        String contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/{itemId}/docs/{docId}/delete")
    public String deleteDocument(@PathVariable Long itemId, @PathVariable Long docId) throws IOException {
        documentService.deleteDocument(docId);
        return "redirect:/safety/" + itemId + "/edit";
    }
}
