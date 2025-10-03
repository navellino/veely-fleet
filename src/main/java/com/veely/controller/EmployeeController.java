package com.veely.controller;

import com.veely.entity.Document;
import com.veely.entity.Employee;
import com.veely.entity.Employment;
import com.veely.model.DocumentType;
import com.veely.model.EducationLevel;
import com.veely.model.EmploymentStatus;
import com.veely.model.FullAddress;
import com.veely.model.Gender;
import com.veely.model.MaritalStatus;
import com.veely.service.CountryService;
import com.veely.service.CountryService.CountryDto;
import com.veely.service.DocumentService;
import com.veely.service.EmployeeService;
import com.veely.service.EmploymentService;
import com.veely.service.LocationService;
import com.veely.service.LocationService.CityDto;
import com.veely.service.LocationService.ProvinceDto;
import com.veely.service.LocationService.RegionDto;
import com.veely.service.EmployeeRoleService;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.http.*;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import java.io.IOException;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j 
@Controller
@RequestMapping("/fleet/employees")
@RequiredArgsConstructor
public class EmployeeController {

    private final EmployeeService employeeService;
    private final DocumentService documentService;
    private final EmploymentService employmentService;
    private final CountryService countryService;
    private final LocationService locationService;
    private final EmployeeRoleService employeeRoleService;
    
    private static final DocumentType[] EMPLOYEE_DOC_TYPES = {
            DocumentType.IDENTITY_PHOTO,
            DocumentType.FISCAL_CODE,
            DocumentType.IDENTITY_DOCUMENT,
            DocumentType.SAP,
            DocumentType.GRADE,
            DocumentType.OTHER
        };
    
    /*
     * IDENTITY_PHOTO("Foto Profilo"),
    IDENTITY_DOCUMENT("Documento identità"),
    FISCAL_CODE("Tessera Sanitaria"),
    SAP("Scheda Anagrafica Professionale"),
    GRADE("Titolo di studio"),
     */
    
    @GetMapping("/manage")
    public String manage(
	    //log.debug("Caricamento pagina gestione dipendenti - page: {}, size: {}", page, size);
      @RequestParam(defaultValue="0") int page,
      @RequestParam(defaultValue="15") int size,
      Model model
    ) {
    	// 1) Pagine e metadati
	PageRequest pr = PageRequest.of(page, size, Sort.by("lastName"));
	Page<Employee> emps = employeeService.findAllWithEmployments(pr);
	
        List<Employee> employeeList = emps.getContent();
        
        model.addAttribute("employees", emps);

     // 2) Preparo la mappa con liste vuote
        Map<Long, List<Employment>> empToEmpls = new HashMap<>();
        for (Employee emp : employeeList) {
            empToEmpls.put(emp.getId(), new ArrayList<>());
        }
        
        // 3) Carico tutti gli Employment dei dipendenti correnti
        List<Long> ids = employeeList.stream()
                                     .map(Employee::getId)
                                     .toList();
        List<Employment> allEmpls = employmentService.findByEmployeeIds(ids);
        
     // Mappa delle foto profilo dei dipendenti
        Map<Long, Document> profilePhotos = documentService.getProfilePhotosBatch(
                emps.getContent().stream().map(Employee::getId).toList()
            );
        
     // 4) Popolo la mappa
        for (Employment empl : allEmpls) {
            Long empId = empl.getEmployee().getId();
            empToEmpls.get(empId).add(empl);
        }
        
     // Mappa degli impieghi attivi per mostrare la qualifica corrente
        Map<Long, Employment> activeEmpls = new HashMap<>();
        for (Employment empl : allEmpls) {
            if (empl.getStatus() == EmploymentStatus.ACTIVE) {
                activeEmpls.putIfAbsent(empl.getEmployee().getId(), empl);
            }
        }

     // Determina lo stato del rapporto di lavoro mostrato in tabella
        Map<Long, EmploymentStatus> employmentStatuses = new HashMap<>();
        Comparator<Employment> latestEmploymentComparator = Comparator
                .comparing(Employment::getStartDate, Comparator.nullsFirst(Comparator.naturalOrder()))
                .thenComparing(Employment::getId);
        
        for (Employee emp : employeeList) {
            List<Employment> employments = empToEmpls.get(emp.getId());
            EmploymentStatus status = null;
            
            if (employments != null && !employments.isEmpty()) {
                
                boolean hasActiveEmployment = employments.stream()
                		 .anyMatch(e -> e.getStatus() == EmploymentStatus.ACTIVE);
                
                if (hasActiveEmployment) {
                	status = EmploymentStatus.ACTIVE;
                } else {
                	Employment latest = employments.stream()
                            .max(latestEmploymentComparator)
                            .orElse(null);
                    if (latest != null) {
                        status = latest.getStatus();
                    }
                }
            }
            
            employmentStatuses.put(emp.getId(), status);
        }
        
        long activeCount = employmentStatuses.values().stream()
                .filter(status -> status == EmploymentStatus.ACTIVE)
                .count();
        long inactiveCount = employeeList.size() - activeCount;
        
        // Aggiungi le statistiche al model
        model.addAttribute("activeEmployeesCount", activeCount);
        model.addAttribute("inactiveEmployeesCount", inactiveCount);
        model.addAttribute("employmentStatuses", employmentStatuses);
        model.addAttribute("empToEmpls", empToEmpls);
        model.addAttribute("activeEmployments", activeEmpls);
        model.addAttribute("profilePhotos", profilePhotos);
        return "fleet/employees/manage";
    }
        
