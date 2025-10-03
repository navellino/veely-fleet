package com.veely.entity;

import com.veely.model.*;
import jakarta.persistence.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import jakarta.validation.constraints.*;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.Period;
import java.util.ArrayList;
import java.util.List;

/**
 * Contratto stipulato con un fornitore.
 */
@Entity
@Table(name = "contracts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Contract {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    @ManyToOne
    @JoinColumn(name = "project_id", nullable=true)
    private Project project;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @NotNull
    private SupplierContractType type;

    /** Oggetto del contratto */
    @Column(nullable = false)
    @NotBlank
    private String subject;

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    @Builder.Default
    private SupplierContractStatus status = SupplierContractStatus.BOZZA;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    /** Preavviso di recesso in giorni */
    private Integer terminationNoticeDays;

    /** Promemoria scadenza */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expiryReminder;

    /** Importo imponibile */
    @Column(name = "amount_net", precision = 15, scale = 2)
    private BigDecimal amountNet;

    /** Aliquota IVA in percentuale */
    @Column(name = "vat_rate", precision = 5, scale = 2)
    private BigDecimal vatRate;

    /** Valuta dell'importo */
    @Enumerated(EnumType.STRING)
    private CurrencyCode currency;

    /** Condizioni di pagamento */
    @Column(length = 1000)
    private String paymentTerms;

    /** Canone periodico, se applicabile */
    @Column(name = "periodic_fee", precision = 15, scale = 2, nullable=true)
    @PositiveOrZero
    private BigDecimal periodicFee;

    @Enumerated(EnumType.STRING)
    private RecurringFrequency recurringFrequency;

    /** Indicatore se è richiesto il DURC */
    private Boolean needsDurc;

    /** Scadenza del DURC */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate durcExpiry;

    /** Persona di riferimento */
    private String referencePerson;

    @OneToMany(mappedBy = "contract", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Document> documents = new ArrayList<>();

    /** Calcola la durata in mesi tra startDate ed endDate */
    @Transient
    public Integer getDurationMonths() {
        if (startDate == null || endDate == null) {
            return null;
        }
        Period p = Period.between(startDate, endDate);
        return p.getYears() * 12 + p.getMonths();
    }

    /** Calcola l'importo lordo */
    @Transient
    public BigDecimal getAmountGross() {
        if (amountNet == null || vatRate == null) {
            return null;
        }
        return amountNet.add(amountNet.multiply(vatRate).divide(BigDecimal.valueOf(100)));
    }
}