package com.veely.entity;

import com.veely.model.*;
import com.veely.entity.VehicleBooking;
import jakarta.persistence.*;
import jakarta.validation.constraints.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.Set;

import org.hibernate.annotations.Formula;
import org.springframework.format.annotation.DateTimeFormat;

/**
 * Veicolo aziendale (auto o camion), sia di proprietà sia in leasing.
 * Campi ereditati dal tuo modello precedente: canoni, km contrattuali,
 * fuel-card, telepass, ecc.:contentReference[oaicite:2]{index=2}
 */
@Entity
@Table(name = "vehicles")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class Vehicle {

    // -----------------------------------
    // Identificativi e Descrittivi
    // -----------------------------------
    @Id @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /** Targa – univoca. */
    @NotBlank(message = "Targa obbligatoria")
    @Pattern(regexp = "^[A-Z]{2}[0-9]{3}[A-Z]{2}$", message = "Formato targa non valido")
    @Column(nullable = false, unique = true, length = 20)
    private String plate;

    /** Numero telaio (VIN). */
    @Column(unique = true, length = 50)
    private String chassisNumber;

    private String brand;         // Marca
    private String model;         // Modello
    private String series;        // Allestimento / serie
    
    @NotNull
    @Min(1900)
    @Max(2100)
    private Integer year;         // Anno di immatricolazione (solo cifra)

    @Enumerated(EnumType.STRING)
    private VehicleType type;     // CAR / TRUCK

    @Enumerated(EnumType.STRING)
    private FuelType fuelType;    // Benzina, Diesel, ...

    @Enumerated(EnumType.STRING)
    private OwnershipType ownership; // OWNED / LEASED

    // -----------------------------------
    // Contratto di leasing / proprietà
    // -----------------------------------
    
    /** Società di leasing / concessionario */
    @ManyToOne
    @JoinColumn(name = "supplier_id")
    private Supplier supplier;
    
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate registrationDate;   // Data immatricolazione
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate contractStartDate;  // Inizio leasing
    
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate contractEndDate;    // Fine leasing
    
    private Integer contractDuration;     // Durata (mesi)
    private Integer contractualKm;        // Km previsti dal contratto

    private BigDecimal financialFee;      // Canone finanziario
    private BigDecimal assistanceFee;     // Canone assistenza
    private BigDecimal totalFee;          // Canone complessivo

    private BigDecimal annualFringeBenefit;   // Fringe benefit annuo
    private BigDecimal monthlyFringeBenefit;  // Fringe benefit mensile

    // -----------------------------------
    // Status operativi
    // -----------------------------------
    @Enumerated(EnumType.STRING)
    private VehicleStatus status = VehicleStatus.IN_SERVICE;

    private Integer currentMileage;       // Km attuali
    // -----------------------------------
    // Accessori gestionali
    // -----------------------------------

    @OneToOne(mappedBy = "vehicle")
    private FuelCard fuelCard;
    
    private String telepass;              // Telepass associato
    
    /** Scadenza polizza assicurativa (in precedenza telepassExpiryDate). */
   // @Future(message = "Data scadenza deve essere futura")
    @Column(name = "telepass_expiry_date")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate insuranceExpiryDate;

    /** Scadenza bollo auto. */
  //  @Future(message = "Data scadenza deve essere futura")
    @DateTimeFormat(iso = DateTimeFormat.ISO.DATE)
    private LocalDate carTaxExpiryDate;
    
    /** Numero totale di veicoli attualmente in servizio. */
    @Formula("(SELECT COUNT(*) FROM vehicles v WHERE v.status = 'IN_SERVICE')")
    private Long inServiceCount;

    /** Numero totale di veicoli assegnati. */
    @Formula("(SELECT COUNT(DISTINCT a.vehicle_id) FROM assignments a WHERE a.status = 'ASSIGNED')")
    private Long assignedCount;
    
    private String imagePath;             // Path immagine del veicolo

    // -----------------------------------
    // Relazioni
    // -----------------------------------
    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Assignment> assignments;
    
    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<VehicleBooking> bookings;

    @OneToMany(mappedBy = "vehicle", cascade = CascadeType.ALL, orphanRemoval = true)
    private Set<Document> documents;
}