package com.veely.validation;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.Arrays;
import java.util.HashSet;
import java.util.Set;

@Slf4j
@Component
public class FileValidator {
    
    // Dimensione massima file: 10MB
    private static final long MAX_FILE_SIZE = 10 * 1024 * 1024;
    
    // Tipi MIME permessi per documenti
    private static final Set<String> ALLOWED_DOCUMENT_TYPES = new HashSet<>(Arrays.asList(
        "application/pdf",
        "image/jpeg", 
        "image/jpg",
        "image/png",
        "image/webp",
        "application/msword", // .doc
        "application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .docx
        "application/vnd.ms-outlook" // .msg
    ));
    
    // Tipi MIME permessi per immagini (foto profilo, foto veicoli)
    private static final Set<String> ALLOWED_IMAGE_TYPES = new HashSet<>(Arrays.asList(
        "image/jpeg",
        "image/jpg",
        "image/webp",
        "image/png"
    ));
    
    // Estensioni permesse (controllo aggiuntivo)
    private static final Set<String> ALLOWED_EXTENSIONS = new HashSet<>(Arrays.asList(
    		"pdf", "jpg", "jpeg", "png", "webp", "doc", "docx", "msg"
    ));
    
    /**
     * Valida un file generico (documento)
     */
    public void validateDocument(MultipartFile file) {
        log.debug("Validazione documento: {}", file.getOriginalFilename());
        
        // Controlla se il file è vuoto
        if (file.isEmpty()) {
            throw new ValidationException("Il file è vuoto");
        }
        
        // Controlla dimensione
        if (file.getSize() > MAX_FILE_SIZE) {
            throw new ValidationException(
                String.format("Il file supera la dimensione massima consentita di %d MB", 
                    MAX_FILE_SIZE / (1024 * 1024))
            );
        }
        
        // Controlla tipo MIME
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_DOCUMENT_TYPES.contains(contentType.toLowerCase())) {
            throw new ValidationException(
            		"Tipo di file non permesso. Tipi accettati: PDF, JPG, PNG, WEBP, DOC, DOCX, MSG"
            );
        }
        
        // Controlla estensione del file
        String filename = file.getOriginalFilename();
        if (filename != null) {
            String extension = getFileExtension(filename);
            if (!ALLOWED_EXTENSIONS.contains(extension.toLowerCase())) {
                throw new ValidationException(
                    "Estensione file non permessa: ." + extension
                );
            }
            
            // Controlla caratteri nel nome file
            if (!isValidFilename(filename)) {
                throw new ValidationException(
                    "Il nome del file contiene caratteri non validi"
                );
            }
        }
        
        log.info("File validato con successo: {} ({})", filename, contentType);
    }
    
    /**
     * Valida specificamente un'immagine (per foto profilo o veicoli)
     */
    public void validateImage(MultipartFile file) {
        log.debug("Validazione immagine: {}", file.getOriginalFilename());
        
        if (file.isEmpty()) {
            throw new ValidationException("Il file è vuoto");
        }
        
        // Dimensione massima per immagini: 5MB
        long maxImageSize = 5 * 1024 * 1024;
        if (file.getSize() > maxImageSize) {
            throw new ValidationException(
                "L'immagine supera la dimensione massima consentita di 5 MB"
            );
        }
        
        String contentType = file.getContentType();
        if (contentType == null || !ALLOWED_IMAGE_TYPES.contains(contentType.toLowerCase())) {
            throw new ValidationException(
            		"Tipo di immagine non permesso. Formati accettati: JPG, PNG, WEBP"
            );
        }
        
        String filename = file.getOriginalFilename();
        if (filename != null) {
            String extension = getFileExtension(filename);
            if (!Arrays.asList("jpg", "jpeg", "png", "webp").contains(extension.toLowerCase())) {
                throw new ValidationException(
                    "Formato immagine non permesso: ." + extension
                );
            }
        }
        
        log.info("Immagine validata con successo: {} ({})", filename, contentType);
    }
    
    /**
     * Estrae l'estensione dal nome del file
     */
    private String getFileExtension(String filename) {
        int lastIndexOf = filename.lastIndexOf(".");
        if (lastIndexOf == -1) {
            return "";
        }
        return filename.substring(lastIndexOf + 1);
    }
    
    /**
     * Verifica che il nome del file non contenga caratteri pericolosi
     */
    private boolean isValidFilename(String filename) {
        // Permette solo lettere, numeri, spazi, trattini, underscore e punti
        return filename.matches("^[a-zA-Z0-9\\s\\-_.]+$");
    }
    
    /**
     * Eccezione custom per errori di validazione
     */
    public static class ValidationException extends RuntimeException {
        public ValidationException(String message) {
            super(message);
        }
    }
}
