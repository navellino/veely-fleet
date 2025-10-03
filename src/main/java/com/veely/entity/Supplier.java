package com.veely.entity;

import com.veely.model.FullAddress;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import java.util.ArrayList;
import java.util.List;

/**
 * Fornitore/Dealer del veicolo. Può essere la società di leasing
 * o il concessionario dal quale è stato acquistato il mezzo.
 */
@Entity
@Table(name = "suppliers")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Supplier {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Denominazione o ragione sociale */
    @Column(nullable = false)
    @NotBlank
    private String name;

    /** Partita IVA o codice fiscale */
    private String vatNumber;
    
    /** Telefono aziendale */
    private String companyPhone;

    /** Email aziendale */
    private String companyEmail;

    /** PEC aziendale */
    private String pec;

    /** IBAN */
    private String iban;

    /** Codice SDI */
    private String sdiCode;

    @OneToMany(mappedBy = "supplier", cascade = CascadeType.ALL, orphanRemoval = true)
    private List<SupplierReferent> referents = new ArrayList<>();
       
    @Embedded
    private FullAddress address;
}

