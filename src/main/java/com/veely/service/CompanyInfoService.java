package com.veely.service;

import com.veely.entity.CompanyInfo;
import com.veely.entity.Document;
import com.veely.exception.ResourceNotFoundException;
import com.veely.model.DocumentType;
import com.veely.repository.CompanyInfoRepository;
import com.veely.service.DocumentService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.beans.BeanUtils;
import org.springframework.beans.BeanWrapper;
import org.springframework.beans.BeanWrapperImpl;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDate;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import java.beans.PropertyDescriptor;
import java.util.HashSet;
import java.util.Set;

@Service
@RequiredArgsConstructor
@Transactional
@Slf4j
public class CompanyInfoService {

    private final CompanyInfoRepository companyInfoRepository;
    private final DocumentService documentService;
    
    // Cartella per il salvataggio dei file
    private static final String UPLOAD_DIR = "uploads/";
    private static final String LOGO_SUBDIR = "company/logos/";
    private static final String DOCS_SUBDIR = "company/documents/";

    /**
     * Ottiene le informazioni aziendali principali
     */
    @Transactional(readOnly = true)
    public CompanyInfo getPrimaryCompanyInfo() {
        return companyInfoRepository.findPrimaryCompanyInfo()
                .orElseThrow(() -> new ResourceNotFoundException("Nessuna configurazione aziendale trovata"));
    }

    /**
     * Ottiene le informazioni aziendali se esistono, altrimenti ritorna null
     */
    @Transactional(readOnly = true)
    public Optional<CompanyInfo> getPrimaryCompanyInfoOptional() {
        return companyInfoRepository.findPrimaryCompanyInfo();
    }

    /**
     * Ottiene tutte le configurazioni aziendali
     */
    @Transactional(readOnly = true)
    public List<CompanyInfo> findAll() {
        return companyInfoRepository.findAll();
    }

    /**
     * Ottiene una configurazione per ID
     */
    @Transactional(readOnly = true)
    public CompanyInfo findById(Long id) {
        return companyInfoRepository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Configurazione aziendale non trovata: " + id));
    }

    /**
     * Verifica se esiste una configurazione aziendale
     */
    @Transactional(readOnly = true)
    public boolean hasCompanyInfo() {
        return companyInfoRepository.existsActiveCompanyInfo();
    }

    /**
     * Crea una nuova configurazione aziendale
     */
    public CompanyInfo create(CompanyInfo companyInfo) {
        // Se è la prima configurazione, la rende attiva
        if (!hasCompanyInfo()) {
            companyInfo.setIsActive(true);
        }
        
        companyInfo.setLastUpdated(LocalDate.now());
        
        // Inizializza i valori di default
        if (companyInfo.getShowLogoInDocuments() == null) {
            companyInfo.setShowLogoInDocuments(true);
        }
        if (companyInfo.getShowAddressInDocuments() == null) {
            companyInfo.setShowAddressInDocuments(true);
        }
        if (companyInfo.getShowContactsInDocuments() == null) {
            companyInfo.setShowContactsInDocuments(true);
        }
        if (companyInfo.getShowTaxInfoInDocuments() == null) {
            companyInfo.setShowTaxInfoInDocuments(true);
        }
        if (companyInfo.getUseCustomEmailSignature() == null) {
            companyInfo.setUseCustomEmailSignature(false);
        }

        return companyInfoRepository.save(companyInfo);
    }

    /**
     * Aggiorna una configurazione esistente
     */
    public CompanyInfo update(Long id, CompanyInfo updatedInfo) {
        CompanyInfo existing = findById(id);
        
        // Copia solo i campi non null per evitare di sovrascrivere valori esistenti
        org.springframework.beans.BeanUtils.copyProperties(
            updatedInfo,
            existing,
            getNullPropertyNames(updatedInfo)
        );
        existing.setLastUpdated(LocalDate.now());

        return companyInfoRepository.save(existing);
    }

    /**
     * Elimina una configurazione
     */
    public void delete(Long id) {
        CompanyInfo companyInfo = findById(id);
        
        // Elimina i file associati
        deleteAssociatedFiles(companyInfo);
        
        companyInfoRepository.delete(companyInfo);
    }

    /**
     * Attiva una configurazione specifica
     */
    public CompanyInfo activate(Long id) {
        // Disattiva tutte le altre configurazioni
        List<CompanyInfo> allConfigs = findAll();
        for (CompanyInfo config : allConfigs) {
            config.setIsActive(false);
        }
        companyInfoRepository.saveAll(allConfigs);

        // Attiva la configurazione selezionata
        CompanyInfo target = findById(id);
        target.setIsActive(true);
        target.setLastUpdated(LocalDate.now());
        
        return companyInfoRepository.save(target);
    }

