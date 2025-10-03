package com.veely.model;

public enum MaritalStatus {
    SINGLE("Celibe/Nubile"),
    MARRIED("Sposato/a"),
    WIDOWED("Vedovo/a"),
    DIVORCED("Divorzio/Separa­to/a"),
    SEPARATED("Separato/a");

    private final String displayName;
    MaritalStatus(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
