package com.veely.model;

public enum TaskStatus {
    OPEN("Aperto"),
    CLOSED("Chiuso");

    private final String displayName;

    TaskStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