    /** 1) Lista di tutti i dipendenti */
    @GetMapping
    public String list(Model model) {
        model.addAttribute("employees", employeeService.findAll());
        return "fleet/employees/list";
    }

    @GetMapping("/export")
    public ResponseEntity<byte[]> exportPdf() throws IOException {
        byte[] pdf = employeeService.exportStyledPdf();
        return ResponseEntity.ok()
                .contentType(MediaType.APPLICATION_PDF)
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=Elenco_dipendenti.pdf")
                .body(pdf);
    }


    
        /** 2) Step 1: form vuoto per creare il dipendente (solo dati anagrafici) */
        @GetMapping("/new")
        public String newForm(Model model) {
            Employee employee = new Employee();
            
            FullAddress ra = employee.getResidenceAddress();
            if (ra == null) {
                ra = new FullAddress();
                employee.setResidenceAddress(ra);
            }
            if (ra.getCountryCode() == null) {
                ra.setCountryCode("IT");
                ra.setCountry("Italia");
            }
            
            model.addAttribute("employee", employee);
            model.addAttribute("residenceAddress", employee.getResidenceAddress());
            
            // Reference data per i dropdown
            populateFormData(model);
            
            return "fleet/employees/form";
        }

        /**
         * 3) POST "step 1" CON VALIDAZIONE AVANZATA
         */
        @PostMapping("/new")
        public String create(@Valid @ModelAttribute("employee") Employee employee,
                            BindingResult bindingResult,
                            Model model,
                            RedirectAttributes redirectAttributes) {
            
            log.info("Tentativo creazione dipendente: {} {}", 
                    employee.getFirstName(), employee.getLastName());
            
            // Validazione business rules aggiuntive
            validateEmployeeBusinessRules(employee, bindingResult);
            
            // Se ci sono errori di validazione
            if (bindingResult.hasErrors()) {
                log.warn("Errori di validazione per dipendente: {} errori trovati", 
                        bindingResult.getErrorCount());
                
                // Ricarica i dati per il form
                populateFormData(model);
                
                // Raggruppa errori per campo
                Map<String, List<String>> errorsByField = groupErrorsByField(bindingResult);
                model.addAttribute("fieldErrors", errorsByField);
                
                // Aggiungi messaggi di errore user-friendly
                model.addAttribute("hasErrors", true);
                model.addAttribute("errorMessage", "Controlla i dati inseriti e riprova");
                
                // Log dettagliato degli errori per debug
                bindingResult.getAllErrors().forEach(error -> 
                    log.debug("Errore validazione: {}", error.getDefaultMessage()));
                
                return "fleet/employees/form";
            }
            
            try {
                Employee saved = employeeService.create(employee);
                
                log.info("Dipendente creato con successo: ID={}, Email={}", 
                        saved.getId(), saved.getEmail());
                
                redirectAttributes.addFlashAttribute("successMessage", 
                    "Dipendente " + saved.getFirstName() + " " + saved.getLastName() + " creato con successo");
                
                return "redirect:/fleet/employees/" + saved.getId() + "/edit";
                
            } catch (DataIntegrityViolationException e) {
                log.error("Errore integrità dati durante creazione dipendente", e);
                handleDataIntegrityError(e, bindingResult, model);
                populateFormData(model);
                return "fleet/employees/form";
                
            } catch (Exception e) {
                log.error("Errore generico durante creazione dipendente", e);
                model.addAttribute("errorMessage", 
                    "Si è verificato un errore durante il salvataggio. Riprova.");
                populateFormData(model);
                return "fleet/employees/form";
            }
        }
        
