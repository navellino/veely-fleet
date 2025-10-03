package com.veely.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "document_categories")
@Getter @Setter @NoArgsConstructor @AllArgsConstructor @Builder
public class DocumentCategory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String name;
}
