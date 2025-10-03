package com.veely.model;

public enum ExpenseStatus {
	Draft("Bozza"),
	Submitted("Completa"),
	Approved("Approvata"),
	Rejected("Rigettata"),
	Paied("Pagata");

    private final String displayName;
    ExpenseStatus(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }

}
