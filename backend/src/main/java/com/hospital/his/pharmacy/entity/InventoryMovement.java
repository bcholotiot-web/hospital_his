package com.hospital.his.pharmacy.entity;

import com.hospital.his.catalogs.entity.Branch;
import com.hospital.his.medications.entity.Medication;

import jakarta.persistence.*;
import lombok.*;

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
}