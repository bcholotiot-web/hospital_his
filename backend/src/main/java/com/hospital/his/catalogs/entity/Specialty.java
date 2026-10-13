package com.hospital.his.catalogs.entity;

import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

/**
 * Catálogo de especialidades médicas.
 *
 * CU-14:
 * Permite crear, actualizar, activar y desactivar
 * especialidades sin eliminar registros históricos.
 */
@Entity
@Table(name = "specialties",
        uniqueConstraints = {@UniqueConstraint(name = "uk_specialty_name", columnNames = "name")},
        indexes = {@Index(name = "idx_specialty_name", columnList = "name"), @Index(name = "idx_specialty_active", columnList = "active")})
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Specialty {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Nombre único de la especialidad.
     *
     * El servicio normaliza espacios y convierte
     * el valor a mayúsculas antes de guardar.
     */
    @Column(nullable = false, unique = true, length = 100)
    private String name;

    /**
     * Descripción funcional de la especialidad.
     */
    @Column(nullable = false, length = 500)
    private String description;

    /**
     * Estado lógico.
     *
     * Una especialidad inactiva permanece en
     * PostgreSQL, pero no aparece en citas,
     * usuarios o configuraciones activas.
     */
    @Column(nullable = false)
    private Boolean active;

    /**
     * Fecha de creación del registro.
     */
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;

    /**
     * Administrador que creó la especialidad.
     */
    @Column(name = "created_by", nullable = false, updatable = false, length = 100)
    private String createdBy;

    /**
     * Fecha de la última actualización.
     */
    @Column(name = "updated_at", nullable = false)
    private LocalDateTime updatedAt;

    /**
     * Usuario que realizó la última modificación.
     */
    @Column(name = "updated_by", nullable = false, length = 100)
    private String updatedBy;

    /**
     * Control de concurrencia optimista.
     *
     * El frontend enviará la versión que recibió.
     * Si otra persona modificó el registro, la
     * actualización será rechazada.
     */
    @Version
    @Column(nullable = false)
    private Long version;
}