package com.veely.controller;

import com.veely.entity.Supplier;
import com.veely.service.SupplierService;
import com.veely.model.DocumentType;
import com.veely.service.DocumentService;
import com.veely.entity.SupplierReferent;
import lombok.*;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import java.io.IOException;
import java.time.LocalDate;
import java.util.Arrays;
import java.util.List;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;

@Controller
@RequestMapping("/fleet/suppliers")
@RequiredArgsConstructor
public class SupplierController {

    private final SupplierService supplierService;
    private final DocumentService documentService;

    @GetMapping
    public String list(Model model) {
        model.addAttribute("suppliers", supplierService.findAll());
        return "fleet/suppliers/index";
    }

    @GetMapping("/{id}/referents")
    @ResponseBody
    public List<SupplierReferent> getReferents(@PathVariable Long id) {
        Supplier supplier = supplierService.findByIdOrThrow(id);
        return supplier.getReferents();
    }
    
    @GetMapping("/new")
    public String newForm(Model model) {
        model.addAttribute("supplier", new Supplier());
        return "fleet/suppliers/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute Supplier supplier, BindingResult binding) {
        if (binding.hasErrors()) {
            return "fleet/suppliers/form";
        }
        Supplier saved = supplierService.create(supplier);
        return "redirect:/fleet/suppliers/" + saved.getId() + "/edit";
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Supplier s = supplierService.findByIdOrThrow(id);
        model.addAttribute("supplier", s);
        model.addAttribute("documents", documentService.getSupplierDocuments(id));
        model.addAttribute("docTypes", Arrays.asList(DocumentType.SUPPLIER_CONTRACT, DocumentType.OTHER));
        return "fleet/suppliers/form";
    }

    /** Upload documento fornitore */
    @PostMapping("/{id}/docs")
    public String uploadDoc(@PathVariable Long id,
                            @RequestParam("file") MultipartFile file,
                            @RequestParam("type") DocumentType type,
                            @RequestParam(value = "issueDate", required = false) String issueDate,
                            @RequestParam(value = "expiryDate", required = false) String expiryDate) throws IOException {
        LocalDate issued = (issueDate == null || issueDate.isBlank()) ? null : LocalDate.parse(issueDate);
        LocalDate exp = (expiryDate == null || expiryDate.isBlank()) ? null : LocalDate.parse(expiryDate);
        documentService.uploadSupplierDocument(id, file, type, issued, exp);
        return "redirect:/fleet/suppliers/" + id + "/edit";
    }

    /** Download documento fornitore */
    @GetMapping("/{id}/docs/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable Long id,
                                              @PathVariable String filename,
                                              HttpServletRequest request) throws IOException {
        Resource resource = documentService.loadSupplierDocumentAsResource(id, filename);
        String contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    /** Delete documento fornitore */
    @GetMapping("/{sId}/docs/{docId}/delete")
    public String deleteDocument(@PathVariable("sId") Long supplierId,
                                 @PathVariable("docId") Long docId) throws IOException {
        documentService.deleteDocument(docId);
        return "redirect:/fleet/suppliers/" + supplierId + "/edit";
    }
    
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @Valid @ModelAttribute Supplier supplier,
                         BindingResult binding) {
        if (binding.hasErrors()) {
            return "fleet/suppliers/form";
        }
        supplierService.update(id, supplier);
        return "redirect:/fleet/suppliers/" + id + "/edit";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        supplierService.delete(id);
        return "redirect:/fleet/suppliers";
    }
}
