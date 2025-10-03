package com.veely.model;

public enum FuelType {
    PETROL("Benzina"),
    DIESEL("Diesel"),
    ELECTRIC("Elettrico"),
    PROVA("Prova"),
    HYBRID("Ibrido");

    private final String displayName;

    FuelType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}