package com.hospital.his.laboratorypayment.dto;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LaboratoryPaymentResponse {

    private Long laboratoryPaymentId;

    private Long laboratoryOrderId;

    private String orderNumber;

    private String patientName;

    private String patientDpi;

    private String doctorName;

    private String branch;

    private Integer totalTests;

    private BigDecimal amount;

    private String currency;

    private String paymentMethod;

    private String paymentStatus;

    private BigDecimal receivedAmount;

    private BigDecimal changeAmount;

    private String cardLastFour;

    private String transactionNumber;

    private String cashierUsername;

    private String processedAt;

    private String laboratoryOrderStatus;

    private String message;
}