package com.veely.controller;

import com.veely.entity.Assignment;
import com.veely.entity.Document;
import com.veely.entity.Vehicle;
import com.veely.entity.VehicleTask;
import com.veely.model.DocumentType;
import com.veely.model.OwnershipType;
import com.veely.model.VehicleStatus;
import com.veely.repository.DocumentRepository;
import com.veely.service.AssignmentService;
import com.veely.service.DocumentService;
import com.veely.service.FuelCardService;
import com.veely.service.SecureFileUploadService;
import com.veely.service.SupplierService;
import com.veely.service.TaskTypeService;
import com.veely.service.VehicleService;
import com.veely.service.VehicleTaskService;
import com.veely.validation.FileValidator;

import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;
import java.util.List;
import java.util.ArrayList;
import java.util.Collections;
import java.time.LocalDate;
import java.time.temporal.ChronoUnit;
import java.time.format.DateTimeFormatter;
import java.nio.charset.StandardCharsets;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import jakarta.servlet.http.HttpServletResponse; 

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;

import jakarta.validation.Valid;
import java.io.IOException;
import java.time.LocalDate;
import java.util.List;

@Slf4j
@Controller
@RequestMapping("/fleet/vehicles")
@RequiredArgsConstructor
public class VehicleController {

    private final VehicleService vehicleService;
    private final DocumentService documentService;
    private final DocumentRepository documentRepo; // per eventuali operazioni dirette
    private final SupplierService supplierService;
    private final AssignmentService assignmentService;
    private final FuelCardService fuelCardService;
    private final VehicleTaskService vehicleTaskService;
    private final TaskTypeService taskTypeService;
    private final SecureFileUploadService fileUploadService;
    private final FileValidator fileValidator;
    
    private static final DocumentType[] VEHICLE_DOC_TYPES = {
            DocumentType.VEHICLE_REGISTRATION,
            DocumentType.INSURANCE,
            DocumentType.MAINTENANCE,
            DocumentType.VEHICLE_IMAGE,
            DocumentType.LEASE_CONTRACT,
            DocumentType.MAINT_REPORT,
            DocumentType.MAINT_INVOICE
        };
    
    /** Mostra la lista (tabella) di tutti i veicoli **/
    @GetMapping
    public String list(Model model,
                       @RequestParam(value = "status", required = false) String status,
                       @RequestParam(value = "type", required = false) String type,
                       @RequestParam(value = "search", required = false) String search) {
        
        List<Vehicle> allVehicles = vehicleService.findAll();
        List<Vehicle> filteredVehicles = allVehicles;
        
        // Apply filters if provided
        if (status != null && !status.isEmpty()) {
            filteredVehicles = filteredVehicles.stream()
                .filter(v -> v.getStatus().name().equals(status))
                .collect(Collectors.toList());
        }
        
        if (type != null && !type.isEmpty()) {
            filteredVehicles = filteredVehicles.stream()
                .filter(v -> v.getType().name().equals(type))
                .collect(Collectors.toList());
        }
        
        if (search != null && !search.trim().isEmpty()) {
            String searchLower = search.toLowerCase().trim();
            filteredVehicles = filteredVehicles.stream()
                .filter(v -> 
                    v.getPlate().toLowerCase().contains(searchLower) ||
                    v.getBrand().toLowerCase().contains(searchLower) ||
                    v.getModel().toLowerCase().contains(searchLower) ||
                    (v.getChassisNumber() != null && v.getChassisNumber().toLowerCase().contains(searchLower))
                )
                .collect(Collectors.toList());
        }
        
        // Calculate statistics
        Map<String, Long> statusStats = allVehicles.stream()
            .collect(Collectors.groupingBy(
                vehicle -> vehicle.getStatus().name(),
                Collectors.counting()
            ));
        
        Map<String, Long> typeStats = allVehicles.stream()
            .collect(Collectors.groupingBy(
                vehicle -> vehicle.getType().name(),
                Collectors.counting()
            ));
        
        // Count vehicles with upcoming deadlines
        LocalDate now = LocalDate.now();
        LocalDate thirtyDaysFromNow = now.plusDays(30);
        
        long vehiclesWithUpcomingDeadlines = allVehicles.stream()
            .mapToLong(v -> {
                long count = 0;
                if (v.getInsuranceExpiryDate() != null && 
                    v.getInsuranceExpiryDate().isBefore(thirtyDaysFromNow)) {
                    count++;
                }
                if (v.getCarTaxExpiryDate() != null && 
                    v.getCarTaxExpiryDate().isBefore(thirtyDaysFromNow)) {
                    count++;
                }
                return count > 0 ? 1 : 0;
            })
            .sum();
        
     // *** AGGIUNTA: Carica le immagini dei veicoli ***
        Map<Long, Document> vehicleImages = new HashMap<>();
        for (Vehicle vehicle : filteredVehicles) {
            List<Document> docs = documentService.getVehicleDocuments(vehicle.getId());
            Document image = docs.stream()
                .filter(d -> d.getType() == DocumentType.VEHICLE_IMAGE)
                .findFirst()
                .orElse(null);
            if (image != null) {
                vehicleImages.put(vehicle.getId(), image);
            }
        }
        
        model.addAttribute("vehicles", filteredVehicles);
        model.addAttribute("totalVehicles", allVehicles.size());
        model.addAttribute("statusStats", statusStats);
        model.addAttribute("typeStats", typeStats);
        model.addAttribute("upcomingDeadlines", vehiclesWithUpcomingDeadlines);
        model.addAttribute("vehicleImages", vehicleImages); // *** AGGIUNTA ***
        model.addAttribute("currentFilters", Map.of(
            "status", status != null ? status : "",
            "type", type != null ? type : "",
            "search", search != null ? search : ""
        ));
        
        return "fleet/vehicles/index";
    }

