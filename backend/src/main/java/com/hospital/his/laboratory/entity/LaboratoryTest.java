package com.hospital.his.laboratory.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Catálogo de exámenes de laboratorio.
 *
 * CU-14:
 * Permite administrar los exámenes que pueden
 * incluirse en una orden médica.
 */
@Entity
@Table(
        name = "laboratory_tests",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_laboratory_test_code",
                        columnNames = "code"
                )
        },
        indexes = {
                @Index(
                        name = "idx_laboratory_test_code",
                        columnList = "code"
                ),
                @Index(
                        name = "idx_laboratory_test_name",
                        columnList = "name"
                ),
                @Index(
                        name = "idx_laboratory_test_active",
                        columnList = "active"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LaboratoryTest {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    /**
     * Código técnico único del examen.
     *
     * Ejemplos:
     *
     * HEM-001
     * GLU-001
     * PCR-001
     */
    @Column(
            nullable = false,
            unique = true,
            length = 30
    )
    private String code;

    /**
     * Nombre del examen mostrado en órdenes,
     * resultados y cobros.
     */
    @Column(
            nullable = false,
            length = 200
    )
    private String name;

    /**
     * Explicación administrativa o clínica
     * del examen.
     */
    @Column(
            length = 500
    )
    private String description;

    /**
     * Rango de referencia utilizado como guía
     * durante el registro del resultado.
     *
     * Ejemplos:
     *
     * 70 - 100
     * 4.0 - 5.5
     * Negativo
     */
    @Column(
            name = "reference_range",
            length = 200
    )
    private String referenceRange;

    /**
     * Unidad predeterminada del resultado.
     *
     * Ejemplos:
     *
     * mg/dL
     * g/dL
     * %
     * UI/L
     */
    @Column(
            name = "default_unit",
            length = 50
    )
    private String defaultUnit;

    /**
     * Precio base del examen.
     *
     * Debe ser mayor que cero.
     * PostgreSQL almacena hasta 8 enteros
     * y 2 decimales.
     */
    @Column(
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal price;

    /**
     * Eliminación lógica.
     *
     * Un examen inactivo:
     *
     * - No aparece al médico.
     * - No puede incluirse en nuevas órdenes.
     * - Permanece disponible en órdenes históricas.
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
     * Administrador que creó el examen.
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