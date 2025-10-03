package com.veely.model;

public enum MaintenanceType {
    ORDINARY_SERVICE("Tagliando"),
    REVISION("Revisione"),
    EXTRA_SERVICE("Manutenzione Straordinaria"),
    TYRE_CHANGE_SUMMER("Cambio gomme estive"),
    TYRE_CHANGE_WINTER("Cambio gomme invernali"),
    OTHER("Altro");

    private final String displayName;

    MaintenanceType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
