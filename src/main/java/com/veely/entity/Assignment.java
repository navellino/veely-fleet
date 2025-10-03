package com.veely.entity;

import com.veely.model.*;
import jakarta.persistence.*;
import lombok.*;
import com.veely.entity.Document;
import java.util.Set;

import java.time.LocalDate;
import java.time.LocalTime;

import org.springframework.format.annotation.DateTimeFormat;

@Entity
@Table(name = "assignments")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Assignment {

    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Rapporto di lavoro a cui questa assegnazione è legata */
    @ManyToOne(optional = false)
    private Employment employment;

    @ManyToOne(optional = false)
    private Vehicle vehicle;

    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate startDate;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime startTime;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate endDate;
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.TIME)
    private LocalTime endTime;

    @Enumerated(EnumType.STRING)
    private AssignmentStatus status;
    
    /** Cantiere di accollo per questa assegnazione. */
    /** Commessa di accollo per questa assegnazione. */
    @ManyToOne
    private Project accolloProject;
    
    /** Nota operativa per assegnazioni brevi (es. motivo utilizzo). */
    @Column(length = 255)
    private String note;
    
    /** Documenti collegati a questa assegnazione (es. verbali di consegna). */
    @OneToMany(mappedBy = "assignment", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Document> documents;
}

