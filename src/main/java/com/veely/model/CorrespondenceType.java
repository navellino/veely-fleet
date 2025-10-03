package com.veely.model;

public enum CorrespondenceType {
    E("Entrata"),
    U("Uscita");

    private final String label;

    CorrespondenceType(String label) { this.label = label; }

    public String getLabel() { return label; }
}
