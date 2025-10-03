package com.veely.model;

/** Natura del contratto di lavoro. */
public enum ContractType {
    PERMANENT("Tempo indeterminato"),
    FIXED_TERM("Tempo determinato");

    private final String displayName;

    ContractType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
