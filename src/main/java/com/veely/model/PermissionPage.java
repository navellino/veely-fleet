package com.veely.model;

/**
 * Enumerates available application pages that can be protected by role permissions.
 */
public enum PermissionPage {
    FLEET("Flotta"),
    DRIVERS("Dipendenti"),
    SETTINGS("Impostazioni");

    private final String label;

    PermissionPage(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }
}
