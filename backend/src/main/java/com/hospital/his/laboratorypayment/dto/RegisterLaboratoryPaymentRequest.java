package com.hospital.his.laboratorypayment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.UUID;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class RegisterLaboratoryPaymentRequest {

    private Long laboratoryOrderId;

    /*
     * Valores permitidos:
     * EFECTIVO
     * VISA
     * MASTERCARD
     * DEBITO
     */
    private String paymentMethod;

    /*
     * Obligatorio únicamente para EFECTIVO.
     */
    private BigDecimal receivedAmount;

    /*
     * Obligatorio para VISA, MASTERCARD o DEBITO.
     * Debe contener exactamente cuatro dígitos.
     */
    private String cardLastFour;

    /*
     * Token de la pasarela simulada.
     *
     * Ejemplos:
     * tok_test_approved
     * tok_test_declined
     * tok_test_error
     * tok_test_communication_error
     */
    private String paymentToken;

    /*
     * UUID generado por el frontend para impedir
     * el doble procesamiento.
     */
    private UUID idempotencyKey;
}