    /**
     * Upload del logo aziendale utilizzando il sistema Document esistente
    public CompanyInfo uploadLogo(Long companyId, MultipartFile file) throws IOException {
        CompanyInfo company = findById(companyId);
        
        log.info("Inizio upload logo per company ID: {}", companyId);
        log.info("File ricevuto - Nome: {}, Dimensione: {}, Tipo: {}", 
                 file.getOriginalFilename(), file.getSize(), file.getContentType());
        
        try {
            // Elimina il vecchio documento logo se esiste
            Optional<Document> existingLogo = documentService.getCompanyLogo(companyId);
            if (existingLogo.isPresent()) {
                log.info("Eliminazione vecchio logo documento ID: {}", existingLogo.get().getId());
                documentService.deleteDocument(existingLogo.get().getId());
            }
            
            // Crea nuovo documento logo usando il sistema esistente
            Document logoDocument = documentService.uploadCompanyDocument(
                companyId,
                file,
                DocumentType.COMPANY_LOGO,
                LocalDate.now(),
                null // Nessuna scadenza per i loghi
            );
            
            log.info("Logo documento creato con ID: {} e path: {}", logoDocument.getId(), logoDocument.getPath());
            
            // Aggiorna il path nel CompanyInfo
            company.setLogoPath(logoDocument.getPath());
            company.setLastUpdated(LocalDate.now());
            
            CompanyInfo saved = companyInfoRepository.save(company);
            log.info("Logo aggiornato in database. Nuovo path: {}", saved.getLogoPath());
            
            return saved;
            
        } catch (Exception e) {
            log.error("Errore durante l'upload del logo", e);
            throw new IOException("Errore nel salvataggio del logo: " + e.getMessage(), e);
        }
    }
     */

    /**
     * Upload del logo per intestazioni
     */
    public CompanyInfo uploadHeaderLogo(Long companyId, MultipartFile file) throws IOException {
        CompanyInfo company = findById(companyId);
        
        String fileName = saveFile(file, LOGO_SUBDIR, "header_logo_");
        
        // Elimina il vecchio logo se esiste
        if (company.getHeaderLogoPath() != null) {
            deleteFile(company.getHeaderLogoPath());
        }
        
        company.setHeaderLogoPath(fileName);
        company.setLastUpdated(LocalDate.now());
        
        return companyInfoRepository.save(company);
    }

    /**
     * Upload del watermark
     */
    public CompanyInfo uploadWatermark(Long companyId, MultipartFile file) throws IOException {
        CompanyInfo company = findById(companyId);
        
        String fileName = saveFile(file, LOGO_SUBDIR, "watermark_");
        
        // Elimina il vecchio watermark se esiste
        if (company.getWatermarkPath() != null) {
            deleteFile(company.getWatermarkPath());
        }
        
        company.setWatermarkPath(fileName);
        company.setLastUpdated(LocalDate.now());
        
        return companyInfoRepository.save(company);
    }

    /**
     * Ottiene il percorso completo di un file
     */
    public String getFullFilePath(String fileName) {
        if (fileName == null || fileName.trim().isEmpty()) {
            return null;
        }
        return UPLOAD_DIR + fileName;
    }

    /**
     * Verifica se un file esiste
     */
    public boolean fileExists(String fileName) {
        if (fileName == null) return false;
        Path filePath = Paths.get(UPLOAD_DIR + fileName);
        return Files.exists(filePath);
    }

    // === METODI PRIVATI ===

    private void deleteFile(String relativePath) {
        try {
            Path filePath = Paths.get(UPLOAD_DIR + relativePath);
            if (Files.exists(filePath)) {
                Files.delete(filePath);
                log.info("File eliminato: {}", filePath.toString());
            }
        } catch (IOException e) {
            log.error("Errore nell'eliminazione del file: {}", relativePath, e);
        }
    }

    private void deleteAssociatedFiles(CompanyInfo companyInfo) {
        if (companyInfo.getLogoPath() != null) {
            deleteFile(companyInfo.getLogoPath());
        }
        if (companyInfo.getHeaderLogoPath() != null) {
            deleteFile(companyInfo.getHeaderLogoPath());
        }
        if (companyInfo.getWatermarkPath() != null) {
            deleteFile(companyInfo.getWatermarkPath());
        }
    }

    private String[] getNullPropertyNames(CompanyInfo source) {
        BeanWrapper src = new BeanWrapperImpl(source);
        PropertyDescriptor[] pds = src.getPropertyDescriptors();
        Set<String> emptyNames = new HashSet<>();
        for (PropertyDescriptor pd : pds) {
            Object srcValue = src.getPropertyValue(pd.getName());
            if (srcValue == null) {
                emptyNames.add(pd.getName());
            }
        }
        return emptyNames.toArray(new String[0]);
    }

