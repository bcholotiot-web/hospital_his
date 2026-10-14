package com.hospital.his.pharmacy.entity;

import com.hospital.his.catalogs.entity.Branch;
import com.hospital.his.medications.entity.Medication;

import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;

/**
 * Bitácora persistente de movimientos de inventario.
 *
 * Cada modificación en currentStock debe generar
 * un registro inmutable en esta tabla.
 */
@Entity
@Table(
        name = "inventory_movements",
        indexes = {
                @Index(
                        name =
                                "idx_inventory_movement_inventory",
                        columnList =
                                "inventory_id"
                ),
                @Index(
                        name =
                                "idx_inventory_movement_branch",
                        columnList =
                                "branch_id"
                ),
                @Index(
                        name =
                                "idx_inventory_movement_medication",
                        columnList =
                                "medication_id"
                ),
                @Index(
                        name =
                                "idx_inventory_movement_type",
                        columnList =
                                "movement_type"
                ),
                @Index(
                        name =
                                "idx_inventory_movement_created_at",
                        columnList =
                                "created_at"
                ),
                @Index(
                        name =
                                "idx_inventory_movement_active",
                        columnList =
                                "active"
                ),
                @Index(
                        name =
                                "idx_inventory_movement_reference_number",
                        columnList =
                                "reference_number"
                ),
                @Index(
                        name =
                                "idx_inventory_movement_branch_active_created",
                        columnList =
                                "branch_id, active, created_at"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class InventoryMovement {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    /**
     * Registro de inventario afectado.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "inventory_id",
            nullable = false
    )
    private PharmacyInventory inventory;

    /**
     * Sucursal almacenada directamente para facilitar
     * búsquedas y conservar contexto histórico.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "branch_id",
            nullable = false
    )
    private Branch branch;

    /**
     * Medicamento asociado con el movimiento.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "medication_id",
            nullable = false
    )
    private Medication medication;

    /**
     * Tipo funcional del movimiento.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "movement_type",
            nullable = false,
            length = 30
    )
    private InventoryMovementType movementType;

    /**
     * Stock existente antes de efectuar
     * el movimiento.
     */
    @Column(
            name = "previous_stock",
            nullable = false
    )
    private Integer previousStock;

    /**
     * Cantidad absoluta involucrada.
     *
     * Siempre se guarda como valor positivo.
     * La dirección se determina por movementType.
     */
    @Column(
            nullable = false
    )
    private Integer quantity;

    /**
     * Stock resultante después del movimiento.
     */
    @Column(
            name = "resulting_stock",
            nullable = false
    )
    private Integer resultingStock;

    /**
     * Costo por unidad registrado en el movimiento.
     *
     * Es obligatorio para COMPRA.
     * Puede ser null para otros movimientos.
     */
    @Column(
            name = "unit_cost",
            precision = 12,
            scale = 2
    )
    private BigDecimal unitCost;

    /**
     * Costo total calculado:
     *
     * quantity * unitCost
     *
     * Es obligatorio para COMPRA.
     * Puede ser null para otros movimientos.
     */
    @Column(
            name = "total_cost",
            precision = 14,
            scale = 2
    )
    private BigDecimal totalCost;


    /**
     * Motivo funcional del movimiento.
     *
     * Ejemplos:
     *
     * Compra a proveedor.
     * Ajuste por conteo físico.
     * Despacho de receta RX-2026-001.
     */
    @Column(
            nullable = false,
            length = 500
    )
    private String reason;

    /**
     * Tipo de entidad que originó el movimiento.
     *
     * Ejemplos:
     *
     * PHARMACY_INVENTORY
     * DISPENSATION
     * MANUAL_ADJUSTMENT
     */
    @Column(
            name = "reference_type",
            length = 50
    )
    private String referenceType;

    /**
     * Identificador de la entidad que originó
     * el movimiento.
     *
     * Puede contener el ID del despacho.
     */
    @Column(
            name = "reference_id"
    )
    private Long referenceId;

    /**
     * Número de referencia funcional ingresado
     * por el usuario.
     *
     * Ejemplos:
     *
     * FAC-2026-001
     * DEV-2026-004
     * VENTA-000125
     * REC-2026-010
     *
     * No sustituye referenceId, porque referenceId
     * continúa relacionando entidades internas.
     */
    @Column(
            name = "reference_number",
            length = 100
    )
    private String referenceNumber;


    /**
     * Usuario responsable de la operación.
     */
    @Column(
            name = "created_by",
            nullable = false,
            length = 100
    )
    private String createdBy;

    /**
     * Fecha exacta del movimiento.
     */
    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    /**
     * Indica si el movimiento está activo
     * administrativamente.
     *
     * Desactivar un movimiento no revierte
     * automáticamente el stock.
     */
    @Builder.Default
    @Column(
            nullable = false
    )
    private Boolean active = true;

    /**
     * Fecha de la última actualización administrativa.
     */
    @Column(
            name = "updated_at",
            nullable = false
    )
    private LocalDateTime updatedAt;

    /**
     * Usuario responsable de la última
     * actualización administrativa.
     */
    @Column(
            name = "updated_by",
            nullable = false,
            length = 100
    )
    private String updatedBy;

    /**
     * Control optimista para cambios de estado.
     */
    @Version
    @Column(
            nullable = false
    )
    private Long version;

}