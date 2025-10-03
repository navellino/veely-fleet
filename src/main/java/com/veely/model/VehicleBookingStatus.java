package com.veely.model;

public enum VehicleBookingStatus {
    PLANNED("Pianificata"),
    CONFIRMED("Confermata"),
    COMPLETED("Completata"),
    CANCELLED("Annullata");

    private final String displayName;

    VehicleBookingStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