    /**
     * Metodo per verificare lo stato dei file
     */
    public void debugFileStatus(Long companyId) {
        CompanyInfo company = findById(companyId);
        
        log.info("=== DEBUG FILE STATUS ===");
        log.info("Company ID: {}", companyId);
        log.info("Logo path in DB: {}", company.getLogoPath());
        
        if (company.getLogoPath() != null) {
            // Prova tutti i possibili percorsi
            String[] possiblePaths = {
                UPLOAD_DIR + company.getLogoPath(),
                "uploads/company/" + company.getLogoPath(),
                company.getLogoPath(),
                Paths.get("uploads", company.getLogoPath()).toString(),
                Paths.get(UPLOAD_DIR, company.getLogoPath()).toString()
            };
            
            for (String path : possiblePaths) {
                Path fullPath = Paths.get(path);
                boolean exists = Files.exists(fullPath);
                log.info("Path: {} - Esiste: {} - Assoluto: {}", 
                         path, exists, fullPath.toAbsolutePath());
            }
            
            // Working directory
            log.info("Working directory: {}", System.getProperty("user.dir"));
            
            // Lista contenuto directory uploads
            try {
                Path uploadsDir = Paths.get("uploads", "company_logos");
                if (Files.exists(uploadsDir)) {
                    log.info("Contenuto directory logos:");
                    Files.list(uploadsDir).forEach(file -> 
                        log.info("  - {}", file.getFileName()));
                } else {
                    log.warn("Directory logos non esiste: {}", uploadsDir.toAbsolutePath());
                }
            } catch (IOException e) {
                log.error("Errore nella lettura directory: {}", e.getMessage());
            }
        }
        log.info("=== END DEBUG ===");
    }

    private String saveFile(MultipartFile file, String subDirectory, String prefix) throws IOException {
        // Crea le directory se non esistono
        Path uploadPath = Paths.get(UPLOAD_DIR + subDirectory);
        if (!Files.exists(uploadPath)) {
            Files.createDirectories(uploadPath);
            log.info("Creata directory: {}", uploadPath.toAbsolutePath());
        }

        // Genera nome file unico
        String originalFileName = file.getOriginalFilename();
        String extension = originalFileName != null && originalFileName.contains(".") 
            ? originalFileName.substring(originalFileName.lastIndexOf(".")) 
            : "";
        String fileName = prefix + UUID.randomUUID().toString() + extension;
        String relativePath = subDirectory + fileName;

        // Salva il file
        Path filePath = uploadPath.resolve(fileName);
        Files.copy(file.getInputStream(), filePath, StandardCopyOption.REPLACE_EXISTING);

        log.info("File salvato: {} (path assoluto: {})", relativePath, filePath.toAbsolutePath());
        
        // Verifica immediata
        if (Files.exists(filePath)) {
            log.info("Verifica immediata: File salvato correttamente");
            log.info("Dimensione file: {} bytes", Files.size(filePath));
        } else {
            log.error("ERRORE: File non trovato dopo il salvataggio!");
        }
        
        return relativePath;
    }
    
    /**
     * Upload del logo aziendale - SOSTITUISCI questo metodo nel CompanyInfoService
     */
    public CompanyInfo uploadLogo(Long companyId, MultipartFile file) throws IOException {
        CompanyInfo company = findById(companyId);
        
        log.info("Inizio upload logo per company ID: {}", companyId);
        log.info("File ricevuto - Nome: {}, Dimensione: {}, Tipo: {}", 
                 file.getOriginalFilename(), file.getSize(), file.getContentType());
        
        // USA LO STESSO SISTEMA DI WATERMARK E HEADER LOGO
        String fileName = saveFile(file, "company/logos/", "logo_");
        
        log.info("File salvato con nome: {}", fileName);
        
        // Elimina il vecchio logo se esiste
        if (company.getLogoPath() != null) {
            log.info("Eliminazione vecchio logo: {}", company.getLogoPath());
            deleteFile(company.getLogoPath());
        }
        
        company.setLogoPath(fileName);
        company.setLastUpdated(LocalDate.now());
        
        CompanyInfo saved = companyInfoRepository.save(company);
        log.info("Logo aggiornato in database. Nuovo path: {}", saved.getLogoPath());
        
        // Verifica che il file esista effettivamente
        Path filePath = Paths.get(UPLOAD_DIR + fileName);
        if (Files.exists(filePath)) {
            log.info("✓ Verifica file: File esistente al path {}", filePath.toAbsolutePath());
            log.info("✓ Dimensione file: {} bytes", Files.size(filePath));
        } else {
            log.error("✗ ERRORE: File non trovato al path {}", filePath.toAbsolutePath());
        }
        
        return saved;
    }
}