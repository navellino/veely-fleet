package com.veely.entity;

import com.veely.model.*;
import jakarta.persistence.*;
import lombok.*;
import com.veely.entity.ComplianceItem;
import com.veely.entity.Contract;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Table(name = "documents")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Document {

	@Id @GeneratedValue
    private Long id;

    @ManyToOne @JoinColumn(name="employee_id")
    private Employee employee;

    @ManyToOne @JoinColumn(name="employment_id")
    private Employment employment;

    @ManyToOne @JoinColumn(name="vehicle_id")
    private Vehicle vehicle;

    @ManyToOne @JoinColumn(name="assignment_id")
    private Assignment assignment;
    
    @ManyToOne
    @JoinColumn(name = "project_id")
    private Project project;
    
    @ManyToOne
    @JoinColumn(name = "admin_document_id")
    private AdminDocument adminDocument;
    
    @ManyToOne
    @JoinColumn(name="expense_item_id")
    private ExpenseItem expenseItem;
    
    @ManyToOne
    @JoinColumn(name = "project_insurance_id")
    private Insurance insurance;

    @ManyToOne
    @JoinColumn(name="correspondence_id")
    private Correspondence correspondence;
    
    @ManyToOne
    @JoinColumn(name="maintenance_id")
    private Maintenance maintenance;
    
    @ManyToOne
    @JoinColumn(name="compliance_item_id")
    private ComplianceItem complianceItem;
    
    @ManyToOne
    @JoinColumn(name = "contract_id")
    private Contract contract;
    
    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;
    
    @Enumerated(EnumType.STRING)
    @Column(name = "type", length = 32, nullable = false)
    private DocumentType type;

    private String path;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate issueDate;
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expiryDate;
}
