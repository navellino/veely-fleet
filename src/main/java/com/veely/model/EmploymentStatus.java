package com.veely.model;

/**
 * Stato corrente del rapporto di lavoro.
 */
public enum EmploymentStatus {

    ACTIVE("Attivo"),
    ON_LEAVE("In aspettativa / congedo"),
    SUSPENDED("Sospeso"),
    TERMINATED("Cessato");

    private final String displayName;

    EmploymentStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}