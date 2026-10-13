package com.hospital.his.users.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Relación entre un rol y un permiso.
 *
 * Se utiliza una entidad explícita en lugar
 * de @ManyToMany porque necesitamos:
 *
 * - Estado lógico.
 * - Auditoría.
 * - Reactivación.
 * - Control de concurrencia.
 */
@Entity
@Table(
        name = "role_permissions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_role_permission",
                        columnNames = {
                                "role_id",
                                "permission_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_role_permission_role",
                        columnList = "role_id"
                ),
                @Index(
                        name = "idx_role_permission_permission",
                        columnList = "permission_id"
                ),
                @Index(
                        name = "idx_role_permission_active",
                        columnList = "active"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RolePermission {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    /**
     * Rol al que se asigna el permiso.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "role_id",
            nullable = false
    )
    private Role role;

    /**
     * Permiso asignado.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "permission_id",
            nullable = false
    )
    private Permission permission;

    /**
     * Estado lógico de la asignación.
     */
    @Column(nullable = false)
    private Boolean active;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "created_by",
            nullable = false,
            updatable = false,
            length = 100
    )
    private String createdBy;

    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    @Column(
            name = "updated_by",
            nullable = false,
            length = 100
    )
    private String updatedBy;

    @Version
    @Column(nullable = false)
    private Long version;
}