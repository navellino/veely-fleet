package com.veely.model;

/** Tipologia di contratto con i fornitori. */
public enum SupplierContractType {
    FORNITURA("Fornitura"),
    SERVIZI("Servizi"),
    LAVORI("Lavori"),
    NOLEGGIO("Noleggio"),
    CONSULENZA("Consulenza"),
    ALTRO("Altro");

    private final String displayName;

    SupplierContractType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
