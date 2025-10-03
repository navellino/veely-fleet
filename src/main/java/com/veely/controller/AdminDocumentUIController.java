package com.veely.controller;

import com.veely.dto.document.DocumentStatistics;
import com.veely.entity.AdminDocument;
import com.veely.service.AdminDocumentService;
import com.veely.service.AdminDocumentTypeService;
import com.veely.service.DocumentService;
import com.veely.service.PublicAuthorityService;
import com.veely.service.EmployeeService;
import com.veely.validation.FileValidator;
import com.veely.model.DocumentType;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Controller
@RequestMapping("/admin/administrative-documents")
@RequiredArgsConstructor
public class AdminDocumentUIController {

	private final AdminDocumentService adminDocumentService;
    private final AdminDocumentTypeService typeService;
    private final PublicAuthorityService authorityService;
    private final EmployeeService employeeService;
    private final DocumentService documentService;
    private final FileValidator fileValidator;
    


    @GetMapping
    public String list(Model model) {
        List<AdminDocument> documents = adminDocumentService.findAll();
        DocumentStatistics stats = adminDocumentService.calculateStatistics();
        
        model.addAttribute("documents", documents);
        model.addAttribute("documentStats", stats);
        
        return "admin/documents/index";
    }

    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("document", new AdminDocument());
        addReferenceData(model);
        return "admin/documents/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("document") AdminDocument document,
                         BindingResult binding, Model model) {
        if (binding.hasErrors()) {
            addReferenceData(model);
            return "admin/documents/form";
        }
        AdminDocument saved = adminDocumentService.save(document);
        return "redirect:/admin/administrative-documents/" + saved.getId() + "/edit";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
    	model.addAttribute("document", adminDocumentService.findByIdOrThrow(id));
        model.addAttribute("documents", documentService.getAdminDocumentDocuments(id));
        addReferenceData(model);
        return "admin/documents/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("document") AdminDocument document,
                         BindingResult binding, Model model) {
        if (binding.hasErrors()) {
            addReferenceData(model);
            return "admin/documents/form";
        }
        document.setId(id);
        adminDocumentService.save(document);
        return "redirect:/admin/administrative-documents/" + id + "/edit";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
    	adminDocumentService.delete(id);
        return "redirect:/admin/administrative-documents";
    }
    
    @PostMapping("/{id}/docs")
    public String uploadFile(@PathVariable Long id,
    		@RequestParam("file") MultipartFile file,
            @RequestParam("type") DocumentType type,
            @RequestParam(value = "issueDate", required = false) String issueDate,
            @RequestParam(value = "expiryDate", required = false) String expiryDate,
            RedirectAttributes redirectAttributes) {
				try {
				fileValidator.validateDocument(file);
				LocalDate issued = (issueDate == null || issueDate.isBlank()) ? null : LocalDate.parse(issueDate);
				LocalDate exp = (expiryDate == null || expiryDate.isBlank()) ? null : LocalDate.parse(expiryDate);
				documentService.uploadAdminDocumentDocument(id, file, type, issued, exp);
				redirectAttributes.addFlashAttribute("successMessage", "Documento caricato con successo");
				} catch (FileValidator.ValidationException e) {
				redirectAttributes.addFlashAttribute("errorMessage", "Errore: " + e.getMessage());
				} catch (IOException e) {
				redirectAttributes.addFlashAttribute("errorMessage", "Errore durante il caricamento del file");
				}
        return "redirect:/admin/administrative-documents/" + id + "/edit";
    }

    @GetMapping("/{id}/docs/{filename:.+}")
    public ResponseEntity<Resource> download(@PathVariable Long id,
                                             @PathVariable String filename,
                                             HttpServletRequest request) throws IOException {
        Resource resource = documentService.loadAdminDocumentDocumentAsResource(id, filename);
        String contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/{id}/docs/{docId}/delete")
    public String deleteFile(@PathVariable Long id,
                             @PathVariable Long docId) throws IOException {
        documentService.deleteDocument(docId);
        return "redirect:/admin/administrative-documents/" + id + "/edit";
    }

    private void addReferenceData(Model model) {
        model.addAttribute("types", typeService.findAll());
        model.addAttribute("authorities", authorityService.findAll());
        model.addAttribute("employees", employeeService.findAll());
        model.addAttribute("docTypes", DocumentType.values());
    }
}
