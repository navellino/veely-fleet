package com.veely.exception.handler;

import com.veely.exception.ResourceNotFoundException;
import com.veely.exception.BusinessValidationException;
import com.veely.validation.FileValidator;

import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.multipart.MaxUploadSizeExceededException;

import jakarta.servlet.http.HttpServletRequest; // <-- Import corretto per Spring Boot 3
import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@RestControllerAdvice(annotations = RestController.class)
public class GlobalExceptionHandler {

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorResponse> handleResourceNotFound(
            ResourceNotFoundException ex, 
            HttpServletRequest request) {
        
        log.error("Risorsa non trovata: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.NOT_FOUND.value(),
            "Risorsa non trovata",
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(DataIntegrityViolationException.class)
    public ResponseEntity<ErrorResponse> handleDataIntegrityViolation(
            DataIntegrityViolationException ex,
            HttpServletRequest request) {
        
        log.error("Violazione vincolo database: ", ex);
        
        String message = "Errore di integrità dati";
        if (ex.getMessage().contains("duplicate key")) {
            message = "Valore duplicato: il record esiste già";
        } else if (ex.getMessage().contains("foreign key")) {
            message = "Impossibile eliminare: ci sono dati collegati";
        }
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.CONFLICT.value(),
            "Conflitto dati",
            message,
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.CONFLICT);
    }
    
    @ExceptionHandler(BusinessValidationException.class)
    public ResponseEntity<Map<String, Object>> handleBusinessValidation(
            BusinessValidationException ex,
            HttpServletRequest request) {

        log.warn("Validazione business fallita: {}", ex.getMessage());

        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Regole di validazione violate");
        response.put("message", ex.getMessage());
        response.put("errors", ex.getErrors());
        response.put("path", request.getRequestURI());

        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<Map<String, Object>> handleValidationExceptions(
            MethodArgumentNotValidException ex,
            HttpServletRequest request) {
        
        log.error("Errore validazione: ", ex);
        
        Map<String, String> errors = new HashMap<>();
        ex.getBindingResult().getAllErrors().forEach((error) -> {
            String fieldName = ((FieldError) error).getField();
            String errorMessage = error.getDefaultMessage();
            errors.put(fieldName, errorMessage);
        });
        
        Map<String, Object> response = new HashMap<>();
        response.put("timestamp", LocalDateTime.now());
        response.put("status", HttpStatus.BAD_REQUEST.value());
        response.put("error", "Errore di validazione");
        response.put("errors", errors);
        response.put("path", request.getRequestURI());
        
        return new ResponseEntity<>(response, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(MaxUploadSizeExceededException.class)
    public ResponseEntity<ErrorResponse> handleMaxSizeException(
            MaxUploadSizeExceededException ex,
            HttpServletRequest request) {
        
        log.error("File troppo grande: ", ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.PAYLOAD_TOO_LARGE.value(),
            "File troppo grande",
            "Il file supera la dimensione massima consentita di 10MB",
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.PAYLOAD_TOO_LARGE);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorResponse> handleGenericException(
            Exception ex,
            HttpServletRequest request) {
        
        log.error("Errore generico: ", ex);
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.INTERNAL_SERVER_ERROR.value(),
            "Errore interno",
            "Si è verificato un errore imprevisto. Contatta l'assistenza.",
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.INTERNAL_SERVER_ERROR);
    }
    
    @ExceptionHandler(FileValidator.ValidationException.class)
    public ResponseEntity<ErrorResponse> handleFileValidationException(
            FileValidator.ValidationException ex,
            HttpServletRequest request) {
        
        log.error("Errore validazione file: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.BAD_REQUEST.value(),
            "File non valido",
            ex.getMessage(),
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(SecurityException.class)
    public ResponseEntity<ErrorResponse> handleSecurityException(
            SecurityException ex,
            HttpServletRequest request) {
        
        log.error("Tentativo di violazione sicurezza: {}", ex.getMessage());
        
        ErrorResponse errorResponse = new ErrorResponse(
            HttpStatus.FORBIDDEN.value(),
            "Accesso negato",
            "Operazione non consentita per motivi di sicurezza",
            request.getRequestURI()
        );
        
        return new ResponseEntity<>(errorResponse, HttpStatus.FORBIDDEN);
    }
}