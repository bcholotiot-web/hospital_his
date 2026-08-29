package com.hospital.his.payments.entity;

import com.hospital.his.appointments.entity.Appointment;
import jakarta.persistence.*;
import lombok.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;
import java.math.BigDecimal;

@Entity
@Table(
        name = "payments",
        uniqueConstraints = {
                @UniqueConstraint(
                        name = "uk_payment_idempotency_key",
                        columnNames = "idempotency_key"
                ),
                @UniqueConstraint(
                        name = "uk_payment_transaction_number",
                        columnNames = "transaction_number"
                )
        }
)
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Payment {

    @Id
    @GeneratedValue(
            strategy = GenerationType.IDENTITY
    )
    private Long id;

    @ManyToOne
    @JoinColumn(
            name = "appointment_id",
            nullable = false
    )
    private Appointment appointment;

    @Column(
            nullable = false,
            precision = 10,
            scale = 2
    )
    private BigDecimal amount;

    @Column(
            nullable = false,
            length = 3
    )
    private String currency;

    @Enumerated(EnumType.STRING)
    @Column(
            nullable = false,
            length = 20
    )
    private PaymentStatus status;

    /*
     * Identificador enviado por el frontend para
     * evitar doble cobro por solicitudes repetidas.
     */
    @Column(
            name = "idempotency_key",
            nullable = false,
            updatable = false
    )
    private UUID idempotencyKey;

    /*
     * Número generado por la pasarela o por el
     * simulador de pagos.
     */
    @Column(
            name = "transaction_number",
            length = 100
    )
    private String transactionNumber;

    /*
     * Token temporal o referencia generada por la
     * pasarela. Nunca debe contener el número
     * completo de la tarjeta.
     */
    @Column(
            name = "gateway_reference",
            length = 150
    )
    private String gatewayReference;

    /*
     * Solo se conservan los últimos cuatro dígitos
     * para mostrar el método utilizado.
     */
    @Column(
            name = "card_last_four",
            length = 4
    )
    private String cardLastFour;

    @Column(
            name = "cardholder_name",
            length = 100
    )
    private String cardholderName;

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

    @Enumerated(EnumType.STRING)
    @Column(
            name = "payment_method",
            nullable = false,
            length = 20
    )
    private PaymentMethod paymentMethod;

    @Column(
            name = "received_amount",
            precision = 10,
            scale = 2
    )
    private BigDecimal receivedAmount;

    @Column(
            name = "change_amount",
            precision = 10,
            scale = 2
    )
    private BigDecimal changeAmount;

    @Column(
            name = "cashier_username",
            length = 50
    )
    private String cashierUsername;
}