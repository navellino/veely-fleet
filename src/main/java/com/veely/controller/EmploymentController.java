package com.veely.controller;

import com.veely.entity.Employment;
import com.veely.service.ProjectService;
import com.veely.model.CcnlType;
import com.veely.model.ContractType;
import com.veely.model.DocumentType;
import com.veely.model.EmploymentStatus;
import com.veely.service.DocumentService;
import com.veely.service.EmploymentService;
import com.veely.service.LaborUnionService;
import com.veely.service.PdfEmploymentService;
import com.veely.service.EmployeeService;
import org.springframework.core.io.Resource;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;

import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.util.UriComponentsBuilder;

import java.io.IOException;
import java.time.LocalDate;

@Controller
@RequestMapping("/fleet/employments")
@RequiredArgsConstructor
public class EmploymentController {

    private final EmploymentService employmentService;
    private final EmployeeService employeeService;
    private final DocumentService documentService;
    private final ProjectService projectService;
    private final LaborUnionService unionService;
    
    private static final DocumentType[] EMPLOYEE_DOC_TYPES = {
            DocumentType.EMPLOYMENT_CONTRACT,
            DocumentType.PAY_RISE,
            DocumentType.EXTENTION,
            DocumentType.UNILAV,
            DocumentType.TRANSFER_ORDER,
            DocumentType.DISCIPLINARY_LETTER,
            DocumentType.OTHER
        };
    
    /*
     * 
     *     	EMPLOYMENT_CONTRACT("Contratto di lavoro"),
		    PAY_RISE("Aumento retribuzione"),
		    EXTENTION("Contratto di Proroga"),
		    UNILAV("Comunicazione Obbligatoria"),
		    TRANSFER_ORDER("Lettera di Trasferimento"),
		    DISCIPLINARY_LETTER("Lettera di richiamo"),
     * 
     */
    
