package com.hospital.his.payments.gateway;

import java.math.BigDecimal;

public interface PaymentGateway {

    GatewayPaymentResult processPayment(
            String paymentToken,
            BigDecimal amount,
            String currency
    );
}