package com.veely.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.math.BigDecimal;
import java.time.LocalDate;

/**
 * Fattura emessa per una commessa.
 */
@Entity
@Table(name = "project_invoices")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProjectInvoice {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;

    /** Numero della fattura */
    private String number;

    /** Data di emissione */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate date;

    /** Descrizione (max 2000 caratteri) */
    @Column(length = 2000)
    private String description;

    /** Importo della fattura */
    private BigDecimal amount;
}
