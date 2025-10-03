package com.veely.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Association between an {@link EmployeeRole} and a single permission string
 * in the format PAGE:<CODE>:<READ|WRITE>.
 */
@Entity
@Table(name = "role_permissions")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermission {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "role_id")
    private EmployeeRole role;

    /**
     * Permission authority constant, e.g. "PAGE:FLEET:READ".
     */
    @Column(nullable = false, length = 100)
    private String authority;
}