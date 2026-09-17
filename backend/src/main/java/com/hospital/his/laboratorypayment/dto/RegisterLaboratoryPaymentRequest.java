package com.hospital.his.laboratorypayment.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
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

    /**
     * Orden que será cobrada.
     */
    @NotNull(
            message =
                    "Debe indicar la orden de laboratorio."
    )
    private Long laboratoryOrderId;

    /**
     * Valores permitidos:
     *
     * EFECTIVO
     * VISA
     * MASTERCARD
     * DEBITO
     */
    @NotBlank(
            message =
                    "Debe seleccionar un método de pago."
    )
    private String paymentMethod;

    /**
     * Obligatorio únicamente para EFECTIVO.
     */
    private BigDecimal receivedAmount;

    /**
     * Obligatorio para VISA, MASTERCARD o DEBITO.
     */
    private String cardLastFour;

    /**
     * Token del simulador.
     *
     * No se utiliza cuando el método es EFECTIVO.
     */
    private String paymentToken;

    /**
     * UUID generado por el frontend.
     */
    @NotNull(
            message =
                    "La llave de idempotencia es obligatoria."
    )
    private UUID idempotencyKey;
}