package com.veely.model;

public enum OwnershipType {
    OWNED("Di proprietà"),
    LEASED("In leasing");

    private final String displayName;
    OwnershipType(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
