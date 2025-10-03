package com.veely.model;

import lombok.Getter;

/**
 * Stato di un cedolino paga all'interno del sistema.
 */
@Getter
public enum PayslipStatus {
    /** Cedolino caricato ma non ancora inviato. */
    PENDING("In attesa di invio"),
    /** Cedolino inviato con successo via email. */
    SENT("Inviato"),
    /** Cedolino non associato a nessun dipendente. */
    UNMATCHED("Dipendente non trovato"),
    /** Invio fallito per un errore durante la spedizione. */
    FAILED("Errore di invio");

    private final String label;

    PayslipStatus(String label) {
        this.label = label;
    }
}
