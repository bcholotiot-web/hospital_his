package com.hospital.his.payments.dto;

import lombok.*;

import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ProcessPaymentRequest {

    private Long appointmentId;

    /*
     * Identificador único generado una sola vez
     * por el frontend para evitar doble envío.
     */
    private UUID idempotencyKey;

    /*
     * Token de prueba o token generado en el
     * frontend por el SDK de una pasarela.
     */
    private String paymentToken;

    private String cardholderName;

    /*
     * Solo para mostrar en el comprobante.
     * Nunca debe contener el número completo.
     */
    private String cardLastFour;
}