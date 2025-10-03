package com.veely.entity;

import com.veely.model.FullAddress;
import com.veely.model.ProjectStatus;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.List;

@Entity
@Table(name = "projects")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Project {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Codice identificativo della commessa */
    @Column(nullable = false, unique = true)
    private String code;
    
    /** Descrizione o denominazione della commessa */
    @Column(nullable = false)
    private String name;
    
    /** Codice Identificativo Gara (opzionale) */
    private String cig;

    /** Codice Unico di Progetto (opzionale) */
    private String cup;
    
    /** Responsabile del progetto */
    @ManyToOne
    @JoinColumn(name = "manager_id")
    private Employee manager;

    /** Data di inizio del progetto */
    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private LocalDate startDate;

    /** Data di fine del progetto */
    @org.springframework.format.annotation.DateTimeFormat(iso = org.springframework.format.annotation.DateTimeFormat.ISO.DATE)
    private LocalDate endDate;

    /** Stato del progetto */
    @Enumerated(EnumType.STRING)
    private ProjectStatus status;

    /** Indirizzo del progetto */
    @Embedded
    private FullAddress address;

    /** Contatti utili del progetto */
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProjectContact> contacts = new ArrayList<>();

    /** Documenti associati al progetto */
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Document> documents = new ArrayList<>();
    
    /** Polizze assicurative associate alla commessa */
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<Insurance> insurances = new ArrayList<>();
    
    /** Descrizione dei lavori da eseguire (note) */
    @Column(length = 2000)
    private String workDescription;

    /** Valore totale della commessa */
    private BigDecimal value;

    /** Importo dell'anticipazione ricevuta */
    private BigDecimal advanceAmount;

    /** Elenco delle fatture emesse */
    @OneToMany(mappedBy = "project", cascade = CascadeType.ALL, orphanRemoval = true)
    @Builder.Default
    private List<ProjectInvoice> invoices = new ArrayList<>();
}
