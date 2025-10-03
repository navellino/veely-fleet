package com.veely.model;

public enum EducationLevel {
    ELEMENTARY("Scuola Elementare"),
    MIDDLE_SCHOOL("Scuola Media"),
    HIGH_SCHOOL("Diploma di Maturità"),
    BACHELORS("Laurea Triennale"),
    MASTERS("Laurea Magistrale"),
    PHD("Dottorato di Ricerca"),
    OTHER("Altro");

    private final String displayName;
    EducationLevel(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }
}
