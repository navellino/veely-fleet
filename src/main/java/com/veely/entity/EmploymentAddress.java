package com.veely.entity;

import com.veely.model.FullAddress;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

/**
 * Indirizzo di lavoro (cantiere esterno) collegato a un Employment.
 * <p>
 * Ricalca {@link FullAddress} (countryCode, regionCode, …, street)
 * ma aggiunge:
 * <ul>
 *   <li><strong>siteName</strong>  – denominazione del cantiere / sede distaccata</li>
 *   <li><strong>jobNumber</strong> – numero di commessa (anche alfanumerico)</li>
 * </ul>
 */
@Embeddable
@Data
@SuperBuilder
@NoArgsConstructor @AllArgsConstructor
public class EmploymentAddress {

    // ===== campi ereditati (copiati) =====
    private String countryCode;
    private String regionCode;
    private String provinceCode;
    private String cityCode;
    private String postalCode;
    private String street;

    // ===== nuovi =====
    @Size(max = 100)
    @Column(name = "workplace_site_name")
    private String siteName;

    @NotBlank
    @Size(max = 30)
    @Column(name = "workplace_job_number")
    private String jobNumber;
}
