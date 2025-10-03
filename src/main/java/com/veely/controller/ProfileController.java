package com.veely.controller;

import com.veely.entity.Document;
import com.veely.entity.Employee;
import com.veely.model.DocumentType;
import com.veely.model.EducationLevel;
import com.veely.model.FullAddress;
import com.veely.model.Gender;
import com.veely.model.MaritalStatus;
import com.veely.service.EmployeeService;
import com.veely.service.LocationService;
import com.veely.service.CountryService.CountryDto;
import com.veely.service.CountryService;
import com.veely.service.DocumentService;
import com.veely.service.EmployeeRoleService;
import com.veely.service.LocationService.CityDto;
import com.veely.service.LocationService.ProvinceDto;
import com.veely.service.LocationService.RegionDto;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.time.LocalDate;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

import org.springframework.core.io.Resource;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.ModelAttribute;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;


@Slf4j
@Controller
@RequestMapping("/profile")
@RequiredArgsConstructor
public class ProfileController {

    private final EmployeeService employeeService;
    private final DocumentService documentService;
    private final CountryService countryService;
    private final LocationService locationService;
    private final EmployeeRoleService employeeRoleService;

    @GetMapping
    public String profile(Model model, Authentication authentication) {
        String email = authentication.getName();
        Employee employee = employeeService.findByEmail(email);
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_Administrator"));
        model.addAttribute("employee", employee);
        model.addAttribute("isAdmin", isAdmin);
        
        
        
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
        
        List<Document> documents = documentService.getEmployeeDocuments(employee.getId());
        
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
        
        return "profile/index";
    }
    
    
    @PostMapping("/docs")
    public String uploadDoc(
    		Authentication authentication,
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
        Employee employee = employeeService.findByEmail(authentication.getName());
        Long id = employee.getId();
        documentService.uploadEmployeeDocument(id, file, type, issued, exp);
        ra.addFlashAttribute("success", "Documento caricato");
        return "redirect:/profile?tab=docs";
    }

    /**
     * 5) Gestione POST “step 2”: aggiorna anagrafica, salva eventuale file,
     *    e rimane sul medesimo form (per caricare altri documenti o uscire).
     */
    @PostMapping("/edit")
    public String update(
                        @Valid @ModelAttribute("employee") Employee employee,
                        BindingResult bindingResult,
                        Model model,
                        RedirectAttributes redirectAttributes,
                        Authentication authentication) {
    	String email = authentication.getName();
        Employee logged = employeeService.findByEmail(email);
        Long id = logged.getId();
        log.info("Tentativo aggiornamento dipendente ID: {}", id);
        
        // Validazione business rules (escludendo i check di unicità per lo stesso record)
        validateEmployeeBusinessRulesForUpdate(id, employee, bindingResult);
        
        boolean isAdmin = authentication.getAuthorities().stream()
                .anyMatch(a -> a.getAuthority().equals("ROLE_Administrator"));
        if (!isAdmin) {
            employee.setRoles(logged.getRoles());
        }
        
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
            
            return "profile/index";
        }
        
        try {
            employee.setId(id);
            Employee updated = employeeService.update(id, employee);
            
            log.info("Dipendente aggiornato con successo: ID={}", id);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Dipendente aggiornato con successo");
            
            return "profile/index";
            
        } catch (DataIntegrityViolationException e) {
            log.error("Errore integrità dati durante aggiornamento dipendente ID: {}", id, e);
            handleDataIntegrityError(e, bindingResult, model);
            populateFormData(model);
            loadLocationData(model, employee);
            model.addAttribute("documents", documentService.getEmployeeDocuments(id));
            return "profile/index";
            
        } catch (Exception e) {
            log.error("Errore generico durante aggiornamento dipendente ID: {}", id, e);
            model.addAttribute("errorMessage", 
                "Si è verificato un errore durante l'aggiornamento. Riprova.");
            populateFormData(model);
            loadLocationData(model, employee);
            model.addAttribute("documents", documentService.getEmployeeDocuments(id));
            return "profile/index";
        }
    }
    

    /**
     * 6) Download di un documento personale:
     *    GET /fleet/employees/{id}/docs/{filename}
     */
    @GetMapping("/docs/{filename:.+}")
    public ResponseEntity<Resource> serveFile(
    		Authentication authentication,
            @PathVariable String filename,
            HttpServletRequest request
    ) throws IOException {
    	Employee emp = employeeService.findByEmail(authentication.getName());
        Resource resource = documentService.loadEmployeeDocumentAsResource(emp.getId(), filename);
    	
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
    
    @GetMapping("/docs/{docId}/delete")
    public String deleteEmployeeDocument(
            @PathVariable Long docId,
            Authentication authentication,
            RedirectAttributes redirectAttrs) {
        Employee emp = employeeService.findByEmail(authentication.getName());
        documentService.deleteEmployeeDocument(emp.getId(), docId);
        redirectAttrs.addFlashAttribute("success", "Documento eliminato");
        return "redirect:/profile?tab=docs";
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
        model.addAttribute("docTypes", DocumentType.values());
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
