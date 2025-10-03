package com.veely.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

@Entity
@Table(name = "expense_items")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class ExpenseItem {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "expense_report_id")
    private ExpenseReport expenseReport;

    private LocalDate date;

    private String description;

    private BigDecimal amount;
    
    private String invoiceNumber;

    @ManyToOne
    private Supplier supplier;

    @ManyToOne
    private Project project;

    private String note;
}
