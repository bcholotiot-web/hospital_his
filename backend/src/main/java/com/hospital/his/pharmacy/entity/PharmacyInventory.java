package com.hospital.his.pharmacy.entity;

import com.hospital.his.catalogs.entity.Branch;
import com.hospital.his.medications.entity.Medication;
import jakarta.persistence.*;
import lombok.*;

import java.time.LocalDateTime;

@Entity
@Table(
        name = "pharmacy_inventory",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_pharmacy_inventory_branch_medication",
                        columnNames = {
                                "branch_id",
                                "medication_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_pharmacy_inventory_branch",
                        columnList = "branch_id"
                ),
                @Index(
                        name = "idx_pharmacy_inventory_medication",
                        columnList = "medication_id"
                ),
                @Index(
                        name = "idx_pharmacy_inventory_active",
                        columnList = "active"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PharmacyInventory {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    /**
     * Sucursal donde se encuentran almacenadas
     * las existencias.
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
     * Medicamento asociado con este registro
     * de inventario.
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
     * Cantidad disponible en la sucursal.
     *
     * Nunca debe quedar con un valor negativo.
     */
    @Column(
            name = "current_stock",
            nullable = false
    )
    private Integer currentStock;

    /**
     * Cantidad mínima permitida antes de mostrar
     * una alerta de reabastecimiento.
     */
    @Column(
            name = "minimum_stock",
            nullable = false
    )
    private Integer minimumStock;

    /**
     * Indica si el registro de inventario está activo.
     *
     * Permite realizar una eliminación lógica sin perder
     * el historial de movimientos.
     */
    @Column(
            nullable = false
    )
    private Boolean active;

    /**
     * Fecha de la última modificación del inventario.
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
     * Si dos operaciones intentan modificar el mismo
     * inventario, Hibernate detectará que la versión
     * ha cambiado.
     */
    @Version
    @Column(
            nullable = false
    )
    private Long version;
}