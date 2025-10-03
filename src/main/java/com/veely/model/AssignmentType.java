package com.veely.model;

public enum AssignmentType {
    LONG_TERM("Lungo termine"),
    SHORT_TERM("Breve termine");

    private final String displayName;

    AssignmentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
