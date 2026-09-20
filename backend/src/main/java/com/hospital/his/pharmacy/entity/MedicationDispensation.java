package com.hospital.his.pharmacy.entity;

import com.hospital.his.catalogs.entity.Branch;
import com.hospital.his.payments.entity.PaymentMethod;
import com.hospital.his.payments.entity.PaymentStatus;
import com.hospital.his.prescriptions.entity.Prescription;
import com.hospital.his.users.entity.User;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Entity
@Table(
        name = "medication_dispensations",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_medication_dispensation_number",
                        columnNames = "dispensation_number"
                ),
                @UniqueConstraint(
                        name = "uk_medication_dispensation_idempotency",
                        columnNames = "idempotency_key"
                ),
                @UniqueConstraint(
                        name = "uk_medication_dispensation_transaction",
                        columnNames = "transaction_number"
                )
        },
        indexes = {
                @Index(
                        name = "idx_dispensation_prescription",
                        columnList = "prescription_id"
                ),
                @Index(
                        name = "idx_dispensation_branch",
                        columnList = "branch_id"
                ),
                @Index(
                        name = "idx_dispensation_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_dispensation_pharmacy_user",
                        columnList = "pharmacy_username"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MedicationDispensation {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    /**
     * Número único y visible del despacho.
     *
     * Ejemplo:
     * DSP-01A2B3C4D5E6
     */
    @Column(
            name = "dispensation_number",
            nullable = false,
            length = 40
    )
    private String dispensationNumber;

    /**
     * Receta atendida por este despacho.
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
     * Paciente de la receta.
     *
     * Se copia para facilitar trazabilidad histórica.
     */
    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "patient_user_id",
            nullable = false
    )
    private User patient;

    /**
     * Sucursal donde se realizó el despacho.
     *
     * Se obtiene desde el usuario autenticado.
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
     * Estado general del despacho.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private DispensationStatus status;

    /**
     * Monto total calculado por el backend.
     */
    @Column(
            name = "total_amount",
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal totalAmount;

    /**
     * Moneda utilizada en la operación.
     */
    @Column(
            nullable = false,
            length = 3
    )
    private String currency;

    /**
     * Método de pago utilizado en Farmacia.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "payment_method",
            nullable = false,
            length = 20
    )
    private PaymentMethod paymentMethod;

    /**
     * Estado del cobro efectuado en Farmacia.
     */
    @Enumerated(EnumType.STRING)
    @Column(
            name = "payment_status",
            nullable = false,
            length = 20
    )
    private PaymentStatus paymentStatus;

    /**
     * Importe recibido para pagos en efectivo.
     */
    @Column(
            name = "received_amount",
            precision = 10,
            scale = 2
    )
    private BigDecimal receivedAmount;

    /**
     * Cambio entregado para pagos en efectivo.
     */
    @Column(
            name = "change_amount",
            precision = 10,
            scale = 2
    )
    private BigDecimal changeAmount;

    /**
     * Solo se almacenan los últimos cuatro dígitos.
     *
     * Nunca se almacena el número completo ni el CVV.
     */
    @Column(
            name = "card_last_four",
            length = 4
    )
    private String cardLastFour;

    /**
     * Número generado por el sistema o la pasarela.
     */
    @Column(
            name = "transaction_number",
            length = 100
    )
    private String transactionNumber;

    /**
     * Referencia segura generada por la pasarela.
     */
    @Column(
            name = "gateway_reference",
            length = 150
    )
    private String gatewayReference;

    /**
     * Evita procesar dos veces la misma solicitud.
     */
    @Column(
            name = "idempotency_key",
            nullable = false,
            updatable = false
    )
    private UUID idempotencyKey;

    /**
     * Usuario de Farmacia obtenido desde el JWT.
     */
    @Column(
            name = "pharmacy_username",
            nullable = false,
            length = 100
    )
    private String pharmacyUsername;

    /**
     * Código de error o rechazo de pago.
     */
    @Column(
            name = "failure_code",
            length = 50
    )
    private String failureCode;

    /**
     * Descripción de un error o rechazo.
     */
    @Column(
            name = "failure_message",
            length = 500
    )
    private String failureMessage;

    @Column(
            name = "created_at",
            nullable = false,
            updatable = false
    )
    private LocalDateTime createdAt;

    @Column(
            name = "confirmed_at"
    )
    private LocalDateTime confirmedAt;

    /**
     * Detalles de medicamentos entregados.
     */
    @OneToMany(
            mappedBy = "dispensation",
            cascade = CascadeType.ALL,
            orphanRemoval = true
    )
    @Builder.Default
    private List<MedicationDispensationItem> items =
            new ArrayList<>();

    /**
     * Control de concurrencia optimista.
     */
    @Version
    @Column(nullable = false)
    private Long version;
}