package com.veely.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Iscrizione ad un sindacato per un rapporto di lavoro.
 */
@Entity
@Table(name = "employment_union_memberships")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class EmploymentUnionMembership {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Rapporto di lavoro associato. */
    @ManyToOne(optional = false)
    private Employment employment;

    /** Codice iscrizione sindacale. */
    private String code;

    /** Descrizione del sindacato. */
    private String unionDescription;

    /** Importo trattenuto in busta paga. */
    private BigDecimal deductionAmount;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    /** Indica se il dipendente è RSA. */
    private Boolean rsa;

    /** Indica se il dipendente è dirigente. */
    private Boolean dirigente;
}