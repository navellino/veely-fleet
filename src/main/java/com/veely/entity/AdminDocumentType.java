package com.veely.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "admin_type_doc")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class AdminDocumentType {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;
}