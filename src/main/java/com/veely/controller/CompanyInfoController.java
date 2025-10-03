package com.veely.controller;

import com.veely.entity.CompanyInfo;
import com.veely.service.CompanyInfoService;
import com.veely.service.DocumentService;

import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.core.io.Resource;
import org.springframework.core.io.UrlResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.mvc.support.RedirectAttributes;
import org.springframework.util.AntPathMatcher;
import org.springframework.web.servlet.HandlerMapping;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/settings/company")
@RequiredArgsConstructor
@Slf4j
public class CompanyInfoController {

    private final CompanyInfoService companyInfoService;
    private final DocumentService documentService;

    /**
     * Lista delle configurazioni aziendali
     */
    @GetMapping
    public String list(Model model) {
        List<CompanyInfo> companies = companyInfoService.findAll();
        Optional<CompanyInfo> primary = companyInfoService.getPrimaryCompanyInfoOptional();
        
        model.addAttribute("companies", companies);
        model.addAttribute("primaryCompany", primary.orElse(null));
        model.addAttribute("hasCompanyInfo", companyInfoService.hasCompanyInfo());
        
        return "settings/company/index";
    }

    /**
     * Form per nuova configurazione aziendale
     */
    @GetMapping("/new")
    public String newForm(Model model) {
        CompanyInfo companyInfo = new CompanyInfo();
        
        // Valori di default
        companyInfo.setLegalCountry("Italia");
        companyInfo.setOperationalCountry("Italia");
        companyInfo.setPrimaryColor("#667eea");
        companyInfo.setSecondaryColor("#764ba2");
        companyInfo.setShowLogoInDocuments(true);
        companyInfo.setShowAddressInDocuments(true);
        companyInfo.setShowContactsInDocuments(true);
        companyInfo.setShowTaxInfoInDocuments(true);
        companyInfo.setUseCustomEmailSignature(false);
        
        model.addAttribute("companyInfo", companyInfo);
        model.addAttribute("isEdit", false);
        
        return "settings/company/form";
    }

    /**
     * Salvataggio nuova configurazione
     */
    @PostMapping("/new")
    public String create(@Valid @ModelAttribute("companyInfo") CompanyInfo companyInfo, 
                        BindingResult bindingResult,
                        RedirectAttributes redirectAttributes,
                        Model model) {
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", false);
            return "settings/company/form";
        }