        /** 4) Step 2: form di modifica + upload documenti CON VALIDAZIONE */
        @GetMapping("/{id}/edit")
        public String editForm(@PathVariable Long id, Model model) {
            Employee employee = employeeService.findByIdOrThrow(id);

            FullAddress ra = employee.getResidenceAddress();
            if (ra == null) {
                ra = new FullAddress();
                employee.setResidenceAddress(ra);
            }
            if (ra.getCountryCode() == null) {
                ra.setCountryCode("IT");
                ra.setCountry("Italia");
            }
            
            if (employee.getResidenceAddress() == null) {
                employee.setResidenceAddress(new FullAddress());
            }
            
            List<Document> documents = documentService.getEmployeeDocuments(id);
            
            String selectedCountry = employee.getResidenceAddress().getCountryCode();
            String selectedRegion  = employee.getResidenceAddress().getRegionCode();
            String selectedProvince = employee.getResidenceAddress().getProvinceCode();

            List<CountryDto> countries = countryService.getAll();
            List<RegionDto> regions = locationService.getRegions(selectedCountry);
            List<ProvinceDto> provinces = locationService.getProvinces(selectedCountry);
            List<CityDto> cities = locationService.getCities(selectedProvince);
            
            model.addAttribute("residenceAddress", employee.getResidenceAddress());
            populateFormData(model);

            model.addAttribute("employee", employee);
            
            String cc = employee.getResidenceAddress() != null ? employee.getResidenceAddress().getCountryCode() : null;
            if (cc != null) {
              model.addAttribute("regions", locationService.getRegions(cc));
              String rc = employee.getResidenceAddress().getRegionCode();
              if (rc != null) {
                model.addAttribute("provinces", locationService.getProvinces(rc));
                String pc = employee.getResidenceAddress().getProvinceCode();
                if (pc != null) {
                  model.addAttribute("cities", locationService.getCities(pc));
                }
              }
            }
            
            Document profilePhoto = documents.stream()
                    .filter(doc -> doc.getType() == DocumentType.IDENTITY_PHOTO)
                    .findFirst()
                    .orElse(null);
            
            model.addAttribute("profilePhoto", profilePhoto);
            model.addAttribute("documents", documents);

            return "fleet/employees/form";
        }
    
    @PostMapping("/{id}/docs")
    public String uploadDoc(
        @PathVariable Long id,
        @RequestParam("file") MultipartFile file,
        @RequestParam("type") DocumentType type,
        @RequestParam(value="issueDate", required=false) String issueDate,
        @RequestParam(value="expiryDate", required=false) String expiryDate,
        RedirectAttributes ra
    ) throws IOException {
        LocalDate issued = (issueDate == null || issueDate.isBlank())
            ? null : LocalDate.parse(issueDate);
        LocalDate exp = (expiryDate == null || expiryDate.isBlank())
            ? null : LocalDate.parse(expiryDate);
        documentService.uploadEmployeeDocument(id, file, type, issued, exp);
        ra.addFlashAttribute("success", "Documento caricato");
        return "redirect:/fleet/employees/" + id + "/edit?tab=docs";
    }

