package com.hospital.his.users.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Catálogo de roles del sistema.
 *
 * CU-14:
 * Permite administrar roles mediante creación,
 * actualización, activación y desactivación lógica.
 */
@Entity
@Table(
        name = "roles",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_role_name",
                        columnNames = "name"
                )
        },
        indexes = {
                @Index(
                        name = "idx_role_name",
                        columnList = "name"
                ),
                @Index(
                        name = "idx_role_active",
                        columnList = "active"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Role {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    /**
     * Nombre técnico utilizado por Spring Security.
     *
     * Ejemplos:
     *
     * ADMIN
     * PACIENTE
     * MEDICO
     * FARMACIA
     */
    @Column(
            nullable = false,
            unique = true,
            length = 50
    )
    private String name;

    /**
     * Descripción administrativa del rol.
     */
    @Column(
            nullable = false,
            length = 250
    )
    private String description;

    /**
     * Estado lógico del rol.
     *
     * Un rol inactivo no puede asignarse a nuevos
     * usuarios, pero permanece en los registros
     * históricos.
     */
    @Column(
            nullable = false
    )
    private Boolean active;

    /**
     * Fecha de creación.
     */
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    /**
     * Administrador que creó el rol.
     */
    @Column(
            name = "created_by",
            nullable = false,
            updatable = false,
            length = 100
    )
    private String createdBy;

    /**
     * Fecha de última actualización.
     */
    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    /**
     * Usuario que realizó la última modificación.
     */
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
    @Column(
            nullable = false
    )
    private Long version;
}