        try {
            CompanyInfo saved = companyInfoService.create(companyInfo);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Configurazione aziendale creata con successo!");
            return "redirect:/settings/company/" + saved.getId() + "/edit";
        } catch (Exception e) {
            log.error("Errore nella creazione della configurazione aziendale", e);
            model.addAttribute("errorMessage", "Errore nella creazione: " + e.getMessage());
            model.addAttribute("isEdit", false);
            return "settings/company/form";
        }
    }

    /**
     * Form per modifica configurazione esistente
     */
    @GetMapping("/{id}/edit")
    public String editForm(@PathVariable Long id, Model model) {
        CompanyInfo companyInfo = companyInfoService.findById(id);
        
        model.addAttribute("companyInfo", companyInfo);
        model.addAttribute("isEdit", true);
        model.addAttribute("hasLogo", companyInfo.hasLogo());
        model.addAttribute("hasHeaderLogo", companyInfo.hasHeaderLogo());
        model.addAttribute("hasWatermark", companyInfo.getWatermarkPath() != null);
        
        return "settings/company/form";
    }

    /**
     * Aggiornamento configurazione esistente
     */
    @PostMapping("/{id}/edit")
    public String update(@PathVariable Long id,
                        @Valid @ModelAttribute("companyInfo") CompanyInfo companyInfo,
                        BindingResult bindingResult,
                        RedirectAttributes redirectAttributes,
                        Model model) {
        
        if (bindingResult.hasErrors()) {
            model.addAttribute("isEdit", true);
            CompanyInfo existing = companyInfoService.findById(id);
            model.addAttribute("hasLogo", existing.hasLogo());
            model.addAttribute("hasHeaderLogo", existing.hasHeaderLogo());
            model.addAttribute("hasWatermark", existing.getWatermarkPath() != null);
            return "settings/company/form";
        }

        try {
            CompanyInfo updated = companyInfoService.update(id, companyInfo);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Configurazione aziendale aggiornata con successo!");
            return "redirect:/settings/company/" + updated.getId() + "/edit";
        } catch (Exception e) {
            log.error("Errore nell'aggiornamento della configurazione aziendale", e);
            model.addAttribute("errorMessage", "Errore nell'aggiornamento: " + e.getMessage());
            model.addAttribute("isEdit", true);
            return "settings/company/form";
        }
    }

    /**
     * Eliminazione configurazione
     */
    @PostMapping("/{id}/delete")
    public String delete(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            companyInfoService.delete(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Configurazione aziendale eliminata con successo!");
        } catch (Exception e) {
            log.error("Errore nell'eliminazione della configurazione aziendale", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Errore nell'eliminazione: " + e.getMessage());
        }
        
        return "redirect:/settings/company";
    }

    /**
     * Attivazione configurazione
     */
    @PostMapping("/{id}/activate")
    public String activate(@PathVariable Long id, RedirectAttributes redirectAttributes) {
        try {
            companyInfoService.activate(id);
            redirectAttributes.addFlashAttribute("successMessage", 
                "Configurazione aziendale attivata con successo!");
        } catch (Exception e) {
            log.error("Errore nell'attivazione della configurazione aziendale", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Errore nell'attivazione: " + e.getMessage());
        }
        
        return "redirect:/settings/company";
    }

   

    /**
     * Upload logo per intestazioni
     */
    @PostMapping("/{id}/upload-header-logo")
    public String uploadHeaderLogo(@PathVariable Long id,
                                  @RequestParam("headerLogoFile") MultipartFile file,
                                  RedirectAttributes redirectAttributes) {
        
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Seleziona un file per il logo intestazione!");
            return "redirect:/settings/company/" + id + "/edit";
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/"))) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Il file deve essere un'immagine (PNG, JPG, GIF, SVG)!");
            return "redirect:/settings/company/" + id + "/edit";
        }

        try {
            companyInfoService.uploadHeaderLogo(id, file);
            redirectAttributes.addFlashAttribute("successMessage", "Logo intestazione caricato con successo!");
        } catch (IOException e) {
            log.error("Errore nel caricamento del logo intestazione", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Errore nel caricamento: " + e.getMessage());
        }

        return "redirect:/settings/company/" + id + "/edit";
    }

    /**
     * Upload watermark
     */
    @PostMapping("/{id}/upload-watermark")
    public String uploadWatermark(@PathVariable Long id,
                                 @RequestParam("watermarkFile") MultipartFile file,
                                 RedirectAttributes redirectAttributes) {
        
        if (file.isEmpty()) {
            redirectAttributes.addFlashAttribute("errorMessage", "Seleziona un file per il watermark!");
            return "redirect:/settings/company/" + id + "/edit";
        }

        String contentType = file.getContentType();
        if (contentType == null || (!contentType.startsWith("image/"))) {
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Il file deve essere un'immagine (PNG, JPG, GIF, SVG)!");
            return "redirect:/settings/company/" + id + "/edit";
        }

        try {
            companyInfoService.uploadWatermark(id, file);
            redirectAttributes.addFlashAttribute("successMessage", "Watermark caricato con successo!");
        } catch (IOException e) {
            log.error("Errore nel caricamento del watermark", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Errore nel caricamento: " + e.getMessage());
        }

        return "redirect:/settings/company/" + id + "/edit";
    }

    /**
     * Download file (logo, watermark, ecc.)
     */
    @GetMapping("/files/**")
    public ResponseEntity<Resource> downloadFile(HttpServletRequest request) {
        try {
            // Estrae il percorso relativo richiesto dopo /files/
            String bestMatchPattern = (String) request.getAttribute(HandlerMapping.BEST_MATCHING_PATTERN_ATTRIBUTE);
            String pathWithinHandler = (String) request.getAttribute(HandlerMapping.PATH_WITHIN_HANDLER_MAPPING_ATTRIBUTE);
            String relativePath = new AntPathMatcher().extractPathWithinPattern(bestMatchPattern, pathWithinHandler);

            log.info("Richiesta download file: {}", relativePath);

            // Costruisci il path completo
            Path uploadsDir = Paths.get("uploads").toAbsolutePath().normalize();
            Path filePath = uploadsDir.resolve(relativePath).normalize();
            
            log.info("Path uploads: {}", uploadsDir);
            log.info("Path file richiesto: {}", filePath);
            
            // Verifica che il file sia nella directory uploads per sicurezza
            if (!filePath.startsWith(uploadsDir)) {
                log.warn("Tentativo di accesso a file fuori dalla directory uploads. Uploads: {}, File: {}", 
                         uploadsDir, filePath);
                return ResponseEntity.notFound().build();
            }
            
            Resource resource = new UrlResource(filePath.toUri());
            
            if (!resource.exists()) {
                log.warn("File non trovato: {}", filePath);
                
                // Debug: lista il contenuto della directory
                Path parentDir = filePath.getParent();
                if (Files.exists(parentDir)) {
                    log.info("Contenuto directory {}:", parentDir);
                    try {
                        Files.list(parentDir).forEach(file -> 
                            log.info("  - {}", file.getFileName()));
                    } catch (IOException e) {
                        log.warn("Errore nella lettura directory", e);
                    }
                } else {
                    log.warn("Directory padre non esiste: {}", parentDir);
                }
                
                return ResponseEntity.notFound().build();
            }

            return buildFileResponse(resource, request);
                        
        } catch (Exception ex) {
            log.error("Errore nel download del file", ex);
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Anteprima configurazione aziendale
     */
    @GetMapping("/{id}/preview")
    public String preview(@PathVariable Long id, Model model) {
        CompanyInfo companyInfo = companyInfoService.findById(id);
        model.addAttribute("companyInfo", companyInfo);
        return "settings/company/preview";
    }

    /**
     * API JSON per ottenere le informazioni aziendali (per AJAX)
     */
    @GetMapping("/api/primary")
    @ResponseBody
    public ResponseEntity<CompanyInfo> getPrimaryCompanyInfoApi() {
        Optional<CompanyInfo> companyInfo = companyInfoService.getPrimaryCompanyInfoOptional();
        
        if (companyInfo.isPresent()) {
            return ResponseEntity.ok(companyInfo.get());
        } else {
            return ResponseEntity.notFound().build();
        }
    }

    /**
     * Test configurazione email
     */
    @PostMapping("/{id}/test-email")
    public String testEmailConfiguration(@PathVariable Long id,
                                        @RequestParam("testEmail") String testEmail,
                                        RedirectAttributes redirectAttributes) {
        
        try {
            // Qui potresti implementare l'invio di un'email di test
            // EmailService.sendTestEmail(testEmail, companyInfo);
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Email di test inviata con successo a: " + testEmail);
        } catch (Exception e) {
            log.error("Errore nell'invio email di test", e);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Errore nell'invio email di test: " + e.getMessage());
        }

        return "redirect:/settings/company/" + id + "/edit";
    }
    
    /**
     * Debug endpoint per verificare lo stato dei file
     */
    @GetMapping("/{id}/debug")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> debugFileStatus(@PathVariable Long id) {
        try {
            CompanyInfo company = companyInfoService.findById(id);
            Map<String, Object> debugInfo = new HashMap<>();
            
            debugInfo.put("companyId", id);
            debugInfo.put("companyName", company.getDisplayName());
            debugInfo.put("logoPathInDB", company.getLogoPath());
            debugInfo.put("hasLogo", company.hasLogo());
            debugInfo.put("workingDirectory", System.getProperty("user.dir"));
            
            // Verifica esistenza file
            List<Map<String, Object>> pathTests = new ArrayList<>();
            
            if (company.getLogoPath() != null) {
                String[] possiblePaths = {
                	"uploads/" + company.getLogoPath(),
                    company.getLogoPath(),
                    Paths.get("uploads", company.getLogoPath()).toString()
                };
                
                for (String path : possiblePaths) {
                    Map<String, Object> pathTest = new HashMap<>();
                    Path fullPath = Paths.get(path);
                    boolean exists = Files.exists(fullPath);
                    
                    pathTest.put("path", path);
                    pathTest.put("exists", exists);
                    pathTest.put("absolutePath", fullPath.toAbsolutePath().toString());
                    
                    if (exists) {
                        try {
                            pathTest.put("size", Files.size(fullPath));
                            pathTest.put("lastModified", Files.getLastModifiedTime(fullPath).toString());
                        } catch (IOException e) {
                            pathTest.put("error", e.getMessage());
                        }
                    }
                    
                    pathTests.add(pathTest);
                }
            }
            
            debugInfo.put("pathTests", pathTests);
            
            // Lista contenuto directory
            try {
        	Path uploadsDir = Paths.get("uploads", "company_logos");
                if (Files.exists(uploadsDir)) {
                    List<String> files = Files.list(uploadsDir)
                        .map(path -> path.getFileName().toString())
                        .collect(Collectors.toList());
                    debugInfo.put("logoDirectoryContents", files);
                } else {
                    debugInfo.put("logoDirectoryContents", "Directory non esiste");
                }
            } catch (IOException e) {
                debugInfo.put("logoDirectoryError", e.getMessage());
            }
            
            // Log le informazioni
            companyInfoService.debugFileStatus(id);
            
            return ResponseEntity.ok(debugInfo);
            
        } catch (Exception e) {
            log.error("Errore nel debug", e);
            Map<String, Object> error = new HashMap<>();
            error.put("error", e.getMessage());
            return ResponseEntity.status(500).body(error);
        }
    }

    /**
     * Upload logo con redirect migliorato e feedback
     */
    /**
     * Upload logo con debug completo - SOSTITUISCI nel CompanyInfoController
     */
    @PostMapping("/{id}/upload-logo")
    public String uploadLogo(@PathVariable Long id,
                            @RequestParam("logoFile") MultipartFile file,
                            RedirectAttributes redirectAttributes) {
        
        log.info("=== INIZIO UPLOAD LOGO ===");
        log.info("Company ID: {}", id);
        log.info("File ricevuto: {}", file != null ? file.getOriginalFilename() : "NULL");
        log.info("File vuoto: {}", file != null ? file.isEmpty() : "FILE NULL");
        log.info("Dimensione file: {}", file != null ? file.getSize() : "FILE NULL");
        log.info("Content type: {}", file != null ? file.getContentType() : "FILE NULL");
        
        if (file == null) {
            log.error("File è NULL!");
            redirectAttributes.addFlashAttribute("errorMessage", "Nessun file ricevuto!");
            return "redirect:/settings/company/" + id + "/edit";
        }
        
        if (file.isEmpty()) {
            log.error("File è vuoto!");
            redirectAttributes.addFlashAttribute("errorMessage", "Seleziona un file per il logo!");
            return "redirect:/settings/company/" + id + "/edit";
        }

        // Validazione tipo file più dettagliata
        String contentType = file.getContentType();
        String originalFilename = file.getOriginalFilename();
        
        log.info("Validazione file - Nome: {}, Tipo: {}, Dimensione: {}", 
                 originalFilename, contentType, file.getSize());
        
        if (contentType == null || (!contentType.startsWith("image/"))) {
            log.error("Tipo file non valido: {}", contentType);
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Il file deve essere un'immagine (PNG, JPG, GIF, SVG)! Tipo ricevuto: " + contentType);
            return "redirect:/settings/company/" + id + "/edit";
        }
        
        // Validazione dimensione file
        if (file.getSize() > 5 * 1024 * 1024) { // 5MB
            log.error("File troppo grande: {} bytes", file.getSize());
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Il file è troppo grande! Massimo 5MB. Dimensione: " + (file.getSize() / 1024 / 1024) + "MB");
            return "redirect:/settings/company/" + id + "/edit";
        }

        try {
            log.info("Chiamata a companyInfoService.uploadLogo()...");
            
            CompanyInfo updated = companyInfoService.uploadLogo(id, file);
            
            log.info("Upload completato. CompanyInfo aggiornato: {}", updated.getId());
            log.info("Nuovo logo path: {}", updated.getLogoPath());
            
            redirectAttributes.addFlashAttribute("successMessage", 
                "Logo caricato con successo! File: " + originalFilename);
            
            // Aggiungi parametro per refreshare la pagina
            redirectAttributes.addAttribute("uploaded", "logo");
            
            log.info("=== UPLOAD LOGO COMPLETATO ===");
            
        } catch (Exception e) {
            log.error("=== ERRORE UPLOAD LOGO ===", e);
            log.error("Errore nel caricamento del logo per company {}: {}", id, e.getMessage());
            log.error("Stack trace completo:", e);
            
            redirectAttributes.addFlashAttribute("errorMessage", 
                "Errore nel caricamento del logo: " + e.getMessage());
        }

        return "redirect:/settings/company/" + id + "/edit";
    }

    /**
     * Test del logo nel PDF
     */
    @GetMapping("/{id}/test-logo")
    @ResponseBody
    public ResponseEntity<Map<String, Object>> testLogoInPdf(@PathVariable Long id) {
        try {
            CompanyInfo company = companyInfoService.findById(id);
            Map<String, Object> result = new HashMap<>();
            
            result.put("companyId", id);
            result.put("hasLogo", company.hasLogo());
            result.put("logoPath", company.getLogoPath());
            
            if (company.hasLogo()) {
                // Prova a caricare il logo come farebbe il PDF
                try {
                    String logoPath = Paths.get("uploads", company.getLogoPath()).toString();
                    Path fullPath = Paths.get(logoPath);
                    
                    result.put("logoPathUsedInPdf", logoPath);
                    result.put("logoExistsForPdf", Files.exists(fullPath));
                    result.put("logoAbsolutePathForPdf", fullPath.toAbsolutePath().toString());
                    
                    if (Files.exists(fullPath)) {
                        result.put("logoSizeForPdf", Files.size(fullPath));
                        
                        // Prova a caricare come Image
                        try {
                            com.lowagie.text.Image logo = com.lowagie.text.Image.getInstance(logoPath);
                            result.put("logoLoadedSuccessfully", true);
                            result.put("logoWidth", logo.getWidth());
                            result.put("logoHeight", logo.getHeight());
                        } catch (Exception e) {
                            result.put("logoLoadedSuccessfully", false);
                            result.put("logoLoadError", e.getMessage());
                        }
                    } else {
                        result.put("logoLoadedSuccessfully", false);
                        result.put("logoLoadError", "File non trovato");
                    }
                    
                } catch (Exception e) {
                    result.put("error", e.getMessage());
                }
            }
            
            return ResponseEntity.ok(result);
            
        } catch (Exception e) {
            log.error("Errore nel test logo", e);
            return ResponseEntity.status(500).body(Map.of("error", e.getMessage()));
        }
    }
    /**
     * Costruisce la risposta HTTP per il download del file
     */
    private ResponseEntity<Resource> buildFileResponse(Resource resource, HttpServletRequest request) {
	    try {
	        // Determina il content type
	        String contentType = null;
	        try {
	            contentType = request.getServletContext().getMimeType(resource.getFile().getAbsolutePath());
	        } catch (IOException ex) {
	            log.info("Could not determine file type.");
	        }

	        if (contentType == null) {
	            contentType = "application/octet-stream";
	        }

	        log.info("Serving file: {} with content type: {}", resource.getFilename(), contentType);

	        return ResponseEntity.ok()
	                .contentType(MediaType.parseMediaType(contentType))
	                .header(HttpHeaders.CONTENT_DISPOSITION,
	                        "inline; filename=\"" + resource.getFilename() + "\"")
	                .body(resource);
	    } catch (Exception e) {
	        log.error("Errore nella costruzione della risposta file", e);
	        return ResponseEntity.notFound().build();
	    }
	}
}