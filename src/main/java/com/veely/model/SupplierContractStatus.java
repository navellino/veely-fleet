package com.veely.model;

/** Stato del contratto fornitore. */
public enum SupplierContractStatus {
    BOZZA("Bozza"),
    IN_APPROVAZIONE("In approvazione"),
    IN_ESECUZIONE("Attivo"),
    SOSPESO("Sospeso"),
    SCADUTO("Scaduto"),
    RECESSO("Recesso");

    private final String displayName;

    SupplierContractStatus(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
