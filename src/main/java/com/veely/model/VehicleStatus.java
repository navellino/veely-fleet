package com.veely.model;

public enum VehicleStatus {
    IN_SERVICE("In servizio"),
    ASSIGNED("Assegnata"),
    UNDER_MAINTENANCE("In manutenzione"),
    OUT_OF_SERVICE("Fuori servizio");

    private final String displayName;

    VehicleStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
