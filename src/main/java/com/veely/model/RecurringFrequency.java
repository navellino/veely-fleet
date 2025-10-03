package com.veely.model;

/** Frequenza di ricorrenza per canoni periodici. */
public enum RecurringFrequency {
    MENSILE("Mensile"),
    TRIMESTRALE("Trimestrale"),
    ANNUALE("Annuale");

    private final String displayName;

    RecurringFrequency(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
