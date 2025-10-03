package com.veely.entity;

import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

/**
 * Polizza assicurativa gestita dal sistema.
 */
@Entity
@Table(name = "project_insurances")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Insurance {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id")
    private Project project;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_id", nullable=true)
    private Supplier supplier;
    
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "supplier_referent_id")
    private SupplierReferent supplierReferent;
    
    /** Numero della polizza assicurativa */
    @NotBlank
    @Column(nullable = false)
    private String policyNumber;

    /** Descrizione o tipo di polizza */
    @NotBlank
    @Column(nullable = false)
    private String policyType;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expiryDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate paymentDate;

    @Column(name = "guaranteed_amount", precision = 15, scale = 2)
    private BigDecimal guaranteedAmount;

    @Column(length = 2000)
    private String notes;

    @OneToMany(mappedBy = "insurance", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Document> documents = new ArrayList<>();
}
