package com.hospital.his.payments.gateway;

import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.util.UUID;

@Component
public class SimulatedPaymentGateway
        implements PaymentGateway {

    private static final String APPROVED_TOKEN =
            "tok_test_approved";

    private static final String DECLINED_TOKEN =
            "tok_test_declined";

    private static final String PROCESSING_ERROR_TOKEN =
            "tok_test_error";

    private static final String COMMUNICATION_ERROR_TOKEN =
            "tok_test_communication_error";

    @Override
    public GatewayPaymentResult processPayment(
            String paymentToken,
            BigDecimal amount,
            String currency
    ) {
        validateRequest(
                paymentToken,
                amount,
                currency
        );

        return switch (paymentToken) {

            case APPROVED_TOKEN ->
                    approvedResult();

            case DECLINED_TOKEN ->
                    declinedResult();

            case PROCESSING_ERROR_TOKEN ->
                    processingErrorResult();

            case COMMUNICATION_ERROR_TOKEN ->
                    communicationErrorResult();

            default ->
                    invalidTokenResult();
        };
    }

    private void validateRequest(
            String paymentToken,
            BigDecimal amount,
            String currency
    ) {
        if (paymentToken == null ||
                paymentToken.isBlank()) {

            throw new RuntimeException(
                    "El token de pago es obligatorio."
            );
        }

        if (amount == null ||
                amount.compareTo(BigDecimal.ZERO) <= 0) {

            throw new RuntimeException(
                    "El monto del pago no es válido."
            );
        }

        if (currency == null ||
                currency.isBlank()) {

            throw new RuntimeException(
                    "La moneda del pago es obligatoria."
            );
        }
    }

    private GatewayPaymentResult approvedResult() {

        String transactionNumber =
                "TXN-"
                        + UUID.randomUUID()
                        .toString()
                        .replace("-", "")
                        .substring(0, 12)
                        .toUpperCase();

        String gatewayReference =
                "GATEWAY-"
                        + UUID.randomUUID();

        return GatewayPaymentResult.builder()
                .approved(true)
                .transactionNumber(
                        transactionNumber
                )
                .gatewayReference(
                        gatewayReference
                )
                .failureCode(null)
                .failureMessage(null)
                .build();
    }

    private GatewayPaymentResult declinedResult() {

        return GatewayPaymentResult.builder()
                .approved(false)
                .transactionNumber(null)
                .gatewayReference(
                        "DECLINED-"
                                + UUID.randomUUID()
                )
                .failureCode(
                        "BANK_DECLINED"
                )
                .failureMessage(
                        "La transacción con tarjeta fue rechazada por el banco. "
                                + "Por favor, verifique los datos de su tarjeta "
                                + "o intente con una tarjeta diferente."
                )
                .build();
    }

    private GatewayPaymentResult processingErrorResult() {

        return GatewayPaymentResult.builder()
                .approved(false)
                .transactionNumber(null)
                .gatewayReference(
                        "ERROR-"
                                + UUID.randomUUID()
                )
                .failureCode(
                        "PROCESSING_ERROR"
                )
                .failureMessage(
                        "El pago no pudo ser procesado. "
                                + "Por favor, intente nuevamente "
                                + "o utilice otra tarjeta."
                )
                .build();
    }

    private GatewayPaymentResult communicationErrorResult() {

        return GatewayPaymentResult.builder()
                .approved(false)
                .transactionNumber(null)
                .gatewayReference(
                        "COMMUNICATION-"
                                + UUID.randomUUID()
                )
                .failureCode(
                        "COMMUNICATION_ERROR"
                )
                .failureMessage(
                        "Error de comunicación con la pasarela de pago. "
                                + "Intente nuevamente en unos minutos."
                )
                .build();
    }

    private GatewayPaymentResult invalidTokenResult() {

        return GatewayPaymentResult.builder()
                .approved(false)
                .transactionNumber(null)
                .gatewayReference(
                        "INVALID-"
                                + UUID.randomUUID()
                )
                .failureCode(
                        "INVALID_PAYMENT_TOKEN"
                )
                .failureMessage(
                        "El método de pago proporcionado no es válido."
                )
                .build();
    }
}