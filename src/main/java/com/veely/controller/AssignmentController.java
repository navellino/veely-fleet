package com.veely.controller;

import com.veely.entity.Assignment;
import com.veely.model.AssignmentStatus;
import com.veely.model.DocumentType;
import com.veely.service.AssignmentService;
import com.veely.service.DocumentService;
import com.veely.service.EmploymentService;
import com.veely.service.ProjectService;
import com.veely.service.VehicleService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

/**
 * Gestisce la visualizzazione e la creazione delle assegnazioni veicoli.
 */
@Controller
@RequestMapping("/fleet/assignments")
@RequiredArgsConstructor
public class AssignmentController {

    private final AssignmentService assignmentService;
    private final EmploymentService employmentService;
    private final VehicleService vehicleService;
    private final DocumentService documentService;
    private final ProjectService projectService;
    
    private static final DocumentType[] ASSIGN_DOC_TYPES = {
            DocumentType.ASSIGNMENT_PHOTO,
            DocumentType.ASSIGNMENT_REPORT,
            DocumentType.ASSIGNMENT_LETTER
        };
    /*
     * 
     * ASSIGNMENT_PHOTO("Foto veicolo durante assegnazione"),
    ASSIGNMENT_REPORT("Report riconsegna / danni"),
    ASSIGNMENT_LETTER("Lettera di assegnazione veicolo"),
    model.addAttribute("docTypes", ASSIGN_DOC_TYPES);
     */
    
    /** Pagina con le liste di assegnazioni lunghe e brevi. */
    @GetMapping
    public String list(Model model) {
    	assignmentService.releaseExpiredAssignments();
        List<Assignment> assignments = assignmentService.findAll();
        model.addAttribute("assignments", assignments);
        return "fleet/assignments/index";
    }

    /** Form di inserimento per le assegnazioni lunghe. */
    @GetMapping("/new")
    public String createForm(Model model) {
        model.addAttribute("assignment", new Assignment());
        model.addAttribute("employments", employmentService.findAll());
        model.addAttribute("vehicles", vehicleService.findAvailable());
        model.addAttribute("projects", projectService.findAll());
        model.addAttribute("docTypes", ASSIGN_DOC_TYPES);
        return "fleet/assignments/form";
    }

    /** Salvataggio di un'assegnazione lunga con eventuale documento allegato. */
    @PostMapping("/new")
    public String saveNew(@Valid @ModelAttribute Assignment assignment,
                          @RequestParam(value = "file", required = false) MultipartFile file,
                          @RequestParam(value = "docType", required = false) DocumentType docType,
                          @RequestParam(value = "issueDate", required = false) String issueDate,
                          @RequestParam(value = "expiryDate", required = false) String expiryDate) throws IOException {
        assignment.setStatus(AssignmentStatus.ASSIGNED);
        Assignment saved = assignmentService.create(assignment);

        if (file != null && !file.isEmpty()) {
            LocalDate issued = (issueDate == null || issueDate.isBlank()) ? null : LocalDate.parse(issueDate);
            LocalDate exp = (expiryDate == null || expiryDate.isBlank()) ? null : LocalDate.parse(expiryDate);
            documentService.uploadAssignmentDocument(saved.getId(), file, docType, issued, exp);
        }
        return "redirect:/fleet/assignments";
    }
    
    /** Form di modifica per qualunque assegnazione. */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Assignment a = assignmentService.findByIdOrThrow(id);
        model.addAttribute("assignment", a);
        model.addAttribute("employments", employmentService.findAll());
        model.addAttribute("vehicles", vehicleService.findAll());
        model.addAttribute("projects", projectService.findAll());
        model.addAttribute("docTypes", ASSIGN_DOC_TYPES);
        model.addAttribute("documents", documentService.getAssignmentDocuments(id));
        return "fleet/assignments/form";
    }

    /** Salvataggio modifiche di un'assegnazione. */
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id, @ModelAttribute Assignment assignment) {
        assignmentService.update(id, assignment);
        return "redirect:/fleet/assignments";
    }
    
    /** Upload documento per un'assegnazione esistente */
    @PostMapping("/{id}/docs")
    public String uploadDoc(@PathVariable Long id,
                            @RequestParam("file") MultipartFile file,
                            @RequestParam("type") DocumentType type,
                            @RequestParam(value = "issueDate", required = false) String issueDate,
                            @RequestParam(value = "expiryDate", required = false) String expiryDate) throws IOException {
        LocalDate issued = (issueDate == null || issueDate.isBlank()) ? null : LocalDate.parse(issueDate);
        LocalDate exp = (expiryDate == null || expiryDate.isBlank()) ? null : LocalDate.parse(expiryDate);
        documentService.uploadAssignmentDocument(id, file, type, issued, exp);
        return "redirect:/fleet/assignments/" + id + "/edit";
    }

    @GetMapping("/{id}/docs/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable Long id,
                                              @PathVariable String filename,
                                              HttpServletRequest request) throws IOException {
        Resource resource = documentService.loadAssignmentDocumentAsResource(id, filename);
        String contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/{asgId}/docs/{docId}/delete")
    public String deleteDocument(@PathVariable("asgId") Long asgId,
                                 @PathVariable("docId") Long docId) throws IOException {
        documentService.deleteDocument(docId);
        return "redirect:/fleet/assignments/" + asgId + "/edit";
    }
}