    @GetMapping
    public String list(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) EmploymentStatus status,
            @RequestParam(required = false, name = "project") Long projectId,
            @RequestParam(defaultValue = "0") int page, Model model) {
    	Page<Employment> emps = employmentService.search(keyword, status, projectId, PageRequest.of(page, 20));
         model.addAttribute("employments", emps);
         model.addAttribute("statuses", EmploymentStatus.values());
         model.addAttribute("activeCount", employmentService.countByStatus(EmploymentStatus.ACTIVE));
         model.addAttribute("projects", projectService.findActive());
         model.addAttribute("expiredCount", employmentService.countByStatus(EmploymentStatus.TERMINATED));
         return "fleet/employments/index";
    }
    
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportPdf(@RequestParam(required = false) String keyword,
            @RequestParam(required = false) EmploymentStatus status,
            @RequestParam(required = false, name = "project") Long projectId) throws IOException {
    	byte[] pdf = employmentService.exportStyledPdf(keyword, status, projectId);
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Prospetto_Rapporti_di_lavoro.pdf")
                .body(pdf);
    }
    
    @GetMapping("/new")
    public String newForm(Model model) {
    	Employment employment = new Employment();
        model.addAttribute("employment", employment);
        model.addAttribute("employees", employeeService.findAvailableForEmployment());
        model.addAttribute("docTypes", EMPLOYEE_DOC_TYPES);
        model.addAttribute("ccnls", CcnlType.values());
        model.addAttribute("statuses", EmploymentStatus.values());
        model.addAttribute("contractType", ContractType.values());
        model.addAttribute("projects", projectService.findActive());
        model.addAttribute("unions", unionService.findAll());
        model.addAttribute("returnQuery", "");
        return "fleet/employments/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute Employment employment,
                         BindingResult binding,
                         @RequestParam(value = "file", required = false) MultipartFile file,
                         @RequestParam(value = "type", required = false) DocumentType type,
                         @RequestParam(value = "issueDate", required = false) String issueDate,
                         @RequestParam(value = "expiryDate", required = false) String expiryDate,
                         Model model) throws IOException {

        if (binding.hasErrors()) {
            model.addAttribute("employees", employeeService.findAvailableForEmployment());
            model.addAttribute("docTypes", DocumentType.values());
            model.addAttribute("ccnls", CcnlType.values());
            model.addAttribute("statuses", EmploymentStatus.values());
            model.addAttribute("contractType", ContractType.values());
            model.addAttribute("projects", projectService.findActive());
            model.addAttribute("unions", unionService.findAll());
            model.addAttribute("returnQuery", "");
            return "fleet/employments/form";
        }

        // 1) salva il rapporto
        Employment saved = employmentService.create(employment);

        // 2) allega documento contrattuale se presente
        if (file != null && !file.isEmpty()) {
            LocalDate issued = (issueDate == null || issueDate.isBlank())
                ? null : LocalDate.parse(issueDate);
            LocalDate exp = (expiryDate == null || expiryDate.isBlank())
                ? null : LocalDate.parse(expiryDate);

            documentService.uploadEmploymentDocument(
                saved.getId(), file, type, issued, exp
            );
        }
        //return "redirect:/fleet/employments";
        return "redirect:/fleet/employments/" + saved.getId() + "/edit";
    }
    
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id,
            @RequestParam(value = "originPage", required = false) Integer originPage,
            @RequestParam(value = "originKeyword", required = false) String originKeyword,
            @RequestParam(value = "originStatus", required = false) EmploymentStatus originStatus,
            @RequestParam(value = "originProject", required = false) Long originProject,
            @RequestParam(value = "originSort", required = false) String originSort,
            @RequestParam(value = "originDir", required = false) String originDir,
            Model model) {
        Employment e = employmentService.findByIdOrThrow(id);
        model.addAttribute("employment", e);
        model.addAttribute("employees", employeeService.findAll());
        model.addAttribute("docTypes", EMPLOYEE_DOC_TYPES);
        model.addAttribute("documents", documentService.getEmploymentDocuments(id));
        model.addAttribute("ccnls", CcnlType.values());
        model.addAttribute("statuses", EmploymentStatus.values());
        model.addAttribute("contractType", ContractType.values());
        model.addAttribute("projects", projectService.findActive());
        model.addAttribute("unions", unionService.findAll());
        model.addAttribute("returnQuery",
                buildReturnQueryString(originPage, originKeyword, originStatus, originProject, originSort, originDir));
        return "fleet/employments/form";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute Employment employment,
                         BindingResult binding,
                         @RequestParam(value = "originPage", required = false) Integer originPage,
                         @RequestParam(value = "originKeyword", required = false) String originKeyword,
                         @RequestParam(value = "originStatus", required = false) EmploymentStatus originStatus,
                         @RequestParam(value = "originProject", required = false) Long originProject,
                         @RequestParam(value = "originSort", required = false) String originSort,
                         @RequestParam(value = "originDir", required = false) String originDir,
                         Model model) {
        if (binding.hasErrors()) {
            model.addAttribute("employees", employeeService.findAll());
            model.addAttribute("docTypes", DocumentType.values());
            model.addAttribute("documents", documentService.getEmploymentDocuments(id));
            model.addAttribute("ccnls", CcnlType.values());
            model.addAttribute("statuses", EmploymentStatus.values());
            model.addAttribute("contractType", ContractType.values());
            model.addAttribute("projects", projectService.findActive());
            model.addAttribute("returnQuery",
                    buildReturnQueryString(originPage, originKeyword, originStatus, originProject, originSort, originDir));
            return "fleet/employments/form";
        }
        employmentService.update(id, employment);
        return "redirect:/fleet/employments/" + id + "/edit"
        + buildOriginQueryString(originPage, originKeyword, originStatus, originProject, originSort, originDir);
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        employmentService.delete(id);
        return "redirect:/fleet/employments";
    }
    
    @PostMapping("/{id}/docs")
    public String uploadDoc(@PathVariable Long id,
                            @RequestParam("file") MultipartFile file,
                            @RequestParam("type") DocumentType type,
                            @RequestParam(value="issueDate", required=false) String issueDate,
                            @RequestParam(value="expiryDate", required=false) String expiryDate) throws IOException {
        LocalDate issued = (issueDate == null || issueDate.isBlank()) ? null : LocalDate.parse(issueDate);
        LocalDate exp = (expiryDate == null || expiryDate.isBlank()) ? null : LocalDate.parse(expiryDate);
        documentService.uploadEmploymentDocument(id, file, type, issued, exp);
     // Se la richiesta è AJAX, ritorna JSON
        //if ("XMLHttpRequest".equals(request.getHeader("X-Requested-With"))) {
          //  return "redirect:/fleet/employments/" + id + "/edit?tab=docs";
       // }
        
        // Altrimenti redirect normale alla tab documenti
        return "redirect:/fleet/employments/" + id + "/edit?tab=docs";
    }

    @GetMapping("/{id}/docs/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable Long id,
                                              @PathVariable String filename,
                                              HttpServletRequest request) throws IOException {
        Resource resource = documentService.loadEmploymentDocumentAsResource(id, filename);
        String contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/{empId}/docs/{docId}/delete")
    public String deleteDocument(@PathVariable Long empId,
                                 @PathVariable Long docId) throws IOException {
        documentService.deleteDocument(docId);
        return "redirect:/fleet/employments/" + empId + "/edit";
    }
    
    private String buildReturnQueryString(Integer page, String keyword, EmploymentStatus status,
            Long projectId, String sort, String dir) {
				UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
				if (page != null) {
				builder.queryParam("page", page);
				}
				if (keyword != null && !keyword.isBlank()) {
				builder.queryParam("keyword", keyword);
				}
				if (status != null) {
				builder.queryParam("status", status);
				}
				if (projectId != null) {
				builder.queryParam("project", projectId);
				}
				if (sort != null && !sort.isBlank()) {
				builder.queryParam("sort", sort);
				}
				if (dir != null && !dir.isBlank()) {
				builder.queryParam("dir", dir);
				}
				String query = builder.build().encode().toUriString();
				return query != null ? query : "";
				}
				
				private String buildOriginQueryString(Integer page, String keyword, EmploymentStatus status,
				            Long projectId, String sort, String dir) {
				UriComponentsBuilder builder = UriComponentsBuilder.newInstance();
				if (page != null) {
				builder.queryParam("originPage", page);
				}
				if (keyword != null && !keyword.isBlank()) {
				builder.queryParam("originKeyword", keyword);
				}
				if (status != null) {
				builder.queryParam("originStatus", status);
				}
				if (projectId != null) {
				builder.queryParam("originProject", projectId);
				}
				if (sort != null && !sort.isBlank()) {
				builder.queryParam("originSort", sort);
				}
				if (dir != null && !dir.isBlank()) {
				builder.queryParam("originDir", dir);
				}
				String query = builder.build().encode().toUriString();
				return query != null ? query : "";
				}
				    
}