    /**
     * 5) Gestione POST “step 2”: aggiorna anagrafica, salva eventuale file,
     *    e rimane sul medesimo form (per caricare altri documenti o uscire).
     */
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                        @Valid @ModelAttribute("employee") Employee employee,
                        BindingResult bindingResult,
                        Model model,
                        RedirectAttributes redirectAttributes) {
        
        log.info("Tentativo aggiornamento dipendente ID: {}", id);
        
        // Validazione business rules (escludendo i check di unicità per lo stesso record)
        validateEmployeeBusinessRulesForUpdate(id, employee, bindingResult);
        
        if (bindingResult.hasErrors()) {
            log.warn("Errori di validazione per aggiornamento dipendente ID: {}", id);
            
            // Ricarica dati form
            populateFormData(model);
            loadLocationData(model, employee);
            
            // Raggruppa errori
            Map<String, List<String>> errorsByField = groupErrorsByField(bindingResult);
            model.addAttribute("fieldErrors", errorsByField);
            model.addAttribute("hasErrors", true);
            model.addAttribute("errorMessage", "Controlla i dati inseriti e riprova");
            
            // Ricarica documenti
            model.addAttribute("documents", documentService.getEmployeeDocuments(id));
            
            return "fleet/employees/form";
        }
        
        try {
            employee.setId(id);
            Employee updated = employeeService.update(id, employee);
            
            log.info("Dipendente aggiornato con successo: ID={}", id);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Dipendente aggiornato con successo");
            
            return "redirect:/fleet/employees/" + id + "/edit";
            
        } catch (DataIntegrityViolationException e) {
            log.error("Errore integrità dati durante aggiornamento dipendente ID: {}", id, e);
            handleDataIntegrityError(e, bindingResult, model);
            populateFormData(model);
            loadLocationData(model, employee);
            model.addAttribute("documents", documentService.getEmployeeDocuments(id));
            return "fleet/employees/form";
            
        } catch (Exception e) {
            log.error("Errore generico durante aggiornamento dipendente ID: {}", id, e);
            model.addAttribute("errorMessage", 
                "Si è verificato un errore durante l'aggiornamento. Riprova.");
            populateFormData(model);
            loadLocationData(model, employee);
            model.addAttribute("documents", documentService.getEmployeeDocuments(id));
            return "fleet/employees/form";
        }
    }
    

    /**
     * 6) Download di un documento personale:
     *    GET /fleet/employees/{id}/docs/{filename}
     */
    @GetMapping("/{id}/docs/{filename:.+}")
    public ResponseEntity<Resource> serveFile(
            @PathVariable Long id,
            @PathVariable String filename,
            HttpServletRequest request
    ) throws IOException {
       // Resource resource = documentService.loadEmployeeDocumentAsResource(id, "employees/" + id + "/docs/" + filename);
    	Resource resource = documentService.loadEmployeeDocumentAsResource(id, filename);
    	
        String contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
        if (contentType == null) {
            contentType = "application/octet-stream";
        }

        return ResponseEntity.ok()
                .contentType(MediaType.parseMediaType(contentType))
                .header(HttpHeaders.CONTENT_DISPOSITION,
                        "attachment; filename=\"" + resource.getFilename() + "\"")
                .body(resource);
    }
    
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes ra) {

        employeeService.delete(id);
        ra.addFlashAttribute("toastSuccess",
              "Dipendente eliminato correttamente");

        return "redirect:/fleet/employees/manage";
    }
    
    @GetMapping("/{empId}/docs/{docId}/delete")
    public String deleteEmployeeDocument(
            @PathVariable Long empId,
            @PathVariable Long docId,
            RedirectAttributes redirectAttrs) {

        documentService.deleteEmployeeDocument(empId, docId);
        redirectAttrs.addFlashAttribute("success", "Documento eliminato");
        return "redirect:/fleet/employees/" + empId + "/edit";
    }
    
