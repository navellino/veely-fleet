package com.veely.model;

public enum DocumentType {
    // ---- VEICOLO ----
	SPACE1("---- VEICOLO ----"),
    VEHICLE_REGISTRATION("Libretto circolazione"),
    INSURANCE("Assicurazione"),
    MAINTENANCE("Manutenzioni"),
    VEHICLE_IMAGE("Immagine veicolo"),
    LEASE_CONTRACT("Contratto di leasing"),
    MAINT_REPORT("Rapportino di manutenzione"),
    MAINT_INVOICE("Fattura manutenzione"),
    
    //---CONTRATTI
    SUPPLIER_CONTRACT("Contratto fornitore"),
    CORRESPONDENCE("Corrispondenza"),
    DURC("DURC"),
    
    // ---- RAPPORTO DI LAVORO ----
    SPACE("---- RAPPORTO DI LAVORO ----"),
    EMPLOYMENT_CONTRACT("Contratto di lavoro"),
    PAY_RISE("Aumento retribuzione"),
    EXTENTION("Contratto di Proroga"),
    UNILAV("Comunicazione Obbligatoria"),
    TRANSFER_ORDER("Lettera di Trasferimento"),
    DISCIPLINARY_LETTER("Lettera di richiamo"),
    TAX_DEDUCTIONS("Modulo Detrazioni"),
    
    // ---- PERSONA ----
    SPACE2("---- PERSONA ----"),
    IDENTITY_PHOTO("Foto Profilo"),
    IDENTITY_DOCUMENT("Documento identità"),
    FISCAL_CODE("Tessera Sanitaria"),
    SAP("Scheda Anagrafica Professionale"),
    GRADE("Titolo di studio"),
    
    // ---- ASSEGNAZIONI ----
    SPACE3("---- ASSEGNAZIONI ----"),
    ASSIGNMENT_PHOTO("Foto veicolo durante assegnazione"),
    ASSIGNMENT_REPORT("Report riconsegna / danni"),
    ASSIGNMENT_LETTER("Lettera di assegnazione veicolo"),
    
    // ------ NOTE SPESE ------
    SPACE4("---- NOTE SPESE ----"),
    INVOICE("Fattura"),
    RECEIPT("Scontrino/Ricevuta"),
    
 // ---- SAFETY & COMPLIANCE ----
    CERTIFICATE("Attestato"),
    MEDICAL("Certificato medico"),
    PPE("Documento DPI"),
    COMPANY_LOGO("Logo Aziendale"),
    OTHER("Altro");

    private final String displayName;

    DocumentType(String displayName) {
        this.displayName = displayName;
    }

    public String getDisplayName() {
        return displayName;
    }
}
