package com.hospital.his.pharmacy.entity;

import com.hospital.his.medications.entity.Medication;
import com.hospital.his.prescriptions.entity.PrescriptionItem;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;

@Entity
@Table(
        name = "medication_dispensation_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_dispensation_prescription_item",
                        columnNames = {
                                "dispensation_id",
                                "prescription_item_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_dispensation_item_dispensation",
                        columnList = "dispensation_id"
                ),
                @Index(
                        name = "idx_dispensation_item_prescription_item",
                        columnList = "prescription_item_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicationDispensationItem {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    /**
     * Despacho al que pertenece este detalle.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "dispensation_id",
            nullable = false
    )
    private MedicationDispensation dispensation;

    /**
     * Ítem original incluido en la receta.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "prescription_item_id",
            nullable = false
    )
    private PrescriptionItem prescriptionItem;

    /**
     * Medicamento originalmente prescrito.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "original_medication_id",
            nullable = false
    )
    private Medication originalMedication;

    /**
     * Medicamento realmente entregado.
     *
     * Si no hubo sustitución, será igual al original.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "dispensed_medication_id",
            nullable = false
    )
    private Medication dispensedMedication;

    /**
     * Cantidad total definida originalmente
     * en la receta.
     */
    @Column(
            name = "prescribed_quantity",
            nullable = false
    )
    private Integer prescribedQuantity;

    /**
     * Cantidad pendiente antes de este despacho.
     */
    @Column(
            name = "pending_quantity_before",
            nullable = false
    )
    private Integer pendingQuantityBefore;

    /**
     * Cantidad entregada en esta operación.
     */
    @Column(
            name = "dispensed_quantity",
            nullable = false
    )
    private Integer dispensedQuantity;

    /**
     * Precio unitario copiado al momento del despacho.
     *
     * Esto conserva el precio histórico.
     */
    @Column(
            name = "unit_price",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal unitPrice;

    /**
     * Subtotal calculado por el backend.
     */
    @Column(
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal subtotal;

    /**
     * Existencia antes del descuento.
     */
    @Column(
            name = "stock_before",
            nullable = false
    )
    private Integer stockBefore;

    /**
     * Existencia después del descuento.
     */
    @Column(
            name = "stock_after",
            nullable = false
    )
    private Integer stockAfter;

    /**
     * Indica si se realizó una sustitución.
     */
    @Column(
            nullable = false
    )
    private Boolean substituted;

    /**
     * Justificación obligatoria cuando se sustituye
     * un medicamento.
     */
    @Column(
            name = "substitution_reason",
            length = 1000
    )
    private String substitutionReason;

    /**
     * Indica que el medicamento original era controlado.
     */
    @Column(
            name = "controlled_medication",
            nullable = false
    )
    private Boolean controlledMedication;

    /**
     * Autorización adicional para medicamentos
     * controlados.
     */
    @Column(
            name = "control_authorization",
            length = 200
    )
    private String controlAuthorization;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 30
    )
    private DispensationItemStatus status;

    @Version
    @Column(nullable = false)
    private Long version;
}