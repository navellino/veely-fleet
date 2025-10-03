package com.veely.entity;

import jakarta.persistence.*;
import lombok.*;

/**
 * Override for a specific user and permission authority. When allow is true the
 * authority is granted even if not present in role permissions. When false the
 * authority is removed even if granted by roles.
 */
@Entity
@Table(name = "user_permission_overrides")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class UserPermissionOverride {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(optional = false)
    @JoinColumn(name = "user_id")
    private Employee user;

    @Column(nullable = false, length = 100)
    private String authority;

    @Column(nullable = false)
    private boolean allow;
}
