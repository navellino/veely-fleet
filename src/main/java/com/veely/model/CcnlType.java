package com.veely.model;

public enum CcnlType {
    METALMECCANICI("Metalmeccanico"),
    COMMERCIO("Commercio"),
    EDILIZIA("Edilizia");

    private final String displayName;

    CcnlType(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }
}
