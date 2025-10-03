package com.veely.controller;

import com.veely.entity.Maintenance;
import com.veely.entity.VehicleTask;
import com.veely.model.DocumentType;
import com.veely.repository.MaintenanceRepository;
import com.veely.service.DocumentService;
import com.veely.service.MaintenanceService;
import com.veely.service.SupplierService;
import com.veely.service.TaskTypeService;
import com.veely.service.VehicleService;
import com.veely.service.VehicleTaskService;

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

import jakarta.validation.Valid;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Collectors;
import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDate;

@Controller
@RequestMapping("/fleet/maintenance")
@RequiredArgsConstructor
public class MaintenanceController {

    private final MaintenanceService maintenanceService;
    private final VehicleService vehicleService;
    private final SupplierService supplierService;
    private final DocumentService documentService;
    private final VehicleTaskService vehicleTaskService;
    private final TaskTypeService taskTypeService;
    private final MaintenanceRepository maintenanceRepository;


    @GetMapping
    public String list(@RequestParam(value = "plate", required = false) String plate,
            @RequestParam(value = "year", required = false) Integer year,
            @RequestParam(value = "typeId", required = false) Long typeId,
                       Model model) {
    	List<Maintenance> list = maintenanceService.search(plate, year, typeId);
        model.addAttribute("maintenances", list);
        model.addAttribute("maintenance", new Maintenance());
        model.addAttribute("totalMaintenances", maintenanceService.countAll());
        model.addAttribute("totalCost", maintenanceService.sumCostYear(LocalDate.now().getYear()));
        model.addAttribute("avgCost", maintenanceService.avgCostYear(LocalDate.now().getYear()));
        addOptions(model);
        return "fleet/maintenance/index";
    }
    @GetMapping("/new")
    public String newForm(@RequestParam(value = "taskId", required = false) Long taskId,
            @RequestParam(value = "vehicleId", required = false) Long vehicleId,
            Model model) {
            Maintenance m = new Maintenance();
            if (taskId != null) {
            VehicleTask task = vehicleTaskService.findById(taskId);
            if (task != null) {
              m.setVehicle(task.getVehicle());
              if (task.getType() != null) {
        	  m.setType(task.getType());
              }
              m.setDate(task.getDueDate());
              m.setMileage(task.getDueMileage());
            }
        } else if (vehicleId != null) {
        m.setVehicle(vehicleService.findByIdOrThrow(vehicleId));
        }
        model.addAttribute("maintenance", m);
        addOptions(model);
        return "fleet/maintenance/form";
    }

    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("maintenance") Maintenance maintenance,
	    BindingResult binding,
            @RequestParam(value = "redirect", required = false) String redirect,
            Model model) {
        if (binding.hasErrors()) {
            addOptions(model);
            return "fleet/maintenance/form";
        }
        try {
            Maintenance saved = maintenanceService.create(maintenance);
            if ("vehicle".equals(redirect)) {
                return "redirect:/fleet/vehicles/" + saved.getVehicle().getId();
            }
            return "redirect:/fleet/maintenance/" + saved.getId() + "/edit";
        } catch (IllegalArgumentException e) {
            binding.rejectValue("mileage", "invalidMileage", e.getMessage());
            // ensure the form remains in creation mode after a validation failure
            maintenance.setId(null);
            addOptions(model);
            return "fleet/maintenance/form";
        }
    }

    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Maintenance m = maintenanceService.findByIdOrThrow(id);
        model.addAttribute("maintenance", m);
        model.addAttribute("documents", documentService.getMaintenanceDocuments(id));
        
     // Filtra i tipi documento solo per la manutenzione
        List<DocumentType> docTypes = Arrays.stream(DocumentType.values())
                .filter(dt -> dt == DocumentType.MAINT_REPORT || dt == DocumentType.MAINT_INVOICE)
                .collect(Collectors.toList());
        
        model.addAttribute("docTypes", docTypes);
        addOptions(model);
        return "fleet/maintenance/form";
    }
    
    /** Upload documento di manutenzione */
    @PostMapping("/{id}/docs")
    public String uploadDoc(@PathVariable Long id,
                            @RequestParam("file") MultipartFile file,
                            @RequestParam("type") DocumentType type,
                            @RequestParam(value = "issueDate", required = false) String issueDate,
                            @RequestParam(value = "expiryDate", required = false) String expiryDate) throws IOException {
        LocalDate issued = (issueDate == null || issueDate.isBlank()) ? null : LocalDate.parse(issueDate);
        LocalDate exp = (expiryDate == null || expiryDate.isBlank()) ? null : LocalDate.parse(expiryDate);
        documentService.uploadMaintenanceDocument(id, file, type, issued, exp);
        return "redirect:/fleet/maintenance/" + id + "/edit";
    }

    /** Download di un documento di manutenzione */
    @GetMapping("/{id}/docs/{filename:.+}")
    public ResponseEntity<Resource> serveFile(@PathVariable Long id,
                                              @PathVariable String filename,
                                              HttpServletRequest request) throws IOException {
        Resource resource = documentService.loadMaintenanceDocumentAsResource(id, filename);
        String contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }
        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }

    @GetMapping("/{mId}/docs/{docId}/delete")
    public String deleteDocument(@PathVariable("mId") Long maintId,
                                 @PathVariable("docId") Long docId) throws IOException {
        documentService.deleteDocument(docId);
        return "redirect:/fleet/maintenance/" + maintId + "/edit";
    }

    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("maintenance") Maintenance maintenance,
                         BindingResult binding,
                         @RequestParam(value = "redirect", required = false) String redirect,
                         Model model) {
        if (binding.hasErrors()) {
            addOptions(model);
            return "fleet/maintenance/form";
        }
        try {
            maintenanceService.update(id, maintenance);
            if ("vehicle".equals(redirect)) {
                return "redirect:/fleet/vehicles/" + maintenance.getVehicle().getId();
            }
            return "redirect:/fleet/maintenance/" + id + "/edit";
        } catch (IllegalArgumentException e) {
            binding.rejectValue("mileage", "invalidMileage", e.getMessage());
            addOptions(model);
            return "fleet/maintenance/form";
        }
    }

    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        maintenanceService.delete(id);
        return "redirect:/fleet/maintenance";
    }

    private void addOptions(Model model) {
        model.addAttribute("vehicles", vehicleService.findAll());
        model.addAttribute("suppliers", supplierService.findAll());
        model.addAttribute("types", taskTypeService.findAll());
    }
}