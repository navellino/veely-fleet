package com.veely.model;

public enum DocumentStatus {
    VALID("Valido"),
    EXPIRED("Scaduto"),
    PENDING("In attesa");

    private final String displayName;

    DocumentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}