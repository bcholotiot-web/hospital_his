package com.hospital.his.prescriptions.entity;

import com.hospital.his.medications.entity.Medication;
import jakarta.persistence.*;
import lombok.*;

@Entity
@Table(
        name = "prescription_items",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_prescription_item_medication",
                        columnNames = {
                                "prescription_id",
                                "medication_id"
                        }
                )
        },
        indexes = {
                @Index(
                        name = "idx_prescription_item_prescription",
                        columnList = "prescription_id"
                ),
                @Index(
                        name = "idx_prescription_item_medication",
                        columnList = "medication_id"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PrescriptionItem {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    /**
     * Receta a la que pertenece el medicamento.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "prescription_id",
            nullable = false
    )
    private Prescription prescription;

    /**
     * Medicamento seleccionado desde el catálogo.
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
     * Dosis prescrita.
     *
     * Ejemplo:
     * 500 mg
     */
    @Column(
            nullable = false,
            length = 100
    )
    private String dosage;

    /**
     * Frecuencia de administración.
     *
     * Ejemplo:
     * Cada 8 horas
     */
    @Column(
            nullable = false,
            length = 150
    )
    private String frequency;

    /**
     * Duración del tratamiento.
     *
     * Ejemplo:
     * 5 días
     */
    @Column(
            nullable = false,
            length = 100
    )
    private String duration;

    /**
     * Cantidad total prescrita.
     */
    @Column(
            name = "prescribed_quantity",
            nullable = false
    )
    private Integer prescribedQuantity;

    /**
     * Cantidad acumulada entregada por Farmacia.
     *
     * Se inicia en cero.
     */
    @Column(
            name = "dispensed_quantity",
            nullable = false
    )
    private Integer dispensedQuantity;

    /**
     * Indicaciones especiales opcionales.
     */
    @Column(
            name = "special_instructions",
            length = 1000
    )
    private String specialInstructions;

    /**
     * Indica si el medicamento de la receta
     * es controlado.
     *
     * Se copia desde el catálogo al crear la receta
     * para conservar la clasificación histórica.
     */
    @Column(
            name = "controlled_medication",
            nullable = false
    )
    private Boolean controlledMedication;

    /**
     * Estado lógico del elemento.
     */
    @Column(nullable = false)
    private Boolean active;

    /**
     * Control de concurrencia optimista.
     */
    @Version
    @Column(nullable = false)
    private Long version;
}