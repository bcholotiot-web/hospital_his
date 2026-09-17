package com.hospital.his.laboratorypayment.dto;

//Clase encargada de entregar los examenes para la pantalla de cobro y el comprobante

import com.hospital.his.laboratory.dto.LaboratoryOrderItemResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class LaboratoryOrderPaymentDetailResponse {

    private Long laboratoryOrderId;

    private String orderNumber;

    private String patientName;

    private String patientDpi;

    private String doctorName;

    private String branch;

    private String createdAt;

    private String status;

    private Integer totalTests;

    private BigDecimal totalAmount;

    private String currency;

    private Boolean canPay;

    private List<LaboratoryOrderItemResponse> items;

    private String message;
}