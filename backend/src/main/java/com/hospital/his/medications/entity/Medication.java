package com.hospital.his.medications.entity;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;



@Entity
@Table(
        name = "medications",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_medication_code",
                        columnNames = "code"
                )
        },
        indexes = {
                @Index(
                        name = "idx_medication_name",
                        columnList = "name"
                ),
                @Index(
                        name = "idx_medication_active",
                        columnList = "active"
                ),
                @Index(
                        name = "idx_medication_controlled",
                        columnList = "controlled"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Medication {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    /**
     * Código único del medicamento.
     *
     * Ejemplos:
     * MED-001
     * PAR-500-TAB
     * AMX-500-CAP
     */
    @Column(nullable = false, length = 40)
    private String code;

    /**
     * Nombre comercial o genérico mostrado
     * en el catálogo.
     */
    @Column(nullable = false, length = 200)
    private String name;

    /**
     * Principio activo del medicamento.
     *
     * Este campo será útil para mostrar información
     * farmacológica y registrar sustituciones.
     */
    @Column(name = "active_ingredient", nullable = false, length = 200)
    private String activeIngredient;

    /**
     * Concentración del medicamento.
     *
     * Ejemplos:
     * 500 mg
     * 250 mg/5 mL
     * 10 mg/mL
     */
    @Column(nullable = false, length = 100)
    private String concentration;

    /**
     * Presentación farmacéutica.
     *
     * Ejemplos:
     * Tableta
     * Cápsula
     * Jarabe
     * Ampolla
     */
    @Column(nullable = false, length = 100)
    private String presentation;

    /**
     * Unidad utilizada para controlar inventario
     * y despacho del medicamento.
     *
     * Ejemplos:
     *
     * TABLETA
     * CÁPSULA
     * FRASCO
     * AMPOLLA
     * MILILITRO
     *
     * CU-14 establece que la unidad es obligatoria
     * y admite un máximo de 50 caracteres.
     */
    @Column(
            nullable = false,
            length = 50
    )
    private String unit;

    /**
     * Descripción obligatoria del medicamento.
     */
    @Column(
            nullable = false,
            length = 1000
    )
    private String description;

    /**
     * Precio unitario utilizado para calcular
     * el monto del despacho.
     *
     * Este precio será copiado al detalle del despacho
     * para conservar el valor histórico.
     */
    @Column(name = "unit_price", nullable = false, precision = 10, scale = 2)
    private BigDecimal unitPrice;

    /**
     * Indica si el medicamento requiere controles
     * adicionales para su despacho.
     */
    @Column(nullable = false)
    private Boolean controlled;

    /**
     * Eliminación lógica.
     *
     * Un medicamento inactivo continúa en PostgreSQL,
     * pero no aparece para generar nuevas recetas.
     */
    @Column(nullable = false)
    private Boolean active;

    /**
     * Fecha de creación del medicamento.
     */
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    /**
     * Administrador que creó el medicamento.
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
     *
     * Hibernate incrementa este valor cada vez
     * que se actualiza el medicamento.
     */
    @Version
    @Column(nullable = false)
    private Long version;
}