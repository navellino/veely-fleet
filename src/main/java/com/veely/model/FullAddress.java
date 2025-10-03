package com.veely.model;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.*;
import lombok.experimental.SuperBuilder;

import java.io.Serializable;

/**
 * Embeddable che rappresenta un indirizzo completo:
 * via, paese, codice e nome regione, provincia, comune, località, CAP.
 */
@Embeddable
@Data
@SuperBuilder(toBuilder = true)   // ⬅️ cambia Builder → SuperBuilder
@NoArgsConstructor
@AllArgsConstructor
public class FullAddress implements Serializable {

    /**
	 * 
	 */
	private static final long serialVersionUID = -1734021424827544642L;

	/** Via, piazza, ecc. */
    @Column(name = "street", length = 200)
    private String street;

    /** Codice ISO nazione (es. IT) */
    @Column(name = "country_code", length = 2)
    private String countryCode;

    /** Nome nazione (es. Italia) */
    @Column(name = "country", length = 100)
    private String country;

    /** Codice Regione (es. 03 per Lazio) */
    @Column(name = "region_code", length = 5)
    private String regionCode;

    /** Nome Regione (es. Lazio) */
    @Column(name = "region", length = 100)
    private String region;

    /** Codice Provincia (es. RM) */
    @Column(name = "province_code", length = 5)
    private String provinceCode;

    /** Nome Provincia (es. Roma) */
    @Column(name = "province", length = 100)
    private String province;

    /** Codice Comune ISTAT (es. 058091) */
    @Column(name = "city_code", length = 10)
    private String cityCode;

    /** Nome Comune (es. Roma) */
    @Column(name = "city", length = 100)
    private String city;

    /** Località (frazione, quartiere) */
    @Column(name = "locality", length = 100)
    private String locality;

    /** CAP */
    @Column(name = "postal_code", length = 10)
    private String postalCode;
}