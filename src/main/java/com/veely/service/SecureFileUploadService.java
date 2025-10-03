package com.veely.service;

import com.veely.validation.FileValidator;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.security.MessageDigest;
import java.security.NoSuchAlgorithmException;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Slf4j
@Service
@RequiredArgsConstructor
public class SecureFileUploadService {
    
    private final FileValidator fileValidator;
    private final FileSystemStorageService storageService;
    
    @Value("${storage.location:uploads}")
    private String uploadDir;
    
    /**
     * Upload sicuro di un documento con validazione
     */
    public String uploadDocument(MultipartFile file, String subDirectory) throws IOException {
        // Valida il file
        fileValidator.validateDocument(file);
        
        // Genera nome sicuro per il file
        String safeFilename = generateSafeFilename(file.getOriginalFilename());
        
        // Crea directory se non esiste
        Path targetDir = Paths.get(uploadDir, subDirectory);
        Files.createDirectories(targetDir);
        
        // Verifica che il path sia all'interno della directory consentita
        Path targetPath = targetDir.resolve(safeFilename).normalize();
        if (!targetPath.startsWith(Paths.get(uploadDir))) {
            throw new SecurityException("Tentativo di accesso a directory non autorizzata");
        }
        
        // Salva il file
        try {
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
            log.info("File salvato con successo: {} in {}", safeFilename, subDirectory);
            
            // Verifica l'integrità del file salvato
            verifyFileIntegrity(file, targetPath);
            
            return safeFilename;
            
        } catch (IOException e) {
            log.error("Errore durante il salvataggio del file: {}", safeFilename, e);
            // Tenta di eliminare il file parzialmente salvato
            try {
                Files.deleteIfExists(targetPath);
            } catch (IOException ignored) {}
            throw e;
        }
    }
    
    /**
     * Upload sicuro di un'immagine con validazione
     */
    public String uploadImage(MultipartFile file, String subDirectory) throws IOException {
        // Valida l'immagine
        fileValidator.validateImage(file);
        
        // Il resto è simile a uploadDocument
        String safeFilename = generateSafeFilename(file.getOriginalFilename());
        Path targetDir = Paths.get(uploadDir, subDirectory);
        Files.createDirectories(targetDir);
        
        Path targetPath = targetDir.resolve(safeFilename).normalize();
        if (!targetPath.startsWith(Paths.get(uploadDir))) {
            throw new SecurityException("Tentativo di accesso a directory non autorizzata");
        }
        
        Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        log.info("Immagine salvata con successo: {} in {}", safeFilename, subDirectory);
        
        return safeFilename;
    }
    
    /**
     * Genera un nome file sicuro e univoco
     */
    private String generateSafeFilename(String originalFilename) {
        // Estrai l'estensione
        String extension = "";
        if (originalFilename != null && originalFilename.contains(".")) {
            extension = originalFilename.substring(originalFilename.lastIndexOf("."));
            // Rimuovi caratteri non sicuri dall'estensione
            extension = extension.replaceAll("[^a-zA-Z0-9.]", "");
        }
        
        // Genera nome univoco con timestamp e UUID
        String timestamp = LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyyMMdd_HHmmss"));
        String uniqueId = UUID.randomUUID().toString().substring(0, 8);
        
        return timestamp + "_" + uniqueId + extension.toLowerCase();
    }
    
    /**
     * Verifica l'integrità del file salvato
     */
    private void verifyFileIntegrity(MultipartFile originalFile, Path savedPath) throws IOException {
        long originalSize = originalFile.getSize();
        long savedSize = Files.size(savedPath);
        
        if (originalSize != savedSize) {
            Files.deleteIfExists(savedPath);
            throw new IOException("Il file salvato ha dimensioni diverse dall'originale");
        }
    }
    
    /**
     * Calcola hash SHA-256 del file (opzionale, per sicurezza extra)
     */
    private String calculateFileHash(Path filePath) throws IOException {
        try {
            MessageDigest digest = MessageDigest.getInstance("SHA-256");
            byte[] fileBytes = Files.readAllBytes(filePath);
            byte[] hashBytes = digest.digest(fileBytes);
            
            StringBuilder hexString = new StringBuilder();
            for (byte b : hashBytes) {
                String hex = Integer.toHexString(0xff & b);
                if (hex.length() == 1) hexString.append('0');
                hexString.append(hex);
            }
            return hexString.toString();
            
        } catch (NoSuchAlgorithmException e) {
            throw new RuntimeException("Algoritmo SHA-256 non disponibile", e);
        }
    }
}