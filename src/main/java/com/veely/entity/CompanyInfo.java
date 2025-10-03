package com.veely.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Entity
@Table(name = "company_info")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class CompanyInfo {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    // === INFORMAZIONI AZIENDALI BASE ===
    @Column(nullable = false, length = 200)
    private String companyName; // Nome azienda - OBBLIGATORIO

    @Column(nullable = true, length = 100)
    private String legalForm; // Forma giuridica (SRL, SPA, etc.)

    @Column(nullable = true, columnDefinition = "TEXT")
    private String businessDescription; // Descrizione attività

    // === INDIRIZZI ===
    @Column(nullable = true, length = 200)
    private String legalAddress; // Sede legale completa

    @Column(nullable = false, length = 100)
    private String legalStreet; // Via sede legale - OBBLIGATORIO

    @Column(nullable = true, length = 10)
    private String legalCivicNumber; // Numero civico

    @Column(nullable = false, length = 10)
    private String legalPostalCode; // CAP sede legale - OBBLIGATORIO

    @Column(nullable = false, length = 100)
    private String legalCity; // Città sede legale - OBBLIGATORIO

    @Column(nullable = true, length = 100)
    private String legalProvince; // Provincia sede legale

    @Column(nullable = true, length = 100)
    private String legalCountry; // Paese sede legale

    // Sede operativa (se diversa)
    @Column(nullable = true, length = 200)
    private String operationalAddress; // Sede operativa completa

    @Column(nullable = true, length = 100)
    private String operationalStreet;

    @Column(nullable = true, length = 10)
    private String operationalCivicNumber;

    @Column(nullable = true, length = 10)
    private String operationalPostalCode;

    @Column(nullable = true, length = 100)
    private String operationalCity;

    @Column(nullable = true, length = 100)
    private String operationalProvince;

    @Column(nullable = true, length = 100)
    private String operationalCountry;

    // === CONTATTI ===
    @Column(nullable = true, length = 40)
    private String primaryPhone; // Telefono principale

    @Column(nullable = true, length = 40)
    private String secondaryPhone; // Telefono secondario

    @Column(nullable = true, length = 40)
    private String faxNumber; // Fax

    @Column(nullable = true, length = 100)
    private String primaryEmail; // Email principale

    @Column(nullable = true, length = 100)
    private String secondaryEmail; // Email secondaria

    @Column(nullable = true, length = 100)
    private String pecEmail; // PEC

    @Column(nullable = true, length = 200)
    private String website; // Sito web

    // === DATI FISCALI ===
    @Column(nullable = true, length = 40)
    private String vatNumber; // Partita IVA - OBBLIGATORIO

    @Column(nullable = true, length = 40)
    private String taxCode; // Codice fiscale

    @Column(nullable = true, length = 50)
    private String chamberOfCommerceNumber; // Numero Camera Commercio

    @Column(nullable = true, length = 100)
    private String chamberOfCommerceCity; // Camera Commercio di...

    @Column(nullable = true, length = 40)
    private String reaNumber; // Numero REA

    @Column(nullable = true, length = 50)
    private String shareCapital; // Capitale sociale

    @Column(nullable = true, length = 50)
    private String economicActivityCode; // Codice ATECO

    @Column(nullable = true, length = 200)
    private String economicActivityDescription; // Descrizione attività ATECO

    // === DATI BANCARI ===
    @Column(nullable = true, length = 100)
    private String bankName; // Nome banca

    @Column(nullable = true, length = 50)
    private String bankBranch; // Filiale

    @Column(nullable = true, length = 34)
    private String iban; // IBAN

    @Column(nullable = true, length = 11)
    private String swift; // Codice SWIFT/BIC

    // === DATI STORICI ===
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(nullable = true)
    private LocalDate foundationDate; // Data costituzione

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(nullable = true)
    private LocalDate registrationDate; // Data iscrizione registro imprese

    // === PERSONE DI RIFERIMENTO ===
    @Column(nullable = true, length = 100)
    private String legalRepresentative; // Legale rappresentante

    @Column(nullable = true, length = 100)
    private String legalRepresentativeRole; // Ruolo legale rappresentante

    @Column(nullable = true, length = 100)
    private String administrativeContact; // Contatto amministrativo

    @Column(nullable = true, length = 20)
    private String administrativePhone; // Telefono amministrativo

    @Column(nullable = true, length = 100)
    private String administrativeEmail; // Email amministrativo

    @Column(nullable = true, length = 100)
    private String technicalContact; // Contatto tecnico

    @Column(nullable = true, length = 20)
    private String technicalPhone; // Telefono tecnico

    @Column(nullable = true, length = 100)
    private String technicalEmail; // Email tecnico

    // === CERTIFICAZIONI E QUALITÀ ===
    @Column(nullable = true, columnDefinition = "TEXT")
    private String certifications; // Certificazioni (ISO, etc.)

    @Column(nullable = true, columnDefinition = "TEXT")
    private String qualityPolicies; // Politiche qualità

    @Column(nullable = true, columnDefinition = "TEXT")
    private String environmentalPolicies; // Politiche ambientali

    @Column(nullable = true, columnDefinition = "TEXT")
    private String safetyPolicies; // Politiche sicurezza

    // === BRANDING ===
    @Column(nullable = true, length = 500)
    private String logoPath; // Percorso logo aziendale

    @Column(nullable = true, length = 500)
    private String headerLogoPath; // Logo per intestazioni

    @Column(nullable = true, length = 500)
    private String watermarkPath; // Watermark documenti

    @Column(nullable = true, length = 20)
    private String primaryColor; // Colore primario brand (#RRGGBB)

    @Column(nullable = true, length = 20)
    private String secondaryColor; // Colore secondario brand

    @Column(nullable = true, length = 100)
    private String fontFamily; // Font aziendale

    // === INFORMAZIONI AGGIUNTIVE ===
    @Column(nullable = true, columnDefinition = "TEXT")
    private String notes; // Note varie

    @Column(nullable = true, length = 500)
    private String slogan; // Slogan aziendale

    @Column(nullable = true, length = 200)
    private String businessSector; // Settore di attività

    @Column(nullable = true, length = 100)
    private String employeeCount; // Numero dipendenti

    @Column(nullable = true, length = 100)
    private String annualRevenue; // Fatturato annuo

    // === SOCIAL MEDIA ===
    @Column(nullable = true, length = 200)
    private String linkedinUrl;

    @Column(nullable = true, length = 200)
    private String facebookUrl;

    @Column(nullable = true, length = 200)
    private String twitterUrl;

    @Column(nullable = true, length = 200)
    private String instagramUrl;

    @Column(nullable = true, length = 200)
    private String youtubeUrl;

    // === METADATI ===
    @Column(nullable = true)
    private Boolean isActive = true; // Record attivo

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(nullable = true)
    private LocalDate lastUpdated; // Ultima modifica

    @Column(nullable = true, length = 100)
    private String updatedBy; // Modificato da

    // === CONFIGURAZIONI DOCUMENTI ===
    @Column(nullable = true)
    private Boolean showLogoInDocuments = true; // Mostra logo nei documenti

    @Column(nullable = true)
    private Boolean showAddressInDocuments = true; // Mostra indirizzo nei documenti

    @Column(nullable = true)
    private Boolean showContactsInDocuments = true; // Mostra contatti nei documenti

    @Column(nullable = true)
    private Boolean showTaxInfoInDocuments = true; // Mostra info fiscali nei documenti

    @Column(nullable = true, columnDefinition = "TEXT")
    private String documentFooterText; // Testo footer documenti

    @Column(nullable = true, columnDefinition = "TEXT")
    private String documentHeaderText; // Testo header documenti

    // === CONFIGURAZIONI EMAIL ===
    @Column(nullable = true, columnDefinition = "TEXT")
    private String emailSignature; // Firma email

    @Column(nullable = true)
    private Boolean useCustomEmailSignature = false;

    // Metodi di utilità
    public String getFullLegalAddress() {
        StringBuilder address = new StringBuilder();
        if (legalStreet != null && !legalStreet.trim().isEmpty()) {
            address.append(legalStreet);
            if (legalCivicNumber != null && !legalCivicNumber.trim().isEmpty()) {
                address.append(", ").append(legalCivicNumber);
            }
            if (legalPostalCode != null && !legalPostalCode.trim().isEmpty()) {
                address.append(" - ").append(legalPostalCode);
            }
            if (legalCity != null && !legalCity.trim().isEmpty()) {
                address.append(" ").append(legalCity);
            }
            if (legalProvince != null && !legalProvince.trim().isEmpty()) {
                address.append(" (").append(legalProvince).append(")");
            }
        }
        return address.toString();
    }

    public String getFullOperationalAddress() {
        StringBuilder address = new StringBuilder();
        if (operationalStreet != null && !operationalStreet.trim().isEmpty()) {
            address.append(operationalStreet);
            if (operationalCivicNumber != null && !operationalCivicNumber.trim().isEmpty()) {
                address.append(", ").append(operationalCivicNumber);
            }
            if (operationalPostalCode != null && !operationalPostalCode.trim().isEmpty()) {
                address.append(" - ").append(operationalPostalCode);
            }
            if (operationalCity != null && !operationalCity.trim().isEmpty()) {
                address.append(" ").append(operationalCity);
            }
            if (operationalProvince != null && !operationalProvince.trim().isEmpty()) {
                address.append(" (").append(operationalProvince).append(")");
            }
        }
        return address.toString();
    }

    public String getDisplayName() {
        StringBuilder name = new StringBuilder();
        if (companyName != null && !companyName.trim().isEmpty()) {
            name.append(companyName);
           // if (legalForm != null && !legalForm.trim().isEmpty()) {
           //     name.append(" ").append(legalForm);
           // }
        }
        return name.toString();
    }

    public boolean hasOperationalAddress() {
        return operationalStreet != null && !operationalStreet.trim().isEmpty();
    }

    public boolean hasLogo() {
        return logoPath != null && !logoPath.trim().isEmpty();
    }

    public boolean hasHeaderLogo() {
        return headerLogoPath != null && !headerLogoPath.trim().isEmpty();
    }
}