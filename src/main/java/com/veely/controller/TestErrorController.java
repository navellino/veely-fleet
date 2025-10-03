package com.veely.controller;

import com.veely.exception.ResourceNotFoundException;
import org.springframework.web.bind.annotation.*;
import jakarta.validation.Valid;      // <-- Import corretto
import jakarta.validation.constraints.Min;     // <-- Import corretto
import jakarta.validation.constraints.NotBlank; // <-- Import corretto
import lombok.Data;

@RestController
@RequestMapping("/test")
public class TestErrorController {
    
    @GetMapping("/not-found")
    public void testNotFound() {
        throw new ResourceNotFoundException("Test: Risorsa non trovata");
    }
    
    @GetMapping("/error")
    public void testError() {
        throw new RuntimeException("Test: Errore generico");
    }
    
    @PostMapping("/validation")
    public void testValidation(@RequestBody @Valid TestRequest request) {
        // Verrà validato automaticamente
    }
    
    @Data
    static class TestRequest {
        @NotBlank(message = "Nome obbligatorio")
        private String name;
        
        @Min(value = 18, message = "Età minima 18 anni")
        private int age;
    }
}