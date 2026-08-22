package com.hospital.his.payments.gateway;

import lombok.*;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class GatewayPaymentResult {
/*approved
    Indica si la pasarela aprobó el pago.

transactionNumber
    Número de transacción para el comprobante.

gatewayReference
    Referencia interna de la pasarela.

failureCode
    Código del error si el pago falla.

failureMessage
    Mensaje amigable del error.*/
    private Boolean approved;

    private String transactionNumber;

    private String gatewayReference;

    private String failureCode;

    private String failureMessage;
}