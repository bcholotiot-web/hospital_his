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
public class LaboratoryOrderPaymentSearchResponse {

    private Long laboratoryOrderId;

    private String orderNumber;

    private String patientName;

    private String patientDpi;

    private String doctorName;

    private String branch;

    private Integer totalTests;

    private BigDecimal totalAmount;

    private String currency;

    private String status;

    private Boolean externalOrder;

    private String createdAt;

    private Boolean canPay;

    private String message;
}