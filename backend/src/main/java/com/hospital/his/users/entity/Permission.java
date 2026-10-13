package com.hospital.his.users.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Permiso granular del sistema.
 *
 * Ejemplos:
 *
 * SPECIALTY_READ
 * SPECIALTY_CREATE
 * MEDICATION_UPDATE
 * INVENTORY_MOVEMENT_READ
 */
@Entity
@Table(
        name = "permissions",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_permission_code",
                        columnNames = "code"
                )
        },
        indexes = {
                @Index(
                        name = "idx_permission_code",
                        columnList = "code"
                ),
                @Index(
                        name = "idx_permission_module",
                        columnList = "module"
                ),
                @Index(
                        name = "idx_permission_active",
                        columnList = "active"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Permission {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    /**
     * Código técnico utilizado como autoridad
     * dentro de Spring Security.
     */
    @Column(
            nullable = false,
            unique = true,
            length = 100
    )
    private String code;

    /**
     * Nombre legible del permiso.
     */
    @Column(
            nullable = false,
            length = 150
    )
    private String name;

    /**
     * Módulo al que pertenece.
     *
     * Ejemplos:
     *
     * SPECIALTY
     * BRANCH
     * MEDICATION
     * SECURITY
     */
    @Column(
            nullable = false,
            length = 50
    )
    private String module;

    @Column(
            nullable = false,
            length = 500
    )
    private String description;

    /**
     * Estado lógico.
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

    /**
     * Control de concurrencia optimista.
     */
    @Version
    @Column(nullable = false)
    private Long version;
}