    /** Form di creazione veicolo */
    @GetMapping("/new")
    public String createForm(Model model) {
    	model.addAttribute("vehicle", new Vehicle());
        addFormOptions(model);
        model.addAttribute("tasks", List.of());
        return "fleet/vehicles/form";
    }

    /** Salva un nuovo veicolo */
    @PostMapping("new")
    public String saveNew(@Valid @ModelAttribute("vehicle") Vehicle vehicle,
                          BindingResult binding, Model model) {
        if (binding.hasErrors()) {
        	addFormOptions(model);
            return "fleet/vehicles/form";
        }
        Vehicle saved = vehicleService.create(vehicle);
        return "redirect:/fleet/vehicles/" + saved.getId() + "/edit";
    }

    /** Form di modifica veicolo con sezione documenti */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        Vehicle v = vehicleService.findByIdOrThrow(id);
        List<Document> docs = documentService.getVehicleDocuments(id);
        Document image = docs.stream()
                .filter(d -> d.getType() == DocumentType.VEHICLE_IMAGE)
                .findFirst()
                .orElse(null);
        
        List<VehicleTask> tasks = vehicleTaskService.findByVehicle(id);
        model.addAttribute("vehicle", v);
        model.addAttribute("documents", docs);
        model.addAttribute("vehicleImage", image);
        model.addAttribute("tasks", tasks);
        model.addAttribute("autoTypes", taskTypeService.findAuto());
        java.util.Set<Long> active = tasks.stream()
                .filter(t -> t.getType() != null && t.getType().isAuto())
                .map(t -> t.getType().getId())
                .collect(java.util.stream.Collectors.toSet());
        model.addAttribute("activeAutoIds", active);
        addFormOptions(model);
        return "fleet/vehicles/form";
    }

    /** Aggiorna un veicolo esistente */
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                         @Valid @ModelAttribute("vehicle") Vehicle vehicle,
                         BindingResult binding, Model model) {
        if (binding.hasErrors()) {
            List<Document> docs = documentService.getVehicleDocuments(id);
            Document image = docs.stream()
                    .filter(d -> d.getType() == DocumentType.VEHICLE_IMAGE)
                    .findFirst()
                    .orElse(null);
            List<VehicleTask> tasks = vehicleTaskService.findByVehicle(id);
            model.addAttribute("documents", docs);
            model.addAttribute("vehicleImage", image);
            model.addAttribute("tasks", tasks);
            model.addAttribute("autoTypes", taskTypeService.findAuto());
            java.util.Set<Long> active = tasks.stream()
                    .filter(t -> t.getType() != null && t.getType().isAuto())
                    .map(t -> t.getType().getId())
                    .collect(java.util.stream.Collectors.toSet());
            model.addAttribute("activeAutoIds", active);
            addFormOptions(model);
            return "fleet/vehicles/form";
        }
        vehicleService.update(id, vehicle);
        return "redirect:/fleet/vehicles/" + id + "/edit";
    }
    
    /** Aggiorna il chilometraggio corrente del veicolo */
    @PostMapping("/{id}/mileage")
    public String updateMileage(@PathVariable Long id,
                                @RequestParam("newMileage") int newMileage) {
        vehicleService.updateMileage(id, newMileage);
        return "redirect:/fleet/vehicles/" + id;
    }

    /** Elimina un veicolo */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id) {
        vehicleService.delete(id);
        return "redirect:/fleet/vehicles";
    }

    /** Mostra il dettaglio di un veicolo, con tab per foto e documenti */
    @GetMapping("/{id}")
    public String detail(@PathVariable Long id, Model model) {
        Vehicle v = vehicleService.findByIdOrThrow(id);
        List<Document> docs = documentRepo.findByVehicleId(id);
        Document image = docs.stream()
                .filter(d -> d.getType() == DocumentType.VEHICLE_IMAGE)
                .findFirst()
                .orElse(null);
        Assignment asg = assignmentService.findActiveByVehicle(id);
        if (asg != null) {
            model.addAttribute("assignedEmployee", asg.getEmployment().getEmployee());
        }
        List<VehicleTask> tasks = vehicleTaskService.findByVehicle(id);
        model.addAttribute("vehicle", v);
        model.addAttribute("documents", docs);
        model.addAttribute("vehicleImage", image);
        model.addAttribute("tasks", tasks);
        model.addAttribute("docTypes", VEHICLE_DOC_TYPES);
        return "fleet/vehicles/detail";
    }

    /** Upload foto veicolo OLD 29/07/2025 
    @PostMapping("/{id}/photos")
    public String uploadPhoto(@PathVariable Long id,
                              @RequestParam("file") MultipartFile file) throws IOException {
        vehicleService.uploadPhoto(id, file);
        return "redirect:/fleet/vehicles/" + id + "/edit";
    }
    */
    
    /** Upload foto veicolo con validazione */
    @PostMapping("/{id}/photos")
    public String uploadPhoto(@PathVariable Long id,
                              @RequestParam("file") MultipartFile file,
                              RedirectAttributes redirectAttributes) {
        try {
            // Usa il servizio sicuro per validare l'immagine
            String filename = fileUploadService.uploadImage(file, "vehicles/" + id + "/photos");
            
            // Usa il metodo esistente del VehicleService per salvare
            // (Non c'è un savePhotoPath, ma possiamo usare uploadPhoto)
            vehicleService.uploadPhoto(id, file);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Foto caricata con successo");
            
        } catch (FileValidator.ValidationException e) {
            log.error("Validazione fallita per foto veicolo {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Errore: " + e.getMessage());
            
        } catch (IOException e) {
            log.error("Errore IO durante upload foto veicolo {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Errore durante il caricamento del file");
        }
        
        return "redirect:/fleet/vehicles/" + id + "/edit";
    }

    
    

    /** Upload documento veicolo 
    @PostMapping("/{id}/docs")
    public String uploadDoc(@PathVariable Long id,
                            @RequestParam("file") MultipartFile file,
                            @RequestParam("type") DocumentType type,
                            @RequestParam(value = "issueDate", required = false) String issueDate,
                            @RequestParam("expiryDate") String expiryDate) {
    	LocalDate issued = (issueDate == null || issueDate.isBlank()) ? null : LocalDate.parse(issueDate);
        LocalDate exp = expiryDate.isBlank() ? null : LocalDate.parse(expiryDate);
        vehicleService.uploadDocument(id, file, type, issued, exp);
        return "redirect:/fleet/vehicles/" + id + "/edit";
    }
    */
    
    /** Upload documento veicolo con validazione */
    @PostMapping("/{id}/docs")
    public String uploadDoc(@PathVariable Long id,
                            @RequestParam("file") MultipartFile file,
                            @RequestParam("type") DocumentType type,
                            @RequestParam(value = "issueDate", required = false) String issueDate,
                            @RequestParam("expiryDate") String expiryDate,
                            RedirectAttributes redirectAttributes) {
        try {
            // Valida il file usando il servizio sicuro
            fileValidator.validateDocument(file);
            
            // Prepara le date
            LocalDate issued = (issueDate == null || issueDate.isBlank()) 
                ? null : LocalDate.parse(issueDate);
            LocalDate exp = expiryDate.isBlank() ? null : LocalDate.parse(expiryDate);
            
            // Usa il metodo esistente del DocumentService per l'upload
            documentService.uploadVehicleDocument(id, file, type, issued, exp);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Documento caricato con successo");
            
        } catch (FileValidator.ValidationException e) {
            log.error("Validazione fallita per documento veicolo {}: {}", id, e.getMessage());
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Errore: " + e.getMessage());
            
        } catch (Exception e) {
            log.error("Errore durante upload documento veicolo {}", id, e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Errore durante il caricamento del documento");
        }
        
        return "redirect:/fleet/vehicles/" + id + "/edit";
    }
    
    /** Upload documento veicolo 
    @PostMapping("/{id}/det")
    public String uploadDocDet(@PathVariable Long id,
                            @RequestParam("file") MultipartFile file,
                            @RequestParam("type") DocumentType type,
                            @RequestParam(value = "issueDate", required = false) String issueDate,
                            @RequestParam("expiryDate") String expiryDate) {
    	LocalDate issued = (issueDate == null || issueDate.isBlank()) ? null : LocalDate.parse(issueDate);
        LocalDate exp = expiryDate.isBlank() ? null : LocalDate.parse(expiryDate);
        vehicleService.uploadDocument(id, file, type, issued, exp);
        return "redirect:/fleet/vehicles/" + id;
    }
    
    */
    
    @PostMapping("/{id}/det")
    public String uploadDocDet(@PathVariable Long id,
                               @RequestParam("file") MultipartFile file,
                               @RequestParam("type") DocumentType type,
                               @RequestParam(value = "issueDate", required = false) String issueDate,
                               @RequestParam("expiryDate") String expiryDate,
                               RedirectAttributes redirectAttributes) {
        try {
            fileValidator.validateDocument(file);
            
            LocalDate issued = (issueDate == null || issueDate.isBlank()) 
                ? null : LocalDate.parse(issueDate);
            LocalDate exp = expiryDate.isBlank() ? null : LocalDate.parse(expiryDate);
            
            documentService.uploadVehicleDocument(id, file, type, issued, exp);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Documento caricato con successo");
            
        } catch (FileValidator.ValidationException e) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Errore: " + e.getMessage());
        } catch (Exception e) {
            log.error("Errore durante upload documento", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Errore durante il caricamento");
        }
        
        return "redirect:/fleet/vehicles/" + id;
    }
    
    /** Download di un file (foto o doc): restituirà il Resource e header content disposition */
    @GetMapping("/files/{area}/{filename:.+}")
    @ResponseBody
    public Resource serveFile(@PathVariable String area,
                              @PathVariable String filename) {
        return vehicleService.loadDocument(Long.valueOf(area), filename);
        // Si assume area = vehicleId, per foto cambiare loadPhoto se necessario.
    }
    
    /** Elimina un documento veicolo */
    @GetMapping("/{vehId}/docs/{docId}/delete")
    public String deleteDocument(@PathVariable("vehId") Long vehId,
                                 @PathVariable("docId") Long docId) throws IOException {
        documentService.deleteDocument(docId);
        return "redirect:/fleet/vehicles/" + vehId;
    }
    
    private void addFormOptions(Model model) {
        model.addAttribute("statuses", VehicleStatus.values());
        model.addAttribute("docTypes", VEHICLE_DOC_TYPES);
        model.addAttribute("ownershipTypes", OwnershipType.values());
        model.addAttribute("suppliers", supplierService.findAll());
        model.addAttribute("fuelCards", fuelCardService.findAll());
    }
    
    
    

    /**
     * API endpoint for vehicle statistics (for AJAX calls)
     */
    @GetMapping("/api/stats")
    @ResponseBody
    public Map<String, Object> getVehicleStats() {
        List<Vehicle> vehicles = vehicleService.findAll();
        
        Map<String, Long> statusStats = vehicles.stream()
            .collect(Collectors.groupingBy(
                vehicle -> vehicle.getStatus().name(),
                Collectors.counting()
            ));
        
        Map<String, Long> typeStats = vehicles.stream()
            .collect(Collectors.groupingBy(
                vehicle -> vehicle.getType().name(),
                Collectors.counting()
            ));
        
        Map<String, Object> stats = new HashMap<>();
        stats.put("total", vehicles.size());
        stats.put("byStatus", statusStats);
        stats.put("byType", typeStats);
        
        return stats;
    }

    /**
     * Export vehicles data as CSV
     */
    @GetMapping("/export")
    public ResponseEntity<byte[]> exportVehicles(
            @RequestParam(value = "format", defaultValue = "csv") String format,
            HttpServletResponse response) throws IOException {
        
        List<Vehicle> vehicles = vehicleService.findAll();
        
        if ("csv".equalsIgnoreCase(format)) {
            StringBuilder csv = new StringBuilder();
            csv.append("Targa,Marca,Modello,Tipo,Anno,Stato,Chilometraggio,Assicurazione,Bollo\n");
            
            for (Vehicle v : vehicles) {
                csv.append(String.format("%s,%s,%s,%s,%d,%s,%s,%s,%s\n",
                    v.getPlate(),
                    v.getBrand(),
                    v.getModel(),
                    v.getType().getDisplayName(),
                    v.getYear(),
                    v.getStatus().getDisplayName(),
                    v.getCurrentMileage() != null ? v.getCurrentMileage().toString() : "N/D",
                    v.getInsuranceExpiryDate() != null ? v.getInsuranceExpiryDate().toString() : "N/D",
                    v.getCarTaxExpiryDate() != null ? v.getCarTaxExpiryDate().toString() : "N/D"
                ));
            }
            
            byte[] data = csv.toString().getBytes(StandardCharsets.UTF_8);
            
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.parseMediaType("text/csv"));
            headers.setContentDispositionFormData("attachment", "veicoli_" + 
                LocalDate.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd")) + ".csv");
            
            return ResponseEntity.ok()
                .headers(headers)
                .body(data);
        }
        
        throw new IllegalArgumentException("Formato non supportato: " + format);
    }

    /**
     * Bulk update vehicle status
     */
    @PostMapping("/bulk-update")
    public String bulkUpdateStatus(@RequestParam("vehicleIds") List<Long> vehicleIds,
                                  @RequestParam("newStatus") VehicleStatus newStatus,
                                  RedirectAttributes redirectAttributes) {
        try {
            int updatedCount = 0;
            for (Long id : vehicleIds) {
                Vehicle vehicle = vehicleService.findByIdOrThrow(id);
                vehicle.setStatus(newStatus);
                vehicleService.update(id, vehicle);
                updatedCount++;
            }
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Aggiornati " + updatedCount + " veicoli con successo");
                
        } catch (Exception e) {
            log.error("Errore durante l'aggiornamento bulk", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Errore durante l'aggiornamento dei veicoli");
        }
        
        return "redirect:/fleet/vehicles";
    }

    /**
     * Get vehicle quick info for tooltips/popups
     */
    @GetMapping("/{id}/quick-info")
    @ResponseBody
    public Map<String, Object> getVehicleQuickInfo(@PathVariable Long id) {
        Vehicle vehicle = vehicleService.findByIdOrThrow(id);
        
        Map<String, Object> info = new HashMap<>();
        info.put("plate", vehicle.getPlate());
        info.put("brand", vehicle.getBrand());
        info.put("model", vehicle.getModel());
        info.put("year", vehicle.getYear());
        info.put("status", vehicle.getStatus().getDisplayName());
        info.put("type", vehicle.getType().getDisplayName());
        info.put("currentMileage", vehicle.getCurrentMileage());
        
        // Check for upcoming deadlines
        LocalDate now = LocalDate.now();
        List<String> deadlines = new ArrayList<>();
        
        if (vehicle.getInsuranceExpiryDate() != null) {
            long daysUntilInsurance = ChronoUnit.DAYS.between(now, vehicle.getInsuranceExpiryDate());
            if (daysUntilInsurance <= 30) {
                deadlines.add("Assicurazione: " + daysUntilInsurance + " giorni");
            }
        }
        
        if (vehicle.getCarTaxExpiryDate() != null) {
            long daysUntilTax = ChronoUnit.DAYS.between(now, vehicle.getCarTaxExpiryDate());
            if (daysUntilTax <= 30) {
                deadlines.add("Bollo: " + daysUntilTax + " giorni");
            }
        }
        
        info.put("upcomingDeadlines", deadlines);
        
        // Get assignment info
        Assignment activeAssignment = assignmentService.findActiveByVehicle(id);
        if (activeAssignment != null) {
            info.put("assignedTo", activeAssignment.getEmployment().getEmployee().getFullName());
        }
        
        return info;
    }

    /**
     * Search vehicles with autocomplete
     */
    @GetMapping("/search")
    @ResponseBody
    public List<Map<String, Object>> searchVehicles(@RequestParam("q") String query) {
        if (query == null || query.trim().length() < 2) {
            return Collections.emptyList();
        }
        
        String searchTerm = query.toLowerCase().trim();
        List<Vehicle> vehicles = vehicleService.findAll().stream()
            .filter(v -> 
                v.getPlate().toLowerCase().contains(searchTerm) ||
                v.getBrand().toLowerCase().contains(searchTerm) ||
                v.getModel().toLowerCase().contains(searchTerm)
            )
            .limit(10)
            .collect(Collectors.toList());
        
        return vehicles.stream()
            .map(v -> {
                Map<String, Object> item = new HashMap<>();
                item.put("id", v.getId());
                item.put("plate", v.getPlate());
                item.put("brand", v.getBrand());
                item.put("model", v.getModel());
                item.put("displayText", v.getPlate() + " - " + v.getBrand() + " " + v.getModel());
                return item;
            })
            .collect(Collectors.toList());
    }
}
