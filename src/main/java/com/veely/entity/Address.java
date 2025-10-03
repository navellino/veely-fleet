package com.veely.entity;

import jakarta.persistence.Embeddable;
import lombok.Getter;
import lombok.Setter;

@Embeddable
@Getter @Setter
public class Address {
    private String street;         // Via, Piazza, etc.
    private String countryCode;    // ISO-3166 alpha2
    private String country;        // “Italia”, “Francia”…
    private String regionCode;     // Es. “IT-56”
    private String region;         // “Lombardia”
    private String provinceCode;   // “MI”
    private String province;       // “Milano”
    private String cityCode;       // Codice ISTAT, optional
    private String city;           // “Milano”
    private String locality;       // eventuale frazione
    private String postalCode;     // CAP
}
