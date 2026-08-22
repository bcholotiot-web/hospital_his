package com.hospital.his.payments.dto;

import lombok.*;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
//Este DTO sirve para mostrar el comprobante requerido para CU-04
public class PaymentResponse {

    private Long paymentId;

    private Long appointmentId;

    private String transactionNumber;

    private BigDecimal amount;

    private String currency;

    private String status;

    private String cardLastFour;

    private String processedAt;

    private String doctorName;

    private String specialty;

    private String branch;

    private String appointmentDateTime;

    private String message;
}