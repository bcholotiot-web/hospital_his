package com.hospital.his.laboratorypayment.entity;

import com.hospital.his.laboratory.entity.LaboratoryOrder;
import com.hospital.his.payments.entity.PaymentMethod;
import com.hospital.his.payments.entity.PaymentStatus;
import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(
        name = "laboratory_payments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_laboratory_payment_idempotency",
                        columnNames = "idempotency_key"
                ),
                @UniqueConstraint(
                        name = "uk_laboratory_payment_transaction",
                        columnNames = "transaction_number"
                )
        },
        indexes = {
                @Index(
                        name = "idx_laboratory_payment_order",
                        columnList = "laboratory_order_id"
                ),
                @Index(
                        name = "idx_laboratory_payment_status",
                        columnList = "status"
                ),
                @Index(
                        name = "idx_laboratory_payment_cashier",
                        columnList = "cashier_username"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LaboratoryPayment {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @ManyToOne(
            fetch = FetchType.LAZY,
            optional = false
    )
    @JoinColumn(
            name = "laboratory_order_id",
            nullable = false
    )
    private LaboratoryOrder laboratoryOrder;

    @Column(
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal amount;

    @Column(
            nullable = false,
            length = 10
    )
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(
            name = "payment_method",
            nullable = false,
            length = 20
    )
    private PaymentMethod paymentMethod;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private PaymentStatus status;

    /*
     * Solo se utiliza cuando el pago es en efectivo.
     */
    @Column(
            name = "received_amount",
            precision = 10,
            scale = 2
    )
    private BigDecimal receivedAmount;

    /*
     * Solo se utiliza cuando el pago es en efectivo.
     */
    @Column(
            name = "change_amount",
            precision = 10,
            scale = 2
    )
    private BigDecimal changeAmount;

    /*
     * Únicamente se almacenan los últimos cuatro
     * dígitos. Nunca se almacena el número completo.
     */
    @Column(
            name = "card_last_four",
            length = 4
    )
    private String cardLastFour;

    @Column(
            name = "transaction_number",
            length = 40
    )
    private String transactionNumber;

    /*
     * Referencia producida por la pasarela simulada.
     */
    @Column(
            name = "gateway_reference",
            length = 100
    )
    private String gatewayReference;

    /*
     * Evita ejecutar dos veces el mismo intento
     * de pago.
     */
    @Column(
            name = "idempotency_key",
            nullable = false,
            unique = true
    )
    private UUID idempotencyKey;

    /*
     * Se obtiene desde el JWT mediante
     * authentication.getName().
     */
    @Column(
            name = "cashier_username",
            nullable = false,
            length = 100
    )
    private String cashierUsername;

    @Column(
            name = "failure_code",
            length = 50
    )
    private String failureCode;

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
            name = "processed_at"
    )
    private LocalDateTime processedAt;

    @Version
    @Column(nullable = false)
    private Long version;
}