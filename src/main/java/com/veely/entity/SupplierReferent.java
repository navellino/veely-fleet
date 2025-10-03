package com.veely.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;

import jakarta.persistence.*;
import lombok.*;

/**
 * Referente associato a un fornitore.
 */
@Entity
@Table(name = "supplier_referents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupplierReferent {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne
    @JoinColumn(name = "supplier_id")
    @JsonIgnore
    private Supplier supplier;

    private String name;
    private String phone;
    private String email;
}