// ========== METODI DI SUPPORTO PER VALIDAZIONE ==========
    
    /**
     * Validazioni business per nuovo dipendente
     */
    private void validateEmployeeBusinessRules(Employee employee, BindingResult bindingResult) {
        // Verifica unicità email
        if (employee.getEmail() != null && 
            employeeService.existsByEmail(employee.getEmail())) {
            bindingResult.rejectValue("email", "employee.email.duplicate", 
                "Email già utilizzata da un altro dipendente");
        }
        
        // Verifica unicità codice fiscale
        if (employee.getFiscalCode() != null && 
            employeeService.existsByFiscalCode(employee.getFiscalCode())) {
            bindingResult.rejectValue("fiscalCode", "employee.fiscalCode.duplicate", 
                "Codice fiscale già utilizzato da un altro dipendente");
        }
        
        // Validazioni indirizzo
        validateAddress(employee.getResidenceAddress(), bindingResult);
        
        // Validazione coerenza dati
        validateDataConsistency(employee, bindingResult);
    }
    
    /**
     * Validazioni business per aggiornamento (esclude check unicità per stesso record)
     */
    private void validateEmployeeBusinessRulesForUpdate(Long id, Employee employee, BindingResult bindingResult) {
        // Verifica unicità email (escludendo il dipendente corrente)
        if (employee.getEmail() != null) {
            Employee existing = employeeService.findByIdOrThrow(id);
            if (!employee.getEmail().equals(existing.getEmail()) && 
                employeeService.existsByEmail(employee.getEmail())) {
                bindingResult.rejectValue("email", "employee.email.duplicate", 
                    "Email già utilizzata da un altro dipendente");
            }
        }
        
        // Verifica unicità codice fiscale (escludendo il dipendente corrente)
        if (employee.getFiscalCode() != null) {
            Employee existing = employeeService.findByIdOrThrow(id);
            if (!employee.getFiscalCode().equals(existing.getFiscalCode()) && 
                employeeService.existsByFiscalCode(employee.getFiscalCode())) {
                bindingResult.rejectValue("fiscalCode", "employee.fiscalCode.duplicate", 
                    "Codice fiscale già utilizzato da un altro dipendente");
            }
        }
        
        // Altre validazioni
        validateAddress(employee.getResidenceAddress(), bindingResult);
        validateDataConsistency(employee, bindingResult);
    }
    
    /**
     * Validazione indirizzo
     */
    private void validateAddress(FullAddress address, BindingResult bindingResult) {
        if (address != null) {
            // CAP obbligatorio per indirizzi italiani
            if ("IT".equals(address.getCountryCode()) && 
                (address.getPostalCode() == null || address.getPostalCode().isBlank())) {
                bindingResult.rejectValue("residenceAddress.postalCode", 
                    "address.postalCode.required", 
                    "CAP obbligatorio per indirizzi italiani");
            }
            
            // Validazione formato CAP italiano
            if ("IT".equals(address.getCountryCode()) && 
                address.getPostalCode() != null && 
                !address.getPostalCode().matches("^[0-9]{5}$")) {
                bindingResult.rejectValue("residenceAddress.postalCode", 
                    "address.postalCode.format", 
                    "Il CAP deve essere di 5 cifre");
            }
        }
    }
    
    /**
     * Validazione coerenza dati
     */
    private void validateDataConsistency(Employee employee, BindingResult bindingResult) {
        // Verifica coerenza età con data nascita
        if (employee.getBirthDate() != null) {
            LocalDate now = LocalDate.now();
            if (employee.getBirthDate().isAfter(now)) {
                bindingResult.rejectValue("birthDate", "employee.birthDate.future", 
                    "La data di nascita non può essere nel futuro");
            }
            
            // Avviso per dipendenti molto giovani (non errore bloccante)
            int age = java.time.Period.between(employee.getBirthDate(), now).getYears();
            if (age < 18) {
               // log.warn("Dipendente minorenne: {} {} (età: {} anni)", employee.getFirstName(), employee.getLastName(), age);
            }
        }
    }
    
    /**
     * Raggruppa errori per campo
     */
    private Map<String, List<String>> groupErrorsByField(BindingResult bindingResult) {
        return bindingResult.getFieldErrors().stream()
            .collect(Collectors.groupingBy(
                FieldError::getField,
                Collectors.mapping(FieldError::getDefaultMessage, Collectors.toList())
            ));
    }
    
    /**
     * Gestione errori di integrità database
     */
    private void handleDataIntegrityError(DataIntegrityViolationException e, BindingResult bindingResult, Model model) {
        String message = e.getMessage().toLowerCase();
        
        if (message.contains("email")) {
            bindingResult.rejectValue("email", "employee.email.duplicate", 
                "Email già utilizzata da un altro dipendente");
        } else if (message.contains("fiscal_code") || message.contains("fiscalcode")) {
            bindingResult.rejectValue("fiscalCode", "employee.fiscalCode.duplicate", 
                "Codice fiscale già utilizzato da un altro dipendente");
        } else {
            model.addAttribute("errorMessage", 
                "Errore nel salvataggio: alcuni dati sono duplicati");
        }
        
        // Raggruppa errori dopo aver aggiunto quello dell'integrità
        Map<String, List<String>> errorsByField = groupErrorsByField(bindingResult);
        model.addAttribute("fieldErrors", errorsByField);
        model.addAttribute("hasErrors", true);
    }
    
    /**
     * Popola i dati di riferimento per il form
     */
    private void populateFormData(Model model) {
        model.addAttribute("maritalStatuses", MaritalStatus.values());
        model.addAttribute("educationLevels", EducationLevel.values());
        model.addAttribute("genders", Gender.values());
        model.addAttribute("employeeRoles", employeeRoleService.findAll());
        model.addAttribute("countries", countryService.getAll());
        model.addAttribute("regions", locationService.getRegions("IT"));
        model.addAttribute("provinces", List.of());
        model.addAttribute("cities", List.of());
        model.addAttribute("docTypes", EMPLOYEE_DOC_TYPES);
    }
    
    /**
     * Carica dati location specifici per l'employee
     */
    private void loadLocationData(Model model, Employee employee) {
        String cc = employee.getResidenceAddress() != null ? 
                   employee.getResidenceAddress().getCountryCode() : null;
        if (cc != null) {
            model.addAttribute("regions", locationService.getRegions(cc));
            String rc = employee.getResidenceAddress().getRegionCode();
            if (rc != null) {
                model.addAttribute("provinces", locationService.getProvinces(rc));
                String pc = employee.getResidenceAddress().getProvinceCode();
                if (pc != null) {
                    model.addAttribute("cities", locationService.getCities(pc));
                }
            }
        }
    }


}