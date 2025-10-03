package com.veely.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;

import org.springframework.format.annotation.DateTimeFormat;

/**
 * Scheda carburante associata a un veicolo o a un dipendente.
 */
@Entity
@Table(name = "fuel_cards")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class FuelCard {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Numero della carta carburante */
    @Column(nullable = false, unique = true)
    private String cardNumber;

    /** Data di scadenza della carta */
    @Column(name = "expiry_date")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate expiryDate;

    /** Fornitore della carta carburante */
    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;

    /** Dipendente assegnatario della carta */
    @ManyToOne(optional = true)
    @JoinColumn(name = "employee_id", nullable = true)
    private Employee employee;

    /** Veicolo associato alla carta */
    @OneToOne(optional = true)
    @JoinColumn(name = "vehicle_id", nullable = true)
    private Vehicle vehicle;

    /** Plafond mensile o totale della carta */
    private BigDecimal plafond;
    
    /** Stato della carta carburante */
    @Builder.Default
    @Column(nullable = false)
    private boolean active = true;
    
    /**
     * Aggiorna lo stato della carta in base alla data di scadenza.
     * La carta è considerata attiva se non ha una data di scadenza oppure
     * se la scadenza è successiva o uguale alla data odierna.
     */
    public void updateStatus() {
        this.active = this.expiryDate == null || !this.expiryDate.isBefore(LocalDate.now());
    }

    @PrePersist
    @PreUpdate
    @PostLoad
    private void applyExpiryRule() {
        updateStatus();
    }
}
