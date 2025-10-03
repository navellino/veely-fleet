package com.veely.model;

public enum AssignmentStatus {
    ASSIGNED("Assegnato"),
    RETURNED("Restituito"),
    BOOKED("Prenotato"),
    OPEN("Aperta"),
    CLOSED("Chiusa");

    private final String displayName;

    AssignmentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
