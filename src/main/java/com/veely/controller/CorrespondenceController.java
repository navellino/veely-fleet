package com.veely.controller;

import com.veely.entity.Correspondence;
import com.veely.model.CorrespondenceType;
import com.veely.model.DocumentType;
import com.veely.service.CorrespondenceService;
import com.veely.service.DocumentService;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.bind.annotation.PathVariable;

import org.springframework.http.ResponseEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;

import java.time.LocalDate;

@Controller
@RequestMapping("/correspondence")
@RequiredArgsConstructor
public class CorrespondenceController {
    private final CorrespondenceService service;
    private final DocumentService documentService;
    
    private static final DocumentType[] CORR_DOC_TYPES = {
            DocumentType.OTHER
        };

    @GetMapping
    public String index(@RequestParam(value = "year", required = false) Integer year,
			    		@RequestParam(value = "keyword", required = false) String keyword,
			            @RequestParam(value = "tab", required = false) String tab,
			            @RequestParam(value = "progressivo", required = false) String progressivo,
			            Model model) {
			int y = (year == null) ? LocalDate.now().getYear() : year;
			model.addAttribute("incoming", service.searchByType(y, CorrespondenceType.E, keyword));
                        model.addAttribute("outgoing", service.searchByType(y, CorrespondenceType.U, keyword));
                        model.addAttribute("types", CorrespondenceType.values());
                        model.addAttribute("progressivo", progressivo);
                        model.addAttribute("year", y);
                        model.addAttribute("keyword", keyword);
                        model.addAttribute("tab", tab);
                        model.addAttribute("years", service.getYears());
        return "correspondence/index";
    }

    @PostMapping
    public String create(@RequestParam("tipo") CorrespondenceType tipo,
                         @RequestParam("descrizione") String descrizione,
                         @RequestParam(value = "progressivo", required = false) Integer progressivo,
                         @RequestParam(value = "data", required = false) String data,
                         @RequestParam("sender") String sender,
                         @RequestParam(value = "recipient", required = false) String recipient,
                         @RequestParam(value = "notes", required = false) String notes,
                         @RequestParam(value = "files", required = false) MultipartFile[] files) throws java.io.IOException {
        LocalDate d = (data == null || data.isBlank()) ? LocalDate.now() : LocalDate.parse(data);
        int progress = progressivo == null ? 0 : progressivo;
        Correspondence saved = service.register(tipo, progress, descrizione, d, sender, recipient, notes);
        if (files != null) {
            for (MultipartFile f : files) {
                if (f != null && !f.isEmpty()) {
                    documentService.uploadCorrespondenceDocument(saved.getId(), f, DocumentType.OTHER, null, null);
                }
            }
        }
        return "redirect:/correspondence";
    }
    
    @GetMapping("/new")
    public String newForm(Model model) {
	Correspondence c = new Correspondence();
	model.addAttribute("correspondence",c);
        model.addAttribute("types", CorrespondenceType.values());
        return "correspondence/form";
    }
    
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Correspondence c = service.findByIdOrThrow(id);
        model.addAttribute("correspondence", c);
        model.addAttribute("types", CorrespondenceType.values());
        model.addAttribute("documents", documentService.getCorrespondenceDocuments(id));
        //model.addAttribute("docTypes", DocumentType.values());
        model.addAttribute("docTypes", CORR_DOC_TYPES);
        return "correspondence/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @RequestParam("tipo") CorrespondenceType tipo,
                         @RequestParam("progressivo") int progressivo,
                         @RequestParam("descrizione") String descrizione,
                         @RequestParam(value = "data", required = false) String data,
                         @RequestParam("sender") String sender,
                         @RequestParam(value = "recipient", required = false) String recipient,
                         @RequestParam(value = "notes", required = false) String notes,
                         @RequestParam(value = "files", required = false) MultipartFile[] files) throws java.io.IOException {
        LocalDate d = (data == null || data.isBlank()) ? LocalDate.now() : LocalDate.parse(data);
        service.update(id, progressivo, tipo, descrizione, d, sender, recipient, notes);
        if (files != null) {
            for (MultipartFile f : files) {
                if (f != null && !f.isEmpty()) {
                    documentService.uploadCorrespondenceDocument(id, f, DocumentType.OTHER, null, null);
                }
            }
        }
        return "redirect:/correspondence";
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) throws java.io.IOException {
    	documentService.getCorrespondenceDocuments(id)
        .forEach(doc -> {
                    try { documentService.deleteDocument(doc.getId()); } catch (java.io.IOException e) { /* ignore */ }
                });
        service.delete(id);
        return "redirect:/correspondence";
    }
    
    @PostMapping("/{id}/docs")
    public String uploadDoc(@PathVariable Long id,
    		@RequestParam("files") MultipartFile[] files,
            @RequestParam("type") DocumentType type,
            @RequestParam(value = "issueDate", required = false) String issueDate,
            @RequestParam(value = "expiryDate", required = false) String expiryDate,
            Model model) throws java.io.IOException {
				if (files != null) {
				LocalDate issue = (issueDate == null || issueDate.isBlank()) ? null : LocalDate.parse(issueDate);
            LocalDate expiry = (expiryDate == null || expiryDate.isBlank()) ? null : LocalDate.parse(expiryDate);
            for (MultipartFile file : files) {
                if (file != null && !file.isEmpty()) {
                    documentService.uploadCorrespondenceDocument(id, file, type, issue, expiry);
                }
            }
        }
        //return "redirect:/correspondence/" + id + "/edit";
        return editForm(id, model);
    }

    @GetMapping("/{id}/docs/{docId}/delete")
    public String deleteDoc(@PathVariable("id") Long id,
                            @PathVariable("docId") Long docId) throws java.io.IOException {
        documentService.deleteDocument(docId);
        return "redirect:/correspondence/" + id + "/edit";
    }
    
    @GetMapping("/docs/{docId}")
    public ResponseEntity<Resource> downloadDoc(@PathVariable Long docId, HttpServletRequest request) throws java.io.IOException {
        Resource res = documentService.loadDocument(docId);
        String contentType = request.getServletContext().getMimeType(res.getFile().getAbsolutePath());
        if (contentType == null) contentType = "application/octet-stream";
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + res.getFilename() + "\"")
                .body(res);
    }
}