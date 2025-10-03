package com.veely.entity;

import jakarta.persistence.*;
import lombok.*;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

@Entity
@Table(name = "admin_document")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDocument {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "type_id")
    private AdminDocumentType type;

    private String documentNumber;

    @ManyToOne
    @JoinColumn(name = "authority_id")
    private PublicAuthority authority;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate issueDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expiryDate;

    @ManyToOne
    @JoinColumn(name = "responsible_id", nullable = true)
    private Employee responsible;

    @Column(columnDefinition = "TEXT")
    private String notes;

    private Integer noticeDays;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate lastReminderDate;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate nextCheckDate;
}
