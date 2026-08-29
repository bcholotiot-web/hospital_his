package com.hospital.his.payments.dto;

import lombok.*;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterCashPaymentRequest {

    private Long appointmentId;

    private String paymentMethod;

    /*
     * Solo se requiere cuando el método
     * seleccionado es EFECTIVO.
     */
    private BigDecimal receivedAmount;

    /*
     * Solo se requiere para pagos con tarjeta.
     * Nunca se almacena el número completo.
     */
    private String cardLastFour;

    /*
     * Token utilizado únicamente por la
     * pasarela simulada.
     */
    private String paymentToken;

    private UUID idempotencyKey;
}