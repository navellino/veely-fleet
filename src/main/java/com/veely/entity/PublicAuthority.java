package com.veely.entity;

import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(name = "public_authority")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PublicAuthority {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, unique = true)
    private String name;
}
