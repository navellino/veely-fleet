package com.veely.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "document_types")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentTypeEntity {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String code;

    private String description;

    @ManyToOne
    @JoinColumn(name = "category_id")
    private DocumentCategory category;
}
