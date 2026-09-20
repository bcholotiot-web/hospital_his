package com.hospital.his.pharmacy.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.*;

import java.math.BigDecimal;
import java.util.List;
import java.util.UUID;

//Request principal del despacho
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class ConfirmDispensationRequest {

    @NotNull(
            message =
                    "Debe indicar la receta médica."
    )
    private Long prescriptionId;

    /**
     * Evita procesar dos veces el mismo despacho.
     */
    @NotNull(
            message =
                    "La llave de idempotencia es obligatoria."
    )
    private UUID idempotencyKey;

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
     * Obligatorio únicamente para efectivo.
     */
    private BigDecimal receivedAmount;

    /**
     * Obligatorio para pagos con tarjeta.
     */
    private String cardLastFour;

    /**
     * Token generado por la pasarela simulada.
     */
    private String paymentToken;

    /**
     * Medicamentos que se entregarán.
     *
     * No es obligatorio incluir todos los medicamentos
     * pendientes, porque se permite despacho parcial.
     */
    @NotEmpty(
            message =
                    "Debe seleccionar al menos un medicamento "
                            + "para el despacho."
    )
    private List<
            @Valid
            @NotNull(
                    message =
                            "El despacho contiene un elemento inválido."
            )
                    DispensationItemRequest
            > items;
}