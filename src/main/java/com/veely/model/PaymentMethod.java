package com.veely.model;

public enum PaymentMethod {
	PAY_SLIP("Busta Paga"),
    BANK_TRANSFER("Bonifico");

    private final String displayName;
    PaymentMethod(String displayName) { this.displayName = displayName; }
    public String getDisplayName() { return displayName; }

}
