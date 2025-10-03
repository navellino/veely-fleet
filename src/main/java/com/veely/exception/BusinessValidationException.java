package com.veely.exception;

import lombok.Getter;
import java.util.ArrayList;
import java.util.List;

@Getter
public class BusinessValidationException extends RuntimeException {
    
    private final List<String> errors;
    
    public BusinessValidationException(String message) {
        super(message);
        this.errors = new ArrayList<>();
    }
    
    public BusinessValidationException(String message, List<String> errors) {
        super(message);
        this.errors = errors != null ? new ArrayList<>(errors) : new ArrayList<>();
    }
    
    public BusinessValidationException(String message, Throwable cause) {
        super(message, cause);
        this.errors = new ArrayList<>();
    }
}
