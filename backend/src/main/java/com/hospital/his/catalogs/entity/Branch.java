package com.hospital.his.catalogs.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Catálogo de sucursales o sedes del hospital.
 *
 * CU-14:
 * Permite crear, actualizar, activar y desactivar
 * sedes sin eliminar información histórica.
 */
@Entity
@Table(
        name = "branches",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_branch_name",
                        columnNames = "name"
                )
        },
        indexes = {
                @Index(
                        name = "idx_branch_name",
                        columnList = "name"
                ),
                @Index(
                        name = "idx_branch_active",
                        columnList = "active"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Branch {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    /**
     * Nombre único de la sucursal.
     *
     * El servicio normaliza espacios y lo convierte
     * a mayúsculas antes de almacenarlo.
     */
    @Column(
            nullable = false,
            unique = true,
            length = 100
    )
    private String name;

    /**
     * Número telefónico de ocho dígitos.
     *
     * El campo es opcional.
     */
    @Column(
            length = 8
    )
    private String phone;

    /**
     * Dirección física de la sucursal.
     *
     * El campo es opcional y admite hasta
     * 500 caracteres.
     */
    @Column(
            length = 500
    )
    private String address;

    /**
     * Descripción administrativa de la sucursal.
     *
     * El campo es opcional y admite hasta
     * 250 caracteres.
     */
    @Column(
            length = 250
    )
    private String description;

    /**
     * Estado lógico de la sede.
     *
     * Una sede inactiva:
     *
     * - Permanece en PostgreSQL.
     * - No aparece al agendar citas.
     * - No admite nuevas asignaciones activas.
     * - Conserva sus citas e historial anteriores.
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
     * Administrador que creó la sucursal.
     */
    @Column(
            name = "created_by",
            nullable = false,
            updatable = false,
            length = 100
    )
    private String createdBy;

    /**
     * Fecha de última modificación.
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