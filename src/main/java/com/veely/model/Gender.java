package com.veely.model;

public enum Gender {
    MALE("M"),
    FEMALE("F"),
    OTHER("Altro");

    private final String displayName;

    Gender(String displayName) { this.displayName = displayName; }

    public String getDisplayName() { return displayName; }
}
