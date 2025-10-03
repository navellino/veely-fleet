package com.veely.model;

/** Valute supportate per gli importi dei contratti. */
public enum CurrencyCode {
    EUR("EUR"),
	USD("US DOllar");

    private final String displayName;

    CurrencyCode(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }

}
