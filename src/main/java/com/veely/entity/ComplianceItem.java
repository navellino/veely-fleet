package com.veely.entity;

import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.util.Set;
import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Table(name = "compliance_items")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ComplianceItem {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "category_id", nullable = false)
    private ComplianceCategory category;

    @ManyToOne
    @JoinColumn(name = "employee_id")
    private Employee employee;

    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;

    @Column(length = 255)
    private String description;
    
    /** Data di svolgimento visita/corso */
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate visitDate;

    /** Periodicità dell'adempimento in anni */
    private Integer periodicity;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    @Column(nullable = false)
    private LocalDate dueDate;

    @OneToMany(mappedBy = "complianceItem", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Document> documents;